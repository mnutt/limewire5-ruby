package org.limewire.xmpp.api.client;

import org.limewire.listener.DefaultEvent;

public class FriendRequestEvent extends DefaultEvent<FriendRequest, FriendRequest.EventType> {

    public FriendRequestEvent(FriendRequest source, FriendRequest.EventType event) {
        super(source, event);
    }
}
