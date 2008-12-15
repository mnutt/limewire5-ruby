package org.limewire.ui.swing.library.table;

import java.util.Comparator;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.PropertyUtils;

/**
 * Table format for the Audio Table when it is in My Library
 */
public class AudioTableFormat<T extends LocalFileItem> extends AbstractMyLibraryFormat<T> {
    
    public static enum Columns {
        PLAY("", true, true, 25),
        TITLE(I18n.tr("Name"), true, true, 260),
        ARTIST(I18n.tr("Artist"), true, true, 120),
        ALBUM(I18n.tr("Album"), true, true, 180),
        LENGTH(I18n.tr("Length"), true, true, 60),
        GENRE(I18n.tr("Genre"), false, true, 60),
        BITRATE(I18n.tr("Bitrate"), false, true, 50),
        SIZE(I18n.tr("Size"), false, true, 50),
        FILENAME(I18n.tr("Filename"), false, true, 100),
        TRACK(I18n.tr("Track"), false, true, 50),
        YEAR(I18n.tr("Year"), false, true, 50),
        QUALITY(I18n.tr("Quality"), false, true, 60),
        DESCRIPTION(I18n.tr("Description"), false, true, 100),
        ACTION(I18n.tr("Sharing"), true, false, 50);
     
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
        case PLAY: return baseObject;
        case TITLE:
            if(baseObject.getProperty(FilePropertyKey.TITLE) == null)
                return baseObject.getProperty(FilePropertyKey.NAME);
            else
                return baseObject.getProperty(FilePropertyKey.TITLE);
        case ARTIST: return baseObject.getProperty(FilePropertyKey.AUTHOR);
        case ALBUM: return baseObject.getProperty(FilePropertyKey.ALBUM);
        case LENGTH: return baseObject.getProperty(FilePropertyKey.LENGTH);
        case GENRE: return baseObject.getProperty(FilePropertyKey.GENRE);
        case BITRATE: return baseObject.getProperty(FilePropertyKey.BITRATE);
        case FILENAME: return baseObject.getFileName();
        case SIZE: return baseObject.getSize();
        case TRACK: return baseObject.getProperty(FilePropertyKey.TRACK_NUMBER);
        case YEAR: return baseObject.getProperty(FilePropertyKey.YEAR);
        case QUALITY: return "";
        case DESCRIPTION: return baseObject.getProperty(FilePropertyKey.COMMENTS);
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
            case ACTION:
            case PLAY:
                return FileItem.class;
        }
        return super.getColumnClass(column);
    }

    @Override
    public Comparator getColumnComparator(int column) {
        Columns other = Columns.values()[column];
        switch(other) {
            case PLAY: return new NameComparator();
            case ACTION: return new ActionComparator();
        }
        return super.getColumnComparator(column);
    }
    
    /**
     * Compares the title field in the NAME_COLUMN
     */
    private class NameComparator implements Comparator<FileItem> {
        @Override
        public int compare(FileItem o1, FileItem o2) {
            String title1 = PropertyUtils.getTitle(o1);
            String title2 = PropertyUtils.getTitle(o2);
            
            return title1.toLowerCase().compareTo(title2.toLowerCase());
        }
    }
}
