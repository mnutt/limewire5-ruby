package org.limewire.collection;

import java.util.TreeSet;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

public class RandomSequenceTest extends BaseTestCase {
    
    public RandomSequenceTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(RandomSequenceTest.class);
    }
    
    public static void testUnique() throws Exception {
        TreeSet<Integer> s = new TreeSet<Integer>();
        RandomSequence r = new RandomSequence(1000);
        for (int i : r)
            assertTrue(s.add(i));
        assertEquals(1000, s.size());
        assertEquals(new Integer(0),s.first());
        assertEquals(new Integer(999), s.last());
    }
    
    public static void testDifferent() throws Exception {
        final int HOST_CATCHER_SIZE = 200;
        String out1 = ""; 
        String out2 = "";

        // iterations over the same sequence are identical
        RandomSequence r = new RandomSequence(HOST_CATCHER_SIZE);
        for (int i : r)
            out1 = out1 +" "+ i;
        for (int i : r)
            out2 = out2 +" "+ i;
        assertEquals(out1, out2);

        // but iterations over different sequences are not
        r = new RandomSequence(HOST_CATCHER_SIZE);
        out2 = "";
        for (int i : r)
            out2 = out2 +" "+ i;
        
        // Note: with this set size, 1 in 10K sequences will be the same
        assertNotEquals(out1, out2);
    }
}
