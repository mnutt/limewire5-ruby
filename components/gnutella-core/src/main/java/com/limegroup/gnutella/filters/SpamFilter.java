package com.limegroup.gnutella.filters;

import com.limegroup.gnutella.messages.Message;

/**
 * A filter to eliminate Gnutella spam.  Subclass to implement custom
 * filters.  Each Gnutella connection has two SpamFilters; the
 * personal filter (for filtering results and the search monitor) and
 * a route filter (for deciding what I even consider).  (Strategy
 * pattern.)  Note that a packet stopped by the route filter will
 * never reach the personal filter.<p>
 *
 * Because one filter is used per connection, and only one invocation of
 * the run(..) method is used, filters are <b>not synchronized</b> by
 * default.  The exception is BlackListFilter, which uses the Singleton
 * pattern and thus must be synchronized.
 */
public interface SpamFilter {
    
    /**
     * Returns true iff this is considered spam and should not be processed.
     */
    boolean allow(Message m);
}
