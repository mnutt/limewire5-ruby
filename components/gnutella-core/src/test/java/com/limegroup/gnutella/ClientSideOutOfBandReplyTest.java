package com.limegroup.gnutella;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import junit.framework.Test;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.SearchSettings;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.gnutella.tests.NetworkManagerStub;
import org.limewire.io.GUID;

import com.google.inject.Injector;
import com.google.inject.Stage;
import com.limegroup.gnutella.helpers.UrnHelper;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.PingReplyFactory;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryReplyFactory;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.messages.vendor.LimeACKVendorMessage;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutella.messages.vendor.OOBProxyControlVendorMessage;
import com.limegroup.gnutella.messages.vendor.PushProxyAcknowledgement;
import com.limegroup.gnutella.messages.vendor.ReplyNumberVendorMessage;
import com.limegroup.gnutella.messages.vendor.ReplyNumberVendorMessageFactory;
import com.limegroup.gnutella.search.QueryHandler;
import com.limegroup.gnutella.search.SearchResultHandler;

/**
 * Checks whether (multi)leaves avoid forwarding messages to ultrapeers, do
 * redirects properly, etc.  The test includes a leaf attached to 3 
 * Ultrapeers.
 */
public class ClientSideOutOfBandReplyTest extends ClientSideTestCase {

    /**
     * Ultrapeer 1 UDP connection.
     */
    private static DatagramSocket UDP_ACCESS;
    
    private NetworkManagerStub networkManagerStub;

    private MessagesSupportedVendorMessage messagesSupportedVendorMessage;

    private SearchServices searchServices;

    private ResponseFactory responseFactory;

    private QueryReplyFactory queryReplyFactory;

    private ReplyNumberVendorMessageFactory replyNumberVendorMessageFactory;

    private QueryRequestFactory queryRequestFactory;

    private MessageFactory messageFactory;

    private PingReplyFactory pingReplyFactory;
    private SearchResultHandler searchResultHandler;
    private ApplicationServices applicationServices;

    public ClientSideOutOfBandReplyTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ClientSideOutOfBandReplyTest.class);
    }    

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    @Override
    public void setSettings() {
        ConnectionSettings.DO_NOT_BOOTSTRAP.setValue(true);
    }
    
    @Override
    protected void setUp() throws Exception {
        networkManagerStub = new NetworkManagerStub();
        Injector injector = LimeTestUtils.createInjector(Stage.PRODUCTION, new LimeTestUtils.NetworkManagerStubModule(networkManagerStub));
        
        super.setUp(injector);

        messagesSupportedVendorMessage = injector.getInstance(MessagesSupportedVendorMessage.class);
        searchServices = injector.getInstance(SearchServices.class);
        responseFactory = injector.getInstance(ResponseFactory.class);
        queryReplyFactory = injector.getInstance(QueryReplyFactory.class);
        replyNumberVendorMessageFactory = injector.getInstance(ReplyNumberVendorMessageFactory.class);
        queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
        messageFactory = injector.getInstance(MessageFactory.class);
        pingReplyFactory = injector.getInstance(PingReplyFactory.class);
        searchResultHandler = injector.getInstance(SearchResultHandler.class);
        applicationServices = injector.getInstance(ApplicationServices.class);
        
        networkManagerStub.setCanReceiveSolicited(true);
        networkManagerStub.setCanReceiveUnsolicited(true);
        networkManagerStub.setOOBCapable(true);
        networkManagerStub.setPort(SERVER_PORT);
        networkManagerStub.setCanDoFWT(true);
        networkManagerStub.setExternalAddress(new byte[] { 10, 17, 0, 1 });
        
        UDP_ACCESS = new DatagramSocket();
        UDP_ACCESS.setSoTimeout(2000);
        
        exchangeMessageSupportAndKeepAlive();
    }
    
    public void exchangeMessageSupportAndKeepAlive() throws Exception {
        for (int i = 0; i < testUP.length; i++) {
            assertTrue("should be open", testUP[i].isOpen());
            assertTrue("should be up -> leaf",
                testUP[i].getConnectionCapabilities().isSupernodeClientConnection());
            BlockingConnectionUtils.drain(testUP[i], 500);
            // OOB client side needs server side leaf guidance
            testUP[i].send(messagesSupportedVendorMessage);
            testUP[i].flush();
        }
        // ----------------------------------------

        Thread.sleep(250);
        
        BlockingConnectionUtils.keepAllAlive(testUP, pingReplyFactory);
        // clear up any messages before we begin the test.
        drainAll();
    }
    
    public void testBasicProtocol() throws Exception {
       
        // first of all, we should confirm that we are sending out a OOB query.
        GUID queryGuid = new GUID(searchServices.newQueryGUID());
        assertTrue(GUID.addressesMatch(queryGuid.bytes(), 
                networkManagerStub.getAddress(), 
                networkManagerStub.getPort()));
        searchServices.query(queryGuid.bytes(), "susheel");
        Thread.sleep(250);

        // all connected UPs should get a OOB query
        for (int i = 0; i < testUP.length; i++) {
            QueryRequest qr = BlockingConnectionUtils.getFirstQueryRequest(testUP[i]);
            assertNotNull(qr);
            assertEquals(new GUID(qr.getGUID()), queryGuid);
            assertTrue(qr.desiresOutOfBandReplies());
        }

        // now confirm that we follow the OOB protocol
        ReplyNumberVendorMessage vm = 
           replyNumberVendorMessageFactory.create(queryGuid, 10);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        vm.write(baos);
        DatagramPacket pack = new DatagramPacket(baos.toByteArray(), 
                                  baos.toByteArray().length,
                                  testUP[0].getInetAddress(), 
                                  SERVER_PORT);
        UDP_ACCESS.send(pack);

        // we should get a LimeACK in response
        LimeACKVendorMessage ack = null;
        while (ack == null) {
            pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
            }
            catch (IOException bad) {
                fail("Did not get ack", bad);
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            Message m = messageFactory.read(in, Network.TCP);
            if (m instanceof LimeACKVendorMessage)
                ack = (LimeACKVendorMessage) m;
        }
        assertEquals(queryGuid, new GUID(ack.getGUID()));
        assertEquals(10, ack.getNumResults());
        assertNotNull(ack.getSecurityToken());
        
        // now send back some results - they should be accepted.
        Response[] res = new Response[10];
        for (int j = 0; j < res.length; j++)
            res[j] = responseFactory.createResponse(10, 10, "susheel"+j, UrnHelper.SHA1);
        Message m = 
            queryReplyFactory.createQueryReply(queryGuid.bytes(), (byte) 1, 6355,
                myIP(), 0, res, GUID.makeGuid(), new byte[0], false, false,
                true, true, false, false, null, ack.getSecurityToken());
        baos = new ByteArrayOutputStream();
        m.write(baos);
        byte [] packet = baos.toByteArray();
        DatagramPacket reply = new DatagramPacket(packet, 
        		packet.length,
        		testUP[0].getInetAddress(), 
        		SERVER_PORT);
        UDP_ACCESS.send(reply);
        Thread.sleep(250);
        assertEquals(10,searchResultHandler.getNumResultsForQuery(queryGuid));
    }

    public void testOOBv2Disabled() throws Exception {
        drainAll();
        testUP[0].send(messagesSupportedVendorMessage);
        testUP[0].flush();
        Thread.sleep(200);
        assertNull(BlockingConnectionUtils.getFirstInstanceOfMessageType(testUP[0], OOBProxyControlVendorMessage.class));
        
        SearchSettings.DISABLE_OOB_V2.setBoolean(true);
        testUP[0].send(messagesSupportedVendorMessage);
        testUP[0].flush();
        Thread.sleep(2000);
        OOBProxyControlVendorMessage m = BlockingConnectionUtils.getFirstInstanceOfMessageType(testUP[0], OOBProxyControlVendorMessage.class);
        assertNotNull(m);
        assertEquals(2,m.getMaximumDisabledVersion());
    }

    public void testRemovedQuerySemantics() throws Exception {
        // send a query and make sure that after it is removed (i.e. stopped by
        // the user) we don't request OOB replies for it

        // first of all, we should confirm that we are sending out a OOB query.
        GUID queryGuid = new GUID(searchServices.newQueryGUID());
        assertTrue(GUID.addressesMatch(queryGuid.bytes(), 
                networkManagerStub.getAddress(), 
                networkManagerStub.getPort()));
        searchServices.query(queryGuid.bytes(), "susheel");
        Thread.sleep(250);

        // all connected UPs should get a OOB query
        for (int i = 0; i < testUP.length; i++) {
            QueryRequest qr = BlockingConnectionUtils.getFirstQueryRequest(testUP[i]);
            assertNotNull(qr);
            assertEquals(new GUID(qr.getGUID()), queryGuid);
            assertTrue(qr.desiresOutOfBandReplies());
        }

        // now confirm that we follow the OOB protocol
        ReplyNumberVendorMessage vm = 
            replyNumberVendorMessageFactory.create(queryGuid, 10);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        vm.write(baos);
        DatagramPacket pack = new DatagramPacket(baos.toByteArray(), 
                                  baos.toByteArray().length,
                                  testUP[0].getInetAddress(), 
                                  SERVER_PORT);
        UDP_ACCESS.send(pack);

        // we should get a LimeACK in response
        LimeACKVendorMessage ack = null;
        while (ack == null) {
            pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
            }
            catch (IOException bad) {
                fail("Did not get ack", bad);
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            Message m = messageFactory.read(in, Network.TCP);
            if (m instanceof LimeACKVendorMessage)
                ack = (LimeACKVendorMessage) m;
        }
        assertEquals(queryGuid, new GUID(ack.getGUID()));
        assertEquals(10, ack.getNumResults());

        // now stop the query
        searchServices.stopQuery(queryGuid);
        BlockingConnectionUtils.keepAllAlive(testUP, pingReplyFactory);
        drainAll();

        // send another ReplyNumber
        vm = replyNumberVendorMessageFactory.create(queryGuid, 5);
        baos = new ByteArrayOutputStream();
        vm.write(baos);
        pack = new DatagramPacket(baos.toByteArray(), 
                                  baos.toByteArray().length,
                                  testUP[0].getInetAddress(), 
                                  SERVER_PORT);
        UDP_ACCESS.send(pack);

        // we should NOT get a LimeACK in response
        while (true) {
            pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
            }
            catch (InterruptedIOException expected) {
                break;
            }
            catch (IOException bad) {
                bad.printStackTrace();
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            Message m = messageFactory.read(in, Network.TCP);
            if (m instanceof LimeACKVendorMessage)
                assertTrue("we got an ack, weren't supposed to!!", false);
        }

    }


    public void testExpiredQuerySemantics() throws Exception {
        DatagramPacket pack = null;
        // send a query and make sure that after it is expired (i.e. enough
        // results are recieved) we don't request OOB replies for it
        
        // clear up messages before we test.
        BlockingConnectionUtils.keepAllAlive(testUP, pingReplyFactory);

        // first of all, we should confirm that we are sending out a OOB query.
        GUID queryGuid = new GUID(searchServices.newQueryGUID());
        assertTrue(GUID.addressesMatch(queryGuid.bytes(), 
                networkManagerStub.getAddress(), 
                networkManagerStub.getPort()));
        searchServices.query(queryGuid.bytes(), "susheel");
        Thread.sleep(250);

        // all connected UPs should get a OOB query
        for (int i = 0; i < testUP.length; i++) {
            QueryRequest qr = BlockingConnectionUtils.getFirstQueryRequest(testUP[i]);
            assertNotNull(qr);
            assertEquals(new GUID(qr.getGUID()), queryGuid);
            assertTrue(qr.desiresOutOfBandReplies());
        }

        // now confirm that we follow the OOB protocol
        ReplyNumberVendorMessage vm = 
            replyNumberVendorMessageFactory.create(queryGuid, 10);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        vm.write(baos);
        pack = new DatagramPacket(baos.toByteArray(), 
                                  baos.toByteArray().length,
                                  testUP[0].getInetAddress(), 
                                  SERVER_PORT);
        UDP_ACCESS.send(pack);

        // we should get a LimeACK in response
        LimeACKVendorMessage ack = null;
        while (ack == null) {
            pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
            }
            catch (IOException bad) {
                fail("Did not get ack", bad);
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            Message m = messageFactory.read(in, Network.TCP);
            if (m instanceof LimeACKVendorMessage)
                ack = (LimeACKVendorMessage) m;
        }
        assertEquals(queryGuid, new GUID(ack.getGUID()));
        assertEquals(10, ack.getNumResults());

        // now expire the query by routing hundreds of replies back
        int respsPerUP = QueryHandler.ULTRAPEER_RESULTS/testUP.length + 5;
        for (int i = 0; i < testUP.length; i++) {
            Response[] res = new Response[respsPerUP];
            for (int j = 0; j < res.length; j++)
                res[j] = responseFactory.createResponse(10, 10, "susheel"+i+j, UrnHelper.SHA1);
            Message m = 
                queryReplyFactory.createQueryReply(queryGuid.bytes(), (byte) 1, 6355,
                    myIP(), 0, res, GUID.makeGuid(), new byte[0], false, false,
                    true, true, false, false, null);

            testUP[i].send(m);
            testUP[i].flush();
        }
        Thread.sleep(2000); // lets process these results...

        // send another ReplyNumber
        vm = replyNumberVendorMessageFactory.create(queryGuid, 5);
        baos = new ByteArrayOutputStream();
        vm.write(baos);
        pack = new DatagramPacket(baos.toByteArray(), 
                                  baos.toByteArray().length,
                                  testUP[0].getInetAddress(), 
                                  SERVER_PORT);
        UDP_ACCESS.send(pack);

        // we should NOT get a LimeACK in response
        while (true) {
            pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
            }
            catch (InterruptedIOException expected) {
                break;
            }
            catch (IOException bad) {
                bad.printStackTrace();
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            Message m = messageFactory.read(in, Network.TCP);
            if (m instanceof LimeACKVendorMessage)
                assertTrue("we got an ack, weren't supposed to!!", false);
        }

    }

    public void testFirewalledReplyLogic() throws Exception {

        // one of the UPs should send a PushProxyAck cuz we don't send the 'FW'
        // header unless we have proxies
        PushProxyAcknowledgement ppAck = 
            new PushProxyAcknowledgement(InetAddress.getLocalHost(), 
                                         testUP[2].getPort(),
                                         new GUID(applicationServices.getMyGUID()));
        testUP[2].send(ppAck); testUP[2].flush();

        { // this should not go through because of firewall/firewall
            BlockingConnectionUtils.drain(testUP[0]);

            QueryRequest query = 
                queryRequestFactory.createQueryRequest(GUID.makeGuid(),
                    (byte)2, 0 | QueryRequest.SPECIAL_MINSPEED_MASK |
                     QueryRequest.SPECIAL_FIREWALL_MASK, "berkeley", "", null, null,
                    false, Network.UNKNOWN, false, 0, false, 0);

            testUP[0].send(query);testUP[0].flush();
            QueryReply reply = BlockingConnectionUtils.getFirstQueryReply(testUP[0]);
            assertNull(reply);
        }

        { // this should go through because of firewall transfer/solicited
            BlockingConnectionUtils.drain(testUP[0]);

            QueryRequest query = 
                queryRequestFactory.createQueryRequest(GUID.makeGuid(),
                    (byte)2, 0 | QueryRequest.SPECIAL_MINSPEED_MASK |
                     QueryRequest.SPECIAL_FIREWALL_MASK |
                     QueryRequest.SPECIAL_FWTRANS_MASK, "susheel", "", null, null,
                    false, Network.UNKNOWN, false, 0, false, 0);
            assertTrue(query.canDoFirewalledTransfer());

            testUP[0].send(query);testUP[0].flush();
            QueryReply reply = BlockingConnectionUtils.getFirstQueryReply(testUP[0]);
            assertNotNull(reply);
            assertTrue(reply.getSupportsFWTransfer());
        }

        { // this should go through because the source isn't firewalled
            BlockingConnectionUtils.drain(testUP[1]);

            QueryRequest query = 
                queryRequestFactory.createQueryRequest(GUID.makeGuid(),
                    (byte)2, 0 | QueryRequest.SPECIAL_MINSPEED_MASK |
                     QueryRequest.SPECIAL_XML_MASK, "susheel", "", null, null,
                    false, Network.UNKNOWN, false, 0, false, 0);

            testUP[1].send(query);testUP[1].flush();
            QueryReply reply = BlockingConnectionUtils.getFirstQueryReply(testUP[1]);
            assertNotNull(reply);
            assertFalse(reply.getSupportsFWTransfer());
        }

        // set test client to non-firewalled
        networkManagerStub.setAcceptedIncomingConnection(true);
        { // this should go through because test node is not firewalled
            BlockingConnectionUtils.drain(testUP[2]);

            QueryRequest query = 
                queryRequestFactory.createQueryRequest(GUID.makeGuid(),
                    (byte)2, 0 | QueryRequest.SPECIAL_MINSPEED_MASK |
                     QueryRequest.SPECIAL_FIREWALL_MASK |
                     QueryRequest.SPECIAL_FWTRANS_MASK, "susheel", "", null, null,
                    false, Network.UNKNOWN, false, 0, false, 0);

            testUP[2].send(query);testUP[2].flush();
            QueryReply reply = BlockingConnectionUtils.getFirstQueryReply(testUP[2]);
            assertNotNull(reply);
            assertFalse(reply.getSupportsFWTransfer());
        }
    }
    
    public void testNoRNVMSent() throws Exception {
        SearchSettings.DISABLE_OOB_V2.setBoolean(true);
    	drainAll();

        // first of all, we should confirm that we are sending out a OOB query.
        GUID queryGuid = new GUID(searchServices.newQueryGUID());
        assertTrue(GUID.addressesMatch(queryGuid.bytes(), 
                networkManagerStub.getAddress(), 
                networkManagerStub.getPort()));
        searchServices.query(queryGuid.bytes(), "susheel");
        Thread.sleep(250);

        // all connected UPs should get a OOB query
        for (int i = 0; i < testUP.length; i++) {
            QueryRequest qr = BlockingConnectionUtils.getFirstQueryRequest(testUP[i]);
            assertNotNull(qr);
            assertEquals(new GUID(qr.getGUID()), queryGuid);
            assertTrue(qr.desiresOutOfBandReplies());
        }

        // now, do not send an RNVM and send a reply directly
        Response[] res = new Response[10];
        for (int j = 0; j < res.length; j++)
            res[j] = responseFactory.createResponse(10, 10, "susheel"+j, UrnHelper.SHA1);
        Message m = 
            queryReplyFactory.createQueryReply(queryGuid.bytes(), (byte) 1, 6355,
                myIP(), 0, res, GUID.makeGuid(), new byte[0], false, false,
                true, true, false, false, null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        m.write(baos);
        byte [] packet = baos.toByteArray();
        DatagramPacket reply = new DatagramPacket(packet, 
        		packet.length,
        		testUP[0].getInetAddress(), 
        		SERVER_PORT);
        UDP_ACCESS.send(reply);
        
        // nothing should be accepted.
        Thread.sleep(250);
        SearchResultHandler handler = searchResultHandler;
        assertEquals(0,handler.getNumResultsForQuery(queryGuid));
    }
    
    
    //////////////////////////////////////////////////////////////////

    @Override
    public int getNumberOfPeers() {
        return 3;
    }

    private static byte[] myIP() {
        return new byte[] { (byte)127, (byte)0, 0, 1 };
    }

}

