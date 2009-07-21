package org.limewire.ui.swing.friends.chat;

import java.util.HashMap;
import java.util.Map;

import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.IncomingChatListener;
import org.limewire.friend.api.MessageReader;
import org.limewire.friend.api.MessageWriter;
import org.limewire.friend.api.Network;
import org.limewire.friend.api.PresenceEvent;
import org.limewire.listener.EventListener;

public class MockUser implements Friend {
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
    public Map<String, FriendPresence> getPresences() {
        return new HashMap<String, FriendPresence>();
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
    public FriendPresence getActivePresence() {
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

            @Override
            public Type getType() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }
        };
    }
}
