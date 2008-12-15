package org.limewire.collection;

import java.util.Iterator;

import junit.framework.Test;

import org.limewire.collection.DoublyLinkedList.ListElement;
import org.limewire.util.BaseTestCase;


/**
 * Unit tests for DoublyLinkedList
 */
@SuppressWarnings("unchecked")
public class DoublyLinkedListTest extends BaseTestCase {
            
	public DoublyLinkedListTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(DoublyLinkedListTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	public void testLegacy() {
        DoublyLinkedList l=new DoublyLinkedList();
        ListElement a=null, b=null, c=null;

        assertNull(l.removeFirst());
        assertNull(l.removeFirst());

        //One element list
        a=l.addLast("a");
        assertTrue(a.getKey().equals("a"));
        assertTrue(l.removeFirst()==a);
        assertNull(l.removeFirst());

        //Two element list: remove first
        a=l.addLast("a");
        assertEquals("a", a.getKey());
        b=l.addLast("b");
        assertEquals("b", b.getKey());
        l.remove(a);
        assertSame(b, l.removeFirst());
        assertNull(l.removeFirst());

        //Two element list: remove last
        a=l.addLast("a");
        assertEquals("a", a.getKey());
        b=l.addLast("b");
        assertEquals("b", b.getKey());
        l.remove(b);
        assertSame(a, l.removeFirst());
        assertNull(l.removeFirst());

        //Three element list: test removing first element of the list.
        a=l.addLast("a");
        assertEquals("a", a.getKey());
        b=l.addLast("b");
        assertEquals("b", b.getKey());
        c=l.addLast("c");
        assertEquals("c", c.getKey());
        l.remove(a);
        assertSame(b, l.removeFirst());
        assertSame(c, l.removeFirst());
        assertNull(l.removeFirst());

        //Three element list: test removing middle element of the list.
        a=l.addLast("a");
        assertEquals("a", a.getKey());
        b=l.addLast("b");
        assertEquals("b", b.getKey());
        c=l.addLast("c");
        assertEquals("c", c.getKey());
        l.remove(b);
        assertSame(a, l.removeFirst());
        assertSame(c, l.removeFirst());
        assertNull(l.removeFirst());

        //Three element list: test removing last element of the list.
        a=l.addLast("a");
        assertEquals("a", a.getKey());
        b=l.addLast("b");
        assertEquals("b", b.getKey());
        c=l.addLast("c");
        assertEquals("c", c.getKey());
        l.remove(c);
        assertSame(a, l.removeFirst());
        assertSame(b, l.removeFirst());
        assertNull(l.removeFirst());

        //Test clear
        l.addLast("d");
        l.clear();
        assertNull(l.removeFirst());
        assertNull(l.removeFirst());

        //Test iterator and contains
        l.clear();
        Iterator iter=l.iterator();
        assertTrue(! iter.hasNext());

        assertTrue(! l.contains(a));
        a=l.addLast("a");
        assertTrue(l.contains(a));
        iter=l.iterator();
        assertTrue(iter.next()==a);
        assertTrue(! iter.hasNext());

        b=l.addLast("b");
        assertTrue(! l.contains(c));
        c=l.addLast("c");
        assertTrue(l.contains(b));
        assertTrue(l.contains(c));
        iter=l.iterator();
        assertSame(a, iter.next());
        assertSame(b, iter.next());
        assertSame(c, iter.next());
        assertTrue(! iter.hasNext());
    }
}