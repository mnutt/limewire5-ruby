package org.limewire.xmpp.api.client;

import org.limewire.listener.DefaultSourceTypeEvent;

public class RosterEvent extends DefaultSourceTypeEvent<User, User.EventType> {

    public RosterEvent(User source, User.EventType event) {
        super(source, event);
    }
}