package org.limewire.xmpp.api.client;

public class XMPPException extends Exception{
    public XMPPException(Throwable cause) {
        super(cause);
    }

    public XMPPException(String message) {
        super(message);
    }

    public XMPPException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
