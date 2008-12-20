package org.limewire.core.api.search;

import java.util.concurrent.CopyOnWriteArrayList;

import org.limewire.io.GUID;

/**
 * A single search.
 */
public interface Search {
    
    /** Returns the category this search is for. */
    SearchCategory getCategory();
    
    /** Returns the search string */
    String getQuery();
    
    /** Returns the search's unique GUID */
    GUID getQueryGuid();
    
    /** Adds a new SearchListener. */
    void addSearchListener(SearchListener searchListener);
    
    /** Removes a SearchListener. */
    void removeSearchListener(SearchListener searchListener);
    
    /** Starts the search. */
    void start();
    
    /** Repeats the search. */
    void repeat();
    
    /** Stops the search. */
    void stop();

    /** List of all of the listeners for this search. */
    CopyOnWriteArrayList<SearchListener> getListenerList();
}
