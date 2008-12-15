package org.limewire.core.impl.library;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.library.FileList;
import org.limewire.core.api.library.FriendLibrary;
import org.limewire.core.api.library.PresenceLibrary;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.RemoteLibraryManager;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;

public class MockRemoteLibraryManager implements RemoteLibraryManager {
    
    @Override
    public FileList<RemoteFileItem> getAllFriendsFileList() {
        return new RemoteFileListAdapter();
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
    public PresenceLibrary addPresenceLibrary(FriendPresence presence) {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public void removeFriendLibrary(Friend friend) {
        // TODO Auto-generated method stub
        
    }
    @Override
    public void removePresenceLibrary(FriendPresence presence) {
        // TODO Auto-generated method stub
        
    }
    
}
