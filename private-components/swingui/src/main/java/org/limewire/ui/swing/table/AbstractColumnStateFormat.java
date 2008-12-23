package org.limewire.ui.swing.table;

import ca.odell.glazedlists.gui.AdvancedTableFormat;
import ca.odell.glazedlists.gui.WritableTableFormat;

public abstract class AbstractColumnStateFormat<T> implements VisibleTableFormat<T>, AdvancedTableFormat<T>, WritableTableFormat<T> {

    private ColumnStateInfo[] columnInfo;
    
    public AbstractColumnStateFormat(ColumnStateInfo... columnInfo) {
        this.columnInfo = columnInfo;
    }
    
    @Override
    public int getColumnCount() {
        return columnInfo.length;
    }

    @Override
    public String getColumnName(int column) {
        return columnInfo[column].getName();
    }
    
    @Override
    public boolean isVisibleAtStartup(int column) {
        return columnInfo[column].isShown();
    }
    
    @Override
    public boolean isColumnHideable(int column) {
        return columnInfo[column].isHideable();
    }

    @Override
    public int getInitialWidth(int column) {
        return columnInfo[column].getDefaultWidth();
    }
    
    public ColumnStateInfo getColumnInfo(int column) {
        return columnInfo[column];
    }
}
