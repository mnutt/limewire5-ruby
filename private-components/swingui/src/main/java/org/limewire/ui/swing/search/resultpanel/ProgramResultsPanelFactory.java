package org.limewire.ui.swing.search.resultpanel;

import ca.odell.glazedlists.EventList;
import org.limewire.core.api.search.Search;
import org.limewire.ui.swing.search.RowSelectionPreserver;
import org.limewire.ui.swing.search.SearchInfo;
import org.limewire.ui.swing.search.model.VisualSearchResult;

public interface ProgramResultsPanelFactory {
    
    ProgramResultsPanel create(
        EventList<VisualSearchResult> eventList,
        Search search, SearchInfo searchInfo, RowSelectionPreserver preserver);
}