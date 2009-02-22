package org.limewire.core.api.search;

import java.util.List;

import org.limewire.core.api.search.sponsored.SponsoredResult;

/** A listener for a search. */
public interface SearchListener {
    
    /** Notification a new search result is received for the search. */
    void handleSearchResult(Search search, SearchResult searchResult);
    
    /** Notification the search has started. */
    void searchStarted(Search search);
    
    /** Notification the search has stopped. */
    void searchStopped(Search search);
    
    /** Notification that sponsored results have been received for the search. */
    void handleSponsoredResults(Search search, List<SponsoredResult> sponsoredResults);

}
