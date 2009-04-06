package com.limegroup.gnutella.lws.server;

import junit.framework.Test;
import junit.textui.TestRunner;

import org.limewire.lws.server.LWSDispatcherSupport;
import org.limewire.lws.server.LWSServerUtil;
import org.limewire.lws.server.LWSDispatcherSupport.Responses;

/**
 * Tests basic authentication.
 */
public class AuthenticateTest extends AbstractCommunicationSupportWithNoLocalServer {

    public AuthenticateTest(String s) {
        super(s);
    }

    public static Test suite() {
        return buildTestSuite(AuthenticateTest.class);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    public void testAuthenticate() {
        final String res = doAuthenticate();
        assertEquals("invalid private key '" + res + "'", Responses.OK, res);
    }

    public void testPing() {
        doAuthenticate();
        assertEquals(LWSDispatcherSupport.PING_BYTES.length, sendPing().length());
    }
    
    public void testPingBeforeAuthentication() {
        assertEquals(LWSDispatcherSupport.ErrorCodes.UNKNOWN_COMMAND,LWSServerUtil.unwrapError(sendPing()));
    }    

}
