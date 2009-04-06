package com.limegroup.gnutella.connection;

import java.net.InetAddress;
import java.util.List;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.FilterSettings;
import org.limewire.core.settings.NetworkSettings;
import org.limewire.core.settings.UltrapeerSettings;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.net.SocketsManager.ConnectType;

import junit.framework.Test;

import com.google.inject.Injector;
import com.google.inject.Stage;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.LifecycleManager;
import com.limegroup.gnutella.StubGnetConnectObserver;
import com.limegroup.gnutella.handshaking.HeadersFactory;
import com.limegroup.gnutella.messages.PingReplyFactory;
import com.limegroup.gnutella.util.EmptyResponder;

public class TLSConnectionTest extends LimeTestCase {
    
    private static int PORT = 9999;

    private Injector injector;
    private Injector injector2;

    private ConnectionManager connectionManager;

    private RoutedConnectionFactory routedConnectionFactory;

    private HeadersFactory headersFactory;
   
    public TLSConnectionTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(TLSConnectionTest.class);
    }    

    public static void main(String argv[]) {
        junit.textui.TestRunner.run(suite());
    }
    
    private static void setSettings() throws Exception {
        String localIP = InetAddress.getLocalHost().getHostAddress();
        FilterSettings.BLACK_LISTED_IP_ADDRESSES.set(new String[] {"*.*.*.*"});
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.set(new String[] {"127.*.*.*", "192.168.*.*", "10.254.*.*", localIP});
        NetworkSettings.PORT.setValue(PORT);
        ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
        UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(true);
        UltrapeerSettings.DISABLE_ULTRAPEER_MODE.setValue(false);
        UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(true);
        UltrapeerSettings.MAX_LEAVES.setValue(33);
        ConnectionSettings.NUM_CONNECTIONS.setValue(33);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        ConnectionSettings.WATCHDOG_ACTIVE.setValue(false);
        UltrapeerSettings.NEED_MIN_CONNECT_TIME.setValue(false);
    }
   
    @Override
    public void setUp() throws Exception {
        injector = LimeTestUtils.createInjector(Stage.PRODUCTION);
        
        PORT++; // TODO: Remove hack to override port
        
        setSettings();
        

        assertEquals("unexpected port", PORT, NetworkSettings.PORT.getValue());
        injector.getInstance(LifecycleManager.class).start();

        
        connectionManager = injector.getInstance(ConnectionManager.class);
        connectionManager.connect();

        injector2 = LimeTestUtils.createInjector(); 
        routedConnectionFactory = injector2.getInstance(RoutedConnectionFactory.class);        
        headersFactory = injector2.getInstance(HeadersFactory.class);
    }
    
    public void testTLSConnectionNonBlockingConnect() throws Exception {
        // Connect to this LW from another LW instance!
        GnutellaConnection c = (GnutellaConnection)routedConnectionFactory.createRoutedConnection("localhost", PORT, ConnectType.TLS);
        assertTrue(c.isTLSCapable());
        assertEquals(0, c.getConnectionBandwidthStatistics().getBytesReceived());
        assertEquals(0, c.getConnectionBandwidthStatistics().getBytesSent());
        assertEquals(0, c.getConnectionBandwidthStatistics().getUncompressedBytesReceived());
        assertEquals(0, c.getConnectionBandwidthStatistics().getUncompressedBytesSent());
        assertEquals(0.0f, c.getConnectionBandwidthStatistics().getSentLostFromSSL());
        assertEquals(0.0f, c.getConnectionBandwidthStatistics().getReadLostFromSSL());
        
        assertEquals(0, connectionManager.getNumConnections());
        StubGnetConnectObserver connector = new StubGnetConnectObserver();
        c.initialize(headersFactory.createLeafHeaders("localhost"), new EmptyResponder(), 1000, connector);
        connector.waitForResponse(1000);
        assertTrue(connector.toString(), connector.isConnect());
        assertGreaterThan(0, c.getConnectionBandwidthStatistics().getSentLostFromSSL());
        assertGreaterThan(0, c.getConnectionBandwidthStatistics().getReadLostFromSSL());

        // Send a few messages
        c.startMessaging();
        c.send(injector.getInstance(PingReplyFactory.class).create(new byte[16], (byte)1));
        
        // Sleep a little bit to let handshaking finish & messages exchange.
        Thread.sleep(1000);
        List<RoutedConnection> l = connectionManager.getConnections();
        assertEquals(1, l.size());
        RoutedConnection mc = l.get(0);
        assertTrue(mc.isTLSCapable());
        
        assertGreaterThan(0, mc.getConnectionBandwidthStatistics().getSentLostFromSSL());
        assertGreaterThan(0, mc.getConnectionBandwidthStatistics().getReadLostFromSSL());
        
        assertGreaterThan(0, mc.getConnectionMessageStatistics().getNumMessagesReceived());
        assertGreaterThan(0, mc.getConnectionMessageStatistics().getNumMessagesSent());
        assertGreaterThan(0, c.getConnectionMessageStatistics().getNumMessagesReceived());
        assertGreaterThan(0, c.getConnectionMessageStatistics().getNumMessagesSent());
    }    
    
}
