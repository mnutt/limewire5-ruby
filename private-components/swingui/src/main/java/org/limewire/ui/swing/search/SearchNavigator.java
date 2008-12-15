package org.limewire.ui.swing.search;

import javax.swing.JComponent;

import org.limewire.core.api.search.Search;

public interface SearchNavigator {
    
    SearchNavItem addSearch(String title, JComponent searchPanel, Search search);

}
