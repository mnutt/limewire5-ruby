package org.limewire.xmpp.api.client;

import org.limewire.listener.DefaultSourceTypeEvent;

public class XMPPConnectionEvent extends DefaultSourceTypeEvent<XMPPConnection, XMPPConnectionEvent.Type> {

    public static enum Type {
        CONNECTING, CONNECTED, CONNECT_FAILED, DISCONNECTED
    }
    
    private final Exception exception;
    
    public XMPPConnectionEvent(XMPPConnection source, XMPPConnectionEvent.Type event) {
        this(source, event, null);
    }

    public XMPPConnectionEvent(XMPPConnection source, XMPPConnectionEvent.Type event, Exception exception) {
        super(source, event);
        this.exception = exception;
    }
    
    /**
     * @return null if there is no exception that was the cause of this event
     */
    public Exception getException() {
        return exception;
    }
}
