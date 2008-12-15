package org.limewire.ui.swing.library.table;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.FileUtils;

/**
 * Table format for the Document Table for LW buddies and Browse hosts
 */
public class RemoteDocumentTableFormat<T extends RemoteFileItem> extends AbstractRemoteLibraryFormat<T> {
    public static enum Columns {
        NAME(I18n.tr("Filename"), true, true, 250),
        TYPE(I18n.tr("Type"), true, true, 80),
        EXTENSION(I18n.tr("Extension"), true, true, 60),
        SIZE(I18n.tr("Size"), true, true, 60),
        CREATED(I18n.tr("Date Created"), true, true, 100),
        AUTHOR(I18n.tr("Author"), false, true, 120),
        DESCRIPTION(I18n.tr("Description"), false, true, 120);
        
        private final String columnName;
        private final boolean isShown;
        private final boolean isHideable;
        private final int initialWidth;
        
        Columns(String name, boolean isShown, boolean isHideable, int initialWidth) {
            this.columnName = name;
            this.isShown = isShown;
            this.isHideable = isHideable;
            this.initialWidth = initialWidth;
        }
        
        public String getColumnName() { return columnName; }
        public boolean isShown() { return isShown; }        
        public boolean isHideable() { return isHideable; }
        public int getInitialWidth() { return initialWidth; }
    }
    
    @Override
    public int getColumnCount() {
        return Columns.values().length;
    }

    @Override
    public String getColumnName(int column) {
        return Columns.values()[column].getColumnName();
    }

    @Override
    public Object getColumnValue(T baseObject, int column) {
        Columns other = Columns.values()[column];
        switch(other) {
             case AUTHOR: return baseObject.getProperty(FilePropertyKey.AUTHOR);
             case CREATED: return baseObject.getCreationTime();
             case NAME: return baseObject;
             case SIZE: return baseObject.getSize();
             case TYPE: return baseObject.getProperty(FilePropertyKey.TOPIC);  
             case EXTENSION: return FileUtils.getFileExtension(baseObject.getFileName());
             case DESCRIPTION: return "";
         }
         throw new IllegalArgumentException("Unknown column:" + column);
    }

    @Override
    public int getActionColumn() {
        return -1;
    }
    
    @Override
    public boolean isColumnHiddenAtStartup(int column) {
        return Columns.values()[column].isShown();
    }
    
    @Override
    public boolean isColumnHideable(int column) {
        return Columns.values()[column].isHideable();
    }

    @Override
    public int getInitialWidth(int column) {
        return Columns.values()[column].getInitialWidth();
    }

    @Override
    public boolean isEditable(T baseObject, int column) {
        return false;
    }

    @Override
    public T setColumnValue(T baseObject, Object editedValue, int column) {
        return baseObject;
    }
}