package org.limewire.ui.swing.search.resultpanel;

import org.limewire.core.api.search.Search;
import org.limewire.ui.swing.search.RowSelectionPreserver;
import org.limewire.ui.swing.search.SearchInfo;
import org.limewire.ui.swing.search.model.VisualSearchResult;

import ca.odell.glazedlists.EventList;

public interface AllResultsPanelFactory {
    
    AllResultsPanel create(
        EventList<VisualSearchResult> eventList,
        Search search, SearchInfo searchInfo, RowSelectionPreserver preserver);
}