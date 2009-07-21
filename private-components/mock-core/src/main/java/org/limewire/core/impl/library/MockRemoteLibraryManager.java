package org.limewire.core.impl.library;

import org.limewire.core.api.library.FriendLibrary;
import org.limewire.core.api.library.PresenceLibrary;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.library.SearchResultList;
import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendPresence;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;

public class MockRemoteLibraryManager implements RemoteLibraryManager {
    
    @Override
    public PresenceLibrary getPresenceLibrary(FriendPresence presence) {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public SearchResultList getAllFriendsFileList() {
        return new SearchResultListAdapter();
    }

    @Override
    public boolean hasFriendLibrary(Friend friend) {
        // TODO Auto-generated method stub
        return false;
    }
    
    @Override
    public EventList<FriendLibrary> getFriendLibraryList() {
        return new BasicEventList<FriendLibrary>();
    }
    
    @Override
    public EventList<FriendLibrary> getSwingFriendLibraryList() {
        return new BasicEventList<FriendLibrary>();
    }
    
    @Override
    public boolean addPresenceLibrary(FriendPresence presence) {
        return false;
    }
    
    @Override
    public void removeFriendLibrary(Friend friend) {
        // TODO Auto-generated method stub
        
    }
    @Override
    public void removePresenceLibrary(FriendPresence presence) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public FriendLibrary getFriendLibrary(Friend friend) {
        // TODO Auto-generated method stub
        return null;
    }    
}
