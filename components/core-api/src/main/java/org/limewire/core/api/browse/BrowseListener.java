package org.limewire.core.api.browse;

import org.limewire.core.api.search.SearchResult;

public interface BrowseListener {
    void handleBrowseResult(SearchResult searchResult);
    void browseFinished(boolean success);
}
