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
     * Used to register a listener for new incoming chats
     * @param listener the <code>IncomingChatListener</code> to be used
     */
    public void setIncomingChatListener(IncomingChatListener listener);

    /**
     * Used for removing the existing listener set in {@link #setIncomingChatListener}
     * for new incoming chats.
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
}
