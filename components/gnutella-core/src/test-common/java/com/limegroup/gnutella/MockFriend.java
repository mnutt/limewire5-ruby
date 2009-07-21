package com.limegroup.gnutella;

import java.util.LinkedHashMap;
import java.util.Map;

import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.IncomingChatListener;
import org.limewire.friend.api.MessageReader;
import org.limewire.friend.api.MessageWriter;
import org.limewire.friend.api.Network;
import org.limewire.friend.api.PresenceEvent;
import org.limewire.listener.EventListener;


public class MockFriend implements Friend {
    
    private String localID;
    private boolean anonymous;
    private Map<String, FriendPresence> friendPresences; 

    public MockFriend() {
        this(null);
    }
    
    public MockFriend(String localID) {
        this(localID, true);
    }
    
    public MockFriend(String localID, boolean anonymous) {
        this.localID = localID;
        this.anonymous = anonymous;
    }
    
    @Override
    public String getId() {
        return localID;
    }

    @Override
    public String getName() {
        return localID;
    }

    @Override
    public String getRenderName() {
        return  localID;
    }

    @Override
    public void setName(String name) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isAnonymous() {
        return anonymous;
    }

    @Override
    public Network getNetwork() {
        return new Network() {
            @Override
            public String getCanonicalizedLocalID() {
                return localID;
            }

            @Override
            public String getNetworkName() {
                return "";
            }

    @Override
            public Type getType() {
                return Type.XMPP;
            }
        };
    }
    
    public void setFriendPresences(Map<String, FriendPresence> presences) {
        this.friendPresences = presences;
    }
    
    public void addPresence(FriendPresence presence) {
        if(friendPresences == null) {
            // use linked to make iterating easier for tests.
            friendPresences = new LinkedHashMap<String, FriendPresence>();
        }
        friendPresences.put(presence.getPresenceId(), presence);
    }

    public String getFirstName() {
        return null;
    }

    @Override
    public void addPresenceListener(EventListener<PresenceEvent> presenceListener) {
    }

    @Override
    public MessageWriter createChat(MessageReader reader) {
        return null;
    }

    @Override
    public void setChatListenerIfNecessary(IncomingChatListener listener) {
    }

    @Override
    public void removeChatListener() {
    }

    @Override
    public FriendPresence getActivePresence() {
        return null;
    }

    @Override
    public boolean hasActivePresence() {
        return false;
    }

    @Override
    public boolean isSignedIn() {
        return false;
    }

    @Override
    public Map<String, FriendPresence> getPresences() {
        return friendPresences;
    }

    @Override
    public boolean isSubscribed() {
        return false;
    }
}
