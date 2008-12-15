package org.limewire.ui.swing.library.table;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.FileUtils;

/**
 * Table format for the Audio Table for LW buddies and Browse hosts
 */
public class RemoteAudioTableFormat<T extends RemoteFileItem> extends AbstractRemoteLibraryFormat<T> {
    public static enum Columns {
        NAME(I18n.tr("Name"), true, true, 260),
        ARTIST(I18n.tr("Artist"), true, true, 120),
        ALBUM(I18n.tr("Album"), true, true, 180),
        LENGTH(I18n.tr("Length"), true, true, 60),
        QUALITY(I18n.tr("Quality"), true, true, 60),
        GENRE(I18n.tr("Genre"), false, true, 60),
        BITRATE(I18n.tr("Bitrate"), false, true, 50),
        SIZE(I18n.tr("Size"), false, true, 60),
        TRACK(I18n.tr("Track"), false, true, 50),
        YEAR(I18n.tr("Year"), false, true, 50),
        FILENAME(I18n.tr("Filename"), false, true, 120),
        EXTENSION(I18n.tr("Extension"), false, true, 60),
        DESCRIPTION(I18n.tr("Description"), false, true, 100);
        
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
            case NAME: return (baseObject.getProperty(FilePropertyKey.TITLE) == null) ? baseObject.getProperty(FilePropertyKey.NAME) : baseObject.getProperty(FilePropertyKey.TITLE);
            case ARTIST: return baseObject.getProperty(FilePropertyKey.AUTHOR);
            case ALBUM: return baseObject.getProperty(FilePropertyKey.ALBUM);
            case LENGTH: return baseObject.getProperty(FilePropertyKey.LENGTH);
            case QUALITY: return "";
            case GENRE: return baseObject.getProperty(FilePropertyKey.GENRE);
            case BITRATE: return baseObject.getProperty(FilePropertyKey.BITRATE);
            case SIZE: return baseObject.getSize();
            case TRACK: return baseObject.getProperty(FilePropertyKey.TRACK_NUMBER);
            case YEAR: return baseObject.getProperty(FilePropertyKey.YEAR);
            case FILENAME: return baseObject.getProperty(FilePropertyKey.NAME);
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
    public T setColumnValue(T baseObject, Object editedValue, int column) {
        return baseObject;
    }

    @Override
    public boolean isEditable(T baseObject, int column) {
        return false;
    }
}
