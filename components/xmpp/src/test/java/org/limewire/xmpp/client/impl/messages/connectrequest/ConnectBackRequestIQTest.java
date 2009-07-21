package org.limewire.xmpp.client.impl.messages.connectrequest;

import static org.limewire.xmpp.client.impl.messages.IQTestUtils.createParser;

import org.limewire.io.ConnectableImpl;
import org.limewire.io.GUID;
import org.limewire.net.ConnectBackRequest;
import org.limewire.util.BaseTestCase;
import org.limewire.xmpp.client.impl.messages.InvalidIQException;
import org.xmlpull.v1.XmlPullParserException;


public class ConnectBackRequestIQTest extends BaseTestCase {

    public ConnectBackRequestIQTest(String name) {
        super(name);
    }
    
    public void testParsesItsOwnOutput() throws Exception {
        GUID guid = new GUID();
        ConnectBackRequest connectRequest = new ConnectBackRequest(new ConnectableImpl("129.0.0.1", 5000, true), guid, 1);

        ConnectBackRequest parsedRequest = new ConnectBackRequestIQ(createParser(new ConnectBackRequestIQ(connectRequest).getChildElementXML())).getConnectBackRequest();
        
        assertEquals(connectRequest.getClientGuid(), parsedRequest.getClientGuid());
        assertEquals(connectRequest.getAddress(), parsedRequest.getAddress());
        assertEquals(connectRequest.getSupportedFWTVersion(), parsedRequest.getSupportedFWTVersion());
    }
    
    public void testThrowsOnMissingAddress() throws Exception {
        try {
            new ConnectBackRequestIQ(createParser("<connect-back-request xmlns=\"jabber:iq:lw-connect-request\" client-guid=\"" + new GUID() + "\" supported-fwt-version=\"0\"></connect-back-request>"));
            fail("invalid iq exception expected");
        } catch (XmlPullParserException e) {
            fail(e);
        } catch (InvalidIQException e) {
        }
    }
    
    public void testThrowsOnInvalidGuid() throws Exception {
        try {
            new ConnectBackRequestIQ(createParser("<connect-back-request xmlns=\"jabber:iq:lw-connect-request\" client-guid=\"ffdfdd\" supported-fwt-version=\"0\"><address type=\"direct-connect\" value=\"AIEAAAETiAE=\"/></connect-back-request>"));
            fail("invalid iq exception expected");
        } catch (XmlPullParserException e) {
            fail(e);
        } catch (InvalidIQException e) {
        }
    }
    
    public void testThrowsOnMissingGuid() throws Exception {
        try {
            new ConnectBackRequestIQ(createParser("<connect-back-request xmlns=\"jabber:iq:lw-connect-request\" supported-fwt-version=\"0\"><address type=\"direct-connect\" value=\"AIEAAAETiAE=\"/></connect-back-request>"));
            fail("invalid iq exception expected");
        } catch (XmlPullParserException e) {
            fail(e);
        } catch (InvalidIQException e) {
        }
    }
    
    public void testThrowsOnMissingFWTVersion() throws Exception {
        try {
            new ConnectBackRequestIQ(createParser("<connect-back-request xmlns=\"jabber:iq:lw-connect-request\" client-guid=\"BD2BF8AA3D262F0957AF5F96B7F16600\"><address type=\"direct-connect\" value=\"AIEAAAETiAE=\"/></connect-back-request>"));
            fail("invalid iq exception expected");
        } catch (XmlPullParserException e) {
            fail(e);
        } catch (InvalidIQException e) {
        }
    }
    
    public void testThrowsOnInvalidFWTVersion() throws Exception {
        try {
            new ConnectBackRequestIQ(createParser("<connect-back-request xmlns=\"jabber:iq:lw-connect-request\" client-guid=\"BD2BF8AA3D262F0957AF5F96B7F16600\"  supported-fwt-version=\"A\"><address type=\"direct-connect\" value=\"AIEAAAETiAE=\"/></connect-back-request>"));
            fail("invalid iq exception expected");
        } catch (XmlPullParserException e) {
            fail(e);
        } catch (InvalidIQException e) {
        }
    }
}
