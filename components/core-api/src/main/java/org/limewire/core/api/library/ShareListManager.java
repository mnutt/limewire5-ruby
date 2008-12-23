package org.limewire.core.api.library;

import org.limewire.core.api.friend.Friend;

/**
 * Manager for all share lists.
 */
public interface ShareListManager {
    
    /**
     * Returns a {@link FileList} for all shared files.
     * This is a combined list of all shared files.
     */
    FileList<LocalFileItem> getCombinedShareList();

    /** Returns a {@link LocalFileList} for all files shared with Gnutella. */
    GnutellaFileList getGnutellaShareList();

    /** Gets a {@link LocalFileList} for the given friend, creating one if it doesn't exist.  */
    FriendFileList getOrCreateFriendShareList(Friend friend);
    
    /** Returns a {@link LocalFileList} for the given friend, returns null if none exist. */
    FriendFileList getFriendShareList(Friend friend);
}
