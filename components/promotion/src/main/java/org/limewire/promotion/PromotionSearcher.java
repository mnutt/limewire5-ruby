package org.limewire.promotion;

import org.limewire.geocode.GeocodeInformation;
import org.limewire.promotion.containers.PromotionMessageContainer;

/**
 * Provides methods and callbacks to perform a search of the promotion system.
 * Implementations may pull from network or locally cached data, and customize
 * results to the user.
 */
public interface PromotionSearcher {
    /**
     * Performs a search based on the given query and then invokes the
     * {@link PromotionSearchResultsCallback#process(PromotionMessageContainer)}
     * method on the passed-in callback passing in the results to display to the
     * user in the order they are expected to be shown. May call the callback
     * method more than once, but will not submit duplicate results.
     * 
     * Will return silently if {@link #isEnabled()} returns false
     * 
     * @param userLocation The current location of the user (best guess). If
     *        null, no location restrictions will be applied. If missing
     *        latitude/longitude, no radius restrictions will be applied. If
     *        missing territory, no territory restrictions will be applied.
     */
    void search(String query, PromotionSearchResultsCallback callback,
            GeocodeInformation userLocation);

    /**
     * Initializes with the maximum number of results to show in the client.
     * 
     * @param maxNumberOfResults the maximum number of results to show in the
     *        client.
     */
    void init(int maxNumberOfResults) throws InitializeException;

    /**
     * The recipient of promotion search results. Implementations should be able
     * to take a list of {@link PromotionMessageContainer} entries and do
     * something with them (ie, display them to the user).
     */
    public interface PromotionSearchResultsCallback {
        /**
         * Process the passed in {@link PromotionMessageContainer} and display
         * it somehow to the user. The PromotionSearcher considers a call to
         * this method as an impression. Should expect to be called more than
         * once. The order results come back is the order they should be
         * displayed.
         */
        void process(PromotionMessageContainer result);
    }
    
    /**
     * Shuts down and releases any resources.
     */
    void shutDown();
    
    /**
     * Returns whether the promotion searcher is enabled and works or not. 
     */
    boolean isEnabled();
}
