package org.limewire.core.api.friend.feature.features;

import org.limewire.xmpp.api.client.FileMetaData;

public interface FileOfferer {
    void offerFile(FileMetaData fileMetaData);
}
