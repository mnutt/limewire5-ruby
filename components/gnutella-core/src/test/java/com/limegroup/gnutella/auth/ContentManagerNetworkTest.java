package com.limegroup.gnutella.auth;


import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import org.limewire.core.settings.ContentSettings;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;

import com.google.inject.Injector;
import com.limegroup.gnutella.Acceptor;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.vendor.ContentRequest;
import com.limegroup.gnutella.messages.vendor.ContentResponse;
import com.limegroup.gnutella.util.LimeTestCase;

import junit.framework.Test;
 
public class ContentManagerNetworkTest extends LimeTestCase {
    
    private Injector injector;
    
    private static final String S_URN_1 = "urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB";
    
    private static URN URN_1;
    
    private ContentManager mgr;
    private ContentResponse crOne;
    private Observer one;
    
    private static  int LISTEN_PORT = 9172;
    
    private MessageRouter messageRouter;
    private Acceptor acceptor;
    private UDPService udpService;
    private IpPortContentAuthorityFactory ipPortContentAuthorityFactory;
    
    
    public ContentManagerNetworkTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(ContentManagerNetworkTest.class);
    }
    
    
    @Override
    public void setUp() throws Exception {
        injector = LimeTestUtils.createInjector();
        
        messageRouter = injector.getInstance(MessageRouter.class);
        acceptor      = injector.getInstance(Acceptor.class);
        udpService    = injector.getInstance(UDPService.class);
        
        
        messageRouter.start();
        
        LISTEN_PORT++; // TODO: Remove port hack, new port needed on each run
        acceptor.setListeningPort(LISTEN_PORT); 
        udpService.start();
        
        URN_1 = URN.createSHA1Urn(S_URN_1);
        
        ContentSettings.CONTENT_MANAGEMENT_ACTIVE.setValue(true);
        ContentSettings.USER_WANTS_MANAGEMENTS.setValue(true);
        
        
        ipPortContentAuthorityFactory = injector.getInstance(IpPortContentAuthorityFactory.class);
        mgr = new ContentManager(ipPortContentAuthorityFactory);
        crOne = new ContentResponse(URN_1, true);
        one = new Observer();
        assertNull(mgr.getResponse(URN_1));
        assertNull(one.urn);
        assertNull(one.response);
    }
    
    @Override
    public void tearDown() throws Exception {
        mgr.stop();
        
        acceptor.shutdown();
        udpService.shutdown();
    }
    
    public void testMessageSent() throws Exception {
        DatagramSocket socket = new DatagramSocket();
        socket.setReuseAddress(true);
        socket.setSoTimeout(5000);
        
        mgr.setContentAuthority(ipPortContentAuthorityFactory
                .createIpPortContentAuthority(new IpPortImpl("127.0.0.1", socket.getLocalPort())));
        mgr.request(URN_1, one, 2000);
        
        DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
        socket.receive(packet);
        byte[] read = packet.getData();
        
        ContentRequest expectSentMsg = new ContentRequest(URN_1);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        expectSentMsg.write(out);
        byte[] expectSentBytes = out.toByteArray();
        
        assertEquals(expectSentBytes.length, packet.getLength());
        
        // start at 16, because less than that is the GUID which is random.
        for(int i = 16; i < expectSentBytes.length; i++)
            assertEquals("byte[" + i + "] wrong. ", expectSentBytes[i], read[i]);
        
        socket.close();
    }
    
    public void testDelayedRequestSent() throws Exception {
        final DatagramSocket socket = new DatagramSocket();
        socket.setReuseAddress(true);
        socket.setSoTimeout(5000);
        final IpPort authority = new IpPortImpl("127.0.0.1", socket.getLocalPort());
        
        mgr.stop();
        mgr = new ContentManager(ipPortContentAuthorityFactory) {
            @Override
            protected ContentAuthority getDefaultContentAuthority() {
                return ipPortContentAuthorityFactory
                        .createIpPortContentAuthority(authority);
            }
        };
        mgr.request(URN_1, one, 2000);
        mgr.start();
        
        DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
        socket.receive(packet);
        byte[] read = packet.getData();
        
        ContentRequest expectSentMsg = new ContentRequest(URN_1);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        expectSentMsg.write(out);
        byte[] expectSentBytes = out.toByteArray();
        
        assertGreaterThan(30, expectSentBytes.length);
        assertEquals(expectSentBytes.length, packet.getLength());
        // start at 16, because less than that is the GUID which is random.
        for(int i = 16; i < expectSentBytes.length; i++)
            assertEquals("byte[" + i + "] wrong. ", expectSentBytes[i], read[i]);
        
        socket.close();
    }
    
    public void testResponseReceived() throws Exception {        
        mgr.stop();
        mgr = injector.getInstance(ContentManager.class);
        mgr.request(URN_1, one, 4000);
        udpService.send(crOne, InetAddress.getLocalHost(), LISTEN_PORT);
        Thread.sleep(1000); // let the message process.
        assertNotNull(mgr.getResponse(URN_1));
        assertTrue(mgr.getResponse(URN_1).isOK());
        assertTrue(mgr.isVerified(URN_1));
        assertEquals(one.urn, URN_1);
        assertEquals(one.response, mgr.getResponse(URN_1));
    }
    
    
    private static class Observer implements ContentResponseObserver {
        private URN urn;
        private ContentResponseData response;
        
        public void handleResponse(URN urn, ContentResponseData response) {
            this.urn = urn;
            this.response = response;
        }
    }
    
}
