package org.limewire.ui.swing.search;

import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.search.model.VisualSearchResult;

import ca.odell.glazedlists.EventList;

interface SearchTabItemsFactory {

    SearchTabItems create(SearchCategory category, EventList<VisualSearchResult> resultsList);
    
}
