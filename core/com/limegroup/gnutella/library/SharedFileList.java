package com.limegroup.gnutella.library;

import java.io.File;
import java.util.List;

import org.limewire.concurrent.ListeningFuture;

import com.limegroup.gnutella.URN;

public interface SharedFileList extends FileList {
    
    /**
     * Returns the <tt>FileDesc</tt> for the specified URN. This only returns
     * one <tt>FileDesc</tt>, even though multiple indices are possible.
     * 
     * @param urn the urn for the file
     * @return the <tt>FileDesc</tt> corresponding to the requested urn, or
     *         <tt>null</tt> if no matching <tt>FileDesc</tt> could be found
     */
    FileDesc getFileDesc(URN urn);
    
    /**
     * Adds this directory to the ManagedFileList and adds all 
     * manageable files in the given folder to this list.
     * 
     * This has the effect of adding recursively adding all
     * manageable files from this folder as managed files, and
     * adding the files in the folder (non-recursively) to 
     * this list.
     */
    @Override
    public ListeningFuture<List<ListeningFuture<FileDesc>>> addFolder(File folder);

}
