package org.limewire.xmpp.client.impl;

import org.apache.http.auth.UsernamePasswordCredentials;
import org.limewire.util.BaseTestCase;

public class XMPPAuthenticatorTest extends BaseTestCase {

    private XMPPAuthenticator authenticator;

    public XMPPAuthenticatorTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        authenticator = new XMPPAuthenticator();
    }
    
    public void testAuthenticatesItsAuthToken() {
        String authToken = authenticator.getAuthToken("me@you.com");
        assertTrue(authenticator.authenticate(new UsernamePasswordCredentials("me@you.com", authToken)));
    }
    
    public void testAuthenticateLooksAtUsername() {
        String authToken = authenticator.getAuthToken("me@you.com");
        assertFalse(authenticator.authenticate(new UsernamePasswordCredentials("different@username", authToken)));
    }
    
    public void testAuthenticateHandlesNullPassword() {
        assertFalse(authenticator.authenticate(new UsernamePasswordCredentials("different@username", null)));
    }
}
