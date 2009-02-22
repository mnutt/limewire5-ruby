package org.limewire.xmpp.activity;

import org.limewire.listener.DefaultSourceEvent;

public class XmppActivityEvent extends DefaultSourceEvent<XmppActivityEvent.ActivityState>{
    public static enum ActivityState {
        Active, Idle
    }

    public XmppActivityEvent(ActivityState source) {
        super(source);
    }
}
