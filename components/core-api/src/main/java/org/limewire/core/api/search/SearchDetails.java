package org.limewire.core.api.search;

/**
 * Details about how the search should be constructed.
 */
public interface SearchDetails {
    
    public static enum SearchType { KEYWORD, WHATS_NEW };
    
    /** What category this search is in. */
    public SearchCategory getSearchCategory();
    
    /** The query that should be sent out for this search. */
    public String getSearchQuery();
    
    /** The kind of search. */
    public SearchType getSearchType();

}
