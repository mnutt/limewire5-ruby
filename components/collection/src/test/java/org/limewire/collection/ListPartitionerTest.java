package org.limewire.collection;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

@SuppressWarnings("unchecked")
public class ListPartitionerTest extends BaseTestCase {

    public ListPartitionerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ListPartitionerTest.class);
    }
    
    public void testEmpty() throws Exception {
        ListPartitioner l = new ListPartitioner(new ArrayList(), 10);
        for (int i = 0; i < 10; i++)
            assertTrue(l.getPartition(i).isEmpty());
        try {
            l.getPartition(11);
            fail("should have thrown NSEX");
        } catch (NoSuchElementException expected){}
    }
    
    public void testPartitionsAppear() throws Exception {
        List list = new ArrayList();
        ListPartitioner partitioner = new ListPartitioner(list, 5);
        
        // add one element - the first and last partition should
        // be the same and have 1 element
        // (the objects themselves are not the same since a new sublist is created every time)
        Object o = new Object();
        list.add(o);
        List zero = partitioner.getPartition(0);
        List first = partitioner.getFirstPartition();
        List last = partitioner.getLastPartition();
        assertEquals(1, zero.size());
        assertEquals(1, first.size());
        assertEquals(1, last.size());
        assertSame(o, zero.get(0));
        assertSame(o, first.get(0));
        assertSame(o, last.get(0));
        
        // the rest empty
        for (int i = 1; i < 5; i++)
            assertTrue(partitioner.getPartition(i).isEmpty());
        
        // add a second object, an new partition should appear
        Object o2 = new Object();
        list.add(o2);
        zero = partitioner.getPartition(0);
        first = partitioner.getFirstPartition();
        last = partitioner.getLastPartition();
        List one = partitioner.getPartition(1);
        assertEquals(1, zero.size());
        assertEquals(1, one.size());
        assertEquals(1, first.size());
        assertEquals(1, last.size());
        
        assertSame(o, zero.get(0));
        assertSame(o, first.get(0));
        
        assertSame(o2, one.get(0));
        assertSame(o2, last.get(0));
        
        // the rest empty
        for (int i = 2; i < 5; i++)
            assertTrue(partitioner.getPartition(i).isEmpty());
        
    }
    
    public void testPartitionsResize() throws Exception {
        List list = new ArrayList();
        ListPartitioner partitioner = new ListPartitioner(list, 5);
        
        // add 50 objects - each partition should have 10
        for (int i = 0; i < 50; i++)
            list.add(new Object());
        for (int i = 1; i < 5; i++)
            assertEquals(10,partitioner.getPartition(i).size());
        
        // remove all objects from one of the partitions
        List partition = partitioner.getPartition(1);
        partition.clear();
        
        // all partitions will hold 8 objects after re-creation
        for (int i = 1; i < 5; i++)
            assertEquals(8,partitioner.getPartition(i).size());
    }
    
    public void testLastPartitionExtends() throws Exception {
        List list = new ArrayList();
        ListPartitioner partitioner = new ListPartitioner(list, 2);
        
        // 2 partitions, 1 object in first, 2 in the second
        list.add(new Object());
        list.add(new Object());
        list.add(new Object());
        
        assertEquals(1, partitioner.getPartition(0).size());
        assertEquals(2, partitioner.getPartition(1).size());
        
        // with 5 objects there will be 2 in the first and 3 in the second
        list.add(new Object());
        list.add(new Object());
        assertEquals(2, partitioner.getPartition(0).size());
        assertEquals(3, partitioner.getPartition(1).size());
    }
}
