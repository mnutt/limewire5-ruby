package org.limewire.ui.swing.library.table;

import java.util.Comparator;
import java.util.Date;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.FileItem;
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
                new ColumnStateInfo(ACTION_INDEX, "SHARED_LIBRARY_DOCUMENT_ACTION", I18n.tr("Sharing"), 50, true, false),
                new ColumnStateInfo(NAME_INDEX, "SHARED_LIBRARY_DOCUMENT_NAME", "Name", 160, true, true), 
                new ColumnStateInfo(TYPE_INDEX, "SHARED_LIBRARY_DOCUMENT_TYPE", I18n.tr("Type"), 80, true, true),     
                new ColumnStateInfo(CREATED_INDEX, "SHARED_LIBRARY_DOCUMENT_CREATED", I18n.tr("Date Created"), 100, true, true), 
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
         case TYPE_INDEX: return baseObject.getProperty(FilePropertyKey.TOPIC);
         }
         throw new IllegalArgumentException("Unknown column:" + column);
    }
    
    @Override
    public Comparator getColumnComparator(int column) {
        switch(column) {
            case ACTION_INDEX: return new CheckBoxComparator();
        }
        return super.getColumnComparator(column);
    }
    
    /**
     * Creates a Comparator for sorting checkboxs.
     */
    private class CheckBoxComparator implements Comparator<FileItem> {
        @Override
        public int compare(FileItem o1, FileItem o2) {
            boolean isShared1 = localFileList.contains(o1.getUrn());
            boolean isShared2 = localFileList.contains(o2.getUrn());

            if(isShared1 && isShared2) {
                return 0;
            } else if(isShared1 && !isShared2) {
                return 1;
            } else {
                return -1;
            }
        }
    }
}