package org.limewire.core.impl.library;

import java.util.concurrent.ConcurrentHashMap;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendEvent;
import org.limewire.core.api.library.FileList;
import org.limewire.core.api.library.FriendFileList;
import org.limewire.core.api.library.FriendShareListEvent;
import org.limewire.core.api.library.GnutellaFileList;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.listener.BlockingEvent;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.library.FileManager;

@Singleton
class ShareListManagerImpl implements ShareListManager {
    
    private static final Log LOG = LogFactory.getLog(ShareListManagerImpl.class);
    
    private final FileManager fileManager;
    
    private final CombinedShareList combinedShareList;
    
    private final GnutellaFileListImpl gnutellaFileList;    
    
    private final ConcurrentHashMap<String, FriendFileListImpl> friendLocalFileLists;

    private final EventBroadcaster<FriendShareListEvent> friendShareListEventBroadcaster;
    
    private final CoreLocalFileItemFactory coreLocalFileItemFactory;

    @Inject
    ShareListManagerImpl(FileManager fileManager, CoreLocalFileItemFactory coreLocalFileItemFactory,
            EventBroadcaster<FriendShareListEvent> friendShareListEventListener,
            LibraryManager libraryManager) {
        this.fileManager = fileManager;
        this.coreLocalFileItemFactory = coreLocalFileItemFactory;
        this.friendShareListEventBroadcaster = friendShareListEventListener;
        this.combinedShareList = new CombinedShareList(libraryManager.getLibraryListEventPublisher(), libraryManager.getReadWriteLock());
        this.gnutellaFileList = new GnutellaFileListImpl(coreLocalFileItemFactory, fileManager.getGnutellaFileList(), combinedShareList);
        this.friendLocalFileLists = new ConcurrentHashMap<String, FriendFileListImpl>();
    }

    @Inject
    void register(@Named("known")ListenerSupport<FriendEvent> knownListeners) {

        knownListeners.addListener(new EventListener<FriendEvent>() {
            @BlockingEvent(queueName="share list friend event handler")
            @Override
            public void handleEvent(FriendEvent event) {
                Friend friend = event.getData();
                switch (event.getType()) {
                    case ADDED:
                        getOrCreateFriendShareList(friend);
                        break;
                    case REMOVED:
                        unloadFilesForFriend(friend);
                        break;
                    case DELETE:
                        deleteFriendShareList(friend);
                        break;
                }
            }
        });
    }

    /**
     * Similar to {@link #deleteFriendShareList}, but does not save.
     * 
     * 1. Removes managedFilelist listener
     * 2. Removes lists associated with this friend from maps
     * 3. Clears the files from the list, decrement share count for each file
     */
    private void unloadFilesForFriend(Friend friend) {  
        fileManager.unloadFilesForFriend(friend.getId());
        FriendFileListImpl list = friendLocalFileLists.remove(friend.getId());
        if(list != null) {
            friendShareListEventBroadcaster.broadcast(new FriendShareListEvent(FriendShareListEvent.Type.FRIEND_SHARE_LIST_REMOVED, list, friend));
            list.dispose();
        }        
    }

    @Override
    public GnutellaFileList getGnutellaShareList() {
        return gnutellaFileList;
    }
    
    @Override
    public FileList<LocalFileItem> getCombinedShareList() {
        return combinedShareList;
    }
    
    @Override
    public FriendFileList getOrCreateFriendShareList(Friend friend) {
        LOG.debugf("get|Create library for friend {0}", friend.getId());
        
        FriendFileList list = friendLocalFileLists.get(friend.getId());
        if(list != null){
            LOG.debugf("Returning existing library for friend {0}", friend.getId());
            return list;
        }
        
        FriendFileListImpl newList = new FriendFileListImpl(coreLocalFileItemFactory, fileManager, friend.getId(), combinedShareList);        
        FriendFileList existing = friendLocalFileLists.putIfAbsent(friend.getId(), newList);        
        
        if(existing == null) {
            LOG.debugf("No existing library for friend {0}", friend.getId());
            newList.commit();
            friendShareListEventBroadcaster.broadcast(new FriendShareListEvent(FriendShareListEvent.Type.FRIEND_SHARE_LIST_ADDED, newList, friend));
            return newList;
        } else {
            LOG.debugf("Already an existing lib for friend {0}", friend.getId());
            newList.dispose();
            return existing;
        }
    }

    @Override
    public FriendFileList getFriendShareList(Friend friend) {
        return friendLocalFileLists.get(friend.getId());
    }

    private void deleteFriendShareList(Friend friend) {  
        fileManager.removeFriendFileList(friend.getId());
        FriendFileListImpl list = friendLocalFileLists.remove(friend.getId());
        if(list != null) {
            friendShareListEventBroadcaster.broadcast(new FriendShareListEvent(FriendShareListEvent.Type.FRIEND_SHARE_LIST_DELETED, list, friend));
            list.dispose();
        }        
    }
}
