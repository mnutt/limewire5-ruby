package org.limewire.ui.swing.search.resultpanel.classic;

import java.util.Comparator;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.ResultsTableFormat;
import org.limewire.ui.swing.util.I18n;

/**
 * This class specifies the content of a table that contains
 * music track descriptions.
 */
public class AudioTableFormat extends ResultsTableFormat<VisualSearchResult> {

    public static enum Columns {
        FROM(I18n.tr("From"), true, 55),
        TITLE(I18n.tr("Name"), true, 550),
        ARTIST(I18n.tr("Artist"), true, 80),
        ALBUM(I18n.tr("Album"), false, 60),
        LENGTH(I18n.tr("Length"), true, 60),
        QUALITY(I18n.tr("Quality"), true, 55),
        BITRATE(I18n.tr("Bitrate"), false, 550),
        GENRE(I18n.tr("Genre"), false, 80),
        TRACK(I18n.tr("Track"), false, 60),
        YEAR(I18n.tr("Year"), false, 60),
        NAME(I18n.tr("Filename"), false, 55),
        EXTENSION(I18n.tr("Extension"), false, 550),
        SIZE(I18n.tr("Size"), true, 80),
        DESCRIPTION(I18n.tr("Description"), false, 60);
        
        private final String columnName;
        private boolean isShown;
        private int initialWidth;
        
        Columns(String name, boolean isShown, int initialWidth) {
            this.columnName = name;
            this.isShown = isShown;
            this.initialWidth = initialWidth;
        }
        
        public String getColumnName() { return columnName; }
        public boolean isShown() { return isShown; }
        public int getInitialWidth() { return initialWidth; }
    }
    
    @Override
    public Class getColumnClass(int column) {
        Columns other = Columns.values()[column];
        switch(other) {
        case BITRATE: return Integer.class;
        case TRACK: return Integer.class;
        case FROM: return VisualSearchResult.class;
        }
        return super.getColumnClass(column);
    }

    public Object getColumnValue(VisualSearchResult vsr, int column) {
        this.vsr = vsr;
        Columns other = Columns.values()[column];
        switch(other) {
            case FROM: return vsr;
            case TITLE: return (getProperty(FilePropertyKey.TITLE) == null) ? getProperty(FilePropertyKey.NAME) : getProperty(FilePropertyKey.TITLE);
            case ARTIST: return getProperty(FilePropertyKey.AUTHOR);
            case ALBUM: return getProperty(FilePropertyKey.ALBUM);
            case LENGTH: return getProperty(FilePropertyKey.LENGTH);
            case QUALITY: return getProperty(FilePropertyKey.QUALITY);
            case BITRATE: return getProperty(FilePropertyKey.BITRATE);
            case GENRE: return getProperty(FilePropertyKey.GENRE);
            case TRACK: return getProperty(FilePropertyKey.TRACK_NUMBER);
            case YEAR: return getProperty(FilePropertyKey.YEAR);
            case NAME: return getProperty(FilePropertyKey.NAME);
            case EXTENSION: return vsr.getFileExtension();
            case SIZE: return vsr.getSize();
            case DESCRIPTION: return "";
            default: return null;
        }
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
    public boolean isColumnHiddenAtStartup(int column) {
        return Columns.values()[column].isShown();
    }

    @Override
    public boolean isColumnHideable(int column) {
        return true;
    }

    @Override
    public int getInitialWidth(int column) {
        return Columns.values()[column].getInitialWidth();
    }
    
    @Override
    public boolean isEditable(VisualSearchResult vsr, int column) {
        return column == Columns.FROM.ordinal();
    }

    @Override
    public int getNameColumn() {
        return Columns.NAME.ordinal();
    }
    
    /**
     * If the FromColumn is sorted, use a custom column sorter
     * otherwise it is assumed the column returns a value that 
     * implements the Comparable interface
     */
    @Override
    public Comparator getColumnComparator(int index) {
        Columns other = Columns.values()[index];
        switch(other) {
            case FROM: return getFromComparator();
        }
        return super.getColumnComparator(index);
    }
}