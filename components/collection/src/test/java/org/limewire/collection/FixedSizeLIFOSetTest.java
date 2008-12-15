package org.limewire.collection;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import junit.framework.Test;

import org.limewire.collection.FixedSizeLIFOSet.EjectionPolicy;
import org.limewire.util.BaseTestCase;

public class FixedSizeLIFOSetTest extends BaseTestCase {

    public FixedSizeLIFOSetTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(FixedSizeLIFOSetTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    public void testFixedSizeLIFOSetLifo(){
        String test = "test";
        String test2 = "test2";
        String test3 = "test3";
        String test4 = "test4";
        String test5 = "test5";
        String test6 = "test6";
        //try a 0 size
        Set<String> lifo = new FixedSizeLIFOSet<String>(0, EjectionPolicy.LIFO);
        lifo.add(test);
        assertFalse(lifo.contains(test));
        
        //try going over capacity
        lifo = new FixedSizeLIFOSet<String>(3, EjectionPolicy.LIFO);
        lifo.add(test);
        assertTrue(lifo.contains(test));
        lifo.add(test2);
        assertTrue(lifo.contains(test2));
        lifo.add(test3);
        assertTrue(lifo.contains(test3));
        lifo.add(test4);
        assertTrue(lifo.contains(test4));

        //contains 4, 2, 1
        //should have kicked out the last one
        assertTrue(lifo.contains(test));
        assertFalse(lifo.contains(test3));
        assertTrue(lifo.contains(test));
        assertEquals(test4, lifo.iterator().next());

        lifo.add(test5);
        //contains 5, 2, 1
        assertTrue(lifo.contains(test5));
        assertTrue(lifo.contains(test2));
        assertTrue(lifo.contains(test));
        assertFalse(lifo.contains(test4));

        
        lifo.add(test6); //
        //contains 6,2,1
        assertTrue(lifo.contains(test6));
        assertFalse(lifo.contains(test3));
        assertTrue(lifo.contains(test2));
        assertTrue(lifo.contains(test));
        
        //try re-adding
        lifo = new FixedSizeLIFOSet<String>(3, EjectionPolicy.LIFO);
        lifo.add(test);
        lifo.add(test2);
        lifo.add(test);        
        //contains 1, 2
        assertEquals(2, lifo.size());
        //contains 3, 2, 1
        lifo.add(test3);
        lifo.add(test);
        assertTrue(lifo.contains(test));
        assertTrue(lifo.contains(test2));
        assertTrue(lifo.contains(test3));
        
        //try add-remove
        lifo = new FixedSizeLIFOSet<String>(3, EjectionPolicy.LIFO);
        lifo.add(test);
        lifo.add(test2);
        lifo.add(test3);
        lifo.remove(test2);
        lifo.add(test4);
        //contains 4,3,1
        assertTrue(lifo.contains(test));
        assertFalse(lifo.contains(test2));
        assertTrue(lifo.contains(test3));
        assertTrue(lifo.contains(test4));
        
        //try adding multiple
        lifo = new FixedSizeLIFOSet<String>(3, EjectionPolicy.LIFO);
        lifo.add(test);
        lifo.add(test2);
        lifo.add(test3);
        List<String> l = Arrays.asList(test,test2,test3,test4);
        lifo.addAll(l);
        //contains 4,2,1
        assertTrue(lifo.contains(test));
        assertTrue(lifo.contains(test2));
        assertFalse(lifo.contains(test3));
        assertTrue(lifo.contains(test4));
        
    }
    public void testFixedSizeLIFOSet() {
        String test = "test";
        String test2 = "test2";
        String test3 = "test3";
        String test4 = "test4";
        String test5 = "test5";
        String test6 = "test6";
        //try a 0 size
        Set<String> lifo = new FixedSizeLIFOSet<String>(0, EjectionPolicy.FIFO);
        lifo.add(test);
        assertFalse(lifo.contains(test));
        
        //try going over capacity
        lifo = new FixedSizeLIFOSet<String>(3, EjectionPolicy.FIFO);
        lifo.add(test);
        assertTrue(lifo.contains(test));
        lifo.add(test2);
        assertTrue(lifo.contains(test2));
        lifo.add(test3);
        assertTrue(lifo.contains(test3));
        lifo.add(test4);
        assertTrue(lifo.contains(test4));
        //should have kicked out the first one
        assertFalse(lifo.contains(test));
        assertEquals(test4, lifo.iterator().next());
        lifo.add(test5);
        assertTrue(lifo.contains(test5));
        lifo.add(test6);
        assertTrue(lifo.contains(test6));
        assertFalse(lifo.contains(test3));
        assertFalse(lifo.contains(test2));
        
        //try re-adding
        lifo = new FixedSizeLIFOSet<String>(3, EjectionPolicy.FIFO);
        lifo.add(test);
        lifo.add(test2);
        lifo.add(test);
        assertEquals(2, lifo.size());
        lifo.add(test3);
        lifo.add(test);
        assertTrue(lifo.contains(test));
        assertTrue(lifo.contains(test2));
        assertTrue(lifo.contains(test3));
        
        //try add-remove
        lifo = new FixedSizeLIFOSet<String>(3, EjectionPolicy.FIFO);
        lifo.add(test);
        lifo.add(test2);
        lifo.add(test3);
        lifo.remove(test2);
        lifo.add(test4);
        assertTrue(lifo.contains(test));
        assertFalse(lifo.contains(test2));
        assertTrue(lifo.contains(test3));
        assertTrue(lifo.contains(test4));
        
        //try adding multiple
        lifo = new FixedSizeLIFOSet<String>(3, EjectionPolicy.FIFO);
        lifo.add(test);
        lifo.add(test2);
        lifo.add(test3);
        List<String> l = Arrays.asList(test,test2,test3,test4);
        lifo.addAll(l);
        assertFalse(lifo.contains(test));
        assertTrue(lifo.contains(test2));
        assertTrue(lifo.contains(test3));
        assertTrue(lifo.contains(test4));
    }
}
