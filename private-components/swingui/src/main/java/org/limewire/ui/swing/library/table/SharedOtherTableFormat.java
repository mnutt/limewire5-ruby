package org.limewire.ui.swing.library.table;

import java.util.Comparator;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.table.ColumnStateInfo;
import org.limewire.ui.swing.util.I18n;

/**
 * Table format for the Other Table when it is in Sharing View
 */
public class SharedOtherTableFormat<T extends LocalFileItem> extends AbstractMyLibraryFormat<T> {
    static final int ACTION_INDEX = 0;
    static final int NAME_INDEX = 1;
    static final int TYPE_INDEX = 2;
    static final int SIZE_INDEX = 3;
    
    private final LocalFileList localFileList;
    
    public SharedOtherTableFormat(LocalFileList localFileList) {
        super(ACTION_INDEX, new ColumnStateInfo[] {
                new ColumnStateInfo(ACTION_INDEX, "SHARED_LIBRARY_OTHER_ACTION", I18n.tr("Sharing"), 60, true, false),
                new ColumnStateInfo(NAME_INDEX, "SHARED_LIBRARY_OTHER_NAME", I18n.tr("Name"), 300, true, true), 
                new ColumnStateInfo(TYPE_INDEX, "SHARED_LIBRARY_OTHER_TYPE", I18n.tr("Type"), 80, true, true),     
                new ColumnStateInfo(SIZE_INDEX, "SHARED_LIBRARY_OTHER_SIZE", I18n.tr("Size"), 60, false, true)
        });
        this.localFileList = localFileList;
    }
    
    @Override
    public Object getColumnValue(T baseObject, int column) {
        switch(column) {
        case NAME_INDEX: return baseObject;
        case TYPE_INDEX: return "";
        case SIZE_INDEX: return baseObject.getSize();
        case ACTION_INDEX: return baseObject;
        }
        throw new IllegalArgumentException("Unknown column:" + column);
    }
    
    @Override
    public Comparator getColumnComparator(int column) {
        switch(column) {
            case ACTION_INDEX: return new CheckBoxComparator(localFileList);
        }
        return super.getColumnComparator(column);
    }
}
