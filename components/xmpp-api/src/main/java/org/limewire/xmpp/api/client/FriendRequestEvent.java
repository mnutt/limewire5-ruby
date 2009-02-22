package org.limewire.xmpp.api.client;

import org.limewire.listener.DefaultSourceTypeEvent;

public class FriendRequestEvent extends DefaultSourceTypeEvent<FriendRequest, FriendRequest.EventType> {

    public FriendRequestEvent(FriendRequest source, FriendRequest.EventType event) {
        super(source, event);
    }
}
