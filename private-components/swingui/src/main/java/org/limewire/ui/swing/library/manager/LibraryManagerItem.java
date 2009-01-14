package org.limewire.ui.swing.library.manager;

import java.io.File;
import java.util.Collection;
import java.util.List;

/**
 * Wraps a file in a directory and a boolean value for whether this
 * directory should be scanned.
 */
public interface LibraryManagerItem {
    
    /** Shows the full name of the file or not. */
    void setShowFullName(boolean show);
    
    /** Returns the File this is an item for. */
    File getFile();
    
    /** Returns all children of this item. */
    List<LibraryManagerItem> getChildren();
    
    /** Returns the render name of this item. */
    String displayName();
    
    /** Returns the item that is this' parent. */
    LibraryManagerItem getParent();

    /** Returns all Files that are subfolders but not listed as children. */
    Collection<? extends File> getExcludedChildren();

    /** Adds the given child to the list of items to manage. */
    int addChild(LibraryManagerItem child);

    /** Removes the given child from the list of items to manage. */
    int removeChild(LibraryManagerItem item);

    /** Returns the LibraryManagerItem for a particular child directory. */
    LibraryManagerItem getChildFor(File directory);
}
