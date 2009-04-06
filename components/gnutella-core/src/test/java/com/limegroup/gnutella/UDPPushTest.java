package com.limegroup.gnutella;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.TreeSet;

import junit.framework.Test;

import org.limewire.concurrent.ThreadExecutor;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.SpeedConstants;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.net.ConnectionDispatcher;
import org.limewire.net.TLSManager;
import org.limewire.nio.NIOSocket;
import org.limewire.util.StringUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Stage;
import com.google.inject.name.Names;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.stubs.AcceptorStub;

/**
 * Tests the issuing of Push Request through udp and failover to tcp.
 */
public class UDPPushTest extends LimeTestCase {

    private final static byte[] guid = GUID.fromHexString("BC1F6870696111D4A74D0001031AE043");

    private RemoteFileDesc rfd1, rfd2, rfdAlt;

    /**
     * the socket that will supposedly be the push download
     */
    private Socket socket;

    /**
     * the socket that will listen for the tcp push request
     */
    private ServerSocket serversocket;

    /**
     * the socket that will listen for the udp push request
     */
    private DatagramSocket udpsocket;

    private MessageFactory messageFactory;

    private ConnectionDispatcher connectionDispatcher;

    private Injector injector;
    private TLSManager tlsManager;

    private PushEndpointFactory pushEndpointFactory;

    public UDPPushTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(UDPPushTest.class);
    }

    @Override
    public void setUp() throws Exception {
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        ConnectionSettings.SOLICITED_GRACE_PERIOD.setValue(5000l);

        // initialize services
        injector = LimeTestUtils.createInjector(Stage.PRODUCTION, new AbstractModule() {
            @Override
            protected void configure() {
                bind(Acceptor.class).to(AcceptorStub.class);
            }
        });
        
        AcceptorStub acceptor = (AcceptorStub) injector.getInstance(Acceptor.class);
        acceptor.setAcceptedIncoming(true);
        acceptor.setAddress(InetAddress.getLocalHost());
        
        serversocket = new ServerSocket(10000);
        serversocket.setSoTimeout(1000);

        udpsocket = new DatagramSocket(20000);
        udpsocket.setSoTimeout(1000);
        
        messageFactory = injector.getInstance(MessageFactory.class);
        connectionDispatcher = injector.getInstance(Key.get(ConnectionDispatcher.class, Names.named("global")));
        pushEndpointFactory = injector.getInstance(PushEndpointFactory.class);
        
        tlsManager = injector.getInstance(TLSManager.class);

        // initialize test data
        long now = System.currentTimeMillis();
        Set<IpPortImpl> proxies = new TreeSet<IpPortImpl>(IpPort.COMPARATOR);
        proxies.add(new IpPortImpl(InetAddress.getLocalHost().getHostAddress(), 10000));
        PushEndpoint pushEndpoint = pushEndpointFactory.createPushEndpoint(guid, proxies, PushEndpoint.PPTLS_BINARY, 0, new ConnectableImpl("127.0.0.1", 20000, true));

        rfd1 = injector.getInstance(RemoteFileDescFactory.class).createRemoteFileDesc(pushEndpoint, 30l, "file1", 100, guid,
                SpeedConstants.CABLE_SPEED_INT, 1, false, null, URN.NO_URN_SET, false, "LIME", now);

        rfd2 = injector.getInstance(RemoteFileDescFactory.class).createRemoteFileDesc(pushEndpoint, 31l, "file2", 100, guid,
                SpeedConstants.CABLE_SPEED_INT, 1, false, null, URN.NO_URN_SET, false, "LIME", now);

        rfdAlt = injector.getInstance(RemoteFileDescFactory.class).createRemoteFileDesc(pushEndpoint, 30l, "file1", 100, guid,
                SpeedConstants.CABLE_SPEED_INT, 1, false, null, URN.NO_URN_SET, false, "ALT", now);
     
        injector.getInstance(LifecycleManager.class).start();
    }

    @Override
    protected void tearDown() throws Exception {
        injector.getInstance(LifecycleManager.class).shutdown();
        
        serversocket.close();
        udpsocket.close();
    }
    
    /**
     * tests the scenario where an udp push is sent, but no connection is
     * received so the failover tcp push is sent.
     */
    public void testUDPPushFailover() throws Exception {
        requestPush(rfd1);

        try {
            serversocket.accept();
            fail("tcp attempt was made");
        } catch (IOException expected) {
        }

        DatagramPacket push = new DatagramPacket(new byte[1000], 1000);
        udpsocket.receive(push);

        ByteArrayInputStream bais = new ByteArrayInputStream(push.getData());
        PushRequest pr = (PushRequest)messageFactory.read(bais, Network.TCP);
        assertEquals(rfd1.getIndex(), pr.getIndex());

        Thread.sleep(5200);

        Socket s = serversocket.accept();
        assertTrue(s.isConnected());
        s.close();
    }

    /**
     * tests the scenario where an udp push is sent, no connection is received
     * but since we're trying to contact an altloc no failover tcp push is sent.
     */
    public void testUDPPushFailoverAlt() throws Exception {
        requestPush(rfdAlt);

        try {
            serversocket.accept();
            fail("tcp attempt was made");
        } catch (IOException expected) {
        }

        DatagramPacket push = new DatagramPacket(new byte[1000], 1000);
        udpsocket.receive(push);

        ByteArrayInputStream bais = new ByteArrayInputStream(push.getData());
        PushRequest pr = (PushRequest) messageFactory.read(bais, Network.TCP);
        assertEquals(rfd1.getIndex(), pr.getIndex());

        Thread.sleep(5200);

        try {
            Socket s = serversocket.accept();
            s.close();
            fail("tcp attempt was made");
        } catch (IOException expected) {
        }
        Thread.sleep(3000);
    }

    /**
     * tests the scenario where an UDP push is sent and a connection is
     * established, so no failover occurs.
     */
    public void testUDPPush() throws Exception {
        requestPush(rfd1);
        try {
            serversocket.accept();
            fail("tcp attempt was made");
        } catch (IOException expected) {
        }

        DatagramPacket push = new DatagramPacket(new byte[1000], 1000);
        udpsocket.receive(push);

        ByteArrayInputStream bais = new ByteArrayInputStream(push.getData());
        PushRequest pr = (PushRequest) messageFactory.read(bais, Network.TCP);
        assertEquals(rfd1.getIndex(), pr.getIndex());

        socket = new NIOSocket(InetAddress.getLocalHost(), 10000);
        Socket other = serversocket.accept();

        assertEquals(InetAddress.getLocalHost(), socket.getInetAddress());
        assertEquals(10000, socket.getPort());

        sendGiv(other, "0:BC1F6870696111D4A74D0001031AE043/file1\n\n");

        connectionDispatcher.dispatch("GIV", socket, false);
        Thread.sleep(1000);
        other.close();

        Thread.sleep(5000);

        try {
            Socket s = serversocket.accept();
            s.close();
            fail("tcp attempt was made");
        } catch (IOException expected) {
        }
        Thread.sleep(3000);
    }

    /**
     * tests the scenario where two pushes are made to the same host for
     * different files and both succeed.
     */
    public void testTwoPushesBothGood() throws Exception {
        requestPush(rfd1);
        requestPush(rfd2);

        try {
            serversocket.accept();
            fail("tcp attempt was made");
        } catch (IOException expected) {
        }

        DatagramPacket push = new DatagramPacket(new byte[1000], 1000);
        udpsocket.receive(push);

        ByteArrayInputStream bais = new ByteArrayInputStream(push.getData());
        PushRequest pr = (PushRequest) messageFactory.read(bais, Network.TCP);
        assertEquals(rfd1.getIndex(), pr.getIndex());

        udpsocket.receive(push);
        bais = new ByteArrayInputStream(push.getData());
        pr = (PushRequest) messageFactory.read(bais, Network.TCP);
        assertEquals(rfd2.getIndex(), pr.getIndex());

        Thread.sleep(2000);

        socket = new NIOSocket(InetAddress.getLocalHost(), 10000);
        Socket other = serversocket.accept();

        sendGiv(other, "0:BC1F6870696111D4A74D0001031AE043/file1\n\n");
        connectionDispatcher.dispatch("GIV", socket, false);
        Thread.sleep(1000);
        socket.close();

        socket = new NIOSocket(InetAddress.getLocalHost(), 10000);
        other = serversocket.accept();
        socket.setSoTimeout(1000);
        sendGiv(other, "0:BC1F6870696111D4A74D0001031AE043/file2\n\n");
        connectionDispatcher.dispatch("GIV", socket, false);
        Thread.sleep(1000);
        socket.close();
        Thread.sleep(5200);

        try {
            serversocket.accept();
            fail("tcp attempt was made");
        } catch (IOException expected) {
        }
    }

    public void testTwoPushesOneFails() throws Exception {

        requestPush(rfd1);
        requestPush(rfd2);

        try {
            serversocket.accept();
            fail("tcp attempt was made");
        } catch (IOException expected) {
        }

        DatagramPacket push = new DatagramPacket(new byte[1000], 1000);
        udpsocket.receive(push);

        ByteArrayInputStream bais = new ByteArrayInputStream(push.getData());
        PushRequest pr = (PushRequest) messageFactory.read(bais, Network.TCP);
        assertEquals(rfd1.getIndex(), pr.getIndex());

        udpsocket.receive(push);
        bais = new ByteArrayInputStream(push.getData());
        pr = (PushRequest) messageFactory.read(bais, Network.TCP);
        assertEquals(rfd2.getIndex(), pr.getIndex());

        Thread.sleep(2000);

        socket = new NIOSocket(InetAddress.getLocalHost(), 10000);
        Socket other = serversocket.accept();

        sendGiv(other, "0:BC1F6870696111D4A74D0001031AE043/file1\n\n");
        connectionDispatcher.dispatch("GIV", socket, false);
        Thread.sleep(1000);
        socket.close();

        Thread.sleep(5200);

        serversocket.accept().close();
    }

    /**
     * tests the scenario where two pushes are made to the same host for
     * different files and both succeed.
     */
    public void testPushContainsTLS() throws Exception {
        tlsManager.setIncomingTLSEnabled(false);
        requestPush(rfd1);
        tlsManager.setIncomingTLSEnabled(true);
        requestPush(rfd2);

        try {
            serversocket.accept();
            fail("tcp attempt was made");
        } catch (IOException expected) {
        }

        DatagramPacket push = new DatagramPacket(new byte[1000], 1000);
        udpsocket.receive(push);

        ByteArrayInputStream bais = new ByteArrayInputStream(push.getData());
        PushRequest pr = (PushRequest) messageFactory.read(bais, Network.TCP);
        assertEquals(rfd1.getIndex(), pr.getIndex());
        assertFalse(pr.isTLSCapable());

        udpsocket.receive(push);
        bais = new ByteArrayInputStream(push.getData());
        pr = (PushRequest) messageFactory.read(bais, Network.TCP);
        assertEquals(rfd2.getIndex(), pr.getIndex());
        assertTrue(pr.isTLSCapable());

        // Finish off the test, just to make sure the failover doesn't kick in
        // later.
        Thread.sleep(2000);
        socket = new NIOSocket(InetAddress.getLocalHost(), 10000);
        Socket other = serversocket.accept();
        sendGiv(other, "0:BC1F6870696111D4A74D0001031AE043/file1\n\n");
        connectionDispatcher.dispatch("GIV", socket, false);
        Thread.sleep(1000);
        socket.close();
        socket = new NIOSocket(InetAddress.getLocalHost(), 10000);
        other = serversocket.accept();
        socket.setSoTimeout(1000);
        sendGiv(other, "0:BC1F6870696111D4A74D0001031AE043/file2\n\n");
        connectionDispatcher.dispatch("GIV", socket, false);
        Thread.sleep(1000);
        socket.close();
        Thread.sleep(5200);
        try {
            serversocket.accept();
            fail("tcp attempt was made");
        } catch (IOException expected) {
        }
    }

    private void requestPush(final RemoteFileDesc rfd) throws Exception {
//        Thread t = ThreadExecutor.newManagedThread(new Runnable() {
//            public void run() {
//                injector.getInstance(DownloadManager.class).getPushManager().sendPush(rfd);
//            }
//        });
//        t.start();
//        Thread.sleep(100);
//        
        ((DownloadManagerImpl)injector.getInstance(DownloadManager.class)).getPushManager().sendPush(rfd);
    }

    private void sendGiv(final Socket sock, final String str) {
        Thread t = ThreadExecutor.newManagedThread(new Runnable() {
            public void run() {
                try {
                    sock.getOutputStream().write(StringUtils.toAsciiBytes(str));
                } catch (IOException e) {
                    fail(e);
                }
            }
        });
        t.start();
    }

}
