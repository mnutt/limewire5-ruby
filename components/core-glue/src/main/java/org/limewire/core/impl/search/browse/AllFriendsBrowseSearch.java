package org.limewire.core.impl.search.browse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;

import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.browse.BrowseStatus;
import org.limewire.core.api.search.browse.BrowseStatusListener;
import org.limewire.core.api.search.browse.BrowseStatus.BrowseState;
import org.limewire.io.GUID;

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

class AllFriendsBrowseSearch extends AbstractBrowseSearch {
    
    private final RemoteLibraryManager remoteLibraryManager;
    private final ExecutorService backgroundExecutor;
    private final ListEventListener<SearchResult> listEventListener = new AllFriendsListEventListener<SearchResult>();

  
    public AllFriendsBrowseSearch(RemoteLibraryManager remoteLibraryManager, ExecutorService backgroundExecutor) {
        this.remoteLibraryManager = remoteLibraryManager;
        this.backgroundExecutor = backgroundExecutor;
    }


    @Override
    public void start() {
        backgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                for (SearchListener listener : searchListeners) {
                    listener.searchStarted(AllFriendsBrowseSearch.this);
                }
                
                installListener();
                loadSnapshot();
            }
        });
    }

    @Override
    public void stop() {
        for (SearchListener listener : searchListeners) {
            listener.searchStopped(AllFriendsBrowseSearch.this);
        }    
        
        removeListener();
    }


    private void loadSnapshot() {
        List<SearchResult> remoteFileItems = new ArrayList<SearchResult>();
        
        remoteLibraryManager.getAllFriendsFileList().getModel().getReadWriteLock().readLock().lock();
        try {        
            remoteFileItems.addAll(remoteLibraryManager.getAllFriendsFileList().getModel());
        } finally {
            remoteLibraryManager.getAllFriendsFileList().getModel().getReadWriteLock().readLock().unlock();
        }
        
        //add all files
        for (SearchListener listener : searchListeners) {
            listener.handleSearchResults(this, remoteFileItems);
        }

        
        //browse is finished
        for (SearchListener listener : searchListeners) {
            listener.searchStopped(AllFriendsBrowseSearch.this);
        }
        
        BrowseStatus status = (remoteFileItems.size() > 0) ? new BrowseStatus(AllFriendsBrowseSearch.this, BrowseState.LOADED) :
            new BrowseStatus(AllFriendsBrowseSearch.this, BrowseState.NO_FRIENDS_SHARING);
        for (BrowseStatusListener listener : browseStatusListeners){
            listener.statusChanged(status);
        }

    }
    
    private void installListener(){
        remoteLibraryManager.getAllFriendsFileList().getModel().addListEventListener(listEventListener);
    }
    
    private void removeListener(){
        remoteLibraryManager.getAllFriendsFileList().getModel().removeListEventListener(listEventListener);        
    }
    
    private class AllFriendsListEventListener<E> implements ListEventListener<E> {
        @Override
        public void listChanged(ListEvent listChanges) {
            BrowseStatus status = new BrowseStatus(AllFriendsBrowseSearch.this, BrowseState.UPDATED);
            for (BrowseStatusListener listener : browseStatusListeners){
                listener.statusChanged(status);
            }
        }        
    }

    @Override
    public void repeat() {
        loadSnapshot();
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
