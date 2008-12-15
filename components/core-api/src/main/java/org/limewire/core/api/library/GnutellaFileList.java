package org.limewire.core.api.library;


public interface GnutellaFileList extends FriendFileList {
    /**
     * This method will validate that every file in the list should still be there.
     * If files are in the list that shouldn't. They will be removed.
     */
    void removeDocuments();
}