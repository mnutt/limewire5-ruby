package com.limegroup.gnutella.connection;


import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.concurrent.ThreadExecutor;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.FilterSettings;
import org.limewire.core.settings.MessageSettings;
import org.limewire.io.GUID;
import org.limewire.io.IOUtils;
import org.limewire.nio.NIOServerSocket;
import org.limewire.nio.observer.AcceptObserver;
import org.limewire.service.ErrorService;
import org.limewire.util.StringUtils;

import com.limegroup.gnutella.BlockingConnectionUtils;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.ServerSideTestCase;
import com.limegroup.gnutella.StubGnetConnectObserver;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.handshaking.HandshakeResponder;
import com.limegroup.gnutella.handshaking.HandshakeResponse;
import com.limegroup.gnutella.handshaking.StubHandshakeResponder;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingReplyFactory;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.PingRequestFactory;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVM;


/**
 * Tests basic connection properties.  All tests are done once with compression
 * and once without.
 */
public class RoutedConnectionTest extends ServerSideTestCase {
    
    private static final int LISTEN_PORT = 12350;
    private ConnectionManager connectionManager;
    private PingRequestFactory pingRequestFactory;
    private PingReplyFactory pingReplyFactory;
    private QueryRequestFactory queryRequestFactory;
    private RoutedConnectionFactory routedConnectionFactory;
    private BlockingConnectionFactory blockingConnectionFactory;

    public RoutedConnectionTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(RoutedConnectionTest.class);
    }    

    public static void main(String argv[]) {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    public int getNumberOfUltrapeers() {
        return 0;
    }

    @Override
    public int getNumberOfLeafpeers() {
        return 0;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        connectionManager = injector.getInstance(ConnectionManager.class);
        pingRequestFactory = injector.getInstance(PingRequestFactory.class);
        pingReplyFactory = injector.getInstance(PingReplyFactory.class);
        queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
        routedConnectionFactory = injector.getInstance(RoutedConnectionFactory.class);
        blockingConnectionFactory = injector.getInstance(BlockingConnectionFactory.class);
    }
    
    private Expectations buildCapVMExpectations(final CapabilitiesVM cvm, final boolean tcp, final boolean fwt) {
        return new Expectations(){{
            atLeast(1).of(cvm).canAcceptIncomingTCP();
            will(returnValue(tcp));
            atLeast(1).of(cvm).canDoFWT();
            will(returnValue(fwt));
            
            
            // capabilities we don't care about
            ignoring(cvm).isActiveDHTNode();
            ignoring(cvm).isPassiveDHTNode();
            ignoring(cvm).supportsTLS();
            ignoring(cvm).supportsWhatIsNew();
            allowing(cvm).supportsSIMPP();
            will(returnValue(-1));
            ignoring(cvm).isPassiveLeafNode();
            ignoring(cvm).supportsFeatureQueries();
            allowing(cvm).supportsUpdate();
            will(returnValue(-1));
        }};
    }
    
    private Expectations buildQueryExpectations(final QueryRequest query, 
            final boolean tcp, 
            final boolean fwt) throws Exception {
        return new Expectations(){{
            allowing(query).canDoFirewalledTransfer();
            will(returnValue(fwt));
            allowing(query).isFirewalledSource();
            will(returnValue(!tcp));
            
            // stubbed out stuff
            allowing(query).getFunc();
            will(returnValue(Message.F_QUERY));
            ignoring(query).isOriginated();
            ignoring(query).getHops();
            ignoring(query).getCreationTime();
            allowing(query).getNetwork();
            will(returnValue(Message.Network.TCP));
            ignoring(query).getLength();
            ignoring(query).getTTL();
        }};
    }
    
    public void testFirewallFirewallDrop() throws Exception {
        ConnectionManager cm = connectionManager;
        BlockingConnection conn = createLeafConnection();
        BlockingConnectionUtils.drain(conn);
        assertEquals(1, cm.getNumConnections());
        
        
        GnutellaConnection mc = (GnutellaConnection)cm.getConnections().get(0);
        Mockery mockery = new Mockery();
        final CapabilitiesVM cvm = mockery.mock(CapabilitiesVM.class);
        mockery.checking(buildCapVMExpectations(cvm, false, false));
        final QueryRequest query = mockery.mock(QueryRequest.class);
        mockery.checking(buildQueryExpectations(query, false, false));
        mc.handleVendorMessage(cvm);
        assertFalse(mc.shouldSendQuery(query));
        mockery.assertIsSatisfied();
    }
    
    public void testFirewallFirewallDropDisabled() throws Exception {
        MessageSettings.ULTRAPEER_FIREWALL_FILTERING.setValue(false);
        ConnectionManager cm = connectionManager;
        BlockingConnection conn = createLeafConnection();
        BlockingConnectionUtils.drain(conn);
        assertEquals(1, cm.getNumConnections());
        
        
        GnutellaConnection mc = (GnutellaConnection)cm.getConnections().get(0);
        Mockery mockery = new Mockery();
        final CapabilitiesVM cvm = mockery.mock(CapabilitiesVM.class);
        mockery.checking(buildCapVMExpectations(cvm, false, false));
        final QueryRequest query = mockery.mock(QueryRequest.class);
        mockery.checking(buildQueryExpectations(query, false, false));
        mc.handleVendorMessage(cvm);
        assertTrue(mc.shouldSendQuery(query));
    }
    
    public void testNonFirewallQuerySend() throws Exception {
        ConnectionManager cm = connectionManager;
        BlockingConnection conn = createLeafConnection();
        BlockingConnectionUtils.drain(conn);
        assertEquals(1, cm.getNumConnections());
        
        
        GnutellaConnection mc = (GnutellaConnection)cm.getConnections().get(0);
        Mockery mockery = new Mockery();
        final CapabilitiesVM cvm = mockery.mock(CapabilitiesVM.class);
        mockery.checking(buildCapVMExpectations(cvm, false, false));
        final QueryRequest query = mockery.mock(QueryRequest.class);
        mockery.checking(buildQueryExpectations(query, true, false));
        mc.handleVendorMessage(cvm);
        assertTrue(mc.shouldSendQuery(query));
        mockery.assertIsSatisfied();
    }
    
    public void testNonFirewallLeafSend() throws Exception {
        ConnectionManager cm = connectionManager;
        BlockingConnection conn = createLeafConnection();
        BlockingConnectionUtils.drain(conn);
        assertEquals(1, cm.getNumConnections());
        
        
        GnutellaConnection mc = (GnutellaConnection)cm.getConnections().get(0);
        Mockery mockery = new Mockery();
        final CapabilitiesVM cvm = mockery.mock(CapabilitiesVM.class);
        mockery.checking(buildCapVMExpectations(cvm, true, false));
        final QueryRequest query = mockery.mock(QueryRequest.class);
        mockery.checking(buildQueryExpectations(query, false, false));
        mc.handleVendorMessage(cvm);
        assertTrue(mc.shouldSendQuery(query));
        mockery.assertIsSatisfied();
    }
    
    public void testNonFirewallBothSend() throws Exception {
        ConnectionManager cm = connectionManager;
        BlockingConnection conn = createLeafConnection();
        BlockingConnectionUtils.drain(conn);
        assertEquals(1, cm.getNumConnections());
        
        
        GnutellaConnection mc = (GnutellaConnection)cm.getConnections().get(0);
        Mockery mockery = new Mockery();
        final CapabilitiesVM cvm = mockery.mock(CapabilitiesVM.class);
        mockery.checking(buildCapVMExpectations(cvm, true, false));
        final QueryRequest query = mockery.mock(QueryRequest.class);
        mockery.checking(buildQueryExpectations(query, true, false));
        mc.handleVendorMessage(cvm);
        assertTrue(mc.shouldSendQuery(query));
        mockery.assertIsSatisfied();
    }
    
    public void testBothFWTSend() throws Exception {
        ConnectionManager cm = connectionManager;
        BlockingConnection conn = createLeafConnection();
        BlockingConnectionUtils.drain(conn);
        assertEquals(1, cm.getNumConnections());
        
        
        GnutellaConnection mc = (GnutellaConnection)cm.getConnections().get(0);
        Mockery mockery = new Mockery();
        final CapabilitiesVM cvm = mockery.mock(CapabilitiesVM.class);
        mockery.checking(buildCapVMExpectations(cvm, false, true));
        final QueryRequest query = mockery.mock(QueryRequest.class);
        mockery.checking(buildQueryExpectations(query, false, true));
        mc.handleVendorMessage(cvm);
        assertTrue(mc.shouldSendQuery(query));
        mockery.assertIsSatisfied();
    }
    
    public void testNoFWTLeafDrop() throws Exception {
        ConnectionManager cm = connectionManager;
        BlockingConnection conn = createLeafConnection();
        BlockingConnectionUtils.drain(conn);
        assertEquals(1, cm.getNumConnections());
        
        
        GnutellaConnection mc = (GnutellaConnection)cm.getConnections().get(0);
        Mockery mockery = new Mockery();
        final CapabilitiesVM cvm = mockery.mock(CapabilitiesVM.class);
        mockery.checking(buildCapVMExpectations(cvm, false, true));
        final QueryRequest query = mockery.mock(QueryRequest.class);
        mockery.checking(buildQueryExpectations(query, false, false));
        mc.handleVendorMessage(cvm);
        assertFalse(mc.shouldSendQuery(query));
        mockery.assertIsSatisfied();
    }
    
    public void testNoFWTQueryDrop() throws Exception {
        ConnectionManager cm = connectionManager;
        BlockingConnection conn = createLeafConnection();
        BlockingConnectionUtils.drain(conn);
        assertEquals(1, cm.getNumConnections());
        
        
        GnutellaConnection mc = (GnutellaConnection)cm.getConnections().get(0);
        Mockery mockery = new Mockery();
        final CapabilitiesVM cvm = mockery.mock(CapabilitiesVM.class);
        mockery.checking(buildCapVMExpectations(cvm, false, false));
        final QueryRequest query = mockery.mock(QueryRequest.class);
        mockery.checking(buildQueryExpectations(query, false, true));
        mc.handleVendorMessage(cvm);
        assertFalse(mc.shouldSendQuery(query));
        mockery.assertIsSatisfied();
    }
    
    /**
     * Tests the method for checking whether or not a connection is stable.
     */
    public void testIsStable() throws Exception {
        BlockingConnection conn = createLeafConnection();
        Thread.sleep(500);
        
        RoutedConnection routedConnection = connectionManager.getConnections().get(0);
        assertTrue("should not yet be considered stable", !routedConnection.isStable());
        Thread.sleep(6000);
        assertTrue("connection should be considered stable", routedConnection.isStable());
        
        // Keep in mind that Connection is a loopback
        // connection! That means Acceptor creates a
        // second ManagedConnection instance for the
        // incoming connection. Give that instance a
        // bit time to die or the next test will fail
        // frequently.
        conn.close();
        Thread.sleep(500);
    }
    
    public void testConnectionStatsRecorded() throws Exception {
        ConnectionManager cm = connectionManager;
        assertEquals(0, cm.getNumConnections());

        BlockingConnection in = createLeafConnection();
        BlockingConnectionUtils.drain(in);
        assertEquals(1, cm.getNumConnections());
        
        
        RoutedConnection out = cm.getConnections().get(0);
        
        // Record initial msgs.
        int initialNumSent = out.getConnectionMessageStatistics().getNumMessagesSent();
        //long initialBytesSent = out.getUncompressedBytesSent();
        long initialBytesRecv = in.getConnectionBandwidthStatistics().getUncompressedBytesReceived();
        
        out.send(pingRequestFactory.createPingRequest((byte)3));
        
        long start = System.currentTimeMillis();        
        PingRequest pr = (PingRequest)in.receive();
        long elapsed = System.currentTimeMillis() - start;
        assertEquals("unexpected number of sent messages", initialNumSent + 1, out.getConnectionMessageStatistics().getNumMessagesSent());
        assertEquals( initialBytesRecv + pr.getTotalLength(), in.getConnectionBandwidthStatistics().getUncompressedBytesReceived() );
        // due to delay in updating, this stat is off always.
        //assertEquals( initialBytesSent + pr.getTotalLength(), out.getUncompressedBytesSent() );
        assertLessThan("Unreasonably long send time", 500, elapsed);
        assertEquals("hopped something other than 0", 0, pr.getHops());
        assertEquals("unexpected ttl", 3, pr.getTTL());
        
        out.close();
        in.close();
    }
    
    public void testForwardsGGEP() throws Exception {
        ConnectionManager cm = connectionManager;
        assertEquals(0, cm.getNumConnections());

        BlockingConnection conn = createLeafConnection();
        BlockingConnectionUtils.drain(conn);
        assertEquals(1, cm.getNumConnections());
        
        
        RoutedConnection out = cm.getConnections().get(0);

        out.send(pingReplyFactory.create(GUID.makeGuid(), (byte)1));
		Message m = BlockingConnectionUtils.getFirstInstanceOfMessageType(conn, PingReply.class);
		assertNotNull(m);
		assertInstanceof("should be a pong", PingReply.class, m);
		
		PingReply pr = (PingReply)m;

        assertTrue("pong should have GGEP", pr.hasGGEPExtension());
		assertTrue("should not support unicast", !pr.supportsUnicast());

		// not necessary check, and is difficult to mock right.
		//assertTrue("incorrect daily uptime!", pr.getDailyUptime() > 0);
		assertTrue("pong should have GGEP", pr.hasGGEPExtension());
		out.close();
		conn.close();
    }

	/**
	 * Tests the method for checking whether or not the connection is a high-
	 * degree connection that maintains high numbers of intra-Ultrapeer 
	 * connections.
	 */
	public void testIsHighDegreeConnection() throws Exception {
        BlockingConnection conn = createLeafConnection();
		assertTrue("connection should be high degree",  conn.getConnectionCapabilities().isHighDegreeConnection());
        conn.close();
	}

	// Tests to make sure that connections are closed correctly from the
    //	  client side.
    public void testClientSideClose() throws Exception {
        ConnectionManager cm = connectionManager;
        assertEquals(0, cm.getNumConnections());

        BlockingConnection out = createLeafConnection();
        BlockingConnectionUtils.drain(out);
        assertEquals(1, cm.getNumConnections());
        RoutedConnection in = cm.getConnections().get(0);

        //in=acceptor.accept(); 
		assertTrue("connection should be open", out.isOpen());
        out.close();
        Thread.sleep(100);
        assertTrue("connection should not be open", !out.isOpen());
        Thread.sleep(100);
        assertTrue(!in.isOpen());
	}

	 // Tests to make sure that connections are closed correctly from the
	 // server side.
    public void testServerSideClose() throws Exception {
        ConnectionManager cm = connectionManager;
        assertEquals(0, cm.getNumConnections());

        BlockingConnection out = createLeafConnection();
        BlockingConnectionUtils.drain(out);
        assertEquals(1, cm.getNumConnections());
        RoutedConnection in = cm.getConnections().get(0);
        assertTrue("connection should be open", out.isOpen());
        
        out.close();
        Message m = pingRequestFactory.createPingRequest((byte)4);
        m.hop();
        in.send(m);   
        Thread.sleep(500);
        in.send(pingRequestFactory.createPingRequest((byte)4));
        Thread.sleep(500);
        in.send(pingRequestFactory.createPingRequest((byte)4));
        Thread.sleep(500);

        assertTrue("connection should not be open", !in.isOpen());
		Thread.sleep(2000);
    }
    
    public void testHashFiltering() throws Exception {
        URN sha1 = URN.createSHA1Urn("urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB");
        QueryRequest urnFile = queryRequestFactory.createQuery(sha1,"java");
        
        GnutellaConnection mc = (GnutellaConnection)routedConnectionFactory.createRoutedConnection("", 1);
        // default should be no filtering
        assertFalse(mc.isSpam(urnFile));
        
        // now turn filtering on and rebuild filters
        FilterSettings.FILTER_HASH_QUERIES.setValue(true);
        mc = (GnutellaConnection)routedConnectionFactory.createRoutedConnection("", 1);
        
        assertTrue(mc.isSpam(urnFile));

        FilterSettings.FILTER_HASH_QUERIES.setValue(false);
    }
    
    public void testNonBlockingHandshakeSucceeds() throws Exception {
        RoutedConnection mc = routedConnectionFactory.createRoutedConnection("127.0.0.1", LISTEN_PORT);
        ConnectionAcceptor acceptor = new ConnectionAcceptor();
        StubGnetConnectObserver observer = new StubGnetConnectObserver();
        acceptor.start();
        ConnectionSettings.PREFERENCING_ACTIVE.setValue(false);
        try {
            mc.initialize(observer);
            observer.waitForResponse(5000);
            assertTrue(observer.isConnect());
            assertFalse(observer.isBadHandshake());
            assertFalse(observer.isNoGOK());
            assertFalse(observer.isShutdown());
            assertEquals("NIODispatcher", observer.getFinishedThread().getName());
            mc.close();
        } finally {
            acceptor.shutdown();
        }
    }
    
    public void testNonBlockingBadHandshake() throws Exception {
        RoutedConnection mc = routedConnectionFactory.createRoutedConnection("127.0.0.1", LISTEN_PORT);
        ConnectionAcceptor acceptor = new ConnectionAcceptor();
        StubGnetConnectObserver observer = new StubGnetConnectObserver();
        acceptor.start();
        ConnectionSettings.PREFERENCING_ACTIVE.setValue(false);
        try {
            acceptor.getObserver().setBadHandshake(true);
            mc.initialize(observer);
            observer.waitForResponse(5000);
            assertFalse(observer.isConnect());
            assertTrue(observer.isBadHandshake());
            assertFalse(observer.isNoGOK());
            assertFalse(observer.isShutdown());
            assertEquals("NIODispatcher", observer.getFinishedThread().getName());
            mc.close();
        } finally {
            acceptor.shutdown();
        }       
    }
    
    public void testNonBlockingNGOK() throws Exception {
        RoutedConnection mc = routedConnectionFactory.createRoutedConnection("127.0.0.1", LISTEN_PORT);
        ConnectionAcceptor acceptor = new ConnectionAcceptor();
        StubGnetConnectObserver observer = new StubGnetConnectObserver();
        acceptor.start();
        ConnectionSettings.PREFERENCING_ACTIVE.setValue(false);
        try {
            acceptor.getObserver().setNoGOK(true);
            mc.initialize(observer);
            observer.waitForResponse(5000);
            assertFalse(observer.isConnect());
            assertFalse(observer.isBadHandshake());
            assertTrue(observer.isNoGOK());
            assertEquals(401, observer.getCode());
            assertFalse(observer.isShutdown());
            assertEquals("NIODispatcher", observer.getFinishedThread().getName());
            mc.close();
        } finally {
            acceptor.shutdown();
        }           
    }
    
    public void testNonBlockingHandshakeTimeout() throws Exception {
        RoutedConnection mc = routedConnectionFactory.createRoutedConnection("127.0.0.1", LISTEN_PORT);
        ConnectionAcceptor acceptor = new ConnectionAcceptor();
        StubGnetConnectObserver observer = new StubGnetConnectObserver();
        acceptor.start();
        ConnectionSettings.PREFERENCING_ACTIVE.setValue(false);
        try {
            acceptor.getObserver().setTimeout(true);
            mc.initialize(observer);
            observer.waitForResponse(10000);
            assertFalse(observer.isConnect());
            assertFalse(observer.isBadHandshake());
            assertFalse(observer.isNoGOK());
            assertTrue(observer.isShutdown());
            assertEquals("NIODispatcher", observer.getFinishedThread().getName());
            mc.close();
        } finally {
            acceptor.shutdown();
        }            
    }

    class EmptyResponder implements HandshakeResponder {
        public HandshakeResponse respond(HandshakeResponse response,
                                         boolean outgoing) {
            Properties props = new Properties();
            return HandshakeResponse.createResponse(props);
        }
        
        public void setLocalePreferencing(boolean b) {}
    }
    
    private class ConnectionAcceptor {
        private ServerSocket socket;
        private SimpleAcceptObserver observer;
        
        
        public void start() throws Exception {
            observer = new SimpleAcceptObserver();
            socket = new NIOServerSocket(observer);
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(LISTEN_PORT));
        }
        
        public void shutdown() throws Exception {
            socket.close();
        }
        
        public SimpleAcceptObserver getObserver() {
            return observer;
        }
    }    
    
    private class SimpleAcceptObserver implements AcceptObserver {
        private boolean noGOK = false;
        private boolean timeout = false;
        private boolean badHandshake = false;

        public void handleIOException(IOException iox) {
        }

        public void handleAccept(final Socket socket) throws IOException {
            ThreadExecutor.startThread(new Runnable() {
                public void run() {
                    try {
                        if (badHandshake) {
                            socket.close();
                            return;
                        }
                        
                        if(timeout) {
                            socket.setSoTimeout(60000);
                            socket.getInputStream().read(new byte[2048]);
                            return;
                        }
                        
                        socket.setSoTimeout(3000);
                        InputStream in = socket.getInputStream();
                        String word = IOUtils.readWord(in, 9);
                        if (!word.equals("GNUTELLA"))
                            throw new IOException("Bad word: " + word);

                        if (noGOK) {
                            socket.getOutputStream().write(StringUtils.toAsciiBytes("GNUTELLA/0.6 401 Failed\r\n\r\n"));
                            socket.getOutputStream().flush();
                            return;
                        }

                        final BlockingConnection con = blockingConnectionFactory.createConnection(socket);
                        con.initialize(null, new StubHandshakeResponder(), 1000);
                    } catch (Exception e) {
                        ErrorService.error(e);
                    }
                }
            }, "conninit");
        }

        public void shutdown() {
        }

        public void setBadHandshake(boolean badHandshake) {
            this.badHandshake = badHandshake;
        }

        public void setNoGOK(boolean noGOK) {
            this.noGOK = noGOK;
        }
        
        public void setTimeout(boolean timeout) {
            this.timeout = timeout;
        }
        
        public void clear() {
            this.badHandshake = false;
            this.noGOK = false;
            this.timeout = false;
        }
    }
}
