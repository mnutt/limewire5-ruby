package org.limewire.ui.swing.library.table;

import org.limewire.core.api.library.FileItem;
import org.limewire.ui.swing.table.VisibleTableFormat;

import ca.odell.glazedlists.gui.AdvancedTableFormat;
import ca.odell.glazedlists.gui.WritableTableFormat;

public interface LibraryTableFormat<T extends FileItem> extends WritableTableFormat<T>, AdvancedTableFormat<T>, VisibleTableFormat<T>{
    
    int getActionColumn();
}
