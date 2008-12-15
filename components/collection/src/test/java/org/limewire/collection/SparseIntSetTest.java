package org.limewire.collection;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

public class SparseIntSetTest extends BaseTestCase {

    public SparseIntSetTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(SparseIntSetTest.class);
    }
    
    public void testAdd() throws Exception {
        Set<Integer> sparseSet = new SparseIntSet();
        assertTrue(sparseSet.add(5));
        assertFalse(sparseSet.add(5));
        assertTrue(sparseSet.contains(5));
        assertEquals(1,sparseSet.size());
    }
    
    public void testAddAll() throws Exception {
        Set<Integer> treeSet = new TreeSet<Integer>();
        SparseIntSet sparseSet = new SparseIntSet();
        
        // test addAll which doesn't modify any elements
        for (int i = 0; i < 1000; i++) {
            int random = (int)(Math.random() * 1000);
            treeSet.add(random);
            sparseSet.add(random);
        }
        int size = sparseSet.size();
        assertFalse(sparseSet.addAll(treeSet));
        assertEquals(size, sparseSet.size());
        
        treeSet.clear();
        sparseSet = new SparseIntSet();
        
        // test that addAll from a SortedSet is optimized
        // and will not allocate more memory than needed
        for (int i = 0; i < 10; i++)
            treeSet.add(i);
        sparseSet.addAll(treeSet);
        assertEquals(10, sparseSet.size());
        assertEquals(40, sparseSet.getActualMemoryUsed());
        sparseSet.compact();
        assertEquals(40, sparseSet.getActualMemoryUsed());
        assertTrue(sparseSet.containsAll(treeSet));
        
        
        
        // merge with 1 element extra
        treeSet.clear();
        sparseSet = new SparseIntSet();
        
        for (int i = 0; i < 10; i++) {
            if (i % 2 == 0)
                treeSet.add(i); //0, 2 .. 8
            else
                sparseSet.add(i); // 1, 3 .. 9
        }
        
        assertEquals(5, sparseSet.size());
        assertEquals(5, treeSet.size());
        assertTrue(sparseSet.addAll(treeSet));
        assertEquals(10, sparseSet.size());
        assertEquals(40, sparseSet.getActualMemoryUsed());
        
        // merge small with big
        treeSet.clear();
        sparseSet = new SparseIntSet();
        for (int i = 0; i < 10; i+=2) // 0, 2, 4.. 8
            sparseSet.add(i);
        for (int i = 5; i < 15; i++)  // 5, 6, 7.. 14
            treeSet.add(i);
        
        assertTrue(sparseSet.addAll(treeSet));
        assertEquals(13, sparseSet.size());
        assertTrue(sparseSet.containsAll(treeSet));
        assertTrue(treeSet.addAll(sparseSet));
        assertEquals(13, treeSet.size());
        assertTrue(treeSet.containsAll(sparseSet));
        
        // merge with empty
        treeSet.clear();
        sparseSet = new SparseIntSet();
        
        sparseSet.add(1);
        assertFalse(sparseSet.addAll(treeSet));
        assertTrue(sparseSet.contains(1));
        assertEquals(1, sparseSet.size());
        
        // merge with bigger gaps
        treeSet.clear();
        sparseSet = new SparseIntSet();
        for (int i = 0; i < 25; i+=5) // 0, 5, 10, 15, 20
            sparseSet.add(i);
        for (int i = 5; i < 15; i++)  // 5, 6, 7.. 14
            treeSet.add(i);
        
        assertTrue(sparseSet.addAll(treeSet));
        assertEquals(13, sparseSet.size());
        assertTrue(sparseSet.containsAll(treeSet));
        assertTrue(treeSet.addAll(sparseSet));
        assertEquals(13, treeSet.size());
        assertTrue(treeSet.containsAll(sparseSet));
    }
    
    
    public void testSortedProperty() throws Exception {
        Set<Integer> treeSet = new TreeSet<Integer>();
        Set<Integer> sparseSet = new SparseIntSet();
        
        for (int i = 0; i < 1000; i++) {
            int random = (int)(Math.random() * 1000);
            treeSet.add(random);
            sparseSet.add(random);
        }
        
        assertEquals(treeSet.size(),sparseSet.size());
        assertTrue(treeSet.containsAll(sparseSet));
        assertTrue(sparseSet.containsAll(treeSet));
        
        Iterator<Integer> treeIterator = treeSet.iterator();
        Iterator<Integer> sparseIterator = sparseSet.iterator();
        
        for (int i = 0; i < treeSet.size(); i++)
            assertEquals(treeIterator.next(),sparseIterator.next());
    }

    public void testRemoveAll() throws Exception {
        Set<Integer> treeSet = new TreeSet<Integer>();
        Set<Integer> sparseSet = new SparseIntSet();
        
        for (int i = 0; i < 1000; i++)
            sparseSet.add(i);
        assertEquals(1000,sparseSet.size());
        
        for (int i = 900; i < 1000; i++)
            treeSet.add(i);
        
        assertTrue(sparseSet.removeAll(treeSet));
        assertFalse(sparseSet.removeAll(treeSet));
        assertEquals(900,sparseSet.size());
        
        for (int i = 0; i < 900; i++)
            assertTrue(sparseSet.contains(i));
        for (int i = 900; i < 1000; i++)
            assertFalse(sparseSet.contains(i));
    }
    
    public void testRetainAll() throws Exception {
        Set<Integer> treeSet = new TreeSet<Integer>();
        Set<Integer> sparseSet = new SparseIntSet();
        
        for (int i = 0; i < 1000; i++)
            sparseSet.add(i);
        
        for (int i = 800; i < 900; i++)
            treeSet.add(i);
        
        assertTrue(sparseSet.retainAll(treeSet));
        assertFalse(sparseSet.retainAll(treeSet));
        
        for (int i = 0; i < 800; i++)
            assertFalse(sparseSet.contains(i));
        for (int i = 800; i < 900; i++)
            assertTrue(sparseSet.contains(i));
        for (int i = 900; i < 1000; i++)
            assertFalse(sparseSet.contains(i));
    }
    
    public void testCompact() throws Exception {
        SparseIntSet sparseSet = new SparseIntSet();
        for (int i = 0; i < 1000; i++)
            sparseSet.add(i);
        
        assertGreaterThan(4000, sparseSet.getActualMemoryUsed());
        sparseSet.compact();
        assertEquals(4000, sparseSet.getActualMemoryUsed());
    }
    
    public void testNextSetBit() throws Exception {
        SparseIntSet sparseSet = new SparseIntSet();
        for (int i = 0; i < 1000; i++)
            sparseSet.add(i);
        
        sparseSet.add(2000);
        
        assertEquals(999, sparseSet.nextSetBit(999));
        assertEquals(2000, sparseSet.nextSetBit(1000));
        assertEquals(2000, sparseSet.nextSetBit(2000));
        assertEquals(-1, sparseSet.nextSetBit(2001));
        
    }
}
