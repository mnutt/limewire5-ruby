package org.limewire.mojito;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import junit.framework.TestSuite;

import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.settings.ContextSettings;
import org.limewire.mojito.settings.KademliaSettings;

public class ContextTest extends MojitoTestCase {
    
    /*static {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
    }*/
    
    public ContextTest(String name) {
        super(name);
    }

    public static TestSuite suite() {
        return buildTestSuite(ContextTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testStartWithoutBind() {
        MojitoDHT dht = MojitoFactory.createDHT("DHT-0");
        
        try {
            dht.start();
            fail("Start without bind should have failed");
        } catch (IllegalStateException expected) {
            
        } finally {
            dht.close();
        }
    }
    
    public void testStartWithBind() throws Exception {
        MojitoDHT dht = MojitoFactory.createDHT("DHT-0");
        dht.bind(2000);
        
        try {
            dht.start();
        } catch (IllegalStateException err) {
            fail(err);            
        } finally {
            dht.close();
        }
    }
    
    public void testStopWithoutBind() {
        MojitoDHT dht = MojitoFactory.createDHT("DHT-0");
        
        try {
            dht.stop();
        } finally {
            dht.close();
        }
    }
    
    public void testStopWithoutStart() throws Exception {
        MojitoDHT dht = MojitoFactory.createDHT("DHT-0");
        dht.bind(2000);
        
        try {
            dht.stop();
        } finally {
            dht.close();
        }
    }
    
    public void testCloseWithoutBind() {
        MojitoDHT dht = MojitoFactory.createDHT("DHT-0");
        dht.close();
    }
    
    public void testCloseWithoutStart() throws Exception {
        MojitoDHT dht = MojitoFactory.createDHT("DHT-0");
        dht.bind(2000);
        dht.close();
    }
    
    public void testShutdown() throws Exception {
        setLocalIsPrivate(false);
        ContextSettings.SHUTDOWN_MESSAGES_MULTIPLIER.setValue(1);
        
        int m = ContextSettings.SHUTDOWN_MESSAGES_MULTIPLIER.getValue();
        int k = KademliaSettings.REPLICATION_PARAMETER.getValue();
        int expected = m*k;
        
        List<MojitoDHT> dhts = new ArrayList<MojitoDHT>();
        
        try {
            for (int i = 0; i < (2*expected); i++) {
                MojitoDHT dht = MojitoFactory.createDHT("DHT-" + i);
                InetSocketAddress addr = new InetSocketAddress("localhost",2000 + i); 
                dht.bind(addr);
                dht.start();
                
                if (i > 0) {
                    // bootstrap from the node 0, but make sure node 0
                    // pings back to mark the new node as ALIVE.
                    dht.bootstrap(new InetSocketAddress("localhost", 2000)).get();
                    dhts.get(0).ping(addr).get();
                    
                    // make sure after the first k nodes bootstrap the rest have at least k nodes in the rt
                    if (i > k)
                        assertGreaterThanOrEquals(k, dht.getRouteTable().getContacts().size());
                }
                dhts.add(dht);
            }
            dhts.get(0).bootstrap(new InetSocketAddress("localhost", 2000+1)).get();
            Thread.sleep(250);
            
            // Shutdown a random MojitoDHT instance
            Random generator = new Random();
            int index = generator.nextInt(dhts.size() / 2) + k;
            MojitoDHT down = dhts.get(index);
            down.close();
            
            // Give everybody a bit time to process the 
            // shutdown message
            Thread.sleep(2000);
            
            // Check now if the expected number of Nodes
            // was notified about the shutdown
            int downCounter = 0;
            int locationCounter = 0;
            
            for (int i = 0; i < dhts.size(); i++) {
                if (i != index) {
                    Context dht = (Context)dhts.get(i);
                    RouteTable routeTable = dht.getRouteTable();
                    Collection<Contact> nodes = routeTable.getContacts();
                    boolean flag = false;
                    for (Contact node : nodes) {
                        if (node.isShutdown()) {
                            assertFalse("There shouldn't be more than one Nodes in DOWN state per RouteTable", flag);
                            
                            downCounter++;
                            flag = true;
                        }
                        
                        if (node.getNodeID().equals(down.getLocalNodeID())) {
                            locationCounter++;
                        }
                    }
                }
            }
            
            assertGreaterThanOrEquals(expected, locationCounter);
            assertEquals(expected, downCounter);
            
        } finally {
            for (MojitoDHT dht : dhts) {
                dht.close();
            }
        }
    }
}
