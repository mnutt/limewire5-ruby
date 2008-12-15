package com.limegroup.gnutella.handshaking;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
public class StubHandshakeResponder implements HandshakeResponder {
    private HandshakeResponse respondedTo;
    private Map respondedToProps;
    private boolean outgoing;
    private HandshakeResponse respondWith;
    
    public StubHandshakeResponder() {
        this(new StubHandshakeResponse());
    }
    
    public StubHandshakeResponder(HandshakeResponse respondWith) {
        this.respondWith = respondWith;
    }

    public HandshakeResponse respond(HandshakeResponse response, boolean outgoing) {
        this.respondedTo = response;
        // copy because otherwise they can change out from under us.
        this.respondedToProps = new HashMap(response.props());
        this.outgoing = outgoing;
        return respondWith;
    }

    public void setLocalePreferencing(boolean b) {
    }

    public boolean isOutgoing() {
        return outgoing;
    }

    public HandshakeResponse getRespondedTo() {
        return respondedTo;
    }

    public Map getRespondedToProps() {
        return respondedToProps;
    }

}
