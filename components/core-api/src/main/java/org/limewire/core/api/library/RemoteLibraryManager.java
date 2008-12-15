package org.limewire.core.api.library;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendPresence;

import ca.odell.glazedlists.EventList;

public interface RemoteLibraryManager {
    
    /**
     * Adds a new presence to the list of remote libraries.
     * If a presence with the same ID already exists, this
     * returns the preexisting library.  If the presence
     * is the first with that particular friend, a FriendLibrary
     * is created.
     */
    PresenceLibrary addPresenceLibrary(FriendPresence presence);
    
    /**
     * Removes a presence from the list of presence libraries
     * for the given friend.  If this is the last live presence
     * for the given friend, the friend is removed from the list
     * of friend libraries.
     */
    void removePresenceLibrary(FriendPresence presence);
    
    /**
     * Removes the given the entire FriendLibrary for the given Friend,
     * including all contained PresenceLibraries.
     */
    void removeFriendLibrary(Friend friend);
    
    /**
     * Returns an {@link EventList} composed of {@link FriendLibrary FriendLibraries}.
     */
    EventList<FriendLibrary> getFriendLibraryList();
    
    /**
     * Returns true if a {@link FriendLibrary} exists for the given friend.
     */
    boolean hasFriendLibrary(Friend friend);

    /** A list of all friend's libraries suitable for use in Swing. */
    EventList<FriendLibrary> getSwingFriendLibraryList();
    
    /** Returns a FileList that is a concatenation of all friends libraries. */
    FileList<RemoteFileItem> getAllFriendsFileList();
    
    
}
