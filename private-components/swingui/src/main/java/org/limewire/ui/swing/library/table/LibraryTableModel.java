package org.limewire.ui.swing.library.table;

import org.limewire.core.api.library.FileItem;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.swing.EventTableModel;

public class LibraryTableModel<T extends FileItem>  extends EventTableModel<T> {

    private final EventList<T> libraryItems;

    public LibraryTableModel(EventList<T> libraryItems, LibraryTableFormat<T> format) {
        super(libraryItems, format, false);
        this.libraryItems = libraryItems;
    }
    
    public T getFileItem(int index) {
        return libraryItems.get(index);
    }
    
    public EventList<T> getAllItems() {
        return libraryItems;
    }
}
