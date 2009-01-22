package org.limewire.ui.swing.search.resultpanel;

import java.util.Comparator;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.table.AbstractColumnStateFormat;
import org.limewire.ui.swing.table.ColumnStateInfo;
import org.limewire.ui.swing.util.EventListTableSortFormat;
import org.limewire.util.StringUtils;


/**
 * This class is the base class for each of the TableFormat classes
 * that describe the various table views of search results.
 */
public abstract class ResultsTableFormat<T> extends AbstractColumnStateFormat<T> 
    implements EventListTableSortFormat {

    protected VisualSearchResult vsr;
    private final int nameColumn;
    private final int fromColumn;

    public ResultsTableFormat(ColumnStateInfo... columnInfo) {
        this(-1, -1, columnInfo);
    }
    
    public ResultsTableFormat(int nameColumn, int fromColumn, ColumnStateInfo... columnInfo) {
        super(columnInfo);
        this.nameColumn = nameColumn;
        this.fromColumn = fromColumn;
    }
    
    @Override
    public Class getColumnClass(int index) {
        return String.class;
    }
    
    @Override
    public boolean isColumnHideable(int column) {
        return true;
    }

    public VisualSearchResult setColumnValue(
        VisualSearchResult vsr, Object value, int index) {
        // do nothing with the new value
        return vsr;
    }
    
    public boolean isEditable(VisualSearchResult vsr, int column) {
        return column == fromColumn;
    }

    public int getNameColumn() {
        return nameColumn;
    }

    /**
     * If the FromColumn is sorted, use a custom column sorter
     * otherwise it is assumed the column returns a value that 
     * implements the Comparable interface
     */
    @Override
    public Comparator getColumnComparator(int index) {
        if(index == fromColumn) 
            return getFromComparator();
        else if(index == nameColumn) 
            return getNameComparator();
        else
            return getLimeComparator();
    }
    
    public FromComparator getFromComparator() {
        return new FromComparator();
    }
    
    public NameComparator getNameComparator() {
        return new NameComparator();
    }
    
    /**
     * Compares the number of files being shared. 
     */
    public static class FromComparator implements Comparator<VisualSearchResult> {
        @Override
        public int compare(VisualSearchResult o1, VisualSearchResult o2) {
            int size1 = o1.getSources().size();
            int size2 = o2.getSources().size();
            
            if(size1 == size2)
                return 0;
            else if(size1 > size2)
                return 1;
            else 
                return -1;
        }
    }
    
    /**
     * Compares the name column. This is essentially a string compare but
     * VSR are returned in this column to also display an icon so we need
     * a custom comparator.
     */
    public static class NameComparator implements Comparator<VisualSearchResult> {
        @Override
        public int compare(VisualSearchResult o1, VisualSearchResult o2) {
            String name1 = getName(o1);
            String name2 = getName(o2);
            
            return name1.compareToIgnoreCase(name2);
        }
        
        private String getName(VisualSearchResult result) {
            String name = result.getPropertyString(FilePropertyKey.NAME);

            if(result.getCategory().equals(Category.AUDIO) && 
                    !StringUtils.isEmpty(result.getPropertyString(FilePropertyKey.TITLE))) {
                name =  result.getPropertyString(FilePropertyKey.TITLE);
            }
            return name;
        }
    }
}