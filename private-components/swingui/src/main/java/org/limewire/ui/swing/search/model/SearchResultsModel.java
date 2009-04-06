package org.limewire.ui.swing.search.model;

import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.search.resultpanel.DownloadHandler;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.matchers.MatcherEditor;

/**
 * Defines a data model containing the results of a search.
 */
public interface SearchResultsModel extends DownloadHandler, Disposable {

    /**
     * Installs the specified search listener and starts the search.  The
     * search listener should handle search results by calling the 
     * <code>addSearchResult(SearchResult)</code> method. 
     */
    void start(SearchListener searchListener);
    
    /**
     * Returns the search category.
     */
    SearchCategory getSearchCategory();
    
    /**
     * Returns the query string for the search.
     */
    String getSearchQuery();
    
    /**
     * Returns the total number of results in the search.
     */
    int getResultCount();

    /**
     * Returns a list of grouped results in the search.
     */
    EventList<VisualSearchResult> getGroupedSearchResults();

    /**
     * Returns an observable list of grouped results in the search.  The 
     * observable list fires update events whenever a result is modified in 
     * place. 
     */
    EventList<VisualSearchResult> getObservableSearchResults();

    /**
     * Returns a list of filtered results for the specified search category.
     */
    EventList<VisualSearchResult> getCategorySearchResults(SearchCategory category);

    /**
     * Returns a list of sorted and filtered results for the selected search
     * category and sort option.
     */
    EventList<VisualSearchResult> getSortedSearchResults();

    /**
     * Returns the selected search category.
     */
    SearchCategory getSelectedCategory();

    /**
     * Selects the specified search category.
     */
    void setSelectedCategory(SearchCategory searchCategory);
    
    /**
     * Sets the sort option.
     */
    void setSortOption(SortOption sortOption);

    /**
     * Sets the MatcherEditor used to filter search results. 
     */
    void setFilterEditor(MatcherEditor<VisualSearchResult> editor);
    
    /**
     * Adds the specified search result to the results list.
     */
    void addSearchResult(SearchResult result);

    /**
     * Removes the specified search result from the results list.
     */
    void removeSearchResult(SearchResult result);
    
}
