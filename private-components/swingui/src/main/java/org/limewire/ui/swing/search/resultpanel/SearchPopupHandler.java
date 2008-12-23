package org.limewire.ui.swing.search.resultpanel;

import java.awt.Component;
import java.awt.Point;

import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.table.ConfigurableTable;
import org.limewire.ui.swing.table.TablePopupHandler;

public class SearchPopupHandler implements TablePopupHandler {
    private int popupRow = -1;
    
    private final ConfigurableTable<VisualSearchResult> configTable;
    private final BaseResultPanel baseResultPanel;
    private final PropertiesFactory<VisualSearchResult> properties;
    
    public SearchPopupHandler(ConfigurableTable<VisualSearchResult> configTable, BaseResultPanel baseResultPanel,
            PropertiesFactory<VisualSearchResult> properties) {
        this.configTable = configTable;
        this.baseResultPanel = baseResultPanel;
        this.properties = properties;
    }
    
    @Override
    public boolean isPopupShowing(int row) {
        return false;
    }

    @Override
    public void maybeShowPopup(Component component, int x, int y) {
        popupRow = configTable.rowAtPoint(new Point(x, y));

        VisualSearchResult vsr = configTable.getEventTableModel().getElementAt(popupRow);
        
        configTable.setRowSelectionInterval(popupRow, popupRow);
        SearchResultMenu searchResultMenu = new SearchResultMenu(baseResultPanel, vsr, properties);
        searchResultMenu.show(component, x, y);
    }
}