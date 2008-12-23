package org.limewire.core.impl.xmpp;

import org.limewire.listener.EventBroadcaster;
import org.limewire.xmpp.activity.XmppActivityEvent;

public class MockActivityEventBroadcaster implements EventBroadcaster<XmppActivityEvent> {

    public XmppActivityEvent event;
    @Override
    public void broadcast(XmppActivityEvent event) {
        this.event = event;
    }
}
