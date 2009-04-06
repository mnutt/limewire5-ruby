package org.limewire.xmpp.api.client;

import java.util.List;

import org.limewire.core.api.friend.Network;
import org.limewire.io.UnresolvedIpPort;
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
    
    /**
     * A list of jabber servers to use for this configuration in the 
     * event that RFC 3920 SRV lookup fails to find valid entries
     * in DNS.
     * @return a list of jabber servers; never null
     */
    public List<UnresolvedIpPort> getDefaultServers();
}
