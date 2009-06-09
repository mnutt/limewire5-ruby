package org.limewire.ui.swing.friends.chat;

import org.limewire.core.api.download.DownloadState;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.client.FileMetaData;

/**
 * Chat message with a file offer.  To perform a file offer
 * the file metadata (info describing a file)  and the address
 * (describing how to locate the file) are necessary.
 */
public interface MessageFileOffer extends Message {

    /**
     * @return file meta data
     */
    FileMetaData getFileOffer();

    void setDownloadState(DownloadState downloadState);

    /**
     * @return the {@link FriendPresence} which sent the fiole offer
     */
    FriendPresence getPresence();
}
