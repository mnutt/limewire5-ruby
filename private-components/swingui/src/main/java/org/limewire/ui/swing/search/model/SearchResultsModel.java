package org.limewire.ui.swing.search.model;

import org.limewire.core.api.search.SearchResult;

import ca.odell.glazedlists.EventList;

public interface SearchResultsModel {

    public abstract int getResultCount();

    public abstract EventList<VisualSearchResult> getObservableSearchResults();

    public abstract void addSearchResult(SearchResult result);

    public abstract void removeSearchResult(SearchResult result);

    EventList<VisualSearchResult> getGroupedSearchResults();

}