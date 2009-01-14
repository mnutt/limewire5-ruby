package org.limewire.ui.swing.search.resultpanel.classic;

import java.awt.Component;
import java.util.Calendar;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.ResultsTableFormat;
import org.limewire.ui.swing.table.ColumnStateInfo;
import org.limewire.ui.swing.util.I18n;

/**
 * This class specifies the content of a table that contains
 * image descriptions.
 */
public class ImageTableFormat extends ResultsTableFormat<VisualSearchResult> {
    static final int FROM_INDEX = 0;
    static final int NAME_INDEX = 1;
    static final int EXTENSION_INDEX = 2;
    static final int DATE_INDEX = 3;
    public static final int SIZE_INDEX = 4;
    static final int DESCRIPTION_INDEX = 5;
    static final int TITLE_INDEX = 6;
    
    public ImageTableFormat() {
        super(NAME_INDEX, FROM_INDEX, new ColumnStateInfo[] {
                new ColumnStateInfo(FROM_INDEX, "CLASSIC_SEARCH_IMAGE_FROM", I18n.tr("From"), 55, true, true), 
                new ColumnStateInfo(NAME_INDEX, "CLASSIC_SEARCH_IMAGE_NAME", I18n.tr("Name"), 540, true, true),     
                new ColumnStateInfo(EXTENSION_INDEX, "CLASSIC_SEARCH_IMAGE_EXTENSION", I18n.tr("Extension"), 60, true, true), 
                new ColumnStateInfo(DATE_INDEX, "CLASSIC_SEARCH_IMAGE_DATE", I18n.tr("Date Created"), 100, true, true), 
                new ColumnStateInfo(SIZE_INDEX, "CLASSIC_SEARCH_IMAGE_SIZE", I18n.tr("Size"), 100, false, true), 
                new ColumnStateInfo(DESCRIPTION_INDEX, "CLASSIC_SEARCH_IMAGE_DESCRIPTION", I18n.tr("Description"), 80, false, true), 
                new ColumnStateInfo(TITLE_INDEX, "CLASSIC_SEARCH_IMAGE_TITLE", I18n.tr("Title"), 80, false, true)
        });
    }

    @Override
    public Class getColumnClass(int index) {
        switch(index) {
        case NAME_INDEX: return Component.class;
        case DATE_INDEX: return Calendar.class;
        case FROM_INDEX: return VisualSearchResult.class;
        }
        return super.getColumnClass(index);
    }

    @Override
    public Object getColumnValue(VisualSearchResult vsr, int index) {
        switch(index) {
            case NAME_INDEX: return vsr;
            case EXTENSION_INDEX: return vsr.getFileExtension();
            case DATE_INDEX: return vsr.getProperty(FilePropertyKey.DATE_CREATED);
            case FROM_INDEX: return vsr;
            case SIZE_INDEX: return vsr.getSize();
            case DESCRIPTION_INDEX: return "";
            case TITLE_INDEX: return vsr.getProperty(FilePropertyKey.TITLE);
        }
        throw new IllegalArgumentException("Unknown column:" + index);
    }
}