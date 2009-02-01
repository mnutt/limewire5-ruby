package org.limewire.core.api.library;

public interface MetaDataManager {
    /**
     * Persists changes to the provided local file items metadata. If there are
     * errors saving the changes, the object is reloaded with the currently
     * persisted values.
     * @throws MetaDataException if there are any problems saving the meta-data 
     */
    void save(LocalFileItem localFileItem) throws MetaDataException;
}
