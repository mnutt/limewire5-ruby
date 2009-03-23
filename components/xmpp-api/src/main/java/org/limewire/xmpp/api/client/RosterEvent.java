package org.limewire.xmpp.api.client;

import org.limewire.listener.DefaultDataTypeEvent;

public class RosterEvent extends DefaultDataTypeEvent<User, RosterEvent.Type> {

    public static enum Type {
        USER_ADDED,
        USER_UPDATED,
        USER_DELETED
    }

    public RosterEvent(User data, Type event) {
        super(data, event);
    }
}