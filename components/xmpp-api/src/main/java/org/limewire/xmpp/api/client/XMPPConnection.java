package org.limewire.xmpp.api.client;

import java.util.Collection;

public interface XMPPConnection {
    
    public XMPPConnectionConfiguration getConfiguration();

    /**
     * logs a user into the xmpp server; blocking call.
     * @throws XMPPException
     */
    public void login() throws XMPPException;
    
    /**
     * logs a user out of the xmpp server; blocking call.
     * @throws XMPPException
     */
    public void logout();
    
    /** Returns true if this connection is logged in. */
    public boolean isLoggedIn();
    
    /** Returns true if this connection is logging in. */
    public boolean isLoggingIn();

    /**
     * Sets a new <code>&lt;presence&gt;</code> mode (i.e., status)
     * @param mode the new mode to set
     */
    public void setMode(Presence.Mode mode);

    /**
     * Add a user to the friend list
     * @param id cannot be null
     * @param name can be null
     * @throws XMPPException
     */
    public void addUser(String id, String name) throws XMPPException;
    
    /**
     * Remove a user from the friend list
     * @param id cannot be null
     * @throws XMPPException
     */
    public void removeUser(String id) throws XMPPException;

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