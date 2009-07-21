package org.limewire.ui.swing.friends;

import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.feature.Feature;
import org.limewire.friend.impl.AbstractFriendPresence;

public class MockFriendPresence extends AbstractFriendPresence implements FriendPresence{
    
    private MockFriend friend;
    
    public MockFriendPresence() {
        this(new MockFriend());
    }
    
    public MockFriendPresence(MockFriend friend, Feature...features) {
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
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Type getType() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getStatus() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getPriority() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Mode getMode() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
