package org.limewire.core.impl.library;

import java.util.Map;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.MetaDataException;
import org.limewire.core.api.library.MetaDataManager;

public class MockMetaDataManager implements MetaDataManager {

    @Override
    public void save(LocalFileItem localFileItem, Map<FilePropertyKey, Object> newData)
            throws MetaDataException {
       
    }

}
