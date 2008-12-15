package org.limewire.ui.swing.search;

import org.limewire.core.api.search.Search;
import org.limewire.ui.swing.search.model.VisualSearchResult;

import ca.odell.glazedlists.EventList;

public interface ResultsContainerFactory {

    public ResultsContainer create(
        EventList<VisualSearchResult> visualSearchResults,
        Search search, SearchInfo searchInfo, RowSelectionPreserver preserver);
}