package org.limewire.xmpp.api.client;

import java.util.Collection;

import org.limewire.listener.DefaultDataTypeEvent;

public class RosterEvent extends DefaultDataTypeEvent<Collection<XMPPFriend>, RosterEvent.Type> {

    public static enum Type {
        FRIENDS_ADDED,
        FRIENDS_UPDATED,
        FRIENDS_DELETED
    }

    public RosterEvent(Collection<XMPPFriend> data, Type event) {
        super(data, event);
    }
}