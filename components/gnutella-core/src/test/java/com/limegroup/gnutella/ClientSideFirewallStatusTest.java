package com.limegroup.gnutella;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import org.limewire.util.StringUtils;

import com.limegroup.gnutella.messages.vendor.CapabilitiesVM;

import junit.framework.Test;

public class ClientSideFirewallStatusTest extends ClientSideTestCase {

    public ClientSideFirewallStatusTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(ClientSideFirewallStatusTest.class);
    }
    
    @Override
    public int getNumberOfPeers() {
        return 1;
    }
    
    public void testFirewalledNoFWT() throws Exception {
        // trigger a capabilities resend
        injector.getInstance(ConnectionManager.class).sendUpdatedCapabilities();
        CapabilitiesVM cvm = BlockingConnectionUtils.getFirstInstanceOfMessageType(testUP[0], CapabilitiesVM.class);
        assertFalse(cvm.canAcceptIncomingTCP());
        assertFalse(cvm.canDoFWT());
    }
    
    public void testIncomingTriggersStatusUpdate() throws Exception {
        // nothing arrives for a while
        assertNull(BlockingConnectionUtils.getFirstInstanceOfMessageType(testUP[0], CapabilitiesVM.class));
        SocketAddress me = new InetSocketAddress("127.0.0.1",SERVER_PORT);
        Socket s = new Socket();
        s.connect(me);
        OutputStream os = s.getOutputStream();
        os.write(StringUtils.toAsciiBytes("CONNECT \n\n"));
        os.flush();
        os.close();
        Thread.sleep(50);
        
        CapabilitiesVM cvm = BlockingConnectionUtils.getFirstInstanceOfMessageType(testUP[0], CapabilitiesVM.class);
        assertTrue(cvm.canAcceptIncomingTCP());
        assertFalse(cvm.canDoFWT());
    }
}
