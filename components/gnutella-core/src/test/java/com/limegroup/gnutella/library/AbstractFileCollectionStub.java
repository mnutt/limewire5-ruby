package com.limegroup.gnutella.library;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.limewire.concurrent.ListeningFuture;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventListenerList;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.xml.LimeXMLDocument;

public abstract class AbstractFileCollectionStub implements FileCollection {

    private final Lock lock = new ReentrantLock();
    protected final List<FileDesc> fileDescList = new CopyOnWriteArrayList<FileDesc>();
    
    private final EventListenerList<FileViewChangeEvent> listeners
        = new EventListenerList<FileViewChangeEvent>();
    
    @Override
    public String getName() {
        return "Stub Collection";
    }
    
    @Override
    public long getNumBytes() {
        long size = 0;
        for(FileDesc fd : fileDescList) {
            size += fd.getFileSize();
        }
        return size;
    }
    
    public boolean add(FileDesc fileDesc) {
        boolean added = fileDescList.add(fileDesc);
        if(added) {
            listeners.broadcast(new FileViewChangeEvent(this, FileViewChangeEvent.Type.FILE_ADDED, fileDesc));
        }
        return added;
    }
    
    @Override
    public ListeningFuture<FileDesc> add(File file) {
        throw new UnsupportedOperationException("Cannot add files");        
    }
    
    @Override
    public ListeningFuture<FileDesc> add(File file, List<? extends LimeXMLDocument> documents) {
        throw new UnsupportedOperationException("Cannot add files");
    }
    
    @Override
    public void addListener(EventListener<FileViewChangeEvent> listener) {
        listeners.addListener(listener);
    }
    
    @Override
    public ListeningFuture<List<ListeningFuture<FileDesc>>> addFolder(File folder, FileFilter fileFilter) {
        throw new UnsupportedOperationException("Cannot add files");
    }
    
    @Override
    public void clear() {
        fileDescList.clear();
        listeners.broadcast(new FileViewChangeEvent(this, FileViewChangeEvent.Type.FILES_CLEARED, false));
    }
    
    @Override
    public boolean contains(File file) {
        for(FileDesc fd : fileDescList) {
            if(fd.getFile().equals(file)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public boolean contains(FileDesc fileDesc) {
        for(FileDesc fd : fileDescList) {
            if(fd.equals(fileDesc)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public FileDesc getFileDesc(File f) {
        for(FileDesc fd : fileDescList) {
            if(fd.getFile().equals(f)) {
                return fd;
            }
        }
        return null;
    }
    
    @Override
    public FileDesc getFileDesc(URN urn) {
        for(FileDesc fd : fileDescList) {
            if(fd.getSHA1Urn().equals(urn)) {
                return fd;
            }
        }
        return null;
    }
    
    @Override
    public FileDesc getFileDescForIndex(int index) {
        for(FileDesc fd : fileDescList) {
            if(fd.getIndex() == index) {
                return fd;
            }
        }
        return null;
    }
    
    @Override
    public List<FileDesc> getFileDescsMatching(URN urn) {
        List<FileDesc> matchingFDs = new ArrayList<FileDesc>();
        for(FileDesc fd : fileDescList) {
            if(fd.getSHA1Urn().equals(urn)) {
                matchingFDs.add(fd);
            }
        }
        return matchingFDs;
    }
    
    public List<FileDesc> getFilesInDirectory(File directory) {
        List<FileDesc> matchingFDs = new ArrayList<FileDesc>();
        for(FileDesc fd : fileDescList) {
            if(fd.getFile().getParentFile().equals(directory)) {
                matchingFDs.add(fd);
            }
        }
        return matchingFDs;
    }
    
    @Override
    public Lock getReadLock() {
        return lock;
    }
    
    @Override
    public Iterator<FileDesc> iterator() {
        return fileDescList.iterator();
    }
    @Override
    public Iterable<FileDesc> pausableIterable() {
        throw new UnsupportedOperationException("not supported");
    }
    
    @Override
    public boolean remove(File file) {
        FileDesc fd = getFileDesc(file);
        if(fd != null) {
            return remove(fd);
        } else {
            return false;
        }
    }
    
    @Override
    public boolean remove(FileDesc fileDesc) {
        boolean removed = fileDescList.remove(fileDesc);
        if(removed) {
            listeners.broadcast(new FileViewChangeEvent(this, FileViewChangeEvent.Type.FILE_REMOVED, fileDesc));
        }
        return removed;
    }
    
    @Override
    public boolean removeListener(EventListener<FileViewChangeEvent> listener) {
        return listeners.removeListener(listener);
    }
    
    @Override
    public int size() {
        return fileDescList.size();
    }    

}
