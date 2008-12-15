package org.limewire.xmpp.api.client;

import org.limewire.listener.DefaultDataEvent;

public class XMPPConnectionEvent extends DefaultDataEvent<XMPPConnection, XMPPConnectionEvent.Type, Exception> {

    public static enum Type {
        CONNECTING, CONNECTED, CONNECT_FAILED, DISCONNECTED
    }
    
    public XMPPConnectionEvent(XMPPConnection source, XMPPConnectionEvent.Type event) {
        super(source, event, null);
    }

    public XMPPConnectionEvent(XMPPConnection source, XMPPConnectionEvent.Type event, Exception data) {
        super(source, event, data);
    }
}
