package org.limewire.core.api.library;

import java.io.File;
import java.util.List;

import org.limewire.concurrent.ListeningFuture;
import org.limewire.core.api.URN;

import com.limegroup.gnutella.library.FileDesc;

/** A list of FileItems that are locally stored on disk. */
public interface LocalFileList extends FileList<LocalFileItem> {

    /**
     * Adds the given file to the list.
     * 
     * Returns a {@link ListeningFuture} that will notify
     * when a LocalFileItem has been created out of this file.
     */
    ListeningFuture<LocalFileItem> addFile(File file);

    /** Removes the given file from the list. */
    void removeFile(File file);
    
    /**
     * Adds all files in the folder to the list.
     * 
     * Returns a {@link ListeningFuture} that will notify
     * when a List of potential {@link LocalFileItem LocalFileItems}
     * have been created from this folder.
     */
    ListeningFuture<List<ListeningFuture<LocalFileItem>>> addFolder(File folder);
    
    /** Returns true if the list contains this file. */
    boolean contains(File file);

    /** Returns true if the list contains a file with this URN. */
    boolean contains(URN urn);
    
    /** Finds a file by URN */
    public List<FileDesc> getFileDescsByURN(URN urn);
    
    /**
     * Returns the of FileItem for the given file, or null if it is not in this list.
     * This may return null if the library has not finished loading.
     */
    LocalFileItem getFileItem(File file);

}
