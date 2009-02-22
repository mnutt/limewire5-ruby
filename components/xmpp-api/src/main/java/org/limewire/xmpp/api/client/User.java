package org.limewire.xmpp.api.client;

import java.util.Map;

import org.limewire.core.api.friend.Friend;
import org.limewire.listener.EventListener;

/**
 * Represents a user ("friend") in a persons roster
 */
public interface User extends Friend {

    public enum EventType {
        USER_ADDED,
        USER_UPDATED,
        USER_DELETED
    }

    /**
     * Allows the xmpp service user to register a listener for presence changes of this user
     * @param presenceListener
     */
    public void addPresenceListener(EventListener<PresenceEvent> presenceListener);

    /**
     * Used to initiate a new chat
     * @param reader the <code>MessageReader</code> used to process incoming messages
     * @return the <code>MessageWriter</code> used to send outgoing messages
     */
    public MessageWriter createChat(MessageReader reader);

    /**
     * Used to register a listener for new incoming chats.  If a chat listener is already set,
     * it is necessary to remove it prior to setting a new one. Does nothing if chat
     * listener is already set.
     *
     * @param listener the <code>IncomingChatListener</code> to be used
     */
    public void setChatListenerIfNecessary(IncomingChatListener listener);

    /**
     * Used for removing the existing listener set in {@link #setChatListenerIfNecessary}
     * for new incoming chats.  Does nothing if there is no chat listener set.
     */
    public void removeChatListener();

    /**
     * The active presence is the presence currently
     * chatting with (sending msgs to) me
     *
     * @return presence the active presence.  null if 
     */
    public Presence getActivePresence();

    /**
     * @return true if this user has an associated active presence
     * (presence this user is currently chatting with)
     */
    public boolean hasActivePresence();


    /**
     * Returns whether or not this user is signed in to chat
     * @return true if this user is signed in with at least 1 presence
     */
    public boolean isSignedIn();
    
    /**
     * An analague to {@link Friend#getFriendPresences()},
     * except returns a Map of {@link Presence}, the keys being
     * the {@link Presence#getJID()}.
     */
    public Map<String, Presence> getPresences();

    /**
     * Returns whether the current login is subscribed to this user.
     * This information is in the roster packet.
     * 
     * For instance, if a user sends the current login a friend
     * add request, and the current login accepts, this method
     * will return true.
     *
     * In the following roster packet, my-mutually-accepted-friend is subscribed,
     * and friend-i-rejected-previously and friend-i-requested-but-has-not-responded
     * are not subscribed.
     *
     * <iq to="limebuddytest@gmail.com/WuXLh6tmNLC3320061" id="0Qj6D-15" type="result">
     *   <query xmlns="jabber:iq:roster">
     *     <item jid="my-mutually-accepted-friend@gmail.com" subscription="both" name="Lime Friend">
     *     <item jid="friend-i-rejected-previously@gmail.com" subscription="none"/>
     *     <item jid="friend-i-requested-but-has-not-responded@gmail.com" subscription="none"/>
     *   </query>
     * </iq>
     *
     * @return true if the roster entry for this user has
     * a subscription attriburte equal to "both" or "to"
     * Returns false otherwise ("from" or "none")
     */
    public boolean isSubscribed();
}
