package org.limewire.lws.server;

import java.util.HashMap;
import java.util.Map;

import org.limewire.lws.server.LWSDispatcherSupport;

import junit.framework.Test;
import junit.textui.TestRunner;

/**
 * Tests bad forms of authenticating.
 */
public class BadAuthenticationTest extends AbstractCommunicationSupport {

    public BadAuthenticationTest(String s) {
        super(s);
    }

    public static Test suite() {
        return buildTestSuite(BadAuthenticationTest.class);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    public void testSendAuthenticationWithoutPrivateKeyOrSharedKey() {
        getCode().sendLocalMsg(LWSDispatcherSupport.Commands.AUTHENTICATE,
                               DUMMY_CALLBACK_ARGS,
                               errorHandlerAny());
    }

    public void testSendAuthenticationWithoutPrivateKey() {
        Map<String,String> args = new HashMap<String,String>();
        args.put(LWSDispatcherSupport.Parameters.SHARED, getSharedKey());
        getCode().sendLocalMsg(LWSDispatcherSupport.Commands.AUTHENTICATE,
                               args,
                               errorHandlerAny());
    }
    
    public void testSendAuthenticationWithoutSharedKey() {
        Map<String,String> args = new HashMap<String,String>();
        args.put(LWSDispatcherSupport.Parameters.PRIVATE, getPrivateKey());
        getCode().sendLocalMsg(LWSDispatcherSupport.Commands.AUTHENTICATE,
                               args,
                               errorHandlerAny());
    }

}
