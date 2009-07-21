package org.limewire.core.impl.search.browse;

import java.util.Collection;

import org.limewire.core.api.search.browse.BrowseSearch;
import org.limewire.core.api.search.browse.BrowseSearchFactory;
import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendPresence;

class MockBrowseSearchFactory implements BrowseSearchFactory {

    @Override
    public BrowseSearch createAllFriendsBrowseSearch() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BrowseSearch createBrowseSearch(FriendPresence person) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BrowseSearch createBrowseSearch(Collection<FriendPresence> people) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BrowseSearch createFriendBrowseSearch(Friend friend) {
        // TODO Auto-generated method stub
        return null;
    }

}
