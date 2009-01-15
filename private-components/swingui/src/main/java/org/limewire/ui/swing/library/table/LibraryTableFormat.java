package org.limewire.ui.swing.library.table;

import org.limewire.core.api.library.FileItem;
import org.limewire.ui.swing.table.VisibleTableFormat;
import org.limewire.ui.swing.util.EventListTableSortFormat;

import ca.odell.glazedlists.gui.AdvancedTableFormat;
import ca.odell.glazedlists.gui.WritableTableFormat;

public interface LibraryTableFormat<T extends FileItem> 
    extends WritableTableFormat<T>, AdvancedTableFormat<T>, VisibleTableFormat<T>, 
    EventListTableSortFormat {
    
    int getActionColumn();
}
