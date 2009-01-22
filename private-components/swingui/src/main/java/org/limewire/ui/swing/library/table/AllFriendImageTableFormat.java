package org.limewire.ui.swing.library.table;

import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.table.ColumnStateInfo;
import org.limewire.ui.swing.util.I18n;

/**
 * Table format for the Image Table for All Friends table
 */
public class AllFriendImageTableFormat <T extends RemoteFileItem> extends RemoteImageTableFormat<T> {
    static final int FROM_INDEX = 6;
    
    public AllFriendImageTableFormat() {
        super(new ColumnStateInfo[] {
                new ColumnStateInfo(NAME_INDEX, "ALL_LIBRARY_IMAGE_NAME", I18n.tr("Name"), 462, true, true),     
                new ColumnStateInfo(EXTENSION_INDEX, "ALL_LIBRARY_IMAGE_EXTENSION", I18n.tr("Extension"), 32, true, true), 
                new ColumnStateInfo(CREATED_INDEX, "ALL_LIBRARY_IMAGE_CREATED", I18n.tr("Date Created"), 100, false, true), 
                new ColumnStateInfo(SIZE_INDEX, "ALL_LIBRARY_IMAGE_SIZE", I18n.tr("Size"), 15, true, true), 
                new ColumnStateInfo(TITLE_INDEX, "ALL_LIBRARY_IMAGE_TITLE", I18n.tr("Title"), 120, false, true), 
                new ColumnStateInfo(DESCRIPTION_INDEX, "ALL_LIBRARY_IMAGE_DESCRIPTION", I18n.tr("Description"), 150, false, true),
                new ColumnStateInfo(FROM_INDEX, "ALL_LIBRARY_IMAGE_FROM", I18n.tr("From"), 29, true, true)
        });
    }
    
    @Override
    public Object getColumnValue(T baseObject, int column) {
        switch(column) {
            case FROM_INDEX: return baseObject;
        }
        return super.getColumnValue(baseObject, column);
    }
}
