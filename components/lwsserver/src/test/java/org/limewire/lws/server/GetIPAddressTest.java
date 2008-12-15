package org.limewire.lws.server;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.limewire.lws.server.LWSServerUtil;
import org.limewire.util.BaseTestCase;

import junit.framework.Test;
import junit.textui.TestRunner;

public class GetIPAddressTest extends BaseTestCase {

    public GetIPAddressTest(String s) { super(s); }
    
    public static Test suite() {
        return buildTestSuite(GetIPAddressTest.class);
    }
    
    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    public void testNull() {
        InetAddress addr = null;
        String ip = LWSServerUtil.getIPAddress(addr);
        assertNull(ip);
    }

    public void testSimple() {
        try {
            InetAddress addr = InetAddress.getByName("localhost");
            String ip = LWSServerUtil.getIPAddress(addr);
            assertEquals("127.0.0.1", ip);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
}
