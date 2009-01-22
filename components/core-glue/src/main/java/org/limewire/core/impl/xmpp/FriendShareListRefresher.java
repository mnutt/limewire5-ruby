package org.limewire.core.impl.xmpp;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import org.limewire.collection.Periodic;
import org.limewire.core.api.browse.server.BrowseTracker;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.feature.Feature;
import org.limewire.core.api.friend.feature.features.LibraryChangedNotifier;
import org.limewire.core.api.friend.feature.features.LibraryChangedNotifierFeature;
import org.limewire.core.api.library.FriendShareListEvent;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.listener.BlockingEvent;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.RegisteringEventListener;
import org.limewire.xmpp.api.client.User;
import org.limewire.xmpp.api.client.XMPPConnection;
import org.limewire.xmpp.api.client.XMPPService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.library.ManagedListStatusEvent;

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

/**
 * Sends library changed messages to friends when:<BR>
 * 1) File manager is finished loading<BR>
 * OR<BR>
 * 2) A friend's sharelist changes
 */

@Singleton
class FriendShareListRefresher implements RegisteringEventListener<FriendShareListEvent> {

    private final BrowseTracker tracker;
    private final XMPPService xmppService;
    private final ScheduledExecutorService scheduledExecutorService;

    private final Map<String, LibraryChangedSender> listeners;
    private final AtomicBoolean fileManagerLoaded = new AtomicBoolean(false);

    @Inject
    FriendShareListRefresher(BrowseTracker tracker,
                             XMPPService xmppService,
                             @Named("backgroundExecutor")ScheduledExecutorService scheduledExecutorService) {
        this.tracker = tracker;
        this.xmppService = xmppService;
        this.scheduledExecutorService = scheduledExecutorService;
        listeners = new ConcurrentHashMap<String, LibraryChangedSender>();
    }

    @Inject
    public void register(FileManager fileManager) {
        fileManager.getManagedFileList().addManagedListStatusListener(new FinishedLoadingListener());
    }

    @Inject
    public void register(ListenerSupport<FriendShareListEvent> friendShareListEventListenerSupport) {
        friendShareListEventListenerSupport.addListener(this);
    }

    public void handleEvent(final FriendShareListEvent event) {
        if(event.getType() == FriendShareListEvent.Type.FRIEND_SHARE_LIST_ADDED) {
            LibraryChangedSender listener = new LibraryChangedSender(event.getFriend());
            listeners.put(event.getFriend().getId(), listener);
            event.getFileList().getModel().addListEventListener(listener);
        } else if(event.getType() == FriendShareListEvent.Type.FRIEND_SHARE_LIST_REMOVED) {
            event.getFileList().getModel().removeListEventListener(listeners.remove(event.getFriend().getId()));
        }
    }
    
    private class FinishedLoadingListener implements EventListener<ManagedListStatusEvent> {
        @SuppressWarnings("unchecked")
        @BlockingEvent
        public void handleEvent(ManagedListStatusEvent evt) {
            if(evt.getType() == ManagedListStatusEvent.Type.LOAD_COMPLETE) {
                fileManagerLoaded.set(true);  
                XMPPConnection connection = xmppService.getActiveConnection();
                if(connection != null) {
                    Collection<User> friends = connection.getUsers();
                    for(Friend friend : friends) {
                        tracker.sentRefresh(friend.getId());
                        Map<String, FriendPresence> presences = friend.getFriendPresences();
                        for(FriendPresence presence : presences.values()) {
                            Feature<LibraryChangedNotifier> notifier = presence.getFeature(LibraryChangedNotifierFeature.ID);                                
                            if(notifier != null) {
                                notifier.getFeature().sendLibraryRefresh();
                            }
                        }
                    }
                }
            }
        }            
    }
    
    private class LibraryChangedSender implements ListEventListener<LocalFileItem> {
        
        private final Friend friend;
        
        private final Periodic libraryRefreshPeriodic;
        
        LibraryChangedSender(Friend friend){
            this.friend = friend;
            this.libraryRefreshPeriodic = new Periodic(new ScheduledLibraryRefreshSender(), scheduledExecutorService);
        }        
        
        public void listChanged(ListEvent<LocalFileItem> listChanges) {
            if(fileManagerLoaded.get()) {
                libraryRefreshPeriodic.rescheduleIfLater(5000);                                
            }
        }        
    
        private class ScheduledLibraryRefreshSender implements Runnable {
    
            @SuppressWarnings("unchecked")
            @Override
            public void run() {
                BrowseTracker browseTracker = FriendShareListRefresher.this.tracker;
                Date lastBrowseTime = browseTracker.lastBrowseTime(friend.getId());
                Date lastRefreshTime = browseTracker.lastRefreshTime(friend.getId());
                if(lastBrowseTime != null && (lastRefreshTime == null || lastBrowseTime.after(lastRefreshTime))) {
                    browseTracker.sentRefresh(friend.getId());
                    Map<String,FriendPresence> presences = friend.getFriendPresences();
                    for(FriendPresence presence : presences.values()) {
                        Feature<LibraryChangedNotifier> notifier = presence.getFeature(LibraryChangedNotifierFeature.ID);
                        if(notifier != null) {
                            notifier.getFeature().sendLibraryRefresh();
                        }
                    }
                }
            }
        }
    }
}
