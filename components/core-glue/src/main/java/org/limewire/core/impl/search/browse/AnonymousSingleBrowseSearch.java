package org.limewire.core.impl.search.browse;

import java.util.concurrent.CopyOnWriteArrayList;

import org.limewire.core.api.browse.Browse;
import org.limewire.core.api.browse.BrowseFactory;
import org.limewire.core.api.browse.BrowseListener;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.browse.BrowseStatus;
import org.limewire.core.api.search.browse.BrowseStatusListener;
import org.limewire.core.api.search.browse.BrowseStatus.BrowseState;
import org.limewire.friend.api.FriendPresence;
import org.limewire.io.GUID;

class AnonymousSingleBrowseSearch extends AbstractBrowseSearch {
   
    private final FriendPresence friendPresence;
    private final BrowseFactory browseFactory;

    private Browse browse;

    /**
     * @param friendPresence the person to be browsed - must be anonymous and can not be null; 
     */
    public AnonymousSingleBrowseSearch(BrowseFactory browseFactory, FriendPresence friendPresence) {
        assert(friendPresence != null && friendPresence.getFriend().isAnonymous());
        this.friendPresence = friendPresence;
        this.browseFactory = browseFactory;
    }
   

    @Override
    public void start() {
        for (SearchListener listener : searchListeners) {
            listener.searchStarted(AnonymousSingleBrowseSearch.this);
        }
        browse = browseFactory.createBrowse(friendPresence);
        browse.start(new BrowseEventForwarder());
    }

    @Override
    public void stop() {
        // let the stopping of the browse trigger the events.
        assert (browse != null);
        browse.stop();
    }


    /**
     * Forwards browse information to searchListeners.
     */
    private class BrowseEventForwarder implements BrowseListener {

        @Override
        public void browseFinished(final boolean success) {
            BrowseStatus status = success? 
                    new BrowseStatus(AnonymousSingleBrowseSearch.this, BrowseState.LOADED): 
                    new BrowseStatus(AnonymousSingleBrowseSearch.this, BrowseState.FAILED, friendPresence.getFriend());
            
            for (SearchListener listener : searchListeners) {
                listener.searchStopped(AnonymousSingleBrowseSearch.this);
            }
           
            for (BrowseStatusListener listener : browseStatusListeners) {
                listener.statusChanged(status);
            }
        }

        @Override
        public void handleBrowseResult(final SearchResult searchResult) {              
            for (SearchListener listener : searchListeners) {
                listener.handleSearchResult(AnonymousSingleBrowseSearch.this, searchResult);
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
