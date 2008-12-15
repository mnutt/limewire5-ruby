package org.limewire.ui.swing.library;

import java.io.File;

import org.limewire.core.api.URN;

/** Allows a way of selecting an item based on URN or File. */
public interface LibrarySelectable {
    
    void selectAndScrollTo(URN urn);
    
    void selectAndScrollTo(File file);
}
