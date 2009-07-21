package org.limewire.friend.impl;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.limewire.friend.api.feature.AuthToken;
import org.limewire.friend.impl.DefaultFriendAuthenticator;
import org.limewire.util.BaseTestCase;
import org.limewire.util.StringUtils;

public class DefaultFriendAuthenticatorTest extends BaseTestCase {

    private DefaultFriendAuthenticator authenticator;

    public DefaultFriendAuthenticatorTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        authenticator = new DefaultFriendAuthenticator();
    }
    
    public void testAuthenticatesItsAuthToken() {
        AuthToken authToken = authenticator.getAuthToken("me@you.com");
        assertTrue(authenticator.authenticate(new UsernamePasswordCredentials("me@you.com", StringUtils.getASCIIString(Base64.encodeBase64(authToken.getToken())))));
        assertTrue(authenticator.authenticate(new UsernamePasswordCredentials("me@you.com", authToken.getBase64())));
    }
    
    public void testAuthenticateLooksAtUsername() {
        AuthToken authToken = authenticator.getAuthToken("me@you.com");
        assertFalse(authenticator.authenticate(new UsernamePasswordCredentials("different@username", StringUtils.getASCIIString(Base64.encodeBase64(authToken.getToken())))));
        assertFalse(authenticator.authenticate(new UsernamePasswordCredentials("different@username", authToken.getBase64())));
    }
    
    public void testAuthenticateHandlesNullPassword() {
        assertFalse(authenticator.authenticate(new UsernamePasswordCredentials("different@username", null)));
    }
}
