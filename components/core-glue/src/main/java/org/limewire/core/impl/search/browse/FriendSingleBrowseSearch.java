package org.limewire.core.impl.search.browse;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import org.limewire.core.api.library.FriendLibrary;
import org.limewire.core.api.library.LibraryState;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.browse.BrowseStatus;
import org.limewire.core.api.search.browse.BrowseStatusListener;
import org.limewire.core.api.search.browse.BrowseStatus.BrowseState;
import org.limewire.friend.api.Friend;
import org.limewire.io.GUID;

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

class FriendSingleBrowseSearch extends AbstractBrowseSearch {

    private final Friend friend;
    private final RemoteLibraryManager remoteLibraryManager;
    private final ExecutorService executorService;
    private final ListEventListener<FriendLibrary> friendLibraryListEventListener = new FriendLibraryListEventListener();
    private final FriendLibraryModelListener friendLibraryModelListener = new FriendLibraryModelListener();
    private final PropertyChangeListener libraryPropertyChangeListener = new LibraryPropertyChangeListener();
    
    private final AtomicReference<FriendLibrary> currentLibrary = new AtomicReference<FriendLibrary>();

    /**
     * @param friend the person to be browsed - can not be anonymous or null
     */
    public FriendSingleBrowseSearch(RemoteLibraryManager remoteLibraryManager, Friend friend, ExecutorService executorService) {
        assert(friend != null && !friend.isAnonymous());
        this.friend = friend;
        this.remoteLibraryManager = remoteLibraryManager;
        this.executorService = executorService;
    }


    @Override
    public void start() {
        executorService.execute(new Runnable() {
            public void run() {
                for (SearchListener listener : searchListeners) {
                    listener.searchStarted(FriendSingleBrowseSearch.this);
                }
                
                installListener();
                startFriendBrowse();
            }
        });
    }

    @Override
    public void stop() {
        for (SearchListener listener : searchListeners) {
            listener.searchStopped(FriendSingleBrowseSearch.this);
        }       
        removeListener();
    }


    private void startFriendBrowse() {
        FriendLibrary library = remoteLibraryManager.getFriendLibrary(friend);
        
        if (library == null) {
            // Failed!
           fireBrowseStatusChanged(BrowseState.OFFLINE, friend);
           for (SearchListener listener : searchListeners) {
               listener.searchStopped(FriendSingleBrowseSearch.this);
           }  
            
        } else {
            setLibrary(library);
            if(library.getState() == LibraryState.LOADING){
                library.addPropertyChangeListener(libraryPropertyChangeListener);                
            } else {
                loadLibrary();
            }
        }
 
    }
    
    /**Loads a snapshot of the available files, alerts BrowseStatusListeners that we have loaded, 
     * and SearchListeners that the search has stopped.*/
    private void loadLibrary(){
        List<SearchResult> remoteFileItems = new ArrayList<SearchResult>();
        
        remoteLibraryManager.getFriendLibrary(friend).getModel().getReadWriteLock().readLock().lock();
        try {        
            remoteFileItems.addAll(remoteLibraryManager.getFriendLibrary(friend).getModel());
        } finally {
            remoteLibraryManager.getFriendLibrary(friend).getModel().getReadWriteLock().readLock().unlock();
        }
        
        // add all files
        for (SearchListener listener : searchListeners) {
            listener.handleSearchResults(this, remoteFileItems);
        }
        
        fireBrowseStatusChanged(BrowseState.LOADED);
        
        for (SearchListener listener : searchListeners) {
            listener.searchStopped(FriendSingleBrowseSearch.this);
        } 
    }
    
    /**Adds friendLibraryListEventListener to the FriendLibraryList*/
    private void installListener(){
        remoteLibraryManager.getFriendLibraryList().addListEventListener(friendLibraryListEventListener);
    }
    
    /**Removes friendLibraryListEventListener from the FriendLibraryList.  
     * Removes libraryPropertyChangeLister from the friend library if necessary.*/
    private void removeListener(){
        remoteLibraryManager.getFriendLibraryList().removeListEventListener(friendLibraryListEventListener);  
        if (currentLibrary.get() != null) {
            currentLibrary.get().removePropertyChangeListener(libraryPropertyChangeListener);
            currentLibrary.get().getModel().removeListEventListener(friendLibraryModelListener);
            currentLibrary.set(null);
        }
    }
    
    private synchronized void setLibrary(FriendLibrary newLibrary){
        FriendLibrary oldLibrary = currentLibrary.get();
        if(newLibrary == oldLibrary){
            return;
        }
        if(oldLibrary != null){
            oldLibrary.getModel().removeListEventListener(friendLibraryModelListener);            
        }
        
        //Add a property change listener to the new library and keep a reference to the library so we can remove the listener later.
        newLibrary.getModel().addListEventListener(friendLibraryModelListener);
        newLibrary.addPropertyChangeListener(libraryPropertyChangeListener);
        currentLibrary.set(newLibrary);
        
        if(currentLibrary.get().getState() == LibraryState.LOADED){
            fireBrowseStatusChanged(BrowseState.UPDATED);
        }
        
    }
    
    private void fireBrowseStatusChanged(BrowseState state, Friend... friends){
        BrowseStatus status = new BrowseStatus(FriendSingleBrowseSearch.this, state, friends);
        for (BrowseStatusListener listener : browseStatusListeners) {
            listener.statusChanged(status);
        } 
    }

    private class FriendLibraryListEventListener implements ListEventListener<FriendLibrary> {
        @Override
        public void listChanged(ListEvent listChanges) {
            while (listChanges.next()) {
                if (listChanges.getType() == ListEvent.INSERT) {
                    FriendLibrary newLibrary = (FriendLibrary) listChanges.getSourceList().get(listChanges.getIndex());
                    if (newLibrary.getFriend().getId().equals(friend.getId())) {//There is a new library for our friend!
                        setLibrary(remoteLibraryManager.getFriendLibrary(friend));
                    }
                } else if (listChanges.getType() == ListEvent.DELETE && remoteLibraryManager.getFriendLibrary(friend) == null){   
                    //our friend has logged off
                    currentLibrary.set(null);
                    fireBrowseStatusChanged(BrowseState.OFFLINE, friend);
                }
            }
        }
    }
    
    private class FriendLibraryModelListener implements ListEventListener<SearchResult>{
        @Override
        public void listChanged(ListEvent<SearchResult> listChanges) {
            fireBrowseStatusChanged(BrowseState.UPDATED);
        }        
    }
    
    private class LibraryPropertyChangeListener implements PropertyChangeListener {
        
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            LibraryState state = (LibraryState)evt.getNewValue();
            if (state != LibraryState.LOADING) {
                // The list has changed - tell the listeners
                if (state == LibraryState.LOADED) {
                    fireBrowseStatusChanged(BrowseState.UPDATED);
                } else {
                    fireBrowseStatusChanged(BrowseState.FAILED, friend);
                }

            }
        }
    }

    @Override
    public void repeat() {
        stop();
        start();
    }


    @Override
    public CopyOnWriteArrayList<SearchListener> getListenerList() {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public String getQuery() {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public GUID getQueryGuid() {
        // TODO Auto-generated method stub
        return null;
    }
}
