package com.limegroup.gnutella.dht;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import junit.framework.Test;

import org.limewire.mojito.KUID;
import org.limewire.mojito.routing.Bucket;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.ContactFactory;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.routing.RouteTable.SelectMode;
import org.limewire.mojito.settings.KademliaSettings;

@SuppressWarnings("null")
public class PassiveLeafRouteTableTest extends DHTTestCase {
    
    public PassiveLeafRouteTableTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(PassiveLeafRouteTableTest.class);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    /**
     * Test the PassiveLeafRouteTable's LRU properties
     */
    public void testPassiveLeafRouteTable() {
        final int k = KademliaSettings.REPLICATION_PARAMETER.getValue();
        
        PassiveLeafRouteTable routeTable = new PassiveLeafRouteTable(Vendor.UNKNOWN, Version.ZERO);
        
        List<Contact> list = new ArrayList<Contact>();
        for (int i = 0; i < 2*k; i++) {
            Contact c = ContactFactory.createUnknownContact(
                    Vendor.UNKNOWN, Version.ZERO, KUID.createRandomID(), 
                    new InetSocketAddress("localhost", 2000+i));
            
            list.add(c);
            routeTable.add(c);
        }
        
        // localhost + k Nodes
        assertEquals(k+1, routeTable.getActiveContacts().size());
        
        // Select k Nodes
        Collection<Contact> selected = routeTable.select(KUID.createRandomID(), k, SelectMode.ALIVE);
        assertEquals(k, selected.size());
        assertFalse(selected.contains(routeTable.getLocalNode()));
        
        // Should make no difference
        selected = routeTable.select(KUID.createRandomID(), k, SelectMode.ALL);
        assertEquals(k, selected.size());
        assertFalse(selected.contains(routeTable.getLocalNode()));
        
        // The first k elements should be gone and the last (newest) 
        // k elements should be there
        for (int i = 0; i < list.size(); i++) {
            Contact c = list.get(i);
            
            if (i < k) {
                assertNull(routeTable.get(c.getNodeID()));
            } else {
                assertEquals(c, routeTable.get(c.getNodeID()));
            }
        }
        
        // Test the actual LRU property. Touch the k+1 th element
        // which should move it to the end of the List
        routeTable.get(list.get(k+1).getNodeID());
        selected = routeTable.select(KUID.createRandomID(), k, SelectMode.ALL);
        
        Contact first = null;
        Contact last = null;
        for (Contact c : selected) {
            if (first == null) {
                first = c;
            }
            last = c;
        }
        
        // And check if it's at the end of the List
        assertEquals(list.get(k+1), last);
        
        // Now add a new Contact to the LRU List
        Contact c = ContactFactory.createUnknownContact(
                Vendor.UNKNOWN, Version.ZERO, KUID.createRandomID(), 
                new InetSocketAddress("localhost", 3000));
        
        list.add(c);
        routeTable.add(c);
        
        // The first element should be gone now and the (previous) 
        // last element should be still there
        assertEquals(c, routeTable.get(c.getNodeID()));
        assertNull(routeTable.get(first.getNodeID()));
        assertEquals(last, routeTable.get(last.getNodeID()));
    }
    
    public void testClassfulNetworkCounter() {
        final int k = KademliaSettings.REPLICATION_PARAMETER.getValue();
        PassiveLeafRouteTable routeTable = new PassiveLeafRouteTable(Vendor.UNKNOWN, Version.ZERO);
        Bucket bucket = routeTable.getBucket(KUID.MINIMUM);
        
        // Fill the Bucket
        KUID firstId = null;
        for (int i = 0; i < k; i++) {
            SocketAddress addr = new InetSocketAddress("192.168." + i + ".1", 1024);
            KUID nodeId = KUID.createRandomID();
            
            if (firstId == null) {
                firstId = nodeId;
            }
            
            Contact node = ContactFactory.createLiveContact(
                    addr, Vendor.UNKNOWN, Version.ZERO, nodeId, addr, 0, 0);
            
            routeTable.add(node);
        }
        
        assertTrue(bucket.containsActiveContact(firstId));
        assertEquals(k, bucket.getClassfulNetworkCounter().size());
        
        // The first Contact changes its IP address
        SocketAddress addr = new InetSocketAddress("192.169.0.1", 1024);
        Contact node = ContactFactory.createLiveContact(
                addr, Vendor.UNKNOWN, Version.ZERO, firstId, addr, 0, 0);
        routeTable.add(node);
        
        // Eject them all from the RouteTable
        for (int i = 0; i < k; i++) {
            addr = new InetSocketAddress("192.170." + i + ".1", 1024);
            KUID nodeId = KUID.createRandomID();
            
            node = ContactFactory.createLiveContact(
                    addr, Vendor.UNKNOWN, Version.ZERO, nodeId, addr, 0, 0);
            
            routeTable.add(node);
        }
    }
}
