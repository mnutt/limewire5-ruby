/*
 * Tests the server-side code for the udp crawler ping
 */
package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;
import java.util.StringTokenizer;
import java.util.zip.GZIPInputStream;

import junit.framework.Test;

import org.limewire.collection.FixedSizeExpiringSet;
import org.limewire.core.settings.ApplicationSettings;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.FilterSettings;
import org.limewire.core.settings.NetworkSettings;
import org.limewire.core.settings.UltrapeerSettings;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.io.NetworkUtils;
import org.limewire.util.ByteUtils;
import org.limewire.util.PrivilegedAccessor;
import org.limewire.util.StringUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.limegroup.gnutella.BlockingConnectionUtils;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.LifecycleManager;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.ResponseFactory;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.connection.CountingConnection;
import com.limegroup.gnutella.connection.CountingConnectionFactory;
import com.limegroup.gnutella.connection.RoutedConnection;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.DHTManagerStub;
import com.limegroup.gnutella.handshaking.HeadersFactory;
import com.limegroup.gnutella.helpers.UrnHelper;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryReplyFactory;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.messages.vendor.VendorMessageFactory.VendorMessageParser;
import com.limegroup.gnutella.util.EmptyResponder;
import com.limegroup.gnutella.util.LimeWireUtils;

public class UDPCrawlerMessagesTest extends LimeTestCase {

	/**
	 * The port that the central Ultrapeer listens on, and that the other nodes
	 * connect to it on.
	 */
    private static int PORT = 6669;
    
    private InetAddress _udpAddress;

    /**
	 * The different connections to the ultrapeer.
	 */
    private CountingConnection LEAF_1,LEAF_2, LEAF_3, UP1, UP2, UP3;
    
    /**
     * the different kinds of queries
     */
    private final UDPCrawlerPing msgAll = new UDPCrawlerPing(new GUID(GUID.makeGuid()));
    
    /**
     * Ultrapeer 1 UDP connection.
     */
    private DatagramSocket UDP_ACCESS;

    private NetworkManager networkManager;

    private LifecycleManager lifecycleManager;

    private ConnectionServices connectionServices;

    private MessageRouter messageRouter;

    private VendorMessageFactory vendorMessageFactory;

    private HeadersFactory headersFactory;

    private UDPCrawlerPongFactory crawlerPongFactory;

    private ConnectionManager connectionManager;

    private ResponseFactory responseFactory;

    private QueryReplyFactory queryReplyFactory;

    private UDPService udpService;

    private CountingConnectionFactory countingConnectionFactory;
	
	
	public UDPCrawlerMessagesTest(String name) {
		super(name);
	}
	
	public static Test suite() {
        return buildTestSuite(UDPCrawlerMessagesTest.class);
    }    
   
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	/**
	 * builds the connections between the entities in the test
	 * @throws Exception something bad happened (?)
	 */
	private void buildConnections() throws Exception {
		LEAF_1 = countingConnectionFactory.createConnection("localhost", PORT);
	    LEAF_2 = countingConnectionFactory.createConnection("localhost", PORT);
	    LEAF_3 = countingConnectionFactory.createConnection("localhost", PORT);
	    UP1 =  countingConnectionFactory.createConnection("localhost", PORT);
	    UP2 = countingConnectionFactory.createConnection("localhost", PORT);
	    UP3 =  countingConnectionFactory.createConnection("localhost", PORT);
	    
	    UDP_ACCESS = new DatagramSocket();
	}
	
	public void setSettings() {
		
		String localIP = null;
        try {
            localIP = InetAddress.getLocalHost().getHostAddress();
        }
        catch (Exception ignored) {}
        FilterSettings.BLACK_LISTED_IP_ADDRESSES.set(
                new String[] {"*.*.*.*"});
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.set(
                new String[] {localIP,"127.*.*.*"});
        NetworkSettings.PORT.setValue(PORT);
        
        UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(true);
		UltrapeerSettings.DISABLE_ULTRAPEER_MODE.setValue(false);
		UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(true);
		UltrapeerSettings.MAX_LEAVES.setValue(30);
		ConnectionSettings.NUM_CONNECTIONS.setValue(30);
		ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
		ConnectionSettings.WATCHDOG_ACTIVE.setValue(false);
	}

	@Override
	public void setUp() throws Exception {
        PORT++;
        setSettings();
        assertEquals("unexpected port", PORT, NetworkSettings.PORT.getValue());
        
        Injector injector = LimeTestUtils.createInjector(Stage.PRODUCTION, new AbstractModule() {
            @Override
            protected void configure() {
                bind(DHTManager.class).toInstance(new DHTManagerStub());
            }
        });
        networkManager = injector.getInstance(NetworkManager.class);
        lifecycleManager = injector.getInstance(LifecycleManager.class);
        connectionServices = injector.getInstance(ConnectionServices.class);
        messageRouter = injector.getInstance(MessageRouter.class);
        vendorMessageFactory = injector.getInstance(VendorMessageFactory.class);
        headersFactory = injector.getInstance(HeadersFactory.class);
        crawlerPongFactory = injector.getInstance(UDPCrawlerPongFactory.class);
        connectionManager = injector.getInstance(ConnectionManager.class);
        responseFactory = injector.getInstance(ResponseFactory.class);
        queryReplyFactory = injector.getInstance(QueryReplyFactory.class);
        udpService = injector.getInstance(UDPService.class);
        countingConnectionFactory = injector.getInstance(CountingConnectionFactory.class);
        
        networkManager.setListeningPort(PORT);
		lifecycleManager.start();
        connectionServices.connect();	
		connect();
        assertEquals("unexpected port", PORT, 
					 NetworkSettings.PORT.getValue());
        
        Object handler = messageRouter.getUDPMessageHandler(UDPCrawlerPing.class);
        PrivilegedAccessor.setValue(handler,
				"_UDPListRequestors",
				new FixedSizeExpiringSet(200,200));
        UDP_ACCESS.connect(InetAddress.getLocalHost(),PORT);
        vendorMessageFactory.setParser(VendorMessage.F_CRAWLER_PONG, VendorMessage.F_LIME_VENDOR_ID, 
                new UDPCrawlerPongParserStub());
	}
	
	@Override
	public void tearDown() throws Exception {
        connectionServices.disconnect();
        lifecycleManager.shutdown();
		LEAF_1.close();
		LEAF_2.close();
		LEAF_3.close();
		UP1.close();
		UP2.close();
		UP3.close();
		UDP_ACCESS.close();
	}
	
	private static void sleep() {
		try {Thread.sleep(300);}catch(InterruptedException e) {}
	}
	
	/**
	 * Connects all of the nodes to the central test Ultrapeer.
	 */
    private void connect() throws Exception {
		buildConnections();
        //ultrapeers 
        UP1.initialize(headersFactory.createUltrapeerHeaders("localhost"), new EmptyResponder(), 1000);
        UP2.initialize(headersFactory.createUltrapeerHeaders("localhost"), new EmptyResponder(), 1000);
        UP3.initialize(headersFactory.createUltrapeerHeaders("localhost"), new EmptyResponder(), 1000);
        
        //leafs
        LEAF_1.initialize(headersFactory.createLeafHeaders("localhost"),new EmptyResponder(), 1000);
        LEAF_2.initialize(headersFactory.createLeafHeaders("localhost"), new EmptyResponder(), 1000);
        LEAF_3.initialize(headersFactory.createLeafHeaders("localhost"), new EmptyResponder(), 1000);
                

		assertTrue("ULTRAPEER_2 should be connected", UP2.isOpen());
		assertTrue("ULTRAPEER_1 should be connected", UP1.isOpen());
		assertTrue("ULTRAPEER_3 should be connected", UP3.isOpen());
		assertTrue("LEAF should be connected", LEAF_1.isOpen());
		assertTrue("LEAF should be connected", LEAF_2.isOpen());
		assertTrue("LEAF should be connected", LEAF_3.isOpen());
		

		// make sure we get rid of any initial ping pong traffic exchanges
		sleep();
		drainAll();
		sleep();
    }
    
    /**
	 * Drains all messages 
	 */
 	private void drainAll() throws Exception {
 		if(UP1.isOpen()) {
 			BlockingConnectionUtils.drain(UP1);
 		}
 		if(UP2.isOpen()) {
 			BlockingConnectionUtils.drain(UP2);
 		}
 		if(UP3.isOpen()){
 			BlockingConnectionUtils.drain(UP3);
 		}
 		if(LEAF_1.isOpen()) {
 			BlockingConnectionUtils.drain(LEAF_1);
 		}
 		if(LEAF_2.isOpen()) {
 			BlockingConnectionUtils.drain(LEAF_2);
 		}
 		if(LEAF_3.isOpen()) {
 			BlockingConnectionUtils.drain(LEAF_3);
 		}
 		
 		
 	}
	
 	/**
 	 * sends out a message requesting all leafs + ups 
 	 * @throws Exception
 	 */
 	public void testMsgAll() throws Exception {
 		UDPCrawlerPong reply = crawlerPongFactory.createUDPCrawlerPong(msgAll);
 		//test whether we got proper # of results
        byte[] payload = reply.getPayload();
        assertEquals(3,payload[0]);
        assertEquals(3,payload[1]);
 		
 		assertEquals(LEAF_1.getInetAddress(),LEAF_2.getInetAddress());
 		assertEquals(LEAF_1.getInetAddress(),LEAF_3.getInetAddress());
 		
 		assertEquals(UP1.getInetAddress(),UP2.getInetAddress());
 		assertEquals(UP1.getInetAddress(),UP3.getInetAddress());
 		
        int index = 3; //the index within the result block.
        
        byte [] current = new byte[6];
        
        System.arraycopy(payload,index,current,0,6);
        IpPort e = NetworkUtils.getIpPort(current, java.nio.ByteOrder.LITTLE_ENDIAN);
        assertEquals(UP1.getInetAddress(),e.getInetAddress());
        assertEquals(UP1.getPort(), e.getPort());
        index +=6;
        System.arraycopy(payload,index,current,0,6);
        e = NetworkUtils.getIpPort(current, java.nio.ByteOrder.LITTLE_ENDIAN);
        assertEquals(UP2.getInetAddress(),e.getInetAddress());
        assertEquals(UP2.getPort(), e.getPort());
        index +=6;
        System.arraycopy(payload,index,current,0,6);
        e = NetworkUtils.getIpPort(current, java.nio.ByteOrder.LITTLE_ENDIAN);
        assertEquals(UP3.getInetAddress(),e.getInetAddress());
        assertEquals(UP3.getPort(), e.getPort());
        index +=6;
        System.arraycopy(payload,index,current,0,6);
        e = NetworkUtils.getIpPort(current, java.nio.ByteOrder.LITTLE_ENDIAN);
        assertEquals(LEAF_1.getInetAddress(),e.getInetAddress());
        assertEquals(LEAF_1.getPort(), e.getPort());
        index +=6;
        System.arraycopy(payload,index,current,0,6);
        e = NetworkUtils.getIpPort(current, java.nio.ByteOrder.LITTLE_ENDIAN);
        assertEquals(LEAF_2.getInetAddress(),e.getInetAddress());
        assertEquals(LEAF_2.getPort(), e.getPort());
        index +=6;
        System.arraycopy(payload,index,current,0,6);
        e = NetworkUtils.getIpPort(current, java.nio.ByteOrder.LITTLE_ENDIAN);
        assertEquals(LEAF_3.getInetAddress(),e.getInetAddress());
        assertEquals(LEAF_3.getPort(), e.getPort());
 	}
 	
 	/**
 	 * sends a message requesting 0 leafs and 0 ups.
 	 */
 	public void testMsgNone() throws Exception {
 	    UDPCrawlerPing msgNone = new UDPCrawlerPing(new GUID(GUID.makeGuid()), 0, 0,(byte)0);
 		UDPCrawlerPong reply = crawlerPongFactory.createUDPCrawlerPong(msgNone);
        byte[] payload = reply.getPayload();
 		assertEquals(0,payload[0]);
 		assertEquals(0,payload[1]);
 	}
 	
 	/**
 	 * sends a message requesting leafs only
 	 */
 	public void testMsgLeafs() throws Exception {
       UDPCrawlerPing msgLeafsOnly = new UDPCrawlerPing(new GUID(GUID.makeGuid()), 0, 2,(byte)0);
 		UDPCrawlerPong reply = crawlerPongFactory.createUDPCrawlerPong(msgLeafsOnly);
        byte[] payload = reply.getPayload();
 		assertEquals(2,payload[1]);
 		assertEquals(0,payload[0]);
 	}
 	
 	/**
 	 * sends a message requesting 1 leafs and 2 ups.
 	 */
 	public void testMsgSome() throws Exception {
 	    UDPCrawlerPing msgSome = new UDPCrawlerPing(new GUID(GUID.makeGuid()), 2, 1,(byte)0);
 		UDPCrawlerPong reply = crawlerPongFactory.createUDPCrawlerPong(msgSome);
        byte[] payload = reply.getPayload();
 		
 		assertEquals(1,payload[1]);
 		assertEquals(2,payload[0]);
 		sleep();
 	}
 	
 	/**
 	 * sends a message requesting more UPs and Leafs that the host has.
 	 */
 	public void testMsgMore() throws Exception {
 	    UDPCrawlerPing msgMore = new UDPCrawlerPing(new GUID(GUID.makeGuid()), 20, 30,(byte)0);
 		UDPCrawlerPong reply = crawlerPongFactory.createUDPCrawlerPong(msgMore);
        byte[] payload = reply.getPayload();
 		
 		//we should get the number we have connected.
 		assertEquals(3,payload[1]);
 		assertEquals(3,payload[0]);
 		sleep();
 	}
 	
    
    public void testUDPCrawlerPingMessage() throws Exception {
        try {
            tryMessage();
        } catch (Exception ex) {
            fail("message should not fail");
            throw ex;
        }
        sleep();
    }
    
    /**
     * tests whether requesting too often will give us a reply
     */
    public void testHammering() throws Exception {
        
        //first message should go through
        tryMessage();
        
        //second shouldn't
        try {
            tryMessage();
            fail("ioex expected");
        }catch (IOException iox) {}
        sleep();
        
        //third should
        tryMessage();
        sleep();
    }
 	
 	/**
 	 * tests a udp ping message requesting the connection lifetimes
 	 */
 	public void testConnectionTime() throws Exception{
 		PrivilegedAccessor.setValue(Constants.class,"MINUTE",new Long(1));
        UDPCrawlerPing msgTimes = new UDPCrawlerPing(new GUID(GUID.makeGuid()),3,3,(byte)1);
 		UDPCrawlerPong reply = crawlerPongFactory.createUDPCrawlerPong(msgTimes);
        byte[] payload = reply.getPayload();
        byte format =  (byte) (payload[2] & UDPCrawlerPing.FEATURE_MASK);
 		assertTrue((format & UDPCrawlerPing.CONNECTION_TIME)
                == UDPCrawlerPing.CONNECTION_TIME);
 		assertFalse((format & UDPCrawlerPing.LOCALE_INFO)
                == UDPCrawlerPing.LOCALE_INFO);
 		
 		//see if the result we got had any uptime (it should!)
 		short uptime = ByteUtils.leb2short(payload,9);
 		assertGreaterThan(0,uptime);
 	}
 	
 	/**
 	 * tests a message send from a newer peer that supports more options.
 	 */
 	public void testBadMask() throws Exception {
 		PrivilegedAccessor.setValue(Constants.class,"MINUTE",new Long(1));
        UDPCrawlerPing msgBadMask = new UDPCrawlerPing(new GUID(GUID.makeGuid()),3,3,(byte)0xFF);
 		UDPCrawlerPong reply = crawlerPongFactory.createUDPCrawlerPong(msgBadMask);
        byte[] payload = reply.getPayload();
        byte format =  (byte) (payload[2] & UDPCrawlerPing.FEATURE_MASK);
        assertTrue((format & UDPCrawlerPing.CONNECTION_TIME)
                == UDPCrawlerPing.CONNECTION_TIME);
        assertTrue((format & UDPCrawlerPing.LOCALE_INFO)
                == UDPCrawlerPing.LOCALE_INFO);
        assertTrue((format & UDPCrawlerPing.DHT_STATUS)
                == UDPCrawlerPing.DHT_STATUS);
        
        //see if the result we got had any uptime (it should!)
        short uptime = ByteUtils.leb2short(payload,14);
        assertGreaterThan(0,uptime);
 	}
 	
 	/**
 	 * tests a ping message requesting locale info.
 	 */
 	public void testLocale() throws Exception {
 		PrivilegedAccessor.setValue(Constants.class,"MINUTE",new Long(1));
        UDPCrawlerPing msgLocale = new UDPCrawlerPing(new GUID(GUID.makeGuid()),3,3,(byte)2);
        UDPCrawlerPong reply = crawlerPongFactory.createUDPCrawlerPong(msgLocale);
        byte[] payload = reply.getPayload();
        byte format =  (byte) (payload[2] & UDPCrawlerPing.FEATURE_MASK);
        assertFalse((format & UDPCrawlerPing.CONNECTION_TIME)
                == UDPCrawlerPing.CONNECTION_TIME);
        assertTrue((format & UDPCrawlerPing.LOCALE_INFO)
                == UDPCrawlerPing.LOCALE_INFO);
 		
 		//see if any of the connections have the locale in them - they should
        String lang = StringUtils.getASCIIString(payload, 9, 2);
 		assertEquals(ApplicationSettings.LANGUAGE.get(),
               lang);
 	}
 	
 	/**
 	 * tests a ping message requesting only results that support
 	 * udp crawling themselves.
 	 */
 	public void testNewOnly() throws Exception {
 	    long savedMinute = Constants.MINUTE;
 	    try {
 	        Constants.MINUTE = 1;

 		//make sure some of our connections support crawling.
 		
 		MessagesSupportedVendorMessage msvm = new MessagesSupportedVendorMessage();
 		
 		assertGreaterThan(0,msvm.supportsUDPCrawling());
 		
 		List<RoutedConnection> upCons = connectionManager.getInitializedConnections();
 		
 		RoutedConnection notSupporting = upCons.get(0);
 		RoutedConnection supporting =  upCons.get(1);
 		
 		assertLessThanOrEquals(1,notSupporting.getConnectionCapabilities().remoteHostSupportsUDPCrawling());
 		assertLessThanOrEquals(1,supporting.getConnectionCapabilities().remoteHostSupportsUDPCrawling());
 		
 		supporting.getConnectionCapabilities().setMessagesSupportedVendorMessage(msvm);
 		
 		assertLessThanOrEquals(1,notSupporting.getConnectionCapabilities().remoteHostSupportsUDPCrawling());
 		assertGreaterThanOrEquals(1,supporting.getConnectionCapabilities().remoteHostSupportsUDPCrawling());
 		
 		
 		
 		//so now, only one UP should be in the result.
        UDPCrawlerPing msgNewOnly = new UDPCrawlerPing(new GUID(GUID.makeGuid()),3,3,(byte)0x4);
 		UDPCrawlerPong pong = crawlerPongFactory.createUDPCrawlerPong(msgNewOnly);
 		
 		assertEquals(1, pong.getPayload()[0]);
 		
 		
 		
 		//now, make one other UP support that message as well
 		
 		notSupporting.getConnectionCapabilities().setMessagesSupportedVendorMessage(msvm);
 		
 		assertGreaterThan(0,notSupporting.getConnectionCapabilities().remoteHostSupportsUDPCrawling());
 		assertGreaterThan(0,supporting.getConnectionCapabilities().remoteHostSupportsUDPCrawling());
 		
 		pong = crawlerPongFactory.createUDPCrawlerPong(msgNewOnly);
 		
 		assertEquals(2,pong.getPayload()[0]);
 	    } finally {
 	        Constants.MINUTE = savedMinute;
 	    }
 	}
 	
 	public void testMsgAgents() throws Exception {
 		PrivilegedAccessor.setValue(Constants.class,"MINUTE",new Long(1));
        UDPCrawlerPing msgAgents = new UDPCrawlerPing(new GUID(GUID.makeGuid()),3,3,(byte)0x8);
 		UDPCrawlerPong pong = crawlerPongFactory.createUDPCrawlerPong(msgAgents);
        byte[] payload = pong.getPayload();
        int agentsOffset=(payload[0]+payload[1])*6+3;
        int agentsSize = ByteUtils.leb2short(payload,agentsOffset);
 		assertGreaterThan(0, agentsSize);
 		
        ByteArrayInputStream bais = 
            new ByteArrayInputStream(payload,agentsOffset+2,agentsSize);
            
        GZIPInputStream gais = null;
        gais = new GZIPInputStream(bais);
        DataInputStream dais = new DataInputStream(gais);
        byte [] length = new byte[2];
        dais.readFully(length);
        int len = ByteUtils.leb2short(length,0);
        byte []agents = new byte[len];
        dais.readFully(agents);
            
        String agentsString = StringUtils.getUTF8String(agents);
 		//we should have 3 agents reported.
 		StringTokenizer tok = new StringTokenizer(agentsString,
 				UDPCrawlerPong.AGENT_SEP);
 		
 		assertEquals(7,tok.countTokens());
 		
 		while(tok.hasMoreTokens())
 			assertEquals(LimeWireUtils.getHttpServer(),tok.nextToken());
 		
 	}
    
    public void testMsgNodeConnectionUptime() throws Exception {
        ApplicationSettings.AVERAGE_CONNECTION_TIME.setValue(5);
        PrivilegedAccessor.setValue(Constants.class,"MINUTE",new Long(1));
        UDPCrawlerPing msgNodeUptime = new UDPCrawlerPing(new GUID(GUID.makeGuid()),3,3,(byte)0x10);
        UDPCrawlerPong pong = crawlerPongFactory.createUDPCrawlerPong(msgNodeUptime);
        byte[] payload = pong.getPayload();
        byte format =  (byte) (payload[2] & UDPCrawlerPing.FEATURE_MASK);
        assertFalse((format & UDPCrawlerPing.CONNECTION_TIME)
                == UDPCrawlerPing.CONNECTION_TIME);
        assertTrue((format & UDPCrawlerPing.NODE_UPTIME)
                == UDPCrawlerPing.NODE_UPTIME);
        
        //we should have an uptime > 5
        assertGreaterThan(5,ByteUtils.leb2int(payload,3));
        //the offset should be correctly set
        byte[] current = new byte[6];
        System.arraycopy(payload,7,current,0,6);
        IpPort combo = 
            NetworkUtils.getIpPort(current, java.nio.ByteOrder.LITTLE_ENDIAN);
        Endpoint e = new Endpoint(combo.getInetAddress(),combo.getPort());
        assertEquals(UP1.getInetAddress(),e.getInetAddress());
        assertTrue(e.getPort() == UP1.getPort());
    }
    
    public void testQueryReplies() throws Exception {
    	// try w/o any query replies
    	UDPCrawlerPing repliesPing = new UDPCrawlerPing(new GUID(GUID.makeGuid()),
    			UDPCrawlerPing.ALL,
    			UDPCrawlerPing.ALL,
    			UDPCrawlerPing.REPLIES);
    	UDPCrawlerPong repliesPong = crawlerPongFactory.createUDPCrawlerPong(repliesPing);
    	byte [] payload = repliesPong.getPayload();
    	byte format =  (byte) (payload[2] & UDPCrawlerPing.FEATURE_MASK);
    	assertTrue((format & UDPCrawlerPing.REPLIES) == UDPCrawlerPing.REPLIES);
    	assertEquals((3 + 6 * (6  + 4)), payload.length);
    	for (int i = 9; i < payload.length; i += 6) {
    		assertEquals(0, ByteUtils.leb2int(payload, i));
    		i += 4;
    	}
    	
    	// send a few query replies on each conn
    	Response[] res = new Response[1];
        for (int j = 0; j < res.length; j++)
            res[j] = responseFactory.createResponse(10, 10, "not proxied", UrnHelper.SHA1);
        Message reply = 
            queryReplyFactory.createQueryReply(GUID.makeGuid(), (byte) 3, 6356,
                InetAddress.getLocalHost().getAddress(), 0, res, GUID.makeGuid(), new byte[0], false, false,
                true, true, false, false, null);
        
        int [] sent = new int[]{1,2,3,1,2,3};
        LEAF_1.send(reply);LEAF_1.flush();
        LEAF_2.send(reply);
        LEAF_2.send(reply);LEAF_2.flush();
        LEAF_3.send(reply);
        LEAF_3.send(reply);
        LEAF_3.send(reply);LEAF_3.flush();
        UP1.send(reply);UP1.flush();
        UP2.send(reply);
        UP2.send(reply);UP2.flush();
        UP3.send(reply);
        UP3.send(reply);
        UP3.send(reply);UP3.flush();
        Thread.sleep(100);
        
        repliesPong = crawlerPongFactory.createUDPCrawlerPong(repliesPing);
        payload = repliesPong.getPayload();
        
        int current = 0;
        for (int i = 9; i < payload.length; i += 6) {
    		assertEquals(sent[current++], ByteUtils.leb2int(payload, i));
    		i += 4;
    	}
    }
    
    public void testMsgDHTStatus() throws Exception {
        UDPCrawlerPing msgDHTStatus = new UDPCrawlerPing(new GUID(GUID.makeGuid()),3,3,(byte) (0x1 << 6));
        UDPCrawlerPong pong = crawlerPongFactory.createUDPCrawlerPong(msgDHTStatus);
        byte[] payload = pong.getPayload();
        byte format =  (byte) (payload[2] & UDPCrawlerPing.FEATURE_MASK);
        assertFalse((format & UDPCrawlerPing.CONNECTION_TIME)
                == UDPCrawlerPing.CONNECTION_TIME);
        assertTrue((format & UDPCrawlerPing.DHT_STATUS)
                == UDPCrawlerPing.DHT_STATUS);
        
        byte status = payload[3];
        assertFalse( (status & UDPCrawlerPong.DHT_WAITING_MASK) == UDPCrawlerPong.DHT_WAITING_MASK);
        assertFalse( (status & UDPCrawlerPong.DHT_PASSIVE_MASK) == UDPCrawlerPong.DHT_PASSIVE_MASK);
        assertTrue( (status & UDPCrawlerPong.DHT_ACTIVE_MASK) == UDPCrawlerPong.DHT_ACTIVE_MASK);
        
    }
    
 	private void tryMessage() throws Exception {
 		assertTrue(udpService.isListening());
 		UDP_ACCESS.setSoTimeout(5000);
 		
 		
 		_udpAddress = UDP_ACCESS.getLocalAddress();
 		
 		//send a packet
 		ByteArrayOutputStream baos = new ByteArrayOutputStream();
 		msgAll.write(baos);
 		DatagramPacket pack = new DatagramPacket(baos.toByteArray(),
 							baos.toByteArray().length,
							_udpAddress, PORT);
 		
 		assertNotNull(baos.toByteArray());
 		assertNotNull(_udpAddress);
 		
 		UDP_ACCESS.send(pack);
 		
 		//now read the response 		
 		//_udpPort = UDP_ACCESS.getLocalPort();
 		pack = new DatagramPacket(new byte[1000],1000);
 		
 		//not catching IOEx here because not replying is a valid scenario.
 		
 		UDP_ACCESS.receive(pack);
 		
 		
 		//parse the response
 		InputStream in = new ByteArrayInputStream(pack.getData());
        byte[] buf = new byte[23];
        in.read(buf,0,23);
        assertEquals((byte)0x31,buf[16]);
        byte vendorId[] = new byte[4];
        in.read(vendorId, 0, 4);
        assertEquals(VendorMessage.F_LIME_VENDOR_ID, vendorId);
        byte rest[] = new byte[in.available()];
        in.read(rest, 0, rest.length);
        //get the selector....
        int selector = ByteUtils.ushort2int(ByteUtils.leb2short(rest, 0));
        // get the version....
        int version = ByteUtils.ushort2int(ByteUtils.leb2short(rest, 2));
        assertEquals(VendorMessage.F_CRAWLER_PONG, selector);
        assertEquals(UDPCrawlerPong.VERSION, version);
 	}
    
    private static class UDPCrawlerPongParserStub implements VendorMessageParser {
        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, 
                byte[] restOf, Network network) throws BadPacketException {
            return null;
        }
    }
}    
