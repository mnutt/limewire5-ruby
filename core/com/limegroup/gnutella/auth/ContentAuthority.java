package com.limegroup.gnutella.auth;

import com.limegroup.gnutella.messages.Message;

/** 
 * Allows ContentRequests to be sent.
 * Different ContentAuthorities can send messages to different folks
 * and request permissions from different authorities.
 * 
 * For example..
 *  A IpPortContentAuthority could send messages to a single IpPort.
 *  A DhtContentAuthority could send messages to a DHT.
 */
public interface ContentAuthority {
    
    /** 
     * Initializes this authority. This is allowed to block. 
     * Returns true if initialization succeeded, false otherwise.
     */
    public boolean initialize();
    
    /** Sends a message through this authority. This should not block. */
    public void send(Message m);
    
}
