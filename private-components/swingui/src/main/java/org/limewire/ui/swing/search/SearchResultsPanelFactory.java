package org.limewire.ui.swing.search;

import org.limewire.core.api.search.Search;
import org.limewire.ui.swing.search.model.VisualSearchResult;

import ca.odell.glazedlists.EventList;

public interface SearchResultsPanelFactory {
    
    public SearchResultsPanel createSearchResultsPanel(SearchInfo searchInfo,
            EventList<VisualSearchResult> visualSearchResults,
            Search search);
}