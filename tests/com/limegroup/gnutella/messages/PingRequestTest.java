package com.limegroup.gnutella.messages;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.LinkedList;
import java.util.List;

import junit.framework.Test;

import org.limewire.core.settings.ApplicationSettings;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.UltrapeerSettings;
import org.limewire.io.GGEP;
import org.limewire.io.GUID;
import org.limewire.io.LocalSocketAddressProvider;
import org.limewire.net.TLSManager;
import org.limewire.util.NameValue;
import org.limewire.util.PrivilegedAccessor;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.stubs.LocalSocketAddressProviderStub;

@SuppressWarnings("unchecked")
// TODO stub / mock out ping requests
public class PingRequestTest extends com.limegroup.gnutella.util.LimeTestCase {
    private PingRequestFactory pingRequestFactory;
    private MessageFactory messageFactory;
    private ConnectionServices connectionServices;
    private TLSManager tlsManager;

    public PingRequestTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(PingRequestTest.class);
    }


    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    //TODO: test other parts of ping!
    
    @Override
    protected void setUp() throws Exception {
        Injector injector = LimeTestUtils.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(LocalSocketAddressProvider.class).to(LocalSocketAddressProviderStub.class);
            }
        });
        pingRequestFactory = injector.getInstance(PingRequestFactory.class);
        messageFactory = injector.getInstance(MessageFactory.class);
        connectionServices = injector.getInstance(ConnectionServices.class);
        tlsManager = injector.getInstance(TLSManager.class);
    }

    public void testQueryKeyPing() throws Exception {
        PingRequest pr = pingRequestFactory.createQueryKeyRequest();
        assertFalse(pr.isQueryKeyRequest()); // hasn't been hopped yet
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        pr.write(baos);
        ByteArrayInputStream bais = 
        new ByteArrayInputStream(baos.toByteArray());
        PingRequest prRead = (PingRequest) messageFactory.read(bais, Network.TCP);
        prRead.hop();
        assertTrue(prRead.isQueryKeyRequest());
    }


    public void testGGEPPing() throws Exception {
        // first make a GGEP block....
        GGEP ggepBlock = new GGEP(true);
        ggepBlock.put(GGEPKeys.GGEP_HEADER_QUERY_KEY_SUPPORT);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ggepBlock.write(baos);

        byte[] ggepBytes = baos.toByteArray();

        //Headers plus payload(6 bytes)
        byte[] buffer = new byte[23+ggepBytes.length+1];
        byte[] guid = GUID.makeGuid();//get a GUID
        System.arraycopy(guid,0,buffer,0,guid.length);//copy GUID
        int currByte = guid.length;
        buffer[currByte] = Message.F_PING;
        currByte++;
        buffer[currByte] = 0x0001; // TTL 
        currByte++;
        buffer[currByte] = 0x0000;// Hops
        currByte++;
        buffer[currByte] = (byte)(ggepBytes.length+1);//1st byte = 6
        currByte++;
        buffer[currByte] = 0x0000;//2nd byte = 0
        currByte++;
        buffer[currByte] = 0x0000;//3rd byte = 0
        currByte++;
        buffer[currByte] = 0x0000;//4th byte = 0 - remember it's little endian
        currByte++;
        // stick in GGEP
        for (int i = 0; i < ggepBytes.length; i++)
            buffer[currByte++] = ggepBytes[i];
        buffer[currByte++] = 0; // trailing 0
        assertGreaterThanOrEquals(buffer.length, currByte);

        //OK, ggep ping ready
        ByteArrayInputStream stream = new ByteArrayInputStream(buffer);
        Message m;
        m = messageFactory.read(stream, Network.TCP);
        PingRequest pr;
        pr = (PingRequest)m;
        assertTrue(!pr.isQueryKeyRequest());
        pr.hop();
        assertTrue(pr.isQueryKeyRequest());
        //Came this far means its all OK
    }


    public void testBigPing() throws Exception {
        byte[] buffer = new byte[23+16];//Headers plus payload(16 bytes)
        //Note: We choose a size of 16 to make sure it does not create a
        //group ping, 
        byte[] guid = GUID.makeGuid();//get a GUID
        System.arraycopy(guid,0,buffer,0,guid.length);//copy GUID
        int currByte = guid.length;
        buffer[currByte] = Message.F_PING;
        currByte++;
        buffer[currByte] = 0x0004; // TTL 
        currByte++;
        buffer[currByte] = 0x0000;// Hops
        currByte++;
        buffer[currByte] = 0x0010;//1st byte = 16, A thro' P
        currByte++;
        buffer[currByte] = 0x0000;//2nd byte = 0
        currByte++;
        buffer[currByte] = 0x0000;//3rd byte = 0
        currByte++;
        buffer[currByte] = 0x0000;//4th byte = 0 - remember it's little endian
        currByte++;
        byte c = 65;//"A"
        byte[] payload = new byte[16];//to be used to test constrcutor
        for(int i=0; i<16; i++){
            buffer[currByte] = c;
            payload[i] = buffer[currByte];
            currByte++;
            c++;
        }
        //OK, Big ping ready
        ByteArrayInputStream stream = new ByteArrayInputStream(buffer);
        Message m;
        m = messageFactory.read(stream, Network.TCP);
        PingRequest pr;
        pr = (PingRequest)m;
        ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
        pr.write(outBuffer);
        byte [] outb = outBuffer.toByteArray();
        String out = new String(outb,23,outb.length-23);
        assertEquals("Wrong payload", "ABCDEFGHIJKLMNOP", out);
      
        //Test the new constructor for big pings read from the network
      
        PingRequest bigPing = pingRequestFactory.createFromNetwork(GUID.makeGuid(), (byte)7,
                (byte)0, payload,  Network.UNKNOWN);
        assertEquals(0, bigPing.getHops());
        assertEquals(7, bigPing.getTTL());
        assertEquals("bad length", 16, bigPing.getLength());
        //Came this far means its all OK
    }
    
    public void testAddIP() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        //try a ping which doesn't ask for ip 
        
        PingRequest noRequest = pingRequestFactory.createPingRequest((byte)1);
        assertFalse(noRequest.requestsIP());
        
        noRequest.write(baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        PingRequest fromNet = (PingRequest) messageFactory.read(bais, Network.TCP);
        
        assertFalse(fromNet.requestsIP());
        
        //try a ping without any other ggeps except the ip request
        byte []guid = GUID.makeGuid();
        
        PingRequest noPayload = pingRequestFactory.createPingRequest(guid, (byte)1,
                (byte)0);
        
        assertFalse(noPayload.requestsIP());
        
        noPayload.addIPRequest();
        assertTrue(noPayload.requestsIP());
        
        baos = new ByteArrayOutputStream();
        noPayload.write(baos);
        bais = new ByteArrayInputStream(baos.toByteArray());
        fromNet = (PingRequest) messageFactory.read(bais, Network.TCP);
        
        assertTrue(fromNet.requestsIP());
        
        // now try a ping with locale
        String original = ApplicationSettings.LANGUAGE.getValue();
        ApplicationSettings.LANGUAGE.setValue("zz");
        PingRequest withLocale = pingRequestFactory.createPingRequest((byte)1);
        ApplicationSettings.LANGUAGE.setValue(original);
        
        withLocale.addIPRequest();
        
        baos = new ByteArrayOutputStream();
        withLocale.write(baos);
        bais = new ByteArrayInputStream(baos.toByteArray());
        fromNet = (PingRequest) messageFactory.read(bais, Network.TCP);
        assertTrue(fromNet.requestsIP());
        assertEquals("zz",fromNet.getLocale());
     
        assertFalse(fromNet.supportsCachedPongs());
        assertNull(fromNet.getSupportsCachedPongData());   
    }
    
    public void testUDPPingRequest() {
        PingRequest pr = pingRequestFactory.createUDPPing();
        assertTrue(pr.supportsCachedPongs());
        
        // Test +UP +TLS
        UltrapeerSettings.MIN_CONNECT_TIME.setValue(0);
        UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(true);
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(true);
        UltrapeerSettings.NEED_MIN_CONNECT_TIME.setValue(false);
        tlsManager.setIncomingTLSEnabled(true);
        assertTrue(connectionServices.isSupernode());
        pr = pingRequestFactory.createUDPPing();
        assertFalse(pr.requestsIP());
        byte[] data = pr.getSupportsCachedPongData();
        assertEquals(0x1, data[0] & 0x1);
        assertEquals(0x2, data[0] & 0x2);
        
        // +UP -TLS
        tlsManager.setIncomingTLSEnabled(false);
        assertTrue(connectionServices.isSupernode());
        pr = pingRequestFactory.createUDPPing();
        assertFalse(pr.requestsIP());
        data = pr.getSupportsCachedPongData();
        assertEquals(0x1, data[0] & 0x1);
        assertEquals(0x0, data[0] & 0x2);
        
        // Test -UP +TLS
        UltrapeerSettings.DISABLE_ULTRAPEER_MODE.setValue(true);
        UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(false);
        tlsManager.setIncomingTLSEnabled(true);
        assertFalse(connectionServices.isSupernode());
        pr = pingRequestFactory.createUDPPing();
        assertFalse(pr.requestsIP());
        data = pr.getSupportsCachedPongData();
        assertEquals(0x0, data[0] & 0x1);
        assertEquals(0x2, data[0] & 0x2);
        
        // Test -UP -TLS
        tlsManager.setIncomingTLSEnabled(false);
        assertFalse(connectionServices.isSupernode());
        pr = pingRequestFactory.createUDPPing();
        assertFalse(pr.requestsIP());
        data = pr.getSupportsCachedPongData();
        assertEquals(0x0, data[0] & 0x1);
        assertEquals(0x0, data[0] & 0x2);
        
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(false);
        pr = pingRequestFactory.createUDPPing();
        assertTrue(pr.requestsIP());
    }
    
    public void testSupportsCachedPongData() throws Exception {
        List ggeps = new LinkedList();
        ggeps.add(new NameValue(GGEPKeys.GGEP_HEADER_SUPPORT_CACHE_PONGS));
        PingRequest pr = make(ggeps);
        assertTrue(pr.supportsCachedPongs());
        byte[] data = pr.getSupportsCachedPongData();
        assertNotNull(data);
        assertEquals(0, data.length);
        
        ggeps.clear();
        ggeps.add(new NameValue(GGEPKeys.GGEP_HEADER_SUPPORT_CACHE_PONGS, new byte[1]));
        pr = make(ggeps);
        assertTrue(pr.supportsCachedPongs());
        data = pr.getSupportsCachedPongData();
        assertNotNull(data);
        assertEquals(1, data.length);        
    }
    
    public void testRequestsDHTIPP() throws Exception{
        List ggeps = new LinkedList();
        ggeps.add(new NameValue(GGEPKeys.GGEP_HEADER_DHT_IPPORTS));
        PingRequest pr = make(ggeps);
        assertTrue(pr.requestsDHTIPP());
        assertFalse(pr.supportsCachedPongs());
    }
    
    private PingRequest make(List ggeps) throws Exception {
        return (PingRequest)PrivilegedAccessor.invokeConstructor(
            PingRequestImpl.class,
            new Object[] { new byte[16], (byte) 1, ggeps },
            new Class[] { byte[].class, byte.class, List.class });
    }
}
