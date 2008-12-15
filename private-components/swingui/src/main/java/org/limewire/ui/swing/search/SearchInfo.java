package org.limewire.ui.swing.search;

import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.SearchDetails.SearchType;

/** Contains information about how the search is being performed. */
public interface SearchInfo {

    /** What the title of the search is. */
    String getTitle();
    
    /** What the actual query of the search is. */
    String getQuery();
    
    /** What category the search is being sent in. */
    SearchCategory getSearchCategory();
    
    /** What kind of search this is. */
    SearchType getSearchType();
    
}
