package com.limegroup.gnutella;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.limewire.core.api.browse.BrowseListener;
import org.limewire.core.api.friend.feature.features.AddressFeature;
import org.limewire.core.api.search.SearchResult;
import org.limewire.io.GUID;
import org.limewire.io.IpPortImpl;
import org.limewire.io.IpPortSet;
import org.limewire.util.Base32;

import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.Stage;
import com.limegroup.gnutella.helpers.UrnHelper;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryReplyFactory;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.stubs.NetworkManagerStub;

/**
 * Checks whether (multi)leaves avoid forwarding messages to ultrapeers, do
 * redirects properly, etc.  The test includes a leaf attached to 3 
 * ultrapeers.
 */
public class ClientSideBrowseHostTest extends ClientSideTestCase {

    private MyActivityCallback callback;
    private NetworkManagerStub networkManagerStub;
    private SearchServices searchServices;
    private ResponseFactory responseFactory;
    private QueryReplyFactory queryReplyFactory;

    public ClientSideBrowseHostTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(ClientSideBrowseHostTest.class);
    }    

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    ///////////////////////// Actual Tests ////////////////////////////
    
    // Tests the following behaviors:
    // ------------------------------
    // 1. that the client makes a correct direct connection if possible
    // 2. that the client makes a correct push proxy connection if necessary
    // 3. if all else fails the client sends a PushRequest

    @Override
    protected void setUp() throws Exception {
        networkManagerStub = new NetworkManagerStub();
        networkManagerStub.setAcceptedIncomingConnection(true);
        networkManagerStub.setPort(SERVER_PORT);
        networkManagerStub.setExternalAddress(new byte[] { (byte)129, 1, 4, 10 });
        Injector injector = LimeTestUtils.createInjector(Stage.PRODUCTION, MyActivityCallback.class,
                new LimeTestUtils.NetworkManagerStubModule(networkManagerStub));
        super.setUp(injector);
        callback = (MyActivityCallback) injector.getInstance(ActivityCallback.class);
        searchServices = injector.getInstance(SearchServices.class);
        responseFactory = injector.getInstance(ResponseFactory.class);
        queryReplyFactory = injector.getInstance(QueryReplyFactory.class);
    }

    public void testHTTPRequest() throws Exception {
        
        BlockingConnectionUtils.drain(testUP[0]);
        // some setup
        final byte[] clientGUID = GUID.makeGuid();

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
        try {
            ss.setReuseAddress(true);
            ss.setSoTimeout(TIMEOUT);

            // send a reply
            Response[] res = new Response[1];
            res[0] = responseFactory.createResponse(10, 10, "boalt.org", UrnHelper.SHA1);
            m = queryReplyFactory.createQueryReply(m.getGUID(), (byte) 1, 7000,
                    InetAddress.getLocalHost().getAddress(), 0, res, clientGUID, new byte[0], false, false,
                    true, true, false, false, null);
            testUP[0].send(m);
            testUP[0].flush();

            // wait a while for Leaf to process result
            assertNotNull(callback.getRFD());

            // tell the leaf to browse host the file, should result in direct HTTP
            // request
            searchServices.doAsynchronousBrowseHost(new MockFriendPresence(new MockFriend(), new AddressFeature(callback.getRFD().getAddress())), new GUID(), new BrowseListener() {
                public void handleBrowseResult(SearchResult searchResult) {
                    //To change body of implemented methods use File | Settings | File Templates.
                }

                public void browseFinished(boolean success) {
                    //To change body of implemented methods use File | Settings | File Templates.
                }
            });

            // wait for the incoming HTTP request
            Socket httpSock = ss.accept();
            try {
                assertIsBrowse(httpSock, 7000);
            } finally {
                httpSock.close();
            }
            
            try {
                do {
                    m = testUP[0].receive(TIMEOUT);
                    assertTrue(!(m instanceof PushRequest));
                } while (true) ;
            }
            catch (InterruptedIOException expected) {}
        } finally {
            // awesome - everything checks out!
            ss.close();
        }
    }

    public void testPushProxyRequest() throws Exception {
        // wait for connections to process any messages
        Thread.sleep(6000);
        
        BlockingConnectionUtils.drain(testUP[0]);
        // some setup
        final byte[] clientGUID = GUID.makeGuid();

        // construct and send a query        
        byte[] guid = GUID.makeGuid();
        searchServices.query(guid, "nyu.edu");

        // the testUP[0] should get it
        Message m = null;
        do {
            m = testUP[0].receive(TIMEOUT);
        } while (!(m instanceof QueryRequest)) ;

        // set up a server socket to wait for proxy request
        ServerSocket ss = new ServerSocket(7000);
        try {
            ss.setReuseAddress(true);
            ss.setSoTimeout(TIMEOUT*4);

            // send a reply with some PushProxy info
            final IpPortSet proxies = new IpPortSet();
            proxies.add(new IpPortImpl("127.0.0.1", 7000));
            Response[] res = new Response[1];
            res[0] = responseFactory.createResponse(10, 10, "nyu.edu", UrnHelper.SHA1);
            m = queryReplyFactory.createQueryReply(m.getGUID(), (byte) 1, 6999,
                    InetAddress.getLocalHost().getAddress(), 0, res, clientGUID, new byte[0], true, false,
                    true, true, false, false, proxies);
            testUP[0].send(m);
            testUP[0].flush();

            // wait a while for Leaf to process result
            assertNotNull(callback.getRFD());
            
            // tell the leaf to browse host the file, should result in PushProxy
            // request
            searchServices.doAsynchronousBrowseHost(new MockFriendPresence(new MockFriend(), new AddressFeature(callback.getRFD().getAddress())), new GUID(), new BrowseListener() {
                public void handleBrowseResult(SearchResult searchResult) {
                    //To change body of implemented methods use File | Settings | File Templates.
                }

                public void browseFinished(boolean success) {
                    //To change body of implemented methods use File | Settings | File Templates.
                }
            });

            // wait for the incoming PushProxy request
            // increase the timeout since we send udp pushes first
            ss.setSoTimeout(7000);
            Socket httpSock = ss.accept();
            try {
                BufferedWriter sockWriter  = 
                    new BufferedWriter(new
                            OutputStreamWriter(httpSock.getOutputStream()));
                sockWriter.write("HTTP/1.1 202 OK\r\n");
                sockWriter.flush();

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

                // make sure the node sends the correct X-Node
                currLine = reader.readLine();
                assertTrue(currLine.startsWith("X-Node:"));
                StringTokenizer st = new StringTokenizer(currLine, ":");
                assertEquals(st.nextToken(), "X-Node");
                InetAddress addr = InetAddress.getByName(st.nextToken().trim());
                Arrays.equals(addr.getAddress(), networkManagerStub.getAddress());
                assertEquals(SERVER_PORT, Integer.parseInt(st.nextToken()));

                // now we need to GIV
                Socket push = new Socket(InetAddress.getLocalHost(), SERVER_PORT);
                try {
                    BufferedWriter writer = 
                        new BufferedWriter(new
                                OutputStreamWriter(push.getOutputStream()));
                    writer.write("GIV 0:" + new GUID(clientGUID).toHexString() + "/\r\n");
                    writer.write("\r\n");
                    writer.flush();

                    assertIsBrowse(push, push.getLocalPort());
                } finally {
                    push.close();
                }
            } finally {
                httpSock.close();
            }

            try {
                do {
                    m = testUP[0].receive(TIMEOUT);
                    assertNotInstanceof(m.toString(), PushRequest.class, m);
                } while (true) ;
            }
            catch (InterruptedIOException expected) {}
        } finally {
            ss.close();
        }
    }


    public void testSendsPushRequest() throws Exception {
        BlockingConnectionUtils.drain(testUP[0]);
        // some setup
        final byte[] clientGUID = GUID.makeGuid();

        // construct and send a query        
        byte[] guid = GUID.makeGuid();
        searchServices.query(guid, "anita");

        // the testUP[0] should get it
        Message m = null;
        do {
            m = testUP[0].receive(TIMEOUT);
        } while (!(m instanceof QueryRequest)) ;

        // send a reply with some BAD PushProxy info
        final IpPortSet proxies = new IpPortSet(new IpPortImpl("127.0.0.1", 7001));
        Response[] res = new Response[] { responseFactory.createResponse(10, 10, "anita", UrnHelper.SHA1) };
        m = queryReplyFactory.createQueryReply(m.getGUID(), (byte) 1, 7000,
                InetAddress.getLocalHost().getAddress(), 0, res, clientGUID, new byte[0], true, false, true,
                true, false, false, proxies);
        testUP[0].send(m);
        testUP[0].flush();

        // wait a while for Leaf to process result
        assertNotNull(callback.getRFD());

        // tell the leaf to browse host the file,
        searchServices.doAsynchronousBrowseHost(new MockFriendPresence(new MockFriend(), new AddressFeature(callback.getRFD().getAddress())), new GUID(), new BrowseListener() {
                public void handleBrowseResult(SearchResult searchResult) {
                    //To change body of implemented methods use File | Settings | File Templates.
                }

                public void browseFinished(boolean success) {
                    //To change body of implemented methods use File | Settings | File Templates.
                }
            });
        // nothing works for the guy, we should get a PushRequest
        do {
            m = testUP[0].receive(TIMEOUT*30);
        } while (!(m instanceof PushRequest));

        // awesome - everything checks out!
    }


    private void assertIsBrowse(Socket httpSock, int port) throws IOException {
        // start reading and confirming the HTTP request
        String currLine = null;
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                httpSock.getInputStream()));

        // confirm a GET/HEAD push proxy request
        currLine = reader.readLine();
        assertEquals("GET / HTTP/1.1", currLine);

        // make sure the node sends the correct Host val
        currLine = reader.readLine();
        assertTrue(currLine.startsWith("Host:"));
        StringTokenizer st = new StringTokenizer(currLine, ":");
        assertEquals(st.nextToken(), "Host");
        // this assertion fails when localhost is bound to multiple IP addresses
        // since the client might connect to a different address than the server
        // socket is listening on
        InetAddress.getByName(st.nextToken().trim());
        // assertEquals(InetAddress.getByName(st.nextToken().trim()), addr);
        assertEquals(port, Integer.parseInt(st.nextToken()));

        // send back a 200 and make sure no PushRequest is sent via the normal
        // way
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                httpSock.getOutputStream()));

        writer.write("HTTP/1.1 200 OK\r\n");
        writer.flush();
        writer.write("\r\n");
        writer.flush();
        // TODO: should i send some Query Hits? Might be a good test.
    }

    @Override
    public int getNumberOfPeers() {
        return 1;
    }

    @Singleton
    private static class MyActivityCallback extends ActivityCallbackStub {
        
        private volatile RemoteFileDesc remoteFileDesc;

        private final CountDownLatch latch = new CountDownLatch(1);
        
        public RemoteFileDesc getRFD() throws InterruptedException {
            latch.await(5, TimeUnit.SECONDS);
            return remoteFileDesc;
        }
        
        @Override
        public void handleQueryResult(RemoteFileDesc rfd,
                                      QueryReply queryReply,
                                      Set locs) {
            remoteFileDesc = rfd;
            latch.countDown();
        }
    }

}
