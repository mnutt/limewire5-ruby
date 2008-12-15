package com.limegroup.gnutella.library;

import java.io.File;
import java.util.List;
import java.util.concurrent.Future;

public interface GnutellaFileList extends FriendFileList {
    
    /** Adds this FileList just for this session. */
    Future<FileDesc> addForSession(File file);

    /** Returns the size of all files within this list, in <b>bytes</b>. */
    long getNumBytes();

    /** Adds the FileDesc to this list. */
    boolean add(FileDesc fileDesc);
    
    /** Returns true if any files are shared through application special share folder. */
    boolean hasApplicationSharedFiles();
    
    /** Returns true if the given filename is shared through the application share. */
    boolean isFileApplicationShare(String filename);
    
    /**
     * Returns a list of all the file descriptors in this list that exist 
     * in the given directory, in any order.
     * 
     * Returns null if directory is not shared, or a zero-length array if it is
     * shared but contains no files.  This method is not recursive; files in 
     * any of the directory's children are not returned.
     * 
     * This operation is <b>not</b> efficient, and should not be done often.
     */
    List<FileDesc> getFilesInDirectory(File directory);

    /**
     * Removes all files of type document from this list.
     */
    void removeDocuments();
}
