package com.limegroup.gnutella;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;

import junit.framework.Test;

import org.limewire.core.settings.MessageSettings;
import org.limewire.core.settings.SearchSettings;
import org.limewire.io.GUID;
import org.limewire.security.AddressSecurityToken;
import org.limewire.security.MACCalculatorRepositoryManager;
import org.limewire.security.SecurityToken;
import org.limewire.util.ByteUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.PrivilegedAccessor;
import org.limewire.util.TestUtils;

import com.google.inject.Injector;
import com.google.inject.Stage;
import com.limegroup.gnutella.helpers.UrnHelper;
import com.limegroup.gnutella.messagehandlers.OOBSecurityToken;
import com.limegroup.gnutella.messagehandlers.OOBSecurityToken.OOBTokenData;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.messages.vendor.AbstractVendorMessage;
import com.limegroup.gnutella.messages.vendor.LimeACKVendorMessage;
import com.limegroup.gnutella.messages.vendor.ReplyNumberVendorMessage;
import com.limegroup.gnutella.messages.vendor.VendorMessage;
import com.limegroup.gnutella.messages.vendor.VendorMessageFactory;
import com.limegroup.gnutella.messages.vendor.VendorMessageFactory.VendorMessageParser;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.routing.RouteTableMessage;
import com.limegroup.gnutella.stubs.NetworkManagerStub;

/**
 *  Tests that an Ultrapeer correctly handles out-of-band queries.  Essentially 
 *  tests the following methods of MessageRouter: handleQueryRequest,
 *  handleUDPMessage, handleLimeAckMessage, Expirer, QueryBundle, sendQueryReply
 *
 *  ULTRAPEER_1  ----  CENTRAL TEST ULTRAPEER  ----  ULTRAPEER[1]
 *                              |
 *                              |
 *                              |
 *                             LEAF[0]
 *
 *  This test should cover the case for leaves too, since there is no difference
 *  between Leaf and UP when it comes to this behavior.
 */
@SuppressWarnings("null")
public final class ServerSideOutOfBandReplyTest extends ServerSideTestCase {

    protected static int TIMEOUT = 2000;

    static String _path = "com/limegroup/gnutella/";

    static File _fileWithSOFlag = TestUtils.getResourceFile(_path + "queryWithSOFlag.bin");
    
    static File _fileWithSO = TestUtils.getResourceFile(_path + "queryWithSO.bin");
    
    /**
     * Ultrapeer 1 UDP connection.
     */
    private volatile static DatagramSocket UDP_ACCESS;
    
    private volatile boolean v2disabled;

    private NetworkManagerStub networkManagerStub;

    private QueryRequestFactory queryRequestFactory;

    private MessageFactory messageFactory;

    private VendorMessageFactory vendorMessageFactory;

    private MACCalculatorRepositoryManager macManager;
    
    public ServerSideOutOfBandReplyTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ServerSideOutOfBandReplyTest.class);
    }    
   
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

	@Override
	public int getNumberOfUltrapeers() {
	    return 3;
    }

	@Override
	public int getNumberOfLeafpeers() {
	    return 1;
    }
	
	@Override
    public void setUpQRPTables() throws Exception {
        QueryRouteTable qrt = new QueryRouteTable();
        qrt.add("berkeley");
        qrt.add("susheel");
        qrt.addIndivisible(UrnHelper.UNIQUE_SHA1.toString());
        for (Iterator iter=qrt.encode(null).iterator(); iter.hasNext(); ) {
            LEAF[0].send((RouteTableMessage)iter.next());
			LEAF[0].flush();
        }

        // for Ultrapeer 1
        qrt = new QueryRouteTable();
        qrt.add("leehsus");
        qrt.add("berkeley");
        for (Iterator iter=qrt.encode(null).iterator(); iter.hasNext(); ) {
            ULTRAPEER[0].send((RouteTableMessage)iter.next());
			ULTRAPEER[0].flush();
        }
    }

    @Override
    protected void setUp() throws Exception {
        networkManagerStub = new NetworkManagerStub();
        Injector injector = LimeTestUtils.createInjector(Stage.PRODUCTION, new LimeTestUtils.NetworkManagerStubModule(networkManagerStub));
        super.setUp(injector);

        queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
        messageFactory = injector.getInstance(MessageFactory.class);
        vendorMessageFactory = injector.getInstance(VendorMessageFactory.class);
        macManager = injector.getInstance(MACCalculatorRepositoryManager.class);
        
        networkManagerStub.setAcceptedIncomingConnection(true);
        networkManagerStub.setCanReceiveSolicited(true);
        networkManagerStub.setCanReceiveUnsolicited(true);
        networkManagerStub.setOOBCapable(true);
        
        UDP_ACCESS = new DatagramSocket(10000);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        if (UDP_ACCESS != null) {
            UDP_ACCESS.close();
        }
    }

    // tests basic out of band functionality
    // this tests solicited UDP support - it should participate in ACK exchange
    public void testBasicOutOfBandRequest() throws Exception {
        drain();

        QueryRequest query = 
            queryRequestFactory.createOutOfBandQuery("txt",
                                              InetAddress.getLocalHost().getAddress(),
                                              UDP_ACCESS.getLocalPort());
        assertTrue(query.desiresOutOfBandReplies());
        assertNotEquals(v2disabled, query.desiresOutOfBandRepliesV2());
        assertTrue(query.desiresOutOfBandRepliesV3());
        
        query.hop();

        // we needed to hop the message because we need to make it seem that it
        // is from sufficiently far away....
        ULTRAPEER[0].send(query);
        ULTRAPEER[0].flush();

        // we should get a ReplyNumberVendorMessage via UDP - we'll get an
        // interrupted exception if not
        Message message = null;
        while (!(message instanceof ReplyNumberVendorMessage)) {
            UDP_ACCESS.setSoTimeout(500);
            DatagramPacket pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
            }
            catch (IOException bad) {
                fail("Did not get VM", bad);
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            message = messageFactory.read(in, Network.TCP);

            // we should NOT get a reply to our query
            assertTrue(!((message instanceof QueryReply) &&
                         (Arrays.equals(message.getGUID(), query.getGUID()))));
        }

        // make sure the GUID is correct
        assertTrue(Arrays.equals(query.getGUID(), message.getGUID()));
        ReplyNumberVendorMessage reply = (ReplyNumberVendorMessage) message;
        assertEquals(2, reply.getNumResults());
        // test that we receive new version
        assertEquals(3, reply.getVersion());
        assertTrue(reply.canReceiveUnsolicited());
        
        // make sure we get no further RNVMs
        try {
            UDP_ACCESS.setSoTimeout(200);
            DatagramPacket pack = new DatagramPacket(new byte[1000], 1000);
            UDP_ACCESS.receive(pack);
            fail("got something");
        } catch (IOException expected){}
        
        //rince and repeat, this time pretend to be firewalled
        
        query = 
            queryRequestFactory.createOutOfBandQuery("txt",
                                              InetAddress.getLocalHost().getAddress(),
                                              UDP_ACCESS.getLocalPort());
        assertTrue(query.desiresOutOfBandReplies());
        assertNotEquals(v2disabled,query.desiresOutOfBandRepliesV2());
        assertTrue(query.desiresOutOfBandRepliesV3());
        
        query.hop();
        
        networkManagerStub.setCanReceiveUnsolicited(false);
        
        assertTrue(query.desiresOutOfBandReplies());

        // we needed to hop the message because we need to make it seem that it
        // is from sufficiently far away....
        ULTRAPEER[1].send(query);
        ULTRAPEER[1].flush();

        // we should get a ReplyNumberVendorMessage via UDP - we'll get an
        // interrupted exception if not
        message = null;
        while (!(message instanceof ReplyNumberVendorMessage)) {
            UDP_ACCESS.setSoTimeout(500);
            DatagramPacket pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
            }
            catch (IOException bad) {
                fail("Did not get VM", bad);
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            message = messageFactory.read(in, Network.TCP);

            // we should NOT get a reply to our query
            assertTrue(!((message instanceof QueryReply) &&
                         (Arrays.equals(message.getGUID(), query.getGUID()))));
        }

        // make sure the GUID is correct
        assertTrue(Arrays.equals(query.getGUID(), message.getGUID()));
        reply = (ReplyNumberVendorMessage) message;
        assertEquals(2, reply.getNumResults());
        assertFalse(reply.canReceiveUnsolicited());
        // test that we receive new version
        assertEquals(3, reply.getVersion());
        
        //restore our un-firewalled status and repeat
        query = 
            queryRequestFactory.createOutOfBandQuery("txt",
                                              InetAddress.getLocalHost().getAddress(),
                                              UDP_ACCESS.getLocalPort());
        query.hop();
        
        networkManagerStub.setCanReceiveUnsolicited(true);
        
        ULTRAPEER[2].send(query);
        ULTRAPEER[2].flush();

        // we should get a ReplyNumberVendorMessage via UDP - we'll get an
        // interrupted exception if not
        message = null;
        DatagramPacket pack = new DatagramPacket(new byte[1000], 1000);
        while (!(message instanceof ReplyNumberVendorMessage)) {
            UDP_ACCESS.setSoTimeout(500);
            
            try {
                UDP_ACCESS.receive(pack);
            }
            catch (IOException bad) {
                fail("Did not get VM", bad);
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            message = messageFactory.read(in, Network.TCP);

            // we should NOT get a reply to our query
            assertTrue(!((message instanceof QueryReply) &&
                         (Arrays.equals(message.getGUID(), query.getGUID()))));
        }

        // make sure the GUID is correct
        assertTrue(Arrays.equals(query.getGUID(), message.getGUID()));
        reply = (ReplyNumberVendorMessage) message;
        assertEquals(2, reply.getNumResults());
        assertTrue(reply.canReceiveUnsolicited());
        // test that we receive new version
        assertEquals(3, reply.getVersion());
        
        SecurityToken token = new OOBSecurityToken(new OOBTokenData(pack.getAddress(), 
                pack.getPort(), reply.getGUID(), reply.getNumResults()), macManager); 
        
        // ok - we should ACK the ReplyNumberVM
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        LimeACKVendorMessage ack = 
            new LimeACKVendorMessage(new GUID(message.getGUID()),
                                     reply.getNumResults(), token);
        ack.write(baos);
        pack = new DatagramPacket(baos.toByteArray(), baos.toByteArray().length,
                                  pack.getAddress(), pack.getPort());
        UDP_ACCESS.send(pack);

        // now we should get TWO replies, each with one response
        //1)
        while (!(message instanceof QueryReply)) {
            UDP_ACCESS.setSoTimeout(500);
            pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
            }
            catch (IOException bad) {
                fail("Did not get reply", bad);
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            // as long as we don't get a ClassCastException we are good to go
            message = messageFactory.read(in, Network.TCP);
            
            // should not be receiving redundant RNVMs in this test
            assertNotInstanceof(ReplyNumberVendorMessage.class, message);
        }
        // make sure this is the correct QR
        assertTrue(Arrays.equals(message.getGUID(), ack.getGUID()));
        assertEquals(1, ((QueryReply)message).getResultCount());
        assertEquals(token.getBytes(), ((QueryReply)message).getSecurityToken());

        byte[] receivedTokenBytes = ((QueryReply)message).getSecurityToken(); 
        assertNotNull(receivedTokenBytes);
        SecurityToken receivedToken = new OOBSecurityToken(receivedTokenBytes, macManager);
        assertTrue(receivedToken.isFor(new OOBTokenData(pack.getAddress(),
                pack.getPort(), message.getGUID(), receivedToken.getBytes()[0] & 0xFF)));


        //2) null out 'message' so we can get the next reply....
        message = null;
        while (!(message instanceof QueryReply)) {
            UDP_ACCESS.setSoTimeout(500);
            pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
            }
            catch (IOException bad) {
                fail("Did not get reply", bad);
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            // as long as we don't get a ClassCastException we are good to go
            message = messageFactory.read(in, Network.TCP);
        }
        // make sure this is the correct QR
        assertTrue(Arrays.equals(message.getGUID(), ack.getGUID()));
        assertEquals(1, ((QueryReply)message).getResultCount());
        // security token is null for old protocol version
        assertEquals(token.getBytes(), ((QueryReply)message).getSecurityToken());

        // make sure that if we send the ACK we don't get another reply - this
        // is current policy but we may want to change it in the future
        baos = new ByteArrayOutputStream();
        ack.write(baos);
        pack = new DatagramPacket(baos.toByteArray(), baos.toByteArray().length,
                                  pack.getAddress(), pack.getPort());
        UDP_ACCESS.send(pack);

        // now we should NOT get the reply!
        try {
            while (true) {
                UDP_ACCESS.setSoTimeout(500);
                pack = new DatagramPacket(new byte[1000], 1000);
                UDP_ACCESS.receive(pack);
                InputStream in = new ByteArrayInputStream(pack.getData());
                message = messageFactory.read(in, Network.TCP);
                assertTrue(!((message instanceof QueryReply) &&
                             (Arrays.equals(message.getGUID(), 
                                            ack.getGUID()))));
            }
        }
        catch (IOException expected) {}


    }
    
    /**
     * tests that we send redundant RNVMs if setting is set 
     */
    public void testRedundantRNVMs() throws Exception {
        MessageSettings.OOB_REDUNDANCY.setValue(true);
        drain();

        QueryRequest query = 
            queryRequestFactory.createOutOfBandQuery("txt",
                                              InetAddress.getLocalHost().getAddress(),
                                              UDP_ACCESS.getLocalPort());
        assertTrue(query.desiresOutOfBandReplies());
        assertNotEquals(v2disabled, query.desiresOutOfBandRepliesV2());
        assertTrue(query.desiresOutOfBandRepliesV3());
        
        query.hop();

        // we needed to hop the message because we need to make it seem that it
        // is from sufficiently far away....
        ULTRAPEER[0].send(query);
        ULTRAPEER[0].flush();

        // we should get a ReplyNumberVendorMessage via UDP - we'll get an
        // interrupted exception if not
        int RNVMs = 0;
        while (RNVMs < 2) {
            UDP_ACCESS.setSoTimeout(500);
            DatagramPacket pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
            }
            catch (IOException bad) {
                fail("Did not get VM", bad);
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            Message message = messageFactory.read(in, Network.TCP);

            // we should get an RNVM
            assertInstanceof(ReplyNumberVendorMessage.class, message);
            RNVMs++;
        }
    }
    
    /**
     * tests that receiving duplicate ACKs will not result in sending
     * results twice 
     */
    public void testDuplicateACKs() throws Exception {
        drain();

        QueryRequest query = 
            queryRequestFactory.createOutOfBandQuery("txt",
                                              InetAddress.getLocalHost().getAddress(),
                                              UDP_ACCESS.getLocalPort());
        assertTrue(query.desiresOutOfBandReplies());
        assertNotEquals(v2disabled, query.desiresOutOfBandRepliesV2());
        assertTrue(query.desiresOutOfBandRepliesV3());
        
        query.hop();

        // we needed to hop the message because we need to make it seem that it
        // is from sufficiently far away....
        ULTRAPEER[0].send(query);
        ULTRAPEER[0].flush();

        // we should get a ReplyNumberVendorMessage via UDP - we'll get an
        // interrupted exception if not
        Message message = null;
        DatagramPacket pack = null;
        while (!(message instanceof ReplyNumberVendorMessage)) {
            UDP_ACCESS.setSoTimeout(500);
            pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
            }
            catch (IOException bad) {
                fail("Did not get VM", bad);
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            message = messageFactory.read(in, Network.TCP);

            // we should NOT get a reply to our query
            assertTrue(!((message instanceof QueryReply) &&
                         (Arrays.equals(message.getGUID(), query.getGUID()))));
        }

        // prepare an ACK
        ReplyNumberVendorMessage reply = (ReplyNumberVendorMessage)message;
        SecurityToken token = new OOBSecurityToken(new OOBTokenData(pack.getAddress(), 
                pack.getPort(), reply.getGUID(), reply.getNumResults()), macManager);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        LimeACKVendorMessage ack = 
            new LimeACKVendorMessage(new GUID(message.getGUID()),
                                     reply.getNumResults(), token);
        ack.write(baos);
        pack = new DatagramPacket(baos.toByteArray(), baos.toByteArray().length,
                                  pack.getAddress(), pack.getPort());
        UDP_ACCESS.send(pack);
        
        // send a duplicate ACK
        Thread.sleep(50);
        UDP_ACCESS.send(pack);
        
        // now we should get TWO replies, each with one response
        //1)
        int replies = 0;
        try {
        while (true) {
            UDP_ACCESS.setSoTimeout(500);
            DatagramPacket pack2 = new DatagramPacket(new byte[1000], 1000);
            UDP_ACCESS.receive(pack2);
            InputStream in = new ByteArrayInputStream(pack2.getData());
            // as long as we don't get a ClassCastException we are good to go
            message = messageFactory.read(in, Network.TCP);
            
            assertInstanceof(QueryReply.class,message);
            replies++;
        }
        } catch (IOException expected) {}
        assertEquals(2, replies);
        
        // send another duplicate ACK
        UDP_ACCESS.send(pack);
        
        // no replies though
        try {
            UDP_ACCESS.receive(pack);
            fail("received something");
        } catch (IOException expected){}
    }
    
    public void testBasicOutOfBandRequestV3Only() throws Exception {
        SearchSettings.DISABLE_OOB_V2.setValue(1);
        v2disabled = true;
        testBasicOutOfBandRequest();
        SearchSettings.DISABLE_OOB_V2.setValue(0);
        v2disabled = false;
    }
    
    private void drain() throws Exception {
        drainAll();
        try {
            UDP_ACCESS.setSoTimeout(10);
            while(true) {
                DatagramPacket pack = new DatagramPacket(new byte[1000],1000);
                UDP_ACCESS.receive(pack);
            }
        } catch (IOException iox){}
    }
    /**
     * Test server still handles OOB protocol version 2 for old clients.
     * 
     *  The test is a bit cumbersome in that we have to install an extra
     *  ReplyNumberVendorMessage parser since version 3 discards the old
     *  protocol versions. 
     */
    public void testServerHandlesOOBVersion2() throws Exception {
        
        drain();
        DatagramPacket pack = null;

        // install extra message parse since old reply number messages are discarded
        VendorMessageFactory factory = vendorMessageFactory;
        VendorMessageParser oldParser = factory.getParser(VendorMessage.F_REPLY_NUMBER, VendorMessage.F_LIME_VENDOR_ID);
        try {
            factory.setParser(VendorMessage.F_REPLY_NUMBER, VendorMessage.F_LIME_VENDOR_ID, new V2ReplyNumberVendorMessageParser());
            
            // raw message without "SO" key, query = "shusheel"
            byte[] rawMessage =  new byte[] { 127, 0, 1, 1, 11, 71, -79, -94, 4, 47, 
                    27, 72, -81, 112, 7, 0, -128, 6, 0, 15, 0, 0, 0, -60, 0, 115, 
                    117, 115, 104, 101, 101, 108, 0, 117, 114, 110, 58, 0 };
            
            // encode this host's address into message payload
            System.arraycopy(InetAddress.getLocalHost().getAddress(), 0, rawMessage, 0, 4);
            ByteUtils.short2leb((short)UDP_ACCESS.getLocalPort(), rawMessage, 13);
            
            QueryRequest query = (QueryRequest)messageFactory.read(new ByteArrayInputStream(rawMessage), Network.TCP);
            assertTrue(query.desiresOutOfBandReplies());
            assertTrue(query.desiresOutOfBandRepliesV2());
            assertFalse(query.desiresOutOfBandRepliesV3());
            
            query.hop();
            
            // we needed to hop the message because we need to make it seem that it
            // is from sufficiently far away....
            ULTRAPEER[1].send(query);
            ULTRAPEER[1].flush();
            
            // we should get a ReplyNumberVendorMessage via UDP - we'll get an
            // interrupted exception if not
            Message message = null;
            while (!(message instanceof V2ReplyNumberVendorMessage)) {
                UDP_ACCESS.setSoTimeout(500);
                pack = new DatagramPacket(new byte[1000], 1000);
                try {
                    UDP_ACCESS.receive(pack);
                }
                catch (IOException bad) {
                    fail("Did not get VM", bad);
                }
                InputStream in = new ByteArrayInputStream(pack.getData());
                // as long as we don't get a ClassCastException we are good to go
                message = messageFactory.read(in, Network.TCP);
            }
            
            // make sure the GUID is correct
            assertEquals(query.getGUID(), message.getGUID());
            V2ReplyNumberVendorMessage reply = (V2ReplyNumberVendorMessage) message;
            // since query supports only OOB version 2, reply should be version 2
            assertEquals(2, reply.getVersion());
            
            // ok - we should ACK the ReplyNumberVM and NOT get a reply
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            // send version 2 ack message
            LimeACKVendorMessage ack = 
                new LimeACKVendorMessage(new GUID(message.getGUID()), 
                        1); // we get one result so ask for it
            ack.write(baos);
            pack = new DatagramPacket(baos.toByteArray(), baos.toByteArray().length,
                    pack.getAddress(), pack.getPort());
            UDP_ACCESS.send(pack);
            
            while (!(message instanceof QueryReply)) {
                UDP_ACCESS.setSoTimeout(500);
                pack = new DatagramPacket(new byte[1000], 1000);
                try {
                    UDP_ACCESS.receive(pack);
                }
                catch (IOException bad) {
                    fail("Did not get VM", bad);
                }
                InputStream in = new ByteArrayInputStream(pack.getData());
                // as long as we don't get a ClassCastException we are good to go
                message = messageFactory.read(in, Network.TCP);
            }
            
            QueryReply qReply = (QueryReply)message;
            assertEquals(query.getGUID(), qReply.getGUID());
            assertNull(qReply.getSecurityToken());
        }
        finally {
            factory.setParser(VendorMessage.F_REPLY_NUMBER, VendorMessage.F_LIME_VENDOR_ID, oldParser);
        }
    }
    
    /**
     *  Test that a message is correctly handled that has both set, 
     *  the OOB mask in the minspeed field and the "SO" key in the GGEP.
     */
    public void testSORequestAndMinSpeedOOBMask() throws Exception {
        drain();
        
        byte[] rawMessage = FileUtils.readFileFully(_fileWithSOFlag);
        // encode this host's address into message payload
        System.arraycopy(InetAddress.getLocalHost().getAddress(), 0, rawMessage, 0, 4);
        ByteUtils.short2leb((short)UDP_ACCESS.getLocalPort(), rawMessage, 13);
        
        DatagramPacket pack = null;

        QueryRequest query = (QueryRequest)messageFactory.read(new ByteArrayInputStream(rawMessage), Network.TCP);
        assertTrue(query.desiresOutOfBandReplies());
        assertTrue(query.desiresOutOfBandRepliesV2());
        assertTrue(query.desiresOutOfBandRepliesV3());
        
        query.hop();

        // we needed to hop the message because we need to make it seem that it
        // is from sufficiently far away....
        ULTRAPEER[1].send(query);
        ULTRAPEER[1].flush();

        // we should get a ReplyNumberVendorMessage via UDP - we'll get an
        // interrupted exception if not
        Message message = null;
        while (!(message instanceof ReplyNumberVendorMessage)) {
            UDP_ACCESS.setSoTimeout(500);
            pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
            }
            catch (IOException bad) {
                fail("Did not get VM", bad);
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            // as long as we don't get a ClassCastException we are good to go
            message = messageFactory.read(in, Network.TCP);
        }

        // make sure the GUID is correct
        assertEquals(query.getGUID(), message.getGUID());
        ReplyNumberVendorMessage reply = (ReplyNumberVendorMessage) message;
        assertEquals(1, reply.getNumResults());
        // since query supports new OOB version, reply should prefer that version
        assertEquals(3, reply.getVersion());

        // ok - we should ACK the ReplyNumberVM and NOT get a reply
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SecurityToken token = new AddressSecurityToken(InetAddress.getLocalHost(), 10000, macManager);
        LimeACKVendorMessage ack = 
            new LimeACKVendorMessage(new GUID(message.getGUID()), 
                                     reply.getNumResults(), token);
        ack.write(baos);
        pack = new DatagramPacket(baos.toByteArray(), baos.toByteArray().length,
                                  pack.getAddress(), pack.getPort());
        UDP_ACCESS.send(pack);

        while (!(message instanceof QueryReply)) {
            UDP_ACCESS.setSoTimeout(500);
            pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
            }
            catch (IOException bad) {
                fail("Did not get VM", bad);
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            // as long as we don't get a ClassCastException we are good to go
            message = messageFactory.read(in, Network.TCP);
        }

        QueryReply qReply = (QueryReply)message;
        assertEquals(query.getGUID(), qReply.getGUID());
        assertEquals(token.getBytes(), qReply.getSecurityToken());
    }
    
    /**
     * Tests protocol version 3, this is actually done in all other
     * tests, this test is part of the transition phase when the server
     * also handled the version 3 , but the client couldn't produce 
     * messages with that version. 
     */
    public void testSORequestNoMinSpeedOOBMask() throws Exception {
        drain();
        
        byte[] rawMessage = FileUtils.readFileFully(_fileWithSO);
        // enocode
        System.arraycopy(InetAddress.getLocalHost().getAddress(), 0, rawMessage, 0, 4);
        ByteUtils.short2leb((short)UDP_ACCESS.getLocalPort(), rawMessage, 13);
        
        
        DatagramPacket pack = null;

        QueryRequest query = (QueryRequest)messageFactory.read(new ByteArrayInputStream(rawMessage), Network.TCP);
        assertTrue(query.desiresOutOfBandReplies());
        assertFalse(query.desiresOutOfBandRepliesV2());
        assertTrue(query.desiresOutOfBandRepliesV3());
        
        query.hop();

        // we needed to hop the message because we need to make it seem that it
        // is from sufficiently far away....
        ULTRAPEER[1].send(query);
        ULTRAPEER[1].flush();

        // we should get a ReplyNumberVendorMessage via UDP - we'll get an
        // interrupted exception if not
        Message message = null;
        while (!(message instanceof ReplyNumberVendorMessage)) {
            UDP_ACCESS.setSoTimeout(500);
            pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
            }
            catch (IOException bad) {
                fail("Did not get VM", bad);
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            // as long as we don't get a ClassCastException we are good to go
            message = messageFactory.read(in, Network.TCP);
        }

        // make sure the GUID is correct
        assertEquals(query.getGUID(), message.getGUID());
        ReplyNumberVendorMessage reply = (ReplyNumberVendorMessage) message;
        assertEquals(1, reply.getNumResults());
        // query is signals protocol version 3 by setting "SO" key, so server
        // should respond with version 3
        assertEquals(3, reply.getVersion());

        // ok - we should ACK the ReplyNumberVM and NOT get a reply
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SecurityToken token = new AddressSecurityToken(InetAddress.getLocalHost(), 10000, macManager);
        LimeACKVendorMessage ack = 
            new LimeACKVendorMessage(new GUID(message.getGUID()), 
                                     reply.getNumResults(), token);
        ack.write(baos);
        pack = new DatagramPacket(baos.toByteArray(), baos.toByteArray().length,
                                  pack.getAddress(), pack.getPort());
        UDP_ACCESS.send(pack);

        while (!(message instanceof QueryReply)) {
            UDP_ACCESS.setSoTimeout(500);
            pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
            }
            catch (IOException bad) {
                fail("Did not get VM", bad);
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            // as long as we don't get a ClassCastException we are good to go
            message = messageFactory.read(in, Network.TCP);
        }

        QueryReply qReply = (QueryReply)message;
        assertEquals(query.getGUID(), qReply.getGUID());
        assertEquals(token.getBytes(), qReply.getSecurityToken());
    }
    

    // makes sure that if we ack back with less results than what the server
    // has we only get back what we asked for.
    public void testPiecemealOutOfBandResults() throws Exception {
        DatagramPacket pack = null;

        drain();

        QueryRequest query = 
            queryRequestFactory.createOutOfBandQuery("txt",
                                              InetAddress.getLocalHost().getAddress(),
                                              UDP_ACCESS.getLocalPort());
        query.hop();

        // we needed to hop the message because we need to make it seem that it
        // is from sufficiently far away....
        ULTRAPEER[0].send(query);
        ULTRAPEER[0].flush();

        // we should get a ReplyNumberVendorMessage via UDP - we'll get an
        // interrupted exception if not
        Message message = null;
        while (!(message instanceof ReplyNumberVendorMessage)) {
            UDP_ACCESS.setSoTimeout(500);
            pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
            }
            catch (IOException bad) {
                fail("Did not get VM", bad);
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            message = messageFactory.read(in, Network.TCP);
            // we should NOT get a reply to our query
            assertTrue(!((message instanceof QueryReply) &&
                         (Arrays.equals(message.getGUID(), query.getGUID()))));
        }

        // make sure the GUID is correct
        assertTrue(Arrays.equals(query.getGUID(), message.getGUID()));
        ReplyNumberVendorMessage reply = (ReplyNumberVendorMessage) message;
        assertEquals(2, reply.getNumResults());
        // expect version 3 in replies
        assertEquals(3, reply.getVersion());

        // ok - we should ACK the ReplyNumberVM
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        OOBSecurityToken token = new OOBSecurityToken(new OOBTokenData(pack.getAddress(), 
                pack.getPort(), reply.getGUID(), 1), macManager);
        LimeACKVendorMessage ack = 
            new LimeACKVendorMessage(new GUID(message.getGUID()), 1, token);
        ack.write(baos);
        pack = new DatagramPacket(baos.toByteArray(), baos.toByteArray().length,
                                  pack.getAddress(), pack.getPort());
        UDP_ACCESS.send(pack);

        // now we should get the reply!
        while (!(message instanceof QueryReply)) {
            UDP_ACCESS.setSoTimeout(500);
            pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
            }
            catch (IOException bad) {
                fail("Did not get reply", bad);
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            // as long as we don't get a ClassCastException we are good to go
            message = messageFactory.read(in, Network.TCP);
        }
        // make sure this is the correct QR
        assertTrue(Arrays.equals(message.getGUID(), ack.getGUID()));
        assertEquals(1, ((QueryReply)message).getResultCount());
        byte[] receivedTokenBytes = ((QueryReply)message).getSecurityToken(); 
        assertNotNull(receivedTokenBytes);
        SecurityToken receivedToken = new OOBSecurityToken(receivedTokenBytes, macManager);
        assertTrue(receivedToken.isFor(new OOBTokenData(pack.getAddress(),
                pack.getPort(), message.getGUID(), receivedToken.getBytes()[0] & 0xFF)));
        assertEquals(1, receivedToken.getBytes()[0] & 0XFF);

        // make sure that if we send the ACK we don't get another reply - this
        // is current policy but we may want to change it in the future
        baos = new ByteArrayOutputStream();
        ack.write(baos);
        pack = new DatagramPacket(baos.toByteArray(), baos.toByteArray().length,
                                  pack.getAddress(), pack.getPort());
        UDP_ACCESS.send(pack);

        // now we should NOT get the reply!
        try {
            while (true) {
                UDP_ACCESS.setSoTimeout(500);
                pack = new DatagramPacket(new byte[1000], 1000);
                UDP_ACCESS.receive(pack);
                InputStream in = new ByteArrayInputStream(pack.getData());
                message = messageFactory.read(in, Network.TCP);
                assertTrue(!((message instanceof QueryReply) &&
                             (Arrays.equals(message.getGUID(), 
                                            ack.getGUID()))));
            }
        }
        catch (IOException expected) {}
    }

    // makes sure that if we ACK back with a 0 for the number of results we
    // don't get a query reply
    public void testSimpleACKOutOfBandResults() throws Exception {
        DatagramPacket pack = null;

        drain();

        QueryRequest query = 
            queryRequestFactory.createOutOfBandQuery("txt",
                                              InetAddress.getLocalHost().getAddress(),
                                              UDP_ACCESS.getLocalPort());
        query.hop();

        // we needed to hop the message because we need to make it seem that it
        // is from sufficiently far away....
        ULTRAPEER[0].send(query);
        ULTRAPEER[0].flush();

        // we should get a ReplyNumberVendorMessage via UDP - we'll get an
        // interrupted exception if not
        Message message = null;
        while (!(message instanceof ReplyNumberVendorMessage)) {
            UDP_ACCESS.setSoTimeout(500);
            pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
            }
            catch (IOException bad) {
                fail("Did not get VM", bad);
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            message = messageFactory.read(in, Network.TCP);
            // we should NOT get a reply to our query
            assertTrue(!((message instanceof QueryReply) &&
                         (Arrays.equals(message.getGUID(), query.getGUID()))));
        }

        // make sure the GUID is correct
        assertTrue(Arrays.equals(query.getGUID(), message.getGUID()));
        ReplyNumberVendorMessage reply = (ReplyNumberVendorMessage) message;
        assertEquals(2, reply.getNumResults());
        assertEquals(3, reply.getVersion());

        // ok - we should ACK the ReplyNumberVM
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OOBSecurityToken token = new OOBSecurityToken(new OOBTokenData(pack.getAddress(), 
                pack.getPort(), reply.getGUID(), 0), macManager);
        // constructor doesn't allow 0 responses anymore, so set 0 manually:
        LimeACKVendorMessage ack = new LimeACKVendorMessage(new GUID(message.getGUID()), 1, token);
        ((byte[])PrivilegedAccessor.getValue(ack, "_payload"))[0] = 0;
        ack.write(baos);
        pack = new DatagramPacket(baos.toByteArray(), baos.toByteArray().length,
                                  pack.getAddress(), pack.getPort());
        UDP_ACCESS.send(pack);

        // now we should NOT get the reply!
        try {
            while (true) {
                UDP_ACCESS.setSoTimeout(500);
                pack = new DatagramPacket(new byte[1000], 1000);
                UDP_ACCESS.receive(pack);
                InputStream in = new ByteArrayInputStream(pack.getData());
                message = messageFactory.read(in, Network.TCP);
                assertTrue(!((message instanceof QueryReply) &&
                             (Arrays.equals(message.getGUID(), 
                                            ack.getGUID()))));
            }
        }
        catch (IOException expected) {}
    }

    // tests that MessageRouter expires GUIDBundles/Replies in a timely fashion
    // this test requires solicited support
    public void testExpirer() throws Exception {
        drain();
        DatagramPacket pack = null;

        // THIS TESTS ASSUMES SOLICITED SUPPORT - set up in a previous test

        QueryRequest query = 
            queryRequestFactory.createOutOfBandQuery("berkeley",
                                              InetAddress.getLocalHost().getAddress(),
                                              UDP_ACCESS.getLocalPort());
        query.hop();

        // we needed to hop the message because we need to make it seem that it
        // is from sufficiently far away....
        ULTRAPEER[1].send(query);
        ULTRAPEER[1].flush();

        // we should get a ReplyNumberVendorMessage via UDP - we'll get an
        // interrupted exception if not
        Message message = null;
        while (!(message instanceof ReplyNumberVendorMessage)) {
            UDP_ACCESS.setSoTimeout(500);
            pack = new DatagramPacket(new byte[1000], 1000);
            try {
                UDP_ACCESS.receive(pack);
            }
            catch (IOException bad) {
                fail("Did not get VM", bad);
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            // as long as we don't get a ClassCastException we are good to go
            message = messageFactory.read(in, Network.TCP);
        }

        // make sure the GUID is correct
        assertTrue(Arrays.equals(query.getGUID(), message.getGUID()));
        ReplyNumberVendorMessage reply = (ReplyNumberVendorMessage) message;
        assertEquals(1, reply.getNumResults());
        assertEquals(3, reply.getVersion());

        // WAIT for the expirer to expire the query reply
        Thread.sleep(60 * 1000); // 1 minute - expirer must run twice

        // ok - we should ACK the ReplyNumberVM and NOT get a reply
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OOBSecurityToken token = new OOBSecurityToken(new OOBTokenData(pack.getAddress(), 
                pack.getPort(), reply.getGUID(), reply.getNumResults()), macManager);
        LimeACKVendorMessage ack = 
            new LimeACKVendorMessage(new GUID(message.getGUID()), 
                                     reply.getNumResults(), token);
        ack.write(baos);
        pack = new DatagramPacket(baos.toByteArray(), baos.toByteArray().length,
                                  pack.getAddress(), pack.getPort());
        UDP_ACCESS.send(pack);

        // now we should NOT get the reply!  keep reading until buffer is empty
        try {
            while (true) {
                UDP_ACCESS.setSoTimeout(500);
                pack = new DatagramPacket(new byte[1000], 1000);
                UDP_ACCESS.receive(pack);
                InputStream in = new ByteArrayInputStream(pack.getData());
                message = messageFactory.read(in, Network.TCP);
                assertTrue(!((message instanceof QueryReply) &&
                             (Arrays.equals(ack.getGUID(), message.getGUID()))));
            }
        }
        catch (IOException expected) {}
    }
    
    /**
     * Ensures that no more than the maximum number of reply bundles are stored at
     * a single time. 
     */
    public void testMaximumOutOfBandResultsBeingStored() throws Exception {
        DatagramPacket pack = null;
        drain();
        
        // now lets test that if we send a LOT of out-of-band queries,
        // we get a lot of ReplyNumberVMs but at the 251st we don't get a
        // ReplyNumberVM - this test may be fragile because i'm hardcoding
        // MAX_BUFFERED_REPLIES from MessageRouter
        final int MAX_BUFFERED_REPLIES = 10;

        // ok, we need to set MAX_BUFFERED_REPLIES in MessageRouter
        MessageSettings.MAX_BUFFERED_OOB_REPLIES.setValue(MAX_BUFFERED_REPLIES);

        // clear stored results from other tests
//        Map table = messageRouterImpl.getOutOfBandReplies();
//        table.clear();

        try {
            // send 10 queries
            Random rand = new Random();
            int numReplyNumberVMs = 0;
            for (int i = 0; i < MAX_BUFFERED_REPLIES; i++) {
                QueryRequest query = 
                    queryRequestFactory.createOutOfBandQuery((i%2==0) ? "berkeley" : "susheel",
                            InetAddress.getLocalHost().getAddress(),
                            UDP_ACCESS.getLocalPort());
                query.hop();

                if (rand.nextInt(2) == 0) {
                    ULTRAPEER[1].send(query);
                    ULTRAPEER[1].flush();
                }
                else {
                    ULTRAPEER[0].send(query);
                    ULTRAPEER[0].flush();
                }

                Thread.sleep(1250);
                // count 10 ReplyNumberVMs
                try {
                    while (true) {
                        UDP_ACCESS.setSoTimeout(500);
                        pack = new DatagramPacket(new byte[1000], 1000);
                        UDP_ACCESS.receive(pack);
                        InputStream in = new ByteArrayInputStream(pack.getData());
                        Message message = messageFactory.read(in, Network.TCP);
                        if (message instanceof ReplyNumberVendorMessage)
                            numReplyNumberVMs++;
                    }
                }
                catch (IOException expected) {}
            }

            assertEquals("Didn't get all VMs!!", MAX_BUFFERED_REPLIES,
                    numReplyNumberVMs);

            // send 2 new queries that shouldn't be ACKed
            for (int i = 0; i < 2; i++) {
                QueryRequest query = 
                    queryRequestFactory.createOutOfBandQuery((i%2==0) ? "berkeley" : "susheel",
                            InetAddress.getLocalHost().getAddress(),
                            UDP_ACCESS.getLocalPort());
                query.hop();

                ULTRAPEER[0].send(query);
                ULTRAPEER[0].flush();
            }

            // should not get any more ReplyNumberVMs
            try {
                while (true) {
                    UDP_ACCESS.setSoTimeout(500);
                    pack = new DatagramPacket(new byte[1000], 1000);
                    UDP_ACCESS.receive(pack);
                    InputStream in = new ByteArrayInputStream(pack.getData());
                    Message message = messageFactory.read(in, Network.TCP);
                    assertNotInstanceof( ReplyNumberVendorMessage.class, message );
                }
            }
            catch (IOException expected) {}
        }
        finally {
//            MessageRouter.MAX_BUFFERED_REPLIES = oldMaxBufferedReplies;
        }
    }   

    // make sure that the Ultrapeer discards out-of-band query requests that
    // have an improper address associated with it
    public void testIdentity() throws Exception {
        drain();

        byte[] crapIP = {(byte)192,(byte)168,(byte)1,(byte)1};
        QueryRequest query = queryRequestFactory.createOutOfBandQuery("berkeley", 
                                                               crapIP, 6346);
        LEAF[0].send(query);
        LEAF[0].flush();

        // ultrapeers should NOT get the QR
        assertNull(BlockingConnectionUtils.getFirstQueryRequest(ULTRAPEER[0]));
        assertNull(BlockingConnectionUtils.getFirstQueryRequest(ULTRAPEER[1]));
        
        
        Socket socket = LEAF[0].getSocket();
        // try a good query
        query = 
            queryRequestFactory.createOutOfBandQuery("berkeley", 
                                              socket.getLocalAddress().getAddress(),
                                              6346);
        LEAF[0].send(query);
        LEAF[0].flush();

        Thread.sleep(4000);

        // ultrapeers should get the QR
        assertNotNull(BlockingConnectionUtils.getFirstQueryRequest(ULTRAPEER[0]) );
        assertNotNull(BlockingConnectionUtils.getFirstQueryRequest(ULTRAPEER[1]) );

        // LEAF[0] should get the reply
        assertNotNull(BlockingConnectionUtils.getFirstQueryReply(LEAF[0]));
    }

    // a node should NOT send a reply out of band via UDP if it is not
    // far away (low hop)
    public void testLowHopOutOfBandRequest() throws Exception {
        drain();

        byte[] meIP = {(byte)127,(byte)0,(byte)0,(byte)1};
        QueryRequest query = 
            queryRequestFactory.createOutOfBandQuery("susheel", meIP, 
                                              UDP_ACCESS.getLocalPort());
        ULTRAPEER[1].send(query);
        ULTRAPEER[1].flush();

        // ULTRAPEER[1] should get a reply via TCP
        assertNotNull(BlockingConnectionUtils.getFirstQueryReply(ULTRAPEER[1]));
    }


    private static class V2ReplyNumberVendorMessageParser implements VendorMessageParser {

        public VendorMessage parse(byte[] guid, byte ttl, byte hops, int version, byte[] restOf, Network network) throws BadPacketException {
            return new V2ReplyNumberVendorMessage(guid, ttl, hops, version, restOf, network);
        }
    }
    
    private static class V2ReplyNumberVendorMessage extends AbstractVendorMessage {
        
        public V2ReplyNumberVendorMessage(byte[] guid, byte ttl, byte hops, int version, 
                byte[] payload, Network network) 
                throws BadPacketException {
            super(guid, ttl, hops, F_LIME_VENDOR_ID, F_REPLY_NUMBER, version,
                    payload, network);
        }
    }
    
}
