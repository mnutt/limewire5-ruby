package com.limegroup.gnutella;

import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import junit.framework.Test;

import org.limewire.io.GUID;
import org.limewire.net.SocketsManager;
import org.limewire.service.ErrorService;

import com.limegroup.gnutella.connection.BlockingConnection;
import com.limegroup.gnutella.helpers.UrnHelper;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryReplyFactory;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutella.messages.vendor.QueryStatusResponse;
import com.limegroup.gnutella.stubs.NetworkManagerStub;

/**
 * Checks whether (multi)leaves avoid forwarding messages to ultrapeers, do
 * redirects properly, etc.  The test includes a leaf attached to 3 
 * Ultrapeers.
 */
@SuppressWarnings("all")
public class ClientSideMixedOOBGuidanceTest extends ClientSideTestCase {

    /**
     * Ultrapeer 1 UDP connection.
     */
    private static DatagramSocket UDP_ACCESS;
    private NetworkManagerStub networkManagerStub;
    private ResponseFactory responseFactory;
    private SocketsManager socketsManager;
    private UDPService udpService;
    private Acceptor acceptor;
    private SearchServices searchServices;
    private QueryReplyFactory queryReplyFactory;
    private MessagesSupportedVendorMessage messagesSupportedVendorMessage;

    public ClientSideMixedOOBGuidanceTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ClientSideMixedOOBGuidanceTest.class);
    }    

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    protected void setUp() throws Exception {
        networkManagerStub = new NetworkManagerStub();
        Injector injector = LimeTestUtils.createInjector(Stage.PRODUCTION, new LimeTestUtils.NetworkManagerStubModule(networkManagerStub));
        super.setUp(injector);
        
        responseFactory = injector.getInstance(ResponseFactory.class);
        socketsManager = injector.getInstance(SocketsManager.class);
        udpService = injector.getInstance(UDPService.class);
        acceptor = injector.getInstance(Acceptor.class);
        searchServices = injector.getInstance(SearchServices.class);
        queryReplyFactory = injector.getInstance(QueryReplyFactory.class);
        messagesSupportedVendorMessage = injector.getInstance(MessagesSupportedVendorMessage.class);
        
        networkManagerStub.setCanReceiveSolicited(true);
        networkManagerStub.setCanReceiveUnsolicited(true);
        networkManagerStub.setOOBCapable(true);
    }
    
    /** @return The first QueryRequest received from this connection.  If null
     *  is returned then it was never recieved (in a timely fashion).
     */
    private QueryStatusResponse getFirstQueryStatus(BlockingConnection c) 
                                       throws IOException, BadPacketException {
        return (QueryStatusResponse)
            BlockingConnectionUtils.getFirstInstanceOfMessageType(c, QueryStatusResponse.class, TIMEOUT);
    }

    // TODO move setup stuff out to setUp method
    public void testMixedProtocol() throws Exception {
        UDP_ACCESS = new DatagramSocket();

        for (int i = 0; i < testUP.length; i++) {
            assertTrue("not open", testUP[i].isOpen());
            assertTrue("not up->leaf", testUP[i].getConnectionCapabilities().isSupernodeClientConnection());
            BlockingConnectionUtils.drain(testUP[i], 500);
            if ((i==2)) { // i'll send 0 later....
                testUP[i].send(messagesSupportedVendorMessage);
                testUP[i].flush();
            }
        }
        
        testUP[0].send(messagesSupportedVendorMessage);
        testUP[0].flush();
        
        // first we need to set up GUESS capability
        UDP_ACCESS.setSoTimeout(TIMEOUT*2);
        // ----------------------------------------
 
        // you also have to set up TCP incoming....
        {
            Socket sock = null;
            OutputStream os = null;
            try {
                sock=socketsManager.connect(new InetSocketAddress(InetAddress.getLocalHost().getHostAddress(),
                                     SERVER_PORT), 12);
                os = sock.getOutputStream();
                os.write("CONNECT BACK\n\n".getBytes());
                os.flush();
            } catch (Throwable t) {
                ErrorService.error(t);
            } finally {
                if(sock != null)
                    try { sock.close(); } catch(IOException ignored) {}
                if(os != null)
                    try { os.close(); } catch(IOException ignored) {}
            }
        }        

        // ----------------------------------------

        Thread.sleep(250);
        // we should now be guess capable and tcp incoming capable....
        // test directly on UPD service and acceptor, since networkmanager is stubbed
        assertTrue("acceptor should have received a connection", acceptor.acceptedIncoming());
        
        // get rid of any messages that are stored up.
        drainAll();

        // first of all, we should confirm that we are sending out a OOB query.
        GUID queryGuid = new GUID(searchServices.newQueryGUID());
        assertTrue(GUID.addressesMatch(queryGuid.bytes(), 
                networkManagerStub.getAddress(), 
                networkManagerStub.getPort()));
        searchServices.query(queryGuid.bytes(), "susheel");
        Thread.sleep(250);

        // some connected UPs should get a OOB query
        for (int i = 0; i < testUP.length; i++) {
            QueryRequest qr = BlockingConnectionUtils.getFirstQueryRequest(testUP[i], TIMEOUT);
            assertNotNull("up " + i + " didn't get query", qr);
            assertEquals(new GUID(qr.getGUID()), queryGuid);
            if ((i==0) || (i==2))
                assertTrue(qr.desiresOutOfBandReplies());
            else
                assertTrue(!qr.desiresOutOfBandReplies());
        }

        // now confirm that we leaf guide the 'even' guys but not the others.
        Message m = null;
        // ensure that we'll get a QueryStatusResponse from the Responses
        // we're sending.
        for (int i = 0; i < testUP.length; i++) {
            Response[] res = new Response[] {
                responseFactory.createResponse(10, 10, "susheel"+i, UrnHelper.SHA1),
                responseFactory.createResponse(10, 10, "susheel smells good"+i, UrnHelper.SHA1),
                responseFactory.createResponse(10, 10, "anita is sweet"+i, UrnHelper.SHA1),
                responseFactory.createResponse(10, 10, "anita is prety"+i, UrnHelper.SHA1),
                responseFactory.createResponse(10, 10, "susheel smells bad" + i, UrnHelper.SHA1),
                responseFactory.createResponse(10, 10, "renu is sweet " + i, UrnHelper.SHA1),
                responseFactory.createResponse(10, 10, "prety is spelled pretty " + i, UrnHelper.SHA1),
                responseFactory.createResponse(10, 10, "go susheel go" + i, UrnHelper.SHA1),
                responseFactory.createResponse(10, 10, "susheel runs fast" + i, UrnHelper.SHA1),
                responseFactory.createResponse(10, 10, "susheel jumps high" + i, UrnHelper.SHA1),
                responseFactory.createResponse(10, 10, "sleepy susheel" + i, UrnHelper.SHA1),
            };
            m = queryReplyFactory.createQueryReply(queryGuid.bytes(), (byte) 1, 6355,
                    myIP(), 0, res, GUID.makeGuid(), new byte[0], false, false,
                    true, true, false, false, null);
            testUP[i].send(m);
            testUP[i].flush();
        }
        
        // all UPs should get a QueryStatusResponse
        for (int i = 0; i < testUP.length; i++) {
            QueryStatusResponse stat = getFirstQueryStatus(testUP[i]);
            if ((i==0) || (i==2)) {
                assertNotNull(stat);
                assertEquals(new GUID(stat.getGUID()), queryGuid);
                assertEquals(5, stat.getNumResults());
            }
            else
                assertNull(stat);
        }

        // shut off the query....
        searchServices.stopQuery(queryGuid);

        // all UPs should get a QueryStatusResponse with 65535
        for (int i = 0; i < testUP.length; i++) {
            QueryStatusResponse stat = getFirstQueryStatus(testUP[i]);
            if ((i==0) || (i==2)) {
                assertNotNull(stat);
                assertEquals(new GUID(stat.getGUID()), queryGuid);
                assertEquals(65535, stat.getNumResults());
            }
            else
                assertNull(stat);
        }

    }


    //////////////////////////////////////////////////////////////////

    private static byte[] myIP() {
        return new byte[] { (byte)127, (byte)0, 0, 1 };
    }

    public int getNumberOfPeers() {
        return 3;
    }
}
