package org.limewire.ui.swing.library.table;

import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.table.ColumnStateInfo;
import org.limewire.ui.swing.util.I18n;

/**
 * Table format for the Document Table for All Friends table
 */
public class AllFriendDocumentTableFormat <T extends RemoteFileItem> extends RemoteDocumentTableFormat<T> {
    static final int FROM_INDEX = 7;
    
    public AllFriendDocumentTableFormat() {
        super("ALL_LIBRARY_DOCUMENT_TABLE", NAME_INDEX, true, new ColumnStateInfo[] {
                new ColumnStateInfo(NAME_INDEX, "ALL_LIBRARY_DOCUMENT_NAME", I18n.tr("Name"), 318, true, true), 
                new ColumnStateInfo(TYPE_INDEX, "ALL_LIBRARY_DOCUMENT_TYPE", I18n.tr("Type"), 118, true, true),     
                new ColumnStateInfo(EXTENSION_INDEX, "ALL_LIBRARY_DOCUMENT_EXTENSION", I18n.tr("Extension"), 42, true, true), 
                new ColumnStateInfo(CREATED_INDEX, "ALL_LIBRARY_DOCUMENT_CREATED", I18n.tr("Date Created"), 100, false, true), 
                new ColumnStateInfo(SIZE_INDEX, "ALL_LIBRARY_DOCUMENT_SIZE", I18n.tr("Size"), 24, true, true),
                new ColumnStateInfo(AUTHOR_INDEX, "ALL_LIBRARY_DOCUMENT_AUTHOR", I18n.tr("Author"), 120, false, true), 
                new ColumnStateInfo(DESCRIPTION_INDEX, "ALL_LIBRARY_DOCUMENT_DESCRIPTION", I18n.tr("Description"), 120, false, false),
                new ColumnStateInfo(FROM_INDEX, "ALL_LIBRARY_DOCUMENT_FROM", I18n.tr("From"), 32, true, true)
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
