package org.limewire.promotion;

import java.util.Date;
import java.util.List;

import org.limewire.promotion.containers.PromotionMessageContainer;

/**
 * Provides a transient database that is optimized for storing/retrieving
 * {@link PromotionMessageContainer} entries.
 */
public interface SearcherDatabase {
    /**
     * Ingests or reingests all the promo messages in the given binder. A
     * signature verification is not run during this phase, instead waiting
     * until the query phase to validate that results actually came from the
     * given signed binder.
     * 
     * @throws DatabaseExecutionException if a database operation goes badly
     */
    void ingest(PromotionBinder binder) throws DatabaseExecutionException;

    /**
     * @return a binder that was previously ingested by a call to
     *         {@link #ingest(PromotionBinder)}, or null if no match.
     */
    PromotionBinder getBinder(String binderUniqueName);

    /**
     * @return a binder that was previously ingested by a call to
     *         {@link #ingest(PromotionBinder)}, or null if no match.
     */
    PromotionBinder getBinder(int bucketNumber);

    /**
     * Removes all expired entries from the database.
     *
     * @throws DatabaseExecutionException if a database operation goes badly
     */
    void expungeExpired() throws DatabaseExecutionException;

    /**
     * Drops the database and recreates an empty one.
     * 
     * @throws DatabaseExecutionException if a database operation goes badly
     */
    void clear() throws DatabaseExecutionException;

    /**
     * Performs a search of the database for the given query. The query will be
     * normalized internally.
     * 
     * @param query the search the user entered in the main search box
     * @return A list of {@link QueryResult} instances that match the query
     * 
     * @throws DatabaseExecutionException if a database operation goes badly
     */
    List<QueryResult> query(String query) throws DatabaseExecutionException;

    /**
     * Provides a wrapper around the {@link PromotionMessageContainer} and
     * attaches a few extra values relating to the query that triggered the
     * promotion.
     */
    public interface QueryResult {
        /**
         * @return The unique id of the binder that this result originally came
         *         from.
         */
        String getBinderUniqueName();

        /**
         * 
         * @return The promo message that matched
         */
        PromotionMessageContainer getPromotionMessageContainer();

        /**
         * 
         * @return The query that found this result
         */
        String getQuery();

        /**
         * 
         * @return The time this query result was returned.
         */
        Date getDate();
    }
    
    /** Initializes this class and any required resources. */
    void init() throws InitializeException;
    
    /**
     * Shuts down and releases any resources.
     */
    void shutDown();
}
