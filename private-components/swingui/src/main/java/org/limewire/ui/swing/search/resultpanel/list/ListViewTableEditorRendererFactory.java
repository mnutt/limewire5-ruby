package org.limewire.ui.swing.search.resultpanel.list;

import java.awt.Color;

import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.search.RemoteHostActions;
import org.limewire.ui.swing.search.resultpanel.ActionColumnTableCellEditor;
import org.limewire.ui.swing.search.resultpanel.DownloadHandler;

public interface ListViewTableEditorRendererFactory {
    
    ListViewTableEditorRenderer create(
            ActionColumnTableCellEditor actionEditor, 
            String searchText, 
            RemoteHostActions fromActions, 
            Navigator navigator, 
            Color rowSelectionColor,
            DownloadHandler downloadHandler, ListViewDisplayedRowsLimit limit);
    
}
