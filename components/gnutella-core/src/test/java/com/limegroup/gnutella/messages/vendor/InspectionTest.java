package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Signature;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.Inflater;

import junit.framework.Test;

import org.limewire.bittorrent.bencoding.Token;
import org.limewire.core.settings.FilterSettings;
import org.limewire.core.settings.MessageSettings;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.inspection.Inspectable;
import org.limewire.io.GGEP;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.security.SecureMessageVerifier;
import org.limewire.security.SecureMessageVerifierImpl;
import org.limewire.util.ByteUtils;
import org.limewire.util.ReadBufferChannel;
import org.limewire.util.StringUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.google.inject.name.Names;
import com.limegroup.gnutella.BlockingConnectionUtils;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.ServerSideTestCase;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.messages.GGEPKeys;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.messages.vendor.RoutableGGEPMessage.GGEPSigner;

public class InspectionTest extends ServerSideTestCase {
    
    /**
     * Ultrapeer 1 UDP connection.
     */
    private DatagramSocket UDP_ACCESS;
    
    private MessageFactory messageFactory;

    private MessagesSupportedVendorMessage messagesSupportedVendorMessage;

    private MessageRouter messageRouter;

    private UDPService udpService;

    private ConnectionManager connectionManager;

    private NetworkManager networkManager;
    
    private KeyPair keyPair;
    
    private Injector injector = null;

    
    public InspectionTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(InspectionTest.class);
    }
    
    @Override
    public int getNumberOfUltrapeers() {
        return 1;
    }

    @Override
    public int getNumberOfLeafpeers() {
        return 2;
    }
    
    @Override
    public void setSettings() throws Exception {
        UDP_ACCESS = new DatagramSocket();
        UDP_ACCESS.setSoTimeout(1000);
        FilterSettings.BLACK_LISTED_IP_ADDRESSES.set(
                new String[] {"*.*.*.*"});
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.set(
                new String[] {InetAddress.getLocalHost().getHostAddress(),"127.*.*.*"});
        FilterSettings.INSPECTOR_IP_ADDRESSES.set(new String[]{"127.*.*.*"});
        MessageSettings.INSPECTION_VERSION.setValue(0);
    }
    
    @Override
    public void setUp() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA");
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN"); 
        keyGen.initialize(1024, random);        
        keyPair = keyGen.generateKeyPair();
        final SecureMessageVerifier smv = new SecureMessageVerifierImpl(keyPair.getPublic(), "testSMV");
        
        injector = LimeTestUtils.createInjector(Stage.PRODUCTION, new AbstractModule() {
            @Override
            protected void configure() {
                bind(SecureMessageVerifier.class).annotatedWith(Names.named("inspection")).toInstance(smv);
                bind(InspectionTestContainer.class);
                bind(InspectionTestContainerLazy.class);
            }
        });
        super.setUp(injector);
        messageFactory = injector.getInstance(MessageFactory.class);
        messagesSupportedVendorMessage = injector.getInstance(MessagesSupportedVendorMessage.class);
        messageRouter = injector.getInstance(MessageRouter.class);
        udpService = injector.getInstance(UDPService.class);
        connectionManager = injector.getInstance(ConnectionManager.class);
        networkManager = injector.getInstance(NetworkManager.class);
    }
    
    private class Signer implements GGEPSigner {
        public GGEP getSecureGGEP(GGEP original) {
            try {
                Signature sig = Signature.getInstance("SHA1withDSA"); 
                sig.initSign(keyPair.getPrivate());
                
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                original.write(out);
                
                sig.update(out.toByteArray());
                byte[] signature = sig.sign();
                GGEP ggep = new GGEP();
                ggep.put(GGEPKeys.GGEP_HEADER_SECURE_BLOCK);
                ggep.put(GGEPKeys.GGEP_HEADER_SIGNATURE, signature);
                return ggep;
            } catch(Throwable t) {
                throw new RuntimeException(t);
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private <T extends Message> T recreate(T req) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        req.write(out);
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        return (T) messageFactory.read(in, Network.TCP);
    }
    
    public void testInspection() throws Exception {
        InspectionTestContainer container = injector.getInstance(InspectionTestContainer.class);
        container.setInspectedValue("a");
        container.setOtherValue("b");
        
        InspectionRequest request = new InspectionRequestImpl(new Signer(),
                "com.limegroup.gnutella.messages.vendor.InspectionTestContainer,inspectedValue",
                "com.limegroup.gnutella.messages.vendor.InspectionTestContainer,otherValue",
                "com.limegroup.gnutella.messages.vendor.InspectionTestContainer,invalidValue");
        request = recreate(request);
        assertEquals(1, request.getRoutableVersion());
        assertEquals(false, request.requestsTimeStamp());
        assertNull(request.getReturnAddress());
        assertEquals("com.limegroup.gnutella.messages.vendor.InspectionTestContainer,inspectedValue",
                request.getRequestedFields()[0]);
        assertEquals("com.limegroup.gnutella.messages.vendor.InspectionTestContainer,otherValue",
                request.getRequestedFields()[1]);
        assertEquals("com.limegroup.gnutella.messages.vendor.InspectionTestContainer,invalidValue",
                request.getRequestedFields()[2]);
        Map response = tryMessage(request);
        assertEquals(1, response.size());
        assertEquals("a", StringUtils.getASCIIString((byte[])response.get("0")));
        
        request = new InspectionRequestImpl(new GUID(), new Signer(), true, false, -1, 2, null, null, 
                "com.limegroup.gnutella.messages.vendor.InspectionTestContainer,inspectedValue",
                "com.limegroup.gnutella.messages.vendor.InspectionTestContainer,otherValue",
                "com.limegroup.gnutella.messages.vendor.InspectionTestContainer,invalidValue");
        request = recreate(request);
        assertEquals(2, request.getRoutableVersion());
        assertEquals(true, request.requestsTimeStamp());
        assertNull(request.getReturnAddress());
        response = tryMessage(request);
        assertEquals(2, response.size());
        Thread.sleep(20);
        long timestamp = Long.valueOf(response.get("-1").toString());
        assertLessThan(System.currentTimeMillis(), timestamp);
        assertGreaterThan(System.currentTimeMillis() - 100, timestamp);
    }
    
    public void testInspectionLazy() throws Exception {
        InspectionTestContainerLazy container = injector.getInstance(InspectionTestContainerLazy.class);
        container.setInspectedValue("c");
        container.setOtherValue("d");
        
        InspectionRequest request = new InspectionRequestImpl(new Signer(),
                "com.limegroup.gnutella.messages.vendor.InspectionTestContainerLazy,inspectedValue",
                "com.limegroup.gnutella.messages.vendor.InspectionTestContainerLazy,otherValue",
                "com.limegroup.gnutella.messages.vendor.InspectionTestContainerLazy,invalidValue");
        request = recreate(request);
        assertEquals(1, request.getRoutableVersion());
        assertEquals(false, request.requestsTimeStamp());
        assertNull(request.getReturnAddress());
        assertEquals("com.limegroup.gnutella.messages.vendor.InspectionTestContainerLazy,inspectedValue",
                request.getRequestedFields()[0]);
        assertEquals("com.limegroup.gnutella.messages.vendor.InspectionTestContainerLazy,otherValue",
                request.getRequestedFields()[1]);
        assertEquals("com.limegroup.gnutella.messages.vendor.InspectionTestContainerLazy,invalidValue",
                request.getRequestedFields()[2]);
        Map response = tryMessage(request);
        assertEquals(1, response.size());
        assertEquals("c", StringUtils.getASCIIString((byte[])response.get("0")));
        
        request = new InspectionRequestImpl(new GUID(), new Signer(), true, false, -1, 2, null, null, 
                "com.limegroup.gnutella.messages.vendor.InspectionTestContainerLazy,inspectedValue",
                "com.limegroup.gnutella.messages.vendor.InspectionTestContainerLazy,otherValue",
                "com.limegroup.gnutella.messages.vendor.InspectionTestContainerLazy,invalidValue");
        request = recreate(request);
        assertEquals(2, request.getRoutableVersion());
        assertEquals(true, request.requestsTimeStamp());
        assertNull(request.getReturnAddress());
        response = tryMessage(request);
        assertEquals(2, response.size());
        Thread.sleep(20);
        long timestamp = Long.valueOf(response.get("-1").toString());
        assertLessThan(System.currentTimeMillis(), timestamp);
        assertGreaterThan(System.currentTimeMillis() - 100, timestamp);
    }

    public void testEmpty() throws Exception {
        InspectionRequest request = new InspectionRequestImpl(new Signer(),
                "com.limegroup.gnutella.messages.vendor.InspectionTestContainer,invalidValue");
        request = recreate(request);
        assertEquals("com.limegroup.gnutella.messages.vendor.InspectionTestContainer,invalidValue", request.getRequestedFields()[0]);
        assertEquals(1, request.getRequestedFields().length);
        try {
            tryMessage(request);
            fail("should not receive anything");
        } catch (IOException expected){
        }
    }
    
    public void testEmptyLazy() throws Exception {
        InspectionRequest request = new InspectionRequestImpl(new Signer(),
                "com.limegroup.gnutella.messages.vendor.InspectionTestContainerLazy,invalidValue");
        request = recreate(request);
        assertEquals("com.limegroup.gnutella.messages.vendor.InspectionTestContainerLazy,invalidValue", request.getRequestedFields()[0]);
        assertEquals(1, request.getRequestedFields().length);
        try {
            tryMessage(request);
            fail("should not receive anything");
        } catch (IOException expected){
        }
    }
    
    public void testInvalidSig() throws Exception {
        InspectionRequestImpl request = new InspectionRequestImpl(
                new RoutableGGEPMessage.GGEPSigner() {
                    public GGEP getSecureGGEP(GGEP original) {
                        GGEP ret = new GGEP(true);
                        ret.put(GGEPKeys.GGEP_HEADER_SECURE_BLOCK);
                        ret.put(GGEPKeys.GGEP_HEADER_SIGNATURE, StringUtils.toAsciiBytes(" adsf adsf asdf "));
                        return ret;
                    }
                }, "com.limegroup.gnutella.messages.vendor.InspectionTestContainer,inspectedValue");
        request = recreate(request);
        try {
            tryMessage(request);
            fail("should not receive anything");
        }catch(IOException expected){}
    }
    
    public void testInvalidSigLazy() throws Exception {
        InspectionRequestImpl request = new InspectionRequestImpl(
                new RoutableGGEPMessage.GGEPSigner() {
                    public GGEP getSecureGGEP(GGEP original) {
                        GGEP ret = new GGEP(true);
                        ret.put(GGEPKeys.GGEP_HEADER_SECURE_BLOCK);
                        ret.put(GGEPKeys.GGEP_HEADER_SIGNATURE, StringUtils.toAsciiBytes(" adsf adsf asdf "));
                        return ret;
                    }
                }, "com.limegroup.gnutella.messages.vendor.InspectionTestContainerLazy,inspectedValue");
        request = recreate(request);
        try {
            tryMessage(request);
            fail("should not receive anything");
        }catch(IOException expected){}
    }

    
    public void testRouting() throws Exception {
        // one of the leafs supports inspections
        LEAF[0].send(messagesSupportedVendorMessage);
        LEAF[0].flush();
        
        // create a request with a return address and one without
        InspectionRequest notRouted = new InspectionRequestImpl(new Signer(), 
                "com.limegroup.gnutella.messages.vendor.InspectionTestContainer,inspectedValue");
        notRouted = recreate(notRouted);

        InspectionRequest routed = new InspectionRequestImpl(new GUID(), new Signer(), true,
                false, -1, 2, new IpPortImpl("127.0.0.1", 20000), null,
                "com.limegroup.gnutella.messages.vendor.InspectionTestContainer,inspectedValue");
        routed = recreate(routed);
        
        assertEquals(1, notRouted.getRoutableVersion());
        assertNull(notRouted.getReturnAddress());
        assertEquals("com.limegroup.gnutella.messages.vendor.InspectionTestContainer,inspectedValue",
                notRouted.getRequestedFields()[0]);
        
        assertEquals(2,routed.getRoutableVersion());
        IpPort retAddr = routed.getReturnAddress();
        assertEquals(0, 
                IpPort.COMPARATOR.compare(new IpPortImpl("127.0.0.1",20000),retAddr));
        assertEquals("com.limegroup.gnutella.messages.vendor.InspectionTestContainer,inspectedValue",
                routed.getRequestedFields()[0]);
        
        
        // the one without should not be forwarded
        BlockingConnectionUtils.drainAll(LEAF);
        tryMessage(notRouted);
        assertNull(BlockingConnectionUtils.getFirstMessageOfType(LEAF[0], InspectionRequestImpl.class));
        assertNull(BlockingConnectionUtils.getFirstMessageOfType(LEAF[1], InspectionRequestImpl.class));
        
        // the one with return address should get answered 
        DatagramSocket socket2 = new DatagramSocket(20000);
        socket2.setSoTimeout(100);
        BlockingConnectionUtils.drainAll(LEAF);
        try {
            tryMessage(routed);
            fail("response should have been sent elsewhere");
        } catch (IOException expected){}
        DatagramPacket pack = new DatagramPacket(new byte[1000],1000);
        socket2.receive(pack);
        assertEquals(2, MessageSettings.INSPECTION_VERSION.getValue());
        
        // and forwarded to the leaf that supports it.
        InspectionRequestImpl received = BlockingConnectionUtils.getFirstMessageOfType(LEAF[0], InspectionRequestImpl.class);
        assertNull(BlockingConnectionUtils.getFirstMessageOfType(LEAF[1], InspectionRequestImpl.class));
        assertNotNull(received);
        
        // the forwarded message should be identical
        assertEquals(routed.getGUID(), received.getGUID());
        assertEquals(20000,received.getReturnAddress().getPort());
        assertEquals("com.limegroup.gnutella.messages.vendor.InspectionTestContainer,inspectedValue",
                received.getRequestedFields()[0]);
        assertEquals(2, received.getRoutableVersion());
        
        // and handling it should also respond to the return address
        MessageSettings.INSPECTION_VERSION.setValue(1);
        MessageRouter router = messageRouter;
        router.handleMessage(received, connectionManager.getInitializedClientConnections().get(0));
        pack = new DatagramPacket(new byte[1000],1000);
        socket2.receive(pack);
        assertEquals(2, received.getRoutableVersion());
    }
    
    public void testRoutingLazy() throws Exception {
        // one of the leafs supports inspections
        LEAF[0].send(messagesSupportedVendorMessage);
        LEAF[0].flush();
        
        // create a request with a return address and one without
        InspectionRequest notRouted = new InspectionRequestImpl(new Signer(), 
                "com.limegroup.gnutella.messages.vendor.InspectionTestContainerLazy,inspectedValue");
        notRouted = recreate(notRouted);

        InspectionRequest routed = new InspectionRequestImpl(new GUID(), new Signer(), true,
                false, -1, 2, new IpPortImpl("127.0.0.1", 20000), null,
                "com.limegroup.gnutella.messages.vendor.InspectionTestContainerLazy,inspectedValue");
        routed = recreate(routed);
        
        assertEquals(1, notRouted.getRoutableVersion());
        assertNull(notRouted.getReturnAddress());
        assertEquals("com.limegroup.gnutella.messages.vendor.InspectionTestContainerLazy,inspectedValue",
                notRouted.getRequestedFields()[0]);
        
        assertEquals(2,routed.getRoutableVersion());
        IpPort retAddr = routed.getReturnAddress();
        assertEquals(0, 
                IpPort.COMPARATOR.compare(new IpPortImpl("127.0.0.1",20000),retAddr));
        assertEquals("com.limegroup.gnutella.messages.vendor.InspectionTestContainerLazy,inspectedValue",
                routed.getRequestedFields()[0]);
        
        
        // the one without should not be forwarded
        BlockingConnectionUtils.drainAll(LEAF);
        tryMessage(notRouted);
        assertNull(BlockingConnectionUtils.getFirstMessageOfType(LEAF[0], InspectionRequestImpl.class));
        assertNull(BlockingConnectionUtils.getFirstMessageOfType(LEAF[1], InspectionRequestImpl.class));
        
        // the one with return address should get answered 
        DatagramSocket socket2 = new DatagramSocket(20000);
        socket2.setSoTimeout(100);
        BlockingConnectionUtils.drainAll(LEAF);
        try {
            tryMessage(routed);
            fail("response should have been sent elsewhere");
        } catch (IOException expected){}
        DatagramPacket pack = new DatagramPacket(new byte[1000],1000);
        socket2.receive(pack);
        assertEquals(2, MessageSettings.INSPECTION_VERSION.getValue());
        
        // and forwarded to the leaf that supports it.
        InspectionRequestImpl received = BlockingConnectionUtils.getFirstMessageOfType(LEAF[0], InspectionRequestImpl.class);
        assertNull(BlockingConnectionUtils.getFirstMessageOfType(LEAF[1], InspectionRequestImpl.class));
        assertNotNull(received);
        
        // the forwarded message should be identical
        assertEquals(routed.getGUID(), received.getGUID());
        assertEquals(20000,received.getReturnAddress().getPort());
        assertEquals("com.limegroup.gnutella.messages.vendor.InspectionTestContainerLazy,inspectedValue",
                received.getRequestedFields()[0]);
        assertEquals(2, received.getRoutableVersion());
        
        // and handling it should also respond to the return address
        MessageSettings.INSPECTION_VERSION.setValue(1);
        MessageRouter router = messageRouter;
        router.handleMessage(received, connectionManager.getInitializedClientConnections().get(0));
        pack = new DatagramPacket(new byte[1000],1000);
        socket2.receive(pack);
        assertEquals(2, received.getRoutableVersion());
    }

    
    private Map tryMessage(Message m) throws Exception {
        assertTrue(udpService.isListening());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        m.write(baos);
        byte [] b = baos.toByteArray();
        DatagramPacket pack = new DatagramPacket(b,
                b.length,InetAddress.getByName("127.0.0.1"),networkManager.getPort());
        UDP_ACCESS.send(pack);
        
        //now read the response       
        pack = new DatagramPacket(new byte[10000],10000);
        
        //not catching IOEx here because not replying is a valid scenario.
        
        UDP_ACCESS.receive(pack);
        byte [] data = pack.getData();
        byte [] guid = new byte[16];
        System.arraycopy(data,0,guid,0,16);
        assertEquals(m.getGUID(),guid);
        assertEquals((byte)0x31,data[16]);
        byte vendorId[] = new byte[4];
        System.arraycopy(data,23,vendorId,0,4);
        assertEquals(VendorMessage.F_LIME_VENDOR_ID, vendorId);
        byte [] selectorVersion = new byte[4];
        System.arraycopy(data,27,selectorVersion,0,4);
        //get the selector....
        int selector = ByteUtils.ushort2int(ByteUtils.leb2short(selectorVersion, 0));
        // get the version....
        int version = ByteUtils.ushort2int(ByteUtils.leb2short(selectorVersion, 2));
        assertEquals(VendorMessage.F_INSPECTION_RESP, selector);
        assertEquals(1, version);
        
        // inflate the rest
        Inflater in = new Inflater();
        in.setInput(data, 31, data.length - 31);
        in.finished();
        byte [] inflated = new byte[60000];
        int numInflated = in.inflate(inflated);
        return (Map)Token.parse(new ReadBufferChannel(inflated, 0, numInflated));
    }
}
@SuppressWarnings("unchecked")
class BEObject implements Inspectable {
    static BEObject self;
    @Override
    public Object inspect() {
        Map m = new HashMap();
        m.put("empty list",new ArrayList());
        m.put("some field",5);
        m.put("wrong type", new BEObject());
        return m;
    }
}   
