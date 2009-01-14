package org.limewire.ui.swing.library;

import org.limewire.core.api.library.FileItem;

/**
 * Interface that amalgamates the method defs for all a library panel's
 *  operations.  Hopefully, these operations would pertain to 
 *  any Library type, ie. image, document, etc..  
 *  
 *  NOTE: At the moment LibraryTraversable is not necessary in
 *         LibraryImagePanel.
 */
public interface LibraryOperable<T extends FileItem> extends LibraryTraversable, LibrarySelectable, SelectAllable<T> {
    
}
