package org.limewire.collection;

import java.util.List;
import java.util.NoSuchElementException;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;
import org.limewire.util.PrivilegedAccessor;

/**
 * Unit tests for IntSet
 */
@SuppressWarnings("unchecked")
public class IntSetTest extends BaseTestCase {
    
    IntSet s, s1, s2;
            
	public IntSetTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(IntSetTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	public void testSearch() throws Exception {
	    //Test search(..) by manually modifying the rep.  It's important to test
        //even and odd length lists here.
        s=new TestingIntSet();
        assertEquals(-1, search(s, 0));

        add(s, newInterval(0));          //[0]
        assertEquals(-1, search(s, -1));
        assertEquals(0, search(s, 0));
        assertEquals(0, search(s, 1));
        
        add(s, newInterval(10));          //[0, 10]
        assertEquals(-1, search(s, -1));
        assertEquals(0, search(s, 0));
        assertEquals(0, search(s, 1));
        assertEquals(1, search(s, 10));
        assertEquals(1, search(s, 11));

        add(s, newInterval(20));          //[0, 10, 20]
        assertEquals(-1, search(s, -1));
        assertEquals(0, search(s, 0));
        assertEquals(0, search(s, 1));
        assertEquals(1, search(s, 10));
        assertEquals(1, search(s, 11));
        assertEquals(2, search(s, 20));
        assertEquals(2, search(s, 21));

        add(s, newInterval(30));          //[0, 10, 20, 30]
        assertEquals(-1, search(s, -1));
        assertEquals(0, search(s, 0));
        assertEquals(0, search(s, 9));
        assertEquals(1, search(s, 10));
        assertEquals(1, search(s, 19));
        assertEquals(2, search(s, 20));
        assertEquals(2, search(s, 29));
        assertEquals(3, search(s, 30));
        assertEquals(3, search(s, 31));
    }
    
    public void testAddAndContainsAtEndpoints() throws Exception {

        //Test add/contains endpoint tests.
        s=new TestingIntSet();                     //[]
        assertTrue(! s.contains(10)); 
        assertTrue(s.add(10));       //[10]
        assertEquals(1, s.size());
        assertTrue(s.contains(10));
        assertTrue(s.add(9));        //[9-10]
        assertEquals(2, s.size());
        assertTrue(s.contains(9));
        assertTrue(s.contains(10));
        assertTrue(s.add(7));        //[7, 9-10]
        assertEquals(3, s.size());
        assertTrue(s.contains(7));
        assertTrue(s.contains(9));
        assertTrue(s.contains(10));
        assertTrue(s.add(11));       //[7, 9-11]
        assertTrue(s.contains(7));
        assertTrue(s.contains(9));
        assertTrue(s.contains(10));
        assertTrue(s.contains(11));
        assertTrue(s.add(13));       //[7, 9-11, 13]
        assertTrue(s.contains(7));
        assertTrue(! s.contains(8));
        assertTrue(s.contains(9));
        assertTrue(s.contains(10));
        assertTrue(s.contains(11));
        assertTrue(!s.contains(12));
        assertTrue(s.contains(13));
        assertTrue(! s.add(7)); 
        assertEquals(5, s.size());
        assertTrue(!s.add(9));
        assertEquals(5, s.size());
        assertTrue(! s.add(10));
        assertTrue(! s.add(11));
    }
    
    public void testAddAndContainsAtMiddle() throws Exception {

        //Test add/contains middle tests.
        s=new TestingIntSet();
        s.add(0);
        s.add(4);                           //[0, 4]
        assertTrue(s.add(1));        //[0-1, 4]
        assertTrue(s.contains(1));
        assertTrue(s.add(3));        //[0-1, 3-4]
        assertEquals(4, s.size());
        assertTrue(s.contains(1));
        assertTrue(s.contains(3));
        assertTrue(s.add(2));        //[0-4]
        assertTrue(s.contains(1));
        assertTrue(s.contains(2));
        assertTrue(s.contains(3));
        assertTrue(s.contains(4));
        s=new TestingIntSet();
        s.add(0);
        s.add(4);                           //[0, 4]
        assertTrue(s.add(2));        //[0, 2, 4]
        assertTrue(s.contains(2));
    }
        
    public void testRemove() throws Exception {

        //Test remove methods.
        s=new TestingIntSet(); 
        s.add(1); s.add(2); s.add(3); 
        s.add(4); s.add(5);                 //[1-5]
        assertTrue(!s.remove(6));
        assertTrue(s.remove(1));     //[2-5]
        assertEquals(4, s.size());
        assertTrue(! s.contains(1));
        assertTrue(s.contains(2));
        assertTrue(s.contains(3));
        assertTrue(s.contains(4));
        assertTrue(s.contains(5));
        assertTrue(s.remove(5));    //[2-4]
        assertTrue(! s.contains(1));
        assertTrue(s.contains(2));
        assertTrue(s.contains(3));
        assertTrue(s.contains(4));
        assertTrue(! s.contains(5));
        assertTrue(s.remove(3));    //[2,4]
        assertTrue(s.size()==2);
        assertTrue(! s.contains(1));
        assertTrue(s.contains(2));
        assertTrue(! s.contains(3));
        assertTrue(s.contains(4));
        assertTrue(! s.contains(5));
        assertTrue(s.remove(2));    //[4]
        assertTrue(! s.contains(1));
        assertTrue(!s.contains(2));
        assertTrue(! s.contains(3));
        assertTrue(s.contains(4));
        assertTrue(! s.contains(5));
        assertTrue(s.remove(4));    //[]
        assertTrue(! s.contains(1));
        assertTrue(! s.contains(2));
        assertTrue(! s.contains(3));
        assertTrue(! s.contains(4));
        assertTrue(! s.contains(5));
    }
        
    public void testIterator() throws Exception {

        //Test iterator() method
        s=new TestingIntSet();
        IntSet.IntSetIterator iter=s.iterator();
        assertTrue(! iter.hasNext());
        s.add(1);
        s.add(4); s.add(5);
        s.add(7); s.add(8); s.add(9);    //[1, 4-5, 7-9]
        iter=s.iterator();
        assertEquals(1, iter.next());
        assertTrue(iter.hasNext());
        assertEquals(4, iter.next());
        assertEquals(5, iter.next());
        assertEquals(7, iter.next());
        assertEquals(8, iter.next());
        assertEquals(9, iter.next());
        assertTrue(! iter.hasNext());
        try {
            iter.next();
            fail("no such element expected");
        } catch (NoSuchElementException e) {
        }
    }
    
    public void testAddAll() throws Exception {
        //Test addAll method
        s1=new TestingIntSet(); s1.add(1); s1.add(2);   //[1-2]
        s2=new TestingIntSet(); s2.add(1);              //[1]
        assertTrue(! s1.addAll(s2));   //s1 unchanged
        assertTrue(s1.contains(1));
        assertTrue(s1.contains(2));
        assertTrue(s2.addAll(s1));    //s2 equals s1
        assertTrue(s2.contains(1));
        assertTrue(s2.contains(2));
    }
    
    public void testRetainAll() throws Exception {
        //Test retainAll method
        s1=new TestingIntSet(); s1.add(1); s1.add(2);   //[1-2]
        s2=new TestingIntSet(); s2.add(1);              //[1]
        assertTrue(! s2.retainAll(s1));  //s2 unchanged
        assertTrue(s2.contains(1));
        assertTrue(! s2.contains(2));  
        assertTrue(s1.retainAll(s2));   //s1 equals s2
        assertTrue(s1.contains(1));
        assertTrue(! s1.contains(2));  
    }
    
    private static int search(IntSet s, int num) throws Exception {
        return (Integer) PrivilegedAccessor.invokeMethod(
                s, "search", new Object[]{num},
                new Class[]{int.class});
    }
    
    private static Object newInterval(int num) throws Exception {
        Class interval = PrivilegedAccessor.getClass(
            IntSet.class, "Interval");
            
        return PrivilegedAccessor.invokeConstructor(
            interval, new Object[] {num},
            new Class[] { int.class } );
    }
    
    private static void add(IntSet set, Object o) throws Exception {
        List list = (List)PrivilegedAccessor.getValue(set, "list");
        list.add(o);
    }

    /**
     * Checks invariants.
     */
    private static class TestingIntSet extends IntSet {
        @Override
        public boolean add(int x) {
            try {
                repOk();
                    return super.add(x);
            } finally {
                repOk();
            }
        }
        
        @Override
        public boolean remove(int x) {
            try {
                repOk();
                return super.remove(x);
            } finally {
                repOk();
            }
        }
    }
	    
    
}