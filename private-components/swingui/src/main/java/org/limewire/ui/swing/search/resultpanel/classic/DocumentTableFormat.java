package org.limewire.ui.swing.search.resultpanel.classic;

import java.awt.Component;
import java.util.Calendar;
import java.util.Comparator;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.ResultsTableFormat;
import org.limewire.ui.swing.util.I18n;

/**
 * This class specifies the content of a table that contains
 * document descriptions.
 */
public class DocumentTableFormat extends ResultsTableFormat<VisualSearchResult> {

    public static enum Columns {
        FROM(I18n.tr("From"), true, 55),
        NAME(I18n.tr("Filename"), true, 360),
        TYPE(I18n.tr("Type"), true, 80),
        EXTENSION(I18n.tr("Extension"), true, 80),
        SIZE(I18n.tr("Size"), true, 100),
        DATE(I18n.tr("Date Created"), true, 60),
        AUTHOR(I18n.tr("Author"), false, 80),
        DESCRIPTION(I18n.tr("Description"), false, 80);
        
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
        case NAME: return Component.class;
        case DATE: return Calendar.class;
        case SIZE: return Integer.class;
        case FROM: return VisualSearchResult.class;
        }
        return super.getColumnClass(column);
    }

    @Override
    public Object getColumnValue(VisualSearchResult vsr, int column) {
        this.vsr = vsr;

        Columns other = Columns.values()[column];
        switch(other) {
            case NAME: return vsr;
            case TYPE: return vsr.getFileExtension();
            case SIZE: return vsr.getSize();
            case DATE: return getProperty(FilePropertyKey.DATE_CREATED);
            case FROM: return vsr;
            case EXTENSION: return vsr.getFileExtension();
            case AUTHOR: return getProperty(FilePropertyKey.AUTHOR);
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