package com.limegroup.gnutella.chat;


import java.net.InetAddress;

import junit.framework.Test;

import org.limewire.net.SocketsManagerImpl;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.stubs.SocketStub;

public class InstantMessengerTest extends BaseTestCase {

    public InstantMessengerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(InstantMessengerTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testIsOutgoing() throws Exception {
        InstantMessenger im = new InstantMessengerImpl("host", 1234, new ActivityCallbackStub(), new SocketsManagerImpl());
        assertTrue(im.isOutgoing());
        assertEquals("host", im.getHost());
        assertEquals(1234, im.getPort());
        
        im = new InstantMessengerImpl(new SocketStub(InetAddress.getByAddress(new byte[] { 1, 2, 3, 4 }), 1234), new ActivityCallbackStub());
        assertFalse(im.isOutgoing());
        assertEquals("1.2.3.4", im.getHost());
        assertEquals(1234, im.getPort());
    }
    
}
