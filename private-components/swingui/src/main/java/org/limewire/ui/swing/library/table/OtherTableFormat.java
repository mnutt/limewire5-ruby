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
    static final int HIT_INDEX = 3;
    static final int UPLOADS_INDEX = 4;
    static final int UPLOAD_ATTEMPTS_INDEX = 5;
    static final int ACTION_INDEX = 6;
    
    public OtherTableFormat() {
        super(ACTION_INDEX,new ColumnStateInfo[] {
                new ColumnStateInfo(NAME_INDEX, "LIBRARY_OTHER_NAME", I18n.tr("Name"), 300, true, true), 
                new ColumnStateInfo(TYPE_INDEX, "LIBRARY_OTHER_TYPE", I18n.tr("Type"), 80, true, true),     
                new ColumnStateInfo(SIZE_INDEX, "LIBRARY_OTHER_SIZE", I18n.tr("Size"), 60, false, true),
                new ColumnStateInfo(HIT_INDEX, "LIBRARY_OTHER_HITS", I18n.tr("Hits"), 100, false, true), 
                new ColumnStateInfo(UPLOADS_INDEX, "LIBRARY_OTHER_UPLOADS", I18n.tr("Uploads"), 100, false, true), 
                new ColumnStateInfo(UPLOAD_ATTEMPTS_INDEX, "LIBRARY_OTHER_UPLOAD_ATTEMPTS", I18n.tr("Upload attempts"), 200, false, true),
                new ColumnStateInfo(ACTION_INDEX, "LIBRARY_OTHER_ACTION", I18n.tr("Sharing"), 60, true, false)
        });
    }

    @Override
    public Object getColumnValue(T baseObject, int column) {
        switch(column) {
        case NAME_INDEX: return baseObject;
        case TYPE_INDEX: return "";
        case SIZE_INDEX: return baseObject.getSize();
        case HIT_INDEX: return baseObject.getNumHits();
        case UPLOAD_ATTEMPTS_INDEX: return baseObject.getNumUploadAttempts();
        case UPLOADS_INDEX: return baseObject.getNumUploads();
        case ACTION_INDEX: return baseObject;
        }
        throw new IllegalArgumentException("Unknown column:" + column);
    }
}
