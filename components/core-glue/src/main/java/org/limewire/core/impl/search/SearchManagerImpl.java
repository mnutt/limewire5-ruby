package org.limewire.core.impl.search;

import java.util.ArrayList;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchManager;
import org.limewire.core.api.search.SearchFactory;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.sponsored.SponsoredResult;
import org.limewire.io.GUID;

@Singleton
public
class SearchManagerImpl implements SearchManager {
    
    private List<SearchWithResults> searchList;
    private SearchFactory searchFactory;
    
    @Inject
    public SearchManagerImpl(SearchFactory searchFactory) {
        this.searchFactory = searchFactory;
        this.searchList = new ArrayList<SearchWithResults>();
    }
    
    @Override
    public SearchWithResults getSearchByGuid(GUID guid) {
        for (SearchWithResults search : this.searchList) {
            if(search.getQueryGuid().toHexString().contentEquals(guid.toHexString())) {
                return search;
            }
        }
        return null;
    }
    
    @Override
    public List<SearchWithResults> getAllSearches() {
        return this.searchList;
    }
    
    public SearchWithResults createSearchFromQuery(String query) {
        SearchDetails searchDetails = new Details(query);
        Search search = searchFactory.createSearch(searchDetails);
        SearchWithResults searchWithResults = new SearchWithResults(search);
        this.searchList.add(searchWithResults);
        return searchWithResults;
    }
    
    public class Listener implements SearchListener {
        private List<SearchResult> searchResults;
        
        public Listener() {
            this.searchResults = new ArrayList<SearchResult>();
        }
        
        @Override
        public void searchStopped(Search search) {
            // Do nothing
        }        
        @Override
        public void handleSponsoredResults(Search search, List<SponsoredResult> sponsoredResult) {
            // Do nothing
        }
        @Override
        public void handleSearchResult(Search search, SearchResult searchResult) {
            this.searchResults.add(searchResult);
        }
        @Override
        public void searchStarted(Search search) {
            // Do nothing
        }
    }
    
    public class Details implements SearchDetails {

        private String query;
        
        public Details(String query) {
            this.query = query;
        }
        
        @Override
        public SearchCategory getSearchCategory() {
            return SearchCategory.ALL;
        }

        @Override
        public String getSearchQuery() {
            return this.query;
        }

        @Override
        public SearchType getSearchType() {
            return SearchType.KEYWORD;
        }   
    }
    
    public class SearchWithResults {
        private Search search;
        private Listener listener;
        
        public SearchWithResults(Search search) {
            System.out.println(search.toString());
            this.search = search;
            this.listener = new Listener();
            this.search.addSearchListener(this.listener);
        }
        
        public GUID getQueryGuid() {
            return this.search.getQueryGuid();
        }
        
        public List<SearchResult> getSearchResults() {
            return this.listener.searchResults;
        }
        
        public void start() {
            this.search.start();
        }
        
        public void stop() {
            this.search.stop();
        }
        
        public String getQueryString() {
            return this.search.getQuery();
        }
    }
}
