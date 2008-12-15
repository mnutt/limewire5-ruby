package org.limewire.xmpp.api.client;

import org.limewire.listener.DefaultEvent;

public class RosterEvent extends DefaultEvent<User, User.EventType> {

    public RosterEvent(User source, User.EventType event) {
        super(source, event);
    }
}