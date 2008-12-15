package org.limewire.core.impl.spam;

import java.util.List;

import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.spam.SpamManager;
import org.limewire.core.impl.search.MockSearchResult;

public class MockSpamManagerImpl implements SpamManager {

    @Override
    public void clearFilterData() {

    }

    @Override
    public void handleUserMarkedGood(List<? extends SearchResult> searchResults) {
        for (SearchResult searchResult : searchResults) {
            MockSearchResult result = (MockSearchResult) searchResult;
            result.setSpam(false);
        }
    }

    @Override
    public void handleUserMarkedSpam(List<? extends SearchResult> searchResults) {
        for (SearchResult searchResult : searchResults) {
            MockSearchResult result = (MockSearchResult) searchResult;
            result.setSpam(true);
        }
    }

    @Override
    public void reloadIPFilter() {
    }

    @Override
    public void adjustSpamFilters() {
        
    }
}
