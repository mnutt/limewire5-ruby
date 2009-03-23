package org.limewire.ui.swing.library;

import java.util.List;

public interface SelectAllable<T> {

    /** Selects all items. */
    public void selectAll();
    
    /** Returns all selected items. */
    public List<T> getSelectedItems();
    
    /** Returns all selected and unselected items. */
    List<T> getAllItems();
    
}
