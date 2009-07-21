package org.limewire.facebook.service;

import org.limewire.friend.api.feature.FeatureEvent;
import org.limewire.friend.impl.AbstractFriendPresence;
import org.limewire.listener.EventBroadcaster;
import org.limewire.util.StringUtils;

/**
 * A <code>FriendPresence</code> that represents a facebook friend that is online and visible
 * for chatting.
 */
public class FacebookFriendPresence extends AbstractFriendPresence {

    private final String presenceId;
    private final FacebookFriend friend;

    public FacebookFriendPresence(String presenceId, FacebookFriend friend, EventBroadcaster<FeatureEvent> featureEventBroadcaster) {
        super(featureEventBroadcaster);
        this.presenceId = presenceId;
        this.friend = friend;
    }
    
    @Override
    public FacebookFriend getFriend() {
        return friend;
    }

    @Override
    public Mode getMode() {
        return Mode.available;
    }

    @Override
    public String getPresenceId() {
        return presenceId;
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
        return Type.available;
    }
    
    @Override
    public String toString() {
        return StringUtils.toString(this);
    }

}
