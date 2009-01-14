package org.limewire.ui.swing.search.resultpanel.classic;

import org.limewire.ui.swing.downloads.MainDownloadPanel;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.search.model.BasicDownloadState;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.BaseResultPanel;
import org.limewire.ui.swing.table.ConfigurableTable;
import org.limewire.ui.swing.table.TableDoubleClickHandler;

import ca.odell.glazedlists.swing.EventTableModel;

/**
 * Handles a double click on a row in the classic search results table.
 */
public class ClassicDoubleClickHandler implements TableDoubleClickHandler {

    private ConfigurableTable<VisualSearchResult> table;
    private BaseResultPanel resultPanel;
    private LibraryNavigator libraryNavigator;
    private Navigator navigator;
    
    public ClassicDoubleClickHandler(ConfigurableTable<VisualSearchResult> table, BaseResultPanel resultPanel, Navigator navigator, LibraryNavigator libraryNavigator) {
        this.table = table;
        this.resultPanel = resultPanel;
        this.navigator = navigator;
        this.libraryNavigator = libraryNavigator;
    }
    
    @Override
    public void handleDoubleClick(int row) {
        if (row == -1 || row == table.getRowCount()) 
            return;

        EventTableModel<VisualSearchResult> results = table.getEventTableModel();
        VisualSearchResult result = results.getElementAt(row);
        
        if(result.getDownloadState() == BasicDownloadState.DOWNLOADED ||
                result.getDownloadState() == BasicDownloadState.DOWNLOADING) {
                navigator.getNavItem(NavCategory.DOWNLOAD, MainDownloadPanel.NAME).select(result);
        } else if(result.getDownloadState() == BasicDownloadState.LIBRARY) {
            libraryNavigator.selectInLibrary(result.getUrn(), result.getCategory());            
        } else {
            resultPanel.download(results.getElementAt(row));
        }
    }
}
