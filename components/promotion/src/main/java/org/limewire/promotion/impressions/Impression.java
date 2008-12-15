package org.limewire.promotion.impressions;

import java.util.Date;

import org.limewire.promotion.containers.PromotionMessageContainer;

/**
 * Represents an impression for a single promo message. Should be contained
 * within a {@link UserQueryEvent} for full information about the event,
 * including ordering of results.
 */
public final class Impression {
    private final String binderUniqueName;
    private final Date timeShown;
    private final long promoUniqueID;

    Impression(PromotionMessageContainer promo, String binderUniqueName, Date timeShown) {
        this.binderUniqueName = binderUniqueName;
        this.timeShown = timeShown;
        this.promoUniqueID = promo.getUniqueID();
    }

    public Date getTimeShown() {
        return timeShown;
    }

    public String getBinderUniqueName() {
        return binderUniqueName;
    }
    
    public long getPromoUniqueID() {
        return promoUniqueID;
    }
}
