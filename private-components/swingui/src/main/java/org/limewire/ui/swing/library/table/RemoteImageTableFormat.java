package org.limewire.ui.swing.library.table;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jdesktop.swingx.decorator.SortKey;
import org.jdesktop.swingx.decorator.SortOrder;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.settings.TablesHandler;
import org.limewire.ui.swing.table.ColumnStateInfo;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.FileUtils;

/**
 * Table format for the Image Table for LW buddies and Browse hosts
 */
public class RemoteImageTableFormat<T extends RemoteFileItem> extends AbstractRemoteLibraryFormat<T> {
    static final int NAME_INDEX = 0;
    static final int EXTENSION_INDEX = 1;
    static final int CREATED_INDEX = 2;
    static final int SIZE_INDEX = 3;
    static final int TITLE_INDEX = 4;
    static final int DESCRIPTION_INDEX = 5;
    
    public RemoteImageTableFormat(String sortID, int sortedColumn, boolean isAscending, ColumnStateInfo[] columnInfo) {
        super(sortID, sortedColumn, isAscending, columnInfo);
    }
    
    public RemoteImageTableFormat() {
        super("REMOTE_LIBRARY_IMAGE_TABLE", NAME_INDEX, true, new ColumnStateInfo[] {
                new ColumnStateInfo(NAME_INDEX, "REMOTE_LIBRARY_IMAGE_NAME", I18n.tr("Name"), 642, true, true),     
                new ColumnStateInfo(EXTENSION_INDEX, "REMOTE_LIBRARY_IMAGE_EXTENSION", I18n.tr("Extension"), 89, true, true), 
                new ColumnStateInfo(CREATED_INDEX, "REMOTE_LIBRARY_IMAGE_CREATED", I18n.tr("Date Created"), 100, false, true), 
                new ColumnStateInfo(SIZE_INDEX, "REMOTE_LIBRARY_IMAGE_SIZE", I18n.tr("Size"), 63, true, true), 
                new ColumnStateInfo(TITLE_INDEX, "REMOTE_LIBRARY_IMAGE_TITLE", I18n.tr("Title"), 120, false, true), 
                new ColumnStateInfo(DESCRIPTION_INDEX, "REMOTE_LIBRARY_IMAGE_DESCRIPTION", I18n.tr("Description"), 150, false, true)
        });
    }

    @Override
    public Object getColumnValue(T baseObject, int column) {
        switch(column) {
            case NAME_INDEX: return baseObject;
            case SIZE_INDEX: return baseObject.getSize();
            case CREATED_INDEX: return baseObject.getCreationTime();
            case EXTENSION_INDEX: return FileUtils.getFileExtension(baseObject.getFileName());
            case TITLE_INDEX: return baseObject.getProperty(FilePropertyKey.TITLE);
            case DESCRIPTION_INDEX: return baseObject.getProperty(FilePropertyKey.DESCRIPTION);
        }
        throw new IllegalArgumentException("Unknown column:" + column);
    }

    @Override
    public List<SortKey> getDefaultSortKeys() {
        if(TablesHandler.getSortedColumn(getSortOrderID(), getSortedColumn()).getValue() == getSortedColumn() &&
                TablesHandler.getSortedOrder(getSortOrderID(), getSortOrder()).getValue() == getSortOrder())
            return Arrays.asList(
                    new SortKey(SortOrder.ASCENDING, CREATED_INDEX),
                    new SortKey(SortOrder.ASCENDING, NAME_INDEX));
        else
            return super.getDefaultSortKeys();
    }

    @Override
    public List<Integer> getSecondarySortColumns(int column) {
        switch (column) {
        case NAME_INDEX:
            return Arrays.asList(CREATED_INDEX);
        case CREATED_INDEX:
            return Arrays.asList(NAME_INDEX);
        default:
            return Collections.emptyList();
        }
    }
}
