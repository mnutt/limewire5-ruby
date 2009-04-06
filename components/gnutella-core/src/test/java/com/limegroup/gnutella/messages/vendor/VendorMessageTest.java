package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.util.Properties;

import junit.framework.Test;

import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.io.GUID;
import org.limewire.io.NetworkUtils;
import org.limewire.security.AddressSecurityToken;
import org.limewire.security.MACCalculatorRepositoryManager;
import org.limewire.security.SecurityToken;
import org.limewire.util.BaseTestCase;
import org.limewire.util.StringUtils;

import com.google.inject.Injector;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.handshaking.HeaderNames;
import com.limegroup.gnutella.library.FileDescStub;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.Message.Network;


@SuppressWarnings( { "cast" } )
public class VendorMessageTest extends BaseTestCase {
    
    private SecurityToken token;
    private ReplyNumberVendorMessageFactory replyNumberVendorMessageFactory;
    private MessageFactory messageFactory;
    private MessagesSupportedVendorMessage messagesSupportedVendorMessage;
    private NetworkManager networkManager;
    
    public VendorMessageTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(VendorMessageTest.class);
    }

    @Override
    protected void setUp() throws Exception {
		Injector injector = LimeTestUtils.createInjector();
		replyNumberVendorMessageFactory = injector.getInstance(ReplyNumberVendorMessageFactory.class);
		messageFactory = injector.getInstance(MessageFactory.class);
		messagesSupportedVendorMessage = injector.getInstance(MessagesSupportedVendorMessage.class);
		networkManager = injector.getInstance(NetworkManager.class);

        token = new AddressSecurityToken(InetAddress.getLocalHost(), 7090, injector.getInstance(MACCalculatorRepositoryManager.class));
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testCreationConstructor() throws Exception {
        byte[] vendorID = null;
        try {
            //test messed up vendor ID
            vendorID = new byte[5];
            new VM(vendorID, 1, 1, new byte[0]);
            fail("bpe should have been thrown.");
        } catch (IllegalArgumentException expected) {}
        
        try {
            // test bad selector
            new VM(new byte[4], 0x10000000, 1, new byte[0]);
            fail("bpe should have been thrown.");
        } catch (IllegalArgumentException expected) {}
        
        try {
            // test bad version
            new VM(vendorID, 1, 0x00020101, new byte[0]);
            fail("bpe should have been thrown.");
        } catch (IllegalArgumentException expected) {}
        
        try {
            // test bad payload
            new VM(new byte[4], 1, 1, null);
            fail("bpe should have been thrown.");
        } catch (NullPointerException expected) {}
    }


    // tests HopsFlowVM and LimeACKVM (very simple messages)
    public void testWriteAndRead() throws Exception {
        // HOPS FLOW
        // -----------------------------
        // test network constructor....
        VendorMessage vm = new HopsFlowVendorMessage(GUID.makeGuid(), (byte) 1, 
                                                     (byte) 0, 1, new byte[1], Network.UNKNOWN);
        testWrite(vm);
        // test other constructor....
        vm = new HopsFlowVendorMessage((byte)6);
        testRead(vm);

        // Lime ACK
        // -----------------------------
        // test network constructor....
        vm = new LimeACKVendorMessage(GUID.makeGuid(), (byte) 1, (byte) 0, 
                                      2, new byte[1], Network.UNKNOWN);
        testWrite(vm);
        // test other constructor....
        vm = new LimeACKVendorMessage(new GUID(GUID.makeGuid()), 5, token);
        testRead(vm);

        // Reply Number
        // -----------------------------
        // test network constructor....
        
        // test old versions are no longer accepted 
        try {
            vm = replyNumberVendorMessageFactory.createFromNetwork(GUID.makeGuid(),
                    (byte) 1, (byte) 0, 1, new byte[1], Network.UNKNOWN);
            fail("should have thrown bad packed exception, protocol version 1 is no longer supported");
        }
        catch (BadPacketException bpe) {
        }
        try {
            vm = replyNumberVendorMessageFactory.createFromNetwork(GUID.makeGuid(),
                    (byte) 1, (byte) 0, 2, new byte[1], Network.UNKNOWN);
            fail("should have thrown bad packed exception, protocol version 2 is no longer supported");
        }
        catch (BadPacketException bpe) {
        }
        
        vm = replyNumberVendorMessageFactory.createFromNetwork(GUID.makeGuid(),
                (byte) 1, (byte) 0, 3, new byte[2], Network.UNKNOWN);
        
        testWrite(vm);
        // test other constructor....
        vm = replyNumberVendorMessageFactory.createV3ReplyNumberVendorMessage(new GUID(GUID.makeGuid()), 5);
        testRead(vm);

        // Push Proxy Request
        // -----------------------------
        // test network constructor....
        vm = new PushProxyRequest(GUID.makeGuid(), (byte) 1, 
                                  (byte) 0, 1, new byte[0], Network.UNKNOWN);
        testWrite(vm);
        // test other constructor....
        vm = new PushProxyRequest(new GUID(GUID.makeGuid()));
        testRead(vm);

        // Push Proxy Acknowledgement
        // -----------------------------
        // test network constructor....

        byte[] bytes = new byte[] {(byte)192, (byte)168, (byte)1, (byte)1,
                                   (byte)1, (byte)1};

        // make sure deprecation is working....
        try {
            vm = new PushProxyAcknowledgement(GUID.makeGuid(), (byte) 1, 
                                              (byte) 0, 1, bytes, Network.UNKNOWN);
            assertTrue(false);
        }
        catch (BadPacketException expected) {}

        vm = new PushProxyAcknowledgement(GUID.makeGuid(), (byte) 1, 
                                          (byte) 0, 2, bytes, Network.UNKNOWN);
        testWrite(vm);
        // test other constructor....
        vm = new PushProxyAcknowledgement(InetAddress.getLocalHost(), 5);
        testRead(vm);

        // Query Status Request
        // -----------------------------
        // test network constructor....
        vm = new QueryStatusRequest(GUID.makeGuid(), (byte) 1, 
                                    (byte) 0, 1, new byte[0], Network.UNKNOWN);
        testWrite(vm);
        // test other constructor....
        vm = new QueryStatusRequest(new GUID(GUID.makeGuid()));
        testRead(vm);

        // Query Status Response
        // -----------------------------
        // test network constructor....
        vm = new QueryStatusResponse(GUID.makeGuid(), (byte) 1, 
                                     (byte) 0, 1, new byte[2], Network.UNKNOWN);
        testWrite(vm);
        // test other constructor....
        vm = new QueryStatusResponse(new GUID(GUID.makeGuid()), 65535);
        testRead(vm);

        // TCP ConnectBack Redirect
        // -----------------------------
        // test network constructor....
        vm = new TCPConnectBackRedirect(GUID.makeGuid(), (byte) 1, 
                                        (byte) 0, 1, bytes, Network.UNKNOWN);
        testWrite(vm);
        // test other constructor....
        vm = new TCPConnectBackRedirect(InetAddress.getLocalHost(), 65535);
        testRead(vm);

        // TCP ConnectBack Redirect
        // -----------------------------
        // test network constructor....
        vm = new UDPConnectBackRedirect(GUID.makeGuid(), (byte) 1, 
                                        (byte) 0, 1, bytes, Network.UNKNOWN);
        testWrite(vm);
        // test other constructor....
        vm = new UDPConnectBackRedirect(new GUID(GUID.makeGuid()),
                                        InetAddress.getLocalHost(), 65535);
        testRead(vm);

    }

    public void testUDPCrawlerPingMessage() throws Exception {
    	GUID guid = new GUID(GUID.makeGuid());
    	UDPCrawlerPing req = new UDPCrawlerPing(guid, 1,2,UDPCrawlerPing.PLAIN);
    	assertEquals(1, req.getNumberUP());
    	assertEquals(2, req.getNumberLeaves());
    	assertFalse(req.hasConnectionTime());
    	assertFalse(req.hasLocaleInfo());
        assertFalse(req.hasNodeUptime());
        assertFalse(req.hasReplies());
    	assertTrue(req.hasFeature(UDPCrawlerPing.PLAIN));
    	testWrite(req);
    	testRead(req);
    	
    	//also test one with newer mask - should be trimmed to our mask
    	req = new UDPCrawlerPing(guid, 1,2,(byte)0xFF);
    	assertTrue(req.hasUserAgent());
    	assertTrue(req.hasFeature(UDPCrawlerPing.FEATURE_MASK));
    	assertEquals(0,req.getFormat() & ~UDPCrawlerPing.FEATURE_MASK);
        
        //test node uptime feature
        req = new UDPCrawlerPing(guid, 1, 2, (byte)0x10);
        assertTrue(req.hasNodeUptime());
        assertFalse(req.hasConnectionTime());
        assertFalse(req.hasLocaleInfo());
        assertFalse(req.hasNewOnly());
        assertFalse(req.hasUserAgent());
        
        //test mask
        req = new UDPCrawlerPing(guid, 1, 2, (byte)0x11);
        assertTrue(req.hasNodeUptime());
        assertTrue(req.hasConnectionTime());
    }
    
    public void testHeadPingMessage() throws Exception {
    	URN urn = FileDescStub.DEFAULT_SHA1;
    	
    	HeadPing ping = new HeadPing(urn);
    	
    	assertEquals(HeadPing.PLAIN, ping.getFeatures());
    	assertFalse(ping.requestsAltlocs());
    	assertFalse(ping.requestsRanges());
    	assertFalse(ping.requestsPushLocs());
    	
   		ping = new HeadPing(new GUID(GUID.makeGuid()),urn, 0xFF & ~HeadPing.GGEP_PING);
    	assertTrue(ping.requestsPushLocs());
    	assertTrue(ping.requestsAltlocs());
    	assertTrue(ping.requestsRanges());
    	
    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
    	ping.write(baos);
    	ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
    	HeadPing ping2 = (HeadPing) messageFactory.read(bais, Network.TCP);
    	
    	assertEquals(ping.getUrn(), ping2.getUrn());
    	assertEquals(ping.getFeatures(),ping2.getFeatures());

    	testWrite(ping);
    	
    	GUID g = new GUID(GUID.makeGuid());
    	ping = new HeadPing(new GUID(GUID.makeGuid()),urn, g, 0xFF);
    	
    	//parse it once, verify guids the same
    	baos = new ByteArrayOutputStream();
    	ping.write(baos);
    	bais = new ByteArrayInputStream(baos.toByteArray());
    	ping2 = (HeadPing) messageFactory.read(bais, Network.TCP);
    	
    	assertEquals(g,ping2.getClientGuid());
    	
    	//pings which have the flag but no guid fail.
    	ping = new HeadPing(new GUID(GUID.makeGuid()),urn, 0xFF);
    	baos = new ByteArrayOutputStream();
    	ping.write(baos);
    	bais = new ByteArrayInputStream(baos.toByteArray());
    	try {
    	ping2 = (HeadPing) messageFactory.read(bais, Network.TCP);
    		fail("parsed a ping which claimed to have a clientguid but didn't");
    	}catch(BadPacketException expected) {}
    }
    
    
    public void testPushProxyVMs() throws Exception {
        GUID guid = new GUID(GUID.makeGuid());
        PushProxyRequest req = new PushProxyRequest(guid);
        assertTrue(req.getClientGUID().equals(new GUID(req.getGUID())));
        assertTrue(req.getClientGUID().equals(guid));
        testWrite(req);
        testRead(req);

        InetAddress addr = InetAddress.getLocalHost();
        PushProxyAcknowledgement ack = new PushProxyAcknowledgement(addr, 6346);
        assertEquals(InetAddress.getLocalHost(), ack.getListeningAddress());
        assertTrue(ack.getListeningPort() == 6346);
        testWrite(ack);
        testRead(req);
    }
    
    private void testWrite(VendorMessage vm) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        vm.write(baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        VendorMessage vmRead = (VendorMessage) messageFactory.read(bais, Network.TCP);
        assertEquals(vm, vmRead);
    }

    private void testRead(VendorMessage vm) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        vm.write(baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        VendorMessage vmRead = (VendorMessage) messageFactory.read(bais, Network.TCP);
        assertEquals(vm,vmRead);
    }

    public void testEquals() throws Exception {
        VM vm1 = new VM(StringUtils.toAsciiBytes("LIME"), 1, 1, new byte[0]);
        VM vm2 = new VM(StringUtils.toAsciiBytes("LIME"), 1, 1, new byte[0]);
        VM vm3 = new VM(StringUtils.toAsciiBytes("BEAR"), 1, 1, new byte[0]);
        VM vm4 = new VM(StringUtils.toAsciiBytes("LIMB"), 1, 1, new byte[0]);
        VM vm5 = new VM(StringUtils.toAsciiBytes("LIME"), 2, 1, new byte[0]);
        VM vm6 = new VM(StringUtils.toAsciiBytes("LIME"), 1, 2, new byte[0]);
        VM vm7 = new VM(StringUtils.toAsciiBytes("LIME"), 1, 1, new byte[1]);
        assertEquals(vm1,vm2);
        assertNotEquals(vm1,(vm3));
        assertNotEquals(vm1,(vm4));
        assertNotEquals(vm1,(vm5));
        assertNotEquals(vm1,(vm7));
        // versions don't effect equality....
        assertEquals(vm1,(vm6));
    }


    public void testHashCode() throws Exception {
        TCPConnectBackVendorMessage vmp1 = 
        new TCPConnectBackVendorMessage(1000);
        TCPConnectBackVendorMessage vmp2 = 
        new TCPConnectBackVendorMessage(1000);
        TCPConnectBackVendorMessage vmp3 = 
        new TCPConnectBackVendorMessage(1001);
        assertEquals(vmp1.hashCode() , vmp2.hashCode());
        assertNotEquals(vmp3.hashCode() , vmp2.hashCode());
    }


    public void testUDPConnectBackRedirectConstructor() throws Exception {
        final int UDP_VERSION = UDPConnectBackRedirect.VERSION;

        byte[] guid = GUID.makeGuid();
        byte ttl = 1, hops = 0;
        
        try {
            // try a VERSION we don't support with a payload that is ok
            new UDPConnectBackRedirect(guid, ttl, hops,
                                                UDP_VERSION+1, bytes(6), Network.UNKNOWN);
        }
        catch (BadPacketException expected) {
            fail("should not have thrown bpe");
        }
        
        try {
            // try a VERSION we don't support, with the old 18-byte payload
            new UDPConnectBackRedirect(guid, ttl, hops,
                                                UDP_VERSION+1, bytes(4), Network.UNKNOWN);
            fail("should have thrown bpe");
        }
        catch (ArrayIndexOutOfBoundsException expected) {}

        try {
            // in the next few tests, try bad sizes of the payload....
            new UDPConnectBackRedirect(guid, ttl, hops,
                                                UDP_VERSION, bytes(0), Network.UNKNOWN);
            fail("should have thrown bpe");
        }
        catch (BadPacketException expected) {}
        try {
            new UDPConnectBackRedirect(guid, ttl, hops,
                                                UDP_VERSION, bytes(5), Network.UNKNOWN);
            fail("should have thrown bpe");
        }
        catch (BadPacketException expected) {}
        try {
            new UDPConnectBackRedirect(guid, ttl, hops,
                                                UDP_VERSION, bytes(7), Network.UNKNOWN);
            fail("should have thrown bpe");
        }
        catch (BadPacketException expected) {}

        // Test version 1 constructor -- 18 bytes in payload
        new UDPConnectBackRedirect(guid, ttl, hops, 1, bytes(6), Network.UNKNOWN);
        // no bpe ...
        
        // make sure we encode things just fine....
        GUID guidObj = new GUID(GUID.makeGuid());

        UDPConnectBackRedirect VendorMessage1 = 
            new UDPConnectBackRedirect(guidObj, InetAddress.getLocalHost(),
                                       6346);
        UDPConnectBackRedirect VendorMessage2 = 
            new UDPConnectBackRedirect(VendorMessage1.getGUID(), ttl, hops,
                                            VendorMessage1.getVersion(),
                                            VendorMessage1.getPayload(), Network.UNKNOWN);
        assertEquals(1, VendorMessage1.getVersion());
        assertEquals(VendorMessage2, VendorMessage1);
        assertEquals(VendorMessage1.getConnectBackPort(),
                     VendorMessage2.getConnectBackPort());
        assertEquals(VendorMessage1.getConnectBackAddress(),
                     VendorMessage2.getConnectBackAddress());
        assertEquals(VendorMessage1.getConnectBackGUID(),
                     VendorMessage2.getConnectBackGUID());

    }
    
    /**
     * Creates a byte array whose first byte is non zero.
     */
    private byte[] bytes(int length) {
        byte[] stuff = new byte[length];
        for (int i = 0; i < stuff.length; i++)
            stuff[i] = (byte)3;
        return stuff;
    }

    public void testTCPConnectBackConstructor() throws Exception {
        final int TCP_VERSION = TCPConnectBackRedirect.VERSION;

        byte[] guid = GUID.makeGuid();
        byte ttl = 1, hops = 0;
        try {
            // try a VERSION we don't support but should be ok
            new TCPConnectBackRedirect(guid, ttl, hops,
                                                TCP_VERSION+1, bytes(6), Network.UNKNOWN);
        }
        catch (BadPacketException expected) {
            fail("should not have thrown bpe");
        }
        try {
            // in the next few tests, try bad sizes of the payload....
            new TCPConnectBackRedirect(guid, ttl, hops,
                                                TCP_VERSION, bytes(0), Network.UNKNOWN);
            fail("should have thrown bpe");
        }
        catch (BadPacketException expected) {}
        try {
            new TCPConnectBackRedirect(guid, ttl, hops,
                                                TCP_VERSION, bytes(5), Network.UNKNOWN);
            fail("should have thrown bpe");
        }
        catch (BadPacketException expected) {}
        try {
            new TCPConnectBackRedirect(guid, ttl, hops,
                                                TCP_VERSION, bytes(7), Network.UNKNOWN);
            fail("should have thrown bpe");
        }
        catch (BadPacketException expected) {}

        // this is the correct size of the payload
        new TCPConnectBackRedirect(guid, ttl, hops,
                                            TCP_VERSION, bytes(6), Network.UNKNOWN);


        // make sure we encode things just fine....
        TCPConnectBackRedirect VendorMessage1 = 
            new TCPConnectBackRedirect(InetAddress.getLocalHost(), 6346);
        TCPConnectBackRedirect VendorMessage2 = 
            new TCPConnectBackRedirect(VendorMessage1.getGUID(),
                                            ttl, hops, TCP_VERSION, 
                                            VendorMessage1.getPayload(), Network.UNKNOWN);
        assertEquals(VendorMessage1, VendorMessage2);
        assertEquals(VendorMessage1.getConnectBackPort(),
                     VendorMessage2.getConnectBackPort());

    }

    public void testGetSpecificVendorMessages() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        TCPConnectBackVendorMessage tcp = null;
        UDPConnectBackVendorMessage udp = null;
        TCPConnectBackRedirect tcpR = null;
        UDPConnectBackRedirect udpR = null;
        HopsFlowVendorMessage hops = null;
        MessagesSupportedVendorMessage ms = null;
            
        tcp = new TCPConnectBackVendorMessage(6346);
        udp = new UDPConnectBackVendorMessage(6346, 
                                              new GUID(GUID.makeGuid()));
        tcpR = new TCPConnectBackRedirect(InetAddress.getLocalHost(), 6346);
        udpR = new UDPConnectBackRedirect(new GUID(GUID.makeGuid()), 
                                          InetAddress.getLocalHost(), 6346);
        hops = new HopsFlowVendorMessage((byte)4);

        ms = messagesSupportedVendorMessage;
        
        tcp.write(baos);
        udp.write(baos);
        tcpR.write(baos);
        udpR.write(baos);
        ms.write(baos);
        hops.write(baos);
        
        ByteArrayInputStream bais = 
            new ByteArrayInputStream(baos.toByteArray());
        
        VendorMessage vm = (VendorMessage) messageFactory.read(bais, Network.TCP);
        assertEquals(vm,(tcp));

        vm = (VendorMessage) messageFactory.read(bais, Network.TCP);
        assertEquals(vm,(udp));

        vm = (VendorMessage) messageFactory.read(bais, Network.TCP);
        assertEquals(vm,(tcpR));

        vm = (VendorMessage) messageFactory.read(bais, Network.TCP);
        assertEquals(vm,(udpR));

        vm = (VendorMessage) messageFactory.read(bais, Network.TCP);
        assertEquals(vm,(ms));
        
        vm = (VendorMessage) messageFactory.read(bais, Network.TCP);
        assertEquals(vm,(hops));
    }


    public void testReadHoppedVendorMessage() throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        TCPConnectBackVendorMessage tcp = 
            new TCPConnectBackVendorMessage(6346);
        VendorMessage vm = (VendorMessage) tcp;
        vm.hop();
        vm.write(baos);
        
        ByteArrayInputStream bais = 
            new ByteArrayInputStream(baos.toByteArray());
        
        vm = (VendorMessage) messageFactory.read(bais, Network.TCP);
        
    }

    public void testReadHeaderUpdateVendorMessage() throws Exception {
        
        // make a header update message as we do when our ip changes
        byte addr[] = networkManager.getAddress();
        int port = networkManager.getPort();
        Properties props = new Properties();
        props.put(HeaderNames.LISTEN_IP, NetworkUtils.ip2string(addr) + ":" + port);
        HeaderUpdateVendorMessage m = new HeaderUpdateVendorMessage(props);

        // serialize it like we're sending it across the wire
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        m.write(data);

        // now pretend we're on the other end, and we've received it

        // split it into header and payload
        int headerLength, payloadLength;
        headerLength = 23;
        payloadLength = data.size() - headerLength;
        byte[] header = new byte[headerLength];
        byte[] payload = new byte[payloadLength];
        System.arraycopy(data.toByteArray(), 0, header, 0, headerLength);
        System.arraycopy(data.toByteArray(), headerLength, payload, 0, payloadLength);

        // see if Message.createMessage() can understand it
        Message m2 = messageFactory.createMessage(header, payload, Network.TCP, (byte)4, null);
        assertNotNull(m2);
    }
    
    private static class VM extends AbstractVendorMessage {
        public VM(byte[] guid, byte ttl, byte hops, byte[] vendorID, 
                  int selector, int version, byte[] payload, Network network) 
            throws BadPacketException {
            super(guid, ttl, hops, vendorID, selector, version, payload, network);
        }

        public VM(byte[] vendorID, int selector, int version, byte[] payload) {
            super(vendorID, selector, version, payload);
        }
    }


}
