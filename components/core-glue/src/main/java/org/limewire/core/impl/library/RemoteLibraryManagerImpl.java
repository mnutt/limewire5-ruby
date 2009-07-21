package org.limewire.core.impl.library;

import java.awt.EventQueue;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.library.FriendLibrary;
import org.limewire.core.api.library.LibraryState;
import org.limewire.core.api.library.PresenceLibrary;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.library.SearchResultList;
import org.limewire.core.api.search.SearchResult;
import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendPresence;
import org.limewire.inspection.Inspectable;
import org.limewire.inspection.InspectableContainer;
import org.limewire.inspection.InspectionPoint;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.StringUtils;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.CompositeList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.TransformedList;
import ca.odell.glazedlists.ObservableElementList.Connector;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventAssembler;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.event.ListEventPublisher;
import ca.odell.glazedlists.impl.ReadOnlyList;
import ca.odell.glazedlists.util.concurrent.LockFactory;
import ca.odell.glazedlists.util.concurrent.ReadWriteLock;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * This class keeps track of all friends libraries. As friend presences are found they are 
 * aggregated into a single friend library per friend. All RemoteFileItems found in the friend libraries
 * are also coalesced into a single FileList.
 */
@Singleton
public class RemoteLibraryManagerImpl implements RemoteLibraryManager {
    
    private static final Log LOG = LogFactory.getLog(RemoteLibraryManagerImpl.class, "friend-library");
    
    private final AllFriendsLibraryImpl allFriendsList;
    private final EventList<FriendLibrary> allFriendLibraries;
    private final EventList<FriendLibrary> readOnlyFriendLibraries;
    private volatile EventList<FriendLibrary> swingFriendLibraries;
    private final ReadWriteLock lock;

    @SuppressWarnings("unused")
    @InspectableContainer
    private class LazyInspectableContainer {
        @InspectionPoint("remote libraries")
        private final Inspectable inspectable = new Inspectable() {
            @Override
            public Object inspect() {
                Map<String, Object> data = new HashMap<String, Object>();
                readOnlyFriendLibraries.getReadWriteLock().readLock().lock();
                try {
                    List<Integer> sizes = new ArrayList<Integer>(readOnlyFriendLibraries.size());
                    for (FriendLibrary friendLibrary : readOnlyFriendLibraries) {
                        sizes.add(friendLibrary.size());
                    }
                    data.put("sizes", sizes);
                } finally {
                    readOnlyFriendLibraries.getReadWriteLock().readLock().unlock();
                }
                return data;
            }
        };
    }
    
    @Inject
    public RemoteLibraryManagerImpl() {
        Connector<FriendLibrary> connector = GlazedLists.beanConnector(FriendLibrary.class);
        lock = LockFactory.DEFAULT.createReadWriteLock();
        allFriendLibraries = GlazedListsFactory.observableElementList(GlazedListsFactory.threadSafeList(new BasicEventList<FriendLibrary>(lock)), connector);
        readOnlyFriendLibraries = GlazedListsFactory.readOnlyList(allFriendLibraries);
        allFriendsList = new AllFriendsLibraryImpl(lock);
    }
    
    @Override
    public SearchResultList getAllFriendsFileList() {
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
    public boolean addPresenceLibrary(FriendPresence presence) {
        assert !presence.getFriend().isAnonymous();
        
        lock.writeLock().lock();
        try {
            FriendLibraryImpl friendLibrary = getOrCreateFriendLibrary(presence.getFriend());
            return friendLibrary.addPresenceLibrary(presence);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public PresenceLibrary getPresenceLibrary(FriendPresence presence) {
        lock.readLock().lock();
        try {
            FriendLibraryImpl friendLibrary = getFriendLibrary(presence.getFriend());
            if(friendLibrary != null) {
                return friendLibrary.getPresenceLibrary(presence);
            } else {
                return null;
            }
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public void removeFriendLibrary(Friend friend) {
        lock.writeLock().lock();
        try {
            FriendLibraryImpl friendLibrary = getFriendLibraryImpl(friend);
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
            FriendLibraryImpl friendLibrary = getFriendLibraryImpl(presence.getFriend());
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
            LOG.debugf("adding friend library for {0}", friend);
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
    
    @Override
    public FriendLibraryImpl getFriendLibrary(Friend friend) {
        return findFriendLibrary(friend);
    }
    
    private FriendLibraryImpl getFriendLibraryImpl(Friend friend) {
        return findFriendLibrary(friend);
    }

    private void removeFriendLibrary(FriendLibraryImpl friendLibrary) {
        LOG.debugf("removing friend library for {0}", friendLibrary.getFriend());
        allFriendLibraries.remove(friendLibrary);
        allFriendsList.removeMemberList(friendLibrary);
        friendLibrary.dispose();
    }
    
    private static class AllFriendsLibraryImpl implements SearchResultList {
        private final CompositeList<SearchResult> compositeList;
        private final ReadOnlyList<SearchResult> readOnlyList;
        private final EventList<SearchResult> threadSafeList;
        private volatile TransformedList<SearchResult, SearchResult> swingList;
        
        public AllFriendsLibraryImpl(ReadWriteLock lock) {
            compositeList = new CompositeList<SearchResult>(ListEventAssembler.createListEventPublisher(), lock);
            readOnlyList = GlazedListsFactory.readOnlyList(compositeList);
            threadSafeList = GlazedListsFactory.threadSafeList(readOnlyList);
        }
        
        @Override
        public EventList<SearchResult> getModel() {
            return threadSafeList;
        }

        @Override
        public EventList<SearchResult> getSwingModel() {
            assert EventQueue.isDispatchThread();
            if(swingList == null) {
                swingList =  GlazedListsFactory.swingThreadProxyEventList(threadSafeList);
            }
            return swingList;
        }

        @Override
        public int size() {
            return threadSafeList.size();
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

        @Override
        public void addNewResult(SearchResult file) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeResult(SearchResult file) {
            throw new UnsupportedOperationException();   
        }

        @Override
        public void setNewResults(Collection<SearchResult> files) {
            throw new UnsupportedOperationException();   
        }
        
        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }
    }

    private static class FriendLibraryImpl implements FriendLibrary {
        private final Friend friend;

        private final ObservableElementList<PresenceLibrary> allPresenceLibraries;
        private final ReadOnlyList<PresenceLibrary> readOnlyPresenceLibraries;
        
        private final CompositeList<SearchResult> compositeList;
        private final ReadOnlyList<SearchResult> readOnlyList;
        private final EventList<SearchResult> threadSafeList;
        private volatile TransformedList<SearchResult, SearchResult> swingList;
        
        private final ReadWriteLock lock;
        
        private final PropertyChangeSupport changeSupport;
        private volatile LibraryState state = LibraryState.LOADING;

        public FriendLibraryImpl(AllFriendsLibraryImpl allFriendsList, Friend friend, ReadWriteLock lock) {
            this.friend = friend;
            this.lock = lock;
            compositeList = new CompositeList<SearchResult>(allFriendsList.getPublisher(), lock);
            readOnlyList = GlazedListsFactory.readOnlyList(compositeList);
            threadSafeList = GlazedListsFactory.threadSafeList(readOnlyList);
            
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
        
        private PresenceLibraryImpl getPresenceLibrary(FriendPresence presence) {
            for(PresenceLibrary library : allPresenceLibraries) {
                if(library.getPresence().getPresenceId().equals(presence.getPresenceId())) {
                    return (PresenceLibraryImpl)library;
                }
            }
            return null;
        }

        private boolean addPresenceLibrary(FriendPresence presence) {
            PresenceLibraryImpl library = getPresenceLibrary(presence);
            if(library == null) {
                LOG.debugf("adding presence library for {0}", presence);
                library = new PresenceLibraryImpl(presence, createMemberList());
                allPresenceLibraries.add(library);
                addMemberList(library);
                library.commit();
                return true;
            } else {
                return false;
            }
        }

        private void removePresenceLibrary(FriendPresence presence) {
            PresenceLibraryImpl presenceLibrary = getPresenceLibrary(presence);
            if(presenceLibrary != null) {
                LOG.debugf("removing presence library for {0}", presence);
                allPresenceLibraries.remove(presenceLibrary);
                presenceLibrary.dispose();
                removeMemberList(presenceLibrary);
            }
        }

        private void removeMemberList(PresenceLibrary presenceLibrary) {
            compositeList.removeMemberList(presenceLibrary.getModel());
        }

        private EventList<SearchResult> createMemberList() {
            return compositeList.createMemberList();
        }

        private void addMemberList(PresenceLibrary presenceLibrary) {
            compositeList.addMemberList(presenceLibrary.getModel());
        }

        @Override
        public EventList<SearchResult> getModel() {
            return threadSafeList;
        }

        @Override
        public EventList<SearchResult> getSwingModel() {
            assert EventQueue.isDispatchThread();
            if(swingList == null) {
                swingList =  GlazedListsFactory.swingThreadProxyEventList(threadSafeList);
            }
            return swingList;
        }

        @Override
        public int size() {
            return threadSafeList.size();
        }

        @Override
        public void addNewResult(SearchResult file) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public void removeResult(SearchResult file) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public void setNewResults(Collection<SearchResult> file) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public void clear() {
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
            threadSafeList.dispose();
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
    }

    private static class PresenceLibraryImpl implements PresenceLibrary {
        protected final EventList<SearchResult> eventList;
        protected volatile EventList<SearchResult> swingEventList;
        private final FriendPresence presence;
        private volatile LibraryState state = LibraryState.LOADING;
        
        private final PropertyChangeSupport changeSupport;

        PresenceLibraryImpl(FriendPresence presence, EventList<SearchResult> list) {
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
        public EventList<SearchResult> getModel() {
            return eventList;
        }

        @Override
        public EventList<SearchResult> getSwingModel() {
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

        @Override
        public void addNewResult(SearchResult file) {
            eventList.add(file);
        }

        @Override
        public void removeResult(SearchResult file) {
            eventList.remove(file);
        }
        
        @Override
        public void setNewResults(Collection<SearchResult> files) {
            eventList.getReadWriteLock().writeLock().lock();
            try {
                eventList.clear();
                eventList.addAll(files);
            } finally {
                eventList.getReadWriteLock().writeLock().unlock();
            }
        }
        
        @Override
        public void clear() {
            eventList.clear();
        }

        @Override
        public int size() {
            return eventList.size();
        }

        void commit() {
            // Add things here after we guarantee we want to use this list.
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
}
