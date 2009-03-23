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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.Test;

import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.MojitoTestCase;
import org.limewire.mojito.db.impl.DHTValueImpl;
import org.limewire.mojito.result.StoreResult;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.settings.DatabaseSettings;
import org.limewire.mojito.settings.KademliaSettings;
import org.limewire.mojito.util.DatabaseUtils;
import org.limewire.mojito.util.MojitoUtils;
import org.limewire.util.PrivilegedAccessor;
import org.limewire.util.StringUtils;

public class StorableTest extends MojitoTestCase {

    public StorableTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(StorableTest.class);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setLocalIsPrivate(false);
    }

    public void testStorableModel() throws Exception {
        int k = KademliaSettings.REPLICATION_PARAMETER.getValue();
        
        PrivilegedAccessor.setValue(DatabaseSettings.STORABLE_PUBLISHER_PERIOD, "value", new Long(100));
        
        Map<KUID, MojitoDHT> dhts = Collections.emptyMap();
        try {
            dhts = MojitoUtils.createBootStrappedDHTsMap(2);
            Thread.sleep(250);
            
            KUID key = KUID.createRandomID();
            DHTValueType type = DHTValueType.TEST;
            Version version = Version.ZERO;
            byte[] b = StringUtils.toAsciiBytes("Hello World");
            
            long time = System.currentTimeMillis();
            
            final Object lock1 = new Object();
            final Object lock2 = new Object();
            
            final Storable storable = new Storable(key, new DHTValueImpl(type, version, b));
            final AtomicBoolean publisherDidRun = new AtomicBoolean(false);
            
            dhts.values().iterator().next().getStorableModelManager().addStorableModel(type, new StorableModel() {
                public Collection<Storable> getStorables() {
                    synchronized (lock1) {
                        try {
                            if (!publisherDidRun.get()) {
                                publisherDidRun.set(true);
                                return Collections.singleton(storable);
                            } else {
                                return Collections.emptySet();
                            }
                        } finally {
                            lock1.notifyAll();
                        }
                    }
                }
                
                public void handleStoreResult(Storable value, StoreResult result) {
                    synchronized (lock2) {
                        lock2.notifyAll();
                    }
                }
                
                public void handleContactChange() {
                }
            });
            
            synchronized (lock1) {
                // Wait for getStorables() call
                lock1.wait(1000L);
            }
            
            assertTrue(publisherDidRun.get());
            
            synchronized (lock2) {
                // Wait for handleStoreResult()
                lock2.wait(1000L);
            }
            
            assertEquals(k, storable.getLocationCount());
            assertGreaterThanOrEquals(time, storable.getPublishTime());
            assertFalse(DatabaseUtils.isPublishingRequired(storable));
            
        } finally {
            for (MojitoDHT dht : dhts.values()) {
                dht.close();
            }
        }
    }
}
