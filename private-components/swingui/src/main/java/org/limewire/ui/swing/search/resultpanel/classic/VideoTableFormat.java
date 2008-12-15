package org.limewire.ui.swing.search.resultpanel.classic;

import java.util.Comparator;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.ResultsTableFormat;
import org.limewire.ui.swing.util.I18n;

/**
 * This class specifies the content of a table that contains
 * video descriptions.
 */
public class VideoTableFormat extends ResultsTableFormat<VisualSearchResult> {

    public static enum Columns {
        FROM(I18n.tr("From"), true, 55),
        NAME(I18n.tr("Filename"), true, 410),
        EXTENSION(I18n.tr("Extension"), true, 60),
        LENGTH(I18n.tr("Length"), true, 60),
        MISC(I18n.tr("Misc"), true, 60),
        QUALITY(I18n.tr("Quality"), true, 60),
        SIZE(I18n.tr("Size"), true, 60),
        RATING(I18n.tr("Rating"), false, 60),
        DIMENSION(I18n.tr("Resolution"), false, 60),
        YEAR(I18n.tr("Year"), false, 60),
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
    public Class getColumnClass(int index) {
        Columns other = Columns.values()[index];
        switch(other) {
        case RATING: return Integer.class;
        case YEAR: return Integer.class;
        case FROM: return VisualSearchResult.class;
        }
        return super.getColumnClass(index);
    }

    @Override
    public Object getColumnValue(VisualSearchResult vsr, int index) {
        this.vsr = vsr;
        
        String fileExtension = vsr.getFileExtension();

        Columns other = Columns.values()[index];
        switch(other) {
            case NAME: return getProperty(FilePropertyKey.NAME);
            case EXTENSION: return fileExtension;
            case LENGTH: return getProperty(FilePropertyKey.LENGTH);
            case YEAR: return getProperty(FilePropertyKey.YEAR);
            case QUALITY: return getProperty(FilePropertyKey.QUALITY);
            case MISC: return getProperty(FilePropertyKey.COMMENTS);
            case DESCRIPTION: return "";
            case FROM: return vsr;
            case RATING: return getProperty(FilePropertyKey.RATING);
            case DIMENSION:
                if(getProperty(FilePropertyKey.WIDTH) == null || getProperty(FilePropertyKey.HEIGHT) == null)
                    return null;
                else
                    return (getProperty(FilePropertyKey.WIDTH) + " X " + getProperty(FilePropertyKey.HEIGHT));
            case SIZE: return vsr.getSize();
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
