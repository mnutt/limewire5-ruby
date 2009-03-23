package com.limegroup.gnutella.lws.server;

import junit.framework.Test;
import junit.textui.TestRunner;

import org.limewire.lws.server.LWSConnectionListener;
import org.limewire.lws.server.LWSDispatcherSupport;
import org.limewire.lws.server.LWSServerUtil;

/**
 * Make's sure {@link LWSConnectionListener}s are working.
 */
public class ConnectionListenerTest extends AbstractCommunicationSupportWithNoLocalServer {

    public ConnectionListenerTest(String s) {
        super(s);
    }

    public static Test suite() {
        return buildTestSuite(ConnectionListenerTest.class);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    public void testAuthenticate() {
        final boolean[] connected = { false };
        LWSConnectionListener lis = new LWSConnectionListener() {
            public void connectionChanged(boolean isConnected) {
                connected[0] = isConnected;
            }
        };
        getLWSManager().addConnectionListener(lis);
        String res = doAuthenticate();
        String privateKey = getPrivateKey();

        assertEquals("invalid log in '" + res + "'", LWSDispatcherSupport.Responses.OK, res);
        assertTrue("invalid private key '" + res + "'", LWSServerUtil.isValidPrivateKey(privateKey));
        assertTrue("not connected", connected[0]);

        // Make sure we can remove it
        getLWSManager().removeConnectionListener(lis);
        doDetatch();
        assertTrue("not connected", connected[0]);
        
        
    }

}
