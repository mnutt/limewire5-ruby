package org.limewire.core.impl.friend;

import org.limewire.core.api.download.DownloadException;
import org.limewire.core.api.friend.FileMetaDataConverter;
import org.limewire.core.api.search.SearchResult;
import org.limewire.friend.api.FileMetaData;
import org.limewire.friend.api.FriendPresence;
import org.limewire.io.InvalidDataException;

public class MockFileMetaDataConverter implements FileMetaDataConverter {

    @Override
    public SearchResult create(FriendPresence presence, FileMetaData fileMetaData)
            throws InvalidDataException, DownloadException {
        // TODO Auto-generated method stub
        return null;
    }

}
