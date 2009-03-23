package org.limewire.ui.swing.search;

import org.limewire.ui.swing.search.model.SearchResultsModel;

/**
 * Defines a factory for creating the sort-and-filter components for the 
 * search results display.
 */
public interface SortAndFilterPanelFactory {

    /**
     * Creates a new SortAndFilterPanel using the specified search results data
     * model and selection preserver.
     */
    public SortAndFilterPanel create(SearchResultsModel searchResultsModel,
            RowSelectionPreserver preserver);

}
