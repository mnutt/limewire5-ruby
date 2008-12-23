package com.limegroup.gnutella;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;

import junit.framework.Test;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.nio.ssl.SSLUtils;
import org.limewire.util.Base32;

import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.Stage;
import com.limegroup.gnutella.helpers.UrnHelper;
import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.messages.Message;
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
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.stubs.NetworkManagerStub;

/**
 * Checks whether (multi)leaves avoid forwarding messages to ultrapeers, do
 * redirects properly, etc. The test includes a leaf attached to 3 Ultrapeers.
 */
public class ClientSidePushProxyTest extends ClientSideTestCase {
    
    protected final int PORT = 6669;

    /**
     * static so the activity callback can access it.
     */
    protected static int TIMEOUT = 1000; // should override super

    private FileManager fileManager;

    private ConnectionManager connectionManager;

    private QueryRequestFactory queryRequestFactory;

    private ApplicationServices applicationServices;

    private SearchServices searchServices;

    private ResponseFactory responseFactory;

    private QueryReplyFactory queryReplyFactory;

    private MyActivityCallback callback;

    private DownloadServices downloadServices;

    private NetworkManagerStub networkManagerStub;

    public ClientSidePushProxyTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(ClientSidePushProxyTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    @Override
    public void setUp() throws Exception {
        networkManagerStub = new NetworkManagerStub();
        networkManagerStub.setPort(PORT);
        Injector injector = LimeTestUtils.createInjector(Stage.PRODUCTION, MyActivityCallback.class, new LimeTestUtils.NetworkManagerStubModule(networkManagerStub));
        super.setUp(injector);

        DownloadManagerImpl downloadManager = (DownloadManagerImpl)injector.getInstance(DownloadManager.class);
        fileManager = injector.getInstance(FileManager.class);
        connectionManager = injector.getInstance(ConnectionManager.class);
        queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
        applicationServices = injector.getInstance(ApplicationServices.class);
        searchServices = injector.getInstance(SearchServices.class);
        responseFactory = injector.getInstance(ResponseFactory.class);
        queryReplyFactory = injector.getInstance(QueryReplyFactory.class);
        callback = (MyActivityCallback) injector.getInstance(ActivityCallback.class);
        downloadServices = injector.getInstance(DownloadServices.class);
        
        downloadManager.clearAllDownloads();
        
        //      Turn off by default, explicitly test elsewhere.
        networkManagerStub.setIncomingTLSEnabled(false);
        networkManagerStub.setOutgoingTLSEnabled(false);
        // duplicate queries are sent out each time, so avoid the DuplicateFilter
        Thread.sleep(2000);        

        // send a MessagesSupportedMessage
        testUP[0].send(injector.getInstance(MessagesSupportedVendorMessage.class));
        testUP[0].flush();
        
        // we expect to get a PushProxy request
        Message m;
        do {
            m = testUP[0].receive(TIMEOUT);
        } while (!(m instanceof PushProxyRequest));

        // we should answer the push proxy request
        PushProxyAcknowledgement ack = new PushProxyAcknowledgement(InetAddress
                .getLocalHost(), 6355, new GUID(m.getGUID()));
        testUP[0].send(ack);
        testUP[0].flush();
    }

    public void testQueryReplyHasProxiesAndCanGIVNoTLSSettingOff() throws Exception {
        // no tls nor setting, doesn't get TLS connection
        doQRPCGTest(false, false, false);
    }
    
    public void testQueryReplyHasProxiesAndCanGIVNoTLSSettingOn() throws Exception {
        // no tls but setting on, doesn't get TLS connection
        doQRPCGTest(false, true, false);
    }
    
    public void testQueryReplyHasProxiesAndCanGIVWithTLSNoSetting() throws Exception {
        // tls requested, but setting off, doesn't get TLS connection
        doQRPCGTest(true, false, false);
    }
    
    public void testQueryReplyHasProxiesAndCanGIVWithTLSAndSetting() throws Exception {
        // tls requested & setting on -- only time we'll get a TLS connection
        doQRPCGTest(true, true, true);
    }
    
    private void doQRPCGTest(boolean sendTLS, boolean settingOn, boolean listenTLS) throws Exception {
        if(settingOn)
            networkManagerStub.setOutgoingTLSEnabled(true);
        
    	setAccepted(false);
        BlockingConnectionUtils.drain(testUP[0]);

        // make sure leaf is sharing
        assertEquals(2, fileManager.getGnutellaFileList().size());
        assertEquals(1, connectionManager.getNumConnections());

        // send a query that should be answered
        QueryRequest query = queryRequestFactory.createQueryRequest(GUID.makeGuid(), (byte) 1,
                "berkeley", null, null, null, false, Network.UNKNOWN, false, 0);
        testUP[0].send(query);
        testUP[0].flush();

        // await a response
        Message m;
        do {
            m = testUP[0].receive(TIMEOUT);
        } while (!(m instanceof QueryReply));

        // confirm it has proxy info
        QueryReply reply = (QueryReply) m;
        assertNotNull(reply.getPushProxies());

        // check out PushProxy info
        Set proxies = reply.getPushProxies();
        assertEquals(1, proxies.size());
        Iterator iter = proxies.iterator();
        IpPort ppi = (IpPort) iter.next();
        assertEquals(ppi.getPort(), 6355);
        assertTrue(ppi.getInetAddress().equals(testUP[0].getInetAddress()));

        // set up a ServerSocket to get give on
        ServerSocket ss;
        if(listenTLS) {
            SSLContext context = SSLUtils.getTLSContext();
            SSLServerSocket sslServer = (SSLServerSocket)context.getServerSocketFactory().createServerSocket();
            sslServer.setNeedClientAuth(false);
            sslServer.setWantClientAuth(false);
            sslServer.setEnabledCipherSuites(new String[] {"TLS_DH_anon_WITH_AES_128_CBC_SHA"});
            ss = sslServer;
        } else {
            ss = new ServerSocket();
        }

        try {
            ss.setReuseAddress(true);        
            ss.setSoTimeout(TIMEOUT);
            ss.bind(new InetSocketAddress(9000));
            // test that the client responds to a PushRequest
            PushRequest pr = new PushRequestImpl(GUID.makeGuid(), (byte) 1, 
                                             applicationServices.getMyGUID(),
                                             0, 
                                             InetAddress.getLocalHost().getAddress(),
                                             9000,
                                             Network.TCP,
                                             sendTLS);

            // send the PR off
            testUP[0].send(pr);
            testUP[0].flush();

            // we should get a incoming GIV
            Socket givSock = ss.accept();
            try {
                assertNotNull(givSock);

                // start reading and confirming the HTTP request
                String currLine;
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(givSock.getInputStream()));

                // confirm a GIV
                currLine = reader.readLine();
                GUID guid = new GUID(
                        applicationServices.getMyGUID());
                String givLine = "GIV 0:" + guid.toHexString();
                assertTrue(currLine.startsWith(givLine));
            } finally {
                givSock.close();
            }
        } finally {
            ss.close();
        }
    }

    public void testHTTPRequestNoTLS() throws Exception {
        doHTTPRequestTest(false, false);
    }
    
    public void testHTTPRequestWithTLS() throws Exception {
        doHTTPRequestTest(true, true);
    }
    
    private void doHTTPRequestTest(boolean settingOn, boolean expectTLS) throws Exception {
        if(settingOn)
            networkManagerStub.setIncomingTLSEnabled(true);
        
    	setAccepted(true);
        BlockingConnectionUtils.drain(testUP[0]);
        // some setup
        byte[] clientGUID = GUID.makeGuid();

        // construct and send a query
        byte[] guid = GUID.makeGuid();
        searchServices.query(guid, "boalt.org");

        // the testUP[0] should get it
        Message m;
        do {
            m = testUP[0].receive(TIMEOUT);
        } while (!(m instanceof QueryRequest));

        // set up a server socket
        ServerSocket ss = new ServerSocket(7000);
        try {
            ss.setReuseAddress(true);
            ss.setSoTimeout(25 * TIMEOUT);

            // send a reply with some PushProxy info
            Set<IpPort> proxies = new TreeSet<IpPort>(IpPort.COMPARATOR);
            proxies.add(new IpPortImpl("127.0.0.1", 7000));
            Response[] res = new Response[1];
            res[0] = responseFactory.createResponse(10, 10, "boalt.org", UrnHelper.SHA1);
            m = queryReplyFactory.createQueryReply(m.getGUID(), (byte) 1, 6355,
                    myIP(), 0, res, clientGUID, new byte[0], true, false,
                    true, true, false, false, proxies);
            testUP[0].send(m);
            testUP[0].flush();

            // wait a while for Leaf to process result
            assertNotNull(callback.getRFD());


            // tell the leaf to download the file, should result in push proxy
            // request
            Downloader download = downloadServices.download((new RemoteFileDesc[] 
                { callback.getRFD() }), true, 
                    new GUID(m.getGUID()));
    
            // wait for the incoming HTTP request
            Socket httpSock = ss.accept();
            try {
                assertNotNull(httpSock);
        
                // start reading and confirming the HTTP request
                String currLine;
                BufferedReader reader = 
                    new BufferedReader(new
                                       InputStreamReader(httpSock.getInputStream()));
        
                // confirm a GET/HEAD pushproxy request
                currLine = reader.readLine();
                assertTrue(currLine.startsWith("GET /gnutella/push-proxy") ||
                           currLine.startsWith("HEAD /gnutella/push-proxy"));
                
                if(expectTLS) {
                    assertTrue(currLine.contains("tls=true"));
                } else {
                    assertFalse(currLine.contains("tls"));
                }
                
                // make sure it sends the correct client GUID
                int beginIndex = currLine.indexOf("ID=") + 3;
                String guidString = currLine.substring(beginIndex, beginIndex+26);
                GUID guidFromBackend = new GUID(clientGUID);
                GUID guidFromNetwork = new GUID(Base32.decode(guidString));
                assertEquals(guidFromNetwork, guidFromBackend);
        
                // make sure the node sends the correct X-Node
                currLine = reader.readLine();
                assertTrue(currLine.startsWith("X-Node:"));
                StringTokenizer st = new StringTokenizer(currLine, ":");
                assertEquals(st.nextToken(), "X-Node");
                InetAddress addr = InetAddress.getByName(st.nextToken().trim());
                Arrays.equals(addr.getAddress(), networkManagerStub.getAddress());
                assertEquals(PORT, Integer.parseInt(st.nextToken()));
        
                // send back a 202 and make sure no PushRequest is sent via the normal
                // way
                BufferedWriter writer = 
                    new BufferedWriter(new
                                       OutputStreamWriter(httpSock.getOutputStream()));
                
                writer.write("HTTP/1.1 202 gobbledygook");
                writer.flush();
            } finally {
                httpSock.close();
            }
    
            try {
                do {
                    m = testUP[0].receive(TIMEOUT);
                    assertTrue(!(m instanceof PushRequest));
                } while (true) ;
            } catch (InterruptedIOException ignore) {}
    
            // now make a connection to the leaf to confirm that it will send a
            // correct download request
            Socket push = new Socket(InetAddress.getLocalHost(), PORT);
            try {
                BufferedWriter writer = 
                    new BufferedWriter(new
                                       OutputStreamWriter(push.getOutputStream()));
                writer.write("GIV ");
                writer.flush();
                LimeTestUtils.waitForNIO();
                
                // the PUSH request is not matched in PushList.getBestHost() if 
                // this is set to false: the RemoteFileDesc contains the IP 
                // 192.168.0.1 but since we are connecting from a different IP 
                // it is not matched but it'll accept it this is set to true and 
                // both IPs are private 
                ConnectionSettings.LOCAL_IS_PRIVATE.setValue(true); 
                writer.write("0:" + new GUID(clientGUID).toHexString() + "/\r\n");
                writer.write("\r\n"); 
                writer.flush(); 
          
               BufferedReader reader = new BufferedReader(new InputStreamReader(push.getInputStream())); 
               String currLine = reader.readLine(); 
               assertEquals(MessageFormat.format("GET /uri-res/N2R?{0} HTTP/1.1", UrnHelper.SHA1), currLine); 
           } finally { 
               push.close(); 
           }
           
           download.stop(false);
           
       } finally { 
           ss.close(); 
       } 
    }

    public void testNoProxiesSendsPushNormalNoTLS() throws Exception {
        doNormalTest(false, false);
    }
    
    public void testNoProxiesSendsPushNormalWithTLS() throws Exception {
        doNormalTest(true, true);
    }
    
    private void doNormalTest(boolean settingOn, boolean expectTLS) throws Exception {
        
        setAccepted(true);
        
        if(settingOn)
            networkManagerStub.setIncomingTLSEnabled(true);
        
        BlockingConnectionUtils.drain(testUP[0]);
        // some setup
        byte[] clientGUID = GUID.makeGuid();

        // construct and send a query
        byte[] guid = GUID.makeGuid();
        searchServices.query(guid, "golf is awesome");

        // the testUP[0] should get it
        Message m;
        do {
            m = testUP[0].receive(TIMEOUT);
        } while (!(m instanceof QueryRequest));

        // send a reply with NO PushProxy info
        Response[] res = new Response[1];
        res[0] = responseFactory.createResponse(10, 10, "golf is awesome", UrnHelper.SHA1);
        m = queryReplyFactory.createQueryReply(m.getGUID(), (byte) 1, 6355,
                myIP(), 0, res, clientGUID, new byte[0], false, false, true,
                true, false, false, null);
        testUP[0].send(m);
        testUP[0].flush();

        // wait a while for Leaf to process result
        assertNotNull(callback.getRFD());

        // tell the leaf to download the file, should result in normal TCP
        // PushRequest
        Downloader downloader = downloadServices.download(
                (new RemoteFileDesc[] { callback.getRFD() }), true, new GUID(m.getGUID()));

        // await a PushRequest
        do {
            m = testUP[0].receive(25 * TIMEOUT);
        } while (!(m instanceof PushRequest));
        
        PushRequest pr = (PushRequest)m;
        assertNotNull(pr);
        assertEquals(expectTLS, pr.isTLSCapable());
        assertEquals(clientGUID, pr.getClientGUID());
        assertEquals(networkManagerStub.getAddress(), pr.getIP());
        assertEquals(networkManagerStub.getPort(), pr.getPort());
        assertEquals(10, pr.getIndex());
        assertFalse(pr.isFirewallTransferPush());
        
        downloader.stop(false);
    }

    public void testCanReactToBadPushProxy() throws Exception {
        // assume client accepted connections from the outside successfully
        setAccepted(true);
        
        BlockingConnectionUtils.drain(testUP[0]);
        // some setup
        byte[] clientGUID = GUID.makeGuid();

        // construct and send a query
        byte[] guid = GUID.makeGuid();
        searchServices.query(guid, "berkeley.edu");

        // the testUP[0] should get it
        Message m;
        do {
            m = testUP[0].receive(TIMEOUT);
        } while (!(m instanceof QueryRequest));

        // set up a server socket
        ServerSocket ss = new ServerSocket(7000);
        try {
            ss.setReuseAddress(true);
            ss.setSoTimeout(25 * TIMEOUT);

            // send a reply with some BAD PushProxy info
            // PushProxyInterface[] proxies = new
            // QueryReply.PushProxyContainer[2];
            Set<IpPort> proxies = new TreeSet<IpPort>(IpPort.COMPARATOR);
            proxies.add(new IpPortImpl("127.0.0.1", 7000));
            proxies.add(new IpPortImpl("127.0.0.1", 8000));
            Response[] res = new Response[1];
            res[0] = responseFactory.createResponse(10, 10, "berkeley.edu", UrnHelper.SHA1);
            m = queryReplyFactory.createQueryReply(m.getGUID(), (byte) 1, 6355,
                    myIP(), 0, res, clientGUID, new byte[0], true, false,
                    true, true, false, false, proxies);
            testUP[0].send(m);
            testUP[0].flush();

            // wait a while for Leaf to process result
            assertNotNull(callback.getRFD());

            // tell the leaf to download the file, should result in push proxy
            // request
            downloadServices
                    .download(
                            (new RemoteFileDesc[] { callback.getRFD() }), true, new GUID((m.getGUID())));

            // wait for the incoming HTTP request
            Socket httpSock = ss.accept();
            try {
                // send back an error and make sure the PushRequest is sent via
                // the normal way
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(httpSock.getOutputStream()));

                writer.write("HTTP/1.1 410 gobbledygook");
                writer.flush();
            } finally {
                httpSock.close();
            }

            // await a PushRequest
            do {
                m = testUP[0].receive(TIMEOUT * 8);
            } while (!(m instanceof PushRequest));
        } finally {
            ss.close();
        }
    }

    @Override
    public int getNumberOfPeers() {
        return 1;
    }

    private static byte[] myIP() {
        return new byte[] { (byte) 192, (byte) 168, 0, 1 };
    }

    @Singleton
    public static class MyActivityCallback extends ActivityCallbackStub {

        private Lock rfdLock = new ReentrantLock();

        private Condition rfdCondition = rfdLock.newCondition();

        private RemoteFileDesc rfd = null;

        public RemoteFileDesc getRFD() throws InterruptedException {
            rfdLock.lock();
            try {
                if (rfd == null) {
                    rfdCondition.await(120, TimeUnit.SECONDS);
                }
            } finally {
                rfdLock.unlock();
            }
            return rfd;
        }

        @Override
        public void handleQueryResult(RemoteFileDesc rfd, QueryReply queryReply,
                Set locs) {
            rfdLock.lock();
            try {
                this.rfd = rfd;
                rfdCondition.signal();
            } finally {
                rfdLock.unlock();
            }
        }

        public void cleanup() {
            rfd = null;
        }
    }

    private void setAccepted(boolean accepted) throws Exception {
        networkManagerStub.setAcceptedIncomingConnection(accepted);
    }

}
