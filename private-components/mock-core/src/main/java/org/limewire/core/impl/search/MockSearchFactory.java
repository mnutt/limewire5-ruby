package org.limewire.core.impl.search;

import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.api.search.SearchFactory;

public class MockSearchFactory implements SearchFactory {
    
    @Override
    public Search createSearch(SearchDetails searchDetails) {
        return new MockSearch(searchDetails);
    }

}
