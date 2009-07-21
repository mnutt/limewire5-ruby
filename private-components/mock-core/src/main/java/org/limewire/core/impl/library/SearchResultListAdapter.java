package org.limewire.core.impl.library;

import java.util.Collection;

import org.limewire.core.api.library.SearchResultList;
import org.limewire.core.api.search.SearchResult;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;

public class SearchResultListAdapter implements SearchResultList {

    @Override
    public void addNewResult(SearchResult file) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public EventList<SearchResult> getModel() {
        return new BasicEventList<SearchResult>();
    }

    @Override
    public EventList<SearchResult> getSwingModel() {
        return new BasicEventList<SearchResult>();
    }

    @Override
    public void removeResult(SearchResult file) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setNewResults(Collection<SearchResult> files) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public int size() {
        // TODO Auto-generated method stub
        return 0;
    }
    
    @Override
    public void clear() {
        // TODO Auto-generated method stub
        
    }


}
