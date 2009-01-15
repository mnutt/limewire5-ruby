package org.limewire.ui.swing.library.table;

import java.util.Comparator;

import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.table.AbstractColumnStateFormat;
import org.limewire.ui.swing.table.ColumnStateInfo;

/**
 * Abstract table format for columns for local file items
 */
public abstract class AbstractMyLibraryFormat<T extends FileItem> extends AbstractColumnStateFormat<T> implements LibraryTableFormat<T>{
    
    private final int actionIndex;
    
    public AbstractMyLibraryFormat(int actionIndex, ColumnStateInfo... columnInfo) {
        super(columnInfo);
        this.actionIndex = actionIndex;
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
