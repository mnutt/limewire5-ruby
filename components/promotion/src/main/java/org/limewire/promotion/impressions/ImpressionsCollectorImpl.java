package org.limewire.promotion.impressions;

import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.limewire.promotion.containers.PromotionMessageContainer;

import com.google.inject.Singleton;

@Singleton
public final class ImpressionsCollectorImpl implements ImpressionsCollector {
    private Map<String, UserQueryEvent> queries = new Hashtable<String, UserQueryEvent>();

    public Set<UserQueryEvent> getCollectedImpressions() {
        synchronized (queries) {
            return new HashSet<UserQueryEvent>(queries.values());
        }
    }

    /*
     * We store the impression into a query event, and store that event in our
     * queries map. If the event doesn't yet exist, create it.
     */
    public void recordImpression(final String originalQuery, final Date timeQueried,
            final Date timeShown, final PromotionMessageContainer promo,
            final String binderUniqueName) {
        UserQueryEvent event = queries.get(getMapKey(originalQuery, timeQueried));
        synchronized (queries) {
            if (event == null) {
                event = new UserQueryEvent(originalQuery, timeQueried);
                queries.put(getMapKey(originalQuery, timeQueried), event);
            }
            event.addImpression(new Impression(promo, binderUniqueName, timeShown));
        }
    }

    /** The key we use to store/retrieve. */
    private String getMapKey(final String originalQuery, final Date timeQueried) {
        return originalQuery + ":" + timeQueried.getTime();
    }

    public void removeImpressions(final Set<UserQueryEvent> events) {
        synchronized (queries) { 
            for (UserQueryEvent event : events)
                queries.remove(getMapKey(event.getOriginalQuery(), event.getOriginalQueryTime()));
        }
    }

}
