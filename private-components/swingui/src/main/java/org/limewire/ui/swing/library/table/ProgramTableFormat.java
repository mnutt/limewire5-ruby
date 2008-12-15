package org.limewire.ui.swing.library.table;

import java.util.Comparator;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.util.I18n;

/**
 * Table format for the Program Table when it is in My Library
 */
public class ProgramTableFormat<T extends LocalFileItem> extends AbstractMyLibraryFormat<T> {
    public static enum Columns {
        NAME(I18n.tr("Filename"), true, true, 250),
        SIZE(I18n.tr("Size"), false, true, 60),
        PLATFORM(I18n.tr("Type"), false, true, 120),
        COMPANY(I18n.tr("Size"), true, true, 120),
        DESCRIPTION(I18n.tr("Type"), false, true,120),
        ACTION(I18n.tr("Sharing"), true, false, 60);
        
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
    public Object getColumnValue(LocalFileItem baseObject, int column) {
        Columns other = Columns.values()[column];
        switch(other) {
        case NAME: return baseObject;
        case PLATFORM: return baseObject.getProperty(FilePropertyKey.PLATFORM);
        case COMPANY: return baseObject.getProperty(FilePropertyKey.COMPANY);
        case SIZE: return baseObject.getSize();
        case DESCRIPTION: return "";
        case ACTION: return baseObject;
        }
        throw new IllegalArgumentException("Unknown column:" + column);
    }

    @Override
    public int getActionColumn() {
        return Columns.ACTION.ordinal();
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
        return column == Columns.ACTION.ordinal();
    }

    @Override
    public T setColumnValue(T baseObject, Object editedValue, int column) {
        return baseObject;
    }
    
    @Override
    public Class getColumnClass(int column) {
        Columns other = Columns.values()[column];
        switch(other) {
            case ACTION: return FileItem.class;
        }
        return super.getColumnClass(column);
    }

    @Override
    public Comparator getColumnComparator(int column) {
        Columns other = Columns.values()[column];
        switch(other) {
            case ACTION: return new ActionComparator();
        }
        return super.getColumnComparator(column);
    }
}
