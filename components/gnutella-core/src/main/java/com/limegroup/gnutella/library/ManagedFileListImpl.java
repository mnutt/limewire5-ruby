package com.limegroup.gnutella.library;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.limewire.collection.CollectionUtils;
import org.limewire.collection.IntSet;
import org.limewire.collection.Tuple;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.concurrent.FutureEvent;
import org.limewire.concurrent.ListeningExecutorService;
import org.limewire.concurrent.ListeningFuture;
import org.limewire.concurrent.ListeningFutureTask;
import org.limewire.concurrent.SimpleFuture;
import org.limewire.core.api.Category;
import org.limewire.inspection.Inspectable;
import org.limewire.inspection.InspectableContainer;
import org.limewire.inspection.InspectableForSize;
import org.limewire.inspection.InspectablePrimitive;
import org.limewire.inspection.InspectionPoint;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventMulticaster;
import org.limewire.listener.EventMulticasterImpl;
import org.limewire.listener.SourcedEventMulticaster;
import org.limewire.listener.SwingSafePropertyChangeSupport;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.ExceptionUtils;
import org.limewire.util.FileUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.CategoryConverter;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UrnSet;
import com.limegroup.gnutella.auth.UrnValidator;
import com.limegroup.gnutella.auth.ValidationEvent;
import com.limegroup.gnutella.downloader.VerifyingFile;
import com.limegroup.gnutella.malware.DangerousFileChecker;
import com.limegroup.gnutella.xml.LimeXMLDocument;

@Singleton
class ManagedFileListImpl implements ManagedFileList, FileList {
    
    private static final Log LOG = LogFactory.getLog(ManagedFileListImpl.class);
    
    private static enum DirectoryLoadStyle { 
        /**
         * Used for the initial loading of all previously managed directories.
         * This preserves excluded files and recurses through subdirectories.
         */
        INITIAL_PASS(true, true, false),
        
        /**
         * Used for adding a brand new folder for managing.
         * This re-adds previously excluded files and recurses through subdirectories. 
         */
        ADD_FOLDER(true, false, true),
        
        /**
         * Used for explicitly setting a single folder to be managed.
         * This re-adds previously excluded files but does not recurse through subdirectories.
         * 
         */
        SET_FOLDER(false, false, true)
        
        ;
        
        private final boolean recurse;
        private final boolean validateDir;
        private final boolean allowExcludedFiles;
        
        DirectoryLoadStyle(boolean recurse, boolean validateDir, boolean allowExcludedFiles) {
            this.recurse = recurse;
            this.validateDir = validateDir;
            this.allowExcludedFiles = allowExcludedFiles;
        }
        
        boolean shouldRecurse() {
            return recurse;
        }
        
        boolean shouldValidateDirectory() {
            return validateDir;
        }
        
        boolean shouldAddExcludedFiles() {
            return allowExcludedFiles;
        }
    }
    
    private final SourcedEventMulticaster<FileDescChangeEvent, FileDesc> fileDescMulticaster;
    private final EventMulticaster<ManagedListStatusEvent> managedListListenerSupport;
    private final EventMulticaster<FileListChangedEvent> fileListListenerSupport;
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final UrnCache urnCache;
    private final FileDescFactory fileDescFactory; 
    private final ListeningExecutorService fileLoader;
    private final PropertyChangeSupport changeSupport;
    private final DangerousFileChecker dangerousFileChecker;
    
    /** 
     * The list of complete and incomplete files.  An entry is null if it
     *  is no longer managed.
     * INVARIANT: for all i, files[i]==null, or files[i].index==i and either
     *  files[i]._path is in a managed directory with a managed extension or
     *  files[i]._path is the incomplete directory if files[i] is an IncompleteFileDesc.
     */
    private final List<FileDesc> files;
    
    /**
     * An index that maps a <tt>File</tt> on disk to the 
     *  <tt>FileDesc</tt> holding it.
     *
     * INVARIANT: For all keys k in _fileToFileDescMap, 
     *  files[_fileToFileDescMap.get(k).getIndex()].getFile().equals(k)
     *
     * Keys must be canonical <tt>File</tt> instances.
     */
    private final Map<File, FileDesc> fileToFileDescMap;
    
    /**
     * The map of pending URNs for each File.
     */
    private final Map<File, Future<Set<URN>>> fileToUrnFuture;
 
    /**
     * A map of appropriately case-normalized URN strings to the
     * indices in files.  Used to make query-by-hash faster.
     * 
     * INVARIANT: for all keys k in urnMap, for all i in urnMap.get(k),
     * files[i].containsUrn(k).  Likewise for all i, for all k in
     * files[i].getUrns(), rnMap.get(k) contains i.
     */
    private final Map<URN, IntSet> urnMap;
    
    /**
     * The set of file extensions to manage, sorted by StringComparator. 
     * INVARIANT: all extensions are lower case.
     */
    private final Set<String> extensions;
    
    /**
     * A Set of directories that are completely managed.  Files in these
     * directories are managed by default and will be managed unless the File is
     * listed in the set of files not to share.
     */
    @InspectableForSize("number completely managed directories")
    private final Set<File> managedDirectories;
    
    /**
     * The revision of the library.  Every time 'loadSettings' is called, the revision
     * is incremented.
     */
    @InspectablePrimitive("filemanager revision")
    private final AtomicInteger revision = new AtomicInteger();
    
    /** All the library data for this library -- loaded on-demand. */
    private final LibraryFileData fileData = new LibraryFileData();  
    
    /** The validator to ask if URNs are OK. */
    private final UrnValidator urnValidator;
    
    /** The revision this finished loading. */
    private volatile int loadingFinished = -1;
    
    /** The number of files that are pending calculation. */
    private final AtomicInteger pendingFiles = new AtomicInteger(0);
    
    /** The revision of the managedFolder update. */
    private final AtomicInteger managedFolderRevision = new AtomicInteger(0);
    
    @SuppressWarnings("unused")
    @InspectableContainer
    private class LazyInspectableContainer {
        @InspectionPoint("managed files")
        private final Inspectable inspectable = new Inspectable() {
            @Override
            public Object inspect() {
                Map<String, Object> data = new HashMap<String, Object>();
                data.put("size", size());
                return data;
            }
        };
    }

    @Inject
    ManagedFileListImpl(SourcedEventMulticaster<FileDescChangeEvent, FileDesc> fileDescMulticaster,
                        UrnCache urnCache,
                        FileDescFactory fileDescFactory,
                        EventMulticaster<ManagedListStatusEvent> managedListSupportMulticaster,
                        UrnValidator urnValidator,
                        DangerousFileChecker dangerousFileChecker) {
        this.urnCache = urnCache;
        this.fileDescFactory = fileDescFactory;
        this.fileDescMulticaster = fileDescMulticaster;
        this.managedListListenerSupport = managedListSupportMulticaster;
        this.fileListListenerSupport = new EventMulticasterImpl<FileListChangedEvent>();
        this.fileLoader = ExecutorsHelper.newProcessingQueue("ManagedList Loader");
        this.files = new ArrayList<FileDesc>();
        this.extensions = new ConcurrentSkipListSet<String>();
        this.managedDirectories = new HashSet<File>();
        this.urnMap = new HashMap<URN, IntSet>();
        this.fileToFileDescMap = new HashMap<File, FileDesc>();
        this.fileToUrnFuture = new HashMap<File, Future<Set<URN>>>();
        this.urnValidator = urnValidator;
        this.changeSupport = new SwingSafePropertyChangeSupport(this);
        this.dangerousFileChecker = dangerousFileChecker;
    }
    
    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }
    
    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }
    
    /** Gets the library data, loading it if necessary. */
    LibraryFileData getLibraryData() {
        if(!fileData.isLoaded()) {
            fileData.load();
        }
        return fileData;
    }
    
    /**
     * Runs the callable in the managed filelist thread & returns a Future used
     * to get its result.
     */
    <V> ListeningFuture<V> submit(Callable<V> callable) {
        return fileLoader.submit(callable);
    }
    
    /** Initializes all listeners. */
    void initialize() {
        fileDescMulticaster.addListener(new EventListener<FileDescChangeEvent>() {
            @Override
            public void handleEvent(FileDescChangeEvent event) {
                switch(event.getType()) {
                case URNS_CHANGED:
                    if(event.getUrn() == null || event.getUrn().isSHA1()) {
                        rwLock.writeLock().lock();
                        try {
                            updateUrnIndex(event.getSource());
                        } finally {
                            rwLock.writeLock().unlock();
                        }
                    }
                    break;
                }
            }
        });
        
        urnValidator.addListener(new EventListener<ValidationEvent>() {
            @Override
            public void handleEvent(ValidationEvent event) {
                switch(event.getType()) {
                case INVALID:
                    List<FileDesc> fds = getFileDescsMatching(event.getUrn());
                    for(FileDesc fd : fds) {
                        remove(fd);
                    }
					break;
                }
            }
        });
    }
    
    /**
     * Dispatches a failure, sending a CHANGE_FAILED & REMOVE event if
     * oldFileDesc is non-null, an ADD_FAILED otherwise.
     */
    private FileListChangedEvent dispatchFailure(File file, FileDesc oldFileDesc) {
        FileListChangedEvent event;
        if(oldFileDesc != null) {
            event = new FileListChangedEvent(this, FileListChangedEvent.Type.CHANGE_FAILED, oldFileDesc.getFile(), oldFileDesc, file);
            // First dispatch a CHANGE_FAILED for the new event
            dispatch(event);
            // Then dispatch a REMOVE for the old FD.
            dispatch(new FileListChangedEvent(this, FileListChangedEvent.Type.REMOVED, oldFileDesc));
        } else {
            event = new FileListChangedEvent(this, FileListChangedEvent.Type.ADD_FAILED, file);
            // Just dispatch an ADD_FAIL for the file.
            dispatch(event);
        }
        
        return event;
    }
    
    void dispatch(FileListChangedEvent event) {
        fileListListenerSupport.broadcast(event);
    }
    
    void dispatch(ManagedListStatusEvent event) {
        managedListListenerSupport.broadcast(event);
    }

    @Override
    public FileDesc getFileDesc(File file) {
        file = FileUtils.canonicalize(file);        
        rwLock.readLock().lock();
        try {
            return fileToFileDescMap.get(file);
        } finally {
            rwLock.readLock().unlock();
        }
    }
    
    @Override
    public FileDesc getFileDescForIndex(int index) {
        rwLock.readLock().lock();
        try {
            if(index < 0 || index >= files.size()) {
                return null;
            }
            return files.get(index);
        } finally {
            rwLock.readLock().unlock();
        }
    }
    
    @Override
    public boolean contains(File file) {
        rwLock.readLock().lock();
        try {
            return fileToFileDescMap.containsKey(FileUtils.canonicalize(file));
        } finally {
            rwLock.readLock().unlock();
        }
    }
    
    
    @Override
    public boolean contains(FileDesc fileDesc) {
        rwLock.readLock().lock();
        try {
            return files.get(fileDesc.getIndex()) == fileDesc;
        } finally {
            rwLock.readLock().unlock();
        }
    }
    
    @Override
    public ListeningFuture<List<ListeningFuture<FileDesc>>> setManagedOptions(
            Collection<File> recursiveFoldersToManage,
            Collection<File> foldersToExclude,
            Collection<Category> categoriesToManage) {
        LibraryFileData libraryData = getLibraryData();
        libraryData.setDirectoriesToManageRecursively(recursiveFoldersToManage);
        libraryData.setDirectoriesToExcludeFromManaging(foldersToExclude);
        libraryData.setManagedCategories(categoriesToManage);
        
        final int loadRevision = revision.get();
        final int managedRevision = managedFolderRevision.incrementAndGet();
        fireLoading();
        return submit(new Callable<List<ListeningFuture<FileDesc>>>() {
            @Override
            public List<ListeningFuture<FileDesc>> call() throws Exception {
                return setManagedOptionsImpl(loadRevision, managedRevision);                
            }
        });
    }
    
    @Override
    public void removeFolder(File folder) {
        folder = FileUtils.canonicalize(folder);
        
        boolean managed;
        List<FileDesc> removedFds = Collections.emptyList();
        rwLock.writeLock().lock();
        try {
            managed = managedDirectories.contains(folder);
            if(!managed) {
                List<String> emptyExtensionList = Collections.emptyList();
                removedFds = removeFilesInDirectoriesOrWithExtensions(Collections.singletonList(folder), emptyExtensionList);
            }
        } finally {
            rwLock.writeLock().unlock();
        }
        
        // If we were able to remove above, fire the events.
        for(FileDesc fd : removedFds) {
            dispatch(new FileListChangedEvent(ManagedFileListImpl.this, FileListChangedEvent.Type.REMOVED, fd));
        }
        
        if(managed) {
            // If it was managed, we shouldn't have tried to remove.
            assert removedFds.size() == 0;
            LibraryFileData libraryData = getLibraryData();
            Collection<File> manage = libraryData.getDirectoriesToManageRecursively();
            Collection<File> exclude = libraryData.getDirectoriesToExcludeFromManaging();
            Collection<Category> categories = libraryData.getManagedCategories();
            if(manage.contains(folder)) {
                manage.remove(folder);
            } else {
                exclude.add(folder);
            }
            try {
                setManagedOptions(manage, exclude, categories).get();
            } catch (InterruptedException e) {
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    private List<ListeningFuture<FileDesc>> setManagedOptionsImpl(int loadRevision, int managedRevision) {
        Collection<FileDesc> removedFds;
        Collection<File> addedDirs;
        Collection<String> addedExtensions;
        Collection<File> preservedDirs;
        
        // Step 0: calculate all the new directories and subfolders to manage
        // this should be performed outside of the lock as it may be long running
        Set<File> newManagedDirs = calculateManagedDirs(managedRevision); 
        
        // if the user has changed their managed directories while calculating
        // the subfolders, return
        if(managedRevision != managedFolderRevision.get())
            return Collections.emptyList();
        
        // Step 1: Setup new extensions & remove now-unshared files.
        rwLock.writeLock().lock();
        try {
            if(loadRevision != revision.get()) {
                return Collections.emptyList();
            }
            
            //Calculate new managed dirs, what dirs were removed, and what dirs were added.
            Set<File> oldManagedDirs = new HashSet<File>(managedDirectories);
            Set<File> removedDirs = new HashSet<File>(oldManagedDirs);
            removedDirs.removeAll(newManagedDirs);                
            addedDirs = new HashSet<File>(newManagedDirs);
            addedDirs.removeAll(oldManagedDirs);
            managedDirectories.removeAll(removedDirs);
            managedDirectories.addAll(addedDirs);
            preservedDirs = new HashSet<File>(managedDirectories);
            preservedDirs.removeAll(addedDirs);
    
            // Calculate what extensions were added & removed,
            // and set extensions to the correct value.
            Collection<String> currentExtensions = new HashSet<String>(getLibraryData().getExtensionsInManagedCategories());
            addedExtensions = new HashSet<String>(currentExtensions);
            addedExtensions.removeAll(extensions);
            Collection<String> removedExtensions = new HashSet<String>(extensions);
            removedExtensions.removeAll(currentExtensions);
            extensions.clear();
            extensions.addAll(currentExtensions);
            
            // Remove any files that need removing.
            removedFds = removeFilesInDirectoriesOrWithExtensions(removedDirs, removedExtensions);
            
            if(!getLibraryData().isProgramManagingAllowed()) {
                Collection<FileDesc> programFiles = removePrograms();
                programFiles.removeAll(removedFds);
                removedFds.addAll(programFiles);
            }
            
        } finally {
            rwLock.writeLock().unlock();
        }
        
        // Step 2: Dispatch all removed files.
        for(FileDesc fd : removedFds) {
            dispatch(new FileListChangedEvent(ManagedFileListImpl.this, FileListChangedEvent.Type.REMOVED, fd));
        }
        
        // Step 3: Go through all newly managed dirs & manage them.
        List<ListeningFuture<FileDesc>> futures = new ArrayList<ListeningFuture<FileDesc>>();
        for(File dir : addedDirs) {
            addManagedDirectory(extensions, dir, loadRevision, DirectoryLoadStyle.SET_FOLDER, futures);
        }
        
        // Step 4: Go through all unchanged dirs & manage new extensions.
        if(!addedExtensions.isEmpty() && !preservedDirs.isEmpty()) {            
            for(File directory : preservedDirs) {
                addManagedDirectory(addedExtensions, directory, loadRevision, DirectoryLoadStyle.SET_FOLDER, futures);
            }
        }
        
        addLoadingListener(futures, loadRevision);
        return futures;
    }
    
    /**
     * Returns a list of all files that are programs so that they can be removed.
     */
    private List<FileDesc> removePrograms() {
        List<FileDesc> removed = new ArrayList<FileDesc>();
        for(FileDesc fd : files) {
            if(fd != null) {
                if(Category.PROGRAM == CategoryConverter.categoryForExtension(FileUtils.getFileExtension(fd.getFile()))) {
                    removed.add(fd);
                }
            }
        }
        return removed;
    }

    private List<FileDesc> removeFilesInDirectoriesOrWithExtensions(Collection<File> removedDirs, Collection<String> removedExtensions) {
        List<FileDesc> removed = new ArrayList<FileDesc>();
        if(!removedDirs.isEmpty() || !removedExtensions.isEmpty()) {
            for(FileDesc fd : files) {
                if(fd != null) {
                    boolean remove = false;
                    File parent = fd.getFile().getParentFile();
                    String ext = FileUtils.getFileExtension(fd.getFile()).toLowerCase(Locale.US);                                
                    if(removedDirs.contains(parent)) {
                        remove = true;
                    } else if(removedExtensions.contains(ext)) {
                        remove = true;
                    }
                    if(remove) {
                        removed.add(removeInternal(fd.getFile(), false));
                    }
                }
            }
        }
        return removed;
    }
    
    /** Calculates all dirs (including subdirs) that should be managed. */
    private Set<File> calculateManagedDirs(int managedRevision) {
        List<File> dirs = getLibraryData().getDirectoriesToManageRecursively();
        Set<File> allManagedDirs = new HashSet<File>();
        Queue<File> fifo = new LinkedList<File>();
        fifo.addAll(dirs);
        calculateManagedDirs(fifo, allManagedDirs, managedRevision);
 
        return allManagedDirs;
    }
    
    /**
     * Accumulator for calculating all the subfolders in a directory
     */
    private void calculateManagedDirs(Queue<File> fifo, Set<File> managedDirectories, int managedRevision) {
        while(!fifo.isEmpty()) {
            // break early if user has modified managed directories
            if(managedRevision != managedFolderRevision.get()) {
                return;
            }
            File dir = fifo.remove();
            dir = FileUtils.canonicalize(dir);
            
            if(managedDirectories.contains(dir))
                continue;
            
            managedDirectories.add(dir);
          
            File[] dirList = dir.listFiles(new ManagedDirectoryFilter());
            fifo.addAll(Arrays.asList(dirList));
        }
    }

    @Override
    public ListeningFuture<List<ListeningFuture<FileDesc>>> addFolder(File f) {
        final File folder = FileUtils.canonicalize(f);   
        
        getLibraryData().addDirectoryToManageRecursively(folder);
        fireLoading();
        return submit(new Callable<List<ListeningFuture<FileDesc>>>() {
            @Override
            public List<ListeningFuture<FileDesc>> call() {
                int rev = revision.get();
                List<ListeningFuture<FileDesc>> futures = new ArrayList<ListeningFuture<FileDesc>>();
                addManagedDirectory(extensions, folder, rev, DirectoryLoadStyle.ADD_FOLDER, futures);
                addLoadingListener(futures, rev);
                return futures;
            }
        });
    }
    
    /** Adds this incomplete file to the list of managed files */
    void addIncompleteFile(File incompleteFile,
                           Set<? extends URN> urns,
                           String name,
                           long size,
                           VerifyingFile vf) {
        // Note -- Purposely not using canonicalize so we can fail on invalid ones.
        try {
            incompleteFile = FileUtils.getCanonicalFile(incompleteFile);
        } catch(IOException ioe) {
            //invalid file?... don't add incomplete file.
            return;
        }
        
        FileDesc fd = null;
        rwLock.writeLock().lock();
        try {
            if(!fileToFileDescMap.containsKey(incompleteFile)) {
                // no indices were found for any URN associated with this
                // IncompleteFileDesc... add it.
                fd = fileDescFactory.createIncompleteFileDesc(incompleteFile, urns, files.size(), name, size, vf);
                files.add(fd);
                fileToFileDescMap.put(incompleteFile, fd);
                updateUrnIndex(fd);
            }
        } finally {
            rwLock.writeLock().unlock();
        }
        
        if(fd != null) {
            dispatch(new FileListChangedEvent(ManagedFileListImpl.this, FileListChangedEvent.Type.ADDED, fd));
        }
    }

    @Override
    public void addFileListListener(EventListener<FileListChangedEvent> listener) {
        fileListListenerSupport.addListener(listener);
    }
    
    @Override
    public void removeFileListListener(EventListener<FileListChangedEvent> listener) {
        fileListListenerSupport.removeListener(listener);
    }
    
    @Override
    public void addManagedListStatusListener(EventListener<ManagedListStatusEvent> listener) {
        managedListListenerSupport.addListener(listener);
    }
    
    @Override
    public void removeManagedListStatusListener(EventListener<ManagedListStatusEvent> listener) {
        managedListListenerSupport.removeListener(listener);
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("cannot clear managed list");
    }
    
    @Override
    public List<FileDesc> getFileDescsMatching(URN urn) {
        rwLock.readLock().lock();
        try {
            IntSet urnsMatching = urnMap.get(urn);
            if(urnsMatching == null || urnsMatching.size() == 0) {
                return Collections.emptyList();
            } else if(urnsMatching.size() == 1) { // Optimal case
                return Collections.singletonList(files.get(urnsMatching.iterator().next()));
            } else {
                return CollectionUtils.listOf(new FileListIterator(this, urnsMatching));
            }
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public Lock getReadLock() {
        return rwLock.readLock();
    }
    
    @Override
    public Iterator<FileDesc> iterator() {
        return CollectionUtils.readOnlyIterator(fileToFileDescMap.values().iterator());
    }
    
    @Override
    public Iterable<FileDesc> pausableIterable() {
        return new Iterable<FileDesc>() {
            @Override
            public Iterator<FileDesc> iterator() {
                rwLock.readLock().lock();
                try {
                    return new ThreadSafeManagedListIterator();
                } finally {
                    rwLock.readLock().unlock();
                }
            }
        };
    }

    @Override
    public boolean remove(FileDesc fileDesc) {
        return remove(fileDesc.getFile());
    }

    @Override
    public int size() {
        rwLock.readLock().lock();
        try {
            return fileToFileDescMap.size();
        } finally {
            rwLock.readLock().unlock();
        }
    }
    
    @Override
    public Collection<String> getDefaultManagedExtensions() {
        return fileData.getDefaultManagedExtensions();
    }
    
    @Override
    public Map<Category, Collection<String>> getExtensionsPerCategory() {
        return fileData.getExtensionsPerCategory();
    }
    
    @Override
    public Collection<Category> getManagedCategories() {
        return fileData.getManagedCategories();
    }
    
    @Override
    public ListeningFuture<List<ListeningFuture<FileDesc>>> setManagedExtensions(Collection<String> newManagedExtensions) {
        LibraryFileData data = getLibraryData();
        data.setManagedExtensions(newManagedExtensions);
        return setManagedOptions(data.getDirectoriesToManageRecursively(),
                                 data.getDirectoriesToExcludeFromManaging(),
                                 data.getManagedCategories());
    }

    @Override
    public List<File> getDirectoriesToManageRecursively() {
        // Make sure that things listed twice in the data are not
        // listed twice in the returned list.  That is, if somehow
        // the serialized user data got messed up and said,
        // "I want to recursively share: /parent and /parent/sub
        // We only want to return just /parent, because /parent/sub
        // is automatically included.
        // The catch is there are exclusions, so we have
        // to make sure that if the listed items are:
        // /parent, /parent/sub/sub2,
        // but the exclusions include /parent/sub, then
        // we don't want to filter out /parent/sub/sub2.
        
        List<File> managed = getLibraryData().getDirectoriesToManageRecursively();
        List<File> excluded = getLibraryData().getDirectoriesToExcludeFromManaging();
        managed.removeAll(excluded); // First remove any things that were duplicated.
        
        List<Tuple<File, File>> tuples = null;
        for(File f1 : managed) {
            for(File f2 : managed) {
                if(f1 == f2) {
                    continue;
                }
                if(FileUtils.isAncestor(f1, f2)) {
                    if(tuples == null) {
                        tuples = new ArrayList<Tuple<File,File>>();                        
                    }
                    tuples.add(new Tuple<File, File>(f1, f2));
                }
            }
        }
        // No ancestors, phew!
        if(tuples == null) {
            return managed;
        }
        
        // Damn, there were some duplicate listings...
        // Check if the child has excluded as an ancestor, the parent
        // must *not* have excluded as an ancestor, too.
        // Consider:
        // managed: /parent, /parent/sub/sub1, parent/sub/sub1/sub2
        // excluded: /parent/sub
        // In this case, sub1 & sub2 have sub as an ancestor,
        // but sub2 is still a duplicate.
        for(File exclude : excluded) {
            for(Iterator<Tuple<File, File>> tupleI = tuples.iterator(); tupleI.hasNext(); ) {
                Tuple<File, File> tuple = tupleI.next();
                if(FileUtils.isAncestor(exclude, tuple.getSecond()) && !FileUtils.isAncestor(exclude, tuple.getFirst())) {
                    tupleI.remove();
                }
            }
        }
        
        // Now that the tuples are cleaned up, go through & remove all tuples.
        for(Tuple<File, File> tuple : tuples) {
            managed.remove(tuple.getSecond());
        }
        return managed;
        
    }
    
    @Override
    public Collection<File> getDirectoriesWithImportedFiles() {
        Collection<File> directories = getLibraryData().getDirectoriesWithImportedFiles();
        rwLock.readLock().lock();
        try {
            directories.removeAll(managedDirectories);
        } finally {
            rwLock.readLock().unlock();
        }
        return directories;
    }
    
    @Override
    public List<File> getDirectoriesToExcludeFromManaging() {
        return getLibraryData().getDirectoriesToExcludeFromManaging();
    }
    
    @Override
    public ListeningFuture<FileDesc> add(File file) {
        return add(file, LimeXMLDocument.EMPTY_LIST);
    }    
    
    @Override
    public ListeningFuture<FileDesc> add(File file, List<? extends LimeXMLDocument> list) {
        return add(file, list, revision.get(), null);
    }
    
    /**
     * Adds a managed file.  Returns a future that can be used to get the FD or failure
     * event from adding the file.  Failures are throws as ExecutionExceptions from the Future.
     * 
     * @param file - the file to be added
     * @param metadata - any LimeXMLDocs associated with this file
     * @param rev - current  version of LimeXMLDocs being used
     * @param oldFileDesc the old FileDesc this is replacing
     */
    private ListeningFuture<FileDesc> add(File file, 
            final List<? extends LimeXMLDocument> metadata,
            final int rev,
            final FileDesc oldFileDesc) {
        LOG.debugf("Attempting to load file: {0}", file);
        
        // Make sure capitals are resolved properly, etc.
        try {
            file = FileUtils.getCanonicalFile(file);
        } catch (IOException e) {
            LOG.debugf("Not adding {0} because canonicalize failed", file);
            FileListChangedEvent event = dispatchFailure(file, oldFileDesc);
            return new SimpleFuture<FileDesc>(new FileListChangeFailedException(event, FileListChangeFailedException.Reason.CANT_CANONICALIZE));
        }
        
        boolean explicitAdd = false;
        rwLock.readLock().lock();
        try {
            explicitAdd = !managedDirectories.contains(file.getParentFile())
                       || !hasManageableExtension(file);
            
            // Exit if already added.
            if(fileToFileDescMap.containsKey(file)) {
                LOG.debugf("Not loading because file already loaded {0}", file);
                FileListChangedEvent event = dispatchFailure(file, oldFileDesc);
                return new SimpleFuture<FileDesc>(new FileListChangeFailedException(event, FileListChangeFailedException.Reason.ALREADY_MANAGED));
            }
        } finally {
            rwLock.readLock().unlock();
        }
        
        //make sure a FileDesc can be created from this file
        if (!LibraryUtils.isFilePhysicallyManagable(file)) {
            LOG.debugf("Not adding {0} because file isn't physically manageable", file);
            FileListChangedEvent event = dispatchFailure(file, oldFileDesc);
            return new SimpleFuture<FileDesc>(new FileListChangeFailedException(event, FileListChangeFailedException.Reason.NOT_MANAGEABLE));
        }
        
        if (!LibraryUtils.isFileAllowedToBeManaged(file)) {
            LOG.debugf("Not adding {0} because programs are not allowed to be manageable", file);
            FileListChangedEvent event = dispatchFailure(file, oldFileDesc);
            return new SimpleFuture<FileDesc>(new FileListChangeFailedException(event, FileListChangeFailedException.Reason.PROGRAMS_NOT_MANAGEABLE));
        }
        
        final File interned = new File(file.getPath().intern());
        getLibraryData().addManagedFile(interned, explicitAdd);

        boolean failed = false;
        rwLock.writeLock().lock();
        try {
            if (rev != revision.get()) {
                failed = true;
            }
        } finally {
            rwLock.writeLock().unlock();
        }
        
        if(failed) {
            LOG.debugf("Not adding {0} because revisions changed while loading", interned);
            FileListChangedEvent event = dispatchFailure(interned, oldFileDesc);
            return new SimpleFuture<FileDesc>(new FileListChangeFailedException(event, FileListChangeFailedException.Reason.REVISIONS_CHANGED));
        } else {
            final PendingFuture task = new PendingFuture();
            ListeningFuture<Set<URN>> urnFuture = urnCache.calculateAndCacheUrns(interned);
            rwLock.writeLock().lock();
            try {
                fileToUrnFuture.put(interned, urnFuture);
            } finally {
                rwLock.writeLock().unlock();
            }
            urnFuture.addFutureListener(new EventListener<FutureEvent<Set<URN>>>() {
                @Override
                public void handleEvent(FutureEvent<Set<URN>> event) {
                    // Report the exception, if one happened, so we know about it.
                    if(event.getException() != null) {
                        ExceptionUtils.reportOrReturn(event.getException());
                    }
                    finishLoadingFileDesc(interned, event, metadata, rev, oldFileDesc, task);
                }
            });
            return task;
        }
    }
    
    /** Finishes the process of loading the FD, now that URNs are known. */
    private void finishLoadingFileDesc(File file, FutureEvent<Set<URN>> urnEvent,
            List<? extends LimeXMLDocument> metadata, int rev, FileDesc oldFileDesc,
            PendingFuture task) {
        FileDesc fd = null;
        boolean revchange = false;
        boolean failed = false;
        
        // Don't add dangerous files to the library (this call may block)
        boolean dangerous = dangerousFileChecker.isDangerous(file);
        
        Set<URN> urns = urnEvent.getResult();
        rwLock.writeLock().lock();
        try {
            if(rev != revision.get()) {
                revchange = true;
                LOG.warn("Revisions changed, dropping share.");
            } else {
                fileToUrnFuture.remove(file);
                
                // Only load the file if we were able to calculate URNs 
                // assume the fd is being shared
                if(urns != null && !urns.isEmpty() && !dangerous) {
                    fd = createFileDesc(file, urns, files.size());
                    if(fd != null) {
                        if(contains(file)) {
                            failed = true;
                            fd = getFileDesc(file);
                        } else {
                            files.add(fd);
                            fileToFileDescMap.put(file, fd);
                            updateUrnIndex(fd);
                        }
                    }
                }
            }
        } finally {
            rwLock.writeLock().unlock();
        }
        
        if(revchange) {
            FileListChangedEvent event = dispatchFailure(file, oldFileDesc);
            task.setException(new FileListChangeFailedException(event, FileListChangeFailedException.Reason.REVISIONS_CHANGED));
        } else if(urnEvent.getType() != FutureEvent.Type.SUCCESS) {
            FileListChangedEvent event = dispatchFailure(file, oldFileDesc);
            task.setException(new FileListChangeFailedException(event, FileListChangeFailedException.Reason.ERROR_LOADING_URNS, urnEvent.getException()));
        } else if(dangerous) {
            LOG.debugf("Not adding {0} because the file is dangerous", file);
            FileListChangedEvent event = dispatchFailure(file, oldFileDesc);
            task.setException(new FileListChangeFailedException(event, FileListChangeFailedException.Reason.DANGEROUS_FILE));
        } else if(fd == null) {
            FileListChangedEvent event = dispatchFailure(file, oldFileDesc);
            task.setException(new FileListChangeFailedException(event, FileListChangeFailedException.Reason.CANT_CREATE_FD));
        } else if(failed) {
            LOG.debugf("Couldn't load FD because FD with file {0} exists already.  FD: {1}", file, fd);
            FileListChangedEvent event = dispatchFailure(file, oldFileDesc);
            task.setException(new FileListChangeFailedException(event, FileListChangeFailedException.Reason.ALREADY_MANAGED));
        } else { // SUCCESS!
            // try loading the XML for this fileDesc
            fileDescMulticaster.broadcast(new FileDescChangeEvent(fd, FileDescChangeEvent.Type.LOAD, metadata));
            
            // It is very important that the events get dispatched
            // prior to setting the value on the task, so that other FileLists
            // listening to these events can receive & process the event
            // prior to the future.get() returning.
            if(oldFileDesc == null) {
                LOG.debugf("Added file: {0}", file);
                dispatch(new FileListChangedEvent(this, FileListChangedEvent.Type.ADDED, fd));
            } else {
                LOG.debugf("Changed to new file: {0}", file);
                dispatch(new FileListChangedEvent(this, FileListChangedEvent.Type.CHANGED, oldFileDesc, fd));
            }
            
            task.set(fd);
        }
    }
  
    /**
     * Creates an FD for the file.  Returns null if the FD cannot be created
     * (because the URN validator says it's not valid, for example).
     */
    private FileDesc createFileDesc(File file, Set<? extends URN> urns, int index){
        if(urnValidator.isInvalid(UrnSet.getSha1(urns))) {
            return null;
        } else {
            return fileDescFactory.createFileDesc(file, urns, index);
        }
    }

    @Override
    public boolean remove(File file) {
        LOG.debugf("Removing file: {0}", file);                

        file = FileUtils.canonicalize(file);
        FileDesc fd = removeInternal(file, true);        
        if(fd != null) {
            dispatch(new FileListChangedEvent(this, FileListChangedEvent.Type.REMOVED, fd));
        }
        return fd != null;
    }
    
    /**
     * Removes the FD for this file, returning the FD that was removed.
     * This does NOT dispatch a remove event.  It will update the libraryData
     * to signify the file should not be shared, though.
     * 
     * The file should be canonicalized already.
     */
    private FileDesc removeInternal(File file, boolean allowExclude) {
        FileDesc fd;
        boolean exclude;
        rwLock.writeLock().lock();
        try {
            exclude = allowExclude && managedDirectories.contains(file.getParentFile()) && hasManageableExtension(file);
            fd = fileToFileDescMap.get(file);
            if(fd != null) {
                removeFileDesc(file, fd);
            }
        } finally {
            rwLock.writeLock().unlock();
        }
        if(fd != null) {
            getLibraryData().removeManagedFile(file, exclude);
        }
        return fd;
    }
    
    /** Removes the given FD. */
    private void removeFileDesc(File file, FileDesc fd) {
        removeUrnIndex(fd);
        FileDesc rm = files.set(fd.getIndex(), null);
        assert rm == fd;
        rm = fileToFileDescMap.remove(file);
        assert rm == fd;
        fileDescMulticaster.removeListeners(fd);
    }

    /** Generic method for adding a fileDesc's URNS to a map */
    private void updateUrnIndex(FileDesc fileDesc) {
        URN sha1 = UrnSet.getSha1(fileDesc.getUrns());
        IntSet indices = urnMap.get(sha1);
        if (indices == null) {
            indices = new IntSet();
            urnMap.put(sha1, indices);
        }
        indices.add(fileDesc.getIndex());
    }
    
    /** 
     * Removes stored indices for a URN associated with a given FileDesc
     */
    private void removeUrnIndex(FileDesc fileDesc) {
        URN sha1 = UrnSet.getSha1(fileDesc.getUrns());
        // Lookup each of desc's URN's ind _urnMap.
        IntSet indices = urnMap.get(sha1);
        if (indices == null) {
            assert fileDesc instanceof IncompleteFileDesc;
            return;
        }

        // Delete index from set. Remove set if empty.
        indices.remove(fileDesc.getIndex());
        if(indices.size() == 0) {
            urnMap.remove(sha1);
        }
    }
    
    @Override
    public ListeningFuture<FileDesc> fileRenamed(File oldName, final File newName) {
        LOG.debugf("Attempting to rename: {0} to: {1}", oldName, newName);      
        
        oldName = FileUtils.canonicalize(oldName);
        FileDesc fd = removeInternal(oldName, false);        
        if (fd != null) {
            // TODO: It's dangerous to prepopulate, because we might actually
            //       be called with wrong data, giving us wrong URNs.
            // Prepopulate the cache with new URNs.
            urnCache.addUrns(newName, fd.getUrns());
            List<LimeXMLDocument> xmlDocs = new ArrayList<LimeXMLDocument>(fd.getLimeXMLDocuments());
            return add(newName, xmlDocs, revision.get(), fd);
        } else {
            return new SimpleFuture<FileDesc>(new FileListChangeFailedException(new FileListChangedEvent(this, FileListChangedEvent.Type.CHANGE_FAILED, oldName, null, newName), FileListChangeFailedException.Reason.OLD_WASNT_MANAGED));
        }
    }
    
    @Override
    public ListeningFuture<FileDesc> fileChanged(File file, List<? extends LimeXMLDocument> xmlDocs) {
        LOG.debugf("File Changed: {0}", file);

        file = FileUtils.canonicalize(file);
        FileDesc fd = removeInternal(file, false);
        if (fd != null) {
            urnCache.removeUrns(file); // Explicitly remove URNs to force recalculating.
            return add(file, xmlDocs, revision.get(), fd);
        } else {
            return new SimpleFuture<FileDesc>(new FileListChangeFailedException(new FileListChangedEvent(this, FileListChangedEvent.Type.CHANGE_FAILED, file, null, file), FileListChangeFailedException.Reason.OLD_WASNT_MANAGED));
        }
    }
    
    /**
     * Loads all files from prior sessions.
     * This returns immediately with a Future that contains
     * the list of all Future FileDescs that will be added.
     */  
    ListeningFuture<List<ListeningFuture<FileDesc>>> loadManagedFiles() {
        final int currentRevision = revision.incrementAndGet();
        LOG.debugf("Starting new library revision: {0}", currentRevision);
        
        ListeningFuture<List<ListeningFuture<FileDesc>>> future = submit(new Callable<List<ListeningFuture<FileDesc>>>() {
            @Override
            public List<ListeningFuture<FileDesc>> call() {
                return loadSettingsInternal(currentRevision);
            }
        });
        return future;
    }

    /** 
     * Loads all shared files, putting them in a queue for being added.
     *
     * If the current revision ever changed from the expected revision, this returns
     * immediately.
     */
    private List<ListeningFuture<FileDesc>> loadSettingsInternal(int rev) {
        LOG.debugf("Loading Library Revision: {0}", rev);
        
        List<Future<Set<URN>>> urnFutures;
        rwLock.writeLock().lock();
        try {
            urnFutures = new ArrayList<Future<Set<URN>>>(fileToUrnFuture.values());
            fileToUrnFuture.clear();
            files.clear();
            urnMap.clear();
            fileToFileDescMap.clear();
            managedDirectories.clear();
            extensions.clear();
            extensions.addAll(getLibraryData().getExtensionsInManagedCategories());
        } finally {
            rwLock.writeLock().unlock();
        }
        
        for(Future<Set<URN>> future : urnFutures) {
            future.cancel(true);
        }
        
        dispatch(new FileListChangedEvent(ManagedFileListImpl.this, FileListChangedEvent.Type.CLEAR));
        
        fireLoading();
        final List<ListeningFuture<FileDesc>> futures = loadManagedFiles(rev);
        addLoadingListener(futures, rev);
        LOG.debugf("Finished queueing files for revision: {0}", rev);
        return futures;
    }
    
    void fireLoading() {
        changeSupport.firePropertyChange("hasPending", false, true);
    }
    
    private void addLoadingListener(final List<ListeningFuture<FileDesc>> futures, final int rev) {
        if(futures.isEmpty() && pendingFiles.get() == 0) {
            loadFinished(rev);
        } else if(!futures.isEmpty()) {
            pendingFiles.addAndGet(futures.size());
            
            EventListener<FutureEvent<FileDesc>> listener = new EventListener<FutureEvent<FileDesc>>() {
                @Override
                public void handleEvent(FutureEvent<FileDesc> event) {
                    if(pendingFiles.addAndGet(-1) == 0) {
                        loadFinished(rev); 
                    }
                }
            };
            
            for(ListeningFuture<FileDesc> future : futures) {
                future.addFutureListener(listener);
            }
        }
    }
    
    /** Kicks off necessary stuff for loading being done. */
    private void loadFinished(int rev) {
        changeSupport.firePropertyChange("hasPending", true, false);
        if(loadingFinished != rev) {
            loadingFinished = rev;
            LOG.debugf("Finished loading revision: {0}", rev);
            dispatch(new ManagedListStatusEvent(this, ManagedListStatusEvent.Type.LOAD_FINISHING));
            save();
            dispatch(new ManagedListStatusEvent(this, ManagedListStatusEvent.Type.LOAD_COMPLETE));
        }
    }
    
    @Override
    public boolean isLoadFinished() {
        return loadingFinished == revision.get();
    }

    private List<ListeningFuture<FileDesc>> loadManagedFiles(int rev) {
        List<ListeningFuture<FileDesc>> futures = new ArrayList<ListeningFuture<FileDesc>>();
        
        // TODO: We want to always share this stuff, not just approved extensions.
//        addManagedDirectory(extensions, LibraryUtils.PROGRAM_SHARE, rev, false, true, true, futures);
//        addManagedDirectory(extensions, LibraryUtils.PREFERENCE_SHARE, rev, false, true, true, futures);

        List<File> directories = getLibraryData().getDirectoriesToManageRecursively();
        // Sorting is not terribly necessary, but we'll do it anyway...
        Collections.sort(directories, new Comparator<File>() {
            public int compare(File a, File b) {
                return a.toString().length()-b.toString().length();
            }
        });
        
        for(File directory : directories) {
            if(rev != revision.get()) {
                break;
            }
            addManagedDirectory(extensions, directory, rev, DirectoryLoadStyle.INITIAL_PASS, futures);        
        }
        
        Set<File> managedDirs = new HashSet<File>();
        rwLock.readLock().lock();
        try {
            managedDirs.addAll(managedDirectories);
        } finally {
            rwLock.readLock().unlock();
        }
        
        // A listener that will remove individually managed files if they can't load.
        EventListener<FutureEvent<FileDesc>> indivListeners = new EventListener<FutureEvent<FileDesc>>() {
            @Override
            public void handleEvent(FutureEvent<FileDesc> event) {
                switch(event.getType()) {
                case EXCEPTION:
                    if(event.getException().getCause() instanceof FileListChangeFailedException) {
                        FileListChangeFailedException ex = (FileListChangeFailedException)event.getException().getCause();
                        switch(ex.getReason()) {
                        case CANT_CANONICALIZE:
                        case CANT_CREATE_FD:
                        case NOT_MANAGEABLE:
                        case PROGRAMS_NOT_MANAGEABLE:
                            getLibraryData().removeManagedFile(ex.getEvent().getFile(), true);
                            break;
                        }
                    }
                    break;
                }
            }
        };
        
        for(File file : getLibraryData().getManagedFiles()) {
            if(rev != revision.get()) {
                break;
            }
            // Load files that aren't in managed dirs & aren't manageable files.
            if(!managedDirs.contains(file.getParentFile()) || !hasManageableExtension(file)) {
                ListeningFuture<FileDesc> future = add(file, LimeXMLDocument.EMPTY_LIST, rev, null);
                future.addFutureListener(indivListeners);
                futures.add(future);
            }
        }
        
        return futures;
    }
    
    /**
     * Recursively adds this directory and all subdirectories to the managed
     * directories as well as queueing their files for managing.  Does nothing
     * if <tt>directory</tt> doesn't exist, isn't a directory, or has already
     * been added.  This method is thread-safe.  It acquires locks on a
     * per-directory basis.  If the current revision ever changes from the
     * expected revision, this returns immediately.
     */
    private void addManagedDirectory(Collection<String> managedExts, File directory, int rev,
            DirectoryLoadStyle loadStyle,
            List<ListeningFuture<FileDesc>> futures) {
        Set<File> addedFolders = new HashSet<File>();
        addManagedDirectoryImpl(managedExts, directory, rev, loadStyle, futures, addedFolders);
    }
    
    /**
     * The implementation of {@link #addManagedDirectory(Collection, File, int, boolean, boolean, boolean, List)}.
     */
    private void addManagedDirectoryImpl(Collection<String> managedExts, File startDirectory, int rev,
            DirectoryLoadStyle loadStyle,
            List<ListeningFuture<FileDesc>> futures, Set<File> addedFolders) {    
        LOG.debugf("Adding [{0}] to managed directories", startDirectory);
         
        Queue<File> fifo = new LinkedList<File>();
        fifo.add(startDirectory);

        while(!fifo.isEmpty()) {
            File directory = fifo.remove();
            
             directory = FileUtils.canonicalize(directory);
             if(!isFolderManageable(directory, true)) {
                 LOG.debugf("Exiting because dir isn't manageable {0}", directory);
                 continue;
             }
             
             // Immediately exit if we already added this directory in this pass.
             if(addedFolders.contains(directory)) {
                 LOG.debugf("Exiting because already added {0} in this pass", directory);
                 continue;
             } else {
                 addedFolders.add(directory);
             }
         
             // Exit quickly (without doing the dir lookup) if revisions changed.
             if(rev != revision.get()) {
                 LOG.debugf("Exiting because revisions changed.  Expected {0}, was {1}", rev, revision.get());
                 return;
             }
        
             // STEP 1:
             // Add directory
             rwLock.readLock().lock();
             try {
                 // if it was already added, ignore.
                 if (loadStyle.shouldValidateDirectory() && managedDirectories.contains(directory)) {
                     LOG.debugf("Exiting because dir already managed {0}", directory);
                     continue;
                 } else {
                     managedDirectories.add(directory);
                 }
             } finally {
                 rwLock.readLock().unlock();
             }
         
             // STEP 2:
             // Scan subdirectory for the amount of manageable files.
             File[] fileList = directory.listFiles(new ManageableFileFilter(managedExts, loadStyle.shouldAddExcludedFiles(), false));
             if (fileList == null) {
                 LOG.debugf("Exiting because of strange return value finding files in {0}", directory);
                 continue;
             }
             
             for(int i = 0; i < fileList.length && rev == revision.get(); i++) {
                 futures.add(add(fileList[i], LimeXMLDocument.EMPTY_LIST, rev, null));
             }
                 
             // Exit quickly (without doing the dir lookup) if revisions changed.
             if(rev != revision.get()) {
                 LOG.debugf("Exiting because revisions changed.  Expected {0}, was {1}", rev, revision.get());
                 return;
             }
        
             // STEP 3:
             // Recursively add subdirectories.
             if(loadStyle.shouldRecurse() && !LibraryUtils.isForcedShareDirectory(directory)) {
                 File[] dirList = directory.listFiles(new ManagedDirectoryFilter());
                 if(dirList != null && dirList.length > 0)
                     fifo.addAll(Arrays.asList(dirList));
             } else {
                 LOG.debugf("Not recursing beyond dir {0}", directory);
             }
        }
    }

    /** Dispatches a SAVE event & tells library data to save. */
    void save() {
        dispatch(new ManagedListStatusEvent(this, ManagedListStatusEvent.Type.SAVE));
        urnCache.persistCache();
        getLibraryData().save();
    }
    
    /** Returns the current revision.  Revisions are incmemented when loadManagedFiles is called. */
    int revision() {
        return revision.get();
    }

    /**
     * Returns true if this folder is manageable.  If excludeExcludedDirectories is true,
     * this will *NOT* check against the list of excluded subdirectories.
     */
    private boolean isFolderManageable(File folder, boolean excludeExcludedDirectories) {
        folder = FileUtils.canonicalize(folder);
        
        if (!folder.isDirectory() || !folder.canRead() || !folder.exists() && folder.getParent() != null) {
            return false;
        }

        if (getLibraryData().isIncompleteDirectory(folder)) {
            return false;
        }

        if (LibraryUtils.isApplicationSpecialShareDirectory(folder)) {
            return false;
        }

        if (excludeExcludedDirectories && getLibraryData().isFolderExcluded(folder)) {
            return false;
        }

        if (LibraryUtils.isFolderBanned(folder)) {
            return false;
        }

        return true;
    }
    
    /** An iterator that works over changes to the list. */
    private class ThreadSafeManagedListIterator implements Iterator<FileDesc> {        
        /** Points to the index that is to be examined next. */
        private int startRevision = revision.get();
        private int index = 0;
        private FileDesc preview;
        
        private boolean preview() {
            assert preview == null;            
            if (revision.get() != startRevision) {
                return false;
            }

            rwLock.readLock().lock();
            try {
                while (index < files.size()) {
                    preview = files.get(index);
                    index++;
                    if (preview != null) {
                        return true;
                    }
                }            
                return false;
            } finally {
                rwLock.readLock().unlock();
            }
            
        }
        
        @Override
        public boolean hasNext() {
            if (revision.get() != startRevision) {
                return false;
            }

            if (preview != null) {
                if (!contains(preview)) {
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
    
    /** Returns true if file has a manageable extension.  Case is ignored. */
    boolean hasManageableExtension(File file) {
        return extensions.contains(FileUtils.getFileExtension(file).toLowerCase(Locale.US));
    }
    
    /** Returns a filter used to get manageable files. */
    FileFilter newManageableFilter() {
        return new ManageableFileFilter(extensions, true, true);
    }
    
    @Override
    public boolean isDirectoryAllowed(File folder) {
        return folder.isDirectory() && isFolderManageable(folder, false);
    }
    
    @Override
    public boolean isDirectoryExcluded(File folder) {
        return getLibraryData().isFolderExcluded(folder);
    }
    
    @Override
    public boolean isProgramManagingAllowed() {
        return getLibraryData().isProgramManagingAllowed();
    }
    
    /** A filter used to see if a file is manageable. */
    private class ManageableFileFilter implements FileFilter {
        private final Set<String> extensions;
        private final boolean allowExcludedFiles;
        private final boolean includeContainedFiles;
        
        /** Constructs the filter with the given set of allowed extensions. */
        public ManageableFileFilter(Collection<String> extensions, boolean allowExcludedFiles, boolean includeContainedFiles) {
            this.extensions = new HashSet<String>(extensions);
            this.allowExcludedFiles = allowExcludedFiles;
            this.includeContainedFiles = includeContainedFiles;
        }
        
        @Override
        public boolean accept(File file) {
            return file.isFile()
                && (includeContainedFiles || !contains(file))
                && LibraryUtils.isFileManagable(file)
                && extensions.contains(FileUtils.getFileExtension(file).toLowerCase(Locale.US))
                && (allowExcludedFiles || !getLibraryData().isFileExcluded(file));
        }
    }
    
    /** The filter object to use to determine directories. */
    private class ManagedDirectoryFilter implements FileFilter {
        @Override
        public boolean accept(File f) {
            return f.isDirectory()
                && isFolderManageable(f, true);
        }        
    }

    /** A simple empty callable for use in PendingFuture. */
    private final static Callable<FileDesc> EMPTY_CALLABLE = new Callable<FileDesc>() {
        @Override
        public FileDesc call() { return null; }
    };
    
    /** A future that delegates on another future, occasionally. */
    private static class PendingFuture extends ListeningFutureTask<FileDesc> {
        public PendingFuture() {
            super(EMPTY_CALLABLE);
        }
        
        @Override
        public void run() {
            // Do nothing -- there is nothing to run.
        }

        @Override
        // Raise access so we can set the FD. */
        public void set(FileDesc v) {
            super.set(v);
        }

        @Override
        // Raise access so we can set the error. */
        public void setException(Throwable t) {
            super.setException(t);
        }
    }
}
