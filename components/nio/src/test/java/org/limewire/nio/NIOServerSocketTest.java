package org.limewire.nio;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.Selector;

import junit.framework.Test;

import org.limewire.nio.observer.StubAcceptObserver;
import org.limewire.util.BaseTestCase;
import org.limewire.util.PrivilegedAccessor;

public class NIOServerSocketTest extends BaseTestCase {
    
    private final int LISTEN_PORT = 9999;
    private final InetSocketAddress LISTEN_ADDR = new InetSocketAddress("127.0.0.1", LISTEN_PORT);

    public NIOServerSocketTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(NIOServerSocketTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testBlockingAccept() throws Exception {
        NIOServerSocket server = new NIOServerSocket(LISTEN_PORT);
        Socket c1 = connect();
        Socket r1 = server.accept();
        assertEquals(c1.getLocalPort(), r1.getPort());
        assertTrue(r1.isConnected());
        assertInstanceof(NIOSocket.class, r1);
        assertEquals(0, interestOps(r1));
        server.close();
        r1.close();
        
        Thread.sleep(100);
    }
    
    public void testNonBlockingAccept() throws Exception {
        StubAcceptObserver observer = new StubAcceptObserver();
        NIOServerSocket server = new NIOServerSocket(LISTEN_PORT, observer);
        Socket c1 = connect();
        Thread.sleep(200);
        assertEquals(1, observer.getSockets().size());
        Socket r1 = observer.getNextSocket();
        assertEquals(c1.getLocalPort(), r1.getPort());
        server.close();
        r1.close();
        
        Thread.sleep(100);
    }
    
    public void testMultipleNonBlockingAccepts() throws Exception {
        StubAcceptObserver observer = new StubAcceptObserver();
        NIOServerSocket server = new NIOServerSocket(LISTEN_PORT, observer);
        Socket c1 = connect();
        Socket c2 = connect();
        Socket c3 = connect();
        Thread.sleep(300);
        assertEquals(3, observer.getSockets().size());
        Socket r1 = observer.getNextSocket();
        Socket r2 = observer.getNextSocket();
        Socket r3 = observer.getNextSocket();
        assertEquals(c1.getLocalPort(), r1.getPort());
        assertEquals(c2.getLocalPort(), r2.getPort());
        assertEquals(c3.getLocalPort(), r3.getPort());
        server.close();
        r1.close();
        r2.close();
        r3.close();
    }
    
    private Socket connect() throws Exception {
        Socket socket = new Socket();
        socket.connect(LISTEN_ADDR, 5000);
        return socket;
    }
    
    private int interestOps(Socket socket) throws Exception {
        // peeks into the NIODispatcher to get the Selector so we can assert the interetOps
        Selector selector = (Selector)PrivilegedAccessor.getValue(NIODispatcher.instance(), "primarySelector");
        return socket.getChannel().keyFor(selector).interestOps();
    }
    
}
