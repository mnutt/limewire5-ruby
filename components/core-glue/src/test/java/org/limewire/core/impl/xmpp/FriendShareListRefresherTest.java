package org.limewire.core.impl.xmpp;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.core.api.browse.server.BrowseTracker;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.feature.Feature;
import org.limewire.core.api.friend.feature.features.LibraryChangedNotifier;
import org.limewire.core.api.friend.feature.features.LibraryChangedNotifierFeature;
import org.limewire.core.api.library.FriendShareListEvent;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.impl.xmpp.FriendShareListRefresher.FinishedLoadingListener;
import org.limewire.core.impl.xmpp.FriendShareListRefresher.LibraryChangedSender;
import org.limewire.core.impl.xmpp.FriendShareListRefresher.LibraryChangedSender.ScheduledLibraryRefreshSender;
import org.limewire.listener.ListenerSupport;
import org.limewire.util.BaseTestCase;
import org.limewire.xmpp.api.client.User;
import org.limewire.xmpp.api.client.XMPPConnection;
import org.limewire.xmpp.api.client.XMPPService;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;

import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.library.ManagedFileList;
import com.limegroup.gnutella.library.ManagedListStatusEvent;

public class FriendShareListRefresherTest extends BaseTestCase {

    public FriendShareListRefresherTest(String name) {
        super(name);
    }

    @SuppressWarnings("unchecked")
    public void testRegister() {
        Mockery context = new Mockery();

        final FileManager fileManager = context.mock(FileManager.class);
        final ManagedFileList managedFileList = context.mock(ManagedFileList.class);
        
        final ListenerSupport<FriendShareListEvent> listenerSupport
            = context.mock(ListenerSupport.class);
      
        final FriendShareListRefresher friendShareListRefresher = new FriendShareListRefresher(null, null, null);
        
        context.checking(new Expectations() {
            {
                exactly(1).of(listenerSupport).addListener(with(same(friendShareListRefresher)));
                
                allowing(fileManager).getManagedFileList();
                will(returnValue(managedFileList));
                
                exactly(1).of(managedFileList).addManagedListStatusListener(with(any(FinishedLoadingListener.class)));
                
            }});
        
        friendShareListRefresher.register(fileManager);
        friendShareListRefresher.register(listenerSupport);
        
        context.assertIsSatisfied();
    }

    /**
     * Confirm completion listeners are added and removed with respective friend share list events.
     */
    @SuppressWarnings("unchecked")
    public void testHandleEvent() {
        Mockery context = new Mockery() {
            {   setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        final FriendShareListEvent event1 = context.mock(FriendShareListEvent.class);
        final FriendShareListEvent event2 = context.mock(FriendShareListEvent.class);
        final FriendShareListEvent event3 = context.mock(FriendShareListEvent.class);
        final Friend friend = context.mock(Friend.class);
        final LocalFileList localFileList = context.mock(LocalFileList.class);
        final EventList<LocalFileItem> eventList = context.mock(EventList.class);;
        
        final FriendShareListRefresher friendShareListRefresher = new FriendShareListRefresher(null, null, null);
        
        final Sequence resultSequence = context.sequence("resultSequence");
        
        context.checking(new Expectations() {
            {
                // These should be checked
                atLeast(1).of(event1).getType();
                will(returnValue(FriendShareListEvent.Type.FRIEND_SHARE_LIST_ADDED));
                atLeast(1).of(event2).getType();
                will(returnValue(FriendShareListEvent.Type.FRIEND_SHARE_LIST_REMOVED));
                atLeast(1).of(event3).getType();
                will(returnValue(FriendShareListEvent.Type.FRIEND_SHARE_LIST_DELETED));
                
                // Non critical actions
                                
                allowing(event1).getFriend();
                will(returnValue(friend));
                
                allowing(event2).getFriend();
                will(returnValue(friend));
                
                allowing(friend).getId();
                will(returnValue("sadsadsa"));
                
                allowing(event1).getFileList();
                will(returnValue(localFileList));
                
                allowing(event2).getFileList();
                will(returnValue(localFileList));
                
                allowing(localFileList).getModel();
                will(returnValue(eventList));
                
                // Assertions
                exactly(1).of(eventList).addListEventListener(with(any(LibraryChangedSender.class)));
                inSequence(resultSequence);
                
                exactly(1).of(eventList).removeListEventListener(with(any(LibraryChangedSender.class)));
                inSequence(resultSequence);
            }});
        
        
        friendShareListRefresher.handleEvent(event1);
        friendShareListRefresher.handleEvent(event2);
        friendShareListRefresher.handleEvent(event3);
        
        context.assertIsSatisfied();

    }
    
    /**
     * Fire handleEvent() of the FinishedLoadingListener with SAVE and LOAD_FINISHING.
     * 
     * Confirm that nothing happens
     */
    public void testFinishedLoadingListenerWithOtherEvents() {
        Mockery context = new Mockery() {
            {   setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        final BrowseTracker tracker = context.mock(BrowseTracker.class);
        final XMPPService xmppService = context.mock(XMPPService.class);
        final ScheduledExecutorService scheduledExecutorService = context.mock(ScheduledExecutorService.class);
        final ManagedListStatusEvent event = context.mock(ManagedListStatusEvent.class);
        
        final FriendShareListRefresher friendShareListRefresher = new FriendShareListRefresher(tracker,
                xmppService, scheduledExecutorService);

        final FinishedLoadingListener finishedLoadingListener = friendShareListRefresher.new FinishedLoadingListener();
        
        context.checking(new Expectations() {
            {   exactly(1).of(event).getType();
                will(returnValue(ManagedListStatusEvent.Type.SAVE));
                exactly(1).of(event).getType();
                will(returnValue(ManagedListStatusEvent.Type.LOAD_FINISHING));
            }});
        
        finishedLoadingListener.handleEvent(event);
        finishedLoadingListener.handleEvent(event);
        
        context.assertIsSatisfied();
    }
    
    
    /**
     * Fire handleEvent() of the FinishedLoadingListener
     * 
     * Ensure that refreshes are sent to all friend and presences
     */
    @SuppressWarnings("unchecked")
    public void testFinishedLoadingListenerWithLoadComplete() {
        Mockery context = new Mockery() {
            {   setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        final BrowseTracker tracker = context.mock(BrowseTracker.class);
        final XMPPService xmppService = context.mock(XMPPService.class);
        final ScheduledExecutorService scheduledExecutorService = context.mock(ScheduledExecutorService.class);
        final ManagedListStatusEvent event = context.mock(ManagedListStatusEvent.class);
        
        final XMPPConnection xmppConnection = context.mock(XMPPConnection.class);
        
        final LinkedList<User> users = new LinkedList<User>();
        users.add(context.mock(User.class));
        users.add(context.mock(User.class));
        
        final Map<String, FriendPresence> presences1 = new HashMap<String, FriendPresence>();
        final Map<String, FriendPresence> presences2 = new HashMap<String, FriendPresence>();
                
        presences1.put("a", context.mock(FriendPresence.class));
        presences1.put("b", context.mock(FriendPresence.class));
        presences2.put("a", context.mock(FriendPresence.class));
        
        final Feature<LibraryChangedNotifier> featureA = context.mock(Feature.class);
        final Feature<LibraryChangedNotifier> featureB = context.mock(Feature.class);
        
        final LibraryChangedNotifier notifierA = context.mock(LibraryChangedNotifier.class);
        final LibraryChangedNotifier notifierB = context.mock(LibraryChangedNotifier.class); 
        
        final FriendShareListRefresher friendShareListRefresher = new FriendShareListRefresher(tracker,
                xmppService, scheduledExecutorService);

        final FinishedLoadingListener finishedLoadingListener = friendShareListRefresher.new FinishedLoadingListener();
        
        final String id1 = "1234";
        final String id2 = "5678";
        
        context.checking(new Expectations() {
            {   // These should be checked
                atLeast(1).of(event).getType();
                will(returnValue(ManagedListStatusEvent.Type.LOAD_COMPLETE));

                // Non critical actions
                allowing(xmppService).getActiveConnection();
                will(returnValue(xmppConnection));
                allowing(xmppConnection).getUsers();
                will(returnValue(users));
                allowing(users.get(0)).getId();
                will(returnValue(id1));
                allowing(users.get(1)).getId();
                will(returnValue(id2));
                allowing(users.get(0)).getFriendPresences();
                will(returnValue(presences1));
                allowing(users.get(1)).getFriendPresences();
                will(returnValue(presences2));
                allowing(presences1.get("a")).getFeature(LibraryChangedNotifierFeature.ID);
                will(returnValue(featureA));
                allowing(presences1.get("b")).getFeature(LibraryChangedNotifierFeature.ID);
                will(returnValue(featureB));
                allowing(presences2.get("a")).getFeature(LibraryChangedNotifierFeature.ID);
                will(returnValue(featureA));
                allowing(featureA).getFeature();
                will(returnValue(notifierA));
                allowing(featureB).getFeature();
                will(returnValue(notifierB));
                
                // Assertions
                exactly(1).of(tracker).sentRefresh(id1);
                exactly(1).of(tracker).sentRefresh(id2);
                exactly(2).of(notifierA).sendLibraryRefresh();
                exactly(1).of(notifierB).sendLibraryRefresh();
                
            }});
        
        finishedLoadingListener.handleEvent(event);
        
        context.assertIsSatisfied();
    }
    
    /**
     * Fire handleEvent() of the FinishedLoadingListener
     * 
     * Ensure that null connections and features are are handled correctly 
     */
    @SuppressWarnings("unchecked")
    public void testFinishedLoadingListenerWithNulls() {
        Mockery context = new Mockery() {
            {   setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        final BrowseTracker tracker = context.mock(BrowseTracker.class);
        final XMPPService xmppService = context.mock(XMPPService.class);
        final ScheduledExecutorService scheduledExecutorService = context.mock(ScheduledExecutorService.class);
        final ManagedListStatusEvent event = context.mock(ManagedListStatusEvent.class);
        
        final XMPPConnection xmppConnection = context.mock(XMPPConnection.class);
        
        final LinkedList<User> users = new LinkedList<User>();
        users.add(context.mock(User.class));
        users.add(context.mock(User.class));
        
        final Map<String, FriendPresence> presences1 = new HashMap<String, FriendPresence>();
        final Map<String, FriendPresence> presences2 = new HashMap<String, FriendPresence>();
                
        presences1.put("a", context.mock(FriendPresence.class));
        presences1.put("b", context.mock(FriendPresence.class));
        presences2.put("a", context.mock(FriendPresence.class));
        
        final Feature<LibraryChangedNotifier> featureA = context.mock(Feature.class);
        final Feature<LibraryChangedNotifier> featureB = context.mock(Feature.class);
        
        final LibraryChangedNotifier notifierA = context.mock(LibraryChangedNotifier.class);
        final LibraryChangedNotifier notifierB = context.mock(LibraryChangedNotifier.class); 
        
        final FriendShareListRefresher friendShareListRefresher = new FriendShareListRefresher(tracker,
                xmppService, scheduledExecutorService);

        final FinishedLoadingListener finishedLoadingListener = friendShareListRefresher.new FinishedLoadingListener();
        
        final String id1 = "1234";
        final String id2 = "5678";
        
        context.checking(new Expectations() {
            {   // These should be checked
                atLeast(1).of(event).getType();
                will(returnValue(ManagedListStatusEvent.Type.LOAD_COMPLETE));

                // Non essential interactions
                
                // --Stunt the first getActiveConnection() with null--
                one(xmppService).getActiveConnection();
                will(returnValue(null));
                
                allowing(xmppService).getActiveConnection();
                will(returnValue(xmppConnection));
                
                allowing(xmppConnection).getUsers();
                will(returnValue(users));
                allowing(users.get(0)).getId();
                will(returnValue(id1));
                allowing(users.get(1)).getId();
                will(returnValue(id2));
                allowing(users.get(0)).getFriendPresences();
                will(returnValue(presences1));
                allowing(users.get(1)).getFriendPresences();
                will(returnValue(presences2));
                
                // --Stunt the first feature request with null, make sure the process recovers--
                allowing(presences1.get("a")).getFeature(LibraryChangedNotifierFeature.ID);
                will(returnValue(null));
                
                allowing(presences1.get("b")).getFeature(LibraryChangedNotifierFeature.ID);
                will(returnValue(featureB));
                allowing(presences2.get("a")).getFeature(LibraryChangedNotifierFeature.ID);
                will(returnValue(featureA));
                allowing(featureA).getFeature();
                will(returnValue(notifierA));
                allowing(featureB).getFeature();
                will(returnValue(notifierB));
                
                // Assertions
                exactly(1).of(tracker).sentRefresh(id1);
                exactly(1).of(tracker).sentRefresh(id2);
                exactly(1).of(notifierA).sendLibraryRefresh();
                exactly(1).of(notifierB).sendLibraryRefresh();
                
            }});
        
        
        // This invocation should fail since the first getConnection() will return null
        finishedLoadingListener.handleEvent(event);
        
        // There will be only one refresh for notifierA since the first getFeature() will fail
        finishedLoadingListener.handleEvent(event);
        
        context.assertIsSatisfied();
    }
    
    /**
     * Test the listChanged() handler when the file manager has not yet been loaded.
     *  No new runnable should be scheduled.
     */
    @SuppressWarnings("unchecked")
    public void testLibraryChangedSenderWithoutFileManagedLoaded() {
        Mockery context = new Mockery() {
            {   setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        final BrowseTracker tracker = context.mock(BrowseTracker.class);
        final XMPPService xmppService = context.mock(XMPPService.class);
        final ScheduledExecutorService scheduledExecutorService = context.mock(ScheduledExecutorService.class);
        
        ListEvent<LocalFileItem> event = context.mock(ListEvent.class);
        
        final Friend friend = context.mock(Friend.class);
        
        final FriendShareListRefresher friendShareListRefresher = new FriendShareListRefresher(tracker,
                xmppService, scheduledExecutorService);
        
        // Ensure file manager loaded flag is cleared
        friendShareListRefresher.fileManagerLoaded.set(false);
        
        final LibraryChangedSender libraryChangedSender = friendShareListRefresher.new LibraryChangedSender(friend);
        
        context.checking(new Expectations() {
            {
                // Assertions
                never(scheduledExecutorService).schedule(with(any(ScheduledLibraryRefreshSender.class)),
                        with(any(Integer.class)), with(any(TimeUnit.class)));
                
            }});
        
        libraryChangedSender.listChanged(event);
        
        context.assertIsSatisfied();
    }
    
    /**
     * Create a new LibraryChangedSender within a ready FriendShareListRefresher and 
     *  fire a listChanged event.  Ensure that a new runnable is scheduled for the
     *  library refresh.
     */
    @SuppressWarnings("unchecked")
    public void testLibraryChangedSenderWithListChanged() {
        Mockery context = new Mockery() {
            {   setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        final BrowseTracker tracker = context.mock(BrowseTracker.class);
        final XMPPService xmppService = context.mock(XMPPService.class);
        final ScheduledExecutorService scheduledExecutorService = context.mock(ScheduledExecutorService.class);
        
        ListEvent<LocalFileItem> event = context.mock(ListEvent.class);
        
        final Friend friend = context.mock(Friend.class);
        
        final FriendShareListRefresher friendShareListRefresher = new FriendShareListRefresher(tracker,
                xmppService, scheduledExecutorService);
        friendShareListRefresher.fileManagerLoaded.set(true);
        
        final LibraryChangedSender libraryChangedSender = friendShareListRefresher.new LibraryChangedSender(friend);
        
        context.checking(new Expectations() {
            {
                // Assertions
                exactly(1).of(scheduledExecutorService).schedule(with(any(ScheduledLibraryRefreshSender.class)),
                        with(any(Integer.class)), with(any(TimeUnit.class)));
                
            }});
        
        libraryChangedSender.listChanged(event);
        
        context.assertIsSatisfied();
    }
    
    /**
     * Force fire a refresh action with the last browse time before the 
     *  last refresh time, therefore with no notifications necessary.
     *  
     * Ensure no notifications are made.
     */
    public void testScheduledLibraryRefreshSenderWithNoRefreshNeeded() {
        Mockery context = new Mockery();
        
        final BrowseTracker tracker = context.mock(BrowseTracker.class);
        final XMPPService xmppService = context.mock(XMPPService.class);
        final ScheduledExecutorService scheduledExecutorService = context.mock(ScheduledExecutorService.class);
        
        final Friend friend = context.mock(Friend.class);
        
        final FriendShareListRefresher friendShareListRefresher = new FriendShareListRefresher(tracker,
                xmppService, scheduledExecutorService);
        
        final LibraryChangedSender libraryChangedSender = friendShareListRefresher.new LibraryChangedSender(friend);
        
        final ScheduledLibraryRefreshSender scheduledLibraryRefreshSender = libraryChangedSender.new ScheduledLibraryRefreshSender();
        
        final String friendID = "anID";
        final Date lastBrowseTime = new Date(50);
        final Date lastRefreshTime = new Date(100);
        
        
        context.checking(new Expectations() {
            {   
                // Non critical actions
                allowing(friend).getId();
                will(returnValue(friendID));
                
                // Browse times should at least be compared
                atLeast(1).of(tracker).lastBrowseTime(friendID);
                will(returnValue(lastBrowseTime));
                atLeast(1).of(tracker).lastRefreshTime(friendID);
                will(returnValue(lastRefreshTime));
                
            }});
        
        scheduledLibraryRefreshSender.run();
        
        context.assertIsSatisfied();
    }

    /**
     * Force fire a refresh action with the last browse time after the 
     *  last refresh time, therefore with notifications necessary.  Also,
     *  force one of the feature lookups to return null and make sure the 
     *  failure is handled correctly.
     *  
     * Ensure that the browse tracker and friend presence notifications are made.  
     */
    @SuppressWarnings("unchecked")
    public void testScheduledLibraryRefreshSender() {
        Mockery context = new Mockery() {
            {   setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
        
        final BrowseTracker tracker = context.mock(BrowseTracker.class);
        final XMPPService xmppService = context.mock(XMPPService.class);
        final ScheduledExecutorService scheduledExecutorService = context.mock(ScheduledExecutorService.class);
        
        
        final Map<String, FriendPresence> presences = new HashMap<String, FriendPresence>();
        presences.put("a", context.mock(FriendPresence.class));
        presences.put("b", context.mock(FriendPresence.class));
        
        final Feature<LibraryChangedNotifier> featureA = context.mock(Feature.class);
        final Feature<LibraryChangedNotifier> featureB = context.mock(Feature.class);
        
        final LibraryChangedNotifier notifierA = context.mock(LibraryChangedNotifier.class);
        final LibraryChangedNotifier notifierB = context.mock(LibraryChangedNotifier.class); 
        
        final Friend friend = context.mock(Friend.class);
        
        final FriendShareListRefresher friendShareListRefresher = new FriendShareListRefresher(tracker,
                xmppService, scheduledExecutorService);
        
        final LibraryChangedSender libraryChangedSender = friendShareListRefresher.new LibraryChangedSender(friend);
        
        final ScheduledLibraryRefreshSender scheduledLibraryRefreshSender = libraryChangedSender.new ScheduledLibraryRefreshSender();
        
        final String friendID = "anID";
        final Date lastBrowseTime = new Date(100);
        final Date lastRefreshTime = new Date(50);
        
        context.checking(new Expectations() {
            {   
                // Non critical actions
                allowing(friend).getId();
                will(returnValue(friendID));
                allowing(friend).getFriendPresences();
                will(returnValue(presences));
                allowing(presences.get("a")).getFeature(LibraryChangedNotifierFeature.ID);
                will(returnValue(featureA));
                
                // -- Stunt return of feature lookup on B with null --
                allowing(presences.get("b")).getFeature(LibraryChangedNotifierFeature.ID);
                will(returnValue(null));
                
                allowing(featureA).getFeature();
                will(returnValue(notifierA));
                allowing(featureB).getFeature();
                will(returnValue(notifierB));
                
                // Browse times should at least be compared
                atLeast(1).of(tracker).lastBrowseTime(friendID);
                will(returnValue(lastBrowseTime));
                atLeast(1).of(tracker).lastRefreshTime(friendID);
                will(returnValue(lastRefreshTime));
                
                // Assertions -- B should not be notified since it's getFeature() failed
                exactly(1).of(tracker).sentRefresh(friendID);
                exactly(1).of(notifierA).sendLibraryRefresh();
                never(notifierB).sendLibraryRefresh();
            }});
        
        scheduledLibraryRefreshSender.run();
        
        context.assertIsSatisfied();
    }
}
