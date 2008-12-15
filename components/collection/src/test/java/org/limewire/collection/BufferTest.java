package org.limewire.collection;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

@SuppressWarnings("unchecked")
public class BufferTest extends BaseTestCase {
    public BufferTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(BufferTest.class);
    }

    public void testLegacy() {
        //1. Tests of old methods.
        Buffer buf=new Buffer(4);
        Iterator iter=null;

        assertEquals(4, buf.getCapacity());
        assertEquals(0, buf.getSize());
        iter=buf.iterator();
        assertTrue(!iter.hasNext());
        try {
            iter.next();
            assertTrue(false);
        } catch (NoSuchElementException e) {
            assertTrue(true);
        }

        buf.add("test");
        assertEquals(1, buf.getSize());
        iter=buf.iterator();
        assertTrue(iter.hasNext());
        assertEquals("test", iter.next());
        assertTrue(!iter.hasNext());
        try {
            iter.next();
            assertTrue(false);
        } catch (NoSuchElementException e) {}

        buf.add("test2");
        assertEquals(2,buf.getSize());
        buf.add("test3");
        assertEquals(3,buf.getSize());
        iter=buf.iterator();
        assertTrue(iter.hasNext());
        assertEquals("test3",iter.next());
        assertTrue(iter.hasNext());
        assertEquals("test2", iter.next());
        assertTrue(iter.hasNext());
        assertEquals("test", iter.next());
        assertTrue(!iter.hasNext());

        buf.add("test4");
        assertEquals(4,buf.getSize());
        buf.add("test5");
        assertEquals(4,buf.getSize());
        iter=buf.iterator();
        assertTrue(iter.hasNext());
        assertEquals("test5", iter.next());
        assertTrue(iter.hasNext());
        assertEquals("test4", iter.next());
        assertTrue(iter.hasNext());
        assertEquals("test3", iter.next());
        assertTrue(iter.hasNext());
        assertEquals("test2",iter.next());
        assertTrue(!iter.hasNext());	    

        buf.add("test6");
        assertEquals(4,buf.getSize());

        //2.  Tests of new methods.  These are definitely not sufficient.
        buf=new Buffer(4);
        assertEquals(0,buf.getSize());
        assertNull(buf.addLast("a"));
        assertEquals(1,buf.getSize());
        assertNull(buf.addLast("b"));
        assertEquals(2,buf.getSize());
        assertNull(buf.addLast("c"));
        assertEquals(3,buf.getSize());
        assertNull(buf.addLast("d"));
        assertEquals(4,buf.getSize());
        assertEquals("a", buf.first());
        assertEquals("a", buf.removeFirst());
        assertEquals(3,buf.getSize());
        assertEquals("b",buf.first());
        assertEquals("b", buf.removeFirst());
        assertEquals(2,buf.getSize());
        assertNull(buf.addFirst("b"));
        assertEquals(3,buf.getSize());
        assertNull(buf.addFirst("a"));
        assertEquals(4,buf.getSize());
        //buf=[a b c d]

        assertEquals("a", buf.addLast("e"));
        //buf=[b c d e]
        assertEquals("e",buf.last());
        assertEquals("b",buf.first());
        assertEquals("e", buf.removeLast());
        assertEquals("d",buf.removeLast());		

        buf=new Buffer(4);
        iter=buf.iterator();
        buf.addFirst("a");
        try {
            iter.hasNext();
            assertTrue(false);
        } catch (ConcurrentModificationException e) {
            assertTrue(true);
        }

        buf=new Buffer(4);
        buf.addFirst("a");
        buf.addLast("b");
        buf.clear();
        assertEquals(0,buf.getSize());
        iter=buf.iterator();
        assertTrue(! iter.hasNext());

        //3. Tests of get and set.
        buf=new Buffer(3);
        try {
            buf.get(0);
            assertTrue(false);
        } catch (IndexOutOfBoundsException e) {
        }
        try {
            buf.get(-1);
            assertTrue(false);
        } catch (IndexOutOfBoundsException e) {
        }

        buf.addLast("a");
        buf.addLast("b");
        buf.addLast("c");
        buf.addLast("d");  //clobbers a!
        assertEquals("b", buf.get(0));
        assertEquals("c", buf.get(1));
        assertEquals("d", buf.get(2));
        buf.set(2,"bb");
        assertEquals("bb", buf.get(2));

        //4. Tests of remove and removeAll methods.
        buf=new Buffer(4);
        buf.addLast("a");
        buf.addLast("b");
        buf.addLast("c");
        buf.addLast("d");
        try {
            buf.remove(-1);
            assertTrue(false);
        } catch (IndexOutOfBoundsException e) { }
        assertEquals("a",(buf.remove(0)));
        assertEquals(3,buf.size());
        assertEquals("b",buf.get(0));
        assertEquals("c", buf.get(1));
        assertEquals("d", buf.get(2)); 

        buf=new Buffer(4);
        buf.addLast("x");
        buf.addLast("y");
        buf.addLast("a");
        buf.addLast("b");
        buf.addLast("c");
        buf.addLast("d");
        try {
            buf.remove(5);
            assertTrue(false);
        } catch (IndexOutOfBoundsException e) { }
        assertEquals("a",(buf.remove(0)));
        assertEquals(3,buf.size());
        assertEquals("b", buf.get(0));
        assertEquals("c", buf.get(1));
        assertEquals("d", buf.get(2));  

        buf=new Buffer(4);
        buf.addLast("a");
        buf.addLast("b");
        buf.addLast("c");
        buf.addLast("d");
        try {
            buf.remove(5);
            assertTrue(false);
        } catch (IndexOutOfBoundsException e) { }
        assertEquals("d",buf.remove(3));
        assertEquals(3,buf.size());
        assertEquals("a", buf.get(0));
        assertEquals("b", buf.get(1));
        assertEquals("c", buf.get(2));  

        buf=new Buffer(4);
        buf.addLast("a");
        buf.addLast("b");
        buf.addLast("c");
        buf.addLast("d");
        try {
            buf.remove(5);
            assertTrue(false);
        } catch (IndexOutOfBoundsException e) { }
        assertEquals("b",buf.remove(1));
        assertEquals(3,buf.size());
        assertEquals("a",buf.get(0));
        assertEquals("c", buf.get(1));
        assertEquals("d", buf.get(2)); 

        buf=new Buffer(4);
        buf.addLast("b");
        buf.addLast("b");
        buf.addLast("c");
        buf.addLast("d");
        assertTrue(buf.remove("d"));
        assertTrue(buf.remove("b"));
        assertEquals(2,buf.size());
        assertEquals("b", buf.get(0));
        assertEquals("c", buf.get(1));

        buf=new Buffer(4);
        buf.addLast("b");
        buf.addLast("b");
        buf.addLast("c");
        buf.addLast("b");
        assertTrue(buf.removeAll("b"));
        assertEquals(1,buf.size());
        assertEquals("c",buf.get(0));

        //5. Test clone() method.
        buf=new Buffer(2);
        buf.addLast("a");
        buf.addLast("b");
        Buffer buf2=new Buffer(buf);
        assertEquals(buf2.size(),buf.size());
        assertEquals(buf2.getCapacity(),buf.getCapacity());
        assertEquals(buf2.first(),buf.first());
        assertEquals(buf2.last(),buf.last());
        
        assertNotNull(buf.removeFirst()); //buf2 unmodified
        assertEquals(1,buf.size());
        assertEquals(2,buf2.size());
        assertEquals("b",buf.first());
        assertEquals("a",buf2.first());
        assertEquals("b",buf2.last());
    }


    public void testContains() {
        Buffer buf = new Buffer(3);
        buf.add("a");
        assertTrue(buf.contains("a"));
        buf.add("b");
        assertTrue(buf.contains("b"));
        buf.addLast("c");
        assertTrue(buf.contains("a"));
        assertTrue(buf.contains("b"));
        assertTrue(buf.contains("c"));
        buf.add("d");
        assertTrue(buf.contains("d"));        
        assertTrue(buf.contains("a"));
        assertTrue(buf.contains("b"));
        assertTrue(!buf.contains("c"));
        buf.addLast("e");
        assertTrue(buf.contains("e"));        
        assertTrue(buf.contains("a"));
        assertTrue(buf.contains("b"));
        assertTrue(!buf.contains("d"));
    }

    public void testRemove() {
        // last from full buffer, then first, already removed one, and last remaining one
        Buffer<Integer> buf = new Buffer<Integer>(3);
        buf.add(1);
        buf.add(2);
        buf.add(3);
        buf.add(4);
        assertTrue(buf.remove(Integer.valueOf(3)));
        assertTrue(buf.remove(Integer.valueOf(4)));
        assertFalse(buf.remove(Integer.valueOf(1)));
        assertTrue(buf.remove(Integer.valueOf(2)));
        
        // last from non-full buffer
        buf = new Buffer<Integer>(3);
        buf.add(1);
        buf.add(2);
        assertTrue(buf.remove(Integer.valueOf(2)));

        // first from non-full buffer
        buf = new Buffer<Integer>(3);
        buf.add(1);
        buf.add(2);
        assertTrue(buf.remove(Integer.valueOf(1)));
        
        // first from full buffer
        buf = new Buffer<Integer>(3);
        buf.add(1);
        buf.add(2);
        buf.add(3);
        assertTrue(buf.remove(Integer.valueOf(1)));
        
        // first from over full buffer
        buf = new Buffer<Integer>(3);
        buf.add(1);
        buf.add(2);
        buf.add(3);
        buf.add(4);
        assertTrue(buf.remove(Integer.valueOf(4)));
        
        // middle one from full buffer
        buf = new Buffer<Integer>(3);
        buf.add(1);
        buf.add(2);
        buf.add(3);
        assertTrue(buf.remove(Integer.valueOf(2)));
        
        // middle one from full buffer
        buf = new Buffer<Integer>(3);
        buf.add(1);
        buf.add(2);
        buf.add(3);
        buf.add(4);
        assertTrue(buf.remove(Integer.valueOf(2)));
        
        
    }

}
