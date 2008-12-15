package com.limegroup.gnutella.lws.server;

import java.util.HashMap;
import java.util.Map;

import org.limewire.lws.server.LWSDispatcherSupport;
import org.limewire.lws.server.LWSServerUtil;
import org.limewire.lws.server.LWSDispatcherSupport.Parameters;

import junit.framework.Test;
import junit.textui.TestRunner;

/**
 * Tests the proper way to handle a good message out of order. Even if we send
 * an authentication message we would like to handle it from the beginning.
 */
public class SendingMessagesBeforeAuthenticatingTest extends AbstractCommunicationSupportWithNoLocalServer {

    public SendingMessagesBeforeAuthenticatingTest(String s) {
        super(s);
    }

    public static Test suite() {
        return buildTestSuite(SendingMessagesBeforeAuthenticatingTest.class);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    public void testSendAuthenticationWithoutPrivateKey() {
        String res = sendMessageFromWebpageToClient(LWSDispatcherSupport.Commands.AUTHENTICATE, 
                                                          NULL_ARGS);
        assertTrue("invalid private key '" + res + "'", LWSServerUtil.isError(res));
    }
    
    public void testSendAuthenticationWithBadPrivateKey() {
        Map<String, String> args = new HashMap<String, String>();
        args.put(Parameters.PRIVATE, "asdfasdf"); // This is too short to be valid
        String res = sendMessageFromWebpageToClient(LWSDispatcherSupport.Commands.AUTHENTICATE, 
                                                          args);
        assertTrue("invalid private key '" + res + "'", LWSServerUtil.isError(res));
    }
    
    public void testAlreadyAuthenticated() {
        String privateKey = doAuthenticate();
        Map<String, String> args = new HashMap<String, String>();
        args.put(Parameters.PRIVATE, privateKey);
        String res = sendMessageFromWebpageToClient(LWSDispatcherSupport.Commands.AUTHENTICATE, 
                                                          args);
        assertTrue("invalid private key '" + res + "'", LWSServerUtil.isError(res));
    }

}
