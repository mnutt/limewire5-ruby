package org.limewire.ui.swing.search.resultpanel.classic;

import java.awt.Component;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jdesktop.swingx.decorator.SortKey;
import org.jdesktop.swingx.decorator.SortOrder;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.ResultsTableFormat;
import org.limewire.ui.swing.table.ColumnStateInfo;
import org.limewire.ui.swing.util.I18n;

/**
 * This class specifies the content of a table that contains
 * document descriptions.
 */
public class OtherTableFormat extends ResultsTableFormat<VisualSearchResult> {
    static final int FROM_INDEX = 0;
    static final int NAME_INDEX = 1;
    static final int TYPE_INDEX = 2;
    static final int EXTENSION_INDEX = 3;
    public static final int SIZE_INDEX = 4;
    
    public OtherTableFormat() {
        super(NAME_INDEX, FROM_INDEX, new ColumnStateInfo[] {
                new ColumnStateInfo(FROM_INDEX, "CLASSIC_SEARCH_OTHER_FROM", I18n.tr("From"), 88, true, true), 
                new ColumnStateInfo(NAME_INDEX, "CLASSIC_SEARCH_OTHER_NAME", I18n.tr("Name"), 525, true, true),     
                new ColumnStateInfo(TYPE_INDEX, "CLASSIC_SEARCH_OTHER_TYPE", I18n.tr("Type"), 75, true, true), 
                new ColumnStateInfo(EXTENSION_INDEX, "CLASSIC_SEARCH_OTHER_EXTENSION", I18n.tr("Extension"), 80, true, true), 
                new ColumnStateInfo(SIZE_INDEX, "CLASSIC_SEARCH_OTHER_SIZE", I18n.tr("Size"), 92, true, true)
        });
    }

    @Override
    public Class getColumnClass(int index) {
        switch(index) {
        case NAME_INDEX: return Component.class;
        case SIZE_INDEX: return Integer.class;
        case FROM_INDEX: return VisualSearchResult.class;
        }
        return super.getColumnClass(index);
    }

    @Override
    public Object getColumnValue(VisualSearchResult vsr, int index) {
        switch(index) {
            case NAME_INDEX: return vsr;
            case EXTENSION_INDEX: return vsr.getFileExtension();
            case TYPE_INDEX: return vsr.getCategory();
            case SIZE_INDEX: return vsr.getSize();
            case FROM_INDEX: return vsr;
        }
        throw new IllegalArgumentException("Unknown column:" + index);
    }
    
    @Override
    public List<SortKey> getDefaultSortKeys() {
        return Arrays.asList(
                new SortKey(SortOrder.DESCENDING, FROM_INDEX),
                new SortKey(SortOrder.ASCENDING, NAME_INDEX),
                new SortKey(SortOrder.ASCENDING, TYPE_INDEX),
                new SortKey(SortOrder.ASCENDING, SIZE_INDEX));
    }
    
    @Override
    public List<Integer> getSecondarySortColumns(int column) {
        switch (column) {
        case NAME_INDEX:
            return Arrays.asList(TYPE_INDEX, SIZE_INDEX);
        case TYPE_INDEX:
            return Arrays.asList(NAME_INDEX, SIZE_INDEX);
        case SIZE_INDEX:
            return Arrays.asList(NAME_INDEX, TYPE_INDEX);
        default:
            return Collections.emptyList();
        }
    }
}