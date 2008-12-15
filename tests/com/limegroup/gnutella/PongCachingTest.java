package com.limegroup.gnutella;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.Test;

import org.limewire.core.settings.ApplicationSettings;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.FilterSettings;
import org.limewire.core.settings.NetworkSettings;
import org.limewire.core.settings.PingPongSettings;
import org.limewire.core.settings.UltrapeerSettings;
import org.limewire.io.GUID;

import com.google.inject.Injector;
import com.google.inject.Stage;
import com.limegroup.gnutella.connection.BlockingConnection;
import com.limegroup.gnutella.connection.BlockingConnectionFactory;
import com.limegroup.gnutella.handshaking.HeadersFactory;
import com.limegroup.gnutella.helpers.UrnHelper;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingReplyFactory;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.PingRequestFactory;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.routing.RouteTableMessage;
import com.limegroup.gnutella.util.EmptyResponder;
import com.limegroup.gnutella.util.LimeTestCase;

/**
 * This test makes sure that pong caching is working correctly between
 * Ultrapeers.
 *
 *  ULTRAPEER_1  ----  CENTRAL TEST ULTRAPEER  ----  ULTRAPEER_2
 *                              |
 *                              |
 *                              |
 *                             LEAF
 */
@SuppressWarnings("unchecked")
public final class PongCachingTest extends LimeTestCase {

	/**
	 * The port that the central Ultrapeer listens on, and that the other nodes
	 * connect to it on.
	 */
    private static int SERVER_PORT = 6667;

	/**
	 * The timeout value for sockets -- how much time we wait to accept 
	 * individual messages before giving up.
	 */
    private static final int TIMEOUT = 1800;


    /**
     * Leaf connection to the Ultrapeer.
     */
    private BlockingConnection LEAF;

    /**
     * Ultrapeer connection.
     */
    private BlockingConnection ULTRAPEER_1;

    /**
	 * Second Ultrapeer connection
     */
    private BlockingConnection ULTRAPEER_2;

    /**
     * Third Ultrapeer connection
     */
    private BlockingConnection ULTRAPEER_3;
    
    /**
     * Fourth Ultrapeer connection
     */
    private BlockingConnection ULTRAPEER_4;

    private BlockingConnectionFactory connectionFactory;

    private ConnectionServices connectionServices;

    private HeadersFactory headersFactory;

    private PingReplyFactory pingReplyFactory;

    private PongCacher pongCacher;

    private PingRequestFactory pingRequestFactory;

    public PongCachingTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(PongCachingTest.class);
    }    
   
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	private void buildConnections() {
	    LEAF = connectionFactory.createConnection("localhost", SERVER_PORT);
        ULTRAPEER_1 = connectionFactory.createConnection("localhost", SERVER_PORT);
        ULTRAPEER_2 = connectionFactory.createConnection("localhost", SERVER_PORT);
        ULTRAPEER_3 = connectionFactory.createConnection("localhost", SERVER_PORT);
        ULTRAPEER_4 = connectionFactory.createConnection("localhost", SERVER_PORT);
    }

	@Override
    public void setUp() throws Exception {
        //Setup LimeWire backend.  For testing other vendors, you can skip all
        //this and manually configure a client to listen on port 6667, with
        //incoming slots and no connections.
        //To keep LimeWire from connecting to the outside network, we filter out
        //all addresses but localhost and 18.239.0.*.  The latter is used in
        //pongs for testing.  TODO: it would be nice to have a way to prevent
        //BootstrapServerManager from adding defaults and connecting.
        FilterSettings.BLACK_LISTED_IP_ADDRESSES.setValue(
            new String[] {"*.*.*.*"});
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.setValue(
            new String[] {"127.*.*.*", "18.239.0.*"});
        // TODO hack: increment static field server port so each test case has its own port
        SERVER_PORT++;
        NetworkSettings.PORT.setValue(SERVER_PORT);
		ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
		UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(true);
		UltrapeerSettings.DISABLE_ULTRAPEER_MODE.setValue(false);
		UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(true);
		UltrapeerSettings.MAX_LEAVES.setValue(4);
		ConnectionSettings.NUM_CONNECTIONS.setValue(3);
		ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
		ConnectionSettings.WATCHDOG_ACTIVE.setValue(false);
		ConnectionSettings.SEND_QRP.setValue(false);
		
        UltrapeerSettings.NEED_MIN_CONNECT_TIME.setValue(false);
        
        assertEquals("unexpected port", SERVER_PORT, 
					 NetworkSettings.PORT.getValue());

        Injector injector = LimeTestUtils.createInjector(Stage.PRODUCTION);
        connectionFactory = injector.getInstance(BlockingConnectionFactory.class);
        connectionServices = injector.getInstance(ConnectionServices.class);
        headersFactory = injector.getInstance(HeadersFactory.class);
        pingReplyFactory = injector.getInstance(PingReplyFactory.class);
        pongCacher = injector.getInstance(PongCacher.class);
        pingRequestFactory = injector.getInstance(PingRequestFactory.class);
        LifecycleManager lifecycleManager = injector.getInstance(LifecycleManager.class);
        
        lifecycleManager.start();
		connectionServices.connect();
        
		connect();
		assertEquals("unexpected port", SERVER_PORT, 
					 NetworkSettings.PORT.getValue());
	}
	
	@Override
    public void tearDown() throws Exception {
        drainAll();
		sleep();
		LEAF.close();
		ULTRAPEER_1.close();
		ULTRAPEER_2.close();
        ULTRAPEER_3.close();
        ULTRAPEER_4.close();        
        ConnectionSettings.SEND_QRP.setValue(true);
		connectionServices.disconnect();
		sleep();
	}

	private void sleep() {
		try {Thread.sleep(300);}catch(InterruptedException e) {}
	}

	/**
	 * Drains all messages 
	 */
 	private void drainAll() throws Exception {
 		if(ULTRAPEER_1.isOpen()) {
 			BlockingConnectionUtils.drain(ULTRAPEER_1, TIMEOUT);
 		}
 		if(ULTRAPEER_2.isOpen()) {
 			BlockingConnectionUtils.drain(ULTRAPEER_2, TIMEOUT);
 		}
        if(ULTRAPEER_3.isOpen()) {
            BlockingConnectionUtils.drain(ULTRAPEER_3, TIMEOUT);
        }
        if(ULTRAPEER_4.isOpen()) {
            BlockingConnectionUtils.drain(ULTRAPEER_4, TIMEOUT);
        }
 		if(LEAF.isOpen()) {
 			BlockingConnectionUtils.drain(LEAF, TIMEOUT);
 		}
 	}

	/**
	 * Connects all of the nodes to the central test Ultrapeer.
	 */
    private void connect() throws Exception {
		buildConnections();
        //1. first Ultrapeer connection 
        ULTRAPEER_2.initialize(headersFactory.createUltrapeerHeaders("localhost"), new EmptyResponder(), 1000);
        assertTrue("should be open", ULTRAPEER_2.isOpen());
        assertTrue("should be up", ULTRAPEER_2.getConnectionCapabilities().isSupernodeSupernodeConnection());
        ULTRAPEER_3.initialize(headersFactory.createUltrapeerHeaders("localhost"), new EmptyResponder(), 1000);
        assertTrue("should be open", ULTRAPEER_3.isOpen());
        assertTrue("should be up", ULTRAPEER_3.getConnectionCapabilities().isSupernodeSupernodeConnection());

        ULTRAPEER_4.initialize(headersFactory.createUltrapeerHeaders("localhost"), new EmptyResponder(), 1000);
        assertTrue("should be open", ULTRAPEER_4.isOpen());
        assertTrue("should be up", ULTRAPEER_4.getConnectionCapabilities().isSupernodeSupernodeConnection());

        //2. second Ultrapeer connection
        ULTRAPEER_1.initialize(headersFactory.createUltrapeerHeaders("localhost"), new EmptyResponder(), 1000);
        assertTrue("should be open", ULTRAPEER_1.isOpen());
        assertTrue("should be up", ULTRAPEER_1.getConnectionCapabilities().isSupernodeSupernodeConnection());        
        //3. routed leaf, with route table for "test"
        LEAF.initialize(headersFactory.createLeafHeaders("localhost"), new EmptyResponder(), 1000);
        assertTrue("should be open", LEAF.isOpen());
        assertTrue("should be up", LEAF.getConnectionCapabilities().isClientSupernodeConnection());        
        QueryRouteTable qrt = new QueryRouteTable();
        qrt.add("test");
        qrt.add("susheel");
        qrt.addIndivisible(UrnHelper.UNIQUE_SHA1.toString());
        for (Iterator iter=qrt.encode(null).iterator(); iter.hasNext(); ) {
            LEAF.send((RouteTableMessage)iter.next());
			LEAF.flush();
        }

        // for Ultrapeer 1
        qrt = new QueryRouteTable();
        qrt.add("leehsus");
        qrt.add("awesome");
        for (Iterator iter=qrt.encode(null).iterator(); iter.hasNext(); ) {
            ULTRAPEER_1.send((RouteTableMessage)iter.next());
			ULTRAPEER_1.flush();
        }

		// make sure we get rid of any initial ping pong traffic exchanges
		sleep();
		drainAll();
    }

    /**
     * Tests to make sure that pongs are received properly via
     * pong caching.
     */
    public void testPongsReceivedFromPing() throws Exception {
        PingPongSettings.PINGS_ACTIVE.setValue(false);


        byte[] ip = { (byte)1, (byte)2, (byte)3, (byte)4 };

        for(int i=0; i<PongCacher.NUM_HOPS+4; i++) {
            PingReply curPong = 
                pingReplyFactory.create(new GUID().bytes(), (byte)3, 13232, ip, 0, 0, 
                    true, -1, false);
            for(int j=0; j<i; j++) {
                if(j < PongCacher.NUM_HOPS) {
                    curPong.hop();
                }
            }
            pongCacher.addPong(curPong);            
        }
        
        List pongs = pongCacher
            .getBestPongs(ApplicationSettings.LANGUAGE.getValue());
        assertEquals( PongCacher.NUM_HOPS, pongs.size() );

        Message m = pingRequestFactory.createPingRequest((byte)7);
        ULTRAPEER_1.send(m);
        ULTRAPEER_1.flush();        
        
        Message received;   
        for(int i=0; i<PongCacher.NUM_HOPS; i++) {
            received = BlockingConnectionUtils.getFirstMessageOfType(ULTRAPEER_1, PingReply.class, 10000);
            assertNotNull("should have gotten pong. hop: " + i, received);
        }
        PingPongSettings.PINGS_ACTIVE.setValue(true);
    }


    /**
     * Tests to make sure that pongs are received properly via
     * pong caching when the locale is specified in the ping
     */
    public void testPongsReceivedFromPingWithLocale() throws Exception {
        PingPongSettings.PINGS_ACTIVE.setValue(false);
        byte[] ip = { (byte)1, (byte)2, (byte)3, (byte)3 };
              
        //add english locale pongs
        for(int i=0; i<PongCacher.NUM_HOPS+4; i++) {
            PingReply curPong = 
                pingReplyFactory.create(new GUID().bytes(), (byte)3, 13232, ip, 0, 0, 
                    true, -1, false, "en", 1);
            for(int j=0; j<i; j++) {
                if(j < PongCacher.NUM_HOPS) {
                    curPong.hop();
                }
            }
            pongCacher.addPong(curPong);            
        }
        
        byte[] ip2 = { (byte)1, (byte)3, (byte)3, (byte)3 };
        //add ja locale pongs
        for(int i=0; i<PongCacher.NUM_HOPS+4; i++) {
            PingReply curPong = 
                pingReplyFactory.create(new GUID().bytes(), (byte)3, 13232, ip2, 
                                 0, 0, true, -1, false, "ja", 1);
            for(int j=0; j<i; j++) {
                if(j < PongCacher.NUM_HOPS) {
                    curPong.hop();
                }
            }
            pongCacher.addPong(curPong);            
        }

        //check that all the pongs are in the PongCacher
        List pongs = pongCacher.getBestPongs("ja");
        assertEquals( PongCacher.NUM_HOPS, pongs.size() );

        pongs = pongCacher.getBestPongs("en");
        assertEquals( PongCacher.NUM_HOPS, pongs.size() );

        //create a ja locale PingRequest
        ApplicationSettings.LANGUAGE.setValue("ja");
        Message m = pingRequestFactory.createPingRequest((byte)7);
        assertEquals("locale of ping should be ja",
                     "ja", ((PingRequest)m).getLocale());

        //send a ja ping using ULTRAPEER_3
        ULTRAPEER_3.send(m);
        ULTRAPEER_3.flush();        

        Thread.sleep(100);

        ApplicationSettings.LANGUAGE.setValue("en");        

        //check for ja pongs
        Message received;   
        for(int i=0; i< PongCacher.NUM_HOPS; i++) {
            received = BlockingConnectionUtils.getFirstMessageOfType(ULTRAPEER_3, 
                                             PingReply.class, 
                                             5000);

            assertNotNull("should have gotten pong. hop: " + i, received);
            PingReply pr = (PingReply)received;
            assertEquals("should be a ja locale pong ",
                         "ja", pr.getClientLocale());
        }


        byte[] ip3 = { (byte)3, (byte)3, (byte)3, (byte)3 };
        //add sv locale pongs
        for(int i=0; i< 2; i++) {
            PingReply curPong = 
                pingReplyFactory.create(new GUID().bytes(), (byte)3, 13232, ip3, 
                                 0, 0, true, -1, false, "sv", 1);
            for(int j=0; j<i; j++) {
                if(j < PongCacher.NUM_HOPS) {
                    curPong.hop();
                }
            }
            pongCacher.addPong(curPong);            
        }
        pongs = pongCacher.getBestPongs("sv");
        assertEquals( PongCacher.NUM_HOPS, pongs.size() );        

        //create a sv locale PingRequest
        ApplicationSettings.LANGUAGE.setValue("sv"); 
        Message m2 = pingRequestFactory.createPingRequest((byte)7);
        assertEquals("locale of ping should be sv",
                     "sv", ((PingRequest)m2).getLocale());

        ULTRAPEER_4.send(m2);
        ULTRAPEER_4.flush();

        //check ofr sv pongs
        List returnedPongs = new ArrayList();
        for(int i = 0; i < PongCacher.NUM_HOPS; i++) {
            received = BlockingConnectionUtils.getFirstMessageOfType(ULTRAPEER_4, 
                                             PingReply.class, 
                                             5000);
            assertNotNull("should have gotten pong. hop: " + i, received);
            PingReply pr = (PingReply)received;
            returnedPongs.add(pr);
        }

        //check that there are two "sv" pongs and the rest are 
        //"en" - the default locale
        int numSVPongs = 0;
        Iterator iter = returnedPongs.iterator();
        while(iter.hasNext()) {
            PingReply pr = (PingReply)iter.next();
            if(pr.getClientLocale().equals("sv"))
                numSVPongs++;
            else
                assertEquals("the locale of pong should be en : ",
                             "en", pr.getClientLocale());
        }
        
        assertTrue("should be 2 || 3 pongs with sv, but was: " + numSVPongs, 
                   numSVPongs == 2 || numSVPongs == 3);
        
        PingPongSettings.PINGS_ACTIVE.setValue(true);
    }
}






