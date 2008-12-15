package org.limewire.core.api.library;


public interface RemoteFileList extends FileList<RemoteFileItem> {
        
    public void addFile(RemoteFileItem file);
    
    public void removeFile(RemoteFileItem file);
}
