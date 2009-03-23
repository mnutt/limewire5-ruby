package org.limewire.ui.swing.search;

import java.util.List;

import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.sponsored.SponsoredResult;
import org.limewire.ui.swing.search.model.SearchResultsModel;
import org.limewire.ui.swing.util.SwingUtils;

/**
 * An implementation of SearchListener to handle search results and update the
 * Swing UI.  Search events are forwarded to the data model and UI using the
 * event dispatch thread.
 */
class SwingSearchListener implements SearchListener {

    private final SearchResultsModel searchResultsModel;
    private final SponsoredResultsView sponsoredView;
    private final SearchNavItem searchNavItem;
    
    /**
     * Constructs a SwingSearchListener for the specified search model,
     * sponsored results view, and navigation item.
     */
    public SwingSearchListener(SearchResultsModel searchResultsModel, 
            SponsoredResultsView searchView, SearchNavItem searchNavItem) {
        this.searchResultsModel = searchResultsModel;
        this.sponsoredView = searchView;
        this.searchNavItem = searchNavItem;
    }

    @Override
    public void handleSearchResult(Search search, final SearchResult searchResult) {
        SwingUtils.invokeLater(new Runnable() {
            @Override
            public void run() {
                // Expect that the core properly filtered the result.
                // That is, we won't see it if we didn't want to.
                searchResultsModel.addSearchResult(searchResult);
                
                // We can update the source count here because
                // we never expect things to be removed.
                // Changes only happen on insertion.
                // If removes ever happen, we'll need to switch
                // to adding a ListEventListener to
                // model.getVisualSearchResults.
                searchNavItem.sourceCountUpdated(searchResultsModel.getResultCount());
            }
        });
    }

    @Override
    public void handleSponsoredResults(Search search, final List<SponsoredResult> sponsoredResults) {
        SwingUtils.invokeLater(new Runnable() {
            @Override
            public void run() {
                sponsoredView.addSponsoredResults(sponsoredResults);
            }
        });
    }

    @Override
    public void searchStarted(Search search) {
    }

    @Override
    public void searchStopped(Search search) {
    }

}
