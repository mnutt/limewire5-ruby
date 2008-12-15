package com.limegroup.gnutella.library;

import java.io.File;
import java.util.List;
import java.util.Set;

import org.limewire.concurrent.ListeningFuture;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.VerifyingFile;
import com.limegroup.gnutella.xml.LimeXMLDocument;


/** A collection of IncompleteFileDescs. */
class IncompleteFileListImpl extends AbstractFileList implements IncompleteFileList {
    
    private ManagedFileListImpl managedList;

    public IncompleteFileListImpl(ManagedFileListImpl managedList) {
        super(managedList);
        this.managedList = managedList;
    }

    public void addIncompleteFile(File incompleteFile, Set<? extends URN> urns, String name,
            long size, VerifyingFile vf) {
        managedList.addIncompleteFile(incompleteFile, urns, name, size, vf);
    }
    
    @Override
    public ListeningFuture<FileDesc> add(File file) {
        throw new UnsupportedOperationException("cannot add from here");
    }
    
    @Override
    public ListeningFuture<FileDesc> add(File file, List<? extends LimeXMLDocument> documents) {
        throw new UnsupportedOperationException("cannot add from here");
    }
    
    @Override
    public ListeningFuture<List<ListeningFuture<FileDesc>>> addFolder(File folder) {
        throw new UnsupportedOperationException("cannot add from here");
    }
    
    @Override
    protected boolean isFileAddable(FileDesc fileDesc) {
        return fileDesc instanceof IncompleteFileDesc;
    }

    @Override
    protected boolean isPending(File file, FileDesc fd) {
        return fd instanceof IncompleteFileDesc;
    }
    
    @Override
    protected void saveChange(File file, boolean added) {
        // Don't save incomplete status.
    }
    
    
}
