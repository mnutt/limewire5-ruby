package org.limewire.collection;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

@SuppressWarnings("unchecked")
public class FixedSizeArrayHashSetTest extends BaseTestCase {

    public FixedSizeArrayHashSetTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(FixedSizeArrayHashSetTest.class);
    }
    
    /**
     * tests that adding a new element ejects the oldest one.
     */
    public void testFixedSize() throws Exception {
        Set s = new FixedSizeArrayHashSet(2);
        s.add(1);s.add(2);s.add(3);
        assertEquals(2, s.size());
        String out = "";
        for (Object o : s)
            out = out + o;
        assertEquals("32",out);
    }

    /**
     * tests that re-adding an existing element will postpone
     * its ejection turn.
     */
    public void testRefresh() throws Exception {
        Set s = new FixedSizeArrayHashSet(3);
        s.add(1);s.add(2);s.add(3);s.add(1);
        assertEquals(3, s.size());
        String out = "";
        for (Object o : s)
            out = out + o;
        assertEquals("132",out);
        s.add(4);
        assertFalse(s.contains(2));
        assertTrue(s.contains(1));
    }
    
    public void testRemove() throws Exception {
        Set s = new FixedSizeArrayHashSet(3);
        s.add(1);s.add(2);s.add(3);
        assertTrue(s.contains(2));
        s.remove(2);
        assertFalse(s.contains(2));
        assertEquals(2, s.size());
        s.remove(4);
        assertEquals(2, s.size());
    }

    /**
     * tests that creating from a collection that's larger than
     * the capacity will happen in fifo order.
     */
    public void testCreateFromCollection() throws Exception {
        List l = new ArrayList();
        l.add(1); l.add(2); l.add(3);
        Set s = new FixedSizeArrayHashSet(2, l);
        assertEquals(2, s.size());
        assertTrue(s.contains(2));
        assertTrue(s.contains(3));
        assertFalse(s.contains(1));
    }
    
    public void testIndexing() throws Exception {
        FixedSizeArrayHashSet<Integer> s = new FixedSizeArrayHashSet<Integer>(3);
        s.add(1);s.add(2);s.add(3);
        assertEquals(new Integer(3),s.get(0));
        assertEquals(new Integer(2),s.get(1));
        assertEquals(new Integer(1),s.get(2));
    }
    
    public void testClone() throws Exception {
        FixedSizeArrayHashSet s = new FixedSizeArrayHashSet(3);
        s.add(1);s.add(2);s.add(3);
        Set s2 = (Set) s.clone();
        assertEquals(s.size(), s2.size());
        assertTrue(s2.containsAll(s));
        assertFalse(s2.retainAll(s));
    }
}
