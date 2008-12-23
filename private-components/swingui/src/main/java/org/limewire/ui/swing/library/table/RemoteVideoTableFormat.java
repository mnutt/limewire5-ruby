package org.limewire.ui.swing.library.table;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.table.ColumnStateInfo;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.FileUtils;

/**
 * Table format for the Video Table for LW buddies and Browse hosts
 */
public class RemoteVideoTableFormat<T extends RemoteFileItem> extends AbstractRemoteLibraryFormat<T> {
    static final int NAME_INDEX = 0;
    static final int EXTENSION_INDEX = 1;
    static final int LENGTH_INDEX = 2;
    static final int QUALITY_INDEX = 3;
    static final int SIZE_INDEX = 4;
    static final int MISC_INDEX = 5;
    static final int YEAR_INDEX = 6;
    static final int RATING_INDEX = 7;
    static final int DIMENSION_INDEX = 8;
    static final int DESCRIPTION_INDEX = 9;

    public RemoteVideoTableFormat() {
        super(new ColumnStateInfo[] {
                new ColumnStateInfo(NAME_INDEX, "REMOTE_LIBRARY_VIDEO_NAME", I18n.tr("Name"), 260, true, true),     
                new ColumnStateInfo(EXTENSION_INDEX, "REMOTE_LIBRARY_VIDEO_EXTENSION", I18n.tr("Extension"), 60, true, true), 
                new ColumnStateInfo(LENGTH_INDEX, "REMOTE_LIBRARY_VIDEO_LENGTH", I18n.tr("Length"), 80, true, true), 
                new ColumnStateInfo(QUALITY_INDEX, "REMOTE_LIBRARY_VIDEO_QUALITY", I18n.tr("Quality"), 60, true, true), 
                new ColumnStateInfo(SIZE_INDEX, "REMOTE_LIBRARY_VIDEO_SIZE", I18n.tr("Size"), 60, true, true),
                new ColumnStateInfo(MISC_INDEX, "REMOTE_LIBRARY_VIDEO_MISC", I18n.tr("Misc"), 120, false, true), 
                new ColumnStateInfo(YEAR_INDEX, "REMOTE_LIBRARY_VIDEO_YEAR", I18n.tr("Year"), 60, false, true), 
                new ColumnStateInfo(RATING_INDEX, "REMOTE_LIBRARY_VIDEO_RATING", I18n.tr("Rating"), 60, false, true),
                new ColumnStateInfo(DIMENSION_INDEX, "REMOTE_LIBRARY_VIDEO_DIMENSION", I18n.tr("Resolution"), 80, false, true), 
                new ColumnStateInfo(DESCRIPTION_INDEX, "REMOTE_LIBRARY_VIDEO_DESCRIPTION", I18n.tr("Description"), 100, false, true) 
        });
    }

    @Override
    public Object getColumnValue(T baseObject, int column) {
        switch(column) {
            case NAME_INDEX: return baseObject.getName();
            case EXTENSION_INDEX: return FileUtils.getFileExtension(baseObject.getFileName());
            case LENGTH_INDEX: return baseObject.getProperty(FilePropertyKey.LENGTH);
            case MISC_INDEX: return baseObject.getProperty(FilePropertyKey.COMMENTS);
            case QUALITY_INDEX: return "";
            case YEAR_INDEX: return baseObject.getProperty(FilePropertyKey.YEAR);
            case RATING_INDEX: return baseObject.getProperty(FilePropertyKey.RATING);
            case SIZE_INDEX: return baseObject.getSize();
            case DESCRIPTION_INDEX: return baseObject.getProperty(FilePropertyKey.COMMENTS);
            case DIMENSION_INDEX:
                if(baseObject.getProperty(FilePropertyKey.WIDTH) == null || baseObject.getProperty(FilePropertyKey.HEIGHT) == null)
                    return null;
                else
                    return baseObject.getProperty(FilePropertyKey.WIDTH) + " X " + baseObject.getProperty(FilePropertyKey.HEIGHT);
        }
        throw new IllegalArgumentException("Unknown column:" + column);
    }
}