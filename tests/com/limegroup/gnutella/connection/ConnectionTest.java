package com.limegroup.gnutella.connection;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

import junit.framework.Test;

import org.limewire.concurrent.ThreadExecutor;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.io.IOUtils;
import org.limewire.net.SocketsManager.ConnectType;
import org.limewire.nio.NIOServerSocket;
import org.limewire.nio.observer.AcceptObserver;

import com.google.inject.Injector;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.StubGnetConnectObserver;
import com.limegroup.gnutella.connection.BlockingConnection;
import com.limegroup.gnutella.connection.BlockingConnectionFactory;
import com.limegroup.gnutella.connection.RoutedConnection;
import com.limegroup.gnutella.connection.RoutedConnectionFactory;
import com.limegroup.gnutella.handshaking.HandshakeResponderFactory;
import com.limegroup.gnutella.util.LimeTestCase;

public class ConnectionTest extends LimeTestCase {
    
    private static int LISTEN_PORT = 9999;
    private ConnectionAcceptor ACCEPTOR;
    
    private RoutedConnectionFactory routedConnectionFactory;    
    
    public ConnectionTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(ConnectionTest.class);
    }

    public static void main(String argv[]) {
        junit.textui.TestRunner.run(suite());
    }

    @Override
    public void setUp() throws Exception {
        routedConnectionFactory = LimeTestUtils.createInjector().getInstance(RoutedConnectionFactory.class);
        
        ACCEPTOR = new ConnectionAcceptor();
        ACCEPTOR.start();
        ConnectionSettings.ALLOW_WHILE_DISCONNECTED.setValue(true);
        ConnectionSettings.PREFERENCING_ACTIVE.setValue(false);
    }
    
    @Override
    public void tearDown() throws Exception {
        ACCEPTOR.shutdown();
        
        Thread.sleep(1000);
    }
    
    public void testNonBlockingConnectFailing() throws Exception {
        RoutedConnection rc = routedConnectionFactory.createRoutedConnection("127.0.0.1", LISTEN_PORT+1);
        StubGnetConnectObserver observer = new StubGnetConnectObserver();
        rc.initialize(observer);
        observer.waitForResponse(3000);
        assertTrue(observer.isShutdown());
        assertFalse(observer.isBadHandshake());
        assertFalse(observer.isConnect());
        assertFalse(observer.isNoGOK());
    }
    
    public void testNonBlockingConnectSucceeds() throws Exception {
        RoutedConnection rc = routedConnectionFactory.createRoutedConnection("127.0.0.1", LISTEN_PORT);
        StubGnetConnectObserver observer = new StubGnetConnectObserver();
        rc.initialize(observer);
        observer.waitForResponse(3000);
        assertFalse(observer.isShutdown());
        assertFalse(observer.isBadHandshake());
        assertFalse(observer.isNoGOK());
        assertTrue(observer.isConnect());
    }
    
    public void testNonBlockingNoGOK() throws Exception {
        ACCEPTOR.getObserver().setNoGOK(true);
        RoutedConnection rc = routedConnectionFactory.createRoutedConnection("127.0.0.1", LISTEN_PORT, ConnectType.PLAIN);
        StubGnetConnectObserver observer = new StubGnetConnectObserver();
        rc.initialize(observer);
        observer.waitForResponse(10000);
        assertFalse(observer.isShutdown());
        assertFalse(observer.isBadHandshake());
        assertFalse(observer.isConnect());
        assertTrue(observer.isNoGOK());
        assertEquals(401, observer.getCode());
    }
    
    public void testNonBlockingBadHandshake() throws Exception {
        ACCEPTOR.getObserver().setBadHandshake(true);
        RoutedConnection rc = routedConnectionFactory.createRoutedConnection("127.0.0.1", LISTEN_PORT, ConnectType.PLAIN);
        StubGnetConnectObserver observer = new StubGnetConnectObserver();
        rc.initialize(observer);
        observer.waitForResponse(10000);
        assertFalse(observer.isShutdown());
        assertTrue(observer.isBadHandshake());
        assertFalse(observer.isConnect());
        assertFalse(observer.isNoGOK());
    }
        
    private static class ConnectionAcceptor {
        private ServerSocket socket;
        private SimpleAcceptObserver observer;
        
        
        public void start() throws Exception {
            observer = new SimpleAcceptObserver();
            socket = new NIOServerSocket(LISTEN_PORT, observer);
        }
        
        public void shutdown() throws Exception {
            socket.close();
        }
        
        public SimpleAcceptObserver getObserver() {
            return observer;
        }
    }
    
    private static class SimpleAcceptObserver implements AcceptObserver {
        private volatile boolean noGOK = false;
        private volatile boolean badHandshake = false;
        
        public void handleIOException(IOException iox) {}

        public void handleAccept(final Socket socket) throws IOException {
            ThreadExecutor.startThread(new Runnable() {
                public void run() {
                    try {
                        if(badHandshake) {
                            socket.close();
                            return;
                        }
                        socket.setSoTimeout(3000);
                        InputStream in = socket.getInputStream();
                        String word = IOUtils.readWord(in, 9);
                        if (!word.equals("GNUTELLA"))
                            throw new IOException("Bad word: " + word);

                        if(noGOK) {
                            socket.getOutputStream().write("GNUTELLA/0.6 401 Failed\r\n\r\n".getBytes());
                            socket.getOutputStream().flush();
                            return;
                        }
                        
                        Injector injector = LimeTestUtils.createInjector();
                        BlockingConnectionFactory connectionFactory = injector.getInstance(BlockingConnectionFactory.class);
                        HandshakeResponderFactory handshakeResponderFactory = injector.getInstance(HandshakeResponderFactory.class);
                        final BlockingConnection con = connectionFactory.createConnection(socket);
                        con.initialize(null, handshakeResponderFactory.createUltrapeerHandshakeResponder("127.0.0.1"), 1000);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }, "conninit");
        }

        public void shutdown() {
        }

        public void setBadHandshake(boolean badHandshake) {
            this.badHandshake = badHandshake;
        }

        public void setNoGOK(boolean noGOK) {
            this.noGOK = noGOK;
        }
    }
}
