package org.limewire.ui.swing.search.resultpanel;

import java.awt.Component;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.table.TablePopupHandler;

public class SearchPopupHandler implements TablePopupHandler {

    private final ResultsTable<VisualSearchResult> resultsTable;
    private final DownloadHandler downloadHandler;
    private final PropertiesFactory<VisualSearchResult> properties;

    public SearchPopupHandler(ResultsTable<VisualSearchResult> resultsTable,
            DownloadHandler downloadHandler, PropertiesFactory<VisualSearchResult> properties) {
        this.resultsTable = resultsTable;
        this.downloadHandler = downloadHandler;
        this.properties = properties;
    }

    @Override
    public boolean isPopupShowing(int row) {
        return false;
    }

    @Override
    public void maybeShowPopup(Component component, int x, int y) {

        List<Integer> selectedRows = asList(resultsTable.getSelectedRows());
        int popupRow = resultsTable.rowAtPoint(new Point(x, y));
        
        if (selectedRows.size() <= 1 || !selectedRows.contains(popupRow)) {
            selectedRows.clear();
            selectedRows.add(popupRow);
            resultsTable.setRowSelectionInterval(popupRow, popupRow);
        }

        List<VisualSearchResult> selectedItems = new ArrayList<VisualSearchResult>();
        for (Integer row : selectedRows) {
            if (row != -1) {
                VisualSearchResult visualSearchResult = resultsTable.getEventTableModel()
                        .getElementAt(row);
                if (visualSearchResult != null) {
                    selectedItems.add(visualSearchResult);
                }
            }
        }

        SearchResultMenu searchResultMenu = new SearchResultMenu(downloadHandler, selectedItems,
                properties, SearchResultMenu.ViewType.Table);
        searchResultMenu.show(component, x, y);
    }

    private List<Integer> asList(int[] array) {
        List<Integer> list = new ArrayList<Integer>();
        for(int i = 0; i < array.length; i++) {
            list.add(array[i]);
        }
        return list;
    }
}