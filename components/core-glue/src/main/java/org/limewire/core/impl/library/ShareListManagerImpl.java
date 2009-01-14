package org.limewire.core.impl.library;

import java.awt.EventQueue;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.Category;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendEvent;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.FileList;
import org.limewire.core.api.library.FriendFileList;
import org.limewire.core.api.library.FriendShareListEvent;
import org.limewire.core.api.library.GnutellaFileList;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import ca.odell.glazedlists.CompositeList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.TransformedList;
import ca.odell.glazedlists.matchers.Matcher;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.FileListChangedEvent;
import com.limegroup.gnutella.library.FileManager;

@Singleton
class ShareListManagerImpl implements ShareListManager {
    
    private static final Log LOG = LogFactory.getLog(ShareListManagerImpl.class);
    
    private final FileManager fileManager;
    
    private final CombinedShareList combinedShareList;
    
    private final GnutellaFileListImpl gnutellaFileList;    
    
    private final ConcurrentHashMap<String, FriendFileListImpl> friendLocalFileLists;

    private final EventListener<FriendShareListEvent> friendShareListEventListener;
    
    private final CoreLocalFileItemFactory coreLocalFileItemFactory;

    @Inject
    ShareListManagerImpl(FileManager fileManager, CoreLocalFileItemFactory coreLocalFileItemFactory,
            EventListener<FriendShareListEvent> friendShareListEventListener) {
        this.fileManager = fileManager;
        this.coreLocalFileItemFactory = coreLocalFileItemFactory;
        this.friendShareListEventListener = friendShareListEventListener;
        this.combinedShareList = new CombinedShareList();
        this.gnutellaFileList = new GnutellaFileListImpl(fileManager.getGnutellaFileList());
        this.friendLocalFileLists = new ConcurrentHashMap<String, FriendFileListImpl>();
    }

    @Inject
    void register(@Named("known")ListenerSupport<FriendEvent> knownListeners) {

        knownListeners.addListener(new EventListener<FriendEvent>() {
            @Override
            public void handleEvent(FriendEvent event) {
                Friend friend = event.getSource();
                switch (event.getType()) {
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
     *
     * @param friend
     */
    private void unloadFilesForFriend(Friend friend) {  
        fileManager.unloadFilesForFriend(friend.getId());
        FriendFileListImpl list = friendLocalFileLists.remove(friend.getId());
        if(list != null) {
            friendShareListEventListener.handleEvent(new FriendShareListEvent(FriendShareListEvent.Type.FRIEND_SHARE_LIST_REMOVED, list, friend));
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
        
        FriendFileListImpl newList = new FriendFileListImpl(fileManager, friend.getId());        
        FriendFileList existing = friendLocalFileLists.putIfAbsent(friend.getId(), newList);        
        
        if(existing == null) {
            LOG.debugf("No existing library for friend {0}", friend.getId());
            newList.commit();
            friendShareListEventListener.handleEvent(new FriendShareListEvent(FriendShareListEvent.Type.FRIEND_SHARE_LIST_ADDED, newList, friend));
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
            friendShareListEventListener.handleEvent(new FriendShareListEvent(FriendShareListEvent.Type.FRIEND_SHARE_LIST_DELETED, list, friend));
            list.dispose();
        }        
    }
    
    private class GnutellaFileListImpl extends AbstractFriendFileList implements GnutellaFileList {
        private final com.limegroup.gnutella.library.GnutellaFileList shareList;
        
        public GnutellaFileListImpl(com.limegroup.gnutella.library.GnutellaFileList shareList) {
            super(combinedShareList.createMemberList(), coreLocalFileItemFactory);            
            this.shareList = shareList;
            this.shareList.addFileListListener(newEventListener());
            combinedShareList.addMemberList(baseList);
        }
        
        @Override
        protected com.limegroup.gnutella.library.GnutellaFileList getCoreFileList() {
            return shareList;
        }

        @Override
        public void removeDocuments() {
            getCoreFileList().removeDocuments();
        }
    }
    
    private class FriendFileListImpl extends AbstractFriendFileList {
        private final FileManager fileManager;
        private com.limegroup.gnutella.library.FriendFileList friendFileList;
        private final String name;
        private volatile boolean committed = false;
        private volatile EventListener<FileListChangedEvent> eventListener;
        
        FriendFileListImpl(FileManager fileManager, String name) {
            super(combinedShareList.createMemberList(), coreLocalFileItemFactory);            
            this.fileManager = fileManager;
            this.name = name;
        }
        
        @Override
        protected com.limegroup.gnutella.library.FriendFileList getCoreFileList() {
            return friendFileList;
        }
        
        @Override
        void dispose() {
            super.dispose();
            if(committed) {
                combinedShareList.removeMemberList(baseList);
                if(friendFileList != null)
                    friendFileList.removeFileListListener(eventListener);
            }
        }
        
        /* Commits to using this list. */
        void commit() {
            committed = true;
            eventListener = newEventListener();
            friendFileList = fileManager.getOrCreateFriendFileList(name);
            friendFileList.addFileListListener(eventListener);
            combinedShareList.addMemberList(baseList);

            com.limegroup.gnutella.library.FileList fileList = fileManager.getFriendFileList(name);

            fileList.getReadLock().lock();
            try {
                for (FileDesc fileDesc : fileList) {
                    addFileDesc(fileDesc);
                }
            } finally {
                fileList.getReadLock().unlock();
            }
        }
    } 
    
    private class CombinedShareList implements FileList<LocalFileItem> {
        private final CompositeList<LocalFileItem> compositeList;
        private final EventList<LocalFileItem> threadSafeUniqueList;
        private volatile TransformedList<LocalFileItem, LocalFileItem> swingList;
        
        public CombinedShareList() {
            compositeList = new CompositeList<LocalFileItem>();
            threadSafeUniqueList = GlazedListsFactory.uniqueList(GlazedListsFactory.threadSafeList(
                    GlazedListsFactory.readOnlyList(compositeList)),
                    new Comparator<LocalFileItem>() {
                @Override
                public int compare(LocalFileItem o1, LocalFileItem o2) {
                    return o1.getFile().getPath().compareTo(o2.getFile().getPath());
                }
            });
        }
        
        public void removeMemberList(EventList<LocalFileItem> eventList) {
            compositeList.getReadWriteLock().writeLock().lock();
            try {
                compositeList.removeMemberList(eventList);
            } finally {
                compositeList.getReadWriteLock().writeLock().unlock();    
            }
        }

        public EventList<LocalFileItem> createMemberList() {
            return compositeList.createMemberList();
        }
        
        public void addMemberList(EventList<LocalFileItem> eventList) {
            compositeList.getReadWriteLock().writeLock().lock();
            try {
                compositeList.addMemberList(eventList);
            } finally {
                compositeList.getReadWriteLock().writeLock().unlock();    
            }
        }  
        
        @Override
        public EventList<LocalFileItem> getModel() {
            return threadSafeUniqueList;
        }
        
        @Override
        public EventList<LocalFileItem> getSwingModel() {
            assert EventQueue.isDispatchThread();
            if(swingList == null) {
                swingList =  GlazedListsFactory.swingThreadProxyEventList(threadSafeUniqueList);
            }
            return swingList;
        }
        
        @Override
        public int size() {
            return threadSafeUniqueList.size();
        }
    }
    
    public class CategoryFilter implements Matcher<FileItem>{
        private final Category category;
        
        public CategoryFilter(Category category) {
            this.category = category;
        }

        @Override
        public boolean matches(FileItem item) {
            if (item == null) {
                return false;
            }
            
            if (category == null) {
                return true;
            }

            return item.getCategory().equals(category);
        }
    }
}
