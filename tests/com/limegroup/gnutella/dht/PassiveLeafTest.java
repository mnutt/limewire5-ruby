package com.limegroup.gnutella.dht;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import junit.framework.Test;

import org.limewire.core.settings.DHTSettings;
import org.limewire.io.IOUtils;
import org.limewire.mojito.Context;
import org.limewire.mojito.EntityKey;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.MojitoFactory;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.db.impl.DHTValueImpl;
import org.limewire.mojito.result.FindValueResult;
import org.limewire.mojito.result.StoreResult;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.ContactFactory;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.settings.KademliaSettings;
import org.limewire.mojito.util.MojitoUtils;

import com.google.inject.Injector;
import com.google.inject.Stage;
import com.limegroup.gnutella.LifecycleManager;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.dht.DHTManager.DHTMode;
import com.limegroup.gnutella.messages.vendor.DHTContactsMessage;
import com.limegroup.gnutella.stubs.LocalSocketAddressProviderStub;

public class PassiveLeafTest extends DHTTestCase {
    
    private MojitoDHT bootstrapDHT;
    private Injector injector;

    public PassiveLeafTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(PassiveLeafTest.class);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    protected void setUp() throws Exception {
        DHTTestUtils.setSettings(PORT);
        
        injector = LimeTestUtils.createInjector(Stage.PRODUCTION, LocalSocketAddressProviderStub.STUB_MODULE);
        
        bootstrapDHT = startBootstrapDHT(injector.getInstance(LifecycleManager.class));
    }
    
    @Override
    protected void tearDown() throws Exception {
        bootstrapDHT.close();
    }
    
    public void testLookup() throws Exception {
        DHTTestUtils.setLocalIsPrivate(injector, false);
        
        final int k = KademliaSettings.REPLICATION_PARAMETER.getValue();
        
        MojitoDHT passiveLeaf = null;
        List<MojitoDHT> dhts = Collections.emptyList();
        try {
            dhts = MojitoUtils.createBootStrappedDHTs(3, 2000);
            // Store a DHTValue
            KUID key = KUID.createRandomID();
            DHTValue value = new DHTValueImpl(
                    DHTValueType.BINARY, Version.ZERO, "Hello World".getBytes());
            StoreResult result = dhts.get(0).put(key, value).get();
            assertEquals(k, result.getLocations().size());
            
            // Create a passive leaf Node
            passiveLeaf = MojitoFactory.createDHT("PassiveLeaf");
            ((Context)passiveLeaf).setBootstrapped(true);
            ((Context)passiveLeaf).setBucketRefresherDisabled(true);
            RouteTable routeTable = new PassiveLeafRouteTable(Vendor.UNKNOWN, Version.ZERO);
            passiveLeaf.setRouteTable(routeTable);
            passiveLeaf.bind(4000);
            passiveLeaf.start();
            
            // Try to get the value which should fail
            try {
                EntityKey lookupKey = EntityKey.createEntityKey(key, DHTValueType.ANY);
                FindValueResult r = passiveLeaf.get(lookupKey).get();
                if (!r.getEntities().isEmpty()) {
                    fail("Should not have got DHTValue: " + r);
                }
            } catch (ExecutionException err) {
                fail(err);
            }
            
            // Ping a Node which will add it to the passive leafs RouteTable
            assertEquals(1, routeTable.size());
            passiveLeaf.ping(dhts.get(0).getContactAddress()).get();
            assertEquals(2, routeTable.size());
            
            // Try again and it should work now
            try {
                EntityKey lookupKey = EntityKey.createEntityKey(key, DHTValueType.ANY);
                FindValueResult r = passiveLeaf.get(lookupKey).get();
                if (r.getEntities().isEmpty()) {
                    fail("Should have found DHTValue");
                }
            } catch (ExecutionException err) {
                fail(err);
            }
            
        } finally {
            IOUtils.close(dhts);
            
            if (passiveLeaf != null) {
                passiveLeaf.close();
            }
        }
    }
    
    public void testPassiveLeafController() throws Exception {
        DHTSettings.FORCE_DHT_CONNECT.setValue(true);
        
        PassiveLeafController controller = injector.getInstance(DHTControllerFactory.class).createPassiveLeafController(Vendor.UNKNOWN,
                Version.ZERO, new DHTEventDispatcherStub());
        try {
            controller.start();
            
            // Check initial state
            MojitoDHT dht = controller.getMojitoDHT();
            assertTrue(dht.isBootstrapped());
            assertTrue(dht.isFirewalled());
            
            RouteTable routeTable = dht.getRouteTable();
            assertEquals(1, routeTable.size());
            assertTrue(routeTable instanceof PassiveLeafRouteTable);
            
            // Add a Contact
            Contact c = ContactFactory.createUnknownContact(
                    Vendor.UNKNOWN, Version.ZERO, KUID.createRandomID(), 
                    new InetSocketAddress("localhost", 3000));
            controller.addContact(c);
            
            // And check the final state of the RouteTable
            assertEquals(2, routeTable.size());
        } finally {
            controller.stop();
        }
    }
    
    public void testPassiveLeafManager() throws Exception {
        DHTSettings.FORCE_DHT_CONNECT.setValue(true);
        
        DHTManager manager = new DHTManagerImpl(Executors.newSingleThreadExecutor(), injector.getInstance(DHTControllerFactory.class));
        try {
            // Check initial state
            assertEquals(DHTMode.INACTIVE, manager.getDHTMode());
            
            // Start in passive mode
            manager.start(DHTMode.PASSIVE_LEAF);
            Thread.sleep(250);
            assertEquals(DHTMode.PASSIVE_LEAF, manager.getDHTMode());
            
            // Stop and it should be in inital state again
            manager.stop();
            Thread.sleep(250);
            assertEquals(DHTMode.INACTIVE, manager.getDHTMode());
            
            // Start again
            manager.start(DHTMode.PASSIVE_LEAF);
            Thread.sleep(250);
            assertEquals(DHTMode.PASSIVE_LEAF, manager.getDHTMode());
            
            // Add a Contact through handleDHTContactsMessage(Contact)
            Contact c = ContactFactory.createUnknownContact(
                    Vendor.UNKNOWN, Version.ZERO, KUID.createRandomID(), 
                    new InetSocketAddress("localhost", 3000));
            DHTContactsMessage msg = new DHTContactsMessage(c);
            manager.handleDHTContactsMessage(msg);
            Thread.sleep(250);
            
            // Check the RouteTable
            MojitoDHT dht = manager.getMojitoDHT();
            RouteTable routeTable = dht.getRouteTable();
            assertEquals(2, routeTable.size());
        } finally {
            manager.stop();
        }
    }
}
