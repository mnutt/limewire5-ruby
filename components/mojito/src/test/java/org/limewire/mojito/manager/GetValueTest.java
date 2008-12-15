package org.limewire.mojito.manager;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import junit.framework.TestSuite;

import org.limewire.mojito.EntityKey;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.MojitoTestCase;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.db.impl.DHTValueImpl;
import org.limewire.mojito.result.FindValueResult;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.util.MojitoUtils;

public class GetValueTest extends MojitoTestCase {
    
    public GetValueTest(String name){
        super(name);
    }
    
    public static TestSuite suite() {
        return buildTestSuite(GetValueTest.class);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setLocalIsPrivate(false);
    }
    
    public void testGetValueByType() throws Exception {
        List<MojitoDHT> dhts = Collections.emptyList();
        try {        
            dhts = MojitoUtils.createBootStrappedDHTs(1);
            MojitoDHT first = dhts.get(0);
            Thread.sleep(250);
            
            KUID valueId = KUID.createRandomID();
            DHTValue value = new DHTValueImpl(
                    DHTValueType.TEXT, Version.ZERO, "Hello World".getBytes());
            first.put(valueId, value).get();
            
            try {
                EntityKey lookupKey = EntityKey.createEntityKey(valueId, DHTValueType.TEXT);
                FindValueResult result = dhts.get(1).get(lookupKey).get();
                Collection<? extends DHTValueEntity> entities = result.getEntities();
                assertEquals(1, entities.size());
                for (DHTValueEntity entity : entities) {
                    assertEquals(value, entity.getValue());
                }
            } catch (Exception err) {
                fail(err);
            }
            
            try {
                EntityKey lookupKey = EntityKey.createEntityKey(valueId, DHTValueType.ANY);
                FindValueResult result = dhts.get(1).get(lookupKey).get();
                Collection<? extends DHTValueEntity> entities = result.getEntities();
                assertEquals(1, entities.size());
                for (DHTValueEntity entity : entities) {
                    assertEquals(value, entity.getValue());
                }
            } catch (Exception err) {
                fail(err);
            }
            
            try {
                EntityKey lookupKey = EntityKey.createEntityKey(valueId, DHTValueType.LIME);
                FindValueResult result = dhts.get(1).get(lookupKey).get();
                Collection<? extends DHTValueEntity> entities = result.getEntities();
                assertEquals("Got " + entities, 0, entities.size());
            } catch (Exception err) {
                fail(err);
            }
            
        } finally {
            for (MojitoDHT dht : dhts) {
                dht.close();
            }
        }
    }
    
    public void testNotSameReference() throws Exception {
        List<MojitoDHT> dhts = Collections.emptyList();
        try {
            dhts = MojitoUtils.createBootStrappedDHTs(1);
            MojitoDHT first = dhts.get(0);
            Thread.sleep(250);
            
            KUID valueId = KUID.createRandomID();
            DHTValue value = new DHTValueImpl(
                    DHTValueType.TEXT, Version.ZERO, "Hello World".getBytes());
            first.put(valueId, value).get();
            
            EntityKey lookupKey1 = EntityKey.createEntityKey(valueId, DHTValueType.TEXT);
            FindValueResult result1 = dhts.get(1).get(lookupKey1).get();
            
            EntityKey lookupKey2 = EntityKey.createEntityKey(valueId, DHTValueType.ANY);
            FindValueResult result2 = dhts.get(1).get(lookupKey2).get();
            
            assertNotSame(result1, result2);
            
        } finally {
            for (MojitoDHT dht : dhts) {
                dht.close();
            }
        }
    }
}
