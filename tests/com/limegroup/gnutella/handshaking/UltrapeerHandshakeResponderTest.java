package com.limegroup.gnutella.handshaking;

import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

import junit.framework.Test;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.UltrapeerSettings;
import org.limewire.util.PrivilegedAccessor;

import com.google.inject.Injector;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.RoutedConnectionStub;
import com.limegroup.gnutella.connection.RoutedConnection;
import com.limegroup.gnutella.util.LimeTestCase;


/**
 * Tests the functionality of the <tt>UltrapeerHandshakeResponderTest</tt> 
 * class.<p>
 *
 * For the Ultrapeer specifications, see:<p>
 *
 * http://groups.yahoo.com/group/the_gdf/files/Proposals/Ultrapeer/Ultrapeers_1.0_clean.html
 */
public final class UltrapeerHandshakeResponderTest extends LimeTestCase {
    
    private Injector injector;
    private ConnectionManager connectionManager;
    private HandshakeResponderFactory handshakeResponderFactory;
    private HeadersFactory headersFactory;
    private ConnectionServices connectionServices;
    
	public UltrapeerHandshakeResponderTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(UltrapeerHandshakeResponderTest.class);
	}

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    /**
     * Always assume we're encoding & accept it.  This simplifies the testing.
     * For further tests on whether how handshaking works in response to these
     * settings, see HandshakeResponseTest.
     */    
    @Override
    public void setUp() {
        ConnectionSettings.ACCEPT_DEFLATE.setValue(true);
        ConnectionSettings.ENCODE_DEFLATE.setValue(true);
        
        injector = LimeTestUtils.createInjector();
        connectionManager = injector.getInstance(ConnectionManager.class);
        handshakeResponderFactory = injector.getInstance(HandshakeResponderFactory.class);
        headersFactory = injector.getInstance(HeadersFactory.class);
        connectionServices = injector.getInstance(ConnectionServices.class);
        
    }    
    
    /**
     * Free all slots
     */
    @Override
    protected void tearDown() throws Exception {
        freeSlots();
    }

    /**
     * Fills all available slots of ConnectionManager so that
     * checks for x < UltrapeerSettings.MAX_LEAVES will always 
     * fail
     */
    private void fillSlots() throws Exception {
        int maxLeaves = UltrapeerSettings.MAX_LEAVES.getValue();
        RoutedConnection[] mc = new RoutedConnection[maxLeaves];
        for(int i = 0; i < mc.length; i++) {
            mc[i] = new RoutedConnectionStub();
        }
        
        PrivilegedAccessor.setValue(connectionManager, 
                "_initializedClientConnections", Arrays.asList(mc));
    }
    
    /**
     * Restores the ConnectionManager._initializedClientConnections List
     */
    private void freeSlots() throws Exception {
        PrivilegedAccessor.setValue(connectionManager, 
                "_initializedClientConnections", Collections.EMPTY_LIST);
    }
    
    /**
     * Tests the method for responding to outgoing connection attempts.
     * The response we send is the final response of the handshake --
     * the third header exchange overall.
     */
    public void testRespondToOutgoingUltrapeer() throws Exception {
        UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(false);
        ConnectionSettings.ALLOW_WHILE_DISCONNECTED.setValue(true);
        setPreferredConnections();

        // test the 3 Ultrapeer cases -- 

        // create the Ultrapeer responder to test off of
        HandshakeResponder responder = 
            handshakeResponderFactory.createUltrapeerHandshakeResponder("23.3.4.5");

        // 1) Ultrapeer-Ultrapeer::No X-Ultrapeer-Needed
        Properties props = headersFactory.createUltrapeerHeaders("40.0.9.8");
        HandshakeResponse headers = HandshakeResponse.createResponse(props);
        
        HandshakeResponse hr = responder.respond(headers, true);

        // we shouldn't send any response header in this case -- it's
        // just assumed that we're becoming an Ultrapeer
        assertTrue("should be accepted", hr.isAccepted());
        assertEquals("should only have deflate header", 1, hr.props().size());
        assertTrue("should be deflated", hr.isDeflateEnabled());


        // 2) Ultrapeer-Ultrapeer::X-Ultrapeer-Needed: true
        props = headersFactory.createUltrapeerHeaders("40.0.9.8");

        // this should be redundant, but make sure it's handled the way
        // we want
        props.put(HeaderNames.X_ULTRAPEER_NEEDED, "true");
        headers = HandshakeResponse.createResponse(props);
        
        hr = responder.respond(headers, true);

        // we shouldn't send any response header in this case -- it's
        // just assumed that we're becoming an Ultrapeer
        assertTrue("should be accepted", hr.isAccepted());
        assertEquals("should only have deflate header", 1, hr.props().size());
        assertTrue("should be deflated", hr.isDeflateEnabled());

        // 3) Ultrapeer-Ultrapeer::X-Ultrapeer-Needed: false
        props = headersFactory.createUltrapeerHeaders("78.9.3.0");
        props.put(HeaderNames.X_ULTRAPEER_NEEDED, "false");
        
        headers = HandshakeResponse.createResponse(props);        
        hr = responder.respond(headers, true);
        assertTrue("should not be an Ultrapeer", !hr.isUltrapeer());
        assertTrue("should be becoming an leaf", hr.isLeaf());
        assertTrue("should be accepted", hr.isAccepted());
        assertEquals("should have two headers", 2, hr.props().size());
        assertTrue("should be deflating",
                hr.isDeflateEnabled());
        ConnectionSettings.ALLOW_WHILE_DISCONNECTED.setValue(false);
    }

    /**
     * Tests to make sure that outgoing connection responses are handled
     * correctly when the host we're responding to is a leaf.
     */
    public void testRespondToOutgoingLeaf() throws Exception {
        ConnectionSettings.ALLOW_WHILE_DISCONNECTED.setValue(true);
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(true);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        setPreferredConnections();

        assertTrue(connectionServices.isSupernode());

        HandshakeResponder responder = 
            handshakeResponderFactory.createUltrapeerHandshakeResponder("23.3.4.5");

        // Leaf-Ultrapeer  --> leaf slots available
        Properties props = headersFactory.createLeafHeaders("78.9.3.0");
        HandshakeResponse headers = HandshakeResponse.createResponse(props);  
        HandshakeResponse hr = responder.respond(headers, true);

        assertTrue("should have returned that we accepted the connection", 
                   hr.isAccepted());
        assertEquals("should only have one header", 1, hr.props().size());
        assertTrue("should deflate to leaf (may change)",
                    hr.isDeflateEnabled());

        
        fillSlots();
        
        hr = responder.respond(headers, true);
        assertTrue("should not have accepted the connection", 
                   !hr.isAccepted());
        assertEquals("should not have any headers", 0, hr.props().size());

        // clean up settings
        ConnectionSettings.ALLOW_WHILE_DISCONNECTED.setValue(false);
    }


    /**
     * Tests the method for responding to incoming connection attempts.
     */
    public void testRespondToIncomingUltrapeer() throws Exception {
        UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(false);
        ConnectionSettings.PREFERENCING_ACTIVE.setValue(true);
        ConnectionSettings.ALLOW_WHILE_DISCONNECTED.setValue(true);
        setPreferredConnections();

        HandshakeResponder responder = 
            handshakeResponderFactory.createUltrapeerHandshakeResponder("23.3.4.5");

        // 1) check the Ultrapeer case -- leaf guidance should be used
        //    here because the Ultrapeer definitely does not have the
        //    maximum number of leaves
        HandshakeResponse up = 
            HandshakeResponse.createResponse(headersFactory.createUltrapeerHeaders("80.45.0.1"));
        
        HandshakeResponse hr = responder.respond(up, false);

        assertTrue("should report Ultrapeer true", hr.isUltrapeer());
        assertTrue("should tell the connecting Ultrapeer to become a leaf", 
                   hr.hasLeafGuidance());
        assertTrue("should be deflated (may change)", hr.isDeflateEnabled());

        //  2) check to make sure that Ultrapeers are accepted as 
        //     Ultrapeer connections when we have enough leaves -- create this
        //     artifially by setting the MAX_LEAVES to zero
        ConnectionSettings.ALLOW_WHILE_DISCONNECTED.setValue(true);
        fillSlots();
        
        hr = responder.respond(up, false);        
        assertTrue("should tell the Ultrapeer to stay an Ultrapeer", 
                   !hr.hasLeafGuidance());
        assertTrue("should still be accepted as an Ultrapeer",
                   hr.isAccepted());
        assertTrue("should be deflating to leaf", hr.isDeflateEnabled());
        UltrapeerSettings.MAX_LEAVES.revertToDefault();
        ConnectionSettings.ALLOW_WHILE_DISCONNECTED.setValue(false);
    }

    /**
     * Test to make sure that incoming leaf connections are handled correctly.
     */
    public void testRespondToIncomingLeaf() throws Exception {
        ConnectionSettings.PREFERENCING_ACTIVE.setValue(true);
        ConnectionSettings.ALLOW_WHILE_DISCONNECTED.setValue(true);
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(true);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        setPreferredConnections();

        assertTrue(connectionServices.isSupernode());
        
          
        // the ultrapeer we'll be testing against
        HandshakeResponder responder = 
            handshakeResponderFactory.createUltrapeerHandshakeResponder("23.3.4.5");


        //  1) check to make sure that leaves are properly accepted as
        //     leaves
        HandshakeResponse leaf = 
            HandshakeResponse.createResponse(headersFactory.createLeafHeaders("80.45.0.1"));
        HandshakeResponse hr = responder.respond(leaf, false);
        
        assertTrue("should report Ultrapeer true", hr.isUltrapeer());
        assertTrue("should be high degree connection", hr.isHighDegreeConnection());
        assertTrue("should be an Ultrapeer query routing connection", 
                   hr.isUltrapeerQueryRoutingConnection());
        assertTrue("should be deflating to leaf", hr.isDeflateEnabled());                   

        //  2) check to make sure that leaves are rejected with X-Try-Ultrapeer
        //     headers when we alread have enough leaves -- create this 
        //     situation artificially by setting the MAX_LEAVES to zero
        fillSlots();
        
        hr = responder.respond(leaf, false);        
        assertTrue("should have rejected the leaf: status code was: "+
                   hr.getStatusLine(), 
                   !hr.isAccepted());

        assertTrue("should have X-Try-Ultrapeer hosts", hr.hasXTryUltrapeers());
        UltrapeerSettings.MAX_LEAVES.revertToDefault();        
        ConnectionSettings.ALLOW_WHILE_DISCONNECTED.setValue(false);
    }
    
    private void setPreferredConnections() throws Exception {
        PrivilegedAccessor.setValue(connectionManager,
                                    "_preferredConnections",
                                    new Integer(ConnectionSettings.NUM_CONNECTIONS.getValue()));
    }
}
