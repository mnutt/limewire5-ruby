package com.limegroup.gnutella;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Test;

import org.limewire.gnutella.tests.ActivityCallbackStub;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;

import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.Stage;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.messages.Message.Network;

/**
 * Tests that UDP host ranker works.
 */
@SuppressWarnings("unchecked")
public class UDPHostRankerTest extends ClientSideTestCase {
    
    final int PORT = 6669;
    private PingRequestFactory pingRequestFactory;
    private UniqueHostPinger uniqueHostPinger;
    private MessageFactory messageFactory;
    private PingReplyFactory pingReplyFactory;
    private MessageRouterImpl messageRouter;
    
    {
        TIMEOUT = 1000; // override super
    }

    public UDPHostRankerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(UDPHostRankerTest.class);
    }    

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    protected void setUp() throws Exception {
        Injector injector = LimeTestUtils.createInjector(Stage.PRODUCTION, MyActivityCallback.class);
        super.setUp(injector);
        pingRequestFactory = injector.getInstance(PingRequestFactory.class);
        uniqueHostPinger = injector.getInstance(UniqueHostPinger.class);
        messageFactory = injector.getInstance(MessageFactory.class);
        pingReplyFactory = injector.getInstance(PingReplyFactory.class);
        messageRouter = (MessageRouterImpl) injector.getInstance(MessageRouter.class);
    }
    
    public void testRanker() throws Exception {
        DatagramSocket[] udps = new DatagramSocket[20];
        for (int i = 0; i < udps.length; i++)   
            udps[i] = new DatagramSocket();
        
        final List list = new ArrayList();
        for (int i = 0; i < udps.length; i++)
            list.add(new IpPortImpl(udps[i]));

        final MLImpl ml = new MLImpl();

        PingRequest pr = pingRequestFactory.createPingRequest(GUID.makeGuid(), (byte)1);
        UDPPinger pinger = uniqueHostPinger;
        pinger.rank(list, ml, null, pr);
        
        Thread.sleep(500);
        assertTrue(ml.registered);

        for (int i = 0; i < udps.length; i++) {
            DatagramPacket pack = new DatagramPacket(new byte[1000], 1000);
            udps[i].setSoTimeout(TIMEOUT);
            try {
                udps[i].receive(pack);
            } catch (IOException bad) {
               fail("Did not get expected message, i = " + i, bad);
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            // as long as we don't get a ClassCastException we are good to go
            PingRequest ping = (PingRequest) messageFactory.read(in, Network.TCP);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PingReply pong = pingReplyFactory.create(ping.getGUID(), (byte)1);
            pong.write(baos);
            pack = new DatagramPacket(baos.toByteArray(), 
                                      baos.toByteArray().length,
                                      InetAddress.getLocalHost(), PORT);
            udps[i].send(pack);
        }

        Thread.sleep(2000); //processing
        assertEquals(udps.length, ml.count);

        // wait 20 seconds, make sure MessageRouter's map is clear
        Thread.sleep(20000);
        Map map = messageRouter.getMessageListenerMap();
        assertEquals(0, map.size());
        assertTrue(ml.unregistered);
    }

    //////////////////////////////////////////////////////////////////
    
    private class MLImpl implements MessageListener {
        public int count = 0;
        public boolean registered = false;
        public boolean unregistered = false;
        public void processMessage(Message m, ReplyHandler handler) {
            assertTrue(m instanceof PingReply);
            count++;
        }
        public void registered(byte[] guid) {
            registered = true;
        }
        public void unregistered(byte[] guid) {
            unregistered = true;
        }
    }
    
    private class IpPortImpl implements IpPort {
        DatagramSocket _ds;
        public IpPortImpl(DatagramSocket ds) {
            _ds = ds;
        }
        public InetSocketAddress getInetSocketAddress() {
            return new InetSocketAddress(getInetAddress(), getPort());
        }
        public InetAddress getInetAddress() { 
            try {
                return InetAddress.getLocalHost();
            }
            catch (Exception noway) {}
            return null;
        }
        public int getPort() { return _ds.getLocalPort(); }
        public String getAddress() {
            try {
                return InetAddress.getLocalHost().getHostAddress();
            }
            catch (Exception noway) {}
            return null;
        }
    }

    @Override
    public int getNumberOfPeers() {
        return 1;
    }
    
    @Singleton
    public static class MyActivityCallback extends ActivityCallbackStub {
        private RemoteFileDesc rfd = null;
        public RemoteFileDesc getRFD() {
            return rfd;
        }

        @Override
        public void handleQueryResult(RemoteFileDesc rfd,
                                      QueryReply queryReply,
                                      Set locs) {
            this.rfd = rfd;
        }
    }


}
