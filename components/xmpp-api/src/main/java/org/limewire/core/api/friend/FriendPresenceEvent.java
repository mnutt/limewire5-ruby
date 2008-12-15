package org.limewire.core.api.friend;

import org.limewire.listener.DefaultEvent;

public class FriendPresenceEvent extends DefaultEvent<FriendPresence, FriendPresenceEvent.Type> {
    
    public static enum Type {
        /** This is a new presence. */
        ADDED, 
        /** This presence is no longer available. */
        REMOVED,
        /** This presence has new information. */
        UPDATE;
    }

    public FriendPresenceEvent(FriendPresence source, Type event) {
        super(source, event);
    }
    
}
