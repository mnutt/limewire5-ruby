package org.limewire.collection;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

/**
 * Unit tests for <code>FixedsizeForgetfulHashMap</code>.
 */
@SuppressWarnings("unchecked")
public class FixedsizeForgetfulHashMapTest extends BaseTestCase {
            
	public FixedsizeForgetfulHashMapTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(FixedsizeForgetfulHashMapTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	public void testLegacy() throws Exception {
        //The cryptic variable names are um..."legacy variables" =)
        FixedsizeForgetfulHashMap rt=null;
        String g1="key1";
        String g2="key2";
        String g3="key3";
        String g4="key4";
        String g5="key5";
        String c1="value1";
        String c2="value2";
        String c3="value3";
        String c4="value4";
        String c5="value5";

        //1. FIFO put/get tests
        rt=new FixedsizeForgetfulHashMap(3);
        rt.put(g1, c1);
        rt.put(g2, c2);
        rt.put(g3, c3);
        assertSame(c1, rt.get(g1));
        assertSame(c2, rt.get(g2));
        assertSame(c3, rt.get(g3));
        rt.put(g4, c4);
        assertNull(rt.get(g1));
        assertSame(c2, rt.get(g2));
        assertSame(c3, rt.get(g3));
        assertSame(c4, rt.get(g4));
        rt.put(g1, c1);
        assertSame(c1, rt.get(g1));
        assertNull(rt.get(g2));
        assertSame(c3, rt.get(g3));
        assertSame(c4, rt.get(g4));

        rt=new FixedsizeForgetfulHashMap(1);
        rt.put(g1, c1);
        assertSame(c1, rt.get(g1));
        rt.put(g2, c2);
        assertNull(rt.get(g1));
        assertSame(c2, rt.get(g2));
        rt.put(g3, c3);
        assertNull(rt.get(g1));
        assertNull(rt.get(g2));
        assertSame(c3, rt.get(g3));

        rt=new FixedsizeForgetfulHashMap(2);
        rt.put(g1,c1);
        rt.remove(g1);
        assertNull(rt.get(g1));

        //1a. Stronger put/get tests with renaming keys
        rt=new FixedsizeForgetfulHashMap(3);
        assertNull(rt.put(g1, c1));
        assertSame(c1, rt.get(g1));
        assertNull(rt.put(g2, c2));
        assertSame(c1, rt.get(g1));
        assertNull(rt.put(g3, c3));
        assertSame(c1, rt.get(g1));
        assertSame(c1, rt.put(g1, c4));
        assertSame(c4, rt.get(g1));  //remapping takes no space...
        assertSame(c2, rt.get(g2));
        assertSame(c3, rt.get(g3));

        assertNull(rt.put(g5, c5));               
        assertSame(c5, rt.get(g5)); //...and it renews the key
        assertSame(c4, rt.get(g1));
        assertSame(c3, rt.get(g3));       
        assertNull(rt.get(g2));
        assertNull(rt.put(g4, c4));
        assertSame(c4, rt.get(g4));
        assertSame(c5, rt.get(g5));
        assertSame(c4, rt.get(g1));
        assertNull(rt.get(g3));       
        assertNull(rt.get(g2));

        //2. Remove tests
        rt=new FixedsizeForgetfulHashMap(2);
        rt.put(g1,c1);
        rt.remove(g1);
        assertNull(rt.get(g1));
        rt.put(g1,c2);
        assertSame(c2, rt.get(g1));

        //3. putAll tests.
        rt=new FixedsizeForgetfulHashMap(3);
        Map m=new HashMap();
        m.put(g1,c1);
        m.put(g2,c2);
        rt.putAll(m);
        assertSame(c1, rt.get(g1));
        assertSame(c2, rt.get(g2));

        //4. keySet().iterator() methods.  (Other methods are incomplete.)
        Iterator iter=null;
        rt=new FixedsizeForgetfulHashMap(4);
        rt.put(g1, c1);
        rt.put(g2, c5);
        rt.put(g2, c2);             //remap c2
        rt.put(g3, c3);
        rt.put(g4, c4);
        iter=rt.keySet().iterator();
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
        
        iter=rt.keySet().iterator();
        assertTrue(rt.containsKey(a1));
        assertTrue(! rt.containsKey(a2));
        assertTrue(! rt.containsKey(a3));
        assertTrue(rt.containsKey(a4));
        Object b1=iter.next();
        assertSame(b1, a1); 
        Object b2=iter.next();
        assertSame(b2, a4);
        assertNotSame(b1, b2);
        assertTrue(! iter.hasNext());
        
        rt = new FixedsizeForgetfulHashMap(4);
        rt.put(g1, c1);
        rt.put(g2, c2);
        assertEquals("should be equals", rt, rt);
        assertEquals("hashcodes should be equal", rt.hashCode(), rt.hashCode());
        FixedsizeForgetfulHashMap rt2 = new FixedsizeForgetfulHashMap(3);
        rt2.put(g1, c1);
        assertNotEquals("should not be equal", rt, rt2);
        assertNotEquals("hashes shouldn't be equal", rt.hashCode(), rt2.hashCode());
        rt2.put(g2, c2);
        assertEquals("should be equals", rt, rt2);
        assertEquals("hashes should be equal", rt.hashCode(), rt2.hashCode());
        
        rt2.remove(g1);
        assertNotEquals("should not be equal", rt, rt2);
        assertNotEquals("hashes shouldn't be equal", rt.hashCode(), rt2.hashCode());
    }
	
	public void testFixedIsNotSurpassed() {
	    FixedsizeForgetfulHashMap<Integer, Integer> map = new FixedsizeForgetfulHashMap<Integer, Integer>(1);
	    map.put(1, 1);
	    map.put(2, 2);
	    assertEquals(1, map.size());
	    assertNull(map.get(1));
	    assertEquals(Integer.valueOf(2), map.get(2));
	}
	
	public void testZeroConstructor() {
	    try {
	        new FixedsizeForgetfulHashMap<Integer, Integer>(0);
	        fail("expected illegal argument exception for size 0");
	    } catch (IllegalArgumentException iae) {
	    }
	}
}