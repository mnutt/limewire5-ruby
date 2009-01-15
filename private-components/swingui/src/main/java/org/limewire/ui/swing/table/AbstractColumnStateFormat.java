package org.limewire.ui.swing.table;

import java.util.Comparator;

import ca.odell.glazedlists.gui.AdvancedTableFormat;
import ca.odell.glazedlists.gui.WritableTableFormat;

public abstract class AbstractColumnStateFormat<T> implements VisibleTableFormat<T>, AdvancedTableFormat<T>, WritableTableFormat<T> {

    private ColumnStateInfo[] columnInfo;
    private final LimeComparator comparator;
    
    public AbstractColumnStateFormat(ColumnStateInfo... columnInfo) {
        this.columnInfo = columnInfo;
        comparator = new LimeComparator();
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
    
    public Comparator getLimeComparator() {
        return comparator;
    }
    
    private static class LimeComparator implements Comparator<Object> {
    
        /**
         * Compares object alpha to object beta by casting object one
         * to Comparable, and calling its compareTo method.
         */
        public int compare(Object alpha, Object beta) {    
            // compare nulls
            if(alpha == null) {
                if(beta == null) return 0;
                return -1;
            } else if (beta == null){
                return 1;
            } else {
                if(alpha instanceof Long) {
                    return ((Long)alpha).compareTo((Long)beta);
                } else if(alpha instanceof Integer) {
                    return ((Integer)alpha).compareTo((Integer)beta);
                }
                return alpha.toString().compareToIgnoreCase(beta.toString());
            }
        }
    }
    
    public static class StringComparator implements Comparator<String> {
        @Override
        public int compare(String o1, String o2) {
            return o1.toLowerCase().compareTo(o2.toLowerCase());
        }
    }
}
