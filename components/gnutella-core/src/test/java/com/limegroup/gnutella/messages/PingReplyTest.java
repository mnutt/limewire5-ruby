package com.limegroup.gnutella.messages;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ScheduledExecutorService;

import junit.framework.Test;

import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.gnutella.tests.NetworkManagerStub;
import org.limewire.io.GGEP;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.net.ConnectionDispatcher;
import org.limewire.net.SocketsManager;
import org.limewire.security.AddressSecurityToken;
import org.limewire.security.MACCalculatorRepositoryManager;
import org.limewire.util.StringUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.ConnectionManagerImpl;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.ExtendedEndpoint;
import com.limegroup.gnutella.HostCatcher;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.NodeAssigner;
import com.limegroup.gnutella.QueryUnicaster;
import com.limegroup.gnutella.connection.ConnectionCheckerManager;
import com.limegroup.gnutella.connection.RoutedConnectionFactory;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactory;
import com.limegroup.gnutella.simpp.SimppManager;

@SuppressWarnings( { "unchecked", "cast" } )
public class PingReplyTest extends LimeTestCase {
    
    /**
     * A non blank IP
     */
    private final byte[] IP = new byte[] { 1, 1, 1, 1 };
    private PingReplyFactory pingReplyFactory;
    private MessageFactory messageFactory;
    private NetworkManagerStub networkManagerStub;
    private HostCatcher hostCatcher;
    private TestConnectionManager testConnectionManager;
    private MACCalculatorRepositoryManager macManager;
    
    public PingReplyTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(PingReplyTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    protected void setUp() throws Exception {
        networkManagerStub = new NetworkManagerStub();
        networkManagerStub.setPort(5555);
        networkManagerStub.setAddress(new byte[] { 89, 1, 45, 54 }) ;
        networkManagerStub.setTls(true);
        networkManagerStub.setIncomingTLSEnabled(true);
        networkManagerStub.setOutgoingTLSEnabled(true);
        
        Injector injector = LimeTestUtils.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(NetworkManager.class).toInstance(networkManagerStub);
                bind(ConnectionManager.class).to(TestConnectionManager.class);
            }
        });
        pingReplyFactory = injector.getInstance(PingReplyFactory.class);
        messageFactory = injector.getInstance(MessageFactory.class);
        hostCatcher = injector.getInstance(HostCatcher.class);
        testConnectionManager = (TestConnectionManager) injector.getInstance(ConnectionManager.class);
        macManager = injector.getInstance(MACCalculatorRepositoryManager.class);
    }
   
    /**
     * Tests the methods for getting the leaf and ultrapeer slots from the 
     * pong.
     * 
     * @throws Exception if an error occurs
     */
    public void testHasFreeSlots() throws Exception {
        byte[] guid = GUID.makeGuid();
        byte[] ip = {1,1,1,1};
        PingReply pr = pingReplyFactory.create(guid, (byte)3, 6346, ip, 
            (long)10, (long)10, true, 100, true);    

        //All values are determined based on connection status, and because
        // we haven't set up connections yet, we don't have free anything.
        assertTrue("slots not empty", !pr.hasFreeSlots());
        assertEquals("unexpected number leaf slots", 0, pr.getNumLeafSlots());
        assertTrue("slots not empty", !pr.hasFreeLeafSlots());
        assertTrue("slots not empty", !pr.hasFreeUltrapeerSlots());
        assertEquals("slots not empty", 0, pr.getNumUltrapeerSlots());
        
        // Switch ConnectionManager to report different values for free leaf
        // and ultrapeer slots.
        testConnectionManager.setNumFreeNonLeafSlots(7);
        testConnectionManager.setNumFreeLeafSlots(10);
        
        pr = pingReplyFactory.create(guid, (byte)3, 6346, ip, 
            (long)10, (long)10, true, 100, true);    
            
        assertTrue("no slots", pr.hasFreeSlots());
        assertTrue("no slots", pr.hasFreeLeafSlots());
        assertTrue("no slots", pr.hasFreeUltrapeerSlots());
        
        // Should now have leaf slots
        assertEquals("unexpected number leaf slots", 
                testConnectionManager.getNumFreeLimeWireLeafSlots(), 
                    pr.getNumLeafSlots());
        
        assertEquals("unexpected number ultrapeer slots", 
                    testConnectionManager.getNumFreeLimeWireNonLeafSlots(), 
                    pr.getNumUltrapeerSlots());
    }
   
    
    /**
     * Tests the method for creating a new pong with a changed GUID out
     * of an existing pong
     */
    public void testMutateGUID() throws Exception {
        byte[] guid = new GUID().bytes();
        byte ttl = 4;
        int port = 6444;
        byte[] ip = {1, 1, 1, 1};
        long files = 500L;
        long kbytes = 75580L;
        boolean isUltrapeer = false;
        int dailyUptime = 10;
        boolean isGUESSCapable = false;
        
        PingReply pr = 
            pingReplyFactory.create(guid, ttl, port, ip, files, kbytes, 
                             isUltrapeer, dailyUptime, isGUESSCapable);
        
        PingReply testPR = pingReplyFactory.mutateGUID(pr, new GUID().bytes());
        
        assertNotEquals(pr.getGUID(), testPR.getGUID());
        assertEquals(pr.getTTL(), testPR.getTTL());
        assertEquals(pr.getPort(), testPR.getPort());
        assertEquals(pr.getInetAddress(), testPR.getInetAddress());
        assertEquals(pr.getFiles(), testPR.getFiles());
        assertEquals(pr.getKbytes(), testPR.getKbytes());
        assertEquals(pr.isUltrapeer(), testPR.isUltrapeer());
        assertEquals(pr.getDailyUptime(), testPR.getDailyUptime());
        assertEquals(pr.supportsUnicast(), testPR.supportsUnicast());
    }


    /**
     * Tests the method for creating a pong from the network.
     */
    public void testCreatePongFromNetwork() throws Exception {
        byte[] guid = new GUID().bytes();
        byte[] payload = new byte[2];

        // make sure we reject invalid payload sizes
        try {
            pingReplyFactory.createFromNetwork(guid, (byte)4, (byte)3,
                                            payload);
            fail("should have not accepted payload size");
        } catch(BadPacketException e) {
            // expected because the payload size is invalid
        }

        // make sure we reject null guids
        payload = new byte[PingReply.STANDARD_PAYLOAD_SIZE];
        addIP(payload);
        try {
            pingReplyFactory.createFromNetwork(null, (byte)4, (byte)3,
                                            payload);
            fail("should have not accepted null guid");
        } catch(NullPointerException e) {
            // expected because the payload size is invalid
        }        

        // make sure we reject null payloads
        try {
            pingReplyFactory.createFromNetwork(guid, (byte)4, (byte)3,
                                            null);
            fail("should have not accepted null payload");
        } catch(NullPointerException e) {
            // expected because the payload size is invalid
        }        

        // make sure we reject bad ggep
        GGEP ggep = new GGEP();
        payload = new byte[3];
        ggep.put(GGEPKeys.GGEP_HEADER_PACKED_IPPORTS, payload);  
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ggep.write(baos);

        byte[] extensions = baos.toByteArray();
        payload = 
            new byte[PingReply.STANDARD_PAYLOAD_SIZE+extensions.length];
        addIP(payload);            

        System.arraycopy(extensions, 0, 
                         payload, PingReply.STANDARD_PAYLOAD_SIZE, 
                         extensions.length);
        try {
            pingReplyFactory.createFromNetwork(guid, (byte)4, (byte)3,
                                            payload);
            fail("should have not accepted bad GGEP in payload");
        } catch(BadPacketException e) {
            // expected because the payload size is invalid
        }                

        // test one that should go through fine
        payload = new byte[PingReply.STANDARD_PAYLOAD_SIZE];
        payload[0] = 1;
        addIP(payload);        

        // this one should go through
        pingReplyFactory.createFromNetwork(guid, (byte)4, (byte)3,
                                        payload);
    }


    public void testNewPong() {
        long u4=0x00000000FFFFFFFFl;
        int u2=0x0000FFFF;
        byte[] ip={(byte)0xFE, (byte)0x00, (byte)0x00, (byte)0x1};
        PingReply pr = pingReplyFactory.create(new byte[16], (byte)0,
                                        u2, ip, u4, u4);

        assertEquals(u2, pr.getPort());
        assertEquals(u4, pr.getFiles());
        long kbytes=pr.getKbytes();
        assertEquals(Long.toHexString(kbytes), u4, kbytes);
        String ip2=pr.getAddress();
        assertEquals("254.0.0.1", ip2);
        assertTrue(! pr.isUltrapeer());
    }      
      
    //TODO: check construction from raw bytes

    public void testPongMarking() {
        PingReply pr = 
            pingReplyFactory.createExternal(new byte[16], (byte)2, 6346, IP,
                                     false);

        
        assertTrue(! pr.isUltrapeer());        
        // all pongs should have a GGEP extension now....
        assertTrue("pong should have GGEP ext", pr.hasGGEPExtension());

        pr = pingReplyFactory.createExternal(new byte[16], (byte)2, 6346, IP,
                                      true);
        assertTrue(pr.isUltrapeer());
        // all pongs should have a GGEP extension now....
        assertTrue("pong should have GGEP ext", pr.hasGGEPExtension());

        pr = pingReplyFactory.create(new byte[16], (byte)2, 6346, IP,
                              5, 2348, false, 0, false);        
        assertTrue(! pr.isUltrapeer());
        assertEquals(2348, pr.getKbytes());
        // all pongs should have a GGEP extension now....
        assertTrue("pong should have GGEP ext", pr.hasGGEPExtension());

        pr = pingReplyFactory.create(new byte[16], (byte)2, 6346, IP,
                              5, 2348, true, 0, true);
        assertTrue(pr.isUltrapeer());
        // all pongs should have a GGEP extension now....
        assertTrue("pong should have GGEP ext", pr.hasGGEPExtension());

        pr = pingReplyFactory.create(new byte[16], (byte)2, 6346, IP,
                              5, 345882, false, 0, false);
        assertTrue(! pr.isUltrapeer());
        // all pongs should have a GGEP extension now....
        assertTrue("pong should have GGEP ext", pr.hasGGEPExtension());

        pr = pingReplyFactory.create(new byte[16], (byte)2, 6346, IP,
                              5, 345882, true, -1, true);
        assertTrue(pr.isUltrapeer());
        // after added unicast support, all Ultrapeer Pongs have GGEP extension
        assertTrue("pong should have GGEP ext", pr.hasGGEPExtension());

        
        assertEquals("pong should not have a daily uptime", -1,
                     pr.getDailyUptime());        
    }
      
    public void testNonGGEPBigPong() throws Exception  {
        //Will this pass big pongs--even if the contents are not GGEP?
        byte[] payload = new byte[14+2];
        //add the port
        payload[0] = 0x0F;
        payload[1] = 0x00;//port 
      
        payload[2] = 0x10;
        payload[3] = 0x10;
        payload[4] = 0x10;
        payload[5] = 0x10;//ip = 16.16.16.16
      
        payload[6] = 0x0F;//
        payload[7] = 0x00;//
        payload[8] = 0x00;//
        payload[9] = 0x00;//15 files shared
      
        payload[10] = 0x0F;//
        payload[11] = 0x00;//
        payload[12] = 0x00;//
        payload[13] = 0x00;//15 KB
        //OK Now for the big pong part
        payload[14] = (byte) 65;
        payload[15] = (byte) 66;
        PingReply pr;
        pr = pingReplyFactory.createFromNetwork(new byte[16], (byte)2, (byte)4, payload);
        assertTrue(! pr.hasGGEPExtension());

        assertEquals("pong should not have a daily uptime", -1,
                     pr.getDailyUptime());  
        
        //Start testing
        assertEquals("wrong port", 15, pr.getPort());
        String ip = pr.getAddress();
        assertEquals("wrong IP", "16.16.16.16", ip);
        assertEquals("wrong files", 15, pr.getFiles());
        assertEquals("Wrong share size", 15, pr.getKbytes());
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        pr.write(stream);
        byte[] op = stream.toByteArray();
        byte[] big = new byte[2];
        big[0] = op[op.length-2];
        big[1] = op[op.length-1];
        String out = StringUtils.getASCIIString(big);
        assertEquals("Big part of pong lost", "AB", out);
        //come this far means its OK
    }


    public void testBasicGGEP() throws Exception {
        networkManagerStub.setIncomingTLSEnabled(true);
        
        // create a pong
        PingReply pr = 
            pingReplyFactory.createExternal(new byte[16], (byte)3, 6349, IP, false);
        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        pr.write(baos);

        byte[] bytes=baos.toByteArray(); 

        //Decode and check contents.
        Message m=messageFactory.read(new ByteArrayInputStream(bytes), Network.TCP);
        PingReply pong=(PingReply)m;
        assertTrue(m instanceof PingReply);
        assertEquals(6349, pong.getPort());
        assertTrue("pong should have GGEP ext", pr.hasGGEPExtension());
        assertFalse(pong.supportsUnicast());
        assertTrue(pong.isTLSCapable());
        // make sure it's still capable if we turn our settings off.
        networkManagerStub.setIncomingTLSEnabled(false);
        assertTrue(pong.isTLSCapable());
        
        // And try creating a new pong w/o TLS.
        // create a pong
        pr = pingReplyFactory.createExternal(new byte[16], (byte)3, 6349, IP, false);
        baos.reset();
        pr.write(baos);
        bytes=baos.toByteArray();
        m=messageFactory.read(new ByteArrayInputStream(bytes), Network.TCP);
        pong=(PingReply)m;
        assertFalse(pong.isTLSCapable());
        // make sure it's still off if we turn our settings on.
        networkManagerStub.setIncomingTLSEnabled(true);
        assertFalse(pong.isTLSCapable());
    }


    /** Test the raw bytes of an encoded GGEP'ed pong.  Then checks that
     *  these can be decoded.  Note that this will need to be changed if
     *  more extensions are added. */
    public void testGGEPEncodeDecode() throws Exception {
        PingReply pr = pingReplyFactory.create(new byte[16], (byte)3, 6349, IP,
                                   0l, 0l, true, 523, true);        

        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        pr.write(baos);
        byte[] bytes=baos.toByteArray(); 
        int duLength=GGEPKeys.GGEP_HEADER_DAILY_AVERAGE_UPTIME.length();
        int gueLength=GGEPKeys.GGEP_HEADER_UNICAST_SUPPORT.length();
        int upLength=GGEPKeys.GGEP_HEADER_UP_SUPPORT.length();
        int dhtLength = GGEPKeys.GGEP_HEADER_DHT_SUPPORT.length();
        int tlsLength = GGEPKeys.GGEP_HEADER_TLS_CAPABLE.length();
        int ggepLength=1   //magic number
                      +1   //"DUPTIME" extension flags
                      +duLength //ID
                      +1   //data length
                      +2   //data bytes
                      +1   //"GUE" extension flags
                      +gueLength // ID
                      +1   //data length
                      +0   //data bytes
                      +1   //"UP" extension flags
                      +upLength // ID
                      +1   // data length
                      +3  // data bytes
                      +1  //"DHT" extension flags
                      +dhtLength
                      +1   // data length
                      +3   // data bytes 
                      +1   // "TLS" extension flags
                      +tlsLength // ID
                      +1;  // EOGGEP.
        assertEquals(23+14+ggepLength, bytes.length);
        int offset=23+14;                                  //GGEP offset
        assertEquals((byte)0xc3,        bytes[offset]);    //GGEP magic number
        assertEquals((byte)(dhtLength), bytes[offset+1]);  //extension flags
        assertEquals((byte)'D',  bytes[offset+2]);
        assertEquals((byte)'H',  bytes[offset+3]);
        assertEquals((byte)'T',  bytes[offset+4]);
        assertEquals((byte)'D',  bytes[offset+2+dhtLength+5]);
        assertEquals((byte)'U',  bytes[offset+2+dhtLength+6]);
        assertEquals((byte)0x0B, bytes[offset+2+dhtLength+8]); // little byte of 523
        assertEquals((byte)0x02, bytes[offset+2+dhtLength+9]); // big byte of 523
        assertEquals((byte)'G',  bytes[offset+2+dhtLength+5+duLength+4]);
        assertEquals((byte)'U',  bytes[offset+2+dhtLength+5+duLength+5]);
        assertEquals((byte)'E',  bytes[offset+2+dhtLength+5+duLength+6]);
        assertEquals((byte)'T',  bytes[offset+2+dhtLength+5+duLength+4+gueLength+2]);
        assertEquals((byte)'L',  bytes[offset+2+dhtLength+5+duLength+4+gueLength+3]);
        assertEquals((byte)'S',  bytes[offset+2+dhtLength+5+duLength+4+gueLength+4]);
        assertEquals((byte)'U',  bytes[offset+2+dhtLength+5+duLength+4+gueLength+2+tlsLength+2]);
        assertEquals((byte)'P',  bytes[offset+2+dhtLength+5+duLength+4+gueLength+2+tlsLength+3]);

        //Decode and check contents.
        Message m=messageFactory.read(new ByteArrayInputStream(bytes), Network.TCP);
        PingReply pong=(PingReply)m;
        assertTrue(m instanceof PingReply);
        assertEquals(6349, pong.getPort());
        assertTrue("pong should have GGEP ext", pr.hasGGEPExtension());
        assertEquals(523, pong.getDailyUptime());
        assertTrue(pong.supportsUnicast());
        assertTrue(pong.isTLSCapable());

    }

    /** Test the raw bytes of an encoded GGEP'ed pong.  Then checks that
     *  these can be decoded.  Note that this will need to be changed if
     *  more extensions are added. */
    public void testGGEPEncodeDecodeNoGUESS() throws Exception {
        PingReply pr=pingReplyFactory.create(new byte[16], (byte)3, 6349, IP,
                                      0l, 0l, true, 523, false);        
        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        pr.write(baos);
        byte[] bytes=baos.toByteArray(); 
        int duLength=GGEPKeys.GGEP_HEADER_DAILY_AVERAGE_UPTIME.length();
        int upLength=GGEPKeys.GGEP_HEADER_UP_SUPPORT.length();
        int dhtLength = GGEPKeys.GGEP_HEADER_DHT_SUPPORT.length();
        int tlsLength = GGEPKeys.GGEP_HEADER_TLS_CAPABLE.length();
        int ggepLength=1   //magic number
                      +1   //"DUPTIME" extension flags
                      +duLength //ID
                      +1   //data length
                      +2   //data bytes
                      +1   //"UP" extension flags
                      +upLength // ID
                      +1   // data length
                      +3  // data bytes
                      +1  //"DHT" extension flags
                      +dhtLength
                      +1   // data length
                      +3   // data bytes 
                      +1   // "TLS" extension flags
                      +tlsLength // ID
                      +1;  // EOGGEP.
        assertEquals(23+14+ggepLength, bytes.length);
        int offset=23+14;                                  //GGEP offset
        assertEquals((byte)0xc3,        bytes[offset]);    //GGEP magic number
        assertEquals((byte)(dhtLength), bytes[offset+1]);  //extension flags
        assertEquals((byte)'D',  bytes[offset+2]);
        assertEquals((byte)'H',  bytes[offset+3]);
        assertEquals((byte)'T',  bytes[offset+4]);
        assertEquals((byte)'D',  bytes[offset+2+dhtLength+5]);
        assertEquals((byte)'U',  bytes[offset+2+dhtLength+6]);
        assertEquals((byte)0x0B, bytes[offset+2+dhtLength+8]); // little byte of 523
        assertEquals((byte)0x02, bytes[offset+2+dhtLength+9]); // big byte of 523
        assertEquals((byte)'T',  bytes[offset+2+dhtLength+5+duLength+4]);
        assertEquals((byte)'L',  bytes[offset+2+dhtLength+5+duLength+5]);
        assertEquals((byte)'S',  bytes[offset+2+dhtLength+5+duLength+6]);
        assertEquals((byte)'U',  bytes[offset+2+dhtLength+5+duLength+4+tlsLength+2]);
        assertEquals((byte)'P',  bytes[offset+2+dhtLength+5+duLength+4+tlsLength+3]);

        //Decode and check contents.
        Message m=messageFactory.read(new ByteArrayInputStream(bytes), Network.TCP);
        PingReply pong=(PingReply)m;
        assertTrue(m instanceof PingReply);
        assertEquals(6349, pong.getPort());
        assertTrue("pong should have GGEP ext", pr.hasGGEPExtension());
        assertEquals(523, pong.getDailyUptime());
        assertFalse(pong.supportsUnicast());
        assertTrue(pong.isTLSCapable());
    }
    
    /** Test the raw bytes of an encoded GGEP'ed pong.  Then checks that
     *  these can be decoded.  Note that this will need to be changed if
     *  more extensions are added. */
    public void testGGEPEncodeDecodeNoTLS() throws Exception {
        networkManagerStub.setIncomingTLSEnabled(false);
        PingReply pr=pingReplyFactory.create(new byte[16], (byte)3, 6349, IP,
                                      0l, 0l, true, 523, false);        
        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        pr.write(baos);
        byte[] bytes=baos.toByteArray(); 
        int duLength=GGEPKeys.GGEP_HEADER_DAILY_AVERAGE_UPTIME.length();
        int upLength=GGEPKeys.GGEP_HEADER_UP_SUPPORT.length();
        int dhtLength = GGEPKeys.GGEP_HEADER_DHT_SUPPORT.length();
        int ggepLength=1   //magic number
                      +1   //"DUPTIME" extension flags
                      +duLength //ID
                      +1   //data length
                      +2   //data bytes
                      +1   //"UP" extension flags
                      +upLength // ID
                      +1   // data length
                      +3  // data bytes
                      +1  //"DHT" extension flags
                      +dhtLength
                      +1   // data length
                      +3;  // data bytes
        assertEquals(23+14+ggepLength, bytes.length);
        int offset=23+14;                                  //GGEP offset
        assertEquals((byte)0xc3,        bytes[offset]);    //GGEP magic number
        assertEquals((byte)(dhtLength), bytes[offset+1]);  //extension flags
        assertEquals((byte)'D',  bytes[offset+2]);
        assertEquals((byte)'H',  bytes[offset+3]);
        assertEquals((byte)'T',  bytes[offset+4]);
        assertEquals((byte)'D',  bytes[offset+2+dhtLength+5]);
        assertEquals((byte)'U',  bytes[offset+2+dhtLength+6]);
        assertEquals((byte)0x0B, bytes[offset+2+dhtLength+8]); // little byte of 523
        assertEquals((byte)0x02, bytes[offset+2+dhtLength+9]); // big byte of 523
        assertEquals((byte)'U',  bytes[offset+2+dhtLength+5+duLength+4]);
        assertEquals((byte)'P',  bytes[offset+2+dhtLength+5+duLength+5]);


        //Decode and check contents.
        Message m=messageFactory.read(new ByteArrayInputStream(bytes), Network.TCP);
        PingReply pong=(PingReply)m;
        assertTrue(m instanceof PingReply);
        assertEquals(6349, pong.getPort());
        assertTrue("pong should have GGEP ext", pr.hasGGEPExtension());
        assertEquals(523, pong.getDailyUptime());
        assertFalse(pong.supportsUnicast());
        assertFalse(pong.isTLSCapable());
    }
    
    public void testPongTooSmall() throws Exception {
        byte[] bytes=new byte[23+25];  //one byte too small
        bytes[16]=Message.F_PING_REPLY;
        bytes[17]=(byte)3;     //hops
        bytes[18]=(byte)3;     //ttl
        bytes[19]=(byte)13;    //payload length
        ByteArrayInputStream in=new ByteArrayInputStream(bytes);
        try {
            messageFactory.read(in, Network.TCP);
            fail("No exception thrown");
        } catch (BadPacketException pass) { 
            //Pass!
        }
    }


    public void testQueryKeyPong() throws Exception {
        byte[] randBytes = new byte[8];
        (new Random()).nextBytes(randBytes);
        AddressSecurityToken qk;
        GUID guid = new GUID(GUID.makeGuid());
        byte[] ip={(byte)18, (byte)239, (byte)3, (byte)144};
        qk = new AddressSecurityToken(randBytes, macManager);
        PingReply pr = 
            pingReplyFactory.createQueryKeyReply(guid.bytes(), (byte) 1, 6346, ip,
                                          2, 2, true, qk);
        assertTrue(pr.getQueryKey().equals(qk));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        pr.write(baos);
        ByteArrayInputStream bais = 
             new ByteArrayInputStream(baos.toByteArray());
        PingReply prStreamed = (PingReply) messageFactory.read(bais, Network.TCP);
        assertTrue(prStreamed.getQueryKey().equals(qk));
            
    }
    
    public void testIpRequestPong() throws Exception {
        networkManagerStub.setAddress(InetAddress.getLocalHost().getAddress());
        
        // a pong carrying an ip:port
        Endpoint e = new Endpoint("1.2.3.4",5);
        PingReply p = pingReplyFactory.create(GUID.makeGuid(),(byte)1,e);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        p.write(baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        PingReply fromNet = (PingReply)messageFactory.read(bais, Network.TCP);
        
        assertEquals("1.2.3.4",fromNet.getMyInetAddress().getHostAddress());
        assertEquals(5,fromNet.getMyPort());
        
        // a pong carrying invalid port
        e = new Endpoint("1.2.3.4",5) {
            @Override
            public int getPort() {
                return 0;
            }
        };
        
        p = pingReplyFactory.create(GUID.makeGuid(),(byte)1,e);
        baos = new ByteArrayOutputStream();
        p.write(baos);
        bais = new ByteArrayInputStream(baos.toByteArray());
        fromNet = (PingReply)messageFactory.read(bais, Network.TCP);
        
        assertNull(fromNet.getMyInetAddress());
        assertEquals(0,fromNet.getMyPort());
        
        //a pong carrying private ip
        e = new Endpoint("192.168.0.1",20);
        p = pingReplyFactory.create(GUID.makeGuid(),(byte)1,e);
        baos = new ByteArrayOutputStream();
        p.write(baos);
        bais = new ByteArrayInputStream(baos.toByteArray());
        fromNet = (PingReply)messageFactory.read(bais, Network.TCP);
        
        assertNull(fromNet.getMyInetAddress());
        assertEquals(0,fromNet.getMyPort());
        
        // a pong not carrying ip:port
        p = pingReplyFactory.create(GUID.makeGuid(),(byte)1);
        baos = new ByteArrayOutputStream();
        p.write(baos);
        bais = new ByteArrayInputStream(baos.toByteArray());
        fromNet = (PingReply)messageFactory.read(bais, Network.TCP);
        
        assertNull(fromNet.getMyInetAddress());
        assertEquals(0,fromNet.getMyPort());
    }
    
    
    public void testUDPHostCacheExtension() throws Exception {
        GGEP ggep = new GGEP();
        ggep.put(GGEPKeys.GGEP_HEADER_UDP_HOST_CACHE);
        PingReply pr = pingReplyFactory.create(GUID.makeGuid(), (byte)1, 1,
                    new byte[] { 1, 1, 1, 1 },
                    (long)0, (long)0, false, ggep);
        assertTrue(pr.isUDPHostCache());
        assertEquals("1.1.1.1", pr.getUDPCacheAddress());
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        pr.write(out);
        byte[] b = out.toByteArray();
        
        PingReply read = (PingReply)messageFactory.read(new ByteArrayInputStream(b), Network.TCP);
        assertTrue(read.isUDPHostCache());
        assertEquals("1.1.1.1", read.getUDPCacheAddress());
        
        ggep = new GGEP();
        ggep.put(GGEPKeys.GGEP_HEADER_UDP_HOST_CACHE, "www.nowhere.org");
        pr = pingReplyFactory.create(GUID.makeGuid(), (byte)1, 1,
                    new byte[] { 1, 1, 1, 1 },
                    (long)0, (long)0, false, ggep);
        assertTrue(pr.isUDPHostCache());
        assertEquals("www.nowhere.org", pr.getUDPCacheAddress());

        out = new ByteArrayOutputStream();
        pr.write(out);
        b = out.toByteArray();
        
        read = (PingReply)messageFactory.read(new ByteArrayInputStream(b), Network.TCP);
        assertTrue(read.isUDPHostCache());
        assertEquals("www.nowhere.org", read.getUDPCacheAddress());
    }
    
    public void testPackedIPsInPong() throws Exception {
        GGEP ggep = new GGEP();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(new byte[] { 1, 1, 1, 1, 1, 0 } );
        out.write(new byte[] { 1, 2, 3, 4, 2, 0 } );
        out.write(new byte[] { 3, 4, 2, 3, 3, 0 } );
        out.write(new byte[] { (byte)0xFE, 0, 0, 3, 4, 0 } );
        ggep.put(GGEPKeys.GGEP_HEADER_PACKED_IPPORTS, out.toByteArray());
        PingReply pr = pingReplyFactory.create(
            GUID.makeGuid(), (byte)1, 1, new byte[] { 1, 1, 1, 1 },
            0, 0, false, ggep);

        List l = pr.getPackedIPPorts();
        assertEquals(4, l.size());
        IpPort ipp = (IpPort)l.get(0);
        assertEquals("1.1.1.1", ipp.getAddress());
        assertEquals(1, ipp.getPort());
        ipp = (IpPort)l.get(1);
        assertEquals("1.2.3.4", ipp.getAddress());
        assertEquals(2, ipp.getPort());
        ipp = (IpPort)l.get(2);
        assertEquals("3.4.2.3", ipp.getAddress());
        assertEquals(3, ipp.getPort());
        ipp = (IpPort)l.get(3);
        assertEquals("254.0.0.3", ipp.getAddress());
        assertEquals(4, ipp.getPort());
        
        // Try with invalid list of IPs (invalid by not being multiple of 6)
        ggep = new GGEP();
        out = new ByteArrayOutputStream();
        out.write(new byte[] { 1, 1, 1, 1, 1, 0 } );
        out.write(new byte[] { 1, 2, 3, 4, 2, 0 } );
        out.write(new byte[] { 3, 4, 2, 3, /* no port */ } );
        ggep.put(GGEPKeys.GGEP_HEADER_PACKED_IPPORTS, out.toByteArray());
        pr = pingReplyFactory.create(
            GUID.makeGuid(), (byte)1, 1, new byte[] { 1, 1, 1, 1 },
            0, 0, false, ggep);
        l = pr.getPackedIPPorts();
        assertTrue(l.isEmpty());
        
        // Try with invalid IPs (invalid by invalid IP addr)
        ggep = new GGEP();
        out = new ByteArrayOutputStream();
        out.write(new byte[] { 0, 0, 0, 0, 1, 0 } );
        out.write(new byte[] { 1, 2, 3, 4, 2, 0 } );
        out.write(new byte[] { 3, 4, 2, 3, 3, 0 } );
        ggep.put(GGEPKeys.GGEP_HEADER_PACKED_IPPORTS, out.toByteArray());
        pr = pingReplyFactory.create(
            GUID.makeGuid(), (byte)1, 1, new byte[] { 1, 1, 1, 1 },
            0, 0, false, ggep);
        l = pr.getPackedIPPorts();
        assertTrue(l.isEmpty());
        
        // Try with invalid IPs (invalid by invalid port)
        ggep = new GGEP();
        out = new ByteArrayOutputStream();
        out.write(new byte[] { 1, 1, 1, 1, 0, 0 } );
        out.write(new byte[] { 1, 2, 3, 4, 2, 0 } );
        out.write(new byte[] { 3, 4, 2, 3, 3, 0 } );
        ggep.put(GGEPKeys.GGEP_HEADER_PACKED_IPPORTS, out.toByteArray());
        pr = pingReplyFactory.create(
            GUID.makeGuid(), (byte)1, 1, new byte[] { 1, 1, 1, 1 },
            0, 0, false, ggep);
        l = pr.getPackedIPPorts();
        assertTrue(l.isEmpty());
        
        // Make sure the extension works with other GGEP flags (like UDP Host Cache)
        ggep = new GGEP();
        out = new ByteArrayOutputStream();
        out.write(new byte[] { 1, 1, 1, 1, 1, 0 } );
        out.write(new byte[] { 1, 2, 3, 4, 2, 0 } );
        out.write(new byte[] { 3, 4, 2, 3, 3, 0 } );
        out.write(new byte[] { (byte)0xFE, 0, 0, 3, 4, 0 } );
        ggep.put(GGEPKeys.GGEP_HEADER_PACKED_IPPORTS, out.toByteArray());
        ggep.put(GGEPKeys.GGEP_HEADER_UDP_HOST_CACHE);
        pr = pingReplyFactory.create(
            GUID.makeGuid(), (byte)1, 1, new byte[] { 1, 1, 1, 1 },
            0, 0, false, ggep);
        assertTrue(pr.isUDPHostCache());
        l = pr.getPackedIPPorts();
        assertEquals(4, l.size());
        ipp = (IpPort)l.get(0);
        assertEquals("1.1.1.1", ipp.getAddress());
        assertEquals(1, ipp.getPort());
        ipp = (IpPort)l.get(1);
        assertEquals("1.2.3.4", ipp.getAddress());
        assertEquals(2, ipp.getPort());
        ipp = (IpPort)l.get(2);
        assertEquals("3.4.2.3", ipp.getAddress());
        assertEquals(3, ipp.getPort());
        ipp = (IpPort)l.get(3);
        assertEquals("254.0.0.3", ipp.getAddress());
        assertEquals(4, ipp.getPort());        
        
        // and make sure we can read from network data.
        out = new ByteArrayOutputStream();
        pr.write(out);
        
        pr = (PingReply)messageFactory.read(new ByteArrayInputStream(out.toByteArray()), Network.TCP);
        assertTrue(pr.isUDPHostCache());
        l = pr.getPackedIPPorts();
        assertEquals(4, l.size());
        ipp = (IpPort)l.get(0);
        assertEquals("1.1.1.1", ipp.getAddress());
        assertEquals(1, ipp.getPort());
        ipp = (IpPort)l.get(1);
        assertEquals("1.2.3.4", ipp.getAddress());
        assertEquals(2, ipp.getPort());
        ipp = (IpPort)l.get(2);
        assertEquals("3.4.2.3", ipp.getAddress());
        assertEquals(3, ipp.getPort());
        ipp = (IpPort)l.get(3);
        assertEquals("254.0.0.3", ipp.getAddress());
        assertEquals(4, ipp.getPort());
        
        // Try with one of the constructors.
        l = new LinkedList(l);
        l.add(new Endpoint("1.5.3.5", 5));
        pr = pingReplyFactory.create(GUID.makeGuid(), (byte)1, l, null);
        assertFalse(pr.isUDPHostCache());
        l = pr.getPackedIPPorts();
        assertEquals(5, l.size());
        ipp = (IpPort)l.get(0);
        assertEquals("1.1.1.1", ipp.getAddress());
        assertEquals(1, ipp.getPort());
        ipp = (IpPort)l.get(1);
        assertEquals("1.2.3.4", ipp.getAddress());
        assertEquals(2, ipp.getPort());
        ipp = (IpPort)l.get(2);
        assertEquals("3.4.2.3", ipp.getAddress());
        assertEquals(3, ipp.getPort());
        ipp = (IpPort)l.get(3);
        assertEquals("254.0.0.3", ipp.getAddress());
        assertEquals(4, ipp.getPort());        
        ipp = (IpPort)l.get(4);
        assertEquals("1.5.3.5", ipp.getAddress());
        assertEquals(5, ipp.getPort());
        
        // Assert none of them supported TLS
        for(Object o : l ) {
            // right now only TLS-capable hosts become ExtendedEndpoints.
            assertNotInstanceof(ExtendedEndpoint.class, o);
        }
    }
    
    public void testTLSPackedIPPorts() throws Exception {
        List l = new LinkedList();
        
        // every % 6 add an endpoint that directly implements HostInfo,
        // every % 3 add an endpoint that doesn't, but give HostCatcher a capable
        for(int i = 1; i < 11; i++) {
            if(i % 6 == 0) {
                ExtendedEndpoint ep = new ExtendedEndpoint("1.2.3." + i, i+1);
                ep.setTLSCapable(true);
                l.add(ep);
                assertTrue(hostCatcher.isHostTLSCapable(ep));
            } else {
                l.add(new IpPortImpl("1.2.3." + i, i+1));
                if(i % 3 == 0) {
                    ExtendedEndpoint ep = new ExtendedEndpoint("1.2.3." + i, i+1);
                    ep.setTLSCapable(true);
                    hostCatcher.add(ep, true);
                }
                
                assertEquals(i%3==0, hostCatcher.isHostTLSCapable(new IpPortImpl("1.2.3." + i, i+1)));
            }
        }
        
        PingReply pr = pingReplyFactory.create(GUID.makeGuid(), (byte)1, l, null);
        l = pr.getPackedIPPorts();
        assertEquals(10, l.size());
        for(int i = 1; i < 11; i++) {
            IpPort ipp = (IpPort)l.get(i-1);
            assertEquals("1.2.3." + i, ipp.getAddress());
            assertEquals(i+1, ipp.getPort());            
            // These are the TLS hosts
            if(i%3==0) {
                assertInstanceof(ExtendedEndpoint.class, ipp);
                assertTrue(((ExtendedEndpoint)ipp).isTLSCapable());
            } else {
                assertNotInstanceof(ExtendedEndpoint.class, ipp);
            }
        }
    }
    
    public void testNetworkTLSPackedIpPorts() throws Exception {
        GGEP ggep = new GGEP();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(new byte[] { 1, 1, 1, 1, 1, 0 } );
        out.write(new byte[] { 1, 2, 3, 4, 2, 0 } );
        out.write(new byte[] { 3, 4, 2, 3, 3, 0 } );
        out.write(new byte[] { (byte)0xFE, 0, 0, 3, 4, 0 } );
        ggep.put(GGEPKeys.GGEP_HEADER_PACKED_IPPORTS, out.toByteArray());
        // mark the second & third items as TLS (and the fifth, just to see if it will ignore it)
        ggep.put(GGEPKeys.GGEP_HEADER_PACKED_IPPORTS_TLS, (0x40 | 0x20 | 0x8));
        PingReply pr = pingReplyFactory.create(
            GUID.makeGuid(), (byte)1, 1, new byte[] { 1, 1, 1, 1 },
            0, 0, false, ggep);

        List l = pr.getPackedIPPorts();
        assertEquals(4, l.size());
        IpPort ipp = (IpPort)l.get(0);
        assertEquals("1.1.1.1", ipp.getAddress());
        assertEquals(1, ipp.getPort());
        assertNotInstanceof(ExtendedEndpoint.class, ipp);
        ipp = (IpPort)l.get(1);
        assertEquals("1.2.3.4", ipp.getAddress());
        assertEquals(2, ipp.getPort());
        assertInstanceof(ExtendedEndpoint.class, ipp);
        assertTrue(((ExtendedEndpoint)ipp).isTLSCapable());
        ipp = (IpPort)l.get(2);
        assertEquals("3.4.2.3", ipp.getAddress());
        assertEquals(3, ipp.getPort());
        assertInstanceof(ExtendedEndpoint.class, ipp);
        assertTrue(((ExtendedEndpoint)ipp).isTLSCapable());
        ipp = (IpPort)l.get(3);
        assertEquals("254.0.0.3", ipp.getAddress());
        assertEquals(4, ipp.getPort());
        assertNotInstanceof(ExtendedEndpoint.class, ipp);
        
        //  and make sure we can read from network data.
        out = new ByteArrayOutputStream();
        pr.write(out);
        pr = (PingReply)messageFactory.read(new ByteArrayInputStream(out.toByteArray()), Network.TCP);
        l = pr.getPackedIPPorts();
        assertEquals(4, l.size());
        ipp = (IpPort)l.get(0);
        assertEquals("1.1.1.1", ipp.getAddress());
        assertEquals(1, ipp.getPort());
        assertNotInstanceof(ExtendedEndpoint.class, ipp);
        ipp = (IpPort)l.get(1);
        assertEquals("1.2.3.4", ipp.getAddress());
        assertEquals(2, ipp.getPort());
        assertInstanceof(ExtendedEndpoint.class, ipp);
        assertTrue(((ExtendedEndpoint)ipp).isTLSCapable());
        ipp = (IpPort)l.get(2);
        assertEquals("3.4.2.3", ipp.getAddress());
        assertEquals(3, ipp.getPort());
        assertInstanceof(ExtendedEndpoint.class, ipp);
        assertTrue(((ExtendedEndpoint)ipp).isTLSCapable());
        ipp = (IpPort)l.get(3);
        assertEquals("254.0.0.3", ipp.getAddress());
        assertEquals(4, ipp.getPort());
        assertNotInstanceof(ExtendedEndpoint.class, ipp);
        
    }
    
    public void testPackedHostCachesInPong() throws Exception {
        // test with compression.
        GGEP ggep = new GGEP();
        List<String> addrs = new LinkedList<String>();
        addrs.add("1.2.3.4:81");
        addrs.add("www.limewire.com:6379");
        addrs.add("www.eff.org");
        addrs.add("www.test.org:1&something=somethingelse&nothing=this");
        ggep.putCompressed(GGEPKeys.GGEP_HEADER_PACKED_HOSTCACHES, toBytes(addrs));
        PingReply pr = pingReplyFactory.create(
            GUID.makeGuid(), (byte)1, 1, new byte[] { 1, 1, 1, 1 },
            0, 0, false, ggep);

        Set s = new TreeSet(IpPort.COMPARATOR);
        s.addAll(pr.getPackedUDPHostCaches());
        assertEquals(4, s.size());
        IpPort ipp = new IpPortImpl("1.2.3.4", 81);
        assertContains(s, ipp);
        s.remove(ipp);
        ipp = new IpPortImpl("www.limewire.com", 6379);
        assertContains(s, ipp);
        s.remove(ipp);
        ipp = new IpPortImpl("www.eff.org", 6346);
        assertContains(s, ipp);
        s.remove(ipp);
        ipp = new IpPortImpl("www.test.org", 1);
        assertContains(s, ipp);
        s.remove(ipp);
        assertEquals(0, s.size());
        
        // test without compression
        ggep = new GGEP();
        addrs.clear();
        addrs.add("1.2.3.4:81");
        addrs.add("www.limewire.com:6379");
        addrs.add("www.eff.org");
        addrs.add("www.test.org:1&something=somethingelse&nothing=this");
        ggep.put(GGEPKeys.GGEP_HEADER_PACKED_HOSTCACHES, toBytes(addrs));
        pr = pingReplyFactory.create(
            GUID.makeGuid(), (byte)1, 1, new byte[] { 1, 1, 1, 1 },
            0, 0, false, ggep);
        s.clear();
        s.addAll(pr.getPackedUDPHostCaches());
        assertEquals(4, s.size());
        ipp = new IpPortImpl("1.2.3.4", 81);
        assertContains(s, ipp);
        s.remove(ipp);
        ipp = new IpPortImpl("www.limewire.com", 6379);
        assertContains(s, ipp);
        s.remove(ipp);
        ipp = new IpPortImpl("www.eff.org", 6346);
        assertContains(s, ipp);
        s.remove(ipp);
        ipp = new IpPortImpl("www.test.org", 1);
        assertContains(s, ipp);
        s.remove(ipp);
        assertEquals(0, s.size());        
        
        ggep = new GGEP();
        addrs.clear();
        addrs.add("1.2.3.4:");
        addrs.add("3.4.2.3");
        addrs.add("5.4.3.2:1:1");
        addrs.add("13.13.1.1:notanumber");
        ggep.putCompressed(GGEPKeys.GGEP_HEADER_PACKED_HOSTCACHES, toBytes(addrs));
        pr = pingReplyFactory.create(
            GUID.makeGuid(), (byte)1, 1, new byte[] { 1, 1, 1, 1 },
            0, 0, false, ggep);
        s.addAll(pr.getPackedUDPHostCaches());
        assertEquals(1, s.size());
        ipp = new IpPortImpl("3.4.2.3", 6346);
        assertContains(s, ipp);
        s.remove(ipp);
        assertEquals(0, s.size());
        
        ggep = new GGEP();
        ggep.put(GGEPKeys.GGEP_HEADER_PACKED_HOSTCACHES, new byte[0]);
        pr = pingReplyFactory.create(
            GUID.makeGuid(), (byte)1, 1, new byte[] { 1, 1, 1, 1 },
            0, 0, false, ggep);
        assertEquals(0, pr.getPackedUDPHostCaches().size());
        
        ggep = new GGEP();
        ggep.putCompressed(GGEPKeys.GGEP_HEADER_PACKED_HOSTCACHES, new byte[] { 1, 1, 1, 1 } );
        pr = pingReplyFactory.create(
            GUID.makeGuid(), (byte)1, 1, new byte[] { 1, 1, 1, 1 },
            0, 0, false, ggep);
        assertEquals(0, pr.getPackedUDPHostCaches().size());
        
        ggep = new GGEP();
        ggep.put(GGEPKeys.GGEP_HEADER_PACKED_HOSTCACHES, new byte[] { 1, 1, 1, 1 } );
        pr = pingReplyFactory.create(
            GUID.makeGuid(), (byte)1, 1, new byte[] { 1, 1, 1, 1 },
            0, 0, false, ggep);
        assertEquals(0, pr.getPackedUDPHostCaches().size());        
    }
    
    private byte[] toBytes(List<String> l) throws Exception {
        StringBuffer sb = new StringBuffer();
        for(Iterator i = l.iterator(); i.hasNext(); ) {
            sb.append(i.next().toString());
            if(i.hasNext())
                sb.append("\n");
        }
        return StringUtils.toUTF8Bytes(sb.toString());
    }
    
    private void addIP(byte[] payload) {
        // fill up the ip so its not blank.
        payload[2] = 1;
        payload[3] = 1;
        payload[4] = 1;
        payload[5] = 1;
    }

    /**
     * Utility class that overrides ConnectionManager methods for getting the
     * number of free leaf and ultrapeer slots.
     */
    @Singleton
    private static class TestConnectionManager extends ConnectionManagerImpl {
        private int NUM_FREE_NON_LEAF_SLOTS;
        private int NUM_FREE_LEAF_SLOTS;

        @Inject
        public TestConnectionManager(NetworkManager networkManager,
                Provider<HostCatcher> hostCatcher,
                @Named("global") Provider<ConnectionDispatcher> connectionDispatcher,
                @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
                Provider<SimppManager> simppManager,
                CapabilitiesVMFactory capabilitiesVMFactory,
                RoutedConnectionFactory managedConnectionFactory,
                Provider<QueryUnicaster> queryUnicaster,
                SocketsManager socketsManager,
                ConnectionServices connectionServices,
                Provider<NodeAssigner> nodeAssigner, 
                 Provider<IPFilter> ipFilter,
                ConnectionCheckerManager connectionCheckerManager,
                PingRequestFactory pingRequestFactory,
                NetworkInstanceUtils networkInstanceUtils) {
            super(networkManager, hostCatcher, connectionDispatcher, 
                    backgroundExecutor, simppManager, capabilitiesVMFactory,
                    managedConnectionFactory, queryUnicaster, 
                    socketsManager, connectionServices, nodeAssigner, 
                    ipFilter, connectionCheckerManager, pingRequestFactory, networkInstanceUtils);
        }
        
        @Override
        public int getNumFreeNonLeafSlots() {
            return NUM_FREE_NON_LEAF_SLOTS != 0 ? NUM_FREE_NON_LEAF_SLOTS : super.getNumFreeNonLeafSlots(); 
        }
        
        public void setNumFreeNonLeafSlots(int numFreeNonLeafSlots) {
            NUM_FREE_NON_LEAF_SLOTS = numFreeNonLeafSlots;
        }
        
        @Override
        public int getNumFreeLeafSlots() {
            return NUM_FREE_LEAF_SLOTS != 0 ? NUM_FREE_LEAF_SLOTS : super.getNumFreeLeafSlots();
        }
        
        public void setNumFreeLeafSlots(int numFreeLeafSlots) {
            NUM_FREE_LEAF_SLOTS = numFreeLeafSlots; 
        }
    }
    // TODO: build a test to test multiple GGEP blocks in the payload!!  the
    // implementation does not cover this it seems, so it should fail ;)

}


