package org.limewire.ui.swing.library.table;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.jdesktop.swingx.decorator.SortKey;
import org.jdesktop.swingx.decorator.SortOrder;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.table.ColumnStateInfo;
import org.limewire.ui.swing.util.I18n;

/**
 * Table format for the Document Table when it is in Sharing View
 */
public class SharedDocumentTableFormat<T extends LocalFileItem> extends AbstractMyLibraryFormat<T> {    
    static final int ACTION_INDEX = 0;
    static final int NAME_INDEX = 1;
    static final int TYPE_INDEX = 2;
    static final int CREATED_INDEX = 3;
    static final int SIZE_INDEX = 4;
    static final int AUTHOR_INDEX = 5;
    static final int DESCRIPTION_INDEX = 6;

    private final LocalFileList localFileList;
    
    public SharedDocumentTableFormat(LocalFileList localFileList) {
        super(ACTION_INDEX, new ColumnStateInfo[] {
                new ColumnStateInfo(ACTION_INDEX, "SHARED_LIBRARY_DOCUMENT_ACTION", I18n.tr("Sharing"), 61, true, false),
                new ColumnStateInfo(NAME_INDEX, "SHARED_LIBRARY_DOCUMENT_NAME", "Name", 493, true, true), 
                new ColumnStateInfo(TYPE_INDEX, "SHARED_LIBRARY_DOCUMENT_TYPE", I18n.tr("Type"), 180, true, true),     
                new ColumnStateInfo(CREATED_INDEX, "SHARED_LIBRARY_DOCUMENT_CREATED", I18n.tr("Date Created"), 100, false, true), 
                new ColumnStateInfo(SIZE_INDEX, "SHARED_LIBRARY_DOCUMENT_SIZE", I18n.tr("Size"), 60, false, true),
                new ColumnStateInfo(AUTHOR_INDEX, "SHARED_LIBRARY_DOCUMENT_AUTHOR", I18n.tr("Author"), 60, false, true), 
                new ColumnStateInfo(DESCRIPTION_INDEX, "SHARED_LIBRARY_DOCUMENT_DESCRIPTION", I18n.tr("Description"), 100, false, true) 
        });
        this.localFileList = localFileList;
    }
    
    @Override
    public Object getColumnValue(T baseObject, int column) {
        switch(column) {
         case AUTHOR_INDEX: return baseObject.getProperty(FilePropertyKey.AUTHOR);
         case CREATED_INDEX:
             // Return creation time if valid.
             long creationTime = baseObject.getCreationTime();
             return (creationTime >= 0) ? new Date(creationTime) : null;
         case DESCRIPTION_INDEX: return "";
         case NAME_INDEX: return baseObject;
         case ACTION_INDEX: return baseObject;
         case SIZE_INDEX: return baseObject.getSize();
         case TYPE_INDEX: return baseObject.getProperty(FilePropertyKey.DESCRIPTION);
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
                new SortKey(SortOrder.ASCENDING, TYPE_INDEX),
                new SortKey(SortOrder.ASCENDING, SIZE_INDEX));
    }

    @Override
    public List<Integer> getSecondarySortColumns(int column) {
        switch (column) {
        case NAME_INDEX:
            return Arrays.asList(TYPE_INDEX, SIZE_INDEX);
        case TYPE_INDEX:
            return Arrays.asList(NAME_INDEX, SIZE_INDEX);
        case SIZE_INDEX:
            return Arrays.asList(NAME_INDEX, TYPE_INDEX);
        default:
            return Collections.emptyList();
        }
    }
}