package com.limegroup.gnutella;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.FilterSettings;
import org.limewire.core.settings.NetworkSettings;
import org.limewire.core.settings.SearchSettings;
import org.limewire.core.settings.UltrapeerSettings;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.io.GUID;
import org.limewire.io.IOUtils;
import org.limewire.net.SocketsManager.ConnectType;
import org.limewire.util.TestUtils;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.limegroup.gnutella.connection.BlockingConnection;
import com.limegroup.gnutella.connection.BlockingConnectionFactory;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;
import com.limegroup.gnutella.handshaking.HandshakeResponder;
import com.limegroup.gnutella.handshaking.HandshakeResponse;
import com.limegroup.gnutella.handshaking.HeaderNames;
import com.limegroup.gnutella.handshaking.HeadersFactory;
import com.limegroup.gnutella.handshaking.NoGnutellaOkException;
import com.limegroup.gnutella.library.FileCollection;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.FileView;
import com.limegroup.gnutella.library.GnutellaFiles;
import com.limegroup.gnutella.library.Library;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingReplyFactory;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.spam.SpamManager;

/**
 * Sets up a Test Scenario of a Leaf connected to some Ultrapeers (default of
 * 4, use {@link #getNumberOfPeers()} to override that number).
 * 
 * Subclasses need to call {@link #setUp(Injector)} and pass in their configured
 * injector.
 * 
 * The leaf has the following settings (which you can override by implementing
 * your own setSettings()): runs on SERVER_PORT, is a Leaf, does not connect on
 * startup, Watchdog are inactive, sharing only txt files, 
 * sharing two txt files (berkeley and susheel), and accepting all search
 * results.
 * and {@link #getNumberOfPeers()} (for the number of Ultrapeers to connect to, must be 1-4), 
 * and main and suite().
 */
public abstract class ClientSideTestCase extends LimeTestCase {
    
    public final int SERVER_PORT = 6669;
    protected int TIMEOUT=500;
    private final byte[] ultrapeerIP=
        new byte[] {(byte)18, (byte)239, (byte)0, (byte)144};
    private final byte[] oldIP=
        new byte[] {(byte)111, (byte)22, (byte)33, (byte)44};


    protected BlockingConnection testUP[];

    @Inject protected LifecycleManager lifecycleManager;
    @Inject protected ConnectionServices connectionServices;
    @Inject private ConnectionManager connectionManager;
    @Inject protected Injector injector;
    @Inject private PingReplyFactory pingReplyFactory;
    @Inject private BlockingConnectionFactory connectionFactory;
    @Inject private SpamManager spamManager;
    @Inject private HeadersFactory headersFactory;
    @Inject protected Library library;
    @Inject @GnutellaFiles protected FileView gnutellaFileView;
    @Inject @GnutellaFiles protected FileCollection gnutellaFileCollection;
    protected FileDesc berkeleyFD;
    protected FileDesc susheelFD;
    
    public ClientSideTestCase(String name) {
        super(name);
    }
    
    public final void doSettings() throws Exception {
        String localIP = InetAddress.getLocalHost().getHostAddress();
        FilterSettings.BLACK_LISTED_IP_ADDRESSES.set(
            new String[] {"*.*.*.*"});
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.set(
            new String[] {"127.*.*.*", "192.168.*.*", "10.254.*.*", localIP});        
        //Setup LimeWire backend.  For testing other vendors, you can skip all
        //this and manually configure a client in leaf mode to listen on port
        //6669, with no slots and no connections.  But you need to re-enable
        //the interactive prompts below.
        NetworkSettings.PORT.setValue(SERVER_PORT);
		ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
		UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(false);
		UltrapeerSettings.DISABLE_ULTRAPEER_MODE.setValue(true);
		UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(false);
		ConnectionSettings.NUM_CONNECTIONS.setValue(0);
		ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
		ConnectionSettings.WATCHDOG_ACTIVE.setValue(false);
        // make sure results get through
        SearchSettings.MINIMUM_SEARCH_QUALITY.setValue(-2);
        setSettings();
    }        
    
    /**
     * Can be overridden by subclasses and is called from {@link #setUp(Injector)}.
     * @throws Exception 
     */
    public void setSettings() throws Exception {
    }
    
    /**
     * Can be overriden in subclasses that need a different number of ultrapeers.
     * 
     * Must be >= 1 && <= 4.
     * 
     * @return the number ultrapeers that should be instantiated, by default 4
     */
    public int getNumberOfPeers() {
        return 4;
    }
    
    /**
     * Can be overridden. Returns <code>true</code> by default.
     */
    protected boolean shouldRespondToPing() {
        return true;
    }
    
    @Override
    protected void setUp() throws Exception {
        this.setUp(LimeTestUtils.createInjector(Stage.PRODUCTION));
    }
    
    public void setUp(Injector injector) throws Exception {
        this.injector = injector;
        doSettings();
        
        assertEquals("unexpected port", SERVER_PORT, NetworkSettings.PORT.getValue());
        
        injector.injectMembers(this);

        lifecycleManager.start();
        connectionServices.connect();
        
        Future<FileDesc> f1 = gnutellaFileCollection.add(
                TestUtils.getResourceFile("com/limegroup/gnutella/berkeley.txt"));
        Future<FileDesc> f2 = gnutellaFileCollection.add(
                TestUtils.getResourceFile("com/limegroup/gnutella/susheel.txt"));
        
        berkeleyFD = f1.get(1, TimeUnit.SECONDS);
        susheelFD = f2.get(1, TimeUnit.SECONDS);
        assertNotNull(berkeleyFD);
        assertNotNull(susheelFD);
        
        assertEquals("unexpected port", SERVER_PORT, NetworkSettings.PORT.getValue());
        int numUPs = getNumberOfPeers();
        
        if (numUPs < 1 || numUPs > 4)
            throw new IllegalArgumentException("Bad value for numUPs!!!");
        testUP = new BlockingConnection[numUPs];
        for (int i = 0; i < testUP.length; i++) {
            try {
                testUP[i] = connect(6355+i, true);
            } catch(NoGnutellaOkException ngoke) {
                fail("couldn't connect ultrapeer: " + (i+1) +
                     ", preferred is: " + 
                     connectionManager.getPreferredConnectionCount(),
                     ngoke);
            }
        }
    }        
    
    @Override
    protected void tearDown() throws Exception {
        if (lifecycleManager != null) {
            lifecycleManager.shutdown();
        }
        if (connectionServices != null) {
            connectionServices.disconnect();
        }
        for (BlockingConnection c : testUP)
            c.close();
    }
    
     ////////////////////////// Initialization ////////////////////////


     private BlockingConnection connect(int port, boolean ultrapeer)
         throws IOException, BadPacketException, Exception {
         ServerSocket ss=new ServerSocket(port);
         connectionServices.connectToHostAsynchronously("127.0.0.1", port, ConnectType.PLAIN);
         Socket socket = ss.accept();
         ss.close();
         
         socket.setSoTimeout(3000);
         InputStream in=socket.getInputStream();
         String word = IOUtils.readWord(in, 9);
         if (! word.equals("GNUTELLA"))
             throw new IOException("Bad word: "+word);
         
         HandshakeResponder responder;
         if (ultrapeer) {
             responder = new UltrapeerResponder();
         } else {
             responder = new OldResponder();
         }
    BlockingConnection con = connectionFactory.createConnection(socket);
         con.initialize(null, responder, 1000);
         if (shouldRespondToPing()) {
             replyToPing(con, ultrapeer);
         }
         return con;
     }
     
     /**
      * Note that this function will _EAT_ messages until it finds a ping to respond to.
      */  
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
        PingReply reply = pingReplyFactory.createExternal(guid, (byte)7,
                                 socket.getLocalPort(), 
                                 ultrapeer ? ultrapeerIP : oldIP,
                                 ultrapeer);
        reply.hop();
        c.send(reply);
        c.flush();
    }

    ///////////////////////// NO Actual Tests ////////////////////////////
    //////////////////////////////////////////////////////////////////

    protected void drainAll() throws Exception {
        BlockingConnectionUtils.drainAll(testUP);
    }

    /** Marks the Responses of QueryReply as NOT spam */
    protected void markAsNotSpam(QueryReply qr) throws Exception {
        spamManager.clearFilterData(); // Start from scratch
        
        Response[] resp = qr.getResultsArray();
        RemoteFileDesc rfds[] = new RemoteFileDesc[resp.length];
        for(int i = 0; i < resp.length; i++) {
            rfds[i] = resp[i].toRemoteFileDesc(qr, null, injector.getInstance(RemoteFileDescFactory.class), injector.getInstance(PushEndpointFactory.class));
            //assertTrue(SpamManager.instance().isSpam(rfds[i]));
        }
        
        spamManager.handleUserMarkedGood(rfds);
        
        // Make sure they're not spam
        for(int i = 0; i < rfds.length; i++) {
            assertFalse(rfds[i].isSpam());
        }
    }
    
    private class UltrapeerResponder implements HandshakeResponder {
        public HandshakeResponse respond(HandshakeResponse response, 
                boolean outgoing)  {
            Properties props = headersFactory.createUltrapeerHeaders("127.0.0.1"); 
            props.put(HeaderNames.X_DEGREE, "42");           
            return HandshakeResponse.createResponse(props);
        }
        
        public void setLocalePreferencing(boolean b) {}
    }

    private static class OldResponder implements HandshakeResponder {
        public HandshakeResponse respond(HandshakeResponse response, 
                boolean outgoing)  {
            Properties props=new Properties();
            return HandshakeResponse.createResponse(props);
        }
        
        public void setLocalePreferencing(boolean b) {}
    }
}
