package com.limegroup.gnutella;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import junit.framework.Test;

import org.limewire.io.GUID;
import org.limewire.io.NetworkUtils;
import org.limewire.util.Base32;

import com.limegroup.gnutella.connection.RoutedConnection;
import com.limegroup.gnutella.helpers.UrnHelper;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutella.messages.vendor.PushProxyAcknowledgement;
import com.limegroup.gnutella.messages.vendor.PushProxyRequest;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.routing.RouteTableMessage;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.EmptyResponder;

/**
 *  Tests that an Ultrapeer correctly handles all aspects of PushProxy.  For
 *  example:
 *  1) handles the VendorMessage exchange as expected
 *  2) handles HTTP requests as expected, forwarding on a PushRequest
 *
 *  This class tests a lot of different pieces of code.
 *
 *  ULTRAPEER_1  ----  CENTRAL TEST ULTRAPEER  ----  ULTRAPEER_2
 *                              |
 *                              |
 *                              |
 *                             LEAF
 *
 *  The leaf must be connected in the first test.
 */
@SuppressWarnings("unchecked")
public final class ServerSidePushProxyTest extends ServerSideTestCase {

	/**
	 * The timeout value for sockets -- how much time we wait to accept 
	 * individual messages before giving up.
	 */
    private static final int TIMEOUT = 2000;

    /**
     * the client guid of the LEAF - please set in the first test.
     */
    private byte[] leafGUIDBytes = null;
    
    /**
     * the client GUID of the leaf as a GUID.
     */
    private GUID leafGUID = null;

    public ServerSidePushProxyTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ServerSidePushProxyTest.class);
    }    
   
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

    @Override
    public int getNumberOfUltrapeers() {
        return 1;
    }

    @Override
    public int getNumberOfLeafpeers() {
        return 1;
    }
	
    public static ActivityCallback getActivityCallback() {
        return new ActivityCallbackStub();
    }

    @Override
    public void setUpQRPTables() throws Exception {
        // for Ultrapeer 1
        QueryRouteTable qrt = new QueryRouteTable();
        qrt.add("leehsus");
        qrt.add("berkeley");
        for (Iterator iter=qrt.encode(null).iterator(); iter.hasNext(); ) {
            ULTRAPEER[0].send((RouteTableMessage)iter.next());
			ULTRAPEER[0].flush();
        }
    }

    // BEGIN TESTS
    // ------------------------------------------------------

    
    protected void setUp() throws Exception {
        super.setUp();
        drainAll();
        Message m = null;
        leafGUIDBytes = GUID.makeGuid();
        leafGUID = new GUID(leafGUIDBytes);
        
        LEAF[0] = blockingConnectionFactory.createConnection("localhost", PORT);
        // routed leaf, with route table for "test"
        LEAF[0].initialize(headersFactory.createLeafHeaders("localhost"), new EmptyResponder(), 1000);
        QueryRouteTable qrt = new QueryRouteTable();
        qrt.add("berkeley");
        qrt.add("susheel");
        qrt.addIndivisible(UrnHelper.UNIQUE_SHA1.toString());
        for (Iterator iter=qrt.encode(null).iterator(); iter.hasNext(); ) {
            LEAF[0].send((RouteTableMessage)iter.next());
            LEAF[0].flush();
        }
        
        // make sure UP is advertised proxy support
        do {
            m = LEAF[0].receive(TIMEOUT);
        } while (!(m instanceof MessagesSupportedVendorMessage)) ;
        assertTrue(((MessagesSupportedVendorMessage)m).supportsPushProxy() > 0);
        
        // send proxy request
        PushProxyRequest req = new PushProxyRequest(new GUID(leafGUIDBytes));
        LEAF[0].send(req);
        LEAF[0].flush();
        
        // wait for ack
        do {
            m = LEAF[0].receive(TIMEOUT);
        } while (!(m instanceof PushProxyAcknowledgement)) ;
        assertTrue(Arrays.equals(m.getGUID(), leafGUIDBytes));
        assertEquals(PORT, ((PushProxyAcknowledgement)m).getListeningPort());
        
        // ultrapeer supports push proxy setup A-OK
    }
    
    /**
     * Integration test to make sure the client guid is set correctly on
     * the connection when the push request is processed.
     */
    public void testClientGuidOfProxiedLeafIsKnown() {
        assertServerHasClientConnectionWithGuid(leafGUID);   
    }
    
    public void testGETWithServerId() throws Exception {
        makeHttpPushRequest("GET", // request method.
                 "/gnutella/push-proxy", // the request
                 "ServerID", // initial param
                 Base32.encode(leafGUIDBytes), // initial value
                 "127.0.0.1", // ip
                 6346, // port
                 null, // params
                 202); // opcode expected in return
    }
    
    public void testGETWithServerIdTLS() throws Exception {
        Map m = new HashMap();
        m.put("tls", "true");
        makeHttpPushRequest("GET", // request method.
                 "/gnutella/push-proxy", // the request
                 "ServerID", // initial param
                 Base32.encode(leafGUIDBytes), // initial value
                 "127.0.0.1", // ip
                 6346, // port
                 m, // params
                 202); // opcode expected in return
    }
    
    public void testGETWithServerIdTLSFalse() throws Exception {
        Map m = new HashMap();
        m.put("tls", "false");
        makeHttpPushRequest("GET", // request method.
                 "/gnutella/push-proxy", // the request
                 "ServerID", // initial param
                 Base32.encode(leafGUIDBytes), // initial value
                 "127.0.0.1", // ip
                 6346, // port
                 m, // params
                 202); // opcode expected in return
    }
    
    public void testHEADWithServerId() throws Exception {
        makeHttpPushRequest("HEAD", "/gnutella/push-proxy", "Serverid", 
                Base32.encode(leafGUIDBytes), "10.238.1.87", 6350, null, 202);
    }
    
    public void testHEADWithServerIdTLS() throws Exception {
        Map m = new HashMap();
        m.put("tls", "true");
        makeHttpPushRequest("HEAD", "/gnutella/push-proxy", "Serverid", 
                Base32.encode(leafGUIDBytes), "10.238.1.87", 6350, m, 202);
    }
    
    public void testHEADWithServerIdTLSFalse() throws Exception {
        Map m = new HashMap();
        m.put("tls", "false");
        makeHttpPushRequest("HEAD", "/gnutella/push-proxy", "Serverid", 
                Base32.encode(leafGUIDBytes), "10.238.1.87", 6350, m, 202);
    }
    
    public void testHEADWithServerIdTLSOther() throws Exception {
        Map m = new HashMap();
        m.put("tls", "asdfa32");
        makeHttpPushRequest("HEAD", "/gnutella/push-proxy", "Serverid", 
                Base32.encode(leafGUIDBytes), "10.238.1.87", 6350, m, 202);
    }
    
    public void testInvalidGUIDWithServerId() throws Exception {
        makeHttpPushRequest("GET", "/gnutella/push-proxy", "serverid",
                Base32.encode(GUID.makeGuid()), "127.0.0.1", 6346, null, 410);
    }
    
    public void testInvalidGUIDWithGuid()  throws Exception {
        makeHttpPushRequest("GET", "/gnutella/push-proxy", "guid", 
                new GUID().toHexString(), "127.0.0.1", 6346, null, 410);
    }
    
    public void testInvalidIP() throws Exception {
        makeHttpPushRequest("GET", "/gnutella/push-proxy", "serverid",
                Base32.encode(leafGUIDBytes), "www.crapalapadapa.com",
                6346, null, 400);
    }
    
    public void testServerIdWithBase16Fails() throws Exception {
        makeHttpPushRequest("GET", "/gnutella/push-proxy", "serverid",
                 leafGUID.toHexString(), "127.0.0.1", 6346, null, 400);
    }
    
    public void testGuidIsBase16() throws Exception {
        makeHttpPushRequest("GET", "/gnutella/push-proxy", "guid",
                leafGUID.toHexString(), "127.0.0.1", 6346, null, 202);
    }
    
    public void testGuidIsBase16TLS() throws Exception {
        Map m = new HashMap();
        m.put("tls", "true");
        makeHttpPushRequest("GET", "/gnutella/push-proxy", "guid",
                leafGUID.toHexString(), "127.0.0.1", 6346, m, 202);
    }
    
    public void testGuidIsBase16TLSFalse() throws Exception {
        Map m = new HashMap();
        m.put("tls", "false");
        makeHttpPushRequest("GET", "/gnutella/push-proxy", "guid",
                leafGUID.toHexString(), "127.0.0.1", 6346, m, 202);
    }
    
    
    public void testGuidWithBase32Fails() throws Exception {
        makeHttpPushRequest("GET", "/gnutella/push-proxy", "guid",
                Base32.encode(leafGUIDBytes), "127.0.0.1", 6346, null, 400);
    }
    
    public void testGuidWithGnet() throws Exception {
        makeHttpPushRequest("GET", "/gnet/push-proxy", "guid",
                Base32.encode(leafGUIDBytes), "127.0.0.1", 6346, null, 400);
    }
        
    public void testFileChangesIndex() throws Exception {
        Map m = new HashMap();
        m.put("file", new Integer(34));
        makeHttpPushRequest("GET", "/gnutella/push-proxy", "guid",
                leafGUID.toHexString(), "127.0.0.1", 6346, m, 202);
    }
    
    public void testFileChangesIndexTLS() throws Exception {
        Map m = new HashMap();
        m.put("file", new Integer(34));
        m.put("tls", "true");
        makeHttpPushRequest("GET", "/gnutella/push-proxy", "guid",
                leafGUID.toHexString(), "127.0.0.1", 6346, m, 202);
    }
    
    public void testFileChangesIndexTLSFalse() throws Exception {
        Map m = new HashMap();
        m.put("file", new Integer(34));
        m.put("tls", "false");
        makeHttpPushRequest("GET", "/gnutella/push-proxy", "guid",
                leafGUID.toHexString(), "127.0.0.1", 6346, m, 202);
    }
    
    public void testFileWithGnet() throws Exception {
        Map m = new HashMap();
        m.put("file", new Integer(34));
        makeHttpPushRequest("GET", "/gnet/push-proxy", "guid",
                leafGUID.toHexString(), "127.0.0.1", 6346, m, 202);
    }
    
    public void testCannotHaveServeridAndGuid() throws Exception {
        Map m = new HashMap();
        m.put("serverid", Base32.encode(leafGUIDBytes));
        makeHttpPushRequest("GET", "/gnutella/push-proxy", "guid",
                leafGUID.toHexString(), "127.0.0.1", 6346, m, 400);
    }
    
    public void testMultipleFileFails() throws Exception {
        Map m = new HashMap();
        m.put("FILE", new Integer(1));
        m.put("file", new Integer(2));
        makeHttpPushRequest("GET", "/gnutella/push-proxy", "guid",
                leafGUID.toHexString(), "127.0.0.1", 6346, m, 400);
    }
    
    public void testFirewallTransferPushProxyWorks() throws Exception {
        Map m = new HashMap();
        m.put("file", new Integer((int)PushRequest.FW_TRANS_INDEX));
        makeHttpPushRequest("GET", "/gnutella/push-proxy", "ServerID",
                 Base32.encode(leafGUIDBytes), "127.0.0.1", 6346, m, 202);
    }
    
    public void testFirewallTransferPushProxyWorksTLS() throws Exception {
        Map m = new HashMap();
        m.put("file", new Integer((int)PushRequest.FW_TRANS_INDEX));
        m.put("tls", "true");
        makeHttpPushRequest("GET", "/gnutella/push-proxy", "ServerID",
                 Base32.encode(leafGUIDBytes), "127.0.0.1", 6346, m, 202);
    }
    
    public void testFirewallTransferPushProxyWorksTLSFalse() throws Exception {
        Map m = new HashMap();
        m.put("file", new Integer((int)PushRequest.FW_TRANS_INDEX));
        m.put("tls", "false");
        makeHttpPushRequest("GET", "/gnutella/push-proxy", "ServerID",
                 Base32.encode(leafGUIDBytes), "127.0.0.1", 6346, m, 202);
    }
    
    private void assertServerHasClientConnectionWithGuid(GUID guid) {
        ConnectionManager connectionManager = injector.getInstance(ConnectionManager.class);
        for (RoutedConnection leaf : connectionManager.getInitializedClientConnections()) {
            if (guid.equals(new GUID(leaf.getClientGUID()))) {
                assertTrue(leaf.isPushProxyFor());
                return;
            }
        }
        fail("No leaf connection found with guid: " + guid);
    }
    
    private void makeHttpPushRequest(String reqMethod, String reqKey, String initKey,
                          String guid, String ip, int port, Map params,
                          int expectedHttpResponseCode)
     throws Exception {
        Socket s = new Socket("localhost", PORT);
        BufferedReader in = 
            new BufferedReader(new InputStreamReader(s.getInputStream()));
        BufferedWriter out = 
            new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
            
        String result = null;
        Message m = null;

        out.write(reqMethod + " " + reqKey + "?");
        out.write(initKey + "=" + guid);
        if( params != null ) {
            for(Iterator i = params.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry entry = (Map.Entry)i.next();
                out.write("&" + entry.getKey() + "=" + entry.getValue());
            }
        }
        out.write(" HTTP/1.1\r\n");
        out.write("X-Node: ");
        out.write(ip + ":" + port + "\r\n");
        out.write("Connection: close\r\n");
        out.write("\r\n");
        out.flush();
        
        // check opcode - less important, but might as well
        result = in.readLine();
        assertGreaterThan(result, -1, result.indexOf("" + expectedHttpResponseCode));
        // clear out other responses
        while (in.readLine() != null) ;
        
        if( expectedHttpResponseCode != 202 ) {
            // leaf NOT expecting PushRequest.
            try {
                do {
                    m = LEAF[0].receive(TIMEOUT);
                    assertTrue(!(m instanceof PushRequest));
                } while (true) ;
            }
            catch (InterruptedIOException expected) {}
        } else {
            // leaf should get PushRequest
            do {
                m = LEAF[0].receive(TIMEOUT);
            } while (!(m instanceof PushRequest)) ;
            PushRequest pr = (PushRequest) m;
            int idx = 0;
            boolean tls = false;
            if(params != null) {
                if(params.get("file") != null )
                    idx = ((Integer)params.get("file")).intValue();
                if(params.get("tls") != null && "true".equalsIgnoreCase((String)params.get("tls")))
                    tls = true;
            }
            assertEquals(idx, pr.getIndex());
            assertEquals(new GUID(leafGUIDBytes), new GUID(pr.getClientGUID()));
            assertEquals(port, pr.getPort());
            assertEquals(ip, NetworkUtils.ip2string(pr.getIP()));
            assertEquals(tls, pr.isTLSCapable());
        }
    }
}
