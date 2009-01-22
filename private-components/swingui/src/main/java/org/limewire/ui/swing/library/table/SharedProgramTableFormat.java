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
 * Table format for the Program Table when it is in Sharing View
 */
public class SharedProgramTableFormat<T extends LocalFileItem> extends AbstractMyLibraryFormat<T> {    
    static final int ACTION_INDEX = 0;
    static final int NAME_INDEX = 1;
    static final int SIZE_INDEX = 2;
    static final int PLATFORM_INDEX = 3;
    static final int COMPANY_INDEX = 4;
    static final int DESCRIPTION_INDEX = 5;

    private final LocalFileList localFileList;
    
    public SharedProgramTableFormat(LocalFileList localFileList) {
        super(ACTION_INDEX, new ColumnStateInfo[] {
                new ColumnStateInfo(ACTION_INDEX, "SHARED_LIBRARY_PROGRAM_ACTION", I18n.tr("Sharing"), 61, true, false),
                new ColumnStateInfo(NAME_INDEX, "SHARED_LIBRARY_PROGRAM_NAME", I18n.tr("Name"), 493, true, true), 
                new ColumnStateInfo(SIZE_INDEX, "SHARED_LIBRARY_PROGRAM_SIZE", I18n.tr("Size"), 60, false, true),
                new ColumnStateInfo(PLATFORM_INDEX, "SHARED_LIBRARY_PROGRAM_PLATFORM", I18n.tr("Platform"), 120, false, true), 
                new ColumnStateInfo(COMPANY_INDEX, "SHARED_LIBRARY_PROGRAM_COMPANY", I18n.tr("Company"), 180, true, true), 
                new ColumnStateInfo(DESCRIPTION_INDEX, "SHARED_LIBRARY_PROGRAM_DESCRIPTION", I18n.tr("Description"), 120, false, true)
        });
        this.localFileList = localFileList;
    }
    
    @Override
    public Object getColumnValue(LocalFileItem baseObject, int column) {
        switch(column) {
        case NAME_INDEX: return baseObject;
        case PLATFORM_INDEX: return baseObject.getProperty(FilePropertyKey.PLATFORM);
        case COMPANY_INDEX: return baseObject.getProperty(FilePropertyKey.COMPANY);
        case SIZE_INDEX: return baseObject.getSize();
        case DESCRIPTION_INDEX: return "";
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
