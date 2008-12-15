package org.limewire.ui.swing.library.manager;

import java.io.File;
import java.util.List;

/**
 * Wraps a file in a directory and a boolean value for whether this
 * directory should be scanned.
 */
public interface LibraryManagerItem {

	/** Marks this item as wanting to be scanned. */
    public void setScanned(boolean value);
    
    /** Returns true if this item is scanned. */
    public boolean isScanned();
    
    /** Returns the File this is an item for. */
    public File getFile();
    
    /** Returns all children of this item. */
    public List<LibraryManagerItem> getChildren();
    
    /** Returns the render name of this item. */
    public String displayName();
    
    /** Returns the item that is this' parent. */
    public LibraryManagerItem getParent();
}
