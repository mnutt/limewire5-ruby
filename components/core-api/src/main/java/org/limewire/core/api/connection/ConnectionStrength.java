package org.limewire.core.api.connection;

/** The strength of your Gnutella connection. */
public enum ConnectionStrength {
    /** You aren't connected to the internet at all. */
    NO_INTERNET,
    
    /** You might be connected to the internet, but not connected to Gnutella. */
    DISCONNECTED,
    
    /** You are attempting to connect to Gnutella. */
    CONNECTING,
    
    /** You have a weak connection to Gnutella. */
    WEAK,
    
    /** You have a slightly better than weak connection to Gnutella **/
    WEAK_PLUS,
    
    /** Your connection to Gnutella is OK, but could be better. */
    MEDIUM, 
    
    /** Your connection to Gnutella is a little better than OK, but still, it could be better. */
    MEDIUM_PLUS,
    
    /** You are fully connected to Gnutella. */
    FULL, 
    
    /** You have a kickass connection to Gnutella. */
    TURBO;
}