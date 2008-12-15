package org.limewire.collection;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

public class IntHashMapTest extends BaseTestCase {

    public IntHashMapTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(IntHashMapTest.class);
    }

    @SuppressWarnings("unchecked")
    public void testSerialization() {
        IntHashMap map1 = new IntHashMap();
        map1.put(1, "Hello World");
        map1.put(4000, null);
        map1.put(5000, "LimeWire");
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(map1);
            oos.close();
        } catch (IOException err) {
            fail(err);
        }
        
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        try {
            ObjectInputStream ois = new ObjectInputStream(bais);
            IntHashMap map2 = (IntHashMap)ois.readObject();
            ois.close();
            
            assertEquals(3, map2.size());
            assertEquals(map1.get(1), map2.get(1));
            
            assertEquals(map1.get(4000), map2.get(4000));
            assertNull(map2.get(4000));
            
            assertEquals(map2.get(5000), map2.get(5000));
            assertEquals(map2.get(9000), map2.get(9000));
            
        } catch (ClassNotFoundException err) {
            fail(err);
        } catch (IOException err) {
            fail(err);
        }
    }
}
