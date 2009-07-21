package org.limewire.core.impl.library;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;

import org.limewire.listener.EventListener;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.FileView;
import com.limegroup.gnutella.library.FileViewChangeEvent;

public class FileViewStub implements FileView {
    
    private final String name;
    
    public FileViewStub(String name) {
        this.name = name;
    }

    @Override
    public boolean contains(File file) {

        return false;
    }

    @Override
    public boolean contains(FileDesc fileDesc) {

        return false;
    }

    @Override
    public FileDesc getFileDesc(URN urn) {

        return null;
    }

    @Override
    public FileDesc getFileDesc(File f) {

        return null;
    }

    @Override
    public FileDesc getFileDescForIndex(int index) {

        return null;
    }

    @Override
    public List<FileDesc> getFileDescsMatching(URN urn) {

        return null;
    }

    @Override
    public List<FileDesc> getFilesInDirectory(File directory) {

        return null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public long getNumBytes() {

        return 0;
    }

    @Override
    public Lock getReadLock() {

        return null;
    }

    @Override
    public Iterator<FileDesc> iterator() {

        return null;
    }

    @Override
    public Iterable<FileDesc> pausableIterable() {

        return null;
    }

    @Override
    public int size() {

        return 0;
    }

    @Override
    public void addListener(EventListener<FileViewChangeEvent> listener) {

        
    }

    @Override
    public boolean removeListener(EventListener<FileViewChangeEvent> listener) {

        return false;
    }

}
