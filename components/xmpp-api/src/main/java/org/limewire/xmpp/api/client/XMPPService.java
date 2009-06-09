package org.limewire.xmpp.api.client;

import org.limewire.concurrent.ListeningFuture;
import org.limewire.xmpp.api.client.XMPPPresence.Mode;


/**
 * Describes an interface for managing XMPP connections. Only one connection
 * can be logged in at a time.
 */
public interface XMPPService {

    /**
     * Attempts to log in a connection using the specified configuration.
     * Any existing connections will be logged out first.
     * 
     * @param configuration the XMPPConnectionConfiguration to use; can not be null
     *
     * @return a {@link ListeningFuture} of {@link XMPPConnection}
     * 
     * The ExecutionException will be to an XMPPException if an error occurs
     */
    public ListeningFuture<XMPPConnection> login(XMPPConnectionConfiguration configuration);

    /**
     * Logs out any existing connections.
     * @return a {@link ListeningFuture} if callers wish to be
     * notified of completion.
     *
     * The ExecutionException will be to an XMPPException if an error occurs
     */
    public ListeningFuture<Void> logout();

    /**
     * Returns the logged in connection, or null if there isn't one.
     */
    public XMPPConnection getActiveConnection();

    /**
     * A non-blocking method that roughly approximates
     * the logged in status.  Suitable for UI callers;
     * non-UI callers should probably use {@link #isLoggedIn()}
     * @return true if any connections are logged in
     */
    boolean isLoggedIn();
    
    /** Returns true if any connections are currently logging in. */
    public boolean isLoggingIn();
    
    /**
     * Sets a new <code>&lt;presence&gt;</code> mode (i.e., status)
     * for every XMPPConnection
     * 
     * @param mode the new mode to set
     * @return a {@link ListeningFuture} if callers wish to be
     * notified of completion.
     * 
     * The ExecutionException will be to an XMPPException
     * if there is an error sending the xmpp message
     */
    ListeningFuture<Void> setMode(Mode mode);
}
