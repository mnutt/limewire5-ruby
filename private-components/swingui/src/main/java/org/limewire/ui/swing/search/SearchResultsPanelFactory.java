package org.limewire.ui.swing.search;

import org.limewire.core.api.search.Search;
import org.limewire.ui.swing.search.model.VisualSearchResult;

import ca.odell.glazedlists.EventList;

/**
 * Defines a factory for creating the search results display container.
 */
public interface SearchResultsPanelFactory {
    
    /**
     * Creates a new SearchResultsPanel using the specified search information,
     * list of search results, and Search object.
     */
    public SearchResultsPanel createSearchResultsPanel(SearchInfo searchInfo,
            EventList<VisualSearchResult> visualSearchResults,
            Search search);
    
}
