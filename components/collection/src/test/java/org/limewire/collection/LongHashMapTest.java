package org.limewire.collection;


import junit.framework.Test;

import org.limewire.util.BaseTestCase;

@SuppressWarnings("unchecked")
public class LongHashMapTest extends BaseTestCase {

    public LongHashMapTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public static Test suite() {
        return buildTestSuite(LongHashMapTest.class);
    }
    
    public void testMap() {
        LongHashMap map = new LongHashMap();
        for(long i = -100; i < 100; i++)
            map.put(i, new Long(i));
        for(long i = -100; i < 100; i++) {
            assertTrue(map.containsKey(i));
            assertEquals(new Long(i), map.get(i));
            assertEquals(new Long(i), map.remove(i));
        }
        assertEquals(0, map.size());
    }
    
    public void testDowncast() {
        LongHashMap map = new LongHashMap();
        
        for(long i = Integer.MIN_VALUE; i > Integer.MIN_VALUE - 1000; i--) {
            map.put(i, new Long(i));
            map.put((int)i, new Long((int)i));
        }
        
        for(long i = Integer.MAX_VALUE; i < Integer.MAX_VALUE + 1000; i++) {
            map.put(i, new Long(i));
            map.put((int)i, new Long((int)i));
        }
        
        for(long i = Integer.MIN_VALUE; i > Integer.MIN_VALUE - 1000; i--) {
            assertTrue(map.containsKey(i));
            assertTrue(map.containsKey((int)i));
            assertNotEquals(i, ((int)i));
            assertEquals(new Long(i), map.get(i));
            assertEquals(new Long((int)i), map.get((int)i));
            assertNotEquals(new Long(i), map.get((int)i));
            assertNotEquals(new Long((int)i), map.get(i));
            assertEquals(new Long(i), map.remove(i));
            assertEquals(new Long((int)i), map.remove((int)i));
        }
        
        for(long i = Integer.MAX_VALUE; i < Integer.MAX_VALUE + 1000; i++) {
            assertTrue(map.containsKey(i));
            assertTrue(map.containsKey((int)i));
            assertNotEquals(i, ((int)i));
            assertEquals(new Long(i), map.get(i));
            assertEquals(new Long((int)i), map.get((int)i));
            assertNotEquals(new Long(i), map.get((int)i));
            assertNotEquals(new Long((int)i), map.get(i));
            assertEquals(new Long(i), map.remove(i));
            assertEquals(new Long((int)i), map.remove((int)i));
        }
        
        assertEquals(0, map.size());
    }
}
