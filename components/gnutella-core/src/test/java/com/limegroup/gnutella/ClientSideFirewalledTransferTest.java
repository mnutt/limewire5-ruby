package com.limegroup.gnutella;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.gnutella.tests.ActivityCallbackStub;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.gnutella.tests.NetworkManagerStub;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.io.IpPortSet;
import org.limewire.rudp.RUDPUtils;
import org.limewire.rudp.messages.SynMessage;
import org.limewire.util.Base32;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.Stage;
import com.limegroup.gnutella.helpers.UrnHelper;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.PushRequestImpl;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryReplyFactory;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutella.messages.vendor.PushProxyAcknowledgement;
import com.limegroup.gnutella.messages.vendor.PushProxyRequest;

/**
 * Checks whether (multi)leaves avoid forwarding messages to ultrapeers, do
 * redirects properly, etc.  The test includes a leaf attached to 1 Ultrapeer.
 */
public class ClientSideFirewalledTransferTest extends ClientSideTestCase {
    protected static final int PORT=6669;
    protected static int TIMEOUT=1000; // should override super

    /**
     * Ultrapeer 1 UDP connection.
     */
    private DatagramSocket UDP_ACCESS;
    private NetworkManagerStub networkManagerStub;
    @Inject private QueryRequestFactory queryRequestFactory;
    @Inject private ConnectionManager connectionManager;
    @Inject private ApplicationServices applicationServices;
    @Inject private MessageFactory messageFactory;
    @Inject private SearchServices searchServices;
    @Inject private ResponseFactory responseFactory;
    @Inject private QueryReplyFactory queryReplyFactory;
    @Inject private MyActivityCallback callback;
    @Inject private DownloadServices downloadServices;
    @Inject private Injector injector;

    public ClientSideFirewalledTransferTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ClientSideFirewalledTransferTest.class);
    }    

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    public void setSettings() {
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
    }
    
    @Override
    public void setUp() throws Exception {
        UDP_ACCESS = new DatagramSocket(9000);
        UDP_ACCESS.setSoTimeout(TIMEOUT*2);
        networkManagerStub = new NetworkManagerStub();
        injector = LimeTestUtils.createInjector(Stage.PRODUCTION, MyActivityCallback.class,
                new LimeTestUtils.NetworkManagerStubModule(networkManagerStub));
        super.setUp(injector);

        callback = (MyActivityCallback) injector.getInstance(ActivityCallback.class);
        
        //  NOTE: UDPService will not change state when a UDP ping or pong is received 
        //      from a conneected host.  Therefore, since UDPServiceTest should be testing
        //      that UDPService functions properly, we will assume that it does and simply
        //      set the flag saying we can support solicited and unsolicited UDP.  This way,
        //      testing can procede as normal...
        networkManagerStub.setCanReceiveSolicited(true);
        networkManagerStub.setCanReceiveUnsolicited(true);
        networkManagerStub.setCanDoFWT(true);
        // has to be false otherwise the client doesn't send fwt results
        networkManagerStub.setAcceptedIncomingConnection(false);
        networkManagerStub.setExternalAddress(new byte[] {(byte) 10, (byte) 07,
                (byte) 19, (byte) 76});
        networkManagerStub.setPort(PORT);
        
        exchangePushProxyMesssages();
    }
    
    @Override
    protected void tearDown() throws Exception {
        // important superclass impl has to be called
        super.tearDown();
        UDP_ACCESS.close();
    }
    
    /**
     * This is needed to provide the client with a valid push proxy it
     * can send along in fwt query replies. 
     */
    private void exchangePushProxyMesssages() throws Exception {
        // send a MessagesSupportedMessage
        testUP[0].send(injector.getInstance(MessagesSupportedVendorMessage.class));
        testUP[0].flush();

        // we expect to get a PushProxy request
        Message m = null;
        do {
            m = testUP[0].receive(TIMEOUT);
        } while (!(m instanceof PushProxyRequest)) ;

        // we should answer the push proxy request
        PushProxyAcknowledgement ack = 
        new PushProxyAcknowledgement(InetAddress.getLocalHost(), 
                                     6355, new GUID(m.getGUID()));
        testUP[0].send(ack);
        testUP[0].flush();

        // client side seems to follow the setup process A-OK
        // set up solicted support (see note in setUp())
    }
    
    
    ///////////////////////// Actual Tests ////////////////////////////
    
    public void testStartsUDPTransfer() throws Exception {
        BlockingConnectionUtils.drain(testUP[0]);
        drainUDP();

        // make sure leaf is sharing
        assertEquals(2, gnutellaFileView.size());

        // send a query that should be answered
        QueryRequest query = queryRequestFactory.createQueryRequest(GUID.makeGuid(), (byte)2,
                0 | QueryRequest.SPECIAL_MINSPEED_MASK |
                 QueryRequest.SPECIAL_FIREWALL_MASK |
                 QueryRequest.SPECIAL_FWTRANS_MASK, "berkeley", "", null, null, false, Network.UNKNOWN,
                false, 0, false, 0);
        assertTrue(query.canDoFirewalledTransfer());
        
        testUP[0].send(query);
        testUP[0].flush();

        // await a response
        Message m = null;
        do {
            m = testUP[0].receive(TIMEOUT * 120);
        } while (!(m instanceof QueryReply)) ;

        // confirm it has proxy info
        QueryReply reply = (QueryReply) m;
        assertTrue(reply.getSupportsFWTransfer());
        assertEquals(RUDPUtils.VERSION, reply.getFWTransferVersion());
        assertNotNull(reply.getPushProxies());

        // check out PushProxy info
        Set proxies = reply.getPushProxies();
        assertEquals(1, proxies.size());
        Iterator iter = proxies.iterator();
        IpPort ppi = (IpPort)iter.next();
        assertEquals(ppi.getPort(), 6355);
        
        assertEquals(ppi.getInetAddress(), 
                (connectionManager.getConnections().get(0)).getInetAddress()
                );

        // set up a ServerSocket to get give on
        ServerSocket ss = new ServerSocket(9000);
        ss.setReuseAddress(true);        
        ss.setSoTimeout(TIMEOUT);

        // test that the client responds to a PushRequest
        PushRequest pr = new PushRequestImpl(GUID.makeGuid(), (byte) 1, 
                                         applicationServices.getMyGUID(),
                                         PushRequest.FW_TRANS_INDEX, 
                                         InetAddress.getLocalHost().getAddress(),
                                         9000);
        
        // send the PR off
        testUP[0].send(pr);
        testUP[0].flush();

        // we should get an incoming UDP transmission
        while (true) {
            DatagramPacket pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
            }
            catch (IOException bad) {
                fail("Did not get SYN", bad);
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            // as long as we don't get a ClassCastException we are good to go
            Message syn = messageFactory.read(in, Network.UDP);
            if (syn instanceof SynMessage) break;
        }

        // but we should NOT get a incoming GIV
        try {
            ss.accept();
            fail("shouldn't have accepted");
        } catch (InterruptedIOException expected) {}
        ss.close();
    }


    public void testHTTPRequest() throws Exception {
        BlockingConnectionUtils.drain(testUP[0]);
        drainUDP();
        // some setup
        byte[] clientGUID = GUID.makeGuid();

        // construct and send a query        
        byte[] guid = GUID.makeGuid();
        searchServices.query(guid, "boalt.org");

        // the testUP[0] should get it
        Message m = null;
        do {
            m = testUP[0].receive(TIMEOUT);
        } while (!(m instanceof QueryRequest)) ;

        // set up a server socket
        ServerSocket ss = new ServerSocket(7000);
        ss.setReuseAddress(true);        
        ss.setSoTimeout(15*TIMEOUT);

        // send a reply with some PushProxy info
        Set<IpPort> proxies = new IpPortSet();
        proxies.add(new IpPortImpl("127.0.0.1", 7000));
        Response[] res = new Response[1];
        res[0] = responseFactory.createResponse(10, 10, "boalt.org", UrnHelper.SHA1);
        m = queryReplyFactory.createQueryReply(m.getGUID(), (byte) 1, 9000,
                myIP(), 0, res, clientGUID, new byte[0], true, false, true,
                true, false, false, true, proxies, null);
        testUP[0].send(m);
        testUP[0].flush();

        final RemoteFileDesc rfd = callback.getRFD(); 
        assertNotNull(rfd);
        assertTrue(rfd.getAddress() instanceof PushEndpoint);

        // tell the leaf to download the file, should result in push proxy
        // request
        final GUID fGuid = new GUID(m.getGUID());
        Thread runLater = new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                    downloadServices.download((new RemoteFileDesc[]{rfd}), 
                                           true, fGuid);
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        runLater.start();

        // wait for the incoming HTTP request
        Socket httpSock = ss.accept();
        assertNotNull(httpSock);

        // start reading and confirming the HTTP request
        String currLine = null;
        BufferedReader reader = 
            new BufferedReader(new
                               InputStreamReader(httpSock.getInputStream()));

        // confirm a GET/HEAD pushproxy request
        currLine = reader.readLine();
        assertTrue(currLine.startsWith("GET /gnutella/push-proxy") ||
                   currLine.startsWith("HEAD /gnutella/push-proxy"));
        
        // make sure it sends the correct client GUID
        int beginIndex = currLine.indexOf("ID=") + 3;
        String guidString = currLine.substring(beginIndex, beginIndex+26);
        GUID guidFromBackend = new GUID(clientGUID);
        GUID guidFromNetwork = new GUID(Base32.decode(guidString));
        assertEquals(guidFromNetwork, guidFromBackend);

        // make sure it sends back the correct index
        beginIndex = currLine.indexOf("&file=") + 6;
        String longString = currLine.substring(beginIndex, beginIndex+10);
        long index = Long.parseLong(longString);
        assertEquals(index, PushRequest.FW_TRANS_INDEX);

        // make sure the node sends the correct X-Node
        currLine = reader.readLine();
        assertTrue(currLine.startsWith("X-Node:"));
        StringTokenizer st = new StringTokenizer(currLine, ":");
        assertEquals(st.nextToken(), "X-Node");
        InetAddress addr = InetAddress.getByName(st.nextToken().trim());
        assertEquals(networkManagerStub.getExternalAddress(), addr.getAddress());
        assertEquals("expecting the stable udp port, since fwt transfer: ", networkManagerStub.getStableUDPPort(), Integer.parseInt(st.nextToken()));

        // send back a 202 and make sure no PushRequest is sent via the normal
        // way
        BufferedWriter writer = 
            new BufferedWriter(new
                               OutputStreamWriter(httpSock.getOutputStream()));
        
        writer.write("HTTP/1.1 202 gobbledygook");
        writer.flush();
        httpSock.close();

        Thread.sleep(500);

        // we should get an incoming UDP transmission
        while (true) {
            DatagramPacket pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
            }
            catch (IOException bad) {
                fail("Did not get SYN", bad);
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            // as long as we don't get a ClassCastException we are good to go
            Message syn = messageFactory.read(in, Network.UDP);
            if (syn instanceof SynMessage) break;
        }

        try {
            do {
                m = testUP[0].receive(TIMEOUT);
                assertTrue(!(m instanceof PushRequest));
            } while (true) ;
        }
        catch (InterruptedIOException expected) {}

        // awesome - everything checks out!
        ss.close();
    }


    //////////////////////////////////////////////////////////////////
    @Override
    public int getNumberOfPeers() {
        return 1;
    }

    private static byte[] myIP() {
        return new byte[] { (byte)127, (byte)0, 0, 1 };
    }

    @Singleton
    public static class MyActivityCallback extends ActivityCallbackStub {
        
        private volatile RemoteFileDesc rfd = null;
        
        private final CountDownLatch latch = new CountDownLatch(1);

        public RemoteFileDesc getRFD() throws InterruptedException {
            latch.await(1, TimeUnit.SECONDS);
            return rfd;
        }

        @Override
        public void handleQueryResult(RemoteFileDesc rfd,
                                      QueryReply queryReply,
                                      Set locs) {
            this.rfd = rfd;
            latch.countDown();
        }
    }

    public void drainUDP() throws Exception {
        while (true) {
            DatagramPacket pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
            }
            catch (InterruptedIOException bad) {
                break;
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            // as long as we don't get a ClassCastException we are good to go
            messageFactory.read(in, Network.TCP);
        }
    }
}
