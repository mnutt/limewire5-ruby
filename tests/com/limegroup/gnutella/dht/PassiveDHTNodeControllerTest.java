package com.limegroup.gnutella.dht;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import junit.framework.Test;

import org.limewire.core.settings.DHTSettings;
import org.limewire.core.settings.NetworkSettings;
import org.limewire.io.IpPort;
import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.MojitoFactory;
import org.limewire.mojito.concurrent.DHTFutureListener;
import org.limewire.mojito.result.PingResult;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.util.CollectionUtils;
import org.limewire.mojito.util.MojitoUtils;
import org.limewire.util.CommonUtils;

import com.google.inject.Injector;
import com.google.inject.Stage;
import com.limegroup.gnutella.LifecycleManager;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.stubs.LocalSocketAddressProviderStub;
import com.limegroup.gnutella.util.EventDispatcher;

public class PassiveDHTNodeControllerTest extends DHTTestCase {
    
    private static final EventDispatcher<DHTEvent, DHTEventListener> dispatcherStub 
        = new DHTEventDispatcherStub();
    private MojitoDHT bootstrapDHT;
    private DHTControllerFactory dhtControllerFactory;
    private Injector injector;
    
    public PassiveDHTNodeControllerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(PassiveDHTNodeControllerTest.class);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    public void setUp() throws Exception {
        DHTTestUtils.setSettings(PORT);
        DHTSettings.FORCE_DHT_CONNECT.setValue(true);
        assertEquals("unexpected port", PORT, NetworkSettings.PORT.getValue());
        
        injector = LimeTestUtils.createInjector(Stage.PRODUCTION, LocalSocketAddressProviderStub.STUB_MODULE);
        dhtControllerFactory = injector.getInstance(DHTControllerFactory.class);
        
        bootstrapDHT = startBootstrapDHT(injector.getInstance(LifecycleManager.class));
    }
    
    @Override
    protected void tearDown() throws Exception {
        bootstrapDHT.close();
    }

    public void testNodesPersistence() throws Exception{
        DHTSettings.PERSIST_ACTIVE_DHT_ROUTETABLE.setValue(true);
        DHTSettings.PERSIST_DHT_DATABASE.setValue(true);
        int numPersistedNodes = DHTSettings.MAX_PERSISTED_NODES.getValue();
        
        //first delete any previous file
        File dhtFile = new File(CommonUtils.getUserSettingsDir(), "mojito.dat");
        dhtFile.delete();
        PassiveDHTNodeController controller = dhtControllerFactory.createPassiveDHTNodeController(
                Vendor.UNKNOWN, Version.ZERO, dispatcherStub);
        try {
            Context context = (Context) controller.getMojitoDHT();
            Contact localContact = context.getLocalNode();
            KUID nodeID = context.getLocalNodeID();
            RouteTable rt = context.getRouteTable();
            rt.setContactPinger(new RouteTable.ContactPinger() {
                public void ping(Contact node,
                        DHTFutureListener<PingResult> listener) {
                }
            });
            
            //fill the routing table a bit
            fillRoutingTable(rt, 2*numPersistedNodes);
            controller.start();
            Thread.sleep(5000);
            controller.stop();
            //now nodeID should have changed and we should have persisted SOME nodes
            controller = dhtControllerFactory.createPassiveDHTNodeController(
                    Vendor.UNKNOWN, Version.ZERO, dispatcherStub);
            context = (Context) controller.getMojitoDHT();
            rt = context.getRouteTable();
            assertNotEquals(nodeID, context.getLocalNodeID());
            assertFalse(rt.select(nodeID).equals(localContact));
            assertEquals(numPersistedNodes, context.getRouteTable().getActiveContacts().size());
        } finally {
            controller.stop();
        }
    }
    
    public void testAddRemoveLeafDHTNode() throws Exception {
        DHTTestUtils.setLocalIsPrivate(injector, false);
        DHTSettings.FORCE_DHT_CONNECT.setValue(true);
        
        // Initial State:
        //   There's one (1) Controller Node
        //   There's one (1) Bootstrap Node (Port 3000)
        //   There are 20 Nodes from Port 2000 to 20019
        //   Total: 22 Nodes
        
        PassiveDHTNodeController controller = dhtControllerFactory.createPassiveDHTNodeController(
                Vendor.UNKNOWN, Version.ZERO, dispatcherStub);
        
        List<MojitoDHT> dhts = new ArrayList<MojitoDHT>();
        try {
            MojitoDHT context = controller.getMojitoDHT();
            controller.start();
            
            PassiveDHTNodeRouteTable rt = (PassiveDHTNodeRouteTable) context.getRouteTable();
            
            // Bootstrap the 20 Nodes from the one Bootstrap Node
            MojitoDHT dht = null;
            for(int i = 0; i < 20; i++) {
                dht = MojitoFactory.createDHT("Mojito-"+i, Vendor.UNKNOWN, Version.ZERO);
                int port = 2000+i;
                dht.bind(port);
                dht.start();
                MojitoUtils.bootstrap(dht, new InetSocketAddress("localhost",BOOTSTRAP_DHT_PORT)).get();
                
                // And add each of the 20 Nodes to the controller's
                // leafs list
                controller.addLeafDHTNode("localhost", port);
                dhts.add(dht);
            }
            
            // The Controller Node pings each Node from its
            // internal leafs list before it adds them to the
            // actual RouteTable. Give it a bit time to do so.
            Thread.sleep(1000);
            
            // We should heve leaves
            assertTrue(rt.hasDHTLeaves());
            
            // size(ActiveContacts) = 20 leaves + bootstrap + self
            // --> 20 leaves
            assertEquals(rt.getDHTLeaves().size(), rt.getActiveContacts().size() - 2);
            
            // Try removing leaf Nodes, was 22 Nodes, now 20 Nodes
            assertNotNull(controller.removeLeafDHTNode("localhost",2000));
            assertNotNull(controller.removeLeafDHTNode("localhost",2015));
            
            // See if leaves were removed
            assertFalse(rt.getDHTLeaves().contains(new InetSocketAddress("localhost", 2000)));
            assertFalse(rt.getDHTLeaves().contains(new InetSocketAddress("localhost", 2015)));
            assertTrue(rt.getDHTLeaves().contains(new InetSocketAddress("localhost", 2001)));
            
            // size(ActiveContacts) = 18 leaves + bootstrap + self
            // --> 20 Nodes
            //assertEquals(20, rt.getActiveContacts().size());
            Collection<Contact> c = rt.getActiveContacts();
            if (c.size() != 20) {
                StringBuilder buffer = new StringBuilder();
                buffer.append("Before: ").append(CollectionUtils.toString(c)).append("\n");
                Thread.sleep(10000); // Race condition?
                buffer.append("After: ").append(CollectionUtils.toString(rt.getActiveContacts())).append("\n");
                fail(buffer.toString());
            }
            
            //getActive nodes should return leaf nodes when node is bootstrapped
            List<IpPort> l = controller.getActiveDHTNodes(30);
            //and not local node
            Contact localNode = context.getLocalNode();
            InetSocketAddress addr = (InetSocketAddress) localNode.getContactAddress();
            boolean found = false;
            for(IpPort ipp: l) {
                assertFalse(ipp.getPort() == 2000);
                assertFalse(ipp.getPort() == 2015);
                assertFalse(ipp.getPort() == addr.getPort());
                found |= (ipp.getPort() == 2001);
            }
            assertTrue(found);
        } finally {
            controller.stop();
            for (MojitoDHT dht : dhts) {
                dht.close();
            }
        }
    }
}
