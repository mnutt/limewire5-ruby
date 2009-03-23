package org.limewire.ui.swing.search.resultpanel;

import org.limewire.ui.swing.search.RowSelectionPreserver;
import org.limewire.ui.swing.search.model.SearchResultsModel;
import org.limewire.ui.swing.search.model.VisualSearchResult;

import ca.odell.glazedlists.EventList;

public interface AllResultsPanelFactory {
    
    AllResultsPanel create(SearchResultsModel searchResultsModel,
        EventList<VisualSearchResult> eventList,
        RowSelectionPreserver preserver);
}