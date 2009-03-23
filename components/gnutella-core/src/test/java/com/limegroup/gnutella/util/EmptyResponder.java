package com.limegroup.gnutella.util;

import java.util.Properties;

import com.limegroup.gnutella.handshaking.HandshakeResponder;
import com.limegroup.gnutella.handshaking.HandshakeResponse;

public final class EmptyResponder implements HandshakeResponder {
    public HandshakeResponse respond(HandshakeResponse response, 
                                     boolean outgoing) {
        return HandshakeResponse.createResponse(new Properties());
    }
    
    public void setLocalePreferencing(boolean b) {}
}
