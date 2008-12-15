package org.limewire.collection;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

public class TupleTest extends BaseTestCase {

    public TupleTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(TupleTest.class);
    }
        
    public void testGetFirst() {
        Tuple<String, Integer> t = new Tuple<String, Integer>("hello", 0);
        assertEquals("hello", t.getFirst());
    }

    public void testGetSecond() {
        Tuple<String, Integer> t = new Tuple<String, Integer>("hello", 154);
        assertEquals(Integer.valueOf(154), t.getSecond());
    }
    
    public void testNullConstructor() {
        Tuple<String, String> t = new Tuple<String, String>(null, null);
        assertNull(t.getFirst());
        assertNull(t.getSecond());
    }
    
    public void testToString() {
        // test for no np exception for null references
        Tuple<String, String> t = new Tuple<String, String>(null, null);
        try {
            t.toString();
        }
        catch (NullPointerException npe) {
            fail("Should not have thrown NPE", npe);
        }
    }

}
