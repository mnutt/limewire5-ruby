package org.limewire.core.impl.friend;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.Network;

public class GnutellaFriend implements Friend {

    private final String name;
    private final String id;
    private final Map<String, FriendPresence> map; 

    public GnutellaFriend(String name, String id, FriendPresence presence) {
        this.name = name;
        this.id = id;
        Map<String, FriendPresence> map = new HashMap<String, FriendPresence>(1);
        map.put(presence.getPresenceId(), presence);
        this.map = Collections.unmodifiableMap(map);
    }

    @Override
    public boolean isAnonymous() {
        return true;
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
    public String getRenderName() {
        return getName();
    }

    @Override
    public void setName(String name) {
    }

    @Override
    public Network getNetwork() {
        return null;
    }

    public Map<String, FriendPresence> getFriendPresences() {
        return map;
    }

    @Override
    public String getFirstName() {
        return getName();
    }
}
