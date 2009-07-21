package org.limewire.xmpp.client.impl.messages.authtoken;

import java.util.Random;

import org.apache.commons.codec.binary.Base64;
import org.limewire.friend.impl.feature.AuthTokenImpl;
import org.limewire.util.BaseTestCase;
import org.limewire.xmpp.client.impl.messages.IQTestUtils;
import org.limewire.xmpp.client.impl.messages.InvalidIQException;

public class AuthTokenIQTest extends BaseTestCase {

    public AuthTokenIQTest(String name) {
        super(name);
    }
    
    public void testParsesOwnOutput() throws Exception {
        byte[] bytes = new byte[20];
        new Random().nextBytes(bytes);
        AuthTokenImpl token = new AuthTokenImpl(bytes);
        AuthTokenIQ authTokenIQ = new AuthTokenIQ(token);
        
        AuthTokenIQ parsedAuthTokenIQ = new AuthTokenIQ(IQTestUtils.createParser(authTokenIQ.getChildElementXML()));
        AuthTokenImpl decodedTwiceToken = new AuthTokenImpl(Base64.decodeBase64(parsedAuthTokenIQ.getAuthToken().getToken()));
        assertEquals(token, decodedTwiceToken);
    }

    public void testParsesMissingTokenGracefully() throws Exception {
        try {
            new AuthTokenIQ(IQTestUtils.createParser("<auth-token xmlns=\"jabber:iq:lw-auth-token\"></auth-token>"));
            fail("expected invalid iq exception");
        } catch (InvalidIQException iie) {
        }
    }
    
    public void testParsesMissingValueAttributeGracefully() throws Exception {
        try {
            new AuthTokenIQ(IQTestUtils.createParser("<auth-token xmlns=\"jabber:iq:lw-auth-token\"><token/></auth-token>"));
            fail("expected invalid iq exception");
        } catch (InvalidIQException iie) {
        }
    }
}
