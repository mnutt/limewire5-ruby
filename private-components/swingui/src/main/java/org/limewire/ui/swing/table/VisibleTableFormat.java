package org.limewire.ui.swing.table;

import ca.odell.glazedlists.gui.TableFormat;

public interface VisibleTableFormat<T> extends TableFormat<T>{

	/** If true, column is not shown at startup*/
    boolean isColumnHiddenAtStartup(int column);
    
    /** If true, the column cannot be hidden and will not be shown in the remove/add column menu */
    boolean isColumnHideable(int column);
    
    /** Initial preferred width of the column*/
    int getInitialWidth(int column);
}
