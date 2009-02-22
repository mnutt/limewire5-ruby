package org.limewire.xmpp.api.client;

import org.limewire.listener.DefaultSourceTypeEvent;

/**
 * This event is dispatched when a chat presence is added or updated
 */
public class PresenceEvent extends DefaultSourceTypeEvent<Presence, Presence.EventType> {

    public PresenceEvent(Presence source, Presence.EventType event) {
        super(source, event);
    }
}
