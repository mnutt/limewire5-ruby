package com.limegroup.gnutella.dht;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;

import junit.framework.Test;

import org.limewire.core.settings.DHTSettings;
import org.limewire.io.IpPort;
import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.routing.Contact.State;
import org.limewire.mojito.routing.impl.RemoteContact;
import org.limewire.mojito.settings.ContextSettings;
import org.limewire.util.CommonUtils;

import com.google.inject.Injector;
import com.google.inject.Stage;
import com.limegroup.gnutella.LifecycleManager;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.util.EventDispatcher;

public class ActiveDHTNodeControllerTest extends DHTTestCase {
    
    private static final EventDispatcher<DHTEvent, DHTEventListener> dispatcherStub = 
        new DHTEventDispatcherStub();
    
    private Injector injector;

    private DHTControllerFactory dhtControllerFactory;
    
    public ActiveDHTNodeControllerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ActiveDHTNodeControllerTest.class);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    protected void setUp() throws Exception {
        DHTTestUtils.setSettings(PORT);
        DHTSettings.FORCE_DHT_CONNECT.setValue(true);
        
        injector = LimeTestUtils.createInjector(Stage.PRODUCTION);
        
        dhtControllerFactory = injector.getInstance(DHTControllerFactory.class);
    }
    
    public void testPersistence() throws Exception{
        DHTSettings.PERSIST_ACTIVE_DHT_ROUTETABLE.setValue(true);
        DHTSettings.PERSIST_DHT_DATABASE.setValue(true);
        
        //first delete any previous file
        File dhtFile = new File(CommonUtils.getUserSettingsDir(), "active.mojito");
        dhtFile.delete();
        //start the node controller
        ActiveDHTNodeController controller = dhtControllerFactory.createActiveDHTNodeController(Vendor.UNKNOWN,
                Version.ZERO, dispatcherStub);
        
        try {
            Context context = (Context) controller.getMojitoDHT();
            KUID nodeID = context.getLocalNodeID();
            RouteTable rt = context.getRouteTable();
            //fill the routing table a bit
            fillRoutingTable(rt, 10);
            
            //add one more
            KUID kuid = KUID.createRandomID();
            RemoteContact node = new RemoteContact(
                    new InetSocketAddress("localhost",4010),
                    ContextSettings.getVendor(),
                    ContextSettings.getVersion(),
                    kuid,
                    new InetSocketAddress("localhost",4010),
                    0,
                    Contact.DEFAULT_FLAG,
                    State.UNKNOWN);
            rt.add(node);
            
            controller.start();
            controller.stop();
            
            controller = dhtControllerFactory.createActiveDHTNodeController(
                    Vendor.UNKNOWN, Version.ZERO, dispatcherStub);
            context = (Context) controller.getMojitoDHT();
            rt = context.getRouteTable();
            //should have the same nodeID as before
            assertEquals(nodeID, context.getLocalNodeID());
            //should have persisted the routetable
            Collection<Contact> contacts = rt.getContacts();
            assertEquals(12, contacts.size()); //11 + localnode
            assertTrue(contacts.contains(node));
        } finally {
            controller.stop();
        }
    }
    
    public void testGetActiveDHTNodes() throws Exception{
        DHTSettings.FORCE_DHT_CONNECT.setValue(true);
        ActiveDHTNodeController controller = dhtControllerFactory.createActiveDHTNodeController(Vendor.UNKNOWN,
                Version.ZERO, dispatcherStub);
        
        MojitoDHT dhtBootstrapNode = startBootstrapDHT(injector.getInstance(LifecycleManager.class));
        try {        
            try {
                controller.start();
                assertTrue(controller.isRunning());

                // bootstrap active node
                controller.addActiveDHTNode(dhtBootstrapNode.getContactAddress());
                for (int i = 0; i < 10; i++) {
                    if (controller.isBootstrapped()) {
                        break;
                    }

                    Thread.sleep(500);
                }
                assertTrue(controller.isBootstrapped());

                // ask for active nodes -- should return itself and the bootstrap node
                List<IpPort> l = controller.getActiveDHTNodes(10);
                assertEquals(2, l.size());
                assertEquals(injector.getInstance(NetworkManager.class).getPort(), l.get(0).getPort());
                assertEquals(3000, l.get(1).getPort());
            } finally {
                controller.stop();
            }
        } finally {
            dhtBootstrapNode.close();
        }
    }
    
    public void testResetRouteTable() {
        DHTSettings.PERSIST_ACTIVE_DHT_ROUTETABLE.setValue(true);
        DHTSettings.PERSIST_DHT_DATABASE.setValue(true);
        
        File dhtFile = new File(CommonUtils.getUserSettingsDir(), "active.mojito");
        dhtFile.delete();
        
        ActiveDHTNodeController controller = dhtControllerFactory.createActiveDHTNodeController(Vendor.UNKNOWN,
                Version.ZERO, dispatcherStub);
        
        Contact localNode1 = controller.getMojitoDHT().getLocalNode();
        controller.start();
        controller.stop();
        
        controller = dhtControllerFactory.createActiveDHTNodeController(Vendor.UNKNOWN,
                Version.ZERO, dispatcherStub);
        Contact localNode2 = controller.getMojitoDHT().getLocalNode();
        controller.start();
        controller.stop();
        
        assertEquals(localNode1.getNodeID(), localNode2.getNodeID());
        
        DHTSettings.ACTIVE_DHT_ROUTETABLE_VERSION.setValue(1);
        controller = dhtControllerFactory.createActiveDHTNodeController(Vendor.UNKNOWN,
                Version.ZERO, dispatcherStub);
        Contact localNode3 = controller.getMojitoDHT().getLocalNode();
        controller.start();
        controller.stop();
        
        assertNotEquals(localNode1.getNodeID(), localNode3.getNodeID());
    }
}
