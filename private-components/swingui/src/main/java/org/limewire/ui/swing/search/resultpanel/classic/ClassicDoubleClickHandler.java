package org.limewire.ui.swing.search.resultpanel.classic;

import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.DownloadHandler;
import org.limewire.ui.swing.table.TableDoubleClickHandler;

import ca.odell.glazedlists.swing.EventTableModel;

/**
 * Handles a double click on a row in the classic search results table.
 */
public class ClassicDoubleClickHandler implements TableDoubleClickHandler {

    private EventTableModel<VisualSearchResult> results;
    private DownloadHandler downloadHandler;
    
    public ClassicDoubleClickHandler(EventTableModel<VisualSearchResult> results, DownloadHandler downloadHandler, Navigator navigator, LibraryNavigator libraryNavigator) {
        this.results = results;
        this.downloadHandler = downloadHandler;
    }
    
    @Override
    public void handleDoubleClick(int row) {
        if (row == -1 || row == results.getRowCount())
            return;

        VisualSearchResult result = results.getElementAt(row);
        downloadHandler.download(result);
    }
}
