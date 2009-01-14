package org.limewire.ui.swing.library;

import java.util.List;

public interface SelectAllable<T> {

    public void selectAll();
    
    public List<T> getSelectedItems();
    
}
