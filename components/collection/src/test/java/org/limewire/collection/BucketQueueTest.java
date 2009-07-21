package org.limewire.collection;

import java.util.Iterator;
import java.util.NoSuchElementException;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

/**
 * Unit tests for <code>BucketQueue</code>.
 */
@SuppressWarnings("unchecked")
public class BucketQueueTest extends BaseTestCase {
            
	public BucketQueueTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(BucketQueueTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

    /**
     * Tests the method for removing objects.
     * 
     * @throws Exception
     */
    public void testRemoveAll() throws Exception {
        Object o = new Object();
        int priorities = 10;
        BucketQueue bq = new BucketQueue(priorities, 5);
        for(int i=0; i<priorities; i++) {
            bq.insert(o, i);
            assertTrue("should have successfully removed object",
                bq.removeAll(o));
            assertTrue("queue should be empty", bq.isEmpty());
        }
    }
    
    
    public void testIterator() throws Exception {
        BucketQueue bq = new BucketQueue(5, 10);
        //Integer curInt = new Integer(4);
        for(int i=0; i<5; i++) {
            bq.insert(i, i);
        }

        Iterator iter = bq.iterator();
        Integer first = (Integer)iter.next();
        assertEquals("unexpected priority", 4, first.intValue());
        //while(iter.hasNext()) {
        //  Integer curInt = (Integer)iter.next();
        //}
    }
   
	
	@SuppressWarnings({"UnusedAssignment", "RedundantStringConstructorCall"})
    public void testLegacy() {
        // for the test the objects need to be
        //  a != b && a.equals(b)
        Object e4= new String("test");
        Object e2a= new String("test");
        Object e2b= new String("test");
        Object e0= new String("test");
        
        assertTrue(e4.equals(e2a) && e4.equals(e2b) && e4.equals(e0));
        assertTrue(e2a.equals(e2b) && e2a.equals(e0));
        assertTrue(e2b.equals(e0));
        
        assertTrue(e4 != e2a && e4 != e2b && e4 != e0);
        assertTrue(e2a != e2b  && e2a != e0);
        assertTrue(e2b != e0);
        
        BucketQueue q=new BucketQueue(5, 10);
        assertTrue(q.isEmpty());

        assertNull(q.insert(e0, 0));
        assertNull(q.insert(e4, 4));
        assertNull(q.insert(e2b, 2));
        assertNull(q.insert(e2a, 2));
        assertEquals(4, q.size());
        assertEquals(1, q.size(4));
        assertEquals(2, q.size(2));
        assertEquals(0, q.size(3));
        assertTrue(! q.isEmpty());

        Iterator iter=q.iterator();
        assertSame(e4, iter.next());
        assertSame(e2a, iter.next());
        assertSame(e2b, iter.next());
        assertSame(e0, iter.next());
        try {
            iter.next();
            fail("no such element expected");
        } catch (NoSuchElementException e) { }

        //Make sure hasNext is idempotent
        iter=q.iterator(4, 100);
        assertTrue(iter.hasNext());
        assertSame(e4, iter.next());
        assertTrue(iter.hasNext());
        assertTrue(iter.hasNext());
        assertSame(e2a, iter.next());
        assertTrue(iter.hasNext());
        assertSame(e2b, iter.next());
        assertTrue(iter.hasNext());
        assertSame(e0, iter.next());
        assertTrue(! iter.hasNext());
        try {
            iter.next();
            fail("nse expected");
        } catch (NoSuchElementException e) { }

        iter=q.iterator(4, 2);
        assertSame(e4, iter.next());
        assertSame(e2a, iter.next());
        assertTrue(! iter.hasNext());        

        iter=q.iterator(2, 3);     //sorting by priority
        assertTrue(iter.hasNext());
        assertSame(e2a, iter.next());
        assertTrue(iter.hasNext());
        assertSame(e2b, iter.next());
        assertTrue(iter.hasNext());
        assertSame(e0, iter.next());
        assertTrue(! iter.hasNext());

        iter=q.iterator(1, 3);     //sorting by priority
        assertTrue(iter.hasNext());
        assertSame(e0, iter.next());
        assertTrue(! iter.hasNext());

        assertSame(e4, q.getMax());
        assertSame(e4, q.extractMax());
        assertSame(e2a, q.extractMax());
        assertSame(e2b, q.extractMax());
        assertSame(e0, q.getMax());
        assertSame(e0, q.extractMax());
        try {
            q.getMax();
            fail("nse expected");
        } catch (NoSuchElementException e) { }            

        // different .equals than objects above
        Object f1= new String("nother test");
        assertTrue(!q.removeAll(f1));
        assertNull(q.insert(e0, 0));
        assertNull(q.insert(e4, 4));
        assertNull(q.insert(f1, 1));
        assertNull(q.insert(e2b, 2));
        assertNull(q.insert(e2a, 2));
        assertTrue(q.removeAll(e0));
        assertEquals(1, q.size());
        assertSame(f1, q.getMax());
        assertTrue(q.removeAll(f1));
        assertTrue(q.isEmpty());        

        //Test clone
        q=new BucketQueue(new int[] {10, 10, 10, 10, 10});
        q.insert(e4, 4);
        q.insert(e2a, 2);
        q.insert(e0, 0);

        BucketQueue q2=new BucketQueue(q);
        assertEquals(q.size(), q2.size());
        Iterator iter1=q.iterator();
        Iterator iter2=q.iterator();
        while (iter1.hasNext()) {
            assertEquals(iter1.next(), iter2.next());
        }
        assertTrue(! iter2.hasNext());

        q.insert(e2b, 2);
        assertEquals((q2.size()+1), q.size());

        //More rigorous test of insertion.  Also checks objects besides
        //Endpoint.        
        q=new BucketQueue(3, 2);
        assertNull(q.insert("oldest medium", 1));
        assertNull(q.insert("older medium", 1));
        assertEquals("oldest medium", q.insert("medium", 1));
        assertNull(q.insert("low", 0));
        assertNull(q.insert("high", 2));
        assertEquals("high", q.extractMax());
        assertEquals("medium", q.extractMax());
        assertEquals("older medium", q.extractMax());
        assertEquals("low", q.extractMax());

        //Test exceptional cases
        try {
            q=new BucketQueue(new int[0]);
            fail("illegal argument expected");
        } catch (IllegalArgumentException e) { }
        try {
            q=new BucketQueue(new int[] {1, 2, 0});
            fail("illegal argument expected");
        } catch (IllegalArgumentException e) { }
        try {
            q=new BucketQueue(new int[] {1});
        } catch (IllegalArgumentException e) { 
            fail("illegal argument expected");
        }
        try {
            q=new BucketQueue(0, 1);
            fail("illegal argument expected");
        } catch (IllegalArgumentException e) { 
        }
        try {
            q=new BucketQueue(1, 0);
            fail("illegal argument expected");
        } catch (IllegalArgumentException e) { 
        }

        q=new BucketQueue(new int[] {10, 10, 10});
        try {
            q.insert("oops", -1);
            fail("illegal argument expected");
        } catch (IllegalArgumentException e) { }
        try {
            q.insert("oops", 3);
            fail("illegal argument expected");
        } catch (IllegalArgumentException e) { }

        try {
            q.size(-1);
            fail("illegal argument expected");
        } catch (IllegalArgumentException e) { }
        try {
            q.size(3);
            fail("illegal argument expected");
        } catch (IllegalArgumentException e) { }


        try {
            iter=q.iterator(-1, 1);
            fail("illegal argument expected");
        } catch (IllegalArgumentException e) { }
        try {
            iter=q.iterator(3, 1);
            fail("illegal argument expected");
        } catch (IllegalArgumentException e) { }
    }
}

