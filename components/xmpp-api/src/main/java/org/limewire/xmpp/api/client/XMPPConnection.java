package org.limewire.xmpp.api.client;

import java.util.Collection;

import org.limewire.concurrent.ListeningFuture;

public interface XMPPConnection {
    
    public XMPPConnectionConfiguration getConfiguration();

    /**
     * logs a user into the xmpp server.
     * @return a {@link ListeningFuture} if callers wish to be
     * notified of completion.
     * 
     * The ExecutionException will be to an XMPPException if an error occurs
     */
    public ListeningFuture<Void> login();
    
    /**
     * logs a user out of the xmpp server.
     * @return a {@link ListeningFuture} if callers wish to be
     * notified of completion.
     * 
     * The ExecutionException will be to an XMPPException if an error occurs
     */
    public ListeningFuture<Void> logout();

    /**
     * @return true if this connection is logged in
     */
    public boolean isLoggedIn();
    
    /**
     * @return true if this connection is now logging in
     */
    public boolean isLoggingIn();

    /**
     * Sets a new <code>&lt;presence&gt;</code> mode (i.e., status)
     * @param mode the new mode to set
     * @return a {@link ListeningFuture} if callers wish to be
     * notified of completion.
     * 
     * The ExecutionException will be to an XMPPException
     * if there is an error sending the xmpp message
     */
    public ListeningFuture<Void> setMode(Presence.Mode mode);

    /**
     * Add a user to the friend list
     * @param id cannot be null
     * @param name can be null
     * @return a {@link ListeningFuture} if callers wish to be
     * notified of completion.
     * 
     * The ExecutionException will be to an XMPPException
     * if there is an error sending the xmpp message
     */
    public ListeningFuture<Void> addUser(String id, String name);
    
    /**
     * Remove a user from the friend list
     * @param id cannot be null
     * @return a {@link ListeningFuture} if callers wish to be
     * notified of completion.
     * 
     * The ExecutionException will be to an XMPPException
     * if there is an error sending the xmpp message
     */
    public ListeningFuture<Void> removeUser(String id);

    /**
     * Returns the user belonging to <code>id</code>. <code>id</code>
     * is the user's email address.
     * 
     * @return null if id is not registered on this connection
     */
    public User getUser(String id);

    /**
     * @return a copy of the current Collection of Users. Does NOT stay up to
     * date with changes.
     */
    public Collection<User> getUsers();
}