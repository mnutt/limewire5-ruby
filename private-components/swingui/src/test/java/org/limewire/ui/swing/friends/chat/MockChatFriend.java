package org.limewire.ui.swing.friends.chat;

import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.Map;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.Network;
import org.limewire.xmpp.api.client.MessageReader;
import org.limewire.xmpp.api.client.MessageWriter;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.Presence.Mode;

class MockChatFriend implements ChatFriend {
    private String name, status;
    private Mode state;
    private boolean activeConversation;
    private boolean receivingUnviewedMessages;
    
    public MockChatFriend(String name, String status, Mode state) {
        this.name = name;
        this.state = state;
        this.status = status;
    }
    
    @Override
    public Friend getFriend() {
        return new Friend() {
            @Override
            public String getId() {
                return name;
            }
            @Override
            public String getName() {
                return name;
            }
            @Override
            public String getRenderName() {
                return name;
            }
            @Override
            public String getFirstName() {
                return name;
            }

            @Override
            public void setName(String name) {
                MockChatFriend.this.name = name;
            }
            
            @Override
            public boolean isAnonymous() {
                return false;
            }

            @Override
            public Network getNetwork() {
                return null;
            }

            @Override
            public Map<String, FriendPresence> getFriendPresences() {
                return Collections.emptyMap();
            }
        };
    }

    @Override
    public String getID() {
        //Use name for now - add an id field if needed in the future
        return name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public Mode getMode() {
        return state;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
    }

    MessageReader reader;
    MessageWriter writer;
    @Override
    public MessageWriter createChat(MessageReader reader) {
        this.reader = reader;
        return writer;
    }

    long chatStartTime;
    @Override
    public long getChatStartTime() {
        return chatStartTime;
    }

    boolean chatting;
    @Override
    public boolean isChatting() {
        return chatting || chatStartTime > 0l;
    }

    @Override
    public void startChat() {
        //no-op
    }

    @Override
    public void stopChat() {
        //no-op
    }

    @Override
    public boolean isActiveConversation() {
        return activeConversation;
    }

    @Override
    public void setActiveConversation(boolean active) {
        this.activeConversation = active;
    }

    @Override
    public boolean isSignedInToLimewire() {
        return false;
    }

    @Override
    public boolean isSignedIn() {
        return false;
    }

    @Override
    public boolean isReceivingUnviewedMessages() {
        return receivingUnviewedMessages;
    }

    @Override
    public void setReceivingUnviewedMessages(boolean hasMessages) {
        this.receivingUnviewedMessages = hasMessages;
    }

    @Override
    public Presence getBestPresence() {
        return null;
    }

    @Override
    public void update() {
        // do nothing
    }

    @Override
    public void setMode(Mode mode) {
        this.state = mode;
    }

    @Override
    public void setStatus(String status) {
        this.status = status;
    }
}