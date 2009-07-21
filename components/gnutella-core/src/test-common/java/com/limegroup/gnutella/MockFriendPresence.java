package com.limegroup.gnutella;

import org.limewire.friend.api.Friend;
import org.limewire.friend.api.feature.Feature;
import org.limewire.friend.impl.AbstractFriendPresence;

public class MockFriendPresence extends AbstractFriendPresence {
    
    private MockFriend friend;
    private String presenceId;
    
    public MockFriendPresence() {
        this(new MockFriend(), null);
    }
    
    public MockFriendPresence(MockFriend friend, String presenceId, Feature...features) {
        this.presenceId = presenceId;
        this.friend = friend;
        for(Feature feature : features) {
            addFeature(feature);
        }
    }
    
    @Override
    public Friend getFriend() {
        return friend;
    }

    @Override
    public String getPresenceId() {
        return presenceId;
    }

    @Override
    public Mode getMode() {
        return null;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public String getStatus() {
        return null;
    }

    @Override
    public Type getType() {
        return null;
    }
}
