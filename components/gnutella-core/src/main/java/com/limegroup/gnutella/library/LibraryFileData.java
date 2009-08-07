package com.limegroup.gnutella.library;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.limewire.collection.CollectionUtils;
import org.limewire.core.api.Category;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.core.settings.SharingSettings;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.setting.AbstractSettingsGroup;
import org.limewire.setting.SettingsGroupManager;
import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.GenericsUtils;
import org.limewire.util.GenericsUtils.ScanMode;

import com.limegroup.gnutella.CategoryConverter;

class LibraryFileData extends AbstractSettingsGroup {
    
    private static final Log LOG = LogFactory.getLog(LibraryFileData.class);
    
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    /** Default file extensions. */
    private final String DEFAULT_MANAGED_EXTENSIONS_STRING =
        "xml;txt;ps;rtf;tex;mp3;mp4;wav;wax;au;aif;aiff;"+
        "ra;ram;mp2v;mlv;mpa;mpv2;mid;midi;rmi;aifc;snd;flac;fla;flv;"+
        "mpg;mpeg;qt;mov;avi;mpe;swf;dcr;gif;jpg;jpeg;jpe;png;tif;tiff;bmp;"+
        "zip;gz;gzip;hqx;tar;tgz;z;rmj;lqt;rar;ace;sit;smi;img;ogg;rm;"+
        "bin;dmg;jve;nsv;med;mod;7z;iso;lwtp;pmf;m4a;bz2;sea;pf;arc;arj;"+
        "bz;tbz;mime;taz;ua;toast;lit;rpm;deb;pkg;sxw;l6t;srt;sub;idx;mkv;"+
        "ogm;shn;dvi;rmvp;kar;cdg;ccd;cue;c;h;m;java;jar;pl;py;pyc;"+
        "pyo;pyz;latex";
    
    private final Collection<String> DEFAULT_MANAGED_EXTENSIONS =
        Collections.unmodifiableList(Arrays.asList(DEFAULT_MANAGED_EXTENSIONS_STRING.split(";")));
    
    private static enum Version {
        // for prior versions [before 5.0], see OldLibraryData & LibraryConverter
        ONE, // the first ever version [active 5.0 -> 5.1]
        TWO; // the current version [active 5.2 -> ]
    }
    
    private static final String CURRENT_VERSION_KEY = "CURRENT_VERSION";
    private static final String USER_EXTENSIONS_KEY = "USER_EXTENSIONS";
    private static final String USER_REMOVED_KEY = "USER_REMOVED";
//    private static final String MANAGED_DIRECTORIES_KEY = "MANAGED_DIRECTORIES";
//    private static final String DO_NOT_MANAGE_KEY = "DO_NOT_MANAGE";
//    private static final String EXCLUDE_FILES_KEY = "EXCLUDE_FILES";
    private static final String SHARE_DATA_KEY = "SHARE_DATA";
    private static final String FILE_DATA_KEY = "FILE_DATA";
    private static final String COLLECTION_NAME_KEY = "COLLECTION_NAMES";
    private static final String COLLECTION_SHARE_DATA_KEY = "COLLECTION_SHARE_DATA";
    private static final String SAFE_URNS = "SAFE_URNS";
    
    static final Integer DEFAULT_SHARED_COLLECTION_ID = 0;
    private static final Integer MIN_COLLECTION_ID = 1;
    
    
    private final Version CURRENT_VERSION = Version.TWO;
    
    private final Set<String> userExtensions = new HashSet<String>();
    private final Set<String> userRemoved = new HashSet<String>();
    private final Map<File, List<Integer>> fileData = new HashMap<File, List<Integer>>();
    private final SortedMap<Integer, String> collectionNames = new TreeMap<Integer, String>();
    private final Map<Integer, List<String>> collectionShareData = new HashMap<Integer, List<String>>();
    private final Set<String> safeUrns = new HashSet<String>();
    private volatile boolean dirty = false;
    
    private final File saveFile = new File(CommonUtils.getUserSettingsDir(), "library5.dat"); 
    private final File backupFile = new File(CommonUtils.getUserSettingsDir(), "library5.bak");
    
    private volatile boolean loaded = false;

    private volatile Set<String> managedExtensions = Collections.unmodifiableSet(new HashSet<String>());
    private volatile Set<String> extensionsInManagedCategories = Collections.unmodifiableSet(new HashSet<String>());
    
    

    LibraryFileData() {
        SettingsGroupManager.instance().addSettingsGroup(this);
        updateManagedExtensions();
    }
    
    public boolean isLoaded() {
        return loaded;
    }
    
    @Override
    public void reload() {
        load();
    }
    
    @Override
    public boolean revertToDefault() {
        clear();
        return true;
    }
    
    private void clear() {
        lock.writeLock().lock();
        try {
            dirty = true;
            userExtensions.clear();
            userRemoved.clear();
            fileData.clear();
            updateManagedExtensions();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean save() {
        if(!loaded || !dirty) {
            return false;
        }
        
        Map<String, Object> save = new HashMap<String, Object>();
        lock.readLock().lock();
        try {
            save.put(CURRENT_VERSION_KEY, CURRENT_VERSION);
            save.put(USER_EXTENSIONS_KEY, userExtensions);
            save.put(USER_REMOVED_KEY, userRemoved);
            save.put(FILE_DATA_KEY, fileData);
            save.put(COLLECTION_NAME_KEY, collectionNames);
            save.put(COLLECTION_SHARE_DATA_KEY, collectionShareData);
            save.put(SAFE_URNS, safeUrns);
            if(FileUtils.writeWithBackupFile(save, backupFile, saveFile, LOG)) {
                dirty = false;
            }
        } finally {
            lock.readLock().unlock();
        }
        
        return true;
    }
    
    void load() {
        boolean failed = false;
        if(!loadFromFile(saveFile)) {
            failed = !loadFromFile(backupFile);
        }
        dirty = failed;
        loaded = true;
    }
    
    private boolean loadFromFile(File file) {
        Map<String, Object> readMap = null;
        try {
            ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)));
            Object read = in.readObject();
            readMap = GenericsUtils.scanForMap(read, String.class, Object.class, ScanMode.REMOVE);
            if (readMap != null) {
                Object currentVersion = readMap.get("CURRENT_VERSION");
                if(currentVersion == null) {
                    currentVersion = Version.ONE;
                }
                
                if(currentVersion instanceof Version) {
                    initializeFromVersion(((Version)currentVersion), readMap);
                } else {
                    return false;
                }
                
                return true;
            }
        } catch(Throwable throwable) {
            LOG.error("Error loading library", throwable);
        }
        
        return false;
    }

    /**
     * Initializes the read map assuming it's a particular version.
     */
    private void initializeFromVersion(Version version, Map<String, Object> readMap) {
        Set<String> userExtensions;
        Set<String> userRemoved;
        Map<File, List<Integer>> fileData;
        Map<Integer, String> collectionNames;
        Map<Integer, List<String>> collectionShareData;
        Set<String> safeUrns;
        
        switch(version) {
        case ONE:
            userExtensions = GenericsUtils.scanForSet(readMap.get(USER_EXTENSIONS_KEY), String.class, ScanMode.REMOVE);
            userRemoved = GenericsUtils.scanForSet(readMap.get(USER_REMOVED_KEY), String.class, ScanMode.REMOVE);
            Map<File, FileProperties> oldShareData = GenericsUtils.scanForMap(readMap.get(SHARE_DATA_KEY), File.class, FileProperties.class, ScanMode.REMOVE);
            fileData = new HashMap<File, List<Integer>>();
            collectionNames = new HashMap<Integer, String>();
            collectionShareData = new HashMap<Integer, List<String>>();
            convertShareData(oldShareData, fileData, collectionNames, collectionShareData);
            
            final Map<File, List<Integer>> fileDataFinal = fileData;
            LibraryConverterHelper helper = new LibraryConverterHelper(new LibraryConverterHelper.FileAdder() {
               @Override
                public void addFile(File file) {
                   if(!fileDataFinal.containsKey(file)) {
                       fileDataFinal.put(file, Collections.<Integer>emptyList());
                   }
                }
            });
            
            //add save directories to library
            Set<File> convertedDirectories = new HashSet<File>();
            List<File> emptyList = Collections.emptyList();
            helper.convertSaveDirectories(emptyList, emptyList, convertedDirectories);
            
            safeUrns = new HashSet<String>();
            break;
        case TWO:
            userExtensions = GenericsUtils.scanForSet(readMap.get(USER_EXTENSIONS_KEY), String.class, ScanMode.REMOVE);
            userRemoved = GenericsUtils.scanForSet(readMap.get(USER_REMOVED_KEY), String.class, ScanMode.REMOVE);
            fileData = GenericsUtils.scanForMapOfList(readMap.get(FILE_DATA_KEY), File.class, List.class, Integer.class, ScanMode.REMOVE);
            collectionNames = GenericsUtils.scanForMap(readMap.get(COLLECTION_NAME_KEY), Integer.class, String.class, ScanMode.REMOVE);
            collectionShareData = GenericsUtils.scanForMapOfList(readMap.get(COLLECTION_SHARE_DATA_KEY), Integer.class, List.class, String.class, ScanMode.REMOVE);
            safeUrns = GenericsUtils.scanForSet(readMap.get(SAFE_URNS), String.class, ScanMode.REMOVE);
            break;
            
        default:
            throw new IllegalStateException("Invalid version: " + version);
        }
    
        validateCollectionData(fileData, collectionNames, collectionShareData);
                
        
        lock.writeLock().lock();
        try {
            clear();
            this.userExtensions.addAll(lowercase(userExtensions));
            this.userRemoved.addAll(userRemoved);
            this.fileData.putAll(fileData);
            this.collectionNames.putAll(collectionNames);
            this.collectionShareData.putAll(collectionShareData);
            this.safeUrns.addAll(safeUrns);
            updateManagedExtensions();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void validateCollectionData(Map<File, List<Integer>> fileData, Map<Integer, String> collectionNames, Map<Integer, List<String>> collectionShareData) {
        // TODO: Do some validation
    }

    /** Converts 5.0 & 5.1 style share data into 5.2-style collections. */
    private void convertShareData(Map<File, FileProperties> oldShareData, Map<File, List<Integer>> fileData, Map<Integer, String> collectionNames, Map<Integer, List<String>> collectionShareData) {
        int currentId = MIN_COLLECTION_ID;
        Map<String, Integer> friendToCollectionMap = new HashMap<String, Integer>();
        for(Map.Entry<File, FileProperties> data : oldShareData.entrySet()) {
            File file = data.getKey();
            FileProperties shareData = data.getValue();
            if (shareData == null
                    || ((shareData.friends == null || shareData.friends.isEmpty()) && !shareData.gnutella)) {
                fileData.put(file, Collections.<Integer> emptyList());
            } else {
                if (shareData.friends != null) {
                    for (String friend : shareData.friends) {
                        Integer collectionId = friendToCollectionMap.get(friend);
                        if (collectionId == null) {
                            collectionId = currentId;
                            friendToCollectionMap.put(friend, collectionId);
                            collectionNames.put(collectionId, friend);
                            List<String> shareList = new ArrayList<String>(1);
                            shareList.add(friend);
                            collectionShareData.put(collectionId, shareList);

                            currentId++;
                        }

                        List<Integer> collections = fileData.get(file);
                        if (collections == null || collections == Collections.<Integer> emptyList()) {
                            collections = new ArrayList<Integer>(1);
                            fileData.put(file, collections);
                        }
                        collections.add(collectionId);
                    }
                }

                if (shareData.gnutella) {
                    List<Integer> collections = fileData.get(file);
                    if (collections == null || collections == Collections.<Integer> emptyList()) {
                        collections = new ArrayList<Integer>(1);
                        fileData.put(file, collections);
                    }
                    collections.add(DEFAULT_SHARED_COLLECTION_ID);
                }
            }
        }
    }
    
    /** Returns true if this URN was marked as safe. */
    boolean isFileSafe(String urn) {
        lock.readLock().lock();
        try {
            return safeUrns.contains(urn);
        } finally {
            lock.readLock().unlock();
        }
    }

	/** Caches the URN as being safe or not. */
    void setFileSafe(String urn, boolean safe) {
        lock.writeLock().lock();
        try {
            if(!safe) {
                if(safeUrns.remove(urn)) {
                    dirty = true;
                }
            } else {
                if(safeUrns.add(urn)) {
                    dirty = true;
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /** Clears all file data. */
    void clearFileData() {
        lock.writeLock().lock();
        try {
            if(!fileData.isEmpty()) {
                fileData.clear();
                dirty = true;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Adds a managed file.
     */
    void addManagedFile(File file) {
        lock.writeLock().lock();
        try {
            boolean changed = false;
            if(!fileData.containsKey(file)) {
                fileData.put(file, Collections.<Integer>emptyList());
                changed = true;
            } 
            dirty |= changed;            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Removes a file from being managed.
     */
    void removeManagedFile(File file) {
        lock.writeLock().lock();
        try {
            boolean changed = fileData.remove(file) != null;
            dirty |= changed;            
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** Returns a list of all files that should be managed. */
    Iterable<File> getManagedFiles() {
        List<File> indivFiles = new ArrayList<File>();
        lock.readLock().lock();
        try {
            indivFiles.addAll(fileData.keySet());
        } finally {
            lock.readLock().unlock();
        }
        return indivFiles;
    }

    /** Retuns true if the given folder is the incomplete folder. */
    boolean isIncompleteDirectory(File folder) {
        return FileUtils.canonicalize(SharingSettings.INCOMPLETE_DIRECTORY.get()).equals(folder);
    }    
    
    /** Returns all categories that should be managed. */
    public Collection<Category> getManagedCategories() {
        Set<Category> categories = EnumSet.noneOf(Category.class);
        if(LibrarySettings.MANAGE_AUDIO.getValue()) {
            categories.add(Category.AUDIO);
        }
        if(LibrarySettings.MANAGE_DOCUMENTS.getValue()) {
            categories.add(Category.DOCUMENT);
        }
        if(LibrarySettings.MANAGE_IMAGES.getValue()) {
            categories.add(Category.IMAGE);
        }
        if(LibrarySettings.MANAGE_OTHER.getValue()) {
            categories.add(Category.OTHER);
        }
        if(LibrarySettings.MANAGE_PROGRAMS.getValue() && LibrarySettings.ALLOW_PROGRAMS.getValue()) {
            categories.add(Category.PROGRAM);
        }
        if(LibrarySettings.MANAGE_VIDEO.getValue()) {
            categories.add(Category.VIDEO);
        }
        return categories;
    }

    /** Sets the new group of categories to manage. */
    public void setManagedCategories(Collection<Category> categoriesToManage) {
    	lock.writeLock().lock();
	    try {
        	LibrarySettings.MANAGE_AUDIO.setValue(categoriesToManage.contains(Category.AUDIO));
	        LibrarySettings.MANAGE_VIDEO.setValue(categoriesToManage.contains(Category.VIDEO));
	        LibrarySettings.MANAGE_DOCUMENTS.setValue(categoriesToManage.contains(Category.DOCUMENT));
    	    LibrarySettings.MANAGE_IMAGES.setValue(categoriesToManage.contains(Category.IMAGE));
        	LibrarySettings.MANAGE_PROGRAMS.setValue(categoriesToManage.contains(Category.PROGRAM));
	        LibrarySettings.MANAGE_OTHER.setValue(categoriesToManage.contains(Category.OTHER));
            updateManagedExtensions();
        } finally {
        	lock.writeLock().unlock();
        }
    }

    /** Returns all extensions that are managed within the managed categories. The returned set cannot be mpodified.*/
    public Set<String> getExtensionsInManagedCategories() {
        return extensionsInManagedCategories;
    }
    
    /**
     * Returns a Map of Category->Collection<String> that defines
     * what extensions are in what category.
     */
    Map<Category, Collection<String>> getExtensionsPerCategory() {
        Set<String> extensions = getManagedExtensions();
        
        Map<Category, Collection<String>> extByCategory = new EnumMap<Category, Collection<String>>(Category.class);
        for(Category category : Category.values()) {
            extByCategory.put(category, new ArrayList<String>());
        }
        
        for(String ext : extensions) {
            extByCategory.get(CategoryConverter.categoryForExtension(ext)).add(ext);
        }
        
        return extByCategory;
    }


    /**
     * Should be called whenever a method is updating the extensions or the categories that the Library manages.
     * It rebuilds the managedExtentiosn and extensions in Managed categories set. So that they are always up to 
     * date and can be returned immediately.
     */
    private void updateManagedExtensions() {
        Set<String> managedExtensions = new HashSet<String>();        
        try {
            lock.writeLock().lock();
            managedExtensions.addAll(DEFAULT_MANAGED_EXTENSIONS);
            managedExtensions.addAll(userExtensions);
            managedExtensions.removeAll(userRemoved);
            this.managedExtensions  = Collections.unmodifiableSet(managedExtensions);
            
            Map<Category, Collection<String>> map = getExtensionsPerCategory();
            map.keySet().retainAll(getManagedCategories());
            extensionsInManagedCategories = Collections.unmodifiableSet(new HashSet<String>(CollectionUtils.flatten(map.values()))); 
            
        } finally {
            lock.writeLock().unlock();
        }
        
    }
    
    /**
     * Returns a new Set with all the currently managed extensions contained within. The returned set cannot be modified.
     */
    Set<String> getManagedExtensions() {
        return managedExtensions;
    }

    /** Sets all extensions that should be managed. */
    void setManagedExtensions(Collection<String> newExtensions) {
        lock.writeLock().lock();
        try {
            newExtensions = lowercase(newExtensions);
            
            boolean changed = false;
            Set<String> removed = new HashSet<String>();
            removed.addAll(DEFAULT_MANAGED_EXTENSIONS);
            removed.removeAll(newExtensions);
            if(!userRemoved.equals(removed)) {
                changed = true;
                userRemoved.clear();
                userRemoved.addAll(removed);
            }
            
            Set<String> added = new HashSet<String>();
            added.addAll(newExtensions);
            added.removeAll(DEFAULT_MANAGED_EXTENSIONS);
            if(!userExtensions.equals(added)) {
                changed = true;
                userExtensions.clear();
                userExtensions.addAll(added);
            }
            if(changed) {
                updateManagedExtensions();
            }
            dirty |= changed;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public Collection<String> getDefaultManagedExtensions() {
        return DEFAULT_MANAGED_EXTENSIONS;
    }
    
    /** Returns the IDs of all collections. */
    Collection<Integer> getStoredCollectionIds() {
        lock.readLock().lock();
        try {
            return new ArrayList<Integer>(collectionNames.keySet());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /** Marks the given file as either in the collection or not in the collection. */
    void setFileInCollection(File file, int collectionId, boolean contained) {
        lock.writeLock().lock();
        try {
            if(contained) {
                dirty |= addFileToCollection(file, collectionId);
            } else {
                dirty |= removeFileFromCollection(file, collectionId);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /** Sets whether or not all the given files should be in the collection. */
    void setFilesInCollection(Iterable<FileDesc> fileDescs, int collectionId, boolean contained) {
        lock.writeLock().lock();
        try {
            if(contained) {
                for(FileDesc fd : fileDescs) {
                    dirty |= addFileToCollection(fd.getFile(), collectionId);
                } 
            } else {
                for(FileDesc fd : fileDescs) {
                    dirty |= removeFileFromCollection(fd.getFile(), collectionId);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /** Returns true if the file was removed from the collection, false if it wasn't in the collection. */
    private boolean removeFileFromCollection(File file, int collectionId) {
        List<Integer> collections = fileData.get(file);
        if(collections == null || collections.isEmpty()) {
            return false;
        }
        
        // cast to ensure we use remove(Object) and not remove(int)
        return collections.remove((Integer)collectionId);
    }

    /** Returns true if file was added to the collection, false if it already was in the collection. */
    private boolean addFileToCollection(File file, int collectionId) {
        boolean changed = false;
        
        List<Integer> collections = fileData.get(file);
        if(collections == null || collections == Collections.<Integer>emptyList()) {
            collections = new ArrayList<Integer>(1);
            fileData.put(file, collections);
        }
        
        if(!collections.contains(collectionId)) {
            collections.add(collectionId);
            changed = true;
        }
        
        return changed;        
    }
    
    /** Returns true if the file is in the given collection. */
    boolean isFileInCollection(File file, int collectionId) {
        lock.readLock().lock();
        try {
            List<Integer> collections = fileData.get(file);
            if(collections != null) {
                return collections.contains(collectionId);
            } else {
                return false;
            }
        } finally {
            lock.readLock().unlock();
        }        
    }

    /** Returns the name of the given collection's id. */
    String getNameForCollection(int collectionId) {
        lock.readLock().lock();
        try {
            return collectionNames.get(collectionId);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /** Sets a new name for the collection of the given id. */
    boolean setNameForCollection(int collectionId, String name) {
        lock.writeLock().lock();
        try {
            String oldName = collectionNames.put(collectionId, name);
            boolean changed = oldName == null || !oldName.equals(name);
            dirty |= changed;
            return changed;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** Returns an ID that will be used for a new collection with the given name. */
    int createNewCollection(String name) {
        lock.writeLock().lock();
        try {
            int nextId = MIN_COLLECTION_ID;
            if(!collectionNames.isEmpty()) {
                nextId = collectionNames.lastKey() + 1;
            }
            collectionNames.put(nextId, name);
            dirty = true;
            return nextId;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /** Removes the collection's share data & name.  This assumes all files have already been dereferenced. */
    void removeCollection(int collectionId) {
        lock.writeLock().lock();
        try {
           dirty |= collectionNames.remove(collectionId) != null;
           dirty |= collectionShareData.remove(collectionId) != null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** Adds a new shareId to the given collection's Id. */
    boolean addFriendToCollection(int collectionId, String friendId) {
        lock.writeLock().lock();
        try {
            List<String> ids = collectionShareData.get(collectionId);
            if(ids == null) {
                ids = Collections.emptyList();
            }
            
            if(!ids.contains(friendId)) {
                ids = new ArrayList<String>(ids);                
                ids.add(friendId);
                collectionShareData.put(collectionId, Collections.unmodifiableList(ids));
                dirty = true;
                return true;
            } else {
                return false;
                }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** Removes a particular shareId from the given collection's Id. */
    boolean removeFriendFromCollection(int collectionId, String friendId) {
        lock.writeLock().lock();
        try {
            List<String> ids = collectionShareData.get(collectionId);
            if(ids != null && ids.contains(friendId)) {
                ids = new ArrayList<String>(ids);
                ids.remove(friendId);
                collectionShareData.put(collectionId, Collections.unmodifiableList(ids));
                dirty = true;
                return true;
            } else {
                return false;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }    
    
    /** Returns all shareIds for the given collection id. */
    List<String> getFriendsForCollection(int collectionId) {
        lock.readLock().lock();
        try {
            List<String> ids = collectionShareData.get(collectionId);
            if(ids != null) {
                return Collections.unmodifiableList(new ArrayList<String>(ids));
            } else {
                return Collections.emptyList();
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Sets a new share id list for the given collection id. Returns null if no
     * change was performed because the lists were the same, otherwise returns
     * the list this replaced.
     */
    List<String> setFriendsForCollection(int collectionId, List<String> newIds) {
        lock.writeLock().lock();
        try {
            List<String> oldIds = collectionShareData.get(collectionId);
            if(oldIds == null) {
                oldIds = Collections.emptyList();
            }
            
            // See if old & new are the same -- if so, don't bother.
            // (use a HashSet so that equality isn't order based)
            if(new HashSet<String>(oldIds).equals(newIds)) {
                return null;
            } else {            
                if(newIds.isEmpty()) {
                    collectionShareData.remove(collectionId);
                } else {
                    newIds = Collections.unmodifiableList(new ArrayList<String>(newIds));            
                    collectionShareData.put(collectionId, newIds);
                }
                dirty = true;
                return oldIds;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    boolean isProgramManagingAllowed() {
        return LibrarySettings.ALLOW_PROGRAMS.getValue();
    }
    
    boolean isGnutellaDocumentSharingAllowed() {
        return LibrarySettings.ALLOW_DOCUMENT_GNUTELLA_SHARING.getValue();
    }
    
    boolean isCollectionSmartAddEnabled(int id, Category category) {
        return false; // TODO: What's going on with this?
    }

    void setCollectionSmartAddEnabled(int collectionId, Category image, boolean enabled) {
        // TODO: What's going on with this?
    }    
    
    private Collection<String> lowercase(Collection<String> extensions) {
        Set<String> exts = new HashSet<String>(extensions.size());
        for(String string : extensions) {
            exts.add(string.toLowerCase(Locale.US));
        }
        return exts;
    }
    
    private static class FileProperties implements Serializable {
        private static final long serialVersionUID = 767248414812908206L;
        private boolean gnutella;
        private Set<String> friends;
        
        @Override
        public String toString() {
            return "FileProperties: gnutella: " + gnutella + ", friends: " + friends;
        }
    }
}
