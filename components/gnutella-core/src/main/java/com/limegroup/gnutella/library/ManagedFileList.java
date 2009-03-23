package com.limegroup.gnutella.library;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.limewire.concurrent.ListeningFuture;
import org.limewire.core.api.Category;
import org.limewire.listener.EventListener;

import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * The list of all files that this library is managing.
 * This list can include files that are shared, are not shared,
 * are files from the store, are shared with friends, are incomplete, etc...
 * 
 * Inclusion in this list means only that LimeWire knows about this file.
 */
public interface ManagedFileList extends FileList {
    
    void addManagedListStatusListener(EventListener<ManagedListStatusEvent> listener);
    void removeManagedListStatusListener(EventListener<ManagedListStatusEvent> listener);
    
    void addPropertyChangeListener(PropertyChangeListener listener);
    void removePropertyChangeListener(PropertyChangeListener listener);
  
    /** Returns true if the initial load of the library has finished. */
    boolean isLoadFinished();
    
    /** Informs the library that the file 'oldName' has been renamed to 'newName'. */
    ListeningFuture<FileDesc> fileRenamed(File oldName, File newName);
    
    /** Informs the library that the file 'file' has changed. */
    ListeningFuture<FileDesc> fileChanged(File file, List<? extends LimeXMLDocument> xmlDocs);
    
    /** Returns a list of all directories that will be recursively managed. */
    List<File> getDirectoriesToManageRecursively();
    
    /** Gets the set of default managed extensions.  This is not the current set. */
    Collection<String> getDefaultManagedExtensions();
    
    /** Gets the current set of managed extensions. */
    Map<Category, Collection<String>> getExtensionsPerCategory();
    
    /**
     * Sets a new collection of managed extensions.
     * Returns a Future of the list of all futures this will add
     * as part of the extensions changing.
     */
    ListeningFuture<List<ListeningFuture<FileDesc>>> setManagedExtensions(Collection<String> extensions);
    
    /**
     * Adds manageable files in this directory and recursively all
     * subdirectories as managed files.
     */
    @Override
    ListeningFuture<List<ListeningFuture<FileDesc>>> addFolder(File folder);
    
    /**
     * Returns true if the directory is allowed to be added as a recursively
     * managed folder.  This <b>allows</b> excluded directories.  It does
     * not allow non-directories, banned directories, or other problematic
     * directories.
     */
    boolean isDirectoryAllowed(File folder);
    
    /**
     * Returns true if the directory is excluded from recursive management.
     */
    boolean isDirectoryExcluded(File folder);
    
    /** Returns all directories that are excluded from managing. */
    List<File> getDirectoriesToExcludeFromManaging();
    
    /** Returns all categories that should be managed. */
    Collection<Category> getManagedCategories();
    
    /** Returns true if this is allowed to many any programs. */
    boolean isProgramManagingAllowed();
    
    /**
     * Returns a collection of all directories that are not managed, but
     * do have files that were imported into LW.
     */
    Collection<File> getDirectoriesWithImportedFiles();
    
    /**
     * Sets the new options for managing directories.
     * This includes the new directories to manage, directories to exclude,
     * and categories to manage.
     */
    ListeningFuture<List<ListeningFuture<FileDesc>>> setManagedOptions(
            Collection<File> recursiveFoldersToManage,
            Collection<File> foldersToExclude,
            Collection<Category> managedCategories);
    
    /** Stops managing all files in the given folder. */
    void removeFolder(File folder);
}
