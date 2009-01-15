package org.limewire.core.api.library;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.limewire.core.api.Category;

/**
 * Allows manipulation of the library.
 */
public interface LibraryData {
        
    /**
     * Returns a list of all directories that will be managed recursively.
     */
    List<File> getDirectoriesToManageRecursively();
    
    /** Returns a list of all excluded directories. */
    List<File> getDirectoriesToExcludeFromManaging();
    
    /**
     * Returns a list of all directories that are not recursively managed, but
     * do have files that were imported into LW.
     */
    Collection<File> getDirectoriesWithImportedFiles();

    /**
     * Returns a Map of Category->Collection<String> that defines
     * what extensions are in what category.
     */
    Map<Category, Collection<String>> getExtensionsPerCategory();

    /**
     * Returns a list of all extensions that this, by default, would
     * manage.  This is not necessarily the current list.
     */
    Collection<String> getDefaultExtensions();

    /**
     * Sets the current managed extensions to be the given set. If the
     * extensions are not in a managed category, the extension will not be
     * managed.
     */
    void setManagedExtensions(Collection<String> extensions);

    /** Returns true if the directory is allowed to be recursively managed. */
    boolean isDirectoryAllowed(File folder);

    /** Returns true if the directory is allowed to be recursively managed. */
    boolean isDirectoryExcluded(File folder);

    /** Returns all categories that should be managed. */
    Collection<Category> getManagedCategories();

    /** Returns true if the library is allowed to manage programs. */
    boolean isProgramManagingAllowed();

    /**
     * Sets the new options for managing directories.
     * This includes the new directories to manage, directories to exclude,
     * and categories to manage.
     */
    void setManagedOptions(Collection<File> recursiveFoldersToManage, 
            Collection<File> foldersToExclude,
            Collection<Category> managedCategories);
    
    /**
     * Removes a bunch of folders from being managed, as well as removing any
     * files within them from being managed.
     */
    void removeFolders(Collection<File> folders);

    /** Returns true if this file is potentially manageable. */
    boolean isFileManageable(File f);

    /**
     * Reloads the library data taking into account any changes to the managed categories.
     * 
     * Currently done by calling setManagedOptions with the current set of options.
     */
    void reload();
}
