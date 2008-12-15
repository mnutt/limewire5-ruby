package org.limewire.xmpp.api.client;

import org.limewire.xmpp.api.client.Presence.Mode;



/**
 * Describes an interface for managing XMPP connections. Only one connection
 * can be logged in at a time.
 */
public interface XMPPService {

    /**
     * Attempts to log in a connection using the specified configuration.
     * Any existing connections will be logged out first.
     */
    public XMPPConnection login(XMPPConnectionConfiguration configuration) throws XMPPException;

    /**
     * Logs out any existing connections.
     */
    public void logout();

    /**
     * Returns the logged in connection, or null if there isn't one.
     */
    public XMPPConnection getActiveConnection();
    
    /**
     * Returns true if this is logged in to atleast one host.
     */
    public boolean isLoggedIn();
    
    /** Returns true if any connections are currently logging in. */
    public boolean isLoggingIn();
    
    /**
     * Sets the Mode for all of known the XMPP connections. 
     */
    void setMode(Mode mode);
}
