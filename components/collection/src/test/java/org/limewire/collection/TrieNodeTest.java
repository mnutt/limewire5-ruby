package org.limewire.collection;

import java.util.Iterator;
import java.util.NoSuchElementException;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

/**
 * Tests TrieNode.
 */
@SuppressWarnings( { "unchecked" } )
public class TrieNodeTest extends BaseTestCase {
    public TrieNodeTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(TrieNodeTest.class);
    }  

    /**
     * Individual test functions
     * All named: public void testXXX()
     */

    // Basic TrieNode operations
    public void testTrieNode() {
        final TrieNode node = new TrieNode();
        final Object value = "abc";
        final TrieNode childA = new TrieNode();
        final TrieNode childB = new TrieNode();
        final TrieNode childC = new TrieNode();
        final String labelA = "a very long key";
        final String labelB = "b";
        final String labelC = "c is also a key";
        Iterator iter = null;
        TrieEdge tmp = null;

        assertNull(node.getValue());
        node.setValue(value);
        assertSame(value, node.getValue());

        assertNull(node.get('a'));
        assertNotNull(iter = node.childrenForward());
        assertFalse(iter.hasNext());

        // Test put/get.  Note we insert at beginning and end of list.
        node.put(labelB, childB);
        assertNull(node.get('a'));
        assertNotNull(tmp = node.get('b'));
        assertSame(childB, tmp.getChild());
        assertNull(node.get('c'));
        assertNull(node.get('d'));
        node.put(labelA, childA);
        assertNotNull(tmp = node.get('a'));
        assertSame(childA, tmp.getChild());
        assertNotNull(tmp = node.get('b'));
        assertSame(childB, tmp.getChild());
        assertNull(node.get('c'));
        assertNull(node.get('d'));
        node.put(labelC, childC);
        assertNotNull(tmp = node.get('a'));
        assertSame(childA, tmp.getChild());
        assertNotNull(tmp = node.get('b'));
        assertSame(childB, tmp.getChild());
        assertNotNull(tmp = node.get('c'));
        assertSame(childC, tmp.getChild());
        assertNull(node.get('d'));

        // Test child iterator
        assertNotNull(iter = node.childrenForward());
        assertSame(childA, iter.next());
        assertSame(childB, iter.next());
        assertSame(childC, iter.next());
        assertFalse(iter.hasNext());
        try {
            iter.next();
            assertTrue("expected NoSuchElementException", false);
        } catch (NoSuchElementException e) {
        }

        // Test label iterator
        assertNotNull(iter = node.labelsForward());
        assertSame(labelA, iter.next());
        assertSame(labelB, iter.next());
        assertSame(labelC, iter.next());
        assertFalse(iter.hasNext());
        try {
            iter.next();
            assertTrue("expected NoSuchElementException", false);
        } catch (NoSuchElementException e) {
        }

        // Test remove operations.
        node.remove('a');
        assertNull(node.get('a'));
        assertNotNull(tmp = node.get('b'));
        assertSame(childB, tmp.getChild());
        assertNotNull(tmp = node.get('c'));
        assertSame(childC, tmp.getChild());
        node.remove('c');
        assertNull(node.get('a'));
        assertNotNull(tmp = node.get('b'));
        assertSame(childB, tmp.getChild());
        assertNull(node.get('c'));
        node.remove('b');
        assertNull(node.get('a'));
        assertNull(node.get('b'));
        assertNull(node.get('c'));

        // Test empty child iterator
        assertNotNull(iter = node.childrenForward());
        assertFalse(iter.hasNext());
        try {
            iter.next();
            fail("expected NoSuchElementException");
        } catch (NoSuchElementException e) {
        }

        // Test empty label iterator
        assertNotNull(iter = node.labelsForward());
        assertFalse(iter.hasNext());
        try {
            iter.next();
            fail("expected NoSuchElementException");
        } catch (NoSuchElementException e) {
        }
    }
    
}
