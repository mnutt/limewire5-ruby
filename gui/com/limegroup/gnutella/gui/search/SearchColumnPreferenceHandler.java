package com.limegroup.gnutella.gui.search;

import com.limegroup.gnutella.gui.tables.DataLineModel;
import com.limegroup.gnutella.gui.tables.DefaultColumnPreferenceHandler;
import com.limegroup.gnutella.gui.tables.LimeJTable;
import com.limegroup.gnutella.gui.tables.LimeTableColumn;

/**
 * Column preference handler for Search columns.
 *
 * Extends DefaultColumnPreferenceHandler to store/read data in memory
 * instead of to/from disk.
 */
final class SearchColumnPreferenceHandler
    extends DefaultColumnPreferenceHandler {
    
    SearchColumnPreferenceHandler(LimeJTable table) {
        super(table);
    }
    
    @Override
    protected void setVisibility(LimeTableColumn col, boolean vis) {
        ((SearchColumn)col).setCurrentVisibility(vis);
    }

    @Override
    protected void setOrder(LimeTableColumn col, int order) {
        ((SearchColumn)col).setCurrentOrder(order);
    }

    @Override
    protected void setWidth(LimeTableColumn col, int width) {
        ((SearchColumn)col).setCurrentWidth(width);
    }

    @Override
    protected boolean getVisibility(LimeTableColumn col) {
        return ((SearchColumn)col).getCurrentVisibility();
    }

    @Override
    protected int getOrder(LimeTableColumn col) {
        return ((SearchColumn)col).getCurrentOrder();
    }

    @Override
    protected int getWidth(LimeTableColumn col) {
        return ((SearchColumn)col).getCurrentWidth();
    }
    
    @Override
    protected boolean isDefaultWidth(LimeTableColumn col) {
        return ((SearchColumn)col).getCurrentWidth() ==
                col.getDefaultWidth();
    }

    @Override
    protected boolean isDefaultOrder(LimeTableColumn col) {
        return ((SearchColumn)col).getCurrentOrder() ==
                col.getDefaultOrder();
    }

    @Override
    protected boolean isDefaultVisibility(LimeTableColumn col) {
        return ((SearchColumn)col).getCurrentOrder() ==
                col.getDefaultOrder();
    }
    
    @Override
    protected void save() {
        DataLineModel dlm = (DataLineModel)table.getModel();
        for(int i = 0; i < dlm.getColumnCount(); i++) {
            LimeTableColumn ltc = dlm.getTableColumn(i);
            super.setVisibility(ltc, getVisibility(ltc));
            super.setOrder(ltc, getOrder(ltc));
            super.setWidth(ltc, getWidth(ltc));
        }
        super.save();
    }
}
    