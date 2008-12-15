package com.limegroup.gnutella;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.FilterSettings;
import org.limewire.core.settings.NetworkSettings;
import org.limewire.core.settings.UltrapeerSettings;
import org.limewire.io.GUID;
import org.limewire.net.SocketsManager.ConnectType;
import org.limewire.util.TestUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.limegroup.gnutella.connection.BlockingConnection;
import com.limegroup.gnutella.connection.BlockingConnectionFactory;
import com.limegroup.gnutella.handshaking.HandshakeResponder;
import com.limegroup.gnutella.handshaking.HandshakeResponse;
import com.limegroup.gnutella.handshaking.HeaderNames;
import com.limegroup.gnutella.handshaking.HeadersFactory;
import com.limegroup.gnutella.helpers.UrnHelper;
import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.library.UrnCache;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingReplyFactory;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.stubs.NetworkManagerStub;
import com.limegroup.gnutella.util.EmptyResponder;
import com.limegroup.gnutella.util.LimeTestCase;

/**
 * Checks whether (multi)leaves avoid forwarding messages to ultrapeers, do
 * redirects properly, etc.  The test includes a leaf attached to 3 
 * Ultrapeers.
 */
public class LeafRoutingTest extends LimeTestCase {
    private static final int SERVER_PORT = 6669;
    private static final int TIMEOUT=5000;
    private static final byte[] ultrapeerIP=
        new byte[] {(byte)18, (byte)239, (byte)0, (byte)144};
    private static final byte[] oldIP=
        new byte[] {(byte)111, (byte)22, (byte)33, (byte)44};

    private BlockingConnection ultrapeer1;
    private BlockingConnection ultrapeer2;
    private BlockingConnection old1;

    private LifecycleManager lifecycleManager;
    private ConnectionServices connectionServices;
    private BlockingConnectionFactory connectionFactory;
    private PingReplyFactory pingReplyFactory;
    private SearchServices searchServices;
    private FileManager fileManager;
    private QueryRequestFactory queryRequestFactory;
    private SpamServices spamServices;
    private UrnCache urnCache;
    private HeadersFactory headersFactory;
    
    public LeafRoutingTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(LeafRoutingTest.class);
    }    

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    private static void doSettings() {
        //Setup LimeWire backend.  For testing other vendors, you can skip all
        //this and manually configure a client in leaf mode to listen on port
        //6669, with no slots and no connections.  But you need to re-enable
        //the interactive prompts below.
        NetworkSettings.PORT.setValue(SERVER_PORT);
		ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
		ConnectionSettings.DO_NOT_BOOTSTRAP.setValue(true);
		UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(false);
		UltrapeerSettings.DISABLE_ULTRAPEER_MODE.setValue(true);
		UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(false);
		ConnectionSettings.NUM_CONNECTIONS.setValue(0);
		ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
    }        
    
    @Override
    public void setUp() throws Exception  {
        doSettings();
        assertEquals("unexpected port", SERVER_PORT, NetworkSettings.PORT.getValue());
        
        final NetworkManagerStub networkManager = new NetworkManagerStub();
        networkManager.setAddress(new byte[] { 127, 0, 0, 1 });
        networkManager.setPort(5454);
        networkManager.setAcceptedIncomingConnection(true);
        networkManager.setSolicitedGUID(new GUID());
        Injector injector = LimeTestUtils.createInjector(Stage.PRODUCTION, new AbstractModule() {
            @Override
            protected void configure() {
                bind(NetworkManager.class).toInstance(networkManager);
            }
        });
        lifecycleManager = injector.getInstance(LifecycleManager.class);
        connectionServices = injector.getInstance(ConnectionServices.class);
        connectionFactory = injector.getInstance(BlockingConnectionFactory.class);
        pingReplyFactory = injector.getInstance(PingReplyFactory.class);
        searchServices = injector.getInstance(SearchServices.class);
        fileManager = injector.getInstance(FileManager.class);
        queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
        spamServices = injector.getInstance(SpamServices.class);
        urnCache = injector.getInstance(UrnCache.class);
        headersFactory = injector.getInstance(HeadersFactory.class);
        
        lifecycleManager.start();
        connectionServices.connect();

        // get the resource file for com/limegroup/gnutella
        File berkeley = TestUtils.getResourceFile("com/limegroup/gnutella/berkeley.txt");
        File susheel = TestUtils.getResourceFile("com/limegroup/gnutella/susheel.txt");
        assertNotNull(fileManager.getGnutellaFileList().add(berkeley).get(1, TimeUnit.SECONDS));
        assertNotNull(fileManager.getGnutellaFileList().add(susheel).get(1, TimeUnit.SECONDS));
        
        assertEquals("unexpected port", SERVER_PORT, NetworkSettings.PORT.getValue());
        connect();
    }
    
    @Override
    public void tearDown() throws Exception {
        shutdown();
        if (lifecycleManager != null) {
            lifecycleManager.shutdown();
        }
    }

     ////////////////////////// Initialization ////////////////////////

     private void connect() throws Exception {
         ultrapeer1 = connect(6350, true);
         ultrapeer2 = connect(6351, true);
         old1 = connect(6352, true);
     }
     
    private BlockingConnection connect(int port, boolean ultrapeer) 
        throws Exception {
         ServerSocket ss=new ServerSocket(port);
         connectionServices.connectToHostAsynchronously("127.0.0.1", port, ConnectType.PLAIN);
         Socket socket = ss.accept();
         ss.close();
         
         socket.setSoTimeout(3000);
         InputStream in=socket.getInputStream();
         String word=readWord(in);
         if (! word.equals("GNUTELLA"))
             throw new IOException("Bad word: "+word);
         
         HandshakeResponder responder;
         if (ultrapeer) {
            responder = new UltrapeerResponder();
         } else {
             responder = new EmptyResponder();
         }
         
         BlockingConnection con = connectionFactory.createConnection(socket);
         con.initialize(null, responder, 1000);
         replyToPing(con, ultrapeer);
         return con;
     }
     
     /**
      * Acceptor.readWord
      *
      * @modifies sock
      * @effects Returns the first word (i.e., no whitespace) of less
      *  than 8 characters read from sock, or throws IOException if none
      *  found.
      */
     private static String readWord(InputStream sock) throws IOException {
         final int N=9;  //number of characters to look at
         char[] buf=new char[N];
         for (int i=0 ; i<N ; i++) {
             int got=sock.read();
             if (got==-1)  //EOF
                 throw new IOException();
             if ((char)got==' ') { //got word.  Exclude space.
                 return new String(buf,0,i);
             }
             buf[i]=(char)got;
         }
         throw new IOException();
     }

	private void replyToPing(BlockingConnection c, boolean ultrapeer) 
        throws Exception {
        // respond to a ping iff one is given.
        Message m = null;
        byte[] guid;
        try {
            while (!(m instanceof PingRequest)) {
                m = c.receive(500);
            }
            guid = ((PingRequest)m).getGUID();            
        } catch(InterruptedIOException iioe) {
            //nothing's coming, send a fake pong anyway.
            guid = new GUID().bytes();
        }
        
        Socket socket = c.getSocket();
        PingReply reply = 
            pingReplyFactory.createExternal(guid, (byte)7,
                                     socket.getLocalPort(), 
                                     ultrapeer ? ultrapeerIP : oldIP,
                                     ultrapeer);
         reply.hop();
         c.send(reply);
         c.flush();
     }

     ///////////////////////// Actual Tests ////////////////////////////

    public void testLeafBroadcast() 
            throws IOException, BadPacketException {
        byte[] guid = searchServices.newQueryGUID();
        searchServices.query(guid, "crap");

        while (true) {
            assertNotNull("ultrapeer1 is null", ultrapeer1);
            Message m=ultrapeer1.receive(2000);
            if (m instanceof QueryRequest) {
                assertEquals("unexpected query name", "crap", 
                             ((QueryRequest)m).getQuery());
                break;
            }
        }       
        while (true) {
            Message m=ultrapeer2.receive(2000);
            if (m instanceof QueryRequest) {
                assertEquals("unexpected query name", "crap", 
                             ((QueryRequest)m).getQuery());
                break;
            }
        }
        while (true) {
            Message m=old1.receive(2000);
            if (m instanceof QueryRequest) {
                assertEquals("unexpected query name", "crap", 
                             ((QueryRequest)m).getQuery());
                break;
            }
        }
//          while (true) {
//              Message m=old2.receive(2000);
//              if (m instanceof QueryRequest) {
//                  assertEquals("unexpected query name", "crap", 
//                               ((QueryRequest)m).getQuery());
//                  break;
//              }
//          }
    }

    /**
     * Tests that the X-Try and X-Try-Ultrapeer headers are correctly
     * being transferred in connection headers.
     */
    public void testRedirect() throws Exception {
        Properties props = new Properties();
        
        props.put(HeaderNames.X_ULTRAPEER, "True");
        //props.put(HeaderNames.X_PROBE_QUERIES, PROBE_VERSION);
        BlockingConnection c = connectionFactory.createConnection("127.0.0.1", SERVER_PORT);

        try {
            c.initialize(props, new EmptyResponder(), 1000);
            fail("handshake should not have succeeded");
        } catch (IOException e) {
            // THESE VALUES WERE POPULATED DURING THE PING/PONG EXCHANGE
            // WHEN THE CONNECTIONS WERE CREATED.
            
            String hosts = c.getConnectionCapabilities().getHeadersRead().getProperty(HeaderNames.X_TRY_ULTRAPEERS);
            //System.out.println("X-Try-Ultrapeers: "+hosts);
            assertNotNull("unexpected null value", hosts);
            Set s = list2set(hosts);
            assertEquals("unexpected size of X-Try-Ultrapeers list hosts: "+
                         hosts, 3, s.size());
            byte[] localhost=new byte[] {(byte)127, (byte)0, (byte)0, (byte)1};
            // because we used 'createExternal' to create the external pongs we
            // did not mark them with slot info.  we shoud do that at some point
            // to test this.
            assertContains("expected Ultrapeer not present in list",
                       s, new Endpoint(localhost, 6350));
            assertContains("expected Ultrapeer not present in list",
                       s, new Endpoint(localhost, 6351));
            assertContains("expected Ultrapeer not present in list",
                       s, new Endpoint(localhost, 6352));

            //assertTrue("expected Ultrapeer not present in list",
            //           s.contains(new Endpoint(localhost, 6350))); 
            //assertTrue("expected Ultrapeer not present in list",
            //           s.contains(new Endpoint(localhost, 6351)));
            //assertTrue("expected Ultrapeer not present in list",
            //           s.contains(new Endpoint(localhost, 6352))); 
            //assertTrue("expected Ultrapeer not present in list",
            //           s.contains(new Endpoint(localhost, 6353)));

        }
    }


		/*
    private void doBroadcastFromUltrapeer() throws IOException {
        debug("-Test query from ultrapeer not broadcasted");
        drain(ultrapeer2);
        drain(old1);
        drain(old2);

		//QueryRequest qr = QueryRequest.createQuery("crap", (byte)7);
		QueryRequest qr = QueryRequest.createNonFirewalledQuery("crap");
        ultrapeer1.send(qr);
        ultrapeer1.flush();

        assertTrue("drain should have returned false", !drain(ultrapeer2));
        //We don't care whether this is forwarded to the old connections
    }
		*/

    /*
    private static void doNoBroadcastFromOld() 
        throws IOException, BadPacketException {
        debug("-Test query from old not broadcasted");
        drain(ultrapeer1);
        drain(ultrapeer2);
        drain(old2);

        QueryRequest qr=new QueryRequest((byte)7, 0, "crap", false);
        old1.send(qr);
        old1.flush();

        assertTrue(! drain(ultrapeer1));
        assertTrue(! drain(ultrapeer2));
        Message m=old2.receive(500);
        assertTrue(((QueryRequest)m).getQuery().equals("crap"));
        assertEquals("unexpected hops", (byte)1, m.getHops()); 
        // we adjust all TTLs down to 6....
		assertEquals("unexpected TTL", (byte)5, m.getTTL());        
    }
    */

    /**
     * Tests to make sure that connections to old hosts are not allowed
     */
    public void testConnectionToOldDisallowed() {
        BlockingConnection c= connectionFactory.createConnection("127.0.0.1", SERVER_PORT);
        try {
            c.initialize(new Properties(), new EmptyResponder(), 1000);
            fail("handshake should not have succeeded");
        } catch (IOException e) {
        }        
    }


    public void testLeafAnswersQueries() throws Exception {
        BlockingConnectionUtils.drain(ultrapeer2);

        // make sure the set up succeeded
        assertTrue(fileManager.getGnutellaFileList().size() == 2);

        // send a query that should hit
        QueryRequest query = queryRequestFactory.createQueryRequest(GUID.makeGuid(), (byte) 1,
                "berkeley", null, null, null, false, Network.UNKNOWN, false, 0);
        ultrapeer2.send(query);
        ultrapeer2.flush();
        
        // hope for the result
        Message m = null;
        do {
            m = ultrapeer2.receive(TIMEOUT);
        } while (!(m instanceof QueryReply)) ;
        
    }


    public void testLeafAnswersURNQueries() throws Exception {
        FilterSettings.FILTER_HASH_QUERIES.setValue(false);
        spamServices.adjustSpamFilters();
        URNtest();
    }
    
    public void testLeafFiltersURNQueries() throws Exception {
        FilterSettings.FILTER_HASH_QUERIES.setValue(true);
        spamServices.adjustSpamFilters();
        try {
            URNtest();
            fail("did not filter URN query");
        }catch(IOException expected){};
    }
    
    private void URNtest() throws Exception {
        BlockingConnectionUtils.drain(ultrapeer2);

        // make sure the set up succeeded
        assertEquals(2, fileManager.getGnutellaFileList().size());

        // get the URNS for the files
        File berkeley = 
            TestUtils.getResourceFile("com/limegroup/gnutella/berkeley.txt");
        File susheel = 
            TestUtils.getResourceFile("com/limegroup/gnutella/susheel.txt");
        Iterator iter = UrnHelper.calculateAndCacheURN(berkeley, urnCache).iterator();
        URN berkeleyURN = (URN) iter.next();
        while (!berkeleyURN.isSHA1())
            berkeleyURN = (URN) iter.next();
        iter = UrnHelper.calculateAndCacheURN(susheel, urnCache).iterator();
        URN susheelURN = (URN) iter.next();
        while (!susheelURN.isSHA1())
            susheelURN = (URN) iter.next();

        // send a query that should hit
        QueryRequest query = queryRequestFactory.createQuery(berkeleyURN);

        ultrapeer2.send(query);
        ultrapeer2.flush();
        
        // hope for the result
        Message m = null;
        do {
            m = ultrapeer2.receive(TIMEOUT);
            if (m instanceof QueryReply) {
                QueryReply qr = (QueryReply) m;
                iter = qr.getResults();
                Response first = (Response) iter.next();
                assertTrue(UrnHelper.calculateAndCacheURN(berkeley, urnCache).containsAll(first.getUrns()));
            }
        } while (!(m instanceof QueryReply)) ;
        
        // send another query that should hit
        query = queryRequestFactory.createQuery(susheelURN);

        ultrapeer2.send(query);
        ultrapeer2.flush();
        
        // hope for the result
        m = null;
        do {
            m = ultrapeer2.receive(TIMEOUT);
            if (m instanceof QueryReply) {
                QueryReply qr = (QueryReply) m;
                iter = qr.getResults();
                Response first = (Response) iter.next();
                assertTrue(UrnHelper.calculateAndCacheURN(susheel, urnCache).containsAll(first.getUrns()));
            }
        } while (!(m instanceof QueryReply)) ;
    }


    /** Converts the given X-Try[-Ultrapeer] header value to
     *  a Set of Endpoints. */
    private Set /* of Endpoint */ list2set(String addresses) throws Exception {
        Set<Endpoint> ret=new HashSet<Endpoint>();
        StringTokenizer st = new StringTokenizer(addresses,
            Constants.ENTRY_SEPARATOR);
        while(st.hasMoreTokens()){
            //get an address
            String address = st.nextToken().trim();
            ret.add(new Endpoint(address));
        }
        return ret;
    }

    private void shutdown() throws IOException {
        //System.out.println("\nShutting down.");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) { }
        ultrapeer1.close();
        ultrapeer2.close();
        old1.close();
    }

    private class UltrapeerResponder implements HandshakeResponder {
        public HandshakeResponse respond(HandshakeResponse response, 
                boolean outgoing) {
            Properties props = headersFactory.createUltrapeerHeaders("127.0.0.1"); 
            props.put(HeaderNames.X_DEGREE, "42");           
            return HandshakeResponse.createResponse(props);
        }
        
        public void setLocalePreferencing(boolean b) {}
    }}
