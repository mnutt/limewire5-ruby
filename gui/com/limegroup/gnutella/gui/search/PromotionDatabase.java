package com.limegroup.gnutella.gui.search;

import java.util.List;

/**
 * Classes implementing this interface provide a {@link java.util.List} of
 * {@link AbstractSearchResult}s for a given {@link SearchInformation} to the
 * passed in {@link SearchResultsCallback}.
 */
interface PromotionDatabase {
    
    /**
     * Generates a {@link List} or {@link SearchInformation}s and passes them
     * in a tail call to the passed in {@link SearchResultsCallback}. And example would be
     * 
     * <pre>
     * public void find(SearchInformation info, Callback callback) {
     *  List&lt;SearchResult&gt; results = getSearchResults(info);
     *  callback.process(results);
     * }
     * private List&lt;SearchResult&gt; getSearchResults(SearchInformation info) {
     *  ...
     *  return results;
     * }
     * </pre>
     * 
     * @param info search query
     * @param callback the recipient of the results
     */
    void find(SearchInformation info, SearchResultsCallback callback);
    
    /**
     * The recipient of search results. Implementations of this should be able
     * to take a {@link List} of {@link SearchResult} and do something with
     * them.
     */
    interface SearchResultsCallback {
        /**
         * Process the passed in {@link List} or {@link SearchResult} and do
         * something with them.
         * 
         * @param results possibly-null {@link List} or {@link SearchResult}
         *        coming from <code>info</code>
         * @param info orignating search
         */
        void process(List<SearchResult> results, SearchInformation info);        
    }
}
