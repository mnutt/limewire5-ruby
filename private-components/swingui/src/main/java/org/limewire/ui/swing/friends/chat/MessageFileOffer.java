package org.limewire.ui.swing.friends.chat;

import org.limewire.xmpp.api.client.FileMetaData;
import org.limewire.core.api.download.DownloadState;

/**
 * chat message with a file offer
 */
public interface MessageFileOffer extends Message {

    FileMetaData getFileOffer();

    void setDownloadState(DownloadState downloadState);
}
