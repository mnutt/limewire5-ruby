package org.limewire.core.api.search;

import java.util.List;

import org.limewire.core.api.search.sponsored.SponsoredResult;

public interface SearchListener {
    
    void handleSearchResult(SearchResult searchResult);
    
    void searchStarted();
    
    void searchStopped();
    
    void handleSponsoredResults(List<SponsoredResult> sponsoredResults);

}
