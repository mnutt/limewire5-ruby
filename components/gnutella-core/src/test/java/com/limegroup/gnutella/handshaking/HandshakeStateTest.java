package com.limegroup.gnutella.handshaking;

import java.util.List;
import java.util.Properties;

import junit.framework.Test;

import com.limegroup.gnutella.util.LimeTestCase;

public class HandshakeStateTest extends LimeTestCase {
    
    public HandshakeStateTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(HandshakeStateTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testGetIncomingHandshakeStates() {
        List states = HandshakeState.getIncomingHandshakeStates(new HandshakeSupport("127.0.0.1"),
                                                                new StubHandshakeResponder());
        assertInstanceof(ReadHandshakeState.ReadRequestState.class, states.remove(0));
        assertInstanceof(WriteHandshakeState.WriteResponseState.class, states.remove(0));
        assertInstanceof(ReadHandshakeState.ReadResponseState.class, states.remove(0));
        assertTrue(states.isEmpty());
    }

    public void testGetOutgoingHandshakeStates() {
        List states = HandshakeState.getOutgoingHandshakeStates(new HandshakeSupport("127.0.0.1"),
                                                                new Properties(),
                                                                new StubHandshakeResponder());
        assertInstanceof(WriteHandshakeState.WriteRequestState.class, states.remove(0));
        assertInstanceof(ReadHandshakeState.ReadResponseState.class, states.remove(0));
        assertInstanceof(WriteHandshakeState.WriteResponseState.class, states.remove(0));
        assertTrue(states.isEmpty());
    }

}
