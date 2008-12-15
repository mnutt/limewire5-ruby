package org.limewire.promotion;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;

import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.geocode.GeocodeInformation;
import org.limewire.promotion.SearcherDatabase.QueryResult;
import org.limewire.promotion.containers.PromotionMessageContainer;
import org.limewire.promotion.containers.PromotionMessageContainer.GeoRestriction;
import org.limewire.promotion.impressions.ImpressionsCollector;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class PromotionSearcherImpl implements PromotionSearcher {

    private final KeywordUtil keywordUtil;

    private final SearcherDatabase searcherDatabase;

    private final ImpressionsCollector impressionsCollector;

    private final PromotionBinderRepository promotionBinderRepository;

    private final PromotionServices promotionServices;

    private final ExecutorService exec;

    private volatile int maxNumberOfResults = 5;

    @Inject
    public PromotionSearcherImpl(final KeywordUtilImpl keywordUtil,
            final SearcherDatabase searcherDatabase,
            final ImpressionsCollector impressionsCollector,
            final PromotionBinderRepository promotionBinderRepository,
            final PromotionServices promotionServices) {
        this.keywordUtil = keywordUtil;
        this.searcherDatabase = searcherDatabase;
        this.impressionsCollector = impressionsCollector;
        this.promotionBinderRepository = promotionBinderRepository;
        this.promotionServices = promotionServices;
        this.exec = ExecutorsHelper.newThreadPool("SearcherThread");
    }

    /**
     * Order of operations:
     * 
     * <ol>
     * <li> normalize the query using {@link KeywordUtilImpl}
     * <li> expire any db results that have passed
     * <li> request the {@link PromotionBinder} for this query from the
     * {@link PromotionBinderRepository} (may be cached)
     * <li> insert all valid {@link PromotionMessageContainer} entries into db
     * <li> run the db search and callback results, but using maxNumberOfResults
     * as a limit, and using the probability field to decide if a return result
     * should REALLY be shown.
     * </ol>
     * 
     * @param query the searched terms
     * @param callback the recipient of the results
     * @param userLocation this can be <code>null</code>
     */
    public void search(final String query, final PromotionSearchResultsCallback callback,
            final GeocodeInformation userLocation) {
        if (isEnabled()) {
            exec.execute(new Searcher(query, callback, userLocation));
        }
    }

    public void init(final int maxNumberOfResults) throws InitializeException {
        this.maxNumberOfResults = maxNumberOfResults;
        searcherDatabase.init();
    }

    /**
     * This thread handles the real work of the {@link PromotionSearcher},
     * conducting the search and invoking the callback passed to the search
     * method. When the search has completed, this thread dies.
     */
    private class Searcher implements Runnable {
        private final String query;

        private final PromotionSearchResultsCallback callback;

        private final String normalizedQuery;

        /** The latitude/longitude of the user, or null if not known. */
        private final LatitudeLongitude userLatLon;

        /** Two character territory of user ('US') or null if not known. */
        private final String userTerritory;

        Searcher(String query, PromotionSearchResultsCallback callback,
                GeocodeInformation userLocation) {
            this.query = query;
            this.callback = callback;
            this.normalizedQuery = keywordUtil.normalizeQuery(query);
            // Now calculate our latitude/longitude from the Geocode, or null
            if (userLocation == null) {
                this.userLatLon = null;
                this.userTerritory = null;
            } else {
                this.userLatLon = getLatitudeLongitude(userLocation);
                this.userTerritory = userLocation
                        .getProperty(GeocodeInformation.Property.CountryCode);
            }
        }

        /**
         * Pulls out the latitude and longitude properties and puts them into a
         * {@link LatitudeLongitude} instance, or returns null if there is a
         * problem parsing or missing data.
         * 
         * @param geocodeInformation this could be <code>null</code>
         */
        private LatitudeLongitude getLatitudeLongitude(GeocodeInformation geocodeInformation) {
            final String lat = geocodeInformation.getProperty(GeocodeInformation.Property.Latitude);
            final String lon = geocodeInformation
                    .getProperty(GeocodeInformation.Property.Longitude);
            if (lat == null || lon == null)
                return null;
            try {
                return new LatitudeLongitude(Double.parseDouble(lat), Double.parseDouble(lon));
            } catch (NumberFormatException ex) {
                return null;
            }
        }

        public void run() {
            // OK, start the meat of the query!
            final Date timeQueried = new Date();
            // Get the binder (maybe cached), ingest it, and search...
            PromotionBinder binder = null;
            List<QueryResult> results = null;
            binder = promotionBinderRepository.getBinderForBucket(keywordUtil.getHashValue(normalizedQuery));
            try {
                if (binder != null) {
                    searcherDatabase.ingest(binder);
                }
                searcherDatabase.expungeExpired();
                results = searcherDatabase.query(normalizedQuery);
            } catch (DatabaseExecutionException e) {
                promotionServices.stop();
            }

            if (results == null) {
                //
                // This will happen if an error occured in a database operation
                //
                return;
            }

            removeInvalidResults(results, timeQueried);

            List<QueryResult> visibleResults = new ArrayList<QueryResult>(results.size());
            for (QueryResult result : results) {
                if (result.getPromotionMessageContainer().isImpressionOnly()) {
                    impressionsCollector.recordImpression(query, timeQueried, new Date(), result
                            .getPromotionMessageContainer(), result.getBinderUniqueName());
                } else {
                    visibleResults.add(result);
                }
            }
            
            int shownResults = 0;
            int idx = 0;
            for(QueryResult result : visibleResults) {
                final float probability = result.getPromotionMessageContainer().getProbability();
                int remainingToIterateThrough = visibleResults.size() - idx;
                int remainingMaxToShow = maxNumberOfResults - shownResults;
                if (remainingToIterateThrough <= remainingMaxToShow
                        || Math.random() <= probability) {
                    shownResults++;
                    callback.process(result.getPromotionMessageContainer());
                    // record we just showed this result.
                    impressionsCollector
                            .recordImpression(query, timeQueried, new Date(), result
                                    .getPromotionMessageContainer(), result
                                    .getBinderUniqueName());
                }
                
                idx++;

                // Exit early if we're done. Assumes impression-only are sorted
                // at top.
                if (shownResults >= maxNumberOfResults)
                    break;
            }
        }

        private boolean isMessageValid(final PromotionMessageContainer promotionMessageContainer,
                final String binderUniqueName, long currentTimeMillis) {
            final PromotionBinder binder = searcherDatabase.getBinder(binderUniqueName);
            if (binder == null)
                return false;
            return binder.isValidMember(promotionMessageContainer, true, currentTimeMillis);
        }

        /**
         * Strips out results that don't seem to be valid due to territory,
         * radius, or other restriction.
         */
        private void removeInvalidResults(List<QueryResult> results, Date timeQueried) {
            for (QueryResult result : new ArrayList<QueryResult>(results)) {
                PromotionMessageContainer promo = result.getPromotionMessageContainer();
                List<GeoRestriction> restrictions = promo.getGeoRestrictions();
                // Check that the user is within any geo restrictions
                if (restrictions.size() > 0) {
                    if (userLatLon == null) {
                        // promo is restricted but we don't know the user's
                        // location so don't show it.
                        results.remove(result);
                        continue;
                    } else {
                        boolean matchedAtLeastOneRestriction = false;
                        for (GeoRestriction restriction : restrictions) {
                            if (restriction.contains(userLatLon)) {
                                matchedAtLeastOneRestriction = true;
                                break;
                            }
                        }
                        if (!matchedAtLeastOneRestriction) {
                            // The user is not within any of the restrictions,
                            // so
                            // remove the result
                            results.remove(result);
                            continue;
                        }
                    }
                }
                // Check that the user is within any territories
                Locale[] territories = promo.getTerritories();
                if (territories.length > 0) {
                    if (userTerritory == null) {
                        // promo is restricted but we don't know the user's
                        // location so don't show it.
                        results.remove(result);
                        continue;
                    } else {
                        boolean matchAtLeastOneTerritory = false;
                        for (Locale territory : territories) {
                            if (userTerritory.equalsIgnoreCase(territory.getCountry())) {
                                matchAtLeastOneTerritory = true;
                                break;
                            }
                        }
                        if (!matchAtLeastOneTerritory) {
                            // User isn't in any valid territories
                            results.remove(result);
                            continue;
                        }
                    }
                }
                
                if (!isMessageValid(result.getPromotionMessageContainer(), result
                        .getBinderUniqueName(), timeQueried.getTime())) {
                    results.remove(result);
                    continue;
                }
            }
        }
    }

    public void shutDown() {
        searcherDatabase.shutDown();
    }

    @Override
    public boolean isEnabled() {
        return promotionServices.isRunning();
    }
}
