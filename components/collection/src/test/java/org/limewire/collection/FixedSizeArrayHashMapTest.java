package org.limewire.collection;

import java.util.LinkedHashMap;
import java.util.Map;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

@SuppressWarnings("unchecked")
public class FixedSizeArrayHashMapTest extends BaseTestCase {

    public FixedSizeArrayHashMapTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(FixedSizeArrayHashMapTest.class);
    }
    
    /**
     * tests that adding a new element ejects the oldest one.
     */
    public void testFixedSize() throws Exception {
        Map m = new FixedSizeArrayHashMap(2);
        m.put(1, 11); m.put(2, 22); m.put(3, 33);
        assertEquals(2, m.size());
        String out = "";
        for (Object o : m.entrySet())
            out += o + ", ";
        assertEquals("3=33, 2=22, ",out);
        assertEquals("{3=33, 2=22}",m.toString());
        assertTrue(m.containsKey(2));
        assertTrue(m.containsKey(3));
        assertFalse(m.containsKey(1));
        assertTrue(m.containsValue(22));
        assertTrue(m.containsValue(33));
        assertFalse(m.containsValue(11));
        
        out = "";
        for(Object o : m.keySet())
            out += o + ", ";
        assertEquals("3, 2, ", out);
        
        out = "";
        for(Object o : m.values())
            out += o + ", ";
        assertEquals("33, 22, ", out);
    }
    
    public void testFixedSizeWithExistingValue() throws Exception {
        int size = 200;
        Map<Integer, Integer> m = new FixedSizeArrayHashMap<Integer, Integer>(size);
        for (int i = 1; i <= size + 2; i++) {
            m.put(i, i);
            assertLessThanOrEquals(size, m.size());
        }
        for (int i = 1; i <= size + 2; i++) {
            m.put(i, i);
            assertLessThanOrEquals(size, m.size());
            m.put(i, i);
        }
    }

    /**
     * tests that re-adding an existing element will postpone
     * its ejection turn.
     */
    public void testRefresh() throws Exception {
        Map m = new FixedSizeArrayHashMap(3);
        m.put(1, 11); m.put(2, 22); m.put(3, 33); m.put(1, 11);
        assertEquals(3, m.size());
        String out = "";
        for (Object o : m.entrySet())
            out += o + ", ";
        assertEquals("1=11, 3=33, 2=22, ", out);
        
        m.put(4, 44);
        assertFalse(m.containsKey(2));
        assertTrue(m.containsKey(1));
        out = "";
        for (Object o : m.entrySet())
            out += o + ", ";
        assertEquals("4=44, 1=11, 3=33, ", out);
    }
    
    public void testRemove() throws Exception {
        RandomAccessMap<Integer, Integer> m = new FixedSizeArrayHashMap<Integer, Integer>(3);
        m.put(1, 11); m.put(2, 22); m.put(3, 33);
        assertTrue(m.containsKey(2));
        assertEquals(3, (int) m.getKeyAt(0));
        assertEquals(2, (int) m.getKeyAt(1));
        assertEquals(1, (int) m.getKeyAt(2));
        assertEquals(33, (int) m.getValueAt(0));
        assertEquals(22, (int) m.getValueAt(1));
        assertEquals(11, (int) m.getValueAt(2));
        assertEquals(22, (int) m.remove(2));
        assertEquals(3, (int) m.getKeyAt(0));
        assertEquals(1, (int) m.getKeyAt(1));
        try {
            m.getKeyAt(2);
            fail("expected exception");
        } catch(IndexOutOfBoundsException expected) {}
        assertEquals(33, (int) m.getValueAt(0));
        assertEquals(11, (int) m.getValueAt(1));
        try {
            m.getValueAt(2);
            fail("expected exception");
        } catch(IndexOutOfBoundsException expected) {}
        assertFalse(m.containsKey(2));
        assertEquals(2, m.size());
        assertNull(m.remove(4));
        assertEquals(2, m.size());
        
        assertEquals(new Integer(11), m.remove(1));
        assertEquals(1, m.size());
        assertEquals(3, (int) m.getKeyAt(0));
        assertEquals(33, (int) m.getValueAt(0));
        try {
            m.getKeyAt(1);
            fail("expected exception");
        } catch(IndexOutOfBoundsException expected) {}
        try {
            m.getValueAt(1);
            fail("expected exception");
        } catch(IndexOutOfBoundsException expected) {}
    }
    
    /**
     * tests that creating from a collection that's larger than
     * the capacity will happen in fifo order.
     */
    public void testCreateFromCollection() throws Exception {
        Map m1 = new LinkedHashMap();
        m1.put(1, 11); m1.put(2, 22); m1.put(3, 33);
        Map m = new FixedSizeArrayHashMap(2, m1);
        assertEquals(2, m.size());
        assertTrue(m.containsKey(2));
        assertTrue(m.containsKey(3));
        assertFalse(m.containsKey(1));
    }
    
    public void testIndexing() throws Exception {
        FixedSizeArrayHashMap<Integer, Integer> m = new FixedSizeArrayHashMap<Integer, Integer>(3);
        m.put(1, 11); m.put(2, 22); m.put(3, 33);
        assertEquals(3, (int) m.getKeyAt(0));
        assertEquals(2, (int) m.getKeyAt(1));
        assertEquals(1, (int) m.getKeyAt(2));
        m.put(4, 44);
        assertEquals(4, (int) m.getKeyAt(0));
        assertEquals(3, (int) m.getKeyAt(1));
        assertEquals(2, (int) m.getKeyAt(2));
    }
    
    public void testClone() throws Exception {
        FixedSizeArrayHashMap m = new FixedSizeArrayHashMap(3);
        m.put(1, 11); m.put(2, 22); m.put(3, 33);
        Map m2 = (Map) m.clone();
        assertEquals(m.size(), m2.size());
        assertTrue(m2.keySet().containsAll(m.keySet()));
    }
}
