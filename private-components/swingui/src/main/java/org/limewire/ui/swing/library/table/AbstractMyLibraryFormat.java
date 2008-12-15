package org.limewire.ui.swing.library.table;

import java.util.Comparator;

import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.LocalFileItem;

import ca.odell.glazedlists.GlazedLists;

public abstract class AbstractMyLibraryFormat<T extends FileItem> implements LibraryTableFormat<T> {

    @Override
    public Class getColumnClass(int column) {
        return String.class;
    }
    
    @Override
    public Comparator getColumnComparator(int column) {
        return GlazedLists.comparableComparator();
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
