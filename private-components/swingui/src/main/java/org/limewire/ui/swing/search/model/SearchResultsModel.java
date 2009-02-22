package org.limewire.ui.swing.search.model;

import org.limewire.core.api.search.SearchResult;

import ca.odell.glazedlists.EventList;

/**
 * Defines an interface for the data model containing the results for a search.
 */
public interface SearchResultsModel {

    /**
     * Returns the total number of results in the search.
     */
    int getResultCount();

    /**
     * Returns an observable list of grouped results in the search.  The 
     * observable list fires update events whenever a result is modified in 
     * place. 
     */
    EventList<VisualSearchResult> getObservableSearchResults();

    /**
     * Returns a list of grouped results in the search.
     */
    EventList<VisualSearchResult> getGroupedSearchResults();

    /**
     * Adds the specified core search result to the data model.
     */
    void addSearchResult(SearchResult result);

    /**
     * Removes the specified core search result from the data model.
     */
    void removeSearchResult(SearchResult result);

}
