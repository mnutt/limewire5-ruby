package org.limewire.lws.server;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Test;
import junit.textui.TestRunner;

/**
 * Tests for an OK from authentication.
 */
public class AuthenticationTest extends AbstractCommunicationSupport {

    public AuthenticationTest(String s) {
        super(s);
    }

    public static Test suite() {
        return buildTestSuite(AuthenticationTest.class);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    public void testSendAuthentication() {
        Map<String,String> args = new HashMap<String,String>();
        args.put(LWSDispatcherSupport.Parameters.PRIVATE, getPrivateKey());
        args.put(LWSDispatcherSupport.Parameters.SHARED, getSharedKey());
        args.put(LWSDispatcherSupport.Parameters.CALLBACK, "dummy");
        getCode().sendLocalMsg(LWSDispatcherSupport.Commands.AUTHENTICATE,
                               args,
                               new FakeJavascriptCodeInTheWebpage.Handler() {
                                public void handle(String res) {
                                    assertTrue("OK != " + res, LWSDispatcherSupport.Responses.OK.equalsIgnoreCase(LWSServerUtil.removeCallback(res)));
                                    assertTrue("should be authenticated", getLocalServer().getDispatcher().isAuthenticated());
                                }
            
        });
    }
    
    public void testPing() {
        Map<String,String> args = new HashMap<String,String>();
        args.put(LWSDispatcherSupport.Parameters.PRIVATE, getPrivateKey());
        args.put(LWSDispatcherSupport.Parameters.SHARED, getSharedKey());
        args.put(LWSDispatcherSupport.Parameters.CALLBACK, "dummy");
        getCode().sendLocalMsg(LWSDispatcherSupport.Commands.AUTHENTICATE,
                               args,
                               new FakeJavascriptCodeInTheWebpage.Handler() {
                                public void handle(String res) {
                                    assertTrue("OK != " + res, LWSDispatcherSupport.Responses.OK.equalsIgnoreCase(LWSServerUtil.removeCallback(res)));
                                    assertTrue("should be authenticated", getLocalServer().getDispatcher().isAuthenticated());
                                }
            
        });
        getCode().sendPing(getPrivateKey(), getSharedKey(), new FakeJavascriptCodeInTheWebpage.Handler() {
            public void handle(String res) {
                //
                // This will be encoded strangely, but just check the length
                //
                String s = new String(LWSDispatcherSupport.PING_BYTES);
                assertTrue("'" + s + "' should contain PNG", s.indexOf("PNG") != -1);
                assertTrue("should be authenticated", getLocalServer().getDispatcher().isAuthenticated());
            }
        });
    }    
    
    public void testPingWithoutAuthenticating() {
        getCode().sendPing("0", "0", new FakeJavascriptCodeInTheWebpage.Handler() {
            public void handle(String res) {
                assertFalse("should NOT be authenticated", getLocalServer().getDispatcher().isAuthenticated());
                assertEquals(LWSDispatcherSupport.ErrorCodes.UNKNOWN_COMMAND, LWSServerUtil.unwrapError(LWSServerUtil.removeCallback(res)));
            }
        });
    }
    
    @Override
    protected void afterSetup() {System.out.println(getLocalServer().getDispatcher());
        assertFalse("should NOT be authenticated", getLocalServer().getDispatcher().isAuthenticated());
    }
  
}
