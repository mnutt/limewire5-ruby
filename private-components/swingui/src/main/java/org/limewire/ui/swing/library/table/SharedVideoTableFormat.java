package org.limewire.ui.swing.library.table;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jdesktop.swingx.decorator.SortKey;
import org.jdesktop.swingx.decorator.SortOrder;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.table.ColumnStateInfo;
import org.limewire.ui.swing.util.I18n;

/**
 * Table format for the Video Table when it is in Sharing View
 */
public class SharedVideoTableFormat<T extends LocalFileItem> extends AbstractMyLibraryFormat<T> {
    static final int ACTION_INDEX = 0;
    static final int NAME_INDEX = 1;
    static final int LENGTH_INDEX = 2;
    static final int MISC_INDEX = 3;
    static final int YEAR_INDEX = 4;
    static final int SIZE_INDEX = 5;
    static final int RATING_INDEX = 6;
    static final int DIMENSION_INDEX = 7;
    static final int DESCRIPTION_INDEX = 8;
    static final int GENRE_INDEX = 9;
    
    private final LocalFileList localFileList;
    
    public SharedVideoTableFormat(LocalFileList localFileList) {
        super(ACTION_INDEX, new ColumnStateInfo[] {
                new ColumnStateInfo(ACTION_INDEX, "SHARED_LIBRARY_VIDEO_ACTION", I18n.tr("Sharing"), 61, true, false),
                new ColumnStateInfo(NAME_INDEX, "SHARED_LIBRARY_VIDEO_NAME", I18n.tr("Name"), 611, true, true), 
                new ColumnStateInfo(LENGTH_INDEX, "SHARED_LIBRARY_VIDEO_LENGTH", I18n.tr("Length"), 62, true, true), 
                new ColumnStateInfo(MISC_INDEX, "SHARED_LIBRARY_VIDEO_MISC", I18n.tr("Misc"), 100, false, true), 
                new ColumnStateInfo(YEAR_INDEX, "SHARED_LIBRARY_VIDEO_YEAR", I18n.tr("Year"), 80, false, true), 
                new ColumnStateInfo(SIZE_INDEX, "SHARED_LIBRARY_VIDEO_SIZE", I18n.tr("Size"), 60, false, true),
                new ColumnStateInfo(RATING_INDEX, "SHARED_LIBRARY_VIDEO_RATING", I18n.tr("Rating"), 60, false, true), 
                new ColumnStateInfo(DIMENSION_INDEX, "SHARED_LIBRARY_VIDEO_RESOLUTION", I18n.tr("Resolution"), 80, false, true), 
                new ColumnStateInfo(DESCRIPTION_INDEX, "SHARED_LIBRARY_VIDEO_DESCRIPTION", I18n.tr("Description"), 100, false, true),
                new ColumnStateInfo(GENRE_INDEX, "SHARED_LIBRARY_VIDEO_GENRE", I18n.tr("Genre"), 80, false, true) 
        });
        this.localFileList = localFileList;
    }
    
    @Override
    public Object getColumnValue(T baseObject, int column) {
        switch(column) {
        case NAME_INDEX: return baseObject;
        case LENGTH_INDEX: return baseObject.getProperty(FilePropertyKey.LENGTH);
        case MISC_INDEX: return baseObject;
        case YEAR_INDEX: return baseObject.getProperty(FilePropertyKey.YEAR);
        case RATING_INDEX: return baseObject.getProperty(FilePropertyKey.RATING);
        case SIZE_INDEX: return baseObject.getSize();
        case DIMENSION_INDEX: 
            if(baseObject.getProperty(FilePropertyKey.WIDTH) == null || baseObject.getProperty(FilePropertyKey.HEIGHT) == null)
                return null;
            else
                return baseObject.getProperty(FilePropertyKey.WIDTH) + " X " + baseObject.getProperty(FilePropertyKey.HEIGHT); 
        case DESCRIPTION_INDEX: return baseObject.getProperty(FilePropertyKey.DESCRIPTION);
        case ACTION_INDEX: return baseObject;
        case GENRE_INDEX: return baseObject.getProperty(FilePropertyKey.GENRE);
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
    
    @Override
    public List<SortKey> getDefaultSortKeys() {
        return Arrays.asList(
                new SortKey(SortOrder.ASCENDING, NAME_INDEX),
                new SortKey(SortOrder.ASCENDING, SIZE_INDEX));
    }

    @Override
    public List<Integer> getSecondarySortColumns(int column) {
        switch (column) {
        case NAME_INDEX:
            return Arrays.asList(SIZE_INDEX);
        case SIZE_INDEX:
            return Arrays.asList(NAME_INDEX);
        default:
            return Collections.emptyList();
        }
    }
}
