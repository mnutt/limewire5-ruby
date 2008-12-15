package org.limewire.collection;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.limewire.util.BaseTestCase;


/**
 * Test for the <tt>Comparators</tt> class that provides access to 
 * common <tt>Comparator</tt> isntances.
 * 
 * TODO:: add tests for both FileComparator and StringComparator.
 */
@SuppressWarnings("unchecked")
public class ComparatorsTest extends BaseTestCase {

    public ComparatorsTest(String name) {
        super(name);
    }

    /**
     * Tests the comparator for <tt>Long</tt>s.
     * 
     * @throws Exception if an error occurs
     */
    public void testLongComparator() throws Exception {
        Long one = (long) 1;
        Long two = (long) 2;
        Long three = (long) 3;
        String a = "a";
        String b = "b";
        String c = "c";
        Map longMap = new TreeMap(Comparators.longComparator());
        java.util.Map referenceLongMap = new java.util.TreeMap();
        longMap.put(one, a);
        longMap.put(two, b);
        longMap.put(three, c);
        
        referenceLongMap.put(one, a);
        referenceLongMap.put(two, b);
        referenceLongMap.put(three, c);
        Iterator iter = longMap.values().iterator();
        assertEquals("unexpected string", a, iter.next());
        assertEquals("unexpected string", b, iter.next());
        assertEquals("unexpected string", c, iter.next());
        
        // Now, test against the new code that has existed since Java 1.2
        java.util.Iterator referenceIter = referenceLongMap.values().iterator();
        assertEquals("unexpected string", a, referenceIter.next());
        assertEquals("unexpected string", b, referenceIter.next());
        assertEquals("unexpected string", c, referenceIter.next());
    }
    
    /**
     * Tests the inverse comparator for <tt>Long</tt>s.
     * 
     * @throws Exception if an error occurs
     */
    public void testInverseLongComparator() throws Exception {
        Long one = (long) 1;
        Long two = (long) 2;
        Long three = (long) 3;
        String a = "a";
        String b = "b";
        String c = "c";
        Map longMap = new TreeMap(Comparators.inverseLongComparator());
        longMap.put(one, a);
        longMap.put(two, b);
        longMap.put(three, c);

        Iterator iter = longMap.values().iterator();
        assertEquals("unexpected string", c, iter.next());
        assertEquals("unexpected string", b, iter.next());
        assertEquals("unexpected string", a, iter.next());
    }
    
    /**
     * Tests the method for comparing two longs.  This method is necessary in
     * LimeWire because the Long compareTo method was only added in Java 1.2.
     * The test works by making sure that the values returned are consistent
     * with the Long compareTo method.
     * 
     * @throws Exception if an error occurs
     */
    public void testLongCompareTo() throws Exception {
        Long firstLong = (long) 1;
        Long secondLong = (long) 1;
        
        // check the case where the arguments are equal.
        int result = Comparators.longCompareTo(firstLong, secondLong);
        assertEquals("unexpected value", 0, result);

        // make sure we get the same value as the Long class.
        assertEquals("unexpected value", result,
            firstLong.compareTo(secondLong));
        
        // check the case where the first argument is less than the second
        // argument
        firstLong = (long) 0;
        result = Comparators.longCompareTo(firstLong, secondLong);
        assertLessThan("unexpected value", 0, result);
        // make sure we get the same value as the Long class.
        
        assertEquals("unexpected value", result, 
            firstLong.compareTo(secondLong));

        // check the case where the second argument is less than the first
        // argument
        firstLong = (long) 4;
        result = Comparators.longCompareTo(firstLong, secondLong);
        assertGreaterThan("unexpected value", 0, result);

        // make sure we get the same value as the Long class.
        assertEquals("unexpected value", result, 
            firstLong.compareTo(secondLong));
        
    }
}
