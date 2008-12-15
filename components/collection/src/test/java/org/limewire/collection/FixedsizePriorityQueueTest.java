package org.limewire.collection;

import java.util.Iterator;
import java.util.NoSuchElementException;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

@SuppressWarnings("unchecked")
public class FixedsizePriorityQueueTest extends BaseTestCase {
    private final Integer one= 1;
    private final Integer two= 2;
    private final Integer three= 3;
    private final Integer four= 4;
    private final Integer five= 5;
    private final Integer six= 6;
    
    /**
     * The default queue for testing, with a capacity of 4 and the elements 2,
     * 3, and 4.
     * @see setUp.
     */
    private FixedsizePriorityQueue q;

    public FixedsizePriorityQueueTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(FixedsizePriorityQueueTest.class);
    }    
    
    @Override
    public void setUp() {
        q=new FixedsizePriorityQueue(Comparators.integerComparator(), 4);
        assertNull(q.insert(three));
        assertNull(q.insert(four));
        assertNull(q.insert(two));
    }

    public void testSize() {
        assertEquals(3, q.size());
    }

    public void testCapacity() {
        assertEquals(4, q.capacity());
    }

    public void testMin() {
        assertEquals(two, q.getMin());
        q=new FixedsizePriorityQueue(Comparators.integerComparator(), 4);
        try {
            q.getMin();
            fail("No no such element exception");
        } catch (NoSuchElementException e) { }
    }

    public void testMax() {
        assertEquals(four, q.getMax());
        q=new FixedsizePriorityQueue(Comparators.integerComparator(), 4);
        try {
            q.getMax();
            fail("No no such element exception");
        } catch (NoSuchElementException e) { }
    }

    public void testContains() {
        assertTrue(! q.contains(one));
        assertTrue(q.contains(two));
        assertTrue(q.contains(three));
        assertTrue(q.contains(four));
        assertTrue(! q.contains(five));
    }

    public void testIterator() {
        Iterator iter=q.iterator();
        assertEquals(two, iter.next());
        assertEquals(three, iter.next());
        assertEquals(four, iter.next());
        assertTrue(!iter.hasNext());
    }
        
    public void testInsert() {
        //Three basic cases in the insert code:
        //a) no elements removed
        assertNull(q.insert(five));        //2, 3, 4, 5
        Iterator iter=q.iterator();
        assertEquals(two, iter.next());
        assertEquals(three, iter.next());
        assertEquals(four, iter.next());
        assertEquals(five, iter.next());
        assertTrue(! iter.hasNext());

        //c) this element removed (i.e., this not added)
        assertEquals(one, q.insert(one));  //2, 3, 4, 5 (case c)
        iter=q.iterator();
        assertEquals(two, iter.next());
        assertEquals(three, iter.next());
        assertEquals(four, iter.next());
        assertEquals(five, iter.next());
        assertTrue(! iter.hasNext());

        //b) smallest element removed
        assertEquals(two, q.insert(six));  //3, 4, 5, 6 (case b)
        iter=q.iterator();
        assertEquals(three, iter.next());
        assertEquals(four, iter.next());
        assertEquals(five, iter.next());
        assertEquals(six, iter.next());
        assertTrue(! iter.hasNext());

        //Test duplicates
        assertEquals(three, q.insert(six)); //4, 5, 6, 6
        assertEquals(four, q.insert(six));  //5, 6, 6, 6
        assertEquals(five, q.insert(six));  //6, 6, 6, 6
        iter=q.iterator();
        assertEquals(six, iter.next());
        assertEquals(six, iter.next());
        assertEquals(six, iter.next());
        assertEquals(six, iter.next());
        assertTrue(! iter.hasNext());
    }
  
    public void testRemoveNotFound() {
        assertTrue(! q.remove(five));
        Iterator iter=q.iterator();
        assertEquals(two, iter.next());
        assertEquals(three, iter.next());
        assertEquals(four, iter.next());
        assertTrue(!iter.hasNext());
    }

    public void testRemoveFound() {
        assertTrue(q.remove(three));
        Iterator iter=q.iterator();
        assertEquals(two, iter.next());
        assertEquals(four, iter.next());
        assertTrue(!iter.hasNext());
    }

    public void testRemoveFoundOne() {
        assertNull(q.insert(two));  //2, 2, 3, 4
        assertTrue(q.remove(two));
        Iterator iter=q.iterator();
        assertEquals(two, iter.next());
        assertEquals(three, iter.next());
        assertEquals(four, iter.next());
        assertTrue(!iter.hasNext());
    }    

    /*
    public void testPerformance() {
        int SIZE=1000;
        Integer[] numbers=new Integer[100000];
        java.util.Random rand=new java.util.Random();
        for (int i=0; i<numbers.length; i++) 
            numbers[i]=new MyInteger(rand.nextInt());

        BinaryHeap q1=new BinaryHeap(SIZE);
        long start=System.currentTimeMillis();
        for (int i=0; i<numbers.length; i++) 
            q1.insert(numbers[i]);
        long stop=System.currentTimeMillis();
        System.out.println("Elapsed time for BinaryHeap: "
                           +(stop-start));

        FixedsizePriorityQueue q2=new FixedsizePriorityQueue(SIZE);
        start=System.currentTimeMillis();
        for (int i=0; i<numbers.length; i++) 
            q2.insert(numbers[i], numbers[i].getInt());
        stop=System.currentTimeMillis();
        System.out.println("Elapsed time for FixedSizePriorityQueue: "
                           +(stop-start));

        assertEquals(q2.getMax(), q1.getMax());        
    }
    */
}
