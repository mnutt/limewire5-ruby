package com.limegroup.gnutella.filters;

import com.limegroup.gnutella.messages.Message;

public class CompositeFilter implements SpamFilter {
    
    SpamFilter[] delegates;
    
    /**
     * @requires filters not modified while this is in use (rep is exposed!),
     *           filters contains no null elements
     * @effects creates a new spam filter from a number of other filters.
     */
    public CompositeFilter(SpamFilter[] filters) {
        this.delegates=filters;
    }
    
    public boolean allow(Message m) {
        for (int i=0; i<delegates.length; i++) {
            if (! delegates[i].allow(m))
                return false;
        }
        return true;
    }
}
