package org.limewire.collection;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

public class IntervalTest extends BaseTestCase {

    public IntervalTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(IntervalTest.class);
    }

	public void testSubrange() {
        Range a=new Interval(0,3);
        Interval b=new Interval(3,5);
        Interval c=new Interval(1,6);
        
        assertFalse(a.isSubrange(b));
        assertTrue(b.isSubrange(c));
        assertFalse(c.isSubrange(b));
        assertTrue(b.isSubrange(b));

        
    }
}
