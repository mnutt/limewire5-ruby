package org.limewire.ui.swing.search;

import javax.swing.JComponent;

import org.limewire.core.api.search.Search;

/** A central hub for showing a new search in the UI. */
public interface SearchNavigator {
    
    /** Adds a new search whose results will show in the given panel. */
    SearchNavItem addSearch(String title, JComponent searchPanel, Search search);

}
