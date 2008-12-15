package org.limewire.ui.swing.library.table;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.FileItem;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.FileUtils;

/**
 * Table format for Programs Table for LW buddies and Browse hosts
 */
public class RemoteProgramTableFormat<T extends FileItem> extends AbstractRemoteLibraryFormat<T> {
    public static enum Columns {
        NAME(I18n.tr("Filename"), true, true, 400),
        EXTENSION(I18n.tr("Extension"), true, true, 60),
        SIZE(I18n.tr("Size"), true, true, 60),
        PLATFORM(I18n.tr("Platform"), true, true, 120),
        COMPANY(I18n.tr("Company"), true, true, 120),
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
    public Object getColumnValue(FileItem baseObject, int column) {
        Columns other = Columns.values()[column];
        switch(other) {
            case NAME: return baseObject;
            case PLATFORM: return baseObject.getProperty(FilePropertyKey.PLATFORM);
            case COMPANY: return baseObject.getProperty(FilePropertyKey.COMPANY);
            case SIZE: return baseObject.getSize();
            case EXTENSION: return FileUtils.getFileExtension(baseObject.getFileName());
            case DESCRIPTION: return baseObject.getProperty(FilePropertyKey.COMMENTS);
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