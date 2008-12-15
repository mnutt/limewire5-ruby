package org.limewire.xmpp.api.client;

import org.limewire.listener.DefaultEvent;

/**
 * This event is dispatched when a chat presence is added or updated
 */
public class PresenceEvent extends DefaultEvent<Presence, Presence.EventType> {

    public PresenceEvent(Presence source, Presence.EventType event) {
        super(source, event);
    }
}
