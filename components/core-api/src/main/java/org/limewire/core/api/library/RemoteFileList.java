package org.limewire.core.api.library;

import java.util.Collection;

/** A list of files from a remote host */
public interface RemoteFileList extends FileList<RemoteFileItem> {
        
    /** Adds a new file into the list. */
    public void addFile(RemoteFileItem file);
    
    /** Removes an existing file from the list. */
    public void removeFile(RemoteFileItem file);
    
    /** Sets all files in the list to be this collection of files. */
    public void setNewFiles(Collection<RemoteFileItem> files);
}
