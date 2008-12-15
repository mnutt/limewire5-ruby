package com.limegroup.gnutella.downloader;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.GUID;
import org.limewire.io.IOUtils;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.io.SimpleNetworkInstanceUtils;
import org.limewire.net.BlockingConnectObserver;
import org.limewire.net.address.FirewalledAddress;
import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpException;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.mortbay.http.HttpServer;
import org.mortbay.http.SocketListener;
import org.mortbay.http.handler.AbstractHttpHandler;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.Stage;
import com.limegroup.gnutella.Acceptor;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.LifecycleManager;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.stubs.AcceptorStub;
import com.limegroup.gnutella.util.LimeTestCase;


public class PushDownloadManagerTest extends LimeTestCase {
    private PushDownloadManager pushDownloadManager;
    private PushedSocketHandlerStub browser;
    private PushedSocketHandlerStub downloader;
    private Injector injector;

    public PushDownloadManagerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(PushDownloadManagerTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        injector = LimeTestUtils.createInjector(Stage.PRODUCTION, new AbstractModule() {
            @Override
            protected void configure() {
                bind(DownloaderStub.class).asEagerSingleton();
                bind(BrowserStub.class).asEagerSingleton();
                bind(NetworkInstanceUtils.class).toInstance(new SimpleNetworkInstanceUtils(false));
                bind(Acceptor.class).to(AcceptorStub.class);
            }
        });
        pushDownloadManager = injector.getInstance(PushDownloadManager.class);
        browser = injector.getInstance(DownloaderStub.class);
        downloader = injector.getInstance(BrowserStub.class);
    }
    
    /**
     * Integration test to ensure that pushed connecting works
     */
    public void testConnect() throws Exception {
        LifecycleManager lifecycleManager = injector.getInstance(LifecycleManager.class);
        NetworkManager networkManager = injector.getInstance(NetworkManager.class);
        ConnectionServices connectionServices = injector.getInstance(ConnectionServices.class);
        Acceptor acceptor = injector.getInstance(Acceptor.class);
        // need to have valid address and accepted incoming to support non-fwt push connects
        acceptor.setAddress(InetAddress.getByAddress(new byte[] { (byte) 192, (byte) 168, 0, 1 }));
        ((AcceptorStub)acceptor).setAcceptedIncoming(true);
        lifecycleManager.start();
        connectionServices.connect();
        HTTPPushProxyServer server = null;
        try {
            server = new HTTPPushProxyServer(9999);
            server.start();
            BlockingConnectObserver observer = new BlockingConnectObserver();
            Set<Connectable> proxies = new TreeSet<Connectable>();
            proxies.add(new ConnectableImpl("localhost", 9999, false));
            networkManager.newPushProxies(proxies);
            GUID guid = new GUID();
            Connectable hostAddress = new ConnectableImpl(new ConnectableImpl("localhost", 1111, false));
            FirewalledAddress address = new FirewalledAddress(ConnectableImpl.INVALID_CONNECTABLE, hostAddress, guid, proxies, 0);
            assertTrue(pushDownloadManager.canConnect(address));
            pushDownloadManager.connect(address, observer);
            // long wait, since we wait for the tcp push as failover after the udp push
            assertTrue(server.latch.await(10, TimeUnit.SECONDS));
            GiveWritingSocket socket = new GiveWritingSocket(guid, networkManager.getPort());
            Socket receivedSocket = observer.getSocket(5, TimeUnit.SECONDS);
            assertEquals(socket.socket.getLocalSocketAddress(), receivedSocket.getRemoteSocketAddress());
            IOUtils.close(socket);
            IOUtils.close(receivedSocket);
        } finally {
            IOUtils.close(server);
            connectionServices.disconnect();
            lifecycleManager.shutdown();
        }
    }

    public void testHandleGIVDownload() {
        byte [] clientGUIDBytes = GUID.makeGuid();
        downloader.setClientGUID(clientGUIDBytes);
        Socket socket = new Socket();
        assertFalse(socket.isClosed());
        PushDownloadManager.GIVLine givLine = new PushDownloadManager.GIVLine("foo", 1, clientGUIDBytes);
        pushDownloadManager.handleGIV(socket, givLine);
        assertTrue(downloader.accepted());
        assertFalse(browser.accepted());
        assertFalse(socket.isClosed());
    }

    public void testHandleGIVBrowse() {
        byte [] clientGUIDBytes = GUID.makeGuid();
        browser.setClientGUID(clientGUIDBytes);
        Socket socket = new Socket();
        assertFalse(socket.isClosed());
        PushDownloadManager.GIVLine givLine = new PushDownloadManager.GIVLine("foo", 1, clientGUIDBytes);
        pushDownloadManager.handleGIV(socket, givLine);
        assertFalse(downloader.accepted());
        assertTrue(browser.accepted());
        assertFalse(socket.isClosed());
    }

    public void testHandleGIVReject() {
        byte [] clientGUIDBytes = GUID.makeGuid();
        Socket socket = new Socket();
        assertFalse(socket.isClosed());
        PushDownloadManager.GIVLine givLine = new PushDownloadManager.GIVLine("foo", 1, clientGUIDBytes);
        pushDownloadManager.handleGIV(socket, givLine);
        assertFalse(downloader.accepted());
        assertFalse(browser.accepted());
        assertTrue(socket.isClosed());
    }

    public static class PushedSocketHandlerStub implements PushedSocketHandler {
        byte [] clientGUID;
        boolean acceptedIncomingConnection = false;
        
        @Inject
        public PushedSocketHandlerStub() {}

        @Inject
        public void register(PushedSocketHandlerRegistry registry) {
            registry.register(this);
        }

        boolean accepted() {
            return acceptedIncomingConnection;
        }

        void setClientGUID(byte [] clientGUID) {
            this.clientGUID = clientGUID;
        }

        public boolean acceptPushedSocket(String file, int index, byte[] clientGUID, Socket socket) {
            if(Arrays.equals(this.clientGUID, clientGUID)) {
                acceptedIncomingConnection = true;
                return true;
            } else {
                return false;
            }
        }
    }
    
    @Singleton
    public static class DownloaderStub extends PushedSocketHandlerStub{}
    @Singleton
    public static class BrowserStub extends PushedSocketHandlerStub{}
    
    private static class HTTPPushProxyServer implements Closeable {
        
        private HttpServer server = new HttpServer();
        
        CountDownLatch latch = new CountDownLatch(1);
        
        public HTTPPushProxyServer(int port) {
            SocketListener listener = new SocketListener();
            listener.setPort(port);
            listener.setMinThreads(1);
            listener.setMaxThreads(5);
            server.addListener(listener);

            HttpContext context = server.addContext("/");
            context.addHandler(new AbstractHttpHandler() {
                @Override
                public void handle(String target, String hm, HttpRequest request, HttpResponse response)
                        throws HttpException, IOException {
                    response.setStatus(HttpResponse.__202_Accepted);
                    response.commit();
                    latch.countDown();
                }
            });
        }
        
        void start() throws Exception {
            server.start();
        }
        
        public void close() throws IOException {
            try {
                server.stop();
                server.join();
            } catch (InterruptedException e) {
                throw new IOException(e);
            }
        }
    }
    
    private static class GiveWritingSocket implements Closeable {
        
        private Socket socket;

        public GiveWritingSocket(GUID guid, int port) throws IOException {
            socket = new Socket("localhost", port);
            socket.setSoTimeout(30 * 1000);
            OutputStream out = socket.getOutputStream();
            out.write(("GIV 0:" + guid + "/file\n\n").getBytes());
            out.flush();
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }
    }

}