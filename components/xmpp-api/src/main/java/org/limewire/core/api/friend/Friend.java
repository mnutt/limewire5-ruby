package org.limewire.core.api.friend;

import java.util.Map;

/** 
 * A Friend.
 */
// TODO: Move this out of xmpp-api and into something that different
//       friend extensions can share.
public interface Friend {

    public static final String P2P_FRIEND_ID = "_@_GNUTELLA_@_";

    /**
     * Returns the ID of the user.  This can be any form of unique ID.
     * For example, an XMPP Friend can be in the form of <code>user@host.com</code>,
     * whereas a Gnutella Friend can be the clientGUID.
     */
    public String getId();

    /**
     * Return the friendly user given name to the user, can be null.
     * For example, an XMPP Friend can be the alias of the user,
     * where a Gnutella friend can be the IP address.
     * */
    public String getName();
    
    /** Returns the best possible name this can be rendered with. */
    public String getRenderName();
    
    /** If getRenderName returns something other than email, will return subString using the first ' ' delimeter*/
    public String getFirstName();

    /** Sets a new name for this Friend. */
    void setName(String name);
    
    /**
     * Returns true if this is an anonymous friend.
     * For example, an XMPP Friend is not anonymous -- it is identified
     * by an email address and is permanent.  A Gnutella Friend is anonymous,
     * in that their existence is temporary and no long-lasting relationship
     * exists.
     * 
     * Callers can use this to determine if data based on this friend is
     * permanent or not.
     */
    boolean isAnonymous();
    
    /** Returns the {@link Network} that this is a friend on. */
    Network getNetwork();

    /**
     * Returns a map of all {@link FriendPresence FriendPresences} for this
     * Friend. Keys are the identifier of the presence, as defined by
     * {@link FriendPresence#getPresenceId()}.
     */
    Map<String, FriendPresence> getFriendPresences();
}
