package org.limewire.ui.swing.search;

import javax.swing.Action;

import org.limewire.ui.swing.search.model.VisualSearchResult;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

public class SourceCountMaintainer implements ListEventListener<VisualSearchResult> {
    
    private final Action countAction;
    private final EventList<VisualSearchResult> source;
    
    public SourceCountMaintainer(
        EventList<VisualSearchResult> resultsEventList, Action action) {
        this.countAction = action;
        this.source = resultsEventList;

        listChanged(null);
        source.addListEventListener(this);
    }
    
    public void dispose() {
        source.removeListEventListener(this);
    }

    @Override
    public void listChanged(ListEvent<VisualSearchResult> listChanges) {
        int count = source.size();
        countAction.putValue(Action.NAME, count == 0 ? null : String.valueOf(count));
    }
}