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
 
package org.limewire.mojito.db;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.Test;

import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.MojitoFactory;
import org.limewire.mojito.MojitoTestCase;
import org.limewire.mojito.db.impl.DHTValueEntityBag;
import org.limewire.mojito.db.impl.DHTValueImpl;
import org.limewire.mojito.db.impl.DatabaseImpl;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.ContactFactory;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.settings.ContextSettings;
import org.limewire.mojito.settings.DatabaseSettings;
import org.limewire.util.PrivilegedAccessor;
import org.limewire.util.StringUtils;


public class DatabaseTest extends MojitoTestCase {
    
    /*static {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
    }*/
    
    public DatabaseTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(DatabaseTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    private static DHTValueEntity createLocalDHTValue(byte[] value) {
        return createLocalDHTValue(KUID.createRandomID(), 
                KUID.createRandomID(), value);
    }
    
    private static DHTValueEntity createLocalDHTValue(KUID nodeId, KUID valueId, byte[] value) {
        Contact node = ContactFactory.createLocalContact(Vendor.UNKNOWN, Version.ZERO, nodeId, 0, false);
        return new DHTValueEntity(node, node, valueId, 
                new DHTValueImpl(DHTValueType.TEST, Version.ZERO, value), true);
    }
    
    private static DHTValueEntity createDirectDHTValue(byte[] value) {
        return createDirectDHTValue(KUID.createRandomID(), 
                KUID.createRandomID(), value);
    }
    
    private static DHTValueEntity createDirectDHTValue(KUID nodeId, KUID valueId, byte[] value) {
        SocketAddress addr = new InetSocketAddress(6666);
        Contact node = ContactFactory.createLiveContact(addr, Vendor.UNKNOWN, Version.ZERO, 
                nodeId, addr, 0, Contact.DEFAULT_FLAG);
        
        return new DHTValueEntity(node, node, valueId, 
                new DHTValueImpl(DHTValueType.TEST, Version.ZERO, value), false);
    }
    
    private static DHTValueEntity createIndirectDHTValue(byte[] value) {
        return createIndirectDHTValue(KUID.createRandomID(), 
                KUID.createRandomID(), KUID.createRandomID(), value);
    }
    
    private static DHTValueEntity createIndirectDHTValue(KUID creatorId, KUID senderId, KUID valueId, byte[] value) {
        SocketAddress addr = new InetSocketAddress(6666);
        
        Contact creator = ContactFactory.createLiveContact(addr, Vendor.UNKNOWN, Version.ZERO, 
                creatorId, addr, 0, Contact.DEFAULT_FLAG);
        Contact sender = ContactFactory.createLiveContact(addr, Vendor.UNKNOWN, Version.ZERO, 
                senderId, addr, 0, Contact.DEFAULT_FLAG);  
        
        return new DHTValueEntity(creator, sender, valueId, 
                new DHTValueImpl(DHTValueType.TEST, Version.ZERO, value), false);
    }
    
    public void testLocalAdd() throws Exception {
        Database database = new DatabaseImpl();
        
        // Add a local value
        DHTValueEntity value1 = createLocalDHTValue(StringUtils.toAsciiBytes("Hello World"));
        database.store(value1);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals(StringUtils.toAsciiBytes("Hello World"), 
                database.get(value1.getPrimaryKey())
                    .get(value1.getSecondaryKey()).getValue().getValue()));
        
        // Neither direct...
        DHTValueEntity value2 = createDirectDHTValue(value1.getSecondaryKey(), 
													 value1.getPrimaryKey(), StringUtils.toAsciiBytes("Mojito"));
        database.store(value2);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals(StringUtils.toAsciiBytes("Hello World"), 
                database.get(value2.getPrimaryKey())
                    .get(value2.getSecondaryKey()).getValue().getValue()));
        
        // ...nor indirect values can replace a local value
        DHTValueEntity value3 = createIndirectDHTValue(value1.getSecondaryKey(), 
													   KUID.createRandomID(), value1.getPrimaryKey(), StringUtils.toAsciiBytes("Mary"));
        database.store(value3);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals(StringUtils.toAsciiBytes("Hello World"), 
                database.get(value3.getPrimaryKey())
                    .get(value3.getSecondaryKey()).getValue().getValue()));
        
        // Only local values can replace local values
        DHTValueEntity value4 = createLocalDHTValue(value1.getSecondaryKey(), 
													value1.getPrimaryKey(), StringUtils.toAsciiBytes("Tonic"));
        database.store(value4);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals(StringUtils.toAsciiBytes("Tonic"), 
                database.get(value4.getPrimaryKey())
                    .get(value4.getSecondaryKey()).getValue().getValue()));
        
        // Add a new direct value
        DHTValueEntity value5 = createDirectDHTValue(StringUtils.toAsciiBytes("Mojito"));
        database.store(value5);
        assertEquals(2, database.getKeyCount());
        assertEquals(2, database.getValueCount());
        assertTrue(Arrays.equals(StringUtils.toAsciiBytes("Mojito"), 
                database.get(value5.getPrimaryKey())
                    .get(value5.getSecondaryKey()).getValue().getValue()));
        
        // local values replace direct values
        DHTValueEntity value6 = createLocalDHTValue(value5.getSecondaryKey(), 
													value5.getPrimaryKey(), StringUtils.toAsciiBytes("Mary"));
        database.store(value6);
        assertEquals(2, database.getKeyCount());
        assertEquals(2, database.getValueCount());
        assertTrue(Arrays.equals(StringUtils.toAsciiBytes("Mary"), 
                database.get(value6.getPrimaryKey())
                    .get(value6.getSecondaryKey()).getValue().getValue()));
        
        // Add an indirect value
        DHTValueEntity value7 = createDirectDHTValue(StringUtils.toAsciiBytes("Bloody"));
        database.store(value7);
        assertEquals(3, database.getKeyCount());
        assertEquals(3, database.getValueCount());
        assertTrue(Arrays.equals(StringUtils.toAsciiBytes("Bloody"), 
                database.get(value7.getPrimaryKey())
                    .get(value7.getSecondaryKey()).getValue().getValue()));
        
        // local values replace indirect values
        DHTValueEntity value8 = createLocalDHTValue(value7.getSecondaryKey(), 
													value7.getPrimaryKey(), StringUtils.toAsciiBytes("Lime"));
        database.store(value8);
        assertEquals(3, database.getKeyCount());
        assertEquals(3, database.getValueCount());
        assertTrue(Arrays.equals(StringUtils.toAsciiBytes("Lime"), 
                database.get(value8.getPrimaryKey())
                    .get(value8.getSecondaryKey()).getValue().getValue()));

    }
    
    public void testDirectAdd() throws Exception {
        Database database = new DatabaseImpl();
        
        // Add a directly stored value
        DHTValueEntity value1 = createDirectDHTValue(StringUtils.toAsciiBytes("Hello World"));
        database.store(value1);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals(StringUtils.toAsciiBytes("Hello World"), 
                database.get(value1.getPrimaryKey())
                    .get(value1.getSecondaryKey()).getValue().getValue()));
        
        // Shouldn't change
        database.store(value1);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        
        // The originator is issuing a direct store request
        DHTValueEntity value2 = createDirectDHTValue(value1.getSecondaryKey(), 
													 value1.getPrimaryKey(), StringUtils.toAsciiBytes("Mojito"));
        database.store(value2);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals(StringUtils.toAsciiBytes("Mojito"), 
                database.get(value2.getPrimaryKey())
                    .get(value2.getSecondaryKey()).getValue().getValue()));
        
        // A directly stored value cannot be replaced by
        // an indirect value
        DHTValueEntity value3 = createIndirectDHTValue(value2.getSecondaryKey(), 
													   KUID.createRandomID(), value2.getPrimaryKey(), StringUtils.toAsciiBytes("Tough"));
        database.store(value3);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals(StringUtils.toAsciiBytes("Mojito"), 
                database.get(value3.getPrimaryKey())
                    .get(value3.getSecondaryKey()).getValue().getValue()));
    }
    
    public void testIndirectAdd() throws Exception {
        Database database = new DatabaseImpl();
        
        // Add an indirectly stored value
        DHTValueEntity value1 = createIndirectDHTValue(StringUtils.toAsciiBytes("Hello World"));
        database.store(value1);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals(StringUtils.toAsciiBytes("Hello World"), 
                database.get(value1.getPrimaryKey())
                    .get(value1.getSecondaryKey()).getValue().getValue()));
        
        // Indirect replaces indirect
        DHTValueEntity value2 = createIndirectDHTValue(value1.getSecondaryKey(), 
													   KUID.createRandomID(), value1.getPrimaryKey(), StringUtils.toAsciiBytes("Mojito"));
        database.store(value2);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals(StringUtils.toAsciiBytes("Mojito"), 
                database.get(value2.getPrimaryKey())
                    .get(value2.getSecondaryKey()).getValue().getValue()));
        
        // Direct replaces indirect
        DHTValueEntity value3 = createDirectDHTValue(value2.getSecondaryKey(), 
													 value2.getPrimaryKey(), StringUtils.toAsciiBytes("Tonic"));
        database.store(value3);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals(StringUtils.toAsciiBytes("Tonic"), 
                database.get(value3.getPrimaryKey())
                    .get(value3.getSecondaryKey()).getValue().getValue()));
        
        // Indirect shouldn't replace direct
        DHTValueEntity value4 = createIndirectDHTValue(value3.getSecondaryKey(), 
													   KUID.createRandomID(), value3.getPrimaryKey(), StringUtils.toAsciiBytes("Mary"));
        database.store(value3);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals(StringUtils.toAsciiBytes("Tonic"), 
                database.get(value4.getPrimaryKey())
                    .get(value4.getSecondaryKey()).getValue().getValue()));
    }
    
    public void testMultipleValues() throws Exception {
        Database database = new DatabaseImpl();
        
        // Add a value
        DHTValueEntity value1 = createIndirectDHTValue(StringUtils.toAsciiBytes("Hello World"));
        database.store(value1);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals(StringUtils.toAsciiBytes("Hello World"), 
                database.get(value1.getPrimaryKey())
                    .get(value1.getSecondaryKey()).getValue().getValue()));
        
        // Same Key but different originator/sender
        DHTValueEntity value2 = createIndirectDHTValue(KUID.createRandomID(), 
													   KUID.createRandomID(), value1.getPrimaryKey(), StringUtils.toAsciiBytes("Tonic"));
        database.store(value2);
        assertEquals(1, database.getKeyCount());
        assertEquals(2, database.getValueCount());
        assertTrue(Arrays.equals(StringUtils.toAsciiBytes("Tonic"), 
                database.get(value2.getPrimaryKey())
                    .get(value2.getSecondaryKey()).getValue().getValue()));
        
        // Same Key but a different originator
        DHTValueEntity value3 = createDirectDHTValue(KUID.createRandomID(),
													 value1.getPrimaryKey(), StringUtils.toAsciiBytes("Mary"));
        database.store(value3);
        assertEquals(1, database.getKeyCount());
        assertEquals(3, database.getValueCount());
        assertTrue(Arrays.equals(StringUtils.toAsciiBytes("Mary"), 
                database.get(value3.getPrimaryKey())
                    .get(value3.getSecondaryKey()).getValue().getValue()));
        
        // Different Key
        DHTValueEntity value4 = createDirectDHTValue(StringUtils.toAsciiBytes("Olga"));
        database.store(value4);
        assertEquals(2, database.getKeyCount());
        assertEquals(4, database.getValueCount());
        assertTrue(Arrays.equals(StringUtils.toAsciiBytes("Olga"), 
                database.get(value4.getPrimaryKey())
                    .get(value4.getSecondaryKey()).getValue().getValue()));
    }
    
    public void testRemove() throws Exception {
        Database database = new DatabaseImpl();
        
        // Add a value
        DHTValueEntity value1 = createIndirectDHTValue(StringUtils.toAsciiBytes("Hello World"));
        database.store(value1);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals(StringUtils.toAsciiBytes("Hello World"), 
                database.get(value1.getPrimaryKey())
                    .get(value1.getSecondaryKey()).getValue().getValue()));
        
        // It's not possible to remove a value indirectly
        DHTValueEntity value2 = createIndirectDHTValue(value1.getSecondaryKey(), 
                KUID.createRandomID(), value1.getPrimaryKey(), new byte[0]);
        database.store(value2);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals(StringUtils.toAsciiBytes("Hello World"), 
                database.get(value2.getPrimaryKey())
                    .get(value2.getSecondaryKey()).getValue().getValue()));
        
        // But we can remove values directly
        DHTValueEntity value3 = createDirectDHTValue(value1.getSecondaryKey(), 
                value1.getPrimaryKey(), new byte[0]);
        database.store(value3);
        assertEquals(0, database.getKeyCount());
        assertEquals(0, database.getValueCount());
        
        // Add a new local value
        DHTValueEntity value4 = createLocalDHTValue(StringUtils.toAsciiBytes("Mojito"));
        database.store(value4);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals(StringUtils.toAsciiBytes("Mojito"), 
                database.get(value4.getPrimaryKey())
                    .get(value4.getSecondaryKey()).getValue().getValue()));
        
        // A local value cannot be removed
        DHTValueEntity value5 = createDirectDHTValue(value4.getSecondaryKey(), 
                value4.getPrimaryKey(), new byte[0]);
        database.store(value5);
        assertEquals(1, database.getKeyCount());
        assertEquals(1, database.getValueCount());
        assertTrue(Arrays.equals(StringUtils.toAsciiBytes("Mojito"), 
                database.get(value5.getPrimaryKey())
                    .get(value5.getSecondaryKey()).getValue().getValue()));
        
        // But a local value can remove a local value
        DHTValueEntity value6 = createLocalDHTValue(value4.getSecondaryKey(), 
                value4.getPrimaryKey(), new byte[0]);
        database.store(value6);
        assertEquals(0, database.getKeyCount());
        assertEquals(0, database.getValueCount());
    }
    
    /**
     * A Database is a Set of DHTValueBags. Each Bag is a Set of
     * DHTValueEntities.
     * 
     * If you iterate over the Bag and remove items from the Bag
     * you leak Bag references because they're not removed from
     * the Database
     * 
     * If you iterate over the Bag and remove items from the Database
     * you don't leak references but run into a ConcurrentModificationException.
     * 
     * Solution: Get a copy of the Bag and remove the items from the Database
     * 
     * Better solution: It'd be nice if the Bag is holding a reference to its
     * Database and cleanup itself once it's empty. There are however all kinds
     * of side effects if you start adding items straight to the Bag instead
     * of using the Database interface and so on.
     * 
     * This test represents the current implemenetation. If you implement the
     * "better solution" this test will fail!
     */
    public void testMultiRemove() {
        MojitoDHT dht = MojitoFactory.createDHT();
        
        DatabaseImpl database = (DatabaseImpl)dht.getDatabase();
        
        KUID key = KUID.createRandomID();
        
        Contact c1 = ContactFactory.createUnknownContact(ContextSettings.getVendor(), 
                ContextSettings.getVersion(), KUID.createRandomID(), 
                new InetSocketAddress("localhost", 1000));
        
        Contact c2 = ContactFactory.createUnknownContact(ContextSettings.getVendor(), 
                ContextSettings.getVersion(), KUID.createRandomID(), 
                new InetSocketAddress("localhost", 2000));
        
        DHTValue value = new DHTValueImpl(DHTValueType.BINARY, Version.ZERO, new byte[1]);
        DHTValueEntity entity1 = new DHTValueEntity(
                dht.getLocalNode(), dht.getLocalNode(), key, value, true);
        
        DHTValueEntity entity2 = new DHTValueEntity(c1, c1, key, value, false);
        DHTValueEntity entity3 = new DHTValueEntity(c2, c2, key, value, false);
        
        database.store(entity1);
        database.store(entity2);
        database.store(entity3);
        
        assertEquals(3, database.getValueCount());
        
        Map<KUID, DHTValueEntity> bag = database.get(key);
        assertNotNull(bag);
        assertEquals(3, bag.size());
       
        // Cannot Iterate over Bag and remove items from it
        // TODO This operation is maybe OK in future (check
        // the Database implementation).
        try {
            for (Iterator<?> it = bag.values().iterator(); it.hasNext(); ) {
                it.remove();
            }
            fail("Should have failed");
        } catch (UnsupportedOperationException expected) {
        }
        
        assertEquals(3, bag.size());
        
        for (DHTValueEntity e : bag.values()) {
            database.remove(e.getPrimaryKey(), e.getSecondaryKey());
        }
        
        assertEquals(0, database.getKeyCount());
        assertEquals(0, database.getValueCount());
        assertEquals(3, bag.size());
        
        // The Bag should be gone
        assertNull(database.getBag(key));
        assertTrue(database.get(key).isEmpty());
    }
    
    public void testMaxValuesPerAddress() {
        DatabaseSettings.LIMIT_VALUES_PER_ADDRESS.setValue(true);
        DatabaseSettings.MAX_VALUES_PER_ADDRESS.setValue(5);
        
        DatabaseSettings.LIMIT_VALUES_PER_NETWORK.setValue(false);
        
        Database db = new DatabaseImpl();
        
        // this should accept
        Contact badHost = ContactFactory.createLiveContact(
                new InetSocketAddress("192.168.1.1", 1111), 
                Vendor.UNKNOWN, Version.ZERO, KUID.createRandomID(), 
                new InetSocketAddress("192.168.1.1", 1111), 0, Contact.DEFAULT_FLAG);
        
        Contact goodHost = ContactFactory.createLiveContact(
                new InetSocketAddress("192.168.1.2", 1111), 
                Vendor.UNKNOWN, Version.ZERO, KUID.createRandomID(), 
                new InetSocketAddress("192.168.1.2", 1111), 0, Contact.DEFAULT_FLAG);
        
        DHTValueEntity value1 = new DHTValueEntity(badHost, badHost, KUID.createRandomID(), 
												   new DHTValueImpl(DHTValueType.TEST, Version.ZERO, StringUtils.toAsciiBytes("test")), false);
        
        DHTValueEntity value2 = new DHTValueEntity(goodHost, goodHost, KUID.createRandomID(), 
												   new DHTValueImpl(DHTValueType.TEST, Version.ZERO, StringUtils.toAsciiBytes("test")), false);
        
        // Store both values
        assertTrue(db.store(value1));
        assertTrue(db.store(value2));
        assertEquals(2, db.getValueCount());
        
        // The bad host fills up all its free slots
        int remaining = DatabaseSettings.MAX_VALUES_PER_ADDRESS.getValue() - 1;
        for (int i = 0; i < remaining; i++) {
            DHTValueEntity value3 = new DHTValueEntity(badHost, badHost, KUID.createRandomID(), 
													   new DHTValueImpl(DHTValueType.TEST, Version.ZERO, StringUtils.toAsciiBytes("test")), false);
            assertTrue(db.store(value3));
        }
        assertEquals(6, db.getValueCount());
        
        // The bad host cannot store values anymore
        DHTValueEntity value4 = new DHTValueEntity(badHost, badHost, KUID.createRandomID(), 
												   new DHTValueImpl(DHTValueType.TEST, Version.ZERO, StringUtils.toAsciiBytes("test")), false);
        assertFalse(db.store(value4));
        assertEquals(6, db.getValueCount());
        
        // The good host can
        DHTValueEntity value5 = new DHTValueEntity(goodHost, goodHost, KUID.createRandomID(), 
												   new DHTValueImpl(DHTValueType.TEST, Version.ZERO, StringUtils.toAsciiBytes("test")), false);
        assertTrue(db.store(value5));
        assertEquals(7, db.getValueCount());
        
        // Store forward a value from bad host? Cannot store!
        DHTValueEntity value6 = new DHTValueEntity(badHost, goodHost, KUID.createRandomID(), 
												   new DHTValueImpl(DHTValueType.TEST, Version.ZERO, StringUtils.toAsciiBytes("test")), false);
        assertFalse(db.store(value6));
        assertEquals(7, db.getValueCount());
        
        // Store forward from bad host? Should work!
        DHTValueEntity value7 = new DHTValueEntity(goodHost, badHost, KUID.createRandomID(), 
												   new DHTValueImpl(DHTValueType.TEST, Version.ZERO, StringUtils.toAsciiBytes("test")), false);
        assertTrue(db.store(value7));
        assertEquals(8, db.getValueCount());
        
        // Bad host but is a local value? Should work!
        DHTValueEntity value8 = new DHTValueEntity(badHost, badHost, KUID.createRandomID(), 
												   new DHTValueImpl(DHTValueType.TEST, Version.ZERO, StringUtils.toAsciiBytes("test")), true);
        assertTrue(db.store(value8));
        assertEquals(9, db.getValueCount());
    }
    
    public void testMaxValuesPerNetwork() {
        DatabaseSettings.LIMIT_VALUES_PER_ADDRESS.setValue(false);
        
        DatabaseSettings.LIMIT_VALUES_PER_NETWORK.setValue(true);
        DatabaseSettings.MAX_VALUES_PER_NETWORK.setValue(5);
        
        Database db = new DatabaseImpl();
        
        // Fill up all slots for the Class C Network
        for (int i = 0; i < DatabaseSettings.MAX_VALUES_PER_NETWORK.getValue(); i++) {
            Contact node1 = ContactFactory.createLiveContact(
                    new InetSocketAddress("192.168.1." + i, 1111+i), 
                    Vendor.UNKNOWN, Version.ZERO, KUID.createRandomID(), 
                    new InetSocketAddress("192.168.1." + i, 1111+i), 0, Contact.DEFAULT_FLAG);
            
            DHTValueEntity value1 = new DHTValueEntity(node1, node1, KUID.createRandomID(), 
													   new DHTValueImpl(DHTValueType.TEST, Version.ZERO, StringUtils.toAsciiBytes("test")), false);
            
            assertTrue(db.store(value1));
        }
        
        assertEquals(5, db.getValueCount());
        
        // Shouldn't be able to store more from the Class C Network!
        Contact node2 = ContactFactory.createLiveContact(
                new InetSocketAddress("192.168.1.50", 2050), 
                Vendor.UNKNOWN, Version.ZERO, KUID.createRandomID(), 
                new InetSocketAddress("192.168.1.50", 2050), 0, Contact.DEFAULT_FLAG);
        
        DHTValueEntity value2 = new DHTValueEntity(node2, node2, KUID.createRandomID(), 
												   new DHTValueImpl(DHTValueType.TEST, Version.ZERO, StringUtils.toAsciiBytes("test")), false);
        assertFalse(db.store(value2));
        assertEquals(5, db.getValueCount());
        
        // Different Class C Network, should work!
        Contact node3 = ContactFactory.createLiveContact(
                new InetSocketAddress("192.168.2.50", 2050), 
                Vendor.UNKNOWN, Version.ZERO, KUID.createRandomID(), 
                new InetSocketAddress("192.168.2.50", 2050), 0, Contact.DEFAULT_FLAG);
        
        DHTValueEntity value3 = new DHTValueEntity(node3, node3, KUID.createRandomID(), 
												   new DHTValueImpl(DHTValueType.TEST, Version.ZERO, StringUtils.toAsciiBytes("test")), false);
        assertTrue(db.store(value3));
        assertEquals(6, db.getValueCount());
        
        // Same Class C Network as Node #2 but the value is local. Should work!
        DHTValueEntity value4 = new DHTValueEntity(node2, node2, KUID.createRandomID(), 
												   new DHTValueImpl(DHTValueType.TEST, Version.ZERO, StringUtils.toAsciiBytes("test")), true);
        assertTrue(db.store(value4));
        assertEquals(7, db.getValueCount());
    }
    
    public void testIncrementRequestLoad() throws Exception{
        DatabaseImpl database = new DatabaseImpl();
        DHTValueEntity entity = createLocalDHTValue(StringUtils.toAsciiBytes("Hello World"));
        database.store(entity);
        
        KUID primaryKey = entity.getPrimaryKey();
        
        assertEquals(0f, database.getRequestLoad(primaryKey, true));
        
        //should start with larger than smoothing factor
        Thread.sleep(500);
        float load = database.getRequestLoad(primaryKey, true);
        assertGreaterThan(
                DatabaseSettings.VALUE_REQUEST_LOAD_SMOOTHING_FACTOR.getValue(), load);
        Thread.sleep(500);
        assertGreaterThan(load, database.getRequestLoad(primaryKey, true));
        
        //test a 0 smoothing factor
        DatabaseSettings.VALUE_REQUEST_LOAD_SMOOTHING_FACTOR.setValue(0);
        
        database.clear();
        database.store(entity);
        
        Thread.sleep(500);
        assertEquals(0F, database.getRequestLoad(primaryKey, true));
        
        //try a delay larger than nulling time
        database.clear();
        database.store(entity);
        
        DHTValueEntityBag bag = database.getBag(entity.getPrimaryKey());
        long now = System.currentTimeMillis();
        PrivilegedAccessor.setValue(bag, "lastRequestTime", 
                now - DatabaseSettings.VALUE_REQUEST_LOAD_NULLING_DELAY.getValue()*1000L);
        Thread.sleep(500);
        //load should be 0
        assertEquals(0, (int)bag.incrementRequestLoad());
        
        // try a very very large delay
        database.clear();
        database.store(entity);
        bag = database.getBag(entity.getPrimaryKey());
        
        PrivilegedAccessor.setValue(bag, "lastRequestTime", 1L);
        load = bag.incrementRequestLoad();
        //load should never get < 0
        assertGreaterThanOrEquals(0f, load);
        //now try a very very small delay
        bag.incrementRequestLoad();
        Thread.sleep(10);
        bag.incrementRequestLoad();
        Thread.sleep(10);
        bag.incrementRequestLoad();
        Thread.sleep(10);
        load = bag.incrementRequestLoad();
        Thread.sleep(10);
        load = bag.incrementRequestLoad();
        Thread.sleep(10);
        load = bag.incrementRequestLoad();
        //should now have increased (a lot!)
        assertGreaterThan(1f, load);
        //but never be larger than 1/0.01
        assertLessThan(1f/0.01f, load);
    }
    
    public void testReplaceFirewalledValue() throws InterruptedException {
        DatabaseSettings.LIMIT_VALUES_PER_ADDRESS.setValue(false);
        DatabaseSettings.LIMIT_VALUES_PER_NETWORK.setValue(false);
       
        final int MAX_VALUES_PER_KEY = 5;
        DatabaseSettings.MAX_VALUES_PER_KEY.setValue(MAX_VALUES_PER_KEY);
        
        DatabaseImpl database = new DatabaseImpl();
        
        List<DHTValueEntity> values = new ArrayList<DHTValueEntity>();
        
        KUID primaryKey = KUID.createRandomID();
        
        // Create a bunch of firewalled values and add them to the Database
        for (int i = 0; i < 2*MAX_VALUES_PER_KEY; i++) {
            SocketAddress addr = new InetSocketAddress(6666);
            Contact node = ContactFactory.createLiveContact(
                    addr, 
                    Vendor.UNKNOWN, 
                    Version.ZERO, 
                    KUID.createRandomID(), 
                    addr, 0, 
                    Contact.FIREWALLED_FLAG);
            
            assertTrue(node.isFirewalled());
            
            DHTValueEntity entity = new DHTValueEntity(node, node, primaryKey, 
													   new DHTValueImpl(DHTValueType.TEST, Version.ZERO, StringUtils.toAsciiBytes("Hello World")), false);
            
            database.store(entity);
            values.add(entity);
            
            // Make sure the creation times are different
            Thread.sleep(100);
        }
        
        // Check initial State
        assertEquals(1, database.getKeyCount());
        assertEquals(MAX_VALUES_PER_KEY, database.getValueCount());
        
        // The first five values should be in the Database
        for (DHTValueEntity entity : values.subList(0, MAX_VALUES_PER_KEY)) {
            assertTrue(database.contains(entity.getPrimaryKey(), entity.getSecondaryKey()));
        }
        
        // And the last five shouldn't!
        for (DHTValueEntity entity : values.subList(MAX_VALUES_PER_KEY, values.size())) {
            assertFalse(database.contains(entity.getPrimaryKey(), entity.getSecondaryKey()));
        }
        
        // Create a non-firewalled value
        SocketAddress addr = new InetSocketAddress(6666);
        Contact node = ContactFactory.createLiveContact(
                addr, 
                Vendor.UNKNOWN, 
                Version.ZERO, 
                KUID.createRandomID(), 
                addr, 0, 
                Contact.DEFAULT_FLAG);
        
        assertFalse(node.isFirewalled());
        
        DHTValueEntity notFirewalled = new DHTValueEntity(node, node, primaryKey, 
														  new DHTValueImpl(DHTValueType.TEST, Version.ZERO, StringUtils.toAsciiBytes("Hello World")), false);
        
        // Store it in the Database
        database.store(notFirewalled);
        values.add(notFirewalled);
        
        // It should be there
        assertTrue(database.contains(notFirewalled.getPrimaryKey(), notFirewalled.getSecondaryKey()));
        
        // The oldest firewalled value should be done
        DHTValueEntity oldest = values.get(0);
        assertFalse(database.contains(oldest.getPrimaryKey(), oldest.getSecondaryKey()));
        
        // And the remaining four firewalled values should be still there
        for (DHTValueEntity firewalled : values.subList(1, MAX_VALUES_PER_KEY)) {
            assertTrue(database.contains(firewalled.getPrimaryKey(), firewalled.getSecondaryKey()));
        }
        
        // Create more non-firewalled values
        for (int i = 0; i < 2*MAX_VALUES_PER_KEY; i++) {
            addr = new InetSocketAddress(6666);
             node = ContactFactory.createLiveContact(
                    addr, 
                    Vendor.UNKNOWN, 
                    Version.ZERO, 
                    KUID.createRandomID(), 
                    addr, 0, 
                    Contact.DEFAULT_FLAG);
            
            assertFalse(node.isFirewalled());
            
            DHTValueEntity entity = new DHTValueEntity(node, node, primaryKey, 
													   new DHTValueImpl(DHTValueType.TEST, Version.ZERO, StringUtils.toAsciiBytes("Hello World")), false);
            
            database.store(entity);
            values.add(entity);
            
            // Make sure the creation times are different
            Thread.sleep(100); 
        }
        
        // Check state
        assertEquals(1, database.getKeyCount());
        assertEquals(MAX_VALUES_PER_KEY, database.getValueCount());
        
        
        // The firewalled values should be all gone
        int fromIndex = 0;
        int toIndex = values.size() - (2*MAX_VALUES_PER_KEY);
        for (DHTValueEntity entity : values.subList(fromIndex, toIndex-1)) {
            assertTrue(entity.getCreator().isFirewalled());
            assertFalse(database.contains(entity.getPrimaryKey(), entity.getSecondaryKey()));
        }
        
        // The first five non-firewalled values should be there
        fromIndex = toIndex;
        toIndex = fromIndex + MAX_VALUES_PER_KEY;
        for (DHTValueEntity entity : values.subList(fromIndex, toIndex-1)) {
            assertFalse(entity.getCreator().isFirewalled());
            assertTrue(database.contains(entity.getPrimaryKey(), entity.getSecondaryKey()));
        }
        
        // The last five non-firewalled shouldn't be there
        fromIndex = toIndex;
        toIndex = values.size();
        for (DHTValueEntity entity : values.subList(fromIndex, toIndex)) {
            assertFalse(entity.getCreator().isFirewalled());
            assertFalse(database.contains(entity.getPrimaryKey(), entity.getSecondaryKey()));
        }
    }
}
