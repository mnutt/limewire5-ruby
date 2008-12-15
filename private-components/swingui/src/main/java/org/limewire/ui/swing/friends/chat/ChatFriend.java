package org.limewire.ui.swing.friends.chat;

import java.beans.PropertyChangeListener;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.xmpp.api.client.MessageReader;
import org.limewire.xmpp.api.client.MessageWriter;
import org.limewire.xmpp.api.client.Presence.Mode;

/**
 * @author Mario Aquino, Object Computing, Inc.
 *
 */
public interface ChatFriend {
    Friend getFriend();
    
    String getID();
    
    String getName();
    
    void setStatus(String status);
    
    String getStatus();

    void setMode(Mode mode);
    
    Mode getMode();
    
    boolean isChatting();
    
    boolean isActiveConversation();
    
    void setActiveConversation(boolean active);
    
    boolean isSignedInToLimewire();

    boolean isSignedIn();
    
    void startChat();
    
    void stopChat();
    
    long getChatStartTime();
    
    boolean isReceivingUnviewedMessages();
    
    void setReceivingUnviewedMessages(boolean hasMessages);
    
    MessageWriter createChat(MessageReader reader);

    void addPropertyChangeListener(PropertyChangeListener listener);
    
    void removePropertyChangeListener(PropertyChangeListener listener);

    /**
     * Returns the highest priority presence, null if user not signed in.
     * Priority of presences is determined as follows:
     *
     * 1. Active presence
     * 2. Highest priority XMPP presence
     */
    FriendPresence getBestPresence();

    /**
     * updates the state of this chatFriend based on its underlying attributes, for instance 
     * the mode and status of the current active presence
     */
    void update();
}
