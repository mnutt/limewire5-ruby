package org.limewire.collection;

import java.util.Set;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

@SuppressWarnings("unchecked")
public class RandomOrderHashSetTest extends BaseTestCase {
    public RandomOrderHashSetTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(RandomOrderHashSetTest.class);
    }
    
    /**
     * tests that two subsequent iterations over the same set
     * will return the elements in different order.
     */
    public void testRandomOrder() throws Exception {
        Set s = new RandomOrderHashSet(50);
        for (int i = 0; i < 50; i++)
            s.add(i);
        String out1 = "";
        String out2 = "";
        for (Object o : s)
            out1 = out1 + o;
        for (Object o : s)
            out2 = out2 + o;
        assertNotEquals(out1, out2);
    }
}
