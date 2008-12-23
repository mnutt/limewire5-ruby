package org.limewire.ui.swing.library.table;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.table.ColumnStateInfo;
import org.limewire.ui.swing.util.I18n;

/**
 * Table format for the Other Table when it is in My Library
 */
public class OtherTableFormat<T extends LocalFileItem> extends AbstractMyLibraryFormat<T> {
    static final int NAME_INDEX = 0;
    static final int TYPE_INDEX = 1;
    static final int SIZE_INDEX = 2;
    static final int ACTION_INDEX = 3;
    
    public OtherTableFormat() {
        super(ACTION_INDEX,new ColumnStateInfo[] {
                new ColumnStateInfo(NAME_INDEX, "LIBRARY_OTHER_NAME", I18n.tr("Name"), 300, true, true), 
                new ColumnStateInfo(TYPE_INDEX, "LIBRARY_OTHER_TYPE", I18n.tr("Type"), 80, true, true),     
                new ColumnStateInfo(SIZE_INDEX, "LIBRARY_OTHER_SIZE", I18n.tr("Size"), 60, false, true),
                new ColumnStateInfo(ACTION_INDEX, "LIBRARY_OTHER_ACTION", I18n.tr("Sharing"), 60, true, false)
        });
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
}
