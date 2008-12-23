package org.limewire.ui.swing.search.resultpanel.classic;

import java.awt.Component;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.ResultsTableFormat;
import org.limewire.ui.swing.table.ColumnStateInfo;
import org.limewire.ui.swing.util.I18n;

/**
 * This class specifies the content of a table that contains
 * document descriptions.
 */
public class ProgramTableFormat extends ResultsTableFormat<VisualSearchResult> {
    static final int FROM_INDEX = 0;
    static final int NAME_INDEX = 1;
    public static final int SIZE_INDEX = 2;
    static final int EXTENSION_INDEX = 3;
    static final int PLATFORM_INDEX = 4;
    static final int COMPANY_INDEX = 5;
    static final int DESCRIPTION_INDEX = 6;
    
    public ProgramTableFormat() {
        super(NAME_INDEX, FROM_INDEX, new ColumnStateInfo[] {
                new ColumnStateInfo(FROM_INDEX, "CLASSIC_SEARCH_PROGRAM_FROM", I18n.tr("From"), 55, true, true), 
                new ColumnStateInfo(NAME_INDEX, "CLASSIC_SEARCH_PROGRAM_NAME", I18n.tr("Name"), 350, true, true),     
                new ColumnStateInfo(SIZE_INDEX, "CLASSIC_SEARCH_PROGRAM_SIZE", I18n.tr("Size"), 80, true, true), 
                new ColumnStateInfo(EXTENSION_INDEX, "CLASSIC_SEARCH_PROGRAM_EXTENSION", I18n.tr("Extension"), 80, true, true), 
                new ColumnStateInfo(PLATFORM_INDEX, "CLASSIC_SEARCH_PROGRAM_PLATFORM", I18n.tr("Platform"), 120, false, true),
                new ColumnStateInfo(COMPANY_INDEX, "CLASSIC_SEARCH_PROGRAM_COMPANY", I18n.tr("Company"), 80, true, true), 
                new ColumnStateInfo(DESCRIPTION_INDEX, "CLASSIC_SEARCH_PROGRAM_DESCRIPTION", I18n.tr("Description"), 80, false, true)
        });
    }

    @Override
    public Class getColumnClass(int index) {
        switch(index) {
        case NAME_INDEX: return Component.class;
        case SIZE_INDEX: return Integer.class;
        case FROM_INDEX: return VisualSearchResult.class;
        }
        return  super.getColumnClass(index);
    }

    @Override
    public Object getColumnValue(VisualSearchResult vsr, int index) {
        switch(index) {
            case NAME_INDEX: return vsr;
            case SIZE_INDEX: return vsr.getSize();
            case PLATFORM_INDEX: return vsr.getProperty(FilePropertyKey.PLATFORM);
            case COMPANY_INDEX: return vsr.getProperty(FilePropertyKey.COMPANY);
            case EXTENSION_INDEX: return vsr.getFileExtension();
            case FROM_INDEX: return vsr;
            case DESCRIPTION_INDEX: return "";
        }
        throw new IllegalArgumentException("Unknown column:" + index);
    }
}
