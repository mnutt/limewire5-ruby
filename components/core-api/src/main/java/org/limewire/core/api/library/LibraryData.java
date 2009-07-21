package org.limewire.core.api.library;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import org.limewire.core.api.Category;

/**
 * Allows manipulation of the library.
 */
public interface LibraryData {
        
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

    /** Returns true if files from this directory are allowed to be added. */
    boolean isDirectoryAllowed(File folder);

    /** Returns all categories that should be used when files are added. */
    Collection<Category> getManagedCategories();

    /** Returns true if the library is allowed to manage programs. */
    boolean isProgramManagingAllowed();

    /**
     * Sets what categories of files will be added when a 
     * folder is added.
     */
    void setCategoriesToIncludeWhenAddingFolders(Collection<Category> managedCategories);

    /** Returns true if this file is potentially manageable. */
    boolean isFileManageable(File f);

}
