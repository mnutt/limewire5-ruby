package org.limewire.ui.swing.search.resultpanel.classic;

import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.BaseResultPanel;
import org.limewire.ui.swing.table.ConfigurableTable;
import org.limewire.ui.swing.table.TableDoubleClickHandler;

import ca.odell.glazedlists.swing.EventTableModel;

public class ClassicDoubleClickHandler implements TableDoubleClickHandler {

    private ConfigurableTable<VisualSearchResult> table;
    private BaseResultPanel resultPanel;
    
    public ClassicDoubleClickHandler(ConfigurableTable<VisualSearchResult> table, BaseResultPanel resultPanel) {
        this.table = table;
        this.resultPanel = resultPanel;
    }
    
    @Override
    public void handleDoubleClick(int row) {
        if (row == -1 || row == table.getRowCount()) 
            return;

        EventTableModel<VisualSearchResult> results = table.getEventTableModel();
        resultPanel.download(results.getElementAt(row));
    }
}
