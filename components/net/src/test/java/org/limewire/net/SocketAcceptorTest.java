package org.limewire.net;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

import junit.framework.Test;

import org.limewire.concurrent.ManagedThread;
import org.limewire.io.SimpleNetworkInstanceUtils;
import org.limewire.util.BaseTestCase;
import org.limewire.util.StringUtils;

public class SocketAcceptorTest extends BaseTestCase {
    
    private int LISTEN_PORT = 9999;
    private SocketAcceptor acceptor;
    
    public SocketAcceptorTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(SocketAcceptorTest.class);
    }
    
    @Override
    public void setUp() throws Exception {
    }
    
    @Override
    public void tearDown() throws Exception {
        if (acceptor != null) {
            acceptor.bind(0);
        }
    }
    
    public void testSetPort() throws Exception {
        acceptor = new SocketAcceptor(new ConnectionDispatcherImpl(new SimpleNetworkInstanceUtils()));
        acceptor.bind(LISTEN_PORT);
        write(LISTEN_PORT, "Hello");
        acceptor.bind(LISTEN_PORT + 1);
        try {
            write(LISTEN_PORT, "Hello");
            fail("Expected connect exception");
        } catch (ConnectException expected) {            
        } catch(SocketTimeoutException expected) {
            // STE is thrown on Windows instead of CE?!?
        }
    }

    private void write(final int port, final String text) throws Exception {
        final Exception[] error = new Exception[1];
        final Throwable throwable = new Throwable();
        Thread t = new ManagedThread() {
            @Override
            public void run() {
                Socket s;
                try {
                    s = new Socket();
                    s.connect(new InetSocketAddress("localhost", port), 200); 
                    s.getOutputStream().write(StringUtils.toAsciiBytes(text));
                } catch (Exception e) {
                    error[0] = e;
                }
            }
        };
        t.start();
        t.join();
        
        if (error[0] != null) {
            throw (Exception)error[0].initCause(throwable);
        }
    }
    
}
