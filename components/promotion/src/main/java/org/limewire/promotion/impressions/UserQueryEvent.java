package org.limewire.promotion.impressions;

import java.util.Date;
import java.util.List;
import java.util.Vector;

public class UserQueryEvent {
    
    private List<Impression> impressions = new Vector<Impression>();
    private final String originalQuery;
    private final Date originalQueryTime;

    UserQueryEvent(String originalQuery, Date originalQueryTime) {
        this.originalQuery = originalQuery;
        this.originalQueryTime = originalQueryTime;
    }

    public List<Impression> getImpressions() {
        return impressions;
    }

    public void addImpression(Impression impression) {
        impressions.add(impression);
    }

    public String getOriginalQuery() {
        return originalQuery;
    }

    public Date getOriginalQueryTime() {
        return originalQueryTime;
    }

    @Override
    public int hashCode() {
        return (int) originalQueryTime.getTime();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof UserQueryEvent) {
            UserQueryEvent event = (UserQueryEvent) obj;
            return event.getOriginalQuery().equals(getOriginalQuery())
                    && event.getOriginalQueryTime().equals(getOriginalQueryTime());
        }
        return false;
    }
}
