package org.limewire.ui.swing.library.table;

import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.table.ColumnStateInfo;
import org.limewire.ui.swing.util.I18n;

/**
 * Table format for the All Table for All Friends table
 */
public class AllFriendVideoTableFormat <T extends RemoteFileItem> extends RemoteVideoTableFormat<T> {
    static final int FROM_INDEX = 11;
    
    public AllFriendVideoTableFormat() {
        super("ALL_LIBRARY_VIDEO_TABLE", NAME_INDEX, true, new ColumnStateInfo[] {
                new ColumnStateInfo(NAME_INDEX, "ALL_LIBRARY_VIDEO_NAME", I18n.tr("Name"), 296, true, true),     
                new ColumnStateInfo(EXTENSION_INDEX, "ALL_LIBRARY_VIDEO_EXTENSION", I18n.tr("Extension"), 44, true, true), 
                new ColumnStateInfo(LENGTH_INDEX, "ALL_LIBRARY_VIDEO_LENGTH", I18n.tr("Length"), 34, true, true), 
                new ColumnStateInfo(QUALITY_INDEX, "ALL_LIBRARY_VIDEO_QUALITY", I18n.tr("Quality"), 78, true, true), 
                new ColumnStateInfo(SIZE_INDEX, "ALL_LIBRARY_VIDEO_SIZE", I18n.tr("Size"), 39, true, true),
                new ColumnStateInfo(MISC_INDEX, "ALL_LIBRARY_VIDEO_MISC", I18n.tr("Misc"), 120, false, true), 
                new ColumnStateInfo(YEAR_INDEX, "ALL_LIBRARY_VIDEO_YEAR", I18n.tr("Year"), 60, false, true), 
                new ColumnStateInfo(RATING_INDEX, "ALL_LIBRARY_VIDEO_RATING", I18n.tr("Rating"), 60, false, true),
                new ColumnStateInfo(DIMENSION_INDEX, "ALL_LIBRARY_VIDEO_DIMENSION", I18n.tr("Resolution"), 80, false, true), 
                new ColumnStateInfo(DESCRIPTION_INDEX, "ALL_LIBRARY_VIDEO_DESCRIPTION", I18n.tr("Description"), 100, false, true),
                new ColumnStateInfo(GENRE_INDEX, "ALL_LIBRARY_VIDEO_GENRE", I18n.tr("Genre"), 80, false, true),
                new ColumnStateInfo(FROM_INDEX, "ALL_LIBRARY_OTHER_FROM", I18n.tr("From"), 38, true, true)
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
