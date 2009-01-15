package com.limegroup.gnutella.library;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.limewire.collection.IntSet;
import org.limewire.concurrent.ListeningFuture;
import org.limewire.concurrent.ListeningFutureDelegator;
import org.limewire.concurrent.SimpleFuture;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventMulticaster;
import org.limewire.listener.EventMulticasterImpl;
import org.limewire.util.FileUtils;
import org.limewire.util.Objects;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.licenses.LicenseType;
import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * A List of FileDescs that are grouped together 
 */
abstract class AbstractFileList implements SharedFileList {

    /**  A list of listeners for this list */
    private final EventMulticaster<FileListChangedEvent> listenerSupport;
    
    /** All indexes this list is holding. */
    private final IntSet fileDescIndexes;
    
    /** The managed list of all FileDescs. */
    protected final ManagedFileListImpl managedList;
    
    /** The listener on the ManagedList, to synchronize changes. */
    private final EventListener<FileListChangedEvent> managedListListener;
    
    /** A rw lock. */
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    
    public AbstractFileList(ManagedFileListImpl managedList) {
        this.managedList = managedList;
        this.fileDescIndexes = new IntSet(); 
        this.listenerSupport = new EventMulticasterImpl<FileListChangedEvent>();
        this.managedListListener = new ManagedListSynchronizer();
        this.managedList.addFileListListener(managedListListener);
    }
    
    @Override
    public FileDesc getFileDescForIndex(int index) {
        FileDesc fd = managedList.getFileDescForIndex(index);
        if(fd != null && contains(fd)) {
            return fd;
        } else {
            return null;
        }
    }
    
    @Override
    public FileDesc getFileDesc(File f) {
        FileDesc fd = managedList.getFileDesc(f);
        if(fd != null && contains(fd)) {
            return fd;
        } else {
            return null;
        }
    }
    
    @Override
    public FileDesc getFileDesc(URN urn) {
        List<FileDesc> descs = getFileDescsMatching(urn);
        if(descs.isEmpty()) {
            return null;
        } else {
            return descs.get(0);
        }
    }
    
    @Override
    public List<FileDesc> getFileDescsMatching(URN urn) {
        List<FileDesc> fds = null;
        List<FileDesc> matching = managedList.getFileDescsMatching(urn);
        
        // Optimal case.
        if(matching.size() == 1 && contains(matching.get(0))) {
            return matching;
        } else {
            for(FileDesc fd : matching) {
                if(contains(fd)) {
                    if(fds == null) {
                        fds = new ArrayList<FileDesc>(matching.size());
                    }
                    fds.add(fd);
                }
            }
            
            if(fds == null) {
                return Collections.emptyList();
            } else {
                return fds;
            }
        }
    }

    @Override
    public ListeningFuture<List<ListeningFuture<FileDesc>>> addFolder(final File folder) {
        managedList.addFolder(folder);
        
        return managedList.submit(new Callable<List<ListeningFuture<FileDesc>>>() {
            @Override
            public List<ListeningFuture<FileDesc>> call() throws Exception {
                File[] potentials = folder.listFiles(managedList.newManageableFilter());
                List<ListeningFuture<FileDesc>> futures = new ArrayList<ListeningFuture<FileDesc>>();
                for(File file : potentials) {
                    if(!contains(file)) {
                        futures.add(add(file));
                    }
                }
                return futures;
            }
        });
    }
    
    @Override
    public ListeningFuture<FileDesc> add(File file) {
        FileDesc fd = managedList.getFileDesc(file);
        if(fd == null) {
            saveChange(canonicalize(file), true); // Save early, will RM if it can't become FD.
            return wrapFuture(managedList.add(file));
        } else {
            add(fd);
            return futureFor(fd);
        }
    }
    
    @Override
    public ListeningFuture<FileDesc> add(File file, List<? extends LimeXMLDocument> documents) {
        FileDesc fd = managedList.getFileDesc(file);
        if(fd == null) {
            saveChange(canonicalize(file), true); // Save early, will RM if it can't become FD.
            return wrapFuture(managedList.add(file, documents));
        } else {
            add(fd);
            return futureFor(fd);
        }
    }
    
    // This is exposed in subclass interfaces -- so it doesn't override
    // anything here, but subclasses use it.
    protected boolean add(FileDesc fileDesc) {
        if(addFileDescImpl(fileDesc)) {
            saveChange(fileDesc.getFile(), true);
            fireAddEvent(fileDesc);
            return true;
        } else {
            // Must rm from save-state, because we optimistically
            // inserted when adding as a file.
            if(!contains(fileDesc)) {
                saveChange(fileDesc.getFile(), false);
            }
            return false;
        }
    }
        
    /**
     * Performs the actual add. No notification is sent when this returns.
     * @return true if the fileDesc was added, false otherwise
     */
    protected boolean addFileDescImpl(FileDesc fileDesc) {
        Objects.nonNull(fileDesc, "fileDesc");        
        rwLock.writeLock().lock();
        try {
            if(isFileAddable(fileDesc) && fileDescIndexes.add(fileDesc.getIndex())) {
                return true;
            } else {
                return false;
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }
    
    @Override
    public boolean remove(File file) {
        FileDesc fd = managedList.getFileDesc(file);
        if(fd != null) {
            return remove(fd);
        } else {
            saveChange(canonicalize(file), false);
            return false;
        }        
    }
    
    @Override
    public boolean remove(FileDesc fileDesc) {
        saveChange(fileDesc.getFile(), false);
        if(removeFileDescImpl(fileDesc)) {
            // TODO: Trigger dirty save.
            fireRemoveEvent(fileDesc);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Performs the actual remove. No notification is sent when this returns. 
     * @return true if the fileDesc was removed, false otherwise
     */
    protected boolean removeFileDescImpl(FileDesc fileDesc) {
        Objects.nonNull(fileDesc, "fileDesc");        
        rwLock.writeLock().lock();
        try {
            if(fileDescIndexes.remove(fileDesc.getIndex())) {
                return true;
            } else {
                return false;
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }
    
    @Override
    public boolean contains(File file) {
        FileDesc fd = managedList.getFileDesc(file);
        if(fd != null) {
            return contains(fd);
        } else {
            return false;
        }
    }
    
    @Override
    public boolean contains(FileDesc fileDesc) {
        rwLock.readLock().lock();
        try {
            return fileDescIndexes.contains(fileDesc.getIndex());
        } finally {
            rwLock.readLock().unlock();
        }
    }
    
    @Override
    public Iterator<FileDesc> iterator() {
        return new FileListIterator(AbstractFileList.this, fileDescIndexes);
    }
    
    @Override
    public Iterable<FileDesc> pausableIterable() {
        return new Iterable<FileDesc>() {
            @Override
            public Iterator<FileDesc> iterator() {
                return new ThreadSafeFileListIterator(AbstractFileList.this, managedList);
            }
        };
    }

    @Override
    public int size() {
        rwLock.readLock().lock();
        try {
            return fileDescIndexes.size();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public void clear() {
        boolean needsClearing;
        rwLock.writeLock().lock();
        try {
            needsClearing = fileDescIndexes.size() > 0;
            fileDescIndexes.clear();
        } finally {
            rwLock.writeLock().unlock();
        }
        
        if(needsClearing) {
            fireClearEvent();
        }
    }
    
    // Exists here so all subclasses can have access if needed.
    protected List<FileDesc> getFilesInDirectory(File directory) {
        // Remove case, trailing separators, etc.
        try {
            directory = FileUtils.getCanonicalFile(Objects.nonNull(directory, "directory"));
        } catch (IOException e) { // invalid directory ?
            return Collections.emptyList();
        }

        List<FileDesc> list = new ArrayList<FileDesc>();
        rwLock.readLock().lock();
        try {
            for(FileDesc fd : this) {
                if(directory.equals(fd.getFile().getParentFile())) {
                    list.add(fd);
                }
            }
        } finally {
            rwLock.readLock().unlock();
        }
        return list;
    }

    public Object inspect() {
        rwLock.readLock().lock();
        try {
            Map<String,Object> inspections = new HashMap<String,Object>();
            inspections.put("num of files", Integer.valueOf(fileDescIndexes.size()));
            return inspections;
        } finally {
            rwLock.readLock().unlock();
        }   
    }
        
    @Override
    public Lock getReadLock() {
        return rwLock.readLock();
    }

    @Override
    public void addFileListListener(EventListener<FileListChangedEvent> listener) {
        listenerSupport.addListener(listener);
    }

    @Override
    public void removeFileListListener(EventListener<FileListChangedEvent> listener) {
        listenerSupport.removeListener(listener);
    }
    
    /**
     * Fires an addFileDesc event to all the listeners
     * @param fileDesc that was added
     */
    protected void fireAddEvent(FileDesc fileDesc) {
        listenerSupport.handleEvent(new FileListChangedEvent(AbstractFileList.this, FileListChangedEvent.Type.ADDED, fileDesc));
    }
    
    /**
     * Fires a removeFileDesc event to all the listeners
     * @param fileDesc that was removed
     */
    protected void fireRemoveEvent(FileDesc fileDesc) {
        listenerSupport.handleEvent(new FileListChangedEvent(AbstractFileList.this, FileListChangedEvent.Type.REMOVED, fileDesc));
    }

    /**
     * Fires a changeEvent to all the listeners
     * @param oldFileDesc FileDesc that was there previously
     * @param newFileDesc FileDesc that replaced oldFileDesc
     */
    protected void fireChangeEvent(FileDesc oldFileDesc, FileDesc newFileDesc) {
        listenerSupport.handleEvent(new FileListChangedEvent(AbstractFileList.this, FileListChangedEvent.Type.CHANGED, oldFileDesc, newFileDesc));
    }
    
    /** Fires a clear event to all listeners. */
    protected void fireClearEvent() {
        listenerSupport.handleEvent(new FileListChangedEvent(AbstractFileList.this, FileListChangedEvent.Type.CLEAR));
    }
    
    /** Fires an event when the state of a shared collection changes*/
    protected void fireCollectionEvent(FileListChangedEvent.Type type, boolean value) {
        listenerSupport.handleEvent(new FileListChangedEvent(type, value));
    }
    
    /**
     * Updates the list if a containing file has been renamed
     */
    protected void updateFileDescs(FileDesc oldFileDesc, FileDesc newFileDesc) {     
        boolean failed = false;
        boolean success = false;
        rwLock.writeLock().lock();
        try {
            if (removeFileDescImpl(oldFileDesc)) {
                if(addFileDescImpl(newFileDesc)) {
                    success = true;
                } else {
                    failed = true;
                }
            } // else nothing to remove -- neither success nor failure
        } finally {
            rwLock.writeLock().unlock();
        }
        
        if(success) {
            fireChangeEvent(oldFileDesc, newFileDesc);
        } else if(failed) {
            // TODO: What do we want to do here?
            //       This will have the side effect of causing ripples
            //       if a rename/change fails for any reason.
            //saveChange(oldFileDesc.getFile(), false);
            fireRemoveEvent(oldFileDesc);
        }
    }

    /**
     * Returns true if this list is allowed to add this FileDesc
     * @param fileDesc - FileDesc to be added
     */
    protected abstract boolean isFileAddable(FileDesc fileDesc);

    /**
     * Returns true if the XML doc contains information regarding the LWS
     */
    protected boolean isStoreXML(LimeXMLDocument doc) {
       return doc != null && doc.getLicenseString() != null &&
               doc.getLicenseString().equals(LicenseType.LIMEWIRE_STORE_PURCHASE.name());
    }

    public void dispose() {
        managedList.removeFileListListener(managedListListener);
    }
    
    /**
     * Returns true if a newly loaded file from the Managed List should be added to this list.
     * If FileDesc is non-null, it's the FileDesc that will be loaded.
     */
    protected abstract boolean isPending(File file, FileDesc fileDesc);
    
    /** Hook for saving changes to data. */
    protected abstract void saveChange(File file, boolean added);
    
    protected int getMaxIndex() {
        rwLock.readLock().lock();
        try {
            return fileDescIndexes.max();
        } finally {
            rwLock.readLock().unlock();
        }
    }
    
    protected int getMinIndex() {
        rwLock.readLock().lock();
        try {
            return fileDescIndexes.min();
        } finally {
            rwLock.readLock().unlock();
        }
    }
    
    private File canonicalize(File file) {
        try {
            return FileUtils.getCanonicalFile(file);
        } catch(IOException iox) {
            return file;
        }
    }
    
    private FileDesc throwExecutionExceptionIfNotContains(FileDesc fd) throws ExecutionException {            
        if(contains(fd)) {
            return fd;
        } else {
            throw new ExecutionException(new FileListChangeFailedException(
                    new FileListChangedEvent(AbstractFileList.this, FileListChangedEvent.Type.ADD_FAILED, fd.getFile()),
                    FileListChangeFailedException.Reason.CANT_ADD_TO_LIST));
        }
    }
    
    private ListeningFuture<FileDesc> wrapFuture(final ListeningFuture<FileDesc> future) {
        return new ListeningFutureDelegator<FileDesc, FileDesc>(future) {
            @Override
            protected FileDesc convertSource(FileDesc source) throws ExecutionException {
                return throwExecutionExceptionIfNotContains(source);
            }
            
            @Override
            protected FileDesc convertException(ExecutionException ee) throws ExecutionException {
                // We can fail because we attempted to add a File that already existed --
                // if that's why we failed, then we return the file anyway (because it is added.)
                if(ee.getCause() instanceof FileListChangeFailedException) {
                    FileListChangeFailedException fe = (FileListChangeFailedException)ee.getCause();
                    if(fe.getEvent().getType() == FileListChangedEvent.Type.ADD_FAILED) {
                        if(contains(fe.getEvent().getFile())) {
                            return getFileDesc(fe.getEvent().getFile());
                        }
                    }
                }
                throw ee;
            }
        };
    }    
    
    private ListeningFuture<FileDesc> futureFor(final FileDesc fd) {
        try {
            return new SimpleFuture<FileDesc>(throwExecutionExceptionIfNotContains(fd));
        } catch(ExecutionException ee) {
            return new SimpleFuture<FileDesc>(ee);
        }
    }
    
    private class ManagedListSynchronizer implements EventListener<FileListChangedEvent> {
        @Override
        public void handleEvent(FileListChangedEvent event) {
            // Note: We only need to check for pending on adds,
            //       because that's the only kind that doesn't
            //       require it already exists.
            switch(event.getType()) {
            case ADDED:
                if(isPending(event.getFile(), event.getFileDesc())) {
                    add(event.getFileDesc());
                }
                break;
            case CHANGED:
                updateFileDescs(event.getOldValue(), event.getFileDesc());
                break;
            case REMOVED:
                remove(event.getFileDesc());
                break;
            case CLEAR:
                clear();
                break;
            case CHANGE_FAILED:
            case ADD_FAILED:
                // This can fail for double-adds, meaning the FD really does exist.
                // If that's why it failed, we pretend this is really an add.
                FileDesc fd = managedList.getFileDesc(event.getFile());
                if(fd == null) { // File doesn't exist, it was a real failure.
                    if(isPending(event.getFile(), null) && !contains(event.getFile())) {
                        saveChange(event.getFile(), false);
                    }           
                } else if(isPending(event.getFile(), fd)) {
                    add(fd);
                }
                break;
            }
        }
    }
}
