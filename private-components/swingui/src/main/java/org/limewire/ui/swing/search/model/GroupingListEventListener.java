package org.limewire.ui.swing.search.model;

import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

/**
 * A listener to handle updates to the list of visual search results.  This
 * listener applies a SimilarResultsDetector to each VisualSearchResult to
 * allow similar results to be grouped.
 */
class GroupingListEventListener implements ListEventListener<VisualSearchResult> {
    private final Log LOG = LogFactory.getLog(getClass());

    private final SimilarResultsDetector similarResultsDetector;

    /**
     * Constructs a GroupingListEventListener with the specified similar 
     * results detector.
     */
    public GroupingListEventListener(SimilarResultsDetector similarResultsDetector) {
        this.similarResultsDetector = similarResultsDetector;
    }

    @Override
    public void listChanged(ListEvent<VisualSearchResult> listChanges) {
        EventList<VisualSearchResult> eventList = listChanges.getSourceList();
        while (listChanges.next()) {
                VisualSearchResult searchResult = eventList.get(listChanges.getIndex());
                similarResultsDetector.detectSimilarResult(searchResult);
        }
        LOG.debugf("finished detecting similar results");
    }
}
