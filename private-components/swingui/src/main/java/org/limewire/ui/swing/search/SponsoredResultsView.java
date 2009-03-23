package org.limewire.ui.swing.search;

import java.util.List;

import org.limewire.core.api.search.sponsored.SponsoredResult;

/**
 * Defines a view of the search results.
 */
public interface SponsoredResultsView {

    /**
     * Adds the specified list of sponsored results to the display.
     */
    void addSponsoredResults(List<SponsoredResult> sponsoredResults);
    
}
