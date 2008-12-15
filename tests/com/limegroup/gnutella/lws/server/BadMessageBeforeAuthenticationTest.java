package com.limegroup.gnutella.lws.server;

import java.util.HashMap;
import java.util.Map;

import org.limewire.lws.server.LWSDispatcherSupport;
import org.limewire.lws.server.LWSServerUtil;

import junit.framework.Test;
import junit.textui.TestRunner;

/**
 * Tests that we handle a bad message correctly.
 */
public class BadMessageBeforeAuthenticationTest extends AbstractCommunicationSupportWithNoLocalServer {

    public BadMessageBeforeAuthenticationTest(String s) {
        super(s);
    }

    public static Test suite() {
        return buildTestSuite(BadMessageBeforeAuthenticationTest.class);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    public void testBadMessageBeforeAuthentication() {
        final Map<String, String> args = new HashMap<String, String>();
        args.put(LWSDispatcherSupport.Parameters.MSG, "badMsg");
        args.put(LWSDispatcherSupport.Parameters.CALLBACK, "callback");
        String res = sendMessageFromWebpageToClient(LWSDispatcherSupport.Commands.AUTHENTICATE, args);
        assertTrue("invalid message '" + res + "'", LWSServerUtil.isError(res));

    }
}
