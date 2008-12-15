/*
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006-2007 LimeWire LLC
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
 
package org.limewire.mojito;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import junit.framework.TestSuite;

import org.limewire.collection.PatriciaTrie;
import org.limewire.collection.Trie;
import org.limewire.collection.TrieUtils;
import org.limewire.mojito.concurrent.CallableDHTTask;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.DHTTask;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.db.impl.DHTValueImpl;
import org.limewire.mojito.exceptions.DHTException;
import org.limewire.mojito.result.Result;
import org.limewire.mojito.result.StoreResult;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.settings.DatabaseSettings;
import org.limewire.mojito.settings.KademliaSettings;
import org.limewire.mojito.settings.RouteTableSettings;
import org.limewire.mojito.util.UnitTestUtils;
import org.limewire.security.SecurityToken;

@SuppressWarnings("null")
public class CacheForwardTest extends MojitoTestCase {
    
    private static final int PORT = 3000;
    
    /*static {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
    }*/
    
    public CacheForwardTest(String name) {
        super(name);
    }
   
    public static TestSuite suite() {
        return buildTestSuite(CacheForwardTest.class);
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setLocalIsPrivate(false);
        RouteTableSettings.INCOMING_REQUESTS_UNKNOWN.setValue(true);
    }

    @SuppressWarnings("unchecked")
    public void testGetSecurityToken() throws Exception {
        
        MojitoDHT dht1 = null;
        MojitoDHT dht2 = null;
        
        try {
            
            // Setup the first instance so that it thinks it's bootstrapping
            dht1 = MojitoFactory.createDHT();
            dht1.bind(2000);
            dht1.start();
            Context context1 = (Context)dht1;
            
            UnitTestUtils.setBootstrapping(dht1, true);            
            assertFalse(dht1.isBootstrapped());
            assertTrue(context1.isBootstrapping());
            
            // And setup the second instance so that it thinks it's bootstrapped 
            dht2 = MojitoFactory.createDHT();
            dht2.bind(3000);
            dht2.start();
            Context context2 = (Context)dht2;
            
            UnitTestUtils.setBootstrapped(dht2, true);
            assertTrue(dht2.isBootstrapped());
            assertFalse(context2.isBootstrapping());
            
            // Get the SecurityToken...
            Class clazz = Class.forName("org.limewire.mojito.manager.StoreProcess$GetSecurityTokenHandler");
            Constructor<DHTTask<Result>> con 
                = clazz.getDeclaredConstructor(Context.class, Contact.class);
            con.setAccessible(true);
            
            DHTTask<Result> task = con.newInstance(context2, context1.getLocalNode());
            CallableDHTTask<Result> callable = new CallableDHTTask<Result>(task);
            
            try {
                Result result = callable.call();
                clazz = Class.forName("org.limewire.mojito.manager.StoreProcess$GetSecurityTokenResult");
                Method m = clazz.getDeclaredMethod("getSecurityToken", new Class[0]);
                m.setAccessible(true);
                
                SecurityToken securityToken = (SecurityToken)m.invoke(result, new Object[0]);
                assertNotNull(securityToken);
            } catch (ExecutionException err) {
            	assertInstanceof(DHTException.class, err.getCause());
                fail("DHT-1 did not return a SecurityToken", err);
            }
            
        } finally {
            if (dht1 != null) { dht1.close(); }
            if (dht2 != null) { dht2.close(); }
        }
    }
    
    public void testCacheForward() throws Exception {
        
        final long waitForNodes = 1000; // ms

        //ContextSettings.SEND_SHUTDOWN_MESSAGE.setValue(false);
        DatabaseSettings.DELETE_VALUE_IF_FURTHEST_NODE.setValue(false);
        int k = KademliaSettings.REPLICATION_PARAMETER.getValue();
        
        // KUID valueId = KUID.create("40229239B68FFA66575E59D0AB1F685AD3191960");
        KUID valueId = KUID.createRandomID();
        
        Map<KUID, MojitoDHT> dhts = new HashMap<KUID, MojitoDHT>();
        MojitoDHT first = null;
        try {
            for (int i = 0; i < 3*k; i++) {
                MojitoDHT dht = MojitoFactory.createDHT("DHT-" + i);
                dht.bind(PORT + i);
                dht.start();
                
                if (i > 0) {
                    Thread.sleep(100);
                    dht.bootstrap(new InetSocketAddress("localhost", PORT)).get();
                    // the bootstrapper needs to ping every joining node.
                    first.ping(new InetSocketAddress("localhost", PORT+i)).get();
                } else {
                    first = dht;
                }
                
                dhts.put(dht.getLocalNodeID(), dht);
            }
            
            first.bootstrap(new InetSocketAddress("localhost", PORT+1)).get();
            
            
            // Sort all KUIDs by XOR distance and use the Node as 
            // creator that's furthest away from the value ID so
            // that it never cannot be member of the k-closest Nodes
            Trie<KUID, KUID> trie = new PatriciaTrie<KUID, KUID>(KUID.KEY_ANALYZER);
            for (KUID id : dhts.keySet()) {
                trie.put(id, id);
            }
            List<KUID> idsByXorDistance = TrieUtils.select(trie, valueId, trie.size());
            
            // Creator happens to be the furthest of the k-closest Nodes. 
            //MojitoDHT creator = dhts.get(idsByXorDistance.get(k-1)); 
            
            // Use the furthest Node as the creator.
            MojitoDHT creator = dhts.get(idsByXorDistance.get(idsByXorDistance.size()-1));
            // Store the value
            DHTValue value = new DHTValueImpl(DHTValueType.TEST, Version.ZERO, "Hello World".getBytes());
            StoreResult evt = creator.put(valueId, value).get();
            assertEquals(k, evt.getLocations().size());
            
            // Give everybody time to process the store request
            Thread.sleep(waitForNodes);
            
            // And check the initial state
            Context closest = null;
            for (Contact remote : evt.getLocations()) {
                Context dht = (Context)dhts.get(remote.getNodeID());
                assertEquals(1, dht.getDatabase().getKeyCount());
                assertEquals(1, dht.getDatabase().getValueCount());
                for (DHTValueEntity dhtValue : dht.getDatabase().values()) {
                    assertEquals(valueId, dhtValue.getPrimaryKey());
                    assertEquals(value, dhtValue.getValue());
                    assertEquals(creator.getLocalNodeID(), dhtValue.getSecondaryKey());
                    assertEquals(creator.getLocalNodeID(), dhtValue.getSender().getNodeID());
                }
                
                if (closest == null) {
                    closest = dht;
                }
            }
            
            // Create a Node with the nearest possible Node ID
            // That means we set the Node ID to the Value ID
            Context nearest = (Context)MojitoFactory.createDHT("Nearest");
            Method m = nearest.getClass().getDeclaredMethod("setLocalNodeID", new Class[]{KUID.class});
            m.setAccessible(true);
            m.invoke(nearest, new Object[]{valueId});
            
            nearest.bind(PORT+500);
            nearest.start();
            bootstrap(nearest, dhts.values());
            
            
            // Give everybody time to figure out whether to forward
            // a value or to remove it
            Thread.sleep(waitForNodes);

            // The Node with the nearest possible ID should have the value
            assertEquals(1, nearest.getDatabase().getValueCount());
            
            for (Contact remote : evt.getLocations()) {
                Context dht = (Context)dhts.get(remote.getNodeID());

                assertEquals("I dont have it?? "+dht.getLocalNodeID(),1, dht.getDatabase().getKeyCount());
                assertEquals(1, dht.getDatabase().getValueCount());
                for (DHTValueEntity dhtValue : dht.getDatabase().values()) {
                    assertEquals(valueId, dhtValue.getPrimaryKey());
                    assertEquals(value, dhtValue.getValue());
                    assertEquals(creator.getLocalNodeID(), dhtValue.getSecondaryKey());
                    assertEquals(creator.getLocalNodeID(), dhtValue.getSender().getNodeID());
                }
            }
            
            // The 'nearest' Node received the value from the 
            // previous 'closest' Node
            assertEquals(1, nearest.getDatabase().getKeyCount());
            assertEquals(1, nearest.getDatabase().getValueCount());
            for (DHTValueEntity dhtValue : nearest.getDatabase().values()) {
                assertEquals(valueId, dhtValue.getPrimaryKey());
                assertEquals(value, dhtValue.getValue());
                assertEquals(creator.getLocalNodeID(), dhtValue.getSecondaryKey());
                
                // The closest Node send us the value!
                assertEquals(closest.getLocalNodeID(), dhtValue.getSender().getNodeID());
            }
            
            // Clear the Database but don't change the instanceId!
            // The other Nodes don't know that we cleared our DB
            // and will this not store values!
            nearest.getDatabase().clear();
            assertEquals(0, nearest.getDatabase().getKeyCount());
            assertEquals(0, nearest.getDatabase().getValueCount());
            bootstrap(nearest, dhts.values());
            
            // Give everybody time to figure out whether to forward
            // a value or to remove it
            Thread.sleep(waitForNodes);
            
            assertEquals(0, nearest.getDatabase().getKeyCount());
            assertEquals(0, nearest.getDatabase().getValueCount());
            
            // Change the instanceId and we'll asked to store the
            // value again!
            nearest.getLocalNode().nextInstanceID();
            bootstrap(nearest, dhts.values());
            
            // Give everybody time to figure out whether to forward
            // a value or to remove it
            Thread.sleep(waitForNodes);
            
            assertEquals(1, nearest.getDatabase().getKeyCount());
            assertEquals(1, nearest.getDatabase().getValueCount());
            for (DHTValueEntity dhtValue : nearest.getDatabase().values()) {
                assertEquals(valueId, dhtValue.getPrimaryKey());
                assertEquals(value, dhtValue.getValue());
                assertEquals(creator.getLocalNodeID(), dhtValue.getSecondaryKey());
                
                // The closest Node send us the value!
                assertEquals(closest.getLocalNodeID(), dhtValue.getSender().getNodeID());
            }
            
            // Pick a Node from the middle of the k-closest Nodes,
            // clear its Database and do the same test as with the
            // nearest Node above
            Context middle = null;
            int index = 0;
            for (Contact node : evt.getLocations()) {
                if (index == k/2) {
                    middle = (Context)dhts.get(node.getNodeID());
                    break;
                }
                
                index++;
            }
            assertNotNull(middle);
            
            middle.getDatabase().clear();
            assertEquals(0, middle.getDatabase().getKeyCount());
            assertEquals(0, middle.getDatabase().getValueCount());
            bootstrap(middle, dhts.values());
            Thread.sleep(waitForNodes);
            
            assertEquals(0, middle.getDatabase().getKeyCount());
            assertEquals(0, middle.getDatabase().getValueCount());
            
            middle.getLocalNode().nextInstanceID();
            bootstrap(middle, dhts.values());
            Thread.sleep(waitForNodes);
            
            assertEquals(1, middle.getDatabase().getKeyCount());
            assertEquals(1, middle.getDatabase().getValueCount());
            for (DHTValueEntity dhtValue : middle.getDatabase().values()) {
                assertEquals(valueId, dhtValue.getPrimaryKey());
                assertEquals(value, dhtValue.getValue());
                assertEquals(creator.getLocalNodeID(), dhtValue.getSecondaryKey());
                
                // The nearest Node send us the value
                assertEquals(nearest.getLocalNodeID(), dhtValue.getSender().getNodeID());
            }
            
            // Check the final state. k + 1 Nodes should have the value!
            int count = 0;
            dhts.put(nearest.getLocalNodeID(), nearest);
            for (MojitoDHT dht : dhts.values()) {
                count += ((Context)dht).getDatabase().values().size();
            }
            
            // If the creator is a member of the k-closest Nodes then
            // make sure we're counting it as well
            for (Contact node : evt.getLocations()) {
                if (node.getNodeID().equals(creator.getLocalNodeID())) {
                    count++;
                    break;
                }
            }
            
            assertEquals(k + 1, count);
            
        } finally {
            for (MojitoDHT dht : dhts.values()) {
                dht.close();
            }
        }
    }
    
    /**
     * This test will fail if the deletion step of store-forwarding
     * is enabled.  Its a reminder to test that functionality if
     * we ever decide to enable it.
     */
    public void testCacheForwardAndDelete() {
        DatabaseSettings.DELETE_VALUE_IF_FURTHEST_NODE.revertToDefault();
        assertFalse(DatabaseSettings.DELETE_VALUE_IF_FURTHEST_NODE.getValue());
    }
    
    public void testStoreMultipleValues() throws Exception {
        MojitoDHT dht1 = null;
        MojitoDHT dht2 = null;
        
        try {
            dht1 = MojitoFactory.createDHT("DHT1");
            dht1.bind(2000);
            dht1.start();
            
            dht2 = MojitoFactory.createDHT("DHT2");
            dht2.bind(2001);
            dht2.start();
            
            dht1.bootstrap(new InetSocketAddress("localhost", 2001)).get();
            dht2.bootstrap(new InetSocketAddress("localhost", 2000)).get();
            
            Context context1 = (Context)dht1;
            
            KUID primaryKey1 = KUID.createRandomID();
            KUID primaryKey2 = KUID.createRandomID();
            
            DHTValue value1 = new DHTValueImpl(DHTValueType.TEST, Version.ZERO, "Hello World".getBytes());
            DHTValue value2 = new DHTValueImpl(DHTValueType.TEST, Version.ZERO, "Foo Bar".getBytes());
            
            DHTValueEntity entity1 = DHTValueEntity.createFromValue(context1, primaryKey1, value1);
            DHTValueEntity entity2 = DHTValueEntity.createFromValue(context1, primaryKey2, value2);
            
            Collection<DHTValueEntity> entities = Arrays.asList(entity1, entity2);
            DHTFuture<StoreResult> future = context1.store(
                    dht2.getLocalNode(), null, entities);
          /*  StoreResult result = */ future.get(); // TODO: should result be tested?
            
            assertEquals(2, dht2.getDatabase().getKeyCount());
            assertEquals(2, dht2.getDatabase().getValueCount());
        } finally {
            if (dht1 != null) { dht1.close(); }
            if (dht2 != null) { dht2.close(); }
        }
    }
    
    
    /**
     * Bootstraps the given Node from one of the other Nodes and makes sure a
     * Node isn't trying to bootstrap from itself which would fail.
     */
    private static void bootstrap(MojitoDHT dht, Collection<MojitoDHT> dhts) 
            throws ExecutionException, InterruptedException {
        
        for (MojitoDHT other : dhts) {
            if (!dht.getLocalNodeID().equals(other.getLocalNodeID())) {
                InetSocketAddress addr = (InetSocketAddress)other.getContactAddress();
                dht.bootstrap(new InetSocketAddress("localhost", addr.getPort())).get();
                return;
            }
        }
        
        throw new IllegalStateException("Could not bootstrap: " + dht);
    }
}
