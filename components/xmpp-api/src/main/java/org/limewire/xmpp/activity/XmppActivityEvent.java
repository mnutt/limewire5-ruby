package org.limewire.xmpp.activity;

import org.limewire.listener.AbstractSourcedEvent;

public class XmppActivityEvent extends AbstractSourcedEvent<XmppActivityEvent.ActivityState>{
    public static enum ActivityState {
        Active, Idle
    }

    public XmppActivityEvent(ActivityState source) {
        super(source);
    }
}
