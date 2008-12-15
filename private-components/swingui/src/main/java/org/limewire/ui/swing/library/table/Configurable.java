package org.limewire.ui.swing.library.table;

import org.limewire.core.api.library.LocalFileItem;

public interface Configurable {
    
    void configure(LocalFileItem item, boolean isRowSelected);
    
}
