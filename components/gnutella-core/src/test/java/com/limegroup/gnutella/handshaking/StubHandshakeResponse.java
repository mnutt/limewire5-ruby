package com.limegroup.gnutella.handshaking;

import java.util.Properties;

public class StubHandshakeResponse extends HandshakeResponse {

    
    public StubHandshakeResponse() {
        this(200, "OK", new Properties());
    }
    
    public StubHandshakeResponse(int code, String msg, Properties props) {
        super(code, msg, props);
    }

}
