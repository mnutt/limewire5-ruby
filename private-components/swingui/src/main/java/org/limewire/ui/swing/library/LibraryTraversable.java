package org.limewire.ui.swing.library;

import java.io.File;

/**
 * The method defs for a library panel's traversal operations.
 *  These mehods can be used to move through and retrieve a panels
 *  elements.
 *  
 *  New general-traversal-like operations should be defined here.
 */
interface LibraryTraversable {
    
    /**
     * Gets the file of the list element after the element
     *  that corresponds to the passed file.
     */
    File getNextItem(File file);
    
    /**
     * Gets the file of the list element before the element
     *  that corresponds to the passed file.
     */
    File getPreviousItem(File file);
}
