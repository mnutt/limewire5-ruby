package org.limewire.ui.swing.library.table;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.FileUtils;

/**
 * Table format for the Video Table for LW buddies and Browse hosts
 */
public class RemoteVideoTableFormat<T extends RemoteFileItem> extends AbstractRemoteLibraryFormat<T> {
    public static enum Columns {
        NAME(I18n.tr("Filename"), true, true, 260),
        EXTENSION(I18n.tr("Extension"), true, true, 60),
        LENGTH(I18n.tr("Length"), true, true, 80),
        MISC(I18n.tr("Misc"), true, true, 120),
        QUALITY(I18n.tr("Quality"), true, true, 60),
        SIZE(I18n.tr("Size"), true, true, 60),
        YEAR(I18n.tr("Year"), false, false, 60),
        RATING(I18n.tr("Rating"), false, true, 60),
        DIMENSION(I18n.tr("Resolution"), false, true, 80),
        DESCRIPTION(I18n.tr("Description"), false, true, 100),;
        
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
            case NAME: return baseObject.getName();
            case EXTENSION: return FileUtils.getFileExtension(baseObject.getFileName());
            case LENGTH: return baseObject.getProperty(FilePropertyKey.LENGTH);
            case MISC: return baseObject.getProperty(FilePropertyKey.COMMENTS);
            case QUALITY: return "";
            case YEAR: return baseObject.getProperty(FilePropertyKey.YEAR);
            case RATING: return baseObject.getProperty(FilePropertyKey.RATING);
            case SIZE: return baseObject.getSize();
            case DESCRIPTION: return baseObject.getProperty(FilePropertyKey.COMMENTS);
            case DIMENSION:
                if(baseObject.getProperty(FilePropertyKey.WIDTH) == null || baseObject.getProperty(FilePropertyKey.HEIGHT) == null)
                    return null;
                else
                    return baseObject.getProperty(FilePropertyKey.WIDTH) + " X " + baseObject.getProperty(FilePropertyKey.HEIGHT);
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