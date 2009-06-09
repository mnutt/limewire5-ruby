package org.limewire.ui.swing.friends.chat;

import java.util.HashMap;
import java.util.Map;

import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.Network;
import org.limewire.core.api.friend.client.IncomingChatListener;
import org.limewire.core.api.friend.client.MessageReader;
import org.limewire.core.api.friend.client.MessageWriter;
import org.limewire.listener.EventListener;
import org.limewire.xmpp.api.client.XMPPPresence;
import org.limewire.xmpp.api.client.PresenceEvent;
import org.limewire.xmpp.api.client.XMPPFriend;

public class MockUser implements XMPPFriend {
    private String id;
    private String name;
    
    public MockUser(String id, String name) {
        this.id = id;
        this.name = name;
    }
    
    @Override
    public boolean isAnonymous() {
        return false;
    }

    @Override
    public void addPresenceListener(EventListener<PresenceEvent> presenceListener) {
        // TODO Auto-generated method stub

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
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Map<String, FriendPresence> getFriendPresences() {
        return new HashMap<String, FriendPresence>();
    }
    
    @Override
    public Map<String, XMPPPresence> getPresences() {
        return new HashMap<String, XMPPPresence>();
    }

    @Override
    public boolean isSubscribed() {
        return true;
    }

    @Override
    public MessageWriter createChat(MessageReader reader) {
        return null;
    }

    @Override
    public void setChatListenerIfNecessary(IncomingChatListener listener) {
        // TODO Auto-generated method stub
    }

    @Override
    public void removeChatListener() {
        // TODO Auto-generated method stub
    }

    @Override
    public XMPPPresence getActivePresence() {
        return null;
    }

    @Override
    public boolean hasActivePresence() {
        return true;
    }

    @Override
    public boolean isSignedIn() {
        return true;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Network getNetwork() {
        return new Network() {
            public String getCanonicalizedLocalID() {
                return "";
            }

            public String getNetworkName() {
                return "mock-network";
            }
        };
    }
}
