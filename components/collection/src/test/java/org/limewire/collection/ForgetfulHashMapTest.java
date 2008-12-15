package org.limewire.collection;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

/**
 * Unit tests for ForgetfulHashMap
 */
@SuppressWarnings("unchecked")
public class ForgetfulHashMapTest extends BaseTestCase {
            
	public ForgetfulHashMapTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(ForgetfulHashMapTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	public void testLegacy() {
        //The cryptic variable names are um..."legacy variables" =)
        ForgetfulHashMap rt=null;
        String g1="key1";
        String g2="key2";
        String g3="key3";
        String g4="key4";
        String c1="value1";
        String c2="value2";
        String c3="value3";
        String c4="value4";
        
        //1. FIFO put/get tests
        rt=new ForgetfulHashMap(3);
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
        
        rt=new ForgetfulHashMap(1);
        rt.put(g1, c1);
        assertSame(c1,  rt.get(g1));
        rt.put(g2, c2);
        assertNull(rt.get(g1));
        assertSame(c2, rt.get(g2));
        rt.put(g3, c3);
        assertNull(rt.get(g1));
        assertNull(rt.get(g2));
        assertSame(c3, rt.get(g3));
        
        rt=new ForgetfulHashMap(2);
        rt.put(g1,c1);
        rt.remove(g1);
        assertNull(rt.get(g1));
        
        //2. Remove tests
        rt=new ForgetfulHashMap(2);
        rt.put(g1,c1);
        rt.remove(g1);
        assertNull(rt.get(g1));
        rt.put(g1,c2);
        assertSame(c2, rt.get(g1));
        
        //3. putAll tests.
        rt=new ForgetfulHashMap(3);
        Map m=new HashMap();
        m.put(g1,c1);
        m.put(g2,c2);
        rt.putAll(m);
        assertSame(c1, rt.get(g1));
        assertSame(c2, rt.get(g2));
    }
}	