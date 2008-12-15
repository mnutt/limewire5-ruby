package org.limewire.core.api.xmpp;

import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.xmpp.api.client.FileMetaData;
import org.limewire.io.InvalidDataException;

public interface RemoteFileItemFactory {
    RemoteFileItem create(FriendPresence presence, FileMetaData fileMetaData) throws InvalidDataException, SaveLocationException;
}
