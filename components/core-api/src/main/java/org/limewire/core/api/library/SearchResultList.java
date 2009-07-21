package org.limewire.core.api.library;

import java.util.Collection;

import org.limewire.core.api.search.SearchResult;

import ca.odell.glazedlists.EventList;

/** A list of files from a remote host */
public interface SearchResultList {
        
    /** Adds a new file into the list. */
    void addNewResult(SearchResult file);
    
    /** Removes an existing file from the list. */
    void removeResult(SearchResult file);
    
    /** Sets all files in the list to be this collection of files. */
    void setNewResults(Collection<SearchResult> files);
    
    /** An {@link EventList} that describes this list. */
    EventList<SearchResult> getModel();
    
    /** An {@link EventList} that, for convenience, is usable from Swing. */
    EventList<SearchResult> getSwingModel();
    
    /** The size of the list. */
    int size();
    
    /** Clears all items out of the list. */
    void clear();
}
