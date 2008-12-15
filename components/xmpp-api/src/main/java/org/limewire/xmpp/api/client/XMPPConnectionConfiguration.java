package org.limewire.xmpp.api.client;

import org.limewire.core.api.friend.Network;
import org.limewire.listener.EventListener;

/**
 * Allows the xmpp service user to provide configuration for xmpp login.
 */
public interface XMPPConnectionConfiguration extends Network {
    
    /**
     * Returns true if the account should enable debugging.
     * (This causes a smack debug window to appear.)
     */
    public boolean isDebugEnabled();
    
    /** Returns the username this configuration will use,
     * as entered by the user; not canonicalized */
    public String getUserInputLocalID();
    
    /** Returns the password this configuration will use. */
    public String getPassword();
    
    /**
     * Returns a user-friendly name for the account.
     * This is something like <code>Example</code> for example.com's
     * XMPP server. 
     */
    public String getLabel();
    
    /**
     * Returns the service name that will be applied to the connection.
     * This is something like <code>example.com</code> for example.com's
     * XMPP server.  
     */
    public String getServiceName();
    
    /**
     * Returns a resource identifier that uniquely identifies the connection
     * even if the user is signed in through multiple clients.
     */
    public String getResource();
    
    // FIXME: this is only used by tests
    public EventListener<RosterEvent> getRosterListener();
}
