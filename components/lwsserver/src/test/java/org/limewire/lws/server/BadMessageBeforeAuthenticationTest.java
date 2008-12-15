package org.limewire.lws.server;

import java.util.HashMap;
import java.util.Map;

import org.limewire.lws.server.LWSDispatcherSupport;

import junit.framework.Test;
import junit.textui.TestRunner;

/**
 * Tests that we handle a bad message correctly.
 */
public class BadMessageBeforeAuthenticationTest extends AbstractCommunicationSupport {

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
        args.put(LWSDispatcherSupport.Parameters.PRIVATE, "asdfasdfasdfasdf");
        getCode().sendLocalMsg("Msg", args,errorHandler(new String[] {
                LWSDispatcherSupport.ErrorCodes.INVALID_PRIVATE_KEY,
                LWSDispatcherSupport.ErrorCodes.UNITIALIZED_PRIVATE_KEY
        }));
    }
}
