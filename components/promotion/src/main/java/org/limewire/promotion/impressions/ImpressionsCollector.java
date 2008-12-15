package org.limewire.promotion.impressions;

import java.util.Date;
import java.util.Set;

import org.limewire.promotion.containers.PromotionMessageContainer;

/**
 * Provides a central collection point for impression data to pool prior to
 * being sent back to the Lime promo servers.
 */
public interface ImpressionsCollector {
    /**
     * Records an impression hit into memory, to be retrieved later in an
     * aggregate form to send back to the Lime servers. 'originalQuery +
     * timeQueried' is used to aggregate impressions to a single 'user search'
     * event.
     */
    void recordImpression(String originalQuery, Date timeQueried, Date timeShown,
            PromotionMessageContainer promo, String binderUniqueName);

    /**
     * @return a Set of {@link UserQueryEvent} entries that have been created
     *         through one or more calls to
     *         {@link #recordImpression(String, Date, Date, PromotionMessageContainer, long)}
     *         or an empty list if no impressions have happened.
     */
    Set<UserQueryEvent> getCollectedImpressions();

    /**
     * Removes the given events (and their impressions) from the backing store.
     * Called after the system has successfully returned the list to the central
     * servers and is ready to remove it from memory.
     */
    void removeImpressions(Set<UserQueryEvent> events);

}
