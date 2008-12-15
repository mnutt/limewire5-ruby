package org.limewire.ui.swing.search.resultpanel.classic;

import java.util.Calendar;
import java.util.Comparator;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.ResultsTableFormat;
import org.limewire.ui.swing.util.I18n;

/**
 * This class specifies the content of a table that contains
 * image descriptions.
 */
public class ImageTableFormat extends ResultsTableFormat<VisualSearchResult> {
    public static enum Columns {
        FROM(I18n.tr("From"), true, 55),
        NAME(I18n.tr("Filename"), true, 540),
        EXTENSION(I18n.tr("Extension"), true, 60),
        DATE(I18n.tr("Date Created"), true, 100),
        SIZE(I18n.tr("Size"), true, 100),
        DESCRIPTION(I18n.tr("Description"), false, 80),
        TITLE(I18n.tr("Title"), false, 80);
        
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
        case DATE: return Calendar.class;
        case FROM: return VisualSearchResult.class;
        }
        return super.getColumnClass(index);
    }

    @Override
    public Object getColumnValue(VisualSearchResult vsr, int index) {
        this.vsr = vsr;

        Columns other = Columns.values()[index];
        switch(other) {
            case NAME: return getProperty(FilePropertyKey.NAME);
            case EXTENSION: return vsr.getFileExtension();
            case DATE: return getProperty(FilePropertyKey.DATE_CREATED);
            case FROM: return vsr;
            case SIZE: return vsr.getSize();
            case DESCRIPTION: return "";
            case TITLE: return getProperty(FilePropertyKey.TITLE);
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