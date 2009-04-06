package com.limegroup.gnutella.dht;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import junit.framework.Test;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.DHTSettings;
import org.limewire.core.settings.FilterSettings;
import org.limewire.core.settings.PingPongSettings;
import org.limewire.core.settings.UltrapeerSettings;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.io.GUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.settings.ContextSettings;
import org.limewire.mojito.settings.NetworkSettings;
import org.limewire.mojito.util.MojitoUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.limegroup.gnutella.BlockingConnectionUtils;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.LifecycleManager;
import com.limegroup.gnutella.NodeAssigner;
import com.limegroup.gnutella.connection.BlockingConnection;
import com.limegroup.gnutella.connection.BlockingConnectionFactory;
import com.limegroup.gnutella.connection.RoutedConnection;
import com.limegroup.gnutella.dht.DHTManager.DHTMode;
import com.limegroup.gnutella.handshaking.HeadersFactory;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVM;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactory;
import com.limegroup.gnutella.messages.vendor.DHTContactsMessage;
import com.limegroup.gnutella.messages.vendor.PushProxyRequest;
import com.limegroup.gnutella.stubs.CapabilitiesVMFactoryImplStub;
import com.limegroup.gnutella.util.EmptyResponder;

/*
 * DHT <----> Ultrapeer <----> Leaf
 * 
 * Everytime an Ultrapeer learns of a new Contact or updates an
 * existing Contact it will forward it to its leafs
 */
public class PassiveLeafForwardContactsTest extends LimeTestCase {
    
    
    private static final int PORT = 6667;
    
    private List<MojitoDHT> dhts;
    
    private Injector injector;

    private ConnectionManager connectionManager;

    private ConnectionServices connectionServices;

    private DHTManager dhtManager;
    
    public PassiveLeafForwardContactsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(PassiveLeafForwardContactsTest.class);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    protected void setUp() throws Exception {
        doSettings();
        
        final NodeAssigner na = new NodeAssignerStub();
        injector = LimeTestUtils.createInjector(Stage.PRODUCTION, new AbstractModule() {
            @Override
            protected void configure() {
                bind(CapabilitiesVMFactory.class).to(CapabilitiesVMFactoryImplStub.class);
                bind(NodeAssigner.class).toInstance(na);
            }            
        });

        // start an instance of LimeWire in Ultrapeer mode
        injector.getInstance(LifecycleManager.class).start();
            
        connectionServices = injector.getInstance(ConnectionServices.class);
        connectionServices.connect();
           
        // make sure LimeWire is running as an Ultrapeer
        assertTrue(connectionServices.isSupernode());
        assertFalse(connectionServices.isActiveSuperNode());
        assertFalse(connectionServices.isShieldedLeaf());
        
        connectionManager = injector.getInstance(ConnectionManager.class);
        
        dhtManager = injector.getInstance(DHTManager.class);
        // Start and bootstrap a bunch of DHT Nodes
        dhts = Collections.emptyList();
        dhts = MojitoUtils.createBootStrappedDHTs(2);
    }

    private void doSettings() {
        ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
        UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(true);
        UltrapeerSettings.DISABLE_ULTRAPEER_MODE.setValue(false);
        UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(true);
        UltrapeerSettings.MAX_LEAVES.setValue(33);
        ConnectionSettings.NUM_CONNECTIONS.setValue(33);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        ConnectionSettings.WATCHDOG_ACTIVE.setValue(false);
        UltrapeerSettings.NEED_MIN_CONNECT_TIME.setValue(false);
        
        ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        ConnectionSettings.WATCHDOG_ACTIVE.setValue(false);
        PingPongSettings.PINGS_ACTIVE.setValue(false);
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(false);
        
        FilterSettings.BLACK_LISTED_IP_ADDRESSES.set(
                new String[] {"*.*.*.*"});
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.set(
                new String[] {"127.*.*.*"});
        
        org.limewire.core.settings.NetworkSettings.PORT.setValue(PORT);
        ConnectionSettings.FORCED_PORT.setValue(PORT);
        
        assertEquals("unexpected port", PORT, 
                 org.limewire.core.settings.NetworkSettings.PORT.getValue());
        
        DHTSettings.DISABLE_DHT_USER.setValue(false);
        DHTSettings.DISABLE_DHT_NETWORK.setValue(false);
        DHTSettings.ENABLE_PASSIVE_DHT_MODE.setValue(true);
        DHTSettings.ENABLE_PASSIVE_LEAF_DHT_MODE.setValue(true);
        DHTSettings.PERSIST_ACTIVE_DHT_ROUTETABLE.setValue(false);
        DHTSettings.PERSIST_DHT_DATABASE.setValue(false);
        
        ContextSettings.SHUTDOWN_MESSAGES_MULTIPLIER.setValue(0);
        
        NetworkSettings.FILTER_CLASS_C.setValue(false);
        NetworkSettings.LOCAL_IS_PRIVATE.setValue(false);
        
        // We're working on the loopback. Everything should be done
        // in less than 500ms
        NetworkSettings.DEFAULT_TIMEOUT.setValue(500);
        
        // Nothing should take longer than 1.5 seconds. If we start seeing
        // LockTimeoutExceptions on the loopback then check this Setting!
        ContextSettings.WAIT_ON_LOCK.setValue(1500);
    }

    @Override
    protected void tearDown() throws Exception {
        for (MojitoDHT dht : dhts) {
            dht.close();
        }
        dhts = null;
        
        injector.getInstance(LifecycleManager.class).shutdown();
    }
    
    public void testForwardContacts() throws Exception {
        // There should be no connections
        assertEquals(0, connectionManager.getNumConnections());

        // Connect a leaf Node to the Ultrapeer
        BlockingConnection out = createLeafConnection();
        try {
            assertTrue(out.isOpen());
            assertTrue(out.isOutgoing());
            BlockingConnectionUtils.drain(out);
            
            // There should be one connection now
            assertEquals(1, connectionManager.getNumConnections());
            assertTrue(connectionServices.isActiveSuperNode());
            
            // Check a few more things
            RoutedConnection in = connectionManager.getConnections().get(0);
            assertFalse(in.isOutgoing());
            assertTrue(in.isSupernodeClientConnection());
            assertEquals(-1, in.getConnectionCapabilities().remoteHostIsPassiveLeafNode());
            assertFalse(in.isPushProxyFor());
            
            // Pretend the leaf is running in PASSIVE_LEAF mode.
            addPassiveLeafCapability();
           
            // Tell our Ultrapeer that we've PASSIVE_LEAF mode enabled
            CapabilitiesVM vm = injector.getInstance(CapabilitiesVMFactory.class).getCapabilitiesVM();
            assertEquals(0, vm.isPassiveLeafNode());
            out.send(vm);
            out.flush();
            
            // A second requirement is that the given Ultrapeer is our
            // Push Proxy
            out.send(new PushProxyRequest(new GUID(GUID.makeGuid())));
            out.flush();
            
            Thread.sleep(250);
            
            // And did it work?
            assertNotEquals(-1, in.getConnectionCapabilities().remoteHostIsPassiveLeafNode());
            assertTrue(in.isPushProxyFor());
            
            // we expect to get the first dht contacts msg about a minute from now
            long firstMsg = System.currentTimeMillis() + 58000;
            dhtManager.start(DHTMode.PASSIVE);
            Thread.sleep(250);
            assertEquals(DHTMode.PASSIVE, dhtManager.getDHTMode());
            
            // Bootstrap the Ultrapeer
            dhtManager.getMojitoDHT().bootstrap(dhts.get(0).getContactAddress()).get();
            
            // -------------------------------------
            
            // the leaf should not receive a dht contacts msg for about a minute
            try {
                long now = System.currentTimeMillis();
                while( now < firstMsg) {
                    assertNotInstanceof(DHTContactsMessage.class, out.receive((int)(firstMsg - now)));
                    now = System.currentTimeMillis();
                }
            } catch (IOException expected){}
            
            // And check what the leaf is receiving...
            Set<Contact> nodes = new HashSet<Contact>();
                while(true) {
                    Message msg = out.receive(10000);
                    if (msg instanceof DHTContactsMessage) {
                        DHTContactsMessage message = (DHTContactsMessage)msg;
                        assertEquals(10,message.getContacts().size());
                        nodes.addAll(message.getContacts());
                        break;
                    }
                }
            
            // the ultrapeer id should not be sent as ups are passive.
            for (Contact c : nodes) 
                assertFalse(dhtManager.getMojitoDHT().getLocalNodeID().equals(c.getNodeID()));
            
        } finally {
            out.close();
        }
    }
    
    public void testDoNotSendCapabilitiesVM() throws Exception {
        // There should be no connections
        assertEquals(0, connectionManager.getNumConnections());

        // Connect a leaf Node to the Ultrapeer
        BlockingConnection out = createLeafConnection();
        try {
            assertTrue(out.isOpen());
            assertTrue(out.isOutgoing());
            BlockingConnectionUtils.drain(out);
            
            // There should be one connection now
            assertEquals(1, connectionManager.getNumConnections());
            assertTrue(connectionServices.isActiveSuperNode());
            
            // Check a few more things
            RoutedConnection in = connectionManager.getConnections().get(0);
            assertFalse(in.isOutgoing());
            assertTrue(in.isSupernodeClientConnection());
            assertEquals(-1, in.getConnectionCapabilities().remoteHostIsPassiveLeafNode());
            assertFalse(in.isPushProxyFor());
            
            // --- WE DO NOT PRETEND TO BE IN PASSIVE_LEAF MODE !!! ---
            
            // A second requirement is that the given Ultrapeer is our
            // Push Proxy
            out.send(new PushProxyRequest(new GUID(GUID.makeGuid())));
            out.flush();
            
            Thread.sleep(2000);
            
            // And did it work?
            assertEquals(-1, in.getConnectionCapabilities().remoteHostIsPassiveLeafNode());
            assertTrue(in.isPushProxyFor());
            
            dhtManager.start(DHTMode.PASSIVE);
            Thread.sleep(350);
            assertEquals(DHTMode.PASSIVE, dhtManager.getDHTMode());
            
            // Bootstrap the Ultrapeer
            dhtManager.getMojitoDHT().bootstrap(dhts.get(0).getContactAddress()).get();
            
            // -------------------------------------
            
            // And check what the leaf is receiving
            try {
                while(true) {
                    Message msg = out.receive(2000);
                    if (msg instanceof DHTContactsMessage) {
                        fail("Should not have received a DHTContactsMessage: " + msg);
                    }
                }
            } catch (InterruptedIOException expected) {
            }
        } finally {
            out.close();
        }
    }
    
    public void testDoNotSendPushProxyRequest() throws Exception {
        // There should be no connections
        assertEquals(0, connectionManager.getNumConnections());

        // Connect a leaf Node to the Ultrapeer
        BlockingConnection out = createLeafConnection();
        try {
            assertTrue(out.isOpen());
            assertTrue(out.isOutgoing());
            BlockingConnectionUtils.drain(out);
            
            // There should be one connection now
            assertEquals(1, connectionManager.getNumConnections());
            assertTrue(connectionServices.isActiveSuperNode());
            
            // Check a few more things
            RoutedConnection in = connectionManager.getConnections().get(0);
            assertFalse(in.isOutgoing());
            assertTrue(in.isSupernodeClientConnection());
            assertEquals(-1, in.getConnectionCapabilities().remoteHostIsPassiveLeafNode());
            assertFalse(in.isPushProxyFor());
            
            // pretend the leaf is running in PASSIVE_LEAF mode.
            addPassiveLeafCapability();
            
            // Tell our Ultrapeer that we've PASSIVE_LEAF mode enabled
            CapabilitiesVM vm = injector.getInstance(CapabilitiesVMFactory.class).getCapabilitiesVM();
            assertEquals(0, vm.isPassiveLeafNode());
            out.send(vm);
            out.flush();
            
            // --- But don't tell the Ultrapeer its our Push Proxy! ---
            
            Thread.sleep(2000);
            
            // And did it work?
            assertNotEquals(-1, in.getConnectionCapabilities().remoteHostIsPassiveLeafNode());
            assertFalse(in.isPushProxyFor());
            
            dhtManager.start(DHTMode.PASSIVE);
            Thread.sleep(250);
            assertEquals(DHTMode.PASSIVE, dhtManager.getDHTMode());
            
            // Bootstrap the Ultrapeer
            dhtManager.getMojitoDHT().bootstrap(dhts.get(0).getContactAddress()).get();
            
            // -------------------------------------
            
            // And check what the leaf is receiving
            try {
                while(true) {
                    Message msg = out.receive(2000);
                    if (msg instanceof DHTContactsMessage) {
                        fail("Should not have received a DHTContactsMessage: " + msg);
                    }
                }
            } catch (InterruptedIOException expected) {
            }
        } finally {
            out.close();
        }
    }
    
    private void addPassiveLeafCapability() {
        CapabilitiesVMFactoryImplStub factory = (CapabilitiesVMFactoryImplStub) injector.getInstance(CapabilitiesVMFactory.class);
        factory.addMessageBlock(DHTMode.PASSIVE_LEAF.getCapabilityName(), 0);
    }

    protected BlockingConnection createLeafConnection() throws Exception {
        return createConnection(injector.getInstance(HeadersFactory.class).createLeafHeaders("localhost"));
    }
    
    /** Builds a single connection with the given headers. */
    protected BlockingConnection createConnection(Properties headers) throws Exception {
        BlockingConnection c = injector.getInstance(BlockingConnectionFactory.class).createConnection("localhost", PORT);
        c.initialize(headers, new EmptyResponder(), 1000);
        return c;
    }
    
    private class NodeAssignerStub implements NodeAssigner {

        public boolean isTooGoodUltrapeerToPassUp() {
            return false;
        }


        public void start() {
        }


        public void stop() {
        }
        
    }
}
