package org.limewire.core.api.connection;

/** The strength of your Gnutella connection. */
public enum ConnectionStrength {
    // THE IDs associated with each enum SHOULD NEVER CHANGE.
    // We are purposely NOT using the ordinal, because
    // ordinals can change over time.  These ids cannot,
    // because they are used by external code.
    
    /** You aren't connected to the Internet at all. */
    NO_INTERNET(0),
    
    /** You might be connected to the Internet, but not connected to Gnutella. */
    DISCONNECTED(1),
    
    /** You are attempting to connect to Gnutella. */
    CONNECTING(2),
    
    /** You have a weak connection to Gnutella. */
    WEAK(3),
    
    /** You have a slightly better than weak connection to Gnutella. **/
    WEAK_PLUS(4),
    
    /** Your connection to Gnutella is OK, but could be better. */
    MEDIUM(5), 
    
    /** Your connection to Gnutella is a little better than OK, but still, it could be better. */
    MEDIUM_PLUS(6),
    
    /** You are fully connected to Gnutella. */
    FULL(7), 
    
    /** You have a kickass connection to Gnutella. */
    TURBO(8);
    
    private final int strengthId;
    
    private ConnectionStrength(int id) {
        this.strengthId = id;
    }

    /**
     * Returns the ID associated with this strength. A given strength's ID will
     * never change.
     */
    public int getStrengthId() {
        return strengthId;
    }
}