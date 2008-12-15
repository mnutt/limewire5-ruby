package org.limewire.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import junit.framework.Test;

import org.limewire.io.SimpleNetworkInstanceUtils;
import org.limewire.net.ProxySettings.ProxyType;
import org.limewire.nio.NBSocketFactory;
import org.limewire.nio.NIOSocketFactory;
import org.limewire.util.BaseTestCase;

public class LimitedSocketControllerTest extends BaseTestCase {
    
    private final int TIMEOUT = 30000;
    private final NBSocketFactory FACTORY = new NIOSocketFactory();
    
    private final int BAD_PORT = 9998;
    private final InetSocketAddress BAD_ADDR = new InetSocketAddress("127.0.0.1", BAD_PORT);
    private final int LISTEN_PORT = 9999;
    private final InetSocketAddress LISTEN_ADDR = new InetSocketAddress("127.0.0.1", LISTEN_PORT);
    private final int BAD_GOOGLE_PORT = 6666;
    private final InetSocketAddress BAD_GOOGLE_ADDR = new InetSocketAddress("www.google.com", BAD_GOOGLE_PORT);
    
    private ServerSocket listenSocket;
    private LimitedSocketController controller;
    private ProxyManager proxyManager;
    private ProxySettingsStub proxySettings;
    
    public LimitedSocketControllerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(LimitedSocketControllerTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    public void setUp() throws Exception {
        listenSocket = new ServerSocket(LISTEN_PORT);
        listenSocket.setReuseAddress(true);
        proxySettings = new ProxySettingsStub();
        proxyManager = new ProxyManagerImpl(proxySettings, new SimpleNetworkInstanceUtils());
        controller = new LimitedSocketController(proxyManager, new EmptySocketBindingSettings(), 2);
    }
    
    @Override
    public void tearDown() throws Exception {
        listenSocket.close();
    }
    
    public void testSimpleConnect() throws Exception {
        connect(false, true);
    }
    
    public void testSimpleConnectNB() throws Exception {
        connect(true, true);
    }
    
    public void testFailureConnect() throws Exception {
        connect(false, false);
    }
    
    public void testFailureConnectNB() throws Exception {
        connect(true, false);
    }
    
    public void testMaxConcurrent() throws Exception {
        assertEquals(2, controller.getNumAllowedSockets());
    }
    
    public void testWaitsForTurn() throws Exception {
        ConnectObserverStub o1 = new ConnectObserverStub();
        ConnectObserverStub o2 = new ConnectObserverStub();
        controller.connect(FACTORY, BAD_GOOGLE_ADDR, null, TIMEOUT, o1);
        controller.connect(FACTORY, BAD_GOOGLE_ADDR, null, TIMEOUT, o2);
        // the above two will stall for awhile while trying to connect...
        long then = System.currentTimeMillis();
        connect(false, true);
        long now = System.currentTimeMillis();
        assertGreaterThan(10000, now - then); // make sure it took awhile
    }
    
    public void testWaitsForTurnNB() throws Exception {
        ConnectObserverStub o1 = new ConnectObserverStub();
        ConnectObserverStub o2 = new ConnectObserverStub();
        controller.connect(FACTORY, BAD_GOOGLE_ADDR, null, TIMEOUT, o1);
        controller.connect(FACTORY, BAD_GOOGLE_ADDR, null, TIMEOUT, o2);
        // the above two will stall for awhile while trying to connect...
        long then = System.currentTimeMillis();
        connect(true, true);
        long now = System.currentTimeMillis();
        assertGreaterThan(10000, now - then); // make sure it took awhile
    }
    
    public void testRemoveObserver() throws Exception {
        ConnectObserverStub o1 = new ConnectObserverStub();
        controller.connect(FACTORY, LISTEN_ADDR, null, 0, o1);
        assertFalse(controller.removeConnectObserver(o1));
        
        ConnectObserverStub o2 = new ConnectObserverStub();
        ConnectObserverStub o3 = new ConnectObserverStub();
        controller.connect(FACTORY, BAD_GOOGLE_ADDR, null, TIMEOUT, o2);
        controller.connect(FACTORY, BAD_GOOGLE_ADDR, null, TIMEOUT, o3);

        ConnectObserverStub o4 = new ConnectObserverStub();
        controller.connect(FACTORY, LISTEN_ADDR, null, 0, o4);
        assertTrue(controller.removeConnectObserver(o4));
        o3.waitForResponse(60000); // wait until o2 & o3 finish, make sure 4 didn't try
        Thread.sleep(1000);
        assertFalse(o4.isShutdown());
        assertNull(o4.getSocket());
        assertNull(o4.getIoException());
    }
    
    public void testRemoveObserverWithProxies() throws Exception {
        proxySettings.setProxyHost("www.google.com");
        proxySettings.setProxyPort(LISTEN_PORT);
        proxySettings.setProxyType(ProxyType.SOCKS5);
        
        ConnectObserverStub o1 = new ConnectObserverStub();
        controller.connect(FACTORY, LISTEN_ADDR, null, 0, o1);
        assertFalse(controller.removeConnectObserver(o1));
        
        ConnectObserverStub o2 = new ConnectObserverStub();
        ConnectObserverStub o3 = new ConnectObserverStub();
        controller.connect(FACTORY, BAD_GOOGLE_ADDR, null, TIMEOUT, o2);
        controller.connect(FACTORY, BAD_GOOGLE_ADDR, null, TIMEOUT, o3);

        ConnectObserverStub o4 = new ConnectObserverStub();
        controller.connect(FACTORY, LISTEN_ADDR, null, 0, o4);
        assertTrue(controller.removeConnectObserver(o4));
        o3.waitForResponse(60000); // wait until o2 & o3 finish, make sure 4 didn't try
        Thread.sleep(1000);
        assertFalse(o4.isShutdown());
        assertNull(o4.getSocket());
        assertNull(o4.getIoException());       
    }
    
    private void connect(boolean nb, boolean success) throws Exception {
        if (success) {
            Socket s;
            if (!nb) {
                s = controller.connect(FACTORY, LISTEN_ADDR, null, 0, null);
            } else {
                ConnectObserverStub o = new ConnectObserverStub();
                s = controller.connect(FACTORY, LISTEN_ADDR, null, 0, o);
                o.waitForResponse(60000);
                assertEquals(s, o.getSocket());
                assertNull(o.getIoException());
                assertFalse(o.isShutdown());
            }
            // Must connect somewhere, NPE is an error
            s.close();
        } else {
            if (!nb) {
                try {
                    controller.connect(FACTORY, BAD_ADDR, null, 0, null);
                    fail("acceptedConnection from a bad proxy server");
                } catch (IOException iox) {
                    // Good -- expected behaviour
                }
            } else {
                ConnectObserverStub o = new ConnectObserverStub();
                controller.connect(FACTORY, BAD_ADDR, null, 0, o);
                o.waitForResponse(60000);
                assertNull(o.getSocket());
                assertNull(o.getIoException());
                assertTrue(o.isShutdown());
            }
        }
    }    

}
