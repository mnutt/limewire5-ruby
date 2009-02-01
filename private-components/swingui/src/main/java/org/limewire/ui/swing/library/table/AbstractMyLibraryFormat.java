package org.limewire.ui.swing.library.table;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jdesktop.swingx.decorator.SortKey;
import org.jdesktop.swingx.decorator.SortOrder;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.settings.TablesHandler;
import org.limewire.ui.swing.table.AbstractColumnStateFormat;
import org.limewire.ui.swing.table.ColumnStateInfo;

/**
 * Abstract table format for columns for local file items
 */
public abstract class AbstractMyLibraryFormat<T extends FileItem> extends AbstractColumnStateFormat<T> implements LibraryTableFormat<T>{
    
    private final int actionIndex;
    private final String sortID;
    private final int sortedColumn;
    private final boolean isAscending;
    
    public AbstractMyLibraryFormat(int actionIndex, String sortID, int sortedColumn, boolean isAscending, ColumnStateInfo... columnInfo) {
        super(columnInfo);
        this.actionIndex = actionIndex;
        this.sortID = sortID;
        this.sortedColumn = sortedColumn;
        this.isAscending = isAscending;
    }
    
    @Override
    public T setColumnValue(T baseObject, Object editedValue, int column) {
        return baseObject;
    }
    
    public int getActionColumn() {
        return actionIndex;
    }
    
    @Override
    public boolean isEditable(T baseObject, int column) {
        return column == actionIndex;
    }
        
    @Override
    public Class getColumnClass(int column) {
        if(column == actionIndex)
           return FileItem.class;
        else
            return String.class;
    }
    
    @Override
    public List<SortKey> getPreSortColumns() {
        return Collections.emptyList();
    }
    
    @Override
    public boolean getSortOrder() {
        return isAscending;
    }

    @Override
    public String getSortOrderID() {
        return sortID;
    }

    @Override
    public int getSortedColumn() {
        return sortedColumn;
    }
    
    @Override
    public List<SortKey> getDefaultSortKeys() {
        return Arrays.asList(
                new SortKey(((TablesHandler.getSortedOrder(getSortOrderID(), getSortOrder()).getValue() == true) ?
                    SortOrder.ASCENDING : SortOrder.DESCENDING ),
                    TablesHandler.getSortedColumn(getSortOrderID(), getSortedColumn()).getValue()));
    }

    @Override
    public Comparator getColumnComparator(int column) {
        if(column == actionIndex) 
            return new ActionComparator();
        else
            return getLimeComparator();
    }
    
    public class ActionComparator implements Comparator<FileItem> {
        @Override
        public int compare(FileItem o1, FileItem o2) {
            int friends1 =((LocalFileItem)o1).getFriendShareCount();
            int friends2 = ((LocalFileItem)o2).getFriendShareCount();
            
            if(friends1 == friends2)
                return 0;
            else if(friends1 > friends2)
                return 1;
            else
                return -1;
        }
    }
}
