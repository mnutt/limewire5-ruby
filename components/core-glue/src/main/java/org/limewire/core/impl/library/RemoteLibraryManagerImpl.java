package org.limewire.core.impl.library;

import java.awt.EventQueue;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Comparator;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.library.FileList;
import org.limewire.core.api.library.FriendLibrary;
import org.limewire.core.api.library.LibraryState;
import org.limewire.core.api.library.PresenceLibrary;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.util.StringUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.CompositeList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.ObservableElementList.Connector;
import ca.odell.glazedlists.TransformedList;
import ca.odell.glazedlists.UniqueList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventAssembler;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.event.ListEventPublisher;
import ca.odell.glazedlists.impl.ReadOnlyList;
import ca.odell.glazedlists.util.concurrent.LockFactory;
import ca.odell.glazedlists.util.concurrent.ReadWriteLock;

@Singleton
public class RemoteLibraryManagerImpl implements RemoteLibraryManager {
    
    private final AllFriendsLibraryImpl allFriendsList;
    private final EventList<FriendLibrary> allFriendLibraries;
    private final EventList<FriendLibrary> readOnlyFriendLibraries;
    private volatile EventList<FriendLibrary> swingFriendLibraries;
    private final ReadWriteLock lock;
    
    private static final RemoteFileComparator COMPARATOR = new RemoteFileComparator();

    @Inject
    public RemoteLibraryManagerImpl() {
        Connector<FriendLibrary> connector = GlazedLists.beanConnector(FriendLibrary.class);
        lock = LockFactory.DEFAULT.createReadWriteLock();
        allFriendLibraries = GlazedListsFactory.observableElementList(GlazedListsFactory.threadSafeList(new BasicEventList<FriendLibrary>(lock)), connector);
        readOnlyFriendLibraries = GlazedListsFactory.readOnlyList(allFriendLibraries);
        allFriendsList = new AllFriendsLibraryImpl(lock);
    }
    
    @Override
    public FileList<RemoteFileItem> getAllFriendsFileList() {
        return allFriendsList;
    }
    
    @Override
    public EventList<FriendLibrary> getFriendLibraryList() {
        return readOnlyFriendLibraries;
    }
    
    @Override
    public EventList<FriendLibrary> getSwingFriendLibraryList() {
        assert EventQueue.isDispatchThread();
        if(swingFriendLibraries == null) {
            swingFriendLibraries = GlazedListsFactory.swingThreadProxyEventList(readOnlyFriendLibraries);            
        }
        return swingFriendLibraries;
    }
    
    @Override
    public PresenceLibrary addPresenceLibrary(FriendPresence presence) {
        lock.writeLock().lock();
        try {
            FriendLibraryImpl friendLibrary = getOrCreateFriendLibrary(presence.getFriend());
            return friendLibrary.getOrCreatePresenceLibrary(presence);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public void removeFriendLibrary(Friend friend) {
        lock.writeLock().lock();
        try {
            FriendLibraryImpl friendLibrary = getFriendLibrary(friend);
            while(friendLibrary.allPresenceLibraries.size() > 0) {
                friendLibrary.removePresenceLibrary(friendLibrary.allPresenceLibraries.get(0).getPresence());
            }
            removeFriendLibrary(friendLibrary);                
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public void removePresenceLibrary(FriendPresence presence) {
        lock.writeLock().lock();
        try {
            FriendLibraryImpl friendLibrary = getFriendLibrary(presence.getFriend());
            if (friendLibrary != null) {
                friendLibrary.removePresenceLibrary(presence); 
                if (friendLibrary.getPresenceLibraryList().size() == 0) {
                    removeFriendLibrary(friendLibrary);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }                       
    }

    private FriendLibraryImpl findFriendLibrary(Friend friend) {
        for(FriendLibrary library : allFriendLibraries) {
            if(library.getFriend().getId().equals(friend.getId())) {
                return (FriendLibraryImpl)library;
            }
        }
        return null;
    }

    private FriendLibraryImpl getOrCreateFriendLibrary(Friend friend) {
        FriendLibraryImpl friendLibrary = findFriendLibrary(friend);
        if(friendLibrary == null) {
            friendLibrary = new FriendLibraryImpl(allFriendsList, friend, lock);
            allFriendsList.addMemberList(friendLibrary);
            friendLibrary.commit();
            allFriendLibraries.add(friendLibrary);
        }
        return friendLibrary;
    }

    @Override
    public boolean hasFriendLibrary(Friend friend) {
        lock.readLock().lock();
        try {
            return getFriendLibrary(friend) != null;
        } finally {
            lock.readLock().unlock();
        }        
    }
    
    private FriendLibraryImpl getFriendLibrary(Friend friend) {
        return findFriendLibrary(friend);
    }

    private void removeFriendLibrary(FriendLibraryImpl friendLibrary) {
        allFriendLibraries.remove(friendLibrary);
        allFriendsList.removeMemberList(friendLibrary);
        friendLibrary.dispose();
    }
    
    private static class AllFriendsLibraryImpl implements FileList<RemoteFileItem> {
        private final CompositeList<RemoteFileItem> compositeList;
        private final ReadOnlyList<RemoteFileItem> readOnlyList;
        private final UniqueList<RemoteFileItem> threadSafeUniqueList;
        private volatile TransformedList<RemoteFileItem, RemoteFileItem> swingList;
        
        public AllFriendsLibraryImpl(ReadWriteLock lock) {
            compositeList = new CompositeList<RemoteFileItem>(ListEventAssembler.createListEventPublisher(), lock);
            readOnlyList = GlazedListsFactory.readOnlyList(compositeList);
            threadSafeUniqueList = GlazedListsFactory.uniqueList(GlazedListsFactory.threadSafeList(readOnlyList),
                    new Comparator<RemoteFileItem>() {
                @Override
                public int compare(RemoteFileItem o1, RemoteFileItem o2) {
                    return o1.getUrn().compareTo(o2.getUrn());
                }
            });
        }
        
        @Override
        public EventList<RemoteFileItem> getModel() {
            return threadSafeUniqueList;
        }

        @Override
        public EventList<RemoteFileItem> getSwingModel() {
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
        
        ListEventPublisher getPublisher() {
            return compositeList.getPublisher();
        }
        
        void removeMemberList(FriendLibrary friendLibrary) {
            compositeList.removeMemberList(friendLibrary.getModel());
        }

        void addMemberList(FriendLibrary friendLibrary) {
            compositeList.addMemberList(friendLibrary.getModel());
        }        
    }

    private static class FriendLibraryImpl implements FriendLibrary {
        private final Friend friend;

        private final ObservableElementList<PresenceLibrary> allPresenceLibraries;
        private final ReadOnlyList<PresenceLibrary> readOnlyPresenceLibraries;
        
        private final CompositeList<RemoteFileItem> compositeList;
        private final ReadOnlyList<RemoteFileItem> readOnlyList;
        private final UniqueList<RemoteFileItem> threadSafeUniqueList;
        private volatile TransformedList<RemoteFileItem, RemoteFileItem> swingList;
        
        private final ReadWriteLock lock;
        
        private final PropertyChangeSupport changeSupport;
        private volatile LibraryState state = LibraryState.LOADING;

        public FriendLibraryImpl(AllFriendsLibraryImpl allFriendsList, Friend friend, ReadWriteLock lock) {
            this.friend = friend;
            this.lock = lock;
            compositeList = new CompositeList<RemoteFileItem>(allFriendsList.getPublisher(), lock);
            readOnlyList = GlazedListsFactory.readOnlyList(compositeList);
            threadSafeUniqueList = GlazedListsFactory.uniqueList(GlazedListsFactory.threadSafeList(readOnlyList),
                    COMPARATOR);
            
            changeSupport = new PropertyChangeSupport(this);
            
            Connector<PresenceLibrary> connector = GlazedLists.beanConnector(PresenceLibrary.class);            
            allPresenceLibraries = GlazedListsFactory.observableElementList(GlazedListsFactory.threadSafeList(new BasicEventList<PresenceLibrary>(lock)), connector);
            readOnlyPresenceLibraries = GlazedListsFactory.readOnlyList(allPresenceLibraries);
            allPresenceLibraries.addListEventListener(new ListEventListener<PresenceLibrary>() {
                @Override
                public void listChanged(ListEvent<PresenceLibrary> listChanges) {
                    LibraryState oldState = state;
                    state = calculateState();
                    changeSupport.firePropertyChange("state", oldState, state);
                }
            });
        }
        
        @Override
        public EventList<PresenceLibrary> getPresenceLibraryList() {
            return readOnlyPresenceLibraries;
        }
        
        @Override
        public LibraryState getState() {
            return state;
        }
        
        private LibraryState calculateState() {
            lock.readLock().lock();
            try {
                boolean oneCompleted = false;
                for(PresenceLibrary library : allPresenceLibraries) {
                    switch (library.getState()) {
                    case LOADING:
                        return LibraryState.LOADING;
                    case LOADED:
                        oneCompleted = true;
                        break;

                    }
                }
                if(oneCompleted) {
                    return LibraryState.LOADED;
                } else {
                    return LibraryState.FAILED_TO_LOAD;
                }
            } finally {
                lock.readLock().unlock();
            }
        }

        public Friend getFriend() {
            return friend;
        }
        
        private PresenceLibraryImpl findPresenceLibrary(FriendPresence presence) {
            for(PresenceLibrary library : allPresenceLibraries) {
                if(library.getPresence().getPresenceId().equals(presence.getPresenceId())) {
                    return (PresenceLibraryImpl)library;
                }
            }
            return null;
        }

        private PresenceLibraryImpl getOrCreatePresenceLibrary(FriendPresence presence) {
            PresenceLibraryImpl library = findPresenceLibrary(presence);
            if(library == null) {
                library = new PresenceLibraryImpl(presence, createMemberList());
                allPresenceLibraries.add(library);
                addMemberList(library);
                library.commit();
            }            
            return library;
        }

        private void removePresenceLibrary(FriendPresence presence) {
            PresenceLibraryImpl presenceLibrary = findPresenceLibrary(presence);
            if(presenceLibrary != null) {
                allPresenceLibraries.remove(presenceLibrary);
                presenceLibrary.dispose();
                removeMemberList(presenceLibrary);
            }
        }

        private void removeMemberList(PresenceLibrary presenceLibrary) {
            compositeList.removeMemberList(presenceLibrary.getModel());
        }

        private EventList<RemoteFileItem> createMemberList() {
            return compositeList.createMemberList();
        }

        private void addMemberList(PresenceLibrary presenceLibrary) {
            compositeList.addMemberList(presenceLibrary.getModel());
        }

        @Override
        public EventList<RemoteFileItem> getModel() {
            return threadSafeUniqueList;
        }

        @Override
        public EventList<RemoteFileItem> getSwingModel() {
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


        public void addFile(RemoteFileItem file) {
            throw new UnsupportedOperationException();
        }

        public void removeFile(RemoteFileItem file) {
            throw new UnsupportedOperationException();
        }

        void commit() {
        }

        void dispose() {
            if(swingList != null) {
                swingList.dispose();
            }
            compositeList.dispose();
            readOnlyList.dispose();
            threadSafeUniqueList.dispose();
            readOnlyPresenceLibraries.dispose();
            allPresenceLibraries.dispose();
        }
        
        public void addPropertyChangeListener(PropertyChangeListener listener) {
            changeSupport.addPropertyChangeListener(listener);
        }
        
        public void removePropertyChangeListener(PropertyChangeListener listener) {
            changeSupport.removePropertyChangeListener(listener);
        }
        
        @Override
        public String toString() {
            return StringUtils.toString(this);
        }
        
        //TODO: add new accessors appropriate for creating FileItems based on
        //      lookups. May also need to subclass CoreFileItem appropriate for
        //      friend library info.
    }

    private static class PresenceLibraryImpl implements PresenceLibrary {
        protected final TransformedList<RemoteFileItem, RemoteFileItem> eventList;
        protected volatile TransformedList<RemoteFileItem, RemoteFileItem> swingEventList;
        private final FriendPresence presence;
        private volatile LibraryState state = LibraryState.LOADING;
        
        private final PropertyChangeSupport changeSupport;

        PresenceLibraryImpl(FriendPresence presence, EventList<RemoteFileItem> list) {
            this.presence = presence;
            eventList = GlazedListsFactory.threadSafeList(list);
            changeSupport = new PropertyChangeSupport(this);
        }

        @Override
        public String toString() {
            return StringUtils.toString(this, presence);
        }
        
        public FriendPresence getPresence() {
            return presence;
        }

        @Override
        public EventList<RemoteFileItem> getModel() {
            return eventList;
        }

        @Override
        public EventList<RemoteFileItem> getSwingModel() {
            assert EventQueue.isDispatchThread();
            if(swingEventList == null) {
                swingEventList =  GlazedListsFactory.swingThreadProxyEventList(eventList);
            }
            return swingEventList;
        }

        void dispose() {
            //eventList.clear();
            if(swingEventList != null) {
                swingEventList.dispose();
            }
            eventList.dispose();
        }

        public void addFile(RemoteFileItem file) {
            eventList.add(file);
        }

        public void removeFile(RemoteFileItem file) {
            eventList.remove(file);
        }

        @Override
        public int size() {
            return eventList.size();
        }

        void commit() {
            // Add things here after we guarantee we want to use this list.
        }

        public void clear() {
        }
        
        @Override
        public LibraryState getState() {
            return state;
        }
        
        @Override
        public void setState(LibraryState newState) {
            LibraryState oldState = state;
            this.state = newState;
            changeSupport.firePropertyChange("state", oldState, newState);
        }
        
        public void addPropertyChangeListener(PropertyChangeListener listener) {
            changeSupport.addPropertyChangeListener(listener);
        }
        
        public void removePropertyChangeListener(PropertyChangeListener listener) {
            changeSupport.removePropertyChangeListener(listener);
        }
    }
    
    private static class RemoteFileComparator implements Comparator<RemoteFileItem> {
        @Override
            public int compare(RemoteFileItem o1, RemoteFileItem o2) {
            return o1.getUrn().compareTo(o2.getUrn());
        }
    }
}
