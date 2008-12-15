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

import java.util.Collections;
import java.util.Map;

import junit.framework.Test;

import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.MojitoTestCase;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.DHTFutureAdapter;
import org.limewire.mojito.db.impl.DHTValueImpl;
import org.limewire.mojito.result.StoreResult;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.settings.KademliaSettings;
import org.limewire.mojito.util.DatabaseUtils;
import org.limewire.mojito.util.MojitoUtils;

public class DHTValueTest extends MojitoTestCase {
    
    /*static {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
    }*/

    public DHTValueTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(DHTValueTest.class);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setLocalIsPrivate(false);
    }

    public void testLocationCount() throws Exception {
        int k = KademliaSettings.REPLICATION_PARAMETER.getValue();
        
        Map<KUID, MojitoDHT> dhts = Collections.emptyMap();
        try {
            dhts = MojitoUtils.createBootStrappedDHTsMap(2);
            Thread.sleep(250);
            
            KUID key = KUID.createRandomID();
            DHTValueType type = DHTValueType.TEST;
            Version version = Version.ZERO;
            byte[] b = "Hello World".getBytes();
            
            long time = System.currentTimeMillis();
            
            Context context = (Context)dhts.values().iterator().next();
            
            final Object lock = new Object();
            final Storable storable = new Storable(key, new DHTValueImpl(type, version, b));
            
            // Pre-Condition
            assertEquals(0, storable.getLocationCount());
            assertEquals(0L, storable.getPublishTime());
            assertTrue(DatabaseUtils.isPublishingRequired(storable));
            
            // Store...
            DHTFuture<StoreResult> future = context.store(storable);
            future.addDHTFutureListener(new DHTFutureAdapter<StoreResult>() {
                @Override
                public void handleFutureSuccess(StoreResult result) {
                    storable.handleStoreResult(result);
                    synchronized (lock) {
                        lock.notifyAll();
                    }
                }
            });
            
            future.get();
            synchronized (lock) {
                if (storable.getLocationCount() == 0L) {
                    lock.wait(1000L);
                }
            }
            
            // Post-Condition
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
