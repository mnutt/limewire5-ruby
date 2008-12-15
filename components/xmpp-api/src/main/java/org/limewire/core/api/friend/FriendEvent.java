package org.limewire.core.api.friend;

import org.limewire.listener.DefaultEvent;

public class FriendEvent extends DefaultEvent<Friend, FriendEvent.Type> {
    
    public static enum Type {
        /** The Friend was added. */
        ADDED,
        /** The Friend was removed. */
        REMOVED,
        /** The Friend was deleted (and will never be added again). */
        DELETE;
    }

    public FriendEvent(Friend source, Type event) {
        super(source, event);
    }
    
}
