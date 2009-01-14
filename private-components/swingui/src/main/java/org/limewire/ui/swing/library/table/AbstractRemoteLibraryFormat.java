package org.limewire.ui.swing.library.table;

import java.util.Comparator;

import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.table.AbstractColumnStateFormat;
import org.limewire.ui.swing.table.ColumnStateInfo;

/**
 * Abstract table format for remote library columns
 */
public abstract class AbstractRemoteLibraryFormat<T extends FileItem> extends AbstractColumnStateFormat<T> implements LibraryTableFormat<T>{
    
    public AbstractRemoteLibraryFormat(ColumnStateInfo... columnInfo) {
        super(columnInfo);
    }
    
    public int getActionColumn() {
        return -1;
    }

    @Override
    public boolean isEditable(T baseObject, int column) {
        return false;
    }

    @Override
    public T setColumnValue(T baseObject, Object editedValue, int column) {
        return baseObject;
    }
    
    @Override
    public Class getColumnClass(int column) {
        return String.class;
    }
    
    @Override
    public Comparator getColumnComparator(int column) {
        return getLimeComparator();
    }
    
    public class ActionComparator implements Comparator<RemoteFileItem> {
        @Override
        public int compare(RemoteFileItem o1, RemoteFileItem o2) {
            // currently this doesn't sort the action column for remotefileItems
            // this is rather expensive and complicated procedure and this column
            // might disappear anyways
            return 0;
//            boolean contains1 = !downloadListManager.contains(o1.getUrn()) && !fileList.contains(o1.getUrn());
//            boolean contains2 = !downloadListManager.contains(o2.getUrn()) && !fileList.contains(o2.getUrn());
//            
//            if(contains1 == contains2)
//                return 0;
//            else if(contains1  && !contains2)
//                return 1;
//            else
//                return -1;
        }
    }
}
