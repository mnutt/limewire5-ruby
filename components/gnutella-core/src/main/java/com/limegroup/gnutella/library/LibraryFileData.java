package com.limegroup.gnutella.library;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.limewire.collection.CollectionUtils;
import org.limewire.core.api.Category;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.core.settings.SharingSettings;
import org.limewire.io.IOUtils;
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
        "asx;html;htm;xml;txt;pdf;ps;rtf;doc;tex;mp3;mp4;wav;wax;au;aif;aiff;"+
        "ra;ram;wma;wm;wmv;mp2v;mlv;mpa;mpv2;mid;midi;rmi;aifc;snd;flac;fla;flv;"+
        "mpg;mpeg;asf;qt;mov;avi;mpe;swf;dcr;gif;jpg;jpeg;jpe;png;tif;tiff;bmp;"+
        "exe;zip;gz;gzip;hqx;tar;tgz;z;rmj;lqt;rar;ace;sit;smi;img;ogg;rm;"+
        "bin;dmg;jve;nsv;med;mod;7z;iso;lwtp;pmf;m4a;bz2;sea;pf;arc;arj;"+
        "bz;tbz;mime;taz;ua;toast;lit;rpm;deb;pkg;sxw;l6t;srt;sub;idx;mkv;"+
        "ogm;shn;dvi;rmvp;kar;cdg;ccd;cue;c;h;m;java;jar;pl;py;pyc;"+
        "pyo;pyz;" +
        // Formerly sensitive extensions..
        "doc;pdf;xls;rtf;bak;csv;dat;docx;xlsx;xlam;xltx;xltm;xlsm;xlsb;dotm;" +
        "docm;dotx;dot;qdf;qtx;qph;qel;qdb;qsd;qif;mbf;mny";
    
    private final Collection<String> DEFAULT_MANAGED_EXTENSIONS =
        Collections.unmodifiableList(Arrays.asList(DEFAULT_MANAGED_EXTENSIONS_STRING.split(";")));
    
    private final Set<String> userExtensions = new HashSet<String>();
    private final Set<String> userRemoved = new HashSet<String>();
    private final Set<File> directoriesToManageRecursively = new HashSet<File>();
    private final Set<File> directoriesNotToManage = new HashSet<File>();
    private final Set<File> excludedFiles = new HashSet<File>();
    private final Map<File, FileProperties> libraryManageData = new HashMap<File, FileProperties>();
    private volatile boolean dirty = false;
    
    private final File saveFile = new File(CommonUtils.getUserSettingsDir(), "library5.dat"); 
    private final File backupFile = new File(CommonUtils.getUserSettingsDir(), "library5.bak");
    
    private volatile boolean loaded = false;

    LibraryFileData() {
        SettingsGroupManager.instance().addSettingsGroup(this);
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
            directoriesToManageRecursively.clear();
            directoriesNotToManage.clear();
            excludedFiles.clear();
            libraryManageData.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean save() {
        if(!loaded || !dirty) {
            return false;
        }
        
        ObjectOutputStream out = null;
        Map<String, Object> save = new HashMap<String, Object>();
        lock.readLock().lock();
        try {
            save.put("USER_EXTENSIONS", userExtensions);
            save.put("USER_REMOVED", userRemoved);
            save.put("MANAGED_DIRECTORIES", directoriesToManageRecursively);
            save.put("DO_NOT_MANAGE", directoriesNotToManage);
            save.put("EXCLUDE_FILES", excludedFiles);
            save.put("SHARE_DATA", libraryManageData);
            
            try {
                out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(backupFile)));
                out.writeObject(save);
                out.flush();
                out.close();
                out = null;
                // Rename backup to save, now that it saved.
                saveFile.delete();
                backupFile.renameTo(saveFile);                
                dirty = false;
            } catch(IOException iox) {
                LOG.debug("IOX saving library", iox);
            }
        } finally {
            lock.readLock().unlock();
            IOUtils.close(out);
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
                Set<String> userExtensions = GenericsUtils.scanForSet(readMap.get("USER_EXTENSIONS"),
                        String.class, ScanMode.REMOVE);
                Set<String> userRemoved = GenericsUtils.scanForSet(readMap.get("USER_REMOVED"),
                        String.class, ScanMode.REMOVE);
                Set<File> directoriesToManageRecursively = GenericsUtils.scanForSet(readMap
                        .get("MANAGED_DIRECTORIES"), File.class, ScanMode.REMOVE);
                Set<File> directoriesNotToManage = GenericsUtils.scanForSet(
                        readMap.get("DO_NOT_MANAGE"), File.class, ScanMode.REMOVE);
                Set<File> excludedFiles = GenericsUtils.scanForSet(readMap.get("EXCLUDE_FILES"),
                        File.class, ScanMode.REMOVE);
                Map<File, FileProperties> libraryShareData = GenericsUtils.scanForMap(readMap
                        .get("SHARE_DATA"), File.class, FileProperties.class, ScanMode.REMOVE);
                
                lock.writeLock().lock();
                try {
                    clear();
                    this.userExtensions.addAll(lowercase(userExtensions));
                    this.userRemoved.addAll(userRemoved);
                    this.directoriesToManageRecursively.addAll(directoriesToManageRecursively);
                    this.directoriesNotToManage.addAll(directoriesNotToManage);
                    this.excludedFiles.addAll(excludedFiles);
                    this.libraryManageData.putAll(libraryShareData);
                } finally {
                    lock.writeLock().unlock();
                }
                return true;
            }
        } catch(Throwable throwable) {
            LOG.error("Error loading library", throwable);
        }
        
        return false;
    }

    /** Returns true if the given file should be excluded from managing. */
    boolean isFileExcluded(File file) {
        lock.readLock().lock();
        try {
            return excludedFiles.contains(file);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Adds a managed file.  If explicit is true, this will explicitly add it to the list of managed files,
     * otherwise, it will just remove it from the list of excluded files.
     */
    void addManagedFile(File file, boolean explicit) {
        lock.writeLock().lock();
        try {
            boolean changed = excludedFiles.remove(file);
            if(explicit && !libraryManageData.containsKey(file)) {
                libraryManageData.put(file, new FileProperties());
                changed = true;
            } 
            dirty |= changed;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Removes a file from being managed.  If explicit is true, this will
     * explicitly add it to a list of excluded files that will not be
     * managed when a folder is scanned.
     * 
     * @param file
     * @param exclude
     */
    void removeManagedFile(File file, boolean explicit) {
        lock.writeLock().lock();
        try {
            boolean changed = libraryManageData.remove(file) != null;
            if(explicit) {
                changed |= excludedFiles.add(file);
            }
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
            indivFiles.addAll(libraryManageData.keySet());
        } finally {
            lock.readLock().unlock();
        }
        return indivFiles;
    }

    /** Retuns true if the given folder is the incomplete folder. */
    boolean isIncompleteDirectory(File folder) {
        return FileUtils.canonicalize(SharingSettings.INCOMPLETE_DIRECTORY.getValue()).equals(folder);
    }
    
    /** Gets the list of directories to exclude from recursive management. */
    List<File> getDirectoriesToExcludeFromManaging() {
        lock.readLock().lock();
        try {
            return new ArrayList<File>(directoriesNotToManage);
        } finally {
            lock.readLock().unlock();
        }
    }    
    
    /** Sets the new directory to exclude from recursive management. */
    void setDirectoriesToExcludeFromManaging(Collection<File> folders) {
        lock.writeLock().lock();
        try {
            boolean changed = false;
            if(!directoriesNotToManage.equals(folders)) {
                changed = true;
                directoriesNotToManage.clear();
                directoriesNotToManage.addAll(folders);
            }
            changed |= directoriesToManageRecursively.removeAll(folders);
            dirty |= changed;
        } finally {
            lock.writeLock().unlock();
        }
    }   

    /** Returns true if the given folder should be excluded. */
    boolean isFolderExcluded(File folder) {
        lock.readLock().lock();
        try {
            return directoriesNotToManage.contains(FileUtils.canonicalize(folder));
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /** Adds a new directory to recursively manage. */
    void addDirectoryToManageRecursively(File folder) {
        lock.writeLock().lock();
        try {
            boolean changed = false;
            changed |= directoriesToManageRecursively.add(folder);
            changed |= directoriesNotToManage.remove(folder);
            dirty |= changed;            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /** Sets the new directory to recursively manage. */
    void setDirectoriesToManageRecursively(Collection<File> folders) {
        lock.writeLock().lock();
        try {
            boolean changed = false;
            if(!directoriesToManageRecursively.equals(folders)) {
                changed = true;
                directoriesToManageRecursively.clear();
                directoriesToManageRecursively.addAll(folders);
            }
            changed |= directoriesNotToManage.removeAll(folders);
            dirty |= changed;            
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** Returns a list of all top-level directories that should be managed recursively. */
    List<File> getDirectoriesToManageRecursively() {
        lock.readLock().lock();
        try {
            return new ArrayList<File>(directoriesToManageRecursively);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Collection<File> getDirectoriesWithImportedFiles() {
        Set<File> directories = new HashSet<File>();
        lock.readLock().lock();
        try {
            for(File file : libraryManageData.keySet()) {
                directories.add(file.getParentFile());
            }
        } finally {
            lock.readLock().unlock();
        }
        return directories;
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
        LibrarySettings.MANAGE_AUDIO.setValue(categoriesToManage.contains(Category.AUDIO));
        LibrarySettings.MANAGE_VIDEO.setValue(categoriesToManage.contains(Category.VIDEO));
        LibrarySettings.MANAGE_DOCUMENTS.setValue(categoriesToManage.contains(Category.DOCUMENT));
        LibrarySettings.MANAGE_IMAGES.setValue(categoriesToManage.contains(Category.IMAGE));
        LibrarySettings.MANAGE_PROGRAMS.setValue(categoriesToManage.contains(Category.PROGRAM));
        LibrarySettings.MANAGE_OTHER.setValue(categoriesToManage.contains(Category.OTHER));
    }

    /** Returns all extensions that are managed within the managed categories. */
    public Collection<String> getExtensionsInManagedCategories() {
        Map<Category, Collection<String>> map = getExtensionsPerCategory();
        map.keySet().retainAll(getManagedCategories());
        return CollectionUtils.flatten(map.values());        
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
     * Returns a new Set with all the currently managed extensions contained within. 
     */
    Set<String> getManagedExtensions() {
        Set<String> extensions = new HashSet<String>();        
        try {
            lock.readLock().lock();
            extensions.addAll(DEFAULT_MANAGED_EXTENSIONS);
            extensions.addAll(userExtensions);
            extensions.removeAll(userRemoved);
        } finally {
            lock.readLock().unlock();
        }
        return extensions;
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
            
            dirty |= changed;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public Collection<String> getDefaultManagedExtensions() {
        return DEFAULT_MANAGED_EXTENSIONS;
    }
    
    /** Marks the given file as either shared or not shared with gnutella. */
    void setSharedWithGnutella(File file, boolean shared) {
        lock.writeLock().lock();
        try {
            FileProperties props = libraryManageData.get(file);
            if(props == null) {
                if(!shared) {
                    return; // Nothing to do.
                }
                props = new FileProperties();
                libraryManageData.put(file, props);
            }
            boolean changed = props.gnutella != shared;
            props.gnutella = shared;
            dirty |= changed;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /** Returns true if the file is shared with gnutella. */
    boolean isSharedWithGnutella(File file) {
        lock.readLock().lock();
        try {
            FileProperties props = libraryManageData.get(file);
            if(props != null) {
                return props.gnutella;
            } else {
                return false;
            }
        } finally {
            lock.readLock().unlock();
        }        
    }

    /** Marks the given file as either shared or not shared with the given friend. */
    void setSharedWithFriend(File file, String friendId, boolean shared) {
        lock.writeLock().lock();
        try {
            FileProperties props = libraryManageData.get(file);
            if(props == null) {
                if(!shared) {
                    return; // Nothing to do.
                }
                props = new FileProperties();
                libraryManageData.put(file, props);
            }
            if(props.friends == null) {
                if(!shared) {
                    return; // Nothing to do.
                }
                props.friends = new HashSet<String>();
            }
            boolean changed;
            if(shared) {
                changed = props.friends.add(friendId);
            } else {
                changed = props.friends.remove(friendId);
                if(props.friends.isEmpty()) {
                    props.friends = null;
                }
            }
            dirty |= changed;
        } finally {
            lock.writeLock().unlock();
        }
    }    
    
    /** Returns true if the file is shared with the given friend. */
    boolean isSharedWithFriend(File file, String friendId) {
        lock.readLock().lock();
        try {
            FileProperties props = libraryManageData.get(file);
            if(props != null && props.friends != null) {
                return props.friends.contains(friendId);
            } else {
                return false;
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    boolean isProgramManagingAllowed() {
        return LibrarySettings.ALLOW_PROGRAMS.getValue();
    }
    
    boolean isGnutellaDocumentSharingAllowed() {
        return LibrarySettings.ALLOW_DOCUMENT_GNUTELLA_SHARING.getValue();
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
    }
}
