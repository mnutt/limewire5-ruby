package org.limewire.collection;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

public class LIFOSetTest extends BaseTestCase {

    public LIFOSetTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(LIFOSetTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testLIFOSet() {
        String test = "test";
        String test2 = "test2";
        String test3 = "test3";
        String test4 = "test4";
        
        LIFOSet<String> lifo = new LIFOSet<String>();
        assertEquals(lifo.size(),0);
        lifo.add(test);
        assertTrue(lifo.contains(test));
        //try readding
        assertFalse(lifo.add(test));
        lifo.add(test2);
        assertTrue(lifo.contains(test2));
        lifo.add(test3);
        assertTrue(lifo.contains(test3));
        //try adding null
        assertTrue(lifo.add(null));
        assertFalse(lifo.add(null));
        assertTrue(lifo.contains(null));
        lifo.add(test4);
        assertTrue(lifo.contains(test4));
        //test the iterator
        Iterator<String> it = lifo.iterator();
        assertEquals(test4, it.next());
        assertNull(it.next());
        assertEquals(test3, it.next());
        assertEquals(test2, it.next());
        assertEquals(test, it.next());
        try {
            assertFalse(it.hasNext());
            it.next();
            assertTrue("should have thrown NoSuchElementException", false);
        }catch(NoSuchElementException ex) {}
        //try removing with iterator
        it = lifo.iterator();
        it.next();
        it.remove();
        assertFalse(lifo.contains(test4));
        
        //try add all & remove all
        lifo = new LIFOSet<String>();
        List<String> l = Arrays.asList(test,test2,test3);
        lifo.addAll(l);
        assertTrue(lifo.contains(test));
        assertTrue(lifo.contains(test2));
        assertTrue(lifo.contains(test3));
        assertTrue(lifo.containsAll(l));
        l = Arrays.asList(test,test3);
        lifo.removeAll(l);
        assertFalse(lifo.contains(test));
        assertTrue(lifo.contains(test2));
        assertFalse(lifo.contains(test3));
        assertFalse(lifo.containsAll(l));
        //test clear
        lifo.clear();
        assertTrue(lifo.isEmpty());
        assertFalse(lifo.iterator().hasNext());
        
        //test remove first (eldest) element from the Set
        lifo = new LIFOSet<String>();
        lifo.add(test);
        lifo.add(test2);
        lifo.removeEldest();
        assertTrue(lifo.contains(test2));
        assertFalse(lifo.contains(test));
        
        //test remove last (newest) element from the Set
        lifo = new LIFOSet<String>();
        lifo.add(test);
        lifo.add(test2);
        lifo.removeNewest();
        assertFalse(lifo.contains(test2));
        assertTrue(lifo.contains(test));
        
        //test toArray
        lifo = new LIFOSet<String>();
        lifo.add(test);
        lifo.add(test4);
        String[] s = lifo.toArray(new String[lifo.size()]);
        assertEquals(s[0], test4);
        assertEquals(s[1], test);
    }
}
