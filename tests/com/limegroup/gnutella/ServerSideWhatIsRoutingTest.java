package com.limegroup.gnutella;

import java.io.File;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.FilterSettings;
import org.limewire.core.settings.NetworkSettings;
import org.limewire.core.settings.UltrapeerSettings;
import org.limewire.io.GUID;
import org.limewire.util.TestUtils;

import com.google.inject.Injector;
import com.google.inject.Stage;
import com.limegroup.gnutella.connection.BlockingConnection;
import com.limegroup.gnutella.connection.BlockingConnectionFactory;
import com.limegroup.gnutella.handshaking.HeadersFactory;
import com.limegroup.gnutella.helpers.UrnHelper;
import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.messages.FeatureSearchData;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactory;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.routing.RouteTableMessage;
import com.limegroup.gnutella.util.EmptyResponder;
import com.limegroup.gnutella.util.LimeTestCase;


public final class ServerSideWhatIsRoutingTest extends LimeTestCase {

	/**
	 * The port that the central Ultrapeer listens on, and that the other nodes
	 * connect to it on.
	 */
    private final int PORT = 6667;

    /**
     * Leaf connection to the Ultrapeer.
     */
    private BlockingConnection LEAF;

    /**
     * Ultrapeer connection.
     */
    private BlockingConnection ULTRAPEER_1;

    /**
     * Ultrapeer 1 UDP connection.
     */
    private DatagramSocket UDP_ACCESS;

    /**
     * Just a TCP connection to use for testing.
     */
    private ServerSocket TCP_ACCESS;

    /**
     * The port for TCP_ACCESS
     */ 
    private final int TCP_ACCESS_PORT = 10776;

    /**
	 * Second Ultrapeer connection
     */
    private BlockingConnection ULTRAPEER_2;

    private LifecycleManager lifecycleManager;

    private ConnectionServices connectionServices;

    private BlockingConnectionFactory blockingConnectionFactory;

    private HeadersFactory headersFactory;

    private QueryRequestFactory queryRequestFactory;

    private CapabilitiesVMFactory capabilitiesVMFactory;

	public ServerSideWhatIsRoutingTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ServerSideWhatIsRoutingTest.class);
    }    
   
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	private void buildConnections() throws Exception {
        ULTRAPEER_1 = blockingConnectionFactory.createConnection("localhost", PORT);
        UDP_ACCESS = new DatagramSocket();
        TCP_ACCESS = new ServerSocket(TCP_ACCESS_PORT);
        ULTRAPEER_2 = blockingConnectionFactory.createConnection("localhost", PORT);
    }

    public void setSettings() {
        FilterSettings.BLACK_LISTED_IP_ADDRESSES.setValue(
            new String[] {"*.*.*.*"});
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.setValue(
            new String[] {"127.*.*.*"});
        NetworkSettings.PORT.setValue(PORT);
		ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
		UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(true);
		UltrapeerSettings.DISABLE_ULTRAPEER_MODE.setValue(false);
		UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(true);
		UltrapeerSettings.MAX_LEAVES.setValue(4);
		ConnectionSettings.NUM_CONNECTIONS.setValue(3);
		ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
		ConnectionSettings.WATCHDOG_ACTIVE.setValue(false);
    }

    @Override
	public void setUp() throws Exception {
        setSettings();
        assertEquals("unexpected port", PORT, NetworkSettings.PORT.getValue());

        Injector injector = LimeTestUtils.createInjector(Stage.PRODUCTION);
        lifecycleManager = injector.getInstance(LifecycleManager.class);
        connectionServices = injector.getInstance(ConnectionServices.class);
        blockingConnectionFactory = injector.getInstance(BlockingConnectionFactory.class);
        headersFactory = injector.getInstance(HeadersFactory.class);
        queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
        capabilitiesVMFactory = injector.getInstance(CapabilitiesVMFactory.class);
        FileManager fileManager = injector.getInstance(FileManager.class);
        
        lifecycleManager.start();
        connectionServices.connect();	
        
        // get the resource file for com/limegroup/gnutella
        File berkeley = TestUtils.getResourceFile("com/limegroup/gnutella/berkeley.txt");
        File susheel = TestUtils.getResourceFile("com/limegroup/gnutella/susheel.txt");
        assertNotNull(fileManager.getGnutellaFileList().add(berkeley).get(1, TimeUnit.SECONDS));
        assertNotNull(fileManager.getGnutellaFileList().add(susheel).get(1, TimeUnit.SECONDS));
        
		connect();
        assertEquals("unexpected port", PORT, NetworkSettings.PORT.getValue());
	}

    @Override
    protected void tearDown() throws Exception {
        connectionServices.disconnect();
        lifecycleManager.shutdown();
        if ((LEAF != null) && LEAF.isOpen())
            LEAF.close();
        ULTRAPEER_1.close();
        ULTRAPEER_2.close();
        UDP_ACCESS.close();
        TCP_ACCESS.close();
    }
    
	private static void sleep() {
		try {Thread.sleep(300);}catch(InterruptedException e) {}
	}

	/**
	 * Drains all messages 
	 */
 	private void drainAll() throws Exception {
 		if(ULTRAPEER_1.isOpen()) {
 			BlockingConnectionUtils.drain(ULTRAPEER_1);
 		}
 		if(ULTRAPEER_2.isOpen()) {
 			BlockingConnectionUtils.drain(ULTRAPEER_2);
 		}
 		if((LEAF != null) && LEAF.isOpen()) {
 			BlockingConnectionUtils.drain(LEAF);
 		}
 	}

	/**
	 * Connects all of the nodes to the central test Ultrapeer.
	 */
    private void connect() throws Exception {
		buildConnections();
        //1. first Ultrapeer connection 
        ULTRAPEER_2.initialize(headersFactory.createUltrapeerHeaders("localhost"), new EmptyResponder(), 1000);
        //2. second Ultrapeer connection
        ULTRAPEER_1.initialize(headersFactory.createUltrapeerHeaders("localhost"), new EmptyResponder(), 1000);
        
        // for Ultrapeer 1
        QueryRouteTable qrt = new QueryRouteTable();
        qrt.add("leehsus");
        qrt.add("berkeley");
        for (Iterator iter=qrt.encode(null).iterator(); iter.hasNext(); ) {
            ULTRAPEER_1.send((RouteTableMessage)iter.next());
			ULTRAPEER_1.flush();
        }

		assertTrue("ULTRAPEER_2 should be connected", ULTRAPEER_2.isOpen());
		assertTrue("ULTRAPEER_1 should be connected", ULTRAPEER_1.isOpen());

	    LEAF = blockingConnectionFactory.createConnection("localhost", PORT);
        //3. routed leaf, with route table for "test"
        LEAF.initialize(headersFactory.createLeafHeaders("localhost"), new EmptyResponder(), 1000);
        qrt = new QueryRouteTable();
        qrt.add("berkeley");
        qrt.add("susheel");
        qrt.addIndivisible(UrnHelper.UNIQUE_SHA1.toString());
        for (Iterator iter=qrt.encode(null).iterator(); iter.hasNext(); ) {
            LEAF.send((RouteTableMessage)iter.next());
			LEAF.flush();
        }
		assertTrue("LEAF should be connected", LEAF.isOpen());

		// make sure we get rid of any initial ping pong traffic exchanges
		sleep();
		drainAll();
		//sleep();
		drainAll();
		sleep();
    }

    public void testDoesNotRouteToLeafWhatIsNewQuery() throws Exception {
        drainAll();

        // send the query
        QueryRequest whatIsNewQuery = 
            queryRequestFactory.createQueryRequest(GUID.makeGuid(), (byte)2,
                QueryRequest.WHAT_IS_NEW_QUERY_STRING, "", null, null, false, Network.UNKNOWN, false,
                FeatureSearchData.WHAT_IS_NEW);
        ULTRAPEER_1.send(whatIsNewQuery);
        ULTRAPEER_1.flush();

        // give time to process
        Thread.sleep(1000);

        // the Leaf should NOT get this query
        QueryRequest rQuery = BlockingConnectionUtils.getFirstInstanceOfMessageType(LEAF,
                                                            QueryRequest.class);
        assertNull(rQuery);
    }

    public void testDoesRouteToLeafWhatIsNewQuery() throws Exception {
        drainAll();

        // send the CapabilitiesVM
        LEAF.send(capabilitiesVMFactory.getCapabilitiesVM());
        LEAF.flush();

        // send the query
        QueryRequest whatIsNewQuery = 
            queryRequestFactory.createQueryRequest(GUID.makeGuid(), (byte)2,
                QueryRequest.WHAT_IS_NEW_QUERY_STRING, "", null, null, false, Network.UNKNOWN, false,
                FeatureSearchData.WHAT_IS_NEW);
        ULTRAPEER_1.send(whatIsNewQuery);
        ULTRAPEER_1.flush();

        // give time to process
        Thread.sleep(1000);

        // the Leaf should get this query
        QueryRequest rQuery = BlockingConnectionUtils.getFirstInstanceOfMessageType(LEAF,
                                                            QueryRequest.class);
        assertNotNull(rQuery);
        assertEquals(new GUID(rQuery.getGUID()), 
                     new GUID(whatIsNewQuery.getGUID()));

        // send the LAST HOP query
        whatIsNewQuery = 
            queryRequestFactory.createQueryRequest(GUID.makeGuid(),
                    (byte)1, QueryRequest.WHAT_IS_NEW_QUERY_STRING, "", null, null, false,
                    Network.UNKNOWN, false, FeatureSearchData.WHAT_IS_NEW);
        ULTRAPEER_2.send(whatIsNewQuery);
        ULTRAPEER_2.flush();

        // give time to process
        Thread.sleep(1000);

        // the Leaf should get this query
        rQuery =  BlockingConnectionUtils.getFirstInstanceOfMessageType(LEAF,
                                                            QueryRequest.class);
        assertNotNull(rQuery);
        assertEquals(new GUID(rQuery.getGUID()), 
                     new GUID(whatIsNewQuery.getGUID()));


    }


    public void testDoesNotRouteToUltrapeerWhatIsNewQuery() throws Exception {
        drainAll();

        // send the query
        QueryRequest whatIsNewQuery = 
            queryRequestFactory.createQueryRequest(GUID.makeGuid(), (byte)2,
                QueryRequest.WHAT_IS_NEW_QUERY_STRING, "", null, null, false, Network.UNKNOWN, false,
                FeatureSearchData.WHAT_IS_NEW);
        ULTRAPEER_2.send(whatIsNewQuery);
        ULTRAPEER_2.flush();

        // give time to process
        Thread.sleep(3000);

        // the UP should NOT get this query
        QueryRequest rQuery = 
           BlockingConnectionUtils.getFirstInstanceOfMessageType(ULTRAPEER_1,
                                                         QueryRequest.class);
        assertNull(rQuery);
    }

    public void testDoesRouteToUltrapeerWhatIsNewQuery() throws Exception {
        drainAll();

        // send the CapabilitiesVM
        ULTRAPEER_1.send(capabilitiesVMFactory.getCapabilitiesVM());
        ULTRAPEER_1.flush();

        // send the query
        QueryRequest whatIsNewQuery = 
            queryRequestFactory.createQueryRequest(GUID.makeGuid(), (byte)2,
                QueryRequest.WHAT_IS_NEW_QUERY_STRING, "", null, null, false, Network.UNKNOWN, false,
                FeatureSearchData.WHAT_IS_NEW);
        ULTRAPEER_2.send(whatIsNewQuery);
        ULTRAPEER_2.flush();

        // give time to process
        Thread.sleep(1000);

        // the UP should get this query
        QueryRequest rQuery = 
             BlockingConnectionUtils.getFirstInstanceOfMessageType(ULTRAPEER_1,
                                                         QueryRequest.class);
        assertNotNull(rQuery);
        assertEquals(new GUID(rQuery.getGUID()), 
                     new GUID(whatIsNewQuery.getGUID()));
    }

    // Ultrapeer 1 should get the query, Ultrapeer 2 should not (because UP 1
    // sent the capabilites VM)
    public void testLeafQueryRoutesCorrectly() throws Exception {
        drainAll();

        // send the CapabilitiesVM
        ULTRAPEER_1.send(capabilitiesVMFactory.getCapabilitiesVM());
        ULTRAPEER_1.flush();
        // send the query
        QueryRequest whatIsNewQuery = 
            queryRequestFactory.createQueryRequest(GUID.makeGuid(), (byte)2,
                QueryRequest.WHAT_IS_NEW_QUERY_STRING, "", null, null, false, Network.UNKNOWN, false,
                FeatureSearchData.WHAT_IS_NEW);
        LEAF.send(whatIsNewQuery);
        LEAF.flush();

        // give time to process
        Thread.sleep(5000);

        // UP 1 should get this query
        QueryRequest rQuery = 
           BlockingConnectionUtils.getFirstInstanceOfMessageType(ULTRAPEER_1,
                                                         QueryRequest.class);
        assertNotNull(rQuery);
        assertEquals(new GUID(rQuery.getGUID()), 
                     new GUID(whatIsNewQuery.getGUID()));
        // UP 2 should NOT get this query
        rQuery = BlockingConnectionUtils.getFirstInstanceOfMessageType(ULTRAPEER_2,
                                                              QueryRequest.class);
        assertNull(rQuery);
    }


    public void testUnsupportedQueryForwardedCorrectly() throws Exception {
        drainAll();

        // send the CapabilitiesVM
        ULTRAPEER_1.send(capabilitiesVMFactory.getCapabilitiesVM());
        ULTRAPEER_1.flush();
        
        // send the query
        QueryRequest unknownFeatureQuery = 
            queryRequestFactory.createQueryRequest(GUID.makeGuid(), (byte)3,
                QueryRequest.WHAT_IS_NEW_QUERY_STRING, "", null, null, false, Network.UNKNOWN, false,
                FeatureSearchData.FEATURE_SEARCH_MAX_SELECTOR+1);
        ULTRAPEER_2.send(unknownFeatureQuery);
        ULTRAPEER_2.flush();

        // give time to process
        Thread.sleep(4000);

        // the Leaf should NOT get this query
        QueryRequest rQuery =  BlockingConnectionUtils.getFirstInstanceOfMessageType(LEAF,
                                                            QueryRequest.class);
        assertNull(rQuery);

        // Ultrapeer 1 should get it though
        rQuery = 
           BlockingConnectionUtils.getFirstInstanceOfMessageType(ULTRAPEER_1,
                                                         QueryRequest.class);
        assertNotNull(rQuery);
        assertEquals(new GUID(rQuery.getGUID()), 
                     new GUID(unknownFeatureQuery.getGUID()));
    }


    public void testLastHopUnsupportedQueryForwardedCorrectly() 
        throws Exception {
        drainAll();
        // send the CapabilitiesVM
        ULTRAPEER_1.send(capabilitiesVMFactory.getCapabilitiesVM());
        ULTRAPEER_1.flush();
        
        // send the query
        QueryRequest unknownFeatureQuery = 
            queryRequestFactory.createQueryRequest(GUID.makeGuid(), (byte)2,
                QueryRequest.WHAT_IS_NEW_QUERY_STRING, "", null, null, false, Network.UNKNOWN, false,
                FeatureSearchData.FEATURE_SEARCH_MAX_SELECTOR+1);
        ULTRAPEER_2.send(unknownFeatureQuery);
        ULTRAPEER_2.flush();

        // give time to process
        Thread.sleep(4000);

        // the Leaf should NOT get this query
        QueryRequest rQuery =  BlockingConnectionUtils.getFirstInstanceOfMessageType(LEAF,
                                                            QueryRequest.class);
        assertNull(rQuery);

        // Ultrapeer 1 should get it though
        rQuery = 
           BlockingConnectionUtils.getFirstInstanceOfMessageType(ULTRAPEER_1,
                                                         QueryRequest.class);
        assertNotNull(rQuery);
        assertEquals(new GUID(rQuery.getGUID()), 
                     new GUID(unknownFeatureQuery.getGUID()));
    }

    // ------------------------------------------------------

}
