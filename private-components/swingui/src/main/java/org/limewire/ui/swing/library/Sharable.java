package org.limewire.ui.swing.library;

import org.limewire.ui.swing.library.sharing.ShareWidget;

public interface Sharable<T> {
    
    public void enableMyLibrarySharing(ShareWidget<T> shareWidget);

}
