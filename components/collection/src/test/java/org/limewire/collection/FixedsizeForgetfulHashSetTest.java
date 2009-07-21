package org.limewire.collection;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

/**
 * Unit tests for FixedsizeForgetfulHashSetTest.
 */
@SuppressWarnings("unchecked")
public class FixedsizeForgetfulHashSetTest extends BaseTestCase {
            
    public FixedsizeForgetfulHashSetTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(FixedsizeForgetfulHashSetTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testLegacy() throws Exception {
        FixedsizeForgetfulHashSet rt=null;
        String g1="key1";
        String g2="key2";
        String g3="key3";
        String g4="key4";
        String g5="key5";

        //1. FIFO put/get tests
        rt=new FixedsizeForgetfulHashSet(3);
        rt.add(g1);
        rt.add(g2);
        rt.add(g3);
        assertTrue(rt.contains(g1));
        assertTrue(rt.contains(g2));
        assertTrue(rt.contains(g3));
        rt.add(g4);
        assertFalse(rt.contains(g1));
        assertTrue(rt.contains(g2));
        assertTrue(rt.contains(g3));
        assertTrue(rt.contains(g4));
        rt.add(g1);
        assertTrue(rt.contains(g1));
        assertFalse(rt.contains(g2));
        assertTrue(rt.contains(g3));
        assertTrue(rt.contains(g4));

        rt=new FixedsizeForgetfulHashSet(1);
        rt.add(g1);
        assertTrue(rt.contains(g1));
        rt.add(g2);
        assertFalse(rt.contains(g1));
        assertTrue(rt.contains(g2));
        rt.add(g3);
        assertFalse(rt.contains(g1));
        assertFalse(rt.contains(g2));
        assertTrue(rt.contains(g3));

        rt=new FixedsizeForgetfulHashSet(2);
        rt.add(g1);
        rt.remove(g1);
        assertFalse(rt.contains(g1));

        //1a. Stronger put/get tests with renaming keys
        rt=new FixedsizeForgetfulHashSet(3);
        assertTrue(rt.add(g1));
        assertTrue(rt.contains(g1));
        assertTrue(rt.add(g2));
        assertTrue(rt.contains(g2));
        assertTrue(rt.contains(g1));
        assertTrue(rt.add(g3));
        assertTrue(rt.contains(g1));
        assertFalse(rt.add(g1)); // remapping takes no space
        assertTrue(rt.contains(g1));
        assertTrue(rt.contains(g2));
        assertTrue(rt.contains(g3));

        assertTrue(rt.add(g5));
        assertTrue(rt.contains(g5));
        assertTrue(rt.contains(g1)); // .. and it renewed the key.
        assertTrue(rt.contains(g3));
        assertFalse(rt.contains(g2));
        
        assertTrue(rt.add(g4));
        assertTrue(rt.contains(g4));
        assertTrue(rt.contains(g5));
        assertTrue(rt.contains(g1));
        assertFalse(rt.contains(g3));
        assertFalse(rt.contains(g2));

        //2. Remove tests
        rt=new FixedsizeForgetfulHashSet(2);
        rt.add(g1);
        rt.remove(g1);
        assertFalse(rt.contains(g1));
        rt.add(g1);
        assertTrue(rt.contains(g1));

        //3. putAll tests.
        rt=new FixedsizeForgetfulHashSet(3);
        Set m=new HashSet();
        m.add(g1);
        m.add(g2);
        rt.addAll(m);
        assertTrue(rt.contains(g1));
        assertTrue(rt.contains(g2));

        //4. keySet().iterator() methods.  (Other methods are incomplete.)
        Iterator iter=null;
        rt=new FixedsizeForgetfulHashSet(4);
        rt.add(g1);
        rt.add(g2);
        rt.add(g2);             //remap c2
        rt.add(g3);
        rt.add(g4);
        iter=rt.iterator();
        assertTrue(iter.hasNext());
        Object a1=iter.next();
        assertSame(a1, g1);
        assertTrue(iter.hasNext());
        Object a2=iter.next();
        assertTrue(rt.size()==4);
        iter.remove();               //remove a2
        assertTrue(rt.size()==3);
        assertSame(a2, g2);
        assertNotSame(a1, a2);
        assertTrue(iter.hasNext());
        Object a3=iter.next();
        assertEquals(3, rt.size());
        iter.remove();               //remove a3
        assertEquals(2, rt.size());
        assertSame(a3, g3);
        assertNotSame(a3, a2);
        assertNotSame(a3, a1);
        Object a4=iter.next();
        assertSame(a4, g4);
        assertNotSame(a4, a3);
        assertNotSame(a4, a2);
        assertNotSame(a4, a1);
        assertTrue(! iter.hasNext());
        
        iter=rt.iterator();
        assertTrue(rt.contains(a1));
        assertTrue(! rt.contains(a2));
        assertTrue(! rt.contains(a3));
        assertTrue(rt.contains(a4));
        Object b1=iter.next();
        assertSame(b1, a1);
        Object b2=iter.next();
        assertSame(b2, a4);
        assertNotSame(b1, b2);
        assertTrue(! iter.hasNext());
        
        rt = new FixedsizeForgetfulHashSet(4);
        rt.add(g1);
        rt.add(g2);
        assertEquals("should be equals", rt, rt);
        assertEquals("hashcodes should be equal", rt.hashCode(), rt.hashCode());
        FixedsizeForgetfulHashSet rt2 = new FixedsizeForgetfulHashSet(3);
        rt2.add(g1);
        assertNotEquals("should not be equal", rt, rt2);
        assertNotEquals("hashes shouldn't be equal", rt.hashCode(), rt2.hashCode());
        rt2.add(g2);
        assertEquals("should be equals", rt, rt2);
        assertEquals("hashes should be equal", rt.hashCode(), rt2.hashCode());
        
        rt2.remove(g1);
        assertNotEquals("should not be equal", rt, rt2);
        assertNotEquals("hashes shouldn't be equal", rt.hashCode(), rt2.hashCode());
    }
}