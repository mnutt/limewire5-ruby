package com.limegroup.gnutella.library;

import java.util.Iterator;
import java.util.NoSuchElementException;

class ThreadSafeFileListIterator implements Iterator<FileDesc> {
    
    private final AbstractFileList fileList;
    private final ManagedFileListImpl managedList;
    private final int startRevision;
    
    /** Points to the index that is to be examined next. */
    private int index = 0;
    private FileDesc preview;
    
    public ThreadSafeFileListIterator(AbstractFileList fileList, ManagedFileListImpl managedList) {
        this.fileList = fileList;
        this.index = fileList.getMinIndex();
        this.managedList = managedList;
        this.startRevision = managedList.revision();
    }
    
    private boolean preview() {
        assert preview == null;
        
        if (managedList.revision() != startRevision) {
            return false;
        }

        fileList.getReadLock().lock();
        try {
            while (index <= fileList.getMaxIndex()) {
                preview = fileList.getFileDescForIndex(index);
                index++;
                if (preview != null) {
                    return true;
                }
            }            
            return false;
        } finally {
            fileList.getReadLock().unlock();
        }
        
    }
    
    @Override
    public boolean hasNext() {
        if (managedList.revision() != startRevision) {
            return false;
        }

        if (preview != null) {
            if (!fileList.contains(preview)) {
                // file was removed in the meantime
                preview = null;
            }
        }
        return preview != null || preview();
    }
    
    @Override
    public FileDesc next() {
        if (hasNext()) {
            FileDesc item = preview;
            preview = null;
            return item;
        }
        throw new NoSuchElementException();     
    }
    
    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

}
