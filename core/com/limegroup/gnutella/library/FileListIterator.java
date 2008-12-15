package com.limegroup.gnutella.library;

import java.util.Iterator;

import org.limewire.collection.IntSet;
import org.limewire.collection.IntSet.IntSetIterator;

class FileListIterator implements Iterator<FileDesc> {
    
    private final FileList fileList;
    private final IntSetIterator iter;
    
    public FileListIterator(FileList fileList, IntSet intSet) {
        this.fileList = fileList;
        this.iter = intSet.iterator();
    }

    @Override
    public boolean hasNext() {
        return iter.hasNext();
    }

    @Override
    public FileDesc next() {
        FileDesc fd = fileList.getFileDescForIndex(iter.next());
        assert fd != null : "FD must be non-null, using out of lock maybe?";
        return fd;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

}
