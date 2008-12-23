package com.limegroup.gnutella;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.Test;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.DHTSettings;
import org.limewire.core.settings.UltrapeerSettings;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.io.NetworkUtils;
import org.limewire.listener.ListenerSupport;
import org.limewire.mojito.EntityKey;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.result.FindValueResult;
import org.limewire.mojito.result.StoreResult;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
import org.limewire.net.SocketsManager;
import org.limewire.net.SocketsManager.ConnectType;
import org.limewire.security.SecureMessageVerifier;
import org.limewire.util.CommonUtils;
import org.limewire.util.PrivilegedAccessor;
import org.limewire.util.TestUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.connection.ConnectionLifecycleListener;
import com.limegroup.gnutella.connection.GnetConnectObserver;
import com.limegroup.gnutella.connection.GnutellaConnection;
import com.limegroup.gnutella.connection.MessageReaderFactory;
import com.limegroup.gnutella.connection.RoutedConnection;
import com.limegroup.gnutella.connection.RoutedConnectionFactory;
import com.limegroup.gnutella.dht.DHTEvent;
import com.limegroup.gnutella.dht.DHTEventListener;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.filters.SpamFilterFactory;
import com.limegroup.gnutella.handshaking.BadHandshakeException;
import com.limegroup.gnutella.handshaking.HandshakeResponderFactory;
import com.limegroup.gnutella.handshaking.HeadersFactory;
import com.limegroup.gnutella.handshaking.NoGnutellaOkException;
import com.limegroup.gnutella.helpers.UrnHelper;
import com.limegroup.gnutella.library.FileDescChangeEvent;
import com.limegroup.gnutella.library.FileDescStub;
import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.PingRequestFactory;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.PushRequestImpl;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryReplyFactory;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.messages.StaticMessages;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactory;
import com.limegroup.gnutella.messages.vendor.DHTContactsMessage;
import com.limegroup.gnutella.messages.vendor.HeadPing;
import com.limegroup.gnutella.messages.vendor.HeadPong;
import com.limegroup.gnutella.messages.vendor.HeadPongFactory;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutella.messages.vendor.PushProxyAcknowledgement;
import com.limegroup.gnutella.messages.vendor.PushProxyRequest;
import com.limegroup.gnutella.routing.QRPUpdater;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.search.SearchResultHandler;
import com.limegroup.gnutella.simpp.SimppManager;
import com.limegroup.gnutella.stubs.NetworkManagerStub;
import com.limegroup.gnutella.stubs.ReplyHandlerStub;
import com.limegroup.gnutella.util.LeafConnection;
import com.limegroup.gnutella.util.LimeTestCase;
import com.limegroup.gnutella.util.TestConnection;
import com.limegroup.gnutella.util.TestConnectionFactory;
import com.limegroup.gnutella.util.TestConnectionManager;
import com.limegroup.gnutella.version.UpdateHandler;

// TODO write test for storing bypassed results
public final class MessageRouterImplTest extends LimeTestCase {

    private MessageRouterImpl messageRouterImpl;
    
    private TestConnectionManager connectionManager;

    private Mockery context;

    private TestConnectionFactory testConnectionFactory;
    
    private static final int NUM_CONNECTIONS = 20;

    /**
     * Constant array for the keywords that I should have (this node).
     */
    private static final String[] MY_KEYWORDS = {
        "me",
    };

    public MessageRouterImplTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(MessageRouterImplTest.class);
    }    
   
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}  
	
	@Override
	protected void setUp() throws Exception {
	    context = new Mockery();
	}

	/**
     * Helper method to configure the injector and return it. It also sets 
     * 
     * Neeeded as each test method needs its own participants. 
     */
    private Injector createInjectorAndInitialize(Module... modules) {
        Injector injector = LimeTestUtils.createInjector(modules);
        testConnectionFactory = injector.getInstance(TestConnectionFactory.class);
        messageRouterImpl = (MessageRouterImpl) injector.getInstance(MessageRouter.class);
        messageRouterImpl.start();
        return injector;
    }
    
    /**
     * Calls {@link #createInjectorAndInitialize(Module...)} and with a module
     * that sets up the TestConnectionManager. 
     */
    private Injector createDefaultInjector(Module... modules) {
        Module m = new AbstractModule() {
            @Override
            protected void configure() {
                bind(ConnectionManager.class).to(TestConnectionManager.class);
            }
        };
        List<Module> list = new ArrayList<Module>();
        list.addAll(Arrays.asList(modules));
        list.add(m);
        Injector injector = createInjectorAndInitialize(list.toArray(new Module[0]));
        connectionManager = (TestConnectionManager)injector.getInstance(ConnectionManager.class);
        return injector;
    }
    
    /**
     * Tests the method for forwarding queries to leaves.
     * 
     * @throws Exception if any error in the test occurs
     */
    public void testForwardQueryRequestToLeaves() throws Exception  {
        
        Injector injector = createDefaultInjector();

        TestConnectionManager tcm = (TestConnectionManager)injector.getInstance(ConnectionManager.class);
        tcm.configureManagerWithVariedLeafs();
        tcm.resetAndInitialize();
        
        QueryRequestFactory queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
        RoutedConnectionFactory connectionFactory = injector.getInstance(RoutedConnectionFactory.class);
        
        QueryRequest qr = queryRequestFactory.createQuery(LeafConnection.ALT_LEAF_KEYWORD);
        ReplyHandler rh = connectionFactory.createRoutedConnection("localhost", 6346);
        
        messageRouterImpl.forwardQueryRequestToLeaves(qr, rh);
        
        int numQueries = tcm.getNumLeafQueries();
        assertEquals("unexpected number of queries received by leaves", 
            UltrapeerSettings.MAX_LEAVES.getValue()/2, numQueries);

        // reconfigre connection manager for a popular query
        tcm.configureDefaultManager();
        tcm.resetAndInitialize();
        
        qr = queryRequestFactory.createQuery(LeafConnection.LEAF_KEYWORD);
        
        messageRouterImpl.forwardQueryRequestToLeaves(qr, rh);
        
        numQueries = tcm.getNumLeafQueries();
        assertEquals("unexpected number of queries received by leaves", 
            UltrapeerSettings.MAX_LEAVES.getValue()/4, numQueries); 
    }

    
    /**
     * Tests the method for creating <tt>QueryReply</tt> instances from
     * <tt>Response</tt> arrays.
     */
    // TODO refactor to unit test case and move to different class
    public void testResponsesToQueryReplies() throws Exception {
        ConnectionSettings.CANNOT_DO_FWT.setValue(true);
        
        // needs valid address && port
        final NetworkManagerStub networkManagerStub = new NetworkManagerStub();
        networkManagerStub.setAddress(new byte[] { (byte) 192, (byte) 168, 0, 1 });
        networkManagerStub.setPort(5555);
        
        Injector injector = createInjectorAndInitialize(new AbstractModule() {
            @Override
            protected void configure() {
                bind(NetworkManager.class).toInstance(networkManagerStub);
            }
        });
        ResponseFactory responseFactory = injector.getInstance(ResponseFactory.class);
        QueryRequestFactory queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
        UploadManager uploadManager = injector.getInstance(UploadManager.class);

        uploadManager.start();
        try {
            Response[] res = new Response[20];
            Arrays.fill(res, responseFactory.createResponse(0, 10, "test", UrnHelper.SHA1));
            QueryRequest query = queryRequestFactory.createQuery("test");
            
            Iterable iter = messageRouterImpl.responsesToQueryReplies(res, query, 10, null); 
            int size = sizeof(iter);

            assertEquals("responses should have been put in 2 hits", 2, size);

            // make sure the query has high hops to test the threshold
            query.hop();
            query.hop();
            query.hop();

            iter = messageRouterImpl.responsesToQueryReplies(res, query, 10, null);
            size = sizeof(iter);

            assertEquals("responses should have been put in 1 hits", 1, size);

            iter = messageRouterImpl.responsesToQueryReplies(res, query, 1, null);
            size = sizeof(iter);

            assertEquals("responses should have been put in 20 hits", 20, size);
        } finally {
            uploadManager.stop();
        }
    }
    
    @SuppressWarnings("unused")
    private int sizeof(Iterable iter) {
        int i = 0;
        for(Object o : iter)
            i++;
        return i;
    }

    /**
     * Test to make sure that the query route tables are forwarded correctly
     * between Ultrapeers.
     */
    public void testIntraUltrapeerForwardQueryRouteTables() throws Exception {
        
        Injector injector = createInjectorAndInitialize(new AbstractModule() {
            @Override
            protected void configure() {
                bind(ConnectionManager.class).to(TestConnectionManager.class);
                bind(QRPUpdater.class).to(TestQRPUpdater.class);
            }
        });
        
        // use connection manager with default configuration
        TestConnectionManager tcm = (TestConnectionManager)injector.getInstance(ConnectionManager.class);
        tcm.resetAndInitialize();

        messageRouterImpl.forwardQueryRouteTables();
        
        List connections = tcm.getInitializedConnections();
        Iterator iter = connections.iterator();
        while(iter.hasNext()) {
            TestConnection tc = (TestConnection)iter.next();
            QueryRouteTable qrt = tc.getQueryRouteTable();            
            assertTrue("route tables should match",tcm.runQRPMatch(qrt));
        }
    }


    /**
     * Test to make sure that the method to forward query requests
     * to other hosts is working correctly.
     */
    public void testForwardLimitedQueryToUltrapeers() throws Exception {
        
        Injector injector = createDefaultInjector();
        connectionManager.setNumNewConnections(4);
        connectionManager.resetAndInitialize();
        
        QueryRequestFactory queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
        
        QueryRequest qr = queryRequestFactory.createQuery("test");      
        ReplyHandler rh = testConnectionFactory.createOldConnection(10);
        
        messageRouterImpl.forwardLimitedQueryToUltrapeers(qr, rh);
        assertEquals("unexpected number of queries sent", 15, 
                     connectionManager.getNumUltrapeerQueries());
        assertEquals("unexpected number of queries old sent", 15, 
                     connectionManager.getNumOldConnectionQueries());
        assertEquals("unexpected number of queries new sent", 0, 
                     connectionManager.getNumNewConnectionQueries());
    }


    /**
     * Tests the method for adding query routing entries to the
     * QRP table for this node, adding the leaves' QRP tables if
     * we're an Ultrapeer.
     */
    public void testCreateRouteTable() throws Exception {
        Injector injector = createInjectorAndInitialize(new AbstractModule() {
            @Override
            protected void configure() {
                bind(ConnectionManager.class).to(TestConnectionManager.class);
            }
        });
        
        // use connection manager with default configuration
        TestConnectionManager tcm = (TestConnectionManager)injector.getInstance(ConnectionManager.class);
        tcm.resetAndInitialize();

		QueryRouteTable qrt = messageRouterImpl.createRouteTable(); 
        tcm.runQRPMatch(qrt);
    }


    /**
     * Test to make sure that queries from old connections are forwarded
     * to new connections if only new connections are available.
     */
    public void testForwardOldQueriesToNewConnections() throws Exception {

        Injector injector = createDefaultInjector();
        connectionManager.resetAndInitialize();

        QueryRequestFactory queryRequestFactory = injector.getInstance(QueryRequestFactory.class);

        QueryRequest qr = queryRequestFactory.createQuery("test");      
        ReplyHandler rh = testConnectionFactory.createOldConnection(10);
        
        messageRouterImpl.forwardLimitedQueryToUltrapeers(qr, rh);
        
        assertEquals("unexpected number of queries sent", 15, 
                connectionManager.getNumUltrapeerQueries());
        assertEquals("unexpected number of queries sent", 0, 
                connectionManager.getNumOldConnectionQueries());
        assertEquals("unexpected number of queries sent", 15, 
                connectionManager.getNumNewConnectionQueries());
    }

    /** 
     * Test to make sure we send a query from an old connection to new 
     * connections if new connections are available, but that we
     * still check the routing tables when the connections are new
     * and the query is on the last hop.
     */
    public void testOldConnectionQueryLastHopForwardingToNewConnection() 
        throws Exception {
        // make sure we send a query from an old connection to new 
        // connections if new connections are available.

        Injector injector = createDefaultInjector();
        connectionManager.resetAndInitialize();
        QueryRequestFactory queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
        QueryRequest qr = queryRequestFactory.createQuery("test", (byte)1);      
        ReplyHandler rh = testConnectionFactory.createOldConnection(10);

        messageRouterImpl.forwardLimitedQueryToUltrapeers(qr, rh);
        assertEquals("unexpected number of queries sent", 0, connectionManager.getNumUltrapeerQueries());
        assertEquals("unexpected number of queries sent", 0, 
                     connectionManager.getNumOldConnectionQueries());
        assertEquals("unexpected number of queries sent", 0, 
                     connectionManager.getNumNewConnectionQueries());
    }

    /** 
     * Test to make sure we send a query from an old connection to an
     * old connection even when it's the last hop -- that we ignore
     * last-hop QRP on old connections.
     */
    public void testOldConnectionQueriesIgnoreLastHopQRP() 
        throws Exception {
        // make sure we send a query from an old connection to new 
        // connections if new connections are available.
        
        Injector injector = createDefaultInjector();
        connectionManager.setNumNewConnections(0);
        connectionManager.resetAndInitialize();
        QueryRequestFactory queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
        
        QueryRequest qr = queryRequestFactory.createQuery("test", (byte)1);
        ReplyHandler rh = testConnectionFactory.createOldConnection(10);
        
        messageRouterImpl.forwardLimitedQueryToUltrapeers(qr, rh);
        // make sure we send a query from an old connection to old 
        // connections even when it's the last hop
        assertEquals("unexpected number of queries sent", 15, 
                     connectionManager.getNumUltrapeerQueries());
        assertEquals("unexpected number of queries sent", 15, 
                     connectionManager.getNumOldConnectionQueries());
        assertEquals("unexpected number of queries sent", 0, 
                     connectionManager.getNumNewConnectionQueries());
    }

    /**
     * Test to make sure that the method to forward query requests
     * from new-style (high out-degree) hosts to others is working
     * correctly when only some of the connections are new.
     */
    public void testForwardQueryToUltrapeers() throws Exception {
        Injector injector = createDefaultInjector();
        connectionManager.setNumNewConnections(4);
        connectionManager.resetAndInitialize();
        QueryRequestFactory queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
        
        QueryRequest qr = queryRequestFactory.createQuery("test");
        ReplyHandler rh = testConnectionFactory.createNewConnection(10);
        
        messageRouterImpl.forwardQueryToUltrapeers(qr, rh);
        assertEquals("unexpected number of queries sent", NUM_CONNECTIONS, 
                     connectionManager.getNumUltrapeerQueries());
        assertEquals("unexpected number of queries sent", NUM_CONNECTIONS-4, 
                     connectionManager.getNumOldConnectionQueries());
        assertEquals("unexpected number of queries sent", 4, 
                     connectionManager.getNumNewConnectionQueries());
    }

    /**
     * Test to make sure that the method to forward query requests
     * from new-style (high out-degree) hosts to others is working
     * correctly when all of the connections are new
     */
    public void testForwardQueryToAllNewUltrapeers() throws Exception {
        Injector injector = createDefaultInjector();
        connectionManager.setNumNewConnections(NUM_CONNECTIONS);
        connectionManager.resetAndInitialize();
        QueryRequestFactory queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
        
        QueryRequest qr = queryRequestFactory.createQuery("test");      
        ReplyHandler rh = testConnectionFactory.createNewConnection(10);
        
        messageRouterImpl.forwardQueryToUltrapeers(qr, rh);
        
        assertEquals("unexpected number of queries sent", NUM_CONNECTIONS, 
                     connectionManager.getNumUltrapeerQueries());
        assertEquals("unexpected number of queries sent", 0, 
                     connectionManager.getNumOldConnectionQueries());
        assertEquals("unexpected number of queries sent", NUM_CONNECTIONS, 
                     connectionManager.getNumNewConnectionQueries());
    }

    /**
     * Test to make sure that the method to forward query requests
     * from new-style (high out-degree) hosts to others is working
     * correctly when none of the connections are new.
     */
    public void testForwardQueryToNoNewUltrapeers() throws Exception {
        Injector injector = createDefaultInjector();
        connectionManager.setNumNewConnections(0);
        connectionManager.resetAndInitialize();
        QueryRequestFactory queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
        
        QueryRequest qr = queryRequestFactory.createQuery("test");      
        ReplyHandler rh = testConnectionFactory.createNewConnection(10);
        
        messageRouterImpl.forwardQueryToUltrapeers(qr, rh);
        
        assertEquals("unexpected number of queries sent", NUM_CONNECTIONS, 
                     connectionManager.getNumUltrapeerQueries());
        assertEquals("unexpected number of queries sent", NUM_CONNECTIONS, 
                     connectionManager.getNumOldConnectionQueries());
        assertEquals("unexpected number of queries sent", 0, 
                     connectionManager.getNumNewConnectionQueries());

    }

    /**
     * Test to make sure that the method to forward query requests
     * from new-style (high out-degree) hosts to others is working
     * correctly when the request is on its last hop
     */
    public void testForwardQueryToNewUltrapeersOnLastHop() throws Exception {
        Injector injector = createDefaultInjector();
        connectionManager.setNumNewConnections(NUM_CONNECTIONS);
        connectionManager.resetAndInitialize();
        QueryRequestFactory queryRequestFactory = injector.getInstance(QueryRequestFactory.class);

        QueryRequest qr = queryRequestFactory.createQuery("test", (byte)1);      
        ReplyHandler rh = testConnectionFactory.createNewConnection(10);

        messageRouterImpl.forwardQueryToUltrapeers(qr, rh);

        // the query should not have been sent along any of the connections,
        // as none of them should have Ultrapeer routing tables.
        assertEquals("unexpected number of queries sent", 0, connectionManager.getNumUltrapeerQueries());
        assertEquals("unexpected number of queries sent", 0, 
                     connectionManager.getNumOldConnectionQueries());
        assertEquals("unexpected number of queries sent", 0, 
                     connectionManager.getNumNewConnectionQueries());

    }

    /**
     * Test to make sure that the method to forward query requests
     * from new-style (high out-degree) hosts to others is working
     * correctly when all of the connections are old and the query
     * is on the last hop.
     */
    public void testForwardQueryToOldUltrapeersOnLastHop() throws Exception {
        Injector injector = createDefaultInjector();
        connectionManager.setNumNewConnections(0);
        connectionManager.resetAndInitialize();
        QueryRequestFactory queryRequestFactory = injector.getInstance(QueryRequestFactory.class);

        QueryRequest qr = queryRequestFactory.createQuery("test", (byte)1);      
        ReplyHandler rh = testConnectionFactory.createNewConnection(10);
        
        messageRouterImpl.forwardQueryToUltrapeers(qr, rh);

        // the query should not have been sent along any of the connections,
        // as none of them should have Ultrapeer routing tables.
        assertEquals("unexpected number of queries sent", NUM_CONNECTIONS, 
                     connectionManager.getNumUltrapeerQueries());
        assertEquals("unexpected number of queries sent", NUM_CONNECTIONS, 
                     connectionManager.getNumOldConnectionQueries());
        assertEquals("unexpected number of queries sent", 0, 
                     connectionManager.getNumNewConnectionQueries());
    }

    /**
     * Tests the method for originating queries from leaves.
     */
    public void testOriginateLeafQuery() throws Exception {
        Injector injector = createDefaultInjector();
        // make sure that queries from leaves are simply sent to
        // the first three hosts
        connectionManager.setNumNewConnections(2);
        connectionManager.setUltraPeer(false);
        connectionManager.resetAndInitialize();
        QueryRequestFactory queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
        
        QueryRequest qr = queryRequestFactory.createQuery("test");      

        messageRouterImpl.originateLeafQuery(qr);
        assertEquals("unexpected number of queries sent", 3, 
                     connectionManager.getNumUltrapeerQueries());
        assertEquals("unexpected number of queries sent", 1, 
                    connectionManager.getNumOldConnectionQueries());
        assertEquals("unexpected number of queries sent", 2, 
                     connectionManager.getNumNewConnectionQueries());        

    }
    
    public void testLimeReply() throws Exception {
        
        Injector injector = createInjectorAndInitialize();
        StaticMessages staticMessages = injector.getInstance(StaticMessages.class);
        QueryRequestFactory queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
        ManagedConnectionStubFactory managedConnectionStubFactory = injector.getInstance(ManagedConnectionStubFactory.class);
        
        
        staticMessages.start();
        
        QueryReply limeReply = staticMessages.getLimeReply();
        
        QueryRequest qr = queryRequestFactory.createQuery("limewire pro");
        assertTrue(qr.isQueryForLW());
        
        ManagedConnectionStub mc = managedConnectionStubFactory.createConnectionStub();
        
        messageRouterImpl.handleMessage(qr, mc);
        
        QueryReply sent = mc.replyRef.get();
        // sig & xml payload should be enough
        assertEquals(limeReply.getSecureSignature(),sent.getSecureSignature());
        assertEquals(limeReply.getXMLBytes(), sent.getXMLBytes());
    }
    
    // TODO make this a unit test with mocks, stub out connection manager and connection services if possible
    public void testUDPPingReplies() throws Exception {
        ConnectionSettings.FILTER_CLASS_C.setValue(true);
        
        // to have a valid address and port
        final NetworkManagerStub networkManagerStub = new NetworkManagerStub();
        networkManagerStub.setAddress(new byte[] { (byte) 192, (byte) 168, 0, 1 });
        networkManagerStub.setPort(5555);
        networkManagerStub.setSolicitedGUID(new GUID());
                
        Injector injector = createInjectorAndInitialize(new AbstractModule() {
            @Override
            protected void configure() {
                bind(NetworkManager.class).toInstance(networkManagerStub);
            }
        });
        
        ConnectionManager cm = injector.getInstance(ConnectionManager.class);
        cm.start();
        ConnectionServices connectionServices = injector.getInstance(ConnectionServices.class);
        PingRequestFactory pingRequestFactory = injector.getInstance(PingRequestFactory.class);
        HostCatcher hostCatcher = injector.getInstance(HostCatcher.class);

        final ReplyHandler replyHandler = context.mock(ReplyHandler.class);
        
        // send a PR that doesn't have SCP in it.
        PingRequest pr = pingRequestFactory.createPingRequest((byte)1);
        assertFalse(pr.supportsCachedPongs());
        assertFalse(pr.requestsIP());
        assertFalse(pr.requestsDHTIPP());
        
        InetSocketAddress addr = new InetSocketAddress(InetAddress.getLocalHost(), 1);
        
        context.checking(new Expectations() {{ 
            one(replyHandler).handlePingReply(with(new TypeSafeMatcher<PingReply>() {
                @Override
                public boolean matchesSafely(PingReply reply) {
                    assertEquals(0, reply.getPackedIPPorts().size());
                    assertNull(reply.getMyInetAddress());
                    assertEquals(0, reply.getMyPort());
                    return true;
                }
                public void describeTo(Description description) {
                }
            }), with(aNull(ReplyHandler.class)));
        }});
        
        messageRouterImpl.respondToUDPPingRequest(pr, addr, replyHandler);
        context.assertIsSatisfied();
        
        // send a PR that does have SCP in it.
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(false);
        UltrapeerSettings.DISABLE_ULTRAPEER_MODE.setValue(true);
        
        assertFalse(connectionServices.isSupernode());
        Collection hosts = connectionServices.getPreferencedHosts(false, null,10);
        assertEquals(hosts.toString(), 0, hosts.size());
        pr = pingRequestFactory.createUDPPing();
        assertTrue(pr.supportsCachedPongs());
        assertEquals(0x0, pr.getSupportsCachedPongData()[0] & 0x1);
        assertTrue(pr.requestsIP());
        
        final InetAddress expectedAddress = InetAddress.getLocalHost(); 
        
        context.checking(new Expectations() {{
            one(replyHandler).handlePingReply(with(new TypeSafeMatcher<PingReply>() {
                @Override
                public boolean matchesSafely(PingReply reply) {
                    assertEquals(expectedAddress, reply.getMyInetAddress());
                    assertEquals(1, reply.getMyPort());
                    assertEquals(0, reply.getPackedIPPorts().size());
                    return true;
                }
                public void describeTo(Description description) {
                }
            }), with(aNull(ReplyHandler.class)));
        }});
        
        messageRouterImpl.respondToUDPPingRequest(pr, addr, replyHandler);
        context.assertIsSatisfied();
        
        // add some hosts with free leaf slots (just 3), make sure we get'm back.
        addFreeLeafSlotHosts(hostCatcher, 3); // these 3 will be returned as 1
        addFreeLeafSlotHostsClassB(hostCatcher, 2);
        hosts = connectionServices.getPreferencedHosts(false, null,10);
        assertEquals(hosts.toString(), 3, hosts.size());
        pr = pingRequestFactory.createUDPPing();
        assertTrue(pr.supportsCachedPongs());
        assertEquals(0x0, pr.getSupportsCachedPongData()[0] & 0x1);

        context.checking(new Expectations() {{
            one(replyHandler).handlePingReply(with(new TypeSafeMatcher<PingReply>() {
                @Override
                public boolean matchesSafely(PingReply reply) {
                    assertEquals(3, reply.getPackedIPPorts().size());
                    return true;
                }
                public void describeTo(Description description) {
                }
            }), with(aNull(ReplyHandler.class)));
        }});
        
        messageRouterImpl.respondToUDPPingRequest(pr, addr, replyHandler);
        context.assertIsSatisfied();
        
        // add 20 more free leaf slots, make sure we only get as many we request.
        final int requested = 10;
        addFreeLeafSlotHostsClassB(hostCatcher, 20);
        hosts = connectionServices.getPreferencedHosts(false, null,requested);
        assertEquals(hosts.toString(), requested, hosts.size());
        pr = pingRequestFactory.createUDPPing();
        assertTrue(pr.supportsCachedPongs());
        assertEquals(0x0, pr.getSupportsCachedPongData()[0] & 0x1);
        
        context.checking(new Expectations() {{
            one(replyHandler).handlePingReply(with(new TypeSafeMatcher<PingReply>() {
                @Override
                public boolean matchesSafely(PingReply reply) {
                    assertEquals(requested, reply.getPackedIPPorts().size());
                    return true;
                }
                public void describeTo(Description description) {
                }
            }), with(aNull(ReplyHandler.class)));
        }});
        
        messageRouterImpl.respondToUDPPingRequest(pr, addr, replyHandler);
        context.assertIsSatisfied();
        
        clearFreeLeafSlotHosts(hostCatcher);
        addFreeLeafSlotHostsClassB(hostCatcher, 2); // odd number, to make sure it isn't impacting other tests.
        
        // now test if we're an ultrapeer.
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        UltrapeerSettings.DISABLE_ULTRAPEER_MODE.setValue(false);
        UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(true);
        UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(true);
        assertTrue(connectionServices.isSupernode());
        addFreeUltrapeerSlotHostsClassB(hostCatcher, 4);
        hosts = connectionServices.getPreferencedHosts(true, null,10);
        assertEquals(hosts.toString(), 4, hosts.size());
        pr = pingRequestFactory.createUDPPing();
        assertTrue(pr.supportsCachedPongs());
        assertEquals(0x1, pr.getSupportsCachedPongData()[0] & 0x1);

        context.checking(new Expectations() {{
            one(replyHandler).handlePingReply(with(new TypeSafeMatcher<PingReply>() {
                @Override
                public boolean matchesSafely(PingReply reply) {
                    assertEquals(4, reply.getPackedIPPorts().size());
                    return true;
                }
                public void describeTo(Description description) {
                }
            }), with(aNull(ReplyHandler.class)));
        }});
        
        messageRouterImpl.respondToUDPPingRequest(pr, addr, replyHandler);
        context.assertIsSatisfied();
        
        // and add a lot again, make sure we only get as many we reqeust.
        addFreeUltrapeerSlotHostsClassB(hostCatcher, 20);
        final int requestedIPs = 15;
        int original = ConnectionSettings.NUM_RETURN_PONGS.getValue();
        ConnectionSettings.NUM_RETURN_PONGS.setValue(requestedIPs);
        hosts = connectionServices.getPreferencedHosts(true, null,requestedIPs);
        assertEquals(hosts.toString(), requestedIPs, hosts.size());
        pr = pingRequestFactory.createUDPPing();
        assertTrue(pr.supportsCachedPongs());
        assertEquals(0x1, pr.getSupportsCachedPongData()[0] & 0x1);

        context.checking(new Expectations() {{
            one(replyHandler).handlePingReply(with(new TypeSafeMatcher<PingReply>() {
                @Override
                public boolean matchesSafely(PingReply reply) {
                    assertEquals(requestedIPs, reply.getPackedIPPorts().size());
                    return true;
                }
                public void describeTo(Description description) {
                }
            }), with(aNull(ReplyHandler.class)));
        }});
        
        messageRouterImpl.respondToUDPPingRequest(pr, addr, replyHandler);
        context.assertIsSatisfied();
        
        ConnectionSettings.NUM_RETURN_PONGS.setValue(original);
        
        // Now try again, without an SCP request, and make sure we got none.
        pr = pingRequestFactory.createPingRequest((byte)1);
        assertFalse(pr.supportsCachedPongs());
        assertFalse(pr.requestsIP());
        
        context.checking(new Expectations() {{
            one(replyHandler).handlePingReply(with(new TypeSafeMatcher<PingReply>() {
                @Override
                public boolean matchesSafely(PingReply reply) {
                    assertEquals(0, reply.getPackedIPPorts().size());
                    assertNull(reply.getMyInetAddress());
                    assertEquals(0, reply.getMyPort());
                    return true;
                }
                public void describeTo(Description description) {
                }
            }), with(aNull(ReplyHandler.class)));
        }});
        
        messageRouterImpl.respondToUDPPingRequest(pr, addr, replyHandler);
        context.assertIsSatisfied();
    }
    
    // TODO this is actually StandardMessageRouter test, move it there
    public void testUDPPingReplyWithDHTIPPs() throws Exception{
        // to have a valid address and port
        final NetworkManagerStub networkManagerStub = new NetworkManagerStub();
        networkManagerStub.setAddress(new byte[] { (byte) 192, (byte) 168, 0, 1 });
        networkManagerStub.setPort(5555);
        networkManagerStub.setSolicitedGUID(new GUID());
        
        final TestDHTManager testDHTManager = new TestDHTManager();
        
        Injector injector = createInjectorAndInitialize(new AbstractModule() {
            @Override
            protected void configure() {
                bind(NetworkManager.class).toInstance(networkManagerStub);
                bind(DHTManager.class).toInstance(testDHTManager);
            }
        });
        
        ConnectionManager cm = injector.getInstance(ConnectionManager.class);
        cm.start();
        PingRequestFactory pingRequestFactory = injector.getInstance(PingRequestFactory.class);
        HostCatcher hostCatcher = injector.getInstance(HostCatcher.class);

        final ReplyHandler replyHandler = context.mock(ReplyHandler.class);
        
        //first set up a DHT node and add it to the manager
        //remove any previous data
        File mojitoFile = new File(CommonUtils.getUserSettingsDir(), "mojito.dat");
        mojitoFile.deleteOnExit();
        assertFalse(mojitoFile.exists());
        
        //now start the router service
        ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        DHTSettings.FORCE_DHT_CONNECT.setValue(true);
        
        //create the request
        PingRequest pr = pingRequestFactory.createUDPingWithDHTIPPRequest();
        InetSocketAddress addr = new InetSocketAddress(InetAddress.getLocalHost(), 1);
        assertTrue(pr.requestsDHTIPP());
        assertFalse(pr.supportsCachedPongs());
        assertFalse(pr.requestsIP());

        // test the reply
        
        context.checking(new Expectations() {{
            one(replyHandler).handlePingReply(with(new TypeSafeMatcher<PingReply>() {
                @Override
                public boolean matchesSafely(PingReply reply) {
                    assertEquals(ConnectionSettings.NUM_RETURN_PONGS.getValue(), reply.getPackedDHTIPPorts().size());
                    IpPort ipp = reply.getPackedDHTIPPorts().get(0);
                    assertEquals(3009, ipp.getPort());
                    return true;
                }
                public void describeTo(Description description) {
                }
            }), with(aNull(ReplyHandler.class)));
        }});
        
        messageRouterImpl.respondToUDPPingRequest(pr, addr, replyHandler);
        context.assertIsSatisfied();
        
        //try requesting other IPP too -- this should not change anything
        hostCatcher.clear();
        
        final GUID guid = new GUID();
        final PingRequest pingRequest = context.mock(PingRequest.class);
        
        context.checking(new Expectations() {{
            // stub part 
            allowing(pingRequest).getGUID();
            will(returnValue(guid.bytes()));
            allowing(pingRequest).getLocale();
            will(returnValue("en"));
            allowing(pingRequest).getSupportsCachedPongData();
            will(returnValue(new byte[] { PingRequest.SCP_LEAF } ));
            allowing(pingRequest).requestsIP();
            will(returnValue(false));
            
            // mock part
            one(replyHandler).handlePingReply(with(new TypeSafeMatcher<PingReply>() {
                @Override
                public boolean matchesSafely(PingReply reply) {
                    assertEquals(ConnectionSettings.NUM_RETURN_PONGS.getValue(), reply.getPackedDHTIPPorts().size());
                    assertEquals(0, reply.getPackedIPPorts().size());
                    IpPort ipp = reply.getPackedDHTIPPorts().get(0);
                    assertEquals(3009, ipp.getPort());
                    return true;
                }
                public void describeTo(Description description) {
                }
            }), with(aNull(ReplyHandler.class)));
            
        }});

        messageRouterImpl.respondToUDPPingRequest(pr, addr, replyHandler);
        context.assertIsSatisfied();
        
        // now try adding some gnutella IPPs
        ConnectionSettings.DHT_TO_GNUT_HOSTS_PONG.setValue(60);
        addFreeLeafSlotHosts(hostCatcher, 20); // filtered - class C
        addFreeLeafSlotHostsClassB(hostCatcher, 20);
        final GUID guid2 = new GUID();
        
        context.checking(new Expectations() {{
            // stub part 
            allowing(pingRequest).getGUID();
            will(returnValue(guid2.bytes()));
            allowing(pingRequest).getLocale();
            will(returnValue("en"));
            allowing(pingRequest).getSupportsCachedPongData();
            will(returnValue(new byte[] { PingRequest.SCP_LEAF } ));
            allowing(pingRequest).requestsIP();
            will(returnValue(false));
            allowing(pingRequest).requestsDHTIPP();
            will(returnValue(true));
            
            // mock part
            one(replyHandler).handlePingReply(with(new TypeSafeMatcher<PingReply>() {
                @Override
                public boolean matchesSafely(PingReply reply) {
                    assertEquals(ConnectionSettings.NUM_RETURN_PONGS.getValue(), 
                            reply.getPackedDHTIPPorts().size() + reply.getPackedIPPorts().size());
                    assertEquals(4, reply.getPackedIPPorts().size());
                    assertEquals(6, reply.getPackedDHTIPPorts().size());
                    IpPort ipp = reply.getPackedDHTIPPorts().get(0);
                    assertEquals(3009, ipp.getPort());
                    return true;
                }
                public void describeTo(Description description) {
                }
            }), with(aNull(ReplyHandler.class)));
        }});
        
        messageRouterImpl.respondToUDPPingRequest(pingRequest, addr, replyHandler);
        context.assertIsSatisfied();
    }
    
    public void testHeadPingForwarding() throws Exception {
        createInjectorAndInitialize();
        
    	HeadListener pingee = new HeadListener();
    	
    	GUID clientGUID = new GUID(GUID.makeGuid());
    	
    	//make sure our routing table contains an entry for the pingee
    	RouteTable pushRt = messageRouterImpl.getPushRouteTable();
    	pushRt.routeReply(clientGUID.bytes(),pingee);
    	
    	// try a HeadPing 
    	URN urn = FileDescStub.DEFAULT_SHA1;
    	HeadPing ping = new HeadPing(new GUID(),urn, clientGUID, 0xFF);
    	
    	messageRouterImpl.handleUDPMessage(ping, new InetSocketAddress(InetAddress.getLocalHost(), 10));
    	
    	// the pingee should have received it
    	assertNotNull(pingee._lastSent);
    	assertEquals(pingee._lastSent.getGUID(),ping.getGUID());
    	assertTrue(pingee._lastSent instanceof HeadPing);
    	
    	// we should have an entry in the routing table
    	RouteTable headRt = messageRouterImpl.getHeadPongRouteTable();
    	
    	ReplyHandler r = headRt.getReplyHandler(ping.getGUID());
    	assertEquals(InetAddress.getLocalHost(),InetAddress.getByName(r.getAddress()));
    	assertEquals(10,r.getPort());
    }
    
    public void testHeadPongForwarding() throws Exception {
        
        Injector injector = createInjectorAndInitialize();
        HeadPongFactory headPongFactory = injector.getInstance(HeadPongFactory.class);
        ManagedConnectionStubFactory managedConnectionStubFactory = injector.getInstance(ManagedConnectionStubFactory.class);
        
    	HeadListener pinger = new HeadListener();
    	
    	//make sure our routing table contains an entry for the pinger
    	RouteTable headRt = messageRouterImpl.getHeadPongRouteTable();
    	
    	//try a headpong
    	URN urn = FileDescStub.DEFAULT_SHA1;
    	HeadPing ping = new HeadPing(new GUID(GUID.makeGuid()),urn, 0xFF);
    	headRt.routeReply(ping.getGUID(),pinger);
    	HeadPong pong = headPongFactory.create(ping);
    	
    	messageRouterImpl.handleMessage(pong, managedConnectionStubFactory.createConnectionStub());
    	
    	//the pinger should have gotten the identical object
    	assertNotNull(pinger._lastSent);
    	assertTrue(pinger._lastSent == pong);
    	
    	//the entry in the routing table should be gone
    	ReplyHandler r = headRt.getReplyHandler(ping.getGUID());
    	assertNull(r);
    }
    
    /**
     * Ensures that push requests are forwared to handlers in push route table.
     */
    public void testForwardPushRequestsToHandlersInPushRouteTable() {
        
        Injector injector = LimeTestUtils.createInjector(NetworkManagerStub.MODULE);
        messageRouterImpl = (MessageRouterImpl) injector.getInstance(MessageRouter.class);
        messageRouterImpl.start();
        
        final GUID guid = new GUID();
        PushProxyRequest pushProxyRequest = new PushProxyRequest(guid);
        final RoutedConnection connection = context.mock(RoutedConnection.class);
        
        context.checking(new Expectations() {{
            one(connection).send(with(any(PushProxyAcknowledgement.class)));
            one(connection).setPushProxyFor(true);
            allowing(connection).isOpen();
            will(returnValue(true));
            allowing(connection).isSupernodeClientConnection();
            will(returnValue(true));
        }});
        
        messageRouterImpl.handlePushProxyRequest(pushProxyRequest, connection);
        context.assertIsSatisfied();
        
        final PushRequest pushRequest = new PushRequestImpl(GUID.makeGuid(), (byte)1, guid.bytes(), 500,
                NetworkUtils.toByteAddress(-444000909), 500);
        final ReplyHandler requestor = context.mock(ReplyHandler.class);

        context.checking(new Expectations() {{
            never(requestor).countDroppedMessage();
            one(connection).handlePushRequest(pushRequest, requestor);
        }});
        
        messageRouterImpl.handleMessage(pushRequest, requestor);
        
        context.assertIsSatisfied();
    }
    
    /**
     * Ensures that push requests are forwarded to leaves even if they are not
     * in the push route table. This could be the case if the push request is
     * not due to a previously sent query reply.
     */
    public void testForwardPushRequestToLeavesNotInPushRouteTable() {
        final ConnectionManager connectionManager = context.mock(ConnectionManager.class);
        final GUID guid = new GUID();
        final RoutedConnection leaf = context.mock(RoutedConnection.class);
        
        context.checking(new Expectations() {{
            allowing(connectionManager).addEventListener(with(any(ConnectionLifecycleListener.class)));
            allowing(connectionManager).getInitializedClientConnections();
            will(returnValue(Collections.singletonList(leaf)));
            allowing(connectionManager).isSupernode();
            will(returnValue(true));
            allowing(leaf).isPushProxyFor();
            will(returnValue(true));
            allowing(leaf).getClientGUID();
            will(returnValue(guid.bytes()));
        }});
        
        Injector injector = LimeTestUtils.createInjector(NetworkManagerStub.MODULE, 
                TestUtils.bind(ConnectionManager.class).toInstances(connectionManager));
        messageRouterImpl = (MessageRouterImpl) injector.getInstance(MessageRouter.class);
        messageRouterImpl.start();
        
        final PushRequest pushRequest = new PushRequestImpl(GUID.makeGuid(), (byte)1, guid.bytes(), 500,
                NetworkUtils.toByteAddress(-444000909), 500);
        final ReplyHandler requestor = context.mock(ReplyHandler.class);

        context.checking(new Expectations() {{
            never(requestor).countDroppedMessage();
            one(leaf).handlePushRequest(pushRequest, requestor);
        }});
        
        messageRouterImpl.handleMessage(pushRequest, requestor);
        
        context.assertIsSatisfied();
    }
    
    @SuppressWarnings("unchecked")
    private void addFreeLeafSlotHosts(HostCatcher hostCatcher, int num) throws Exception {
        Map<ExtendedEndpoint, ExtendedEndpoint> set = (Map)PrivilegedAccessor.getValue(hostCatcher, "FREE_LEAF_SLOTS_SET");
        for(int i = 0; i < num; i++) {
            ExtendedEndpoint e =new ExtendedEndpoint("1.2.3." + i, i+1);
            set.put(e, e);
        }
    }
    
    @SuppressWarnings("unchecked")
    private void addFreeLeafSlotHostsClassB(HostCatcher hostCatcher, int num) throws Exception {
        Map<ExtendedEndpoint, ExtendedEndpoint> set = (Map)PrivilegedAccessor.getValue(hostCatcher, "FREE_LEAF_SLOTS_SET");
        for(int i = 0; i < num; i++) {
            ExtendedEndpoint e = new ExtendedEndpoint("1.2." + i+".3", i+1);
           set.put(e, e);
        }
    }
    
    @SuppressWarnings("unchecked")
    private void addFreeUltrapeerSlotHostsClassB(HostCatcher hostCatcher, int num) throws Exception {
        Map<ExtendedEndpoint, ExtendedEndpoint> set = (Map)PrivilegedAccessor.getValue(hostCatcher, "FREE_ULTRAPEER_SLOTS_SET");
        for(int i = 0; i < num; i++) {
            ExtendedEndpoint e = new ExtendedEndpoint("1.2." + i+".3", i+1);
           set.put(e, e);
        }
    }
    
    private void clearFreeLeafSlotHosts(HostCatcher hostCatcher) throws Exception {
        Map set = (Map)PrivilegedAccessor.getValue(hostCatcher, "FREE_ULTRAPEER_SLOTS_SET");
        set.clear();
    }        
    
    @Singleton
    private static class TestQRPUpdater extends QRPUpdater {

        private final List<String> KEYWORDS = 
            Arrays.asList(MY_KEYWORDS);
        
        @Inject
        public TestQRPUpdater(FileManager fileManager, 
                @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
                ListenerSupport<FileDescChangeEvent> fileDescListenerSupport) {
            super(fileManager, backgroundExecutor, fileDescListenerSupport);
        }
        
        @Override
        public synchronized QueryRouteTable getQRT() {
            QueryRouteTable qrt = super.getQRT();
            for(String s : KEYWORDS ) {
                qrt.add(s);
            }
            return qrt;
        }
    }
     
    private static class HeadListener extends ReplyHandlerStub {
    	Message _lastSent;
    	@Override
        public void reply(Message m) {
    		_lastSent = m;
    	}
    }
    
    @Singleton
    private static class ManagedConnectionStubFactory {

        private final Provider<ConnectionManager> connectionManager;

        private final NetworkManager networkManager;

        private final QueryRequestFactory queryRequestFactory;

        private final HeadersFactory headersFactory;

        private final HandshakeResponderFactory handshakeResponderFactory;

        private final QueryReplyFactory queryReplyFactory;

        private final Provider<MessageDispatcher> messageDispatcher;

        private final Provider<NetworkUpdateSanityChecker> networkUpdateSanityChecker;

        private final Provider<SearchResultHandler> searchResultHandler;

        private final CapabilitiesVMFactory capabilitiesVMFactory;

        private final Provider<SocketsManager> socketsManager;

        private final Provider<Acceptor> acceptor;

        private final MessagesSupportedVendorMessage supportedVendorMessage;

        private final Provider<SimppManager> simppManager;

        private final Provider<UpdateHandler> updateHandler;

        private final Provider<ConnectionServices> connectionServices;

        private final GuidMapManager guidMapManager;

        private final SpamFilterFactory spamFilterFactory;

        private final MessageFactory messageFactory;

        private final MessageReaderFactory messageReaderFactory;

        private final ApplicationServices applicationServices;
        
        private final Provider<SecureMessageVerifier> secureMessageVerifier;
        
        private final NetworkInstanceUtils networkInstanceUtils;

        @Inject
        public ManagedConnectionStubFactory(Provider<ConnectionManager> connectionManager,
                NetworkManager networkManager, QueryRequestFactory queryRequestFactory,
                HeadersFactory headersFactory, HandshakeResponderFactory handshakeResponderFactory,
                QueryReplyFactory queryReplyFactory, Provider<MessageDispatcher> messageDispatcher,
                Provider<NetworkUpdateSanityChecker> networkUpdateSanityChecker,
                Provider<SearchResultHandler> searchResultHandler,
                CapabilitiesVMFactory capabilitiesVMFactory, Provider<SocketsManager> socketsManager,
                Provider<Acceptor> acceptor, MessagesSupportedVendorMessage supportedVendorMessage,
                Provider<SimppManager> simppManager, Provider<UpdateHandler> updateHandler,
                Provider<ConnectionServices> connectionServices, GuidMapManager guidMapManager,
                SpamFilterFactory spamFilterFactory, MessageFactory messageFactory,
                MessageReaderFactory messageReaderFactory, ApplicationServices applicationServices,
                Provider<SecureMessageVerifier> secureMessageVerifier, NetworkInstanceUtils networkInstanceUtils) {
            this.connectionManager = connectionManager;
            this.networkManager = networkManager;
            this.queryRequestFactory = queryRequestFactory;
            this.headersFactory = headersFactory;
            this.handshakeResponderFactory = handshakeResponderFactory;
            this.queryReplyFactory = queryReplyFactory;
            this.messageDispatcher = messageDispatcher;
            this.networkUpdateSanityChecker = networkUpdateSanityChecker;
            this.applicationServices = applicationServices;
            this.searchResultHandler = searchResultHandler;
            this.capabilitiesVMFactory = capabilitiesVMFactory;
            this.socketsManager = socketsManager;
            this.acceptor = acceptor;
            this.supportedVendorMessage = supportedVendorMessage;
            this.simppManager = simppManager;
            this.updateHandler = updateHandler;
            this.connectionServices = connectionServices;
            this.guidMapManager = guidMapManager;
            this.spamFilterFactory = spamFilterFactory;
            this.messageFactory = messageFactory;
            this.messageReaderFactory = messageReaderFactory;
            this.secureMessageVerifier = secureMessageVerifier;
            this.networkInstanceUtils = networkInstanceUtils;
        }

        
        public ManagedConnectionStub createConnectionStub() {
            return new ManagedConnectionStub("1.2.3.4", 6346, ConnectType.PLAIN, connectionManager.get(), networkManager,
                    queryRequestFactory, headersFactory, handshakeResponderFactory, queryReplyFactory,
                    messageDispatcher.get(), networkUpdateSanityChecker.get(), searchResultHandler
                            .get(), capabilitiesVMFactory, socketsManager.get(), acceptor.get(),
                    supportedVendorMessage, simppManager, updateHandler, connectionServices,
                    guidMapManager, spamFilterFactory, messageReaderFactory, messageFactory,
                    applicationServices, secureMessageVerifier.get(), networkInstanceUtils);
        }
        
    }
    
    private static class ManagedConnectionStub extends GnutellaConnection {
        
        final AtomicReference<QueryReply> replyRef = new AtomicReference<QueryReply>(null);

        public ManagedConnectionStub(String host, int port, ConnectType type,
                ConnectionManager connectionManager, NetworkManager networkManager,
                QueryRequestFactory queryRequestFactory, HeadersFactory headersFactory,
                HandshakeResponderFactory handshakeResponderFactory,
                QueryReplyFactory queryReplyFactory, MessageDispatcher messageDispatcher,
                NetworkUpdateSanityChecker networkUpdateSanityChecker,
                SearchResultHandler searchResultHandler,
                CapabilitiesVMFactory capabilitiesVMFactory, SocketsManager socketsManager,
                Acceptor acceptor, MessagesSupportedVendorMessage supportedVendorMessage,
                Provider<SimppManager> simppManager, Provider<UpdateHandler> updateHandler,
                Provider<ConnectionServices> connectionServices, GuidMapManager guidMapManager,
                SpamFilterFactory spamFilterFactory, MessageReaderFactory messageReaderFactory,
                MessageFactory messageFactory, ApplicationServices applicationServices,
                SecureMessageVerifier secureMessageVerifier, NetworkInstanceUtils networkInstanceUtils) {
            super(host, port, type, connectionManager, networkManager, queryRequestFactory, headersFactory,
                    handshakeResponderFactory, queryReplyFactory, messageDispatcher,
                    networkUpdateSanityChecker, searchResultHandler, capabilitiesVMFactory, socketsManager,
                    acceptor, supportedVendorMessage, simppManager, updateHandler, connectionServices,
                    guidMapManager, spamFilterFactory, messageReaderFactory, messageFactory,
                    applicationServices, secureMessageVerifier, null, networkInstanceUtils);
            
        }
        
        @Override
        public void initialize(GnetConnectObserver observer) throws IOException, NoGnutellaOkException, BadHandshakeException {
        }
        
        @Override
        public void handleQueryReply(QueryReply queryReply, ReplyHandler receivingConnection) {
            replyRef.set(queryReply);
        }
        
    }
    
    private static class TestDHTManager implements DHTManager {

        public List<IpPort> getActiveDHTNodes(int maxNodes){
            LinkedList<IpPort> ipps = new LinkedList<IpPort>();
            for(int i = 0; i < maxNodes; i++) {
                IpPort ipp;
                try {
                    ipp = new IpPortImpl("localhost", 3000+i);
                    ipps.addFirst(ipp);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }
            return ipps;
        }

        public void addActiveDHTNode(SocketAddress hostAddress) {}
        
        public void addPassiveDHTNode(SocketAddress hostAddress) {}

        public void addressChanged() {}
        
        public boolean isWaitingForNodes() {
            return false;
        }

        public MojitoDHT getMojitoDHT() { return null; }

        public DHTMode getDHTMode() { 
            return DHTMode.INACTIVE; 
        }

        public boolean isRunning() { 
            return true; 
        }

        public void stop() {}

        public void start(DHTMode mode) {}
        
        public boolean isBootstrapped() {
            return false;
        }

        public boolean isMemberOfDHT() {
            return isRunning() && isBootstrapped();
        }

        public void handleConnectionLifecycleEvent(ConnectionLifecycleEvent evt) {}
        
        public Vendor getVendor() {
            return Vendor.UNKNOWN;
        }
        
        public Version getVersion() {
            return Version.ZERO;
        }

        public void addEventListener(DHTEventListener listener) {
        }

        public void dispatchEvent(DHTEvent event) {
        }

        public void removeEventListener(DHTEventListener listener) {
        }

        public void handleDHTContactsMessage(DHTContactsMessage msg) {
        }

        public boolean isEnabled() {
            return true;
        }

        public void setEnabled(boolean enabled) {
        }
        
        public DHTFuture<FindValueResult> get(EntityKey eKey) {
            return null;
        }
        
        public DHTFuture<StoreResult> put(KUID key, DHTValue value) {
            return null;
        }
    }

}
