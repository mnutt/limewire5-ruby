package org.limewire.ui.swing.library.table;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.table.ColumnStateInfo;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.FileUtils;

/**
 * Table format for the Document Table for LW buddies and Browse hosts
 */
public class RemoteDocumentTableFormat<T extends RemoteFileItem> extends AbstractRemoteLibraryFormat<T> {
    static final int NAME_INDEX = 0;
    static final int TYPE_INDEX = 1;
    static final int EXTENSION_INDEX = 2;
    static final int SIZE_INDEX = 3;
    static final int CREATED_INDEX = 4;
    static final int AUTHOR_INDEX = 5;
    static final int DESCRIPTION_INDEX = 6;
    
    public RemoteDocumentTableFormat() {
        super(new ColumnStateInfo[] {
                new ColumnStateInfo(NAME_INDEX, "REMOTE_LIBRARY_DOCUMENT_NAME", I18n.tr("Name"), 250, true, true), 
                new ColumnStateInfo(TYPE_INDEX, "REMOTE_LIBRARY_DOCUMENT_TYPE", I18n.tr("Type"), 80, true, true),     
                new ColumnStateInfo(EXTENSION_INDEX, "REMOTE_LIBRARY_DOCUMENT_EXTENSION", I18n.tr("Extension"), 60, true, true), 
                new ColumnStateInfo(CREATED_INDEX, "REMOTE_LIBRARY_DOCUMENT_CREATED", I18n.tr("Date Created"), 100, true, true), 
                new ColumnStateInfo(SIZE_INDEX, "REMOTE_LIBRARY_DOCUMENT_SIZE", I18n.tr("Size"), 60, true, true),
                new ColumnStateInfo(AUTHOR_INDEX, "REMOTE_LIBRARY_DOCUMENT_AUTHOR", I18n.tr("Author"), 120, false, true), 
                new ColumnStateInfo(DESCRIPTION_INDEX, "REMOTE_LIBRARY_DOCUMENT_DESCRIPTION", I18n.tr("Description"), 120, false, false)
        });
    }

    @Override
    public Object getColumnValue(T baseObject, int column) {
        switch(column) {
             case AUTHOR_INDEX: return baseObject.getProperty(FilePropertyKey.AUTHOR);
             case CREATED_INDEX: return baseObject.getCreationTime();
             case NAME_INDEX: return baseObject;
             case SIZE_INDEX: return baseObject.getSize();
             case TYPE_INDEX: return baseObject.getProperty(FilePropertyKey.TOPIC);  
             case EXTENSION_INDEX: return FileUtils.getFileExtension(baseObject.getFileName());
             case DESCRIPTION_INDEX: return "";
         }
         throw new IllegalArgumentException("Unknown column:" + column);
    }
}