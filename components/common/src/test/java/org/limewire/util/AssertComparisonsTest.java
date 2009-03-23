package org.limewire.util;

import junit.framework.AssertionFailedError;
import junit.framework.Test;

/**
 * Test that comparisons are correct.
 */
@SuppressWarnings("all")
public class AssertComparisonsTest extends BaseTestCase {
    
    public AssertComparisonsTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(AssertComparisonsTest.class);
    }
    
    public void testIntegerComparisons() {
        
        try {
            assertGreaterThan((int)1, (int)0);
            fail("0 is not greater than 1.");
        } catch (AssertionFailedError ignored) {}
        
        try {
            assertGreaterThan((int)1, (int)1);
            fail("1 is not greater than 1.");
        } catch (AssertionFailedError ignored) {}        
        
        try {
            assertGreaterThan("string", (int)1, (int)0);
            fail("0 is not greater than 1.");
        } catch (AssertionFailedError ignored) {}
        
        try {
            assertGreaterThan("string", (int)1, (int)1);
            fail("1 is not greater than 1.");
        } catch (AssertionFailedError ignored) {}        
        
        assertGreaterThan((int)0, (int)1);
        assertGreaterThan("string", (int)0, (int)1);
        
        try {
            assertLessThan((int)0, (int)1);
            fail("1 is not less than 0.");
        } catch (AssertionFailedError ignored) {}
        
        try {
            assertLessThan("string", (int)0, (int)1);
            fail("1 is not less than 0.");
        } catch(AssertionFailedError ignored) {}
        
        assertLessThan((int)1, (int)0);
        assertLessThan("string", (int)1, (int)0);
        
        try {
            assertGreaterThanOrEquals((int)1, (int)0);
            fail("0 is not greater than or equal to 1");
        } catch(AssertionFailedError ignored) {}
        
        try {
            assertGreaterThanOrEquals((int)1, (int)0);
            fail("0 is not greater than or equal to 1");
        } catch(AssertionFailedError ignored) {}
        
        assertGreaterThanOrEquals((int)0, (int)0);
        assertGreaterThanOrEquals((int)0, (int)1);
        assertGreaterThanOrEquals("string", (int)0, (int)0);
        assertGreaterThanOrEquals("string", (int)0, (int)1);
        
        try {
            assertLessThanOrEquals((int)0, (int)1);
            fail("1 is not less than or equal to 0");
        } catch(AssertionFailedError ignored) {}
        
        try {
            assertLessThanOrEquals("string", (int)0, (int)1);
            fail("1 is not less than or equal to 0");
        } catch(AssertionFailedError ignored) {}
        
        assertLessThanOrEquals((int)0, (int)0);
        assertLessThanOrEquals((int)1, (int)0);
        assertLessThanOrEquals("string", (int)0, (int)0);
        assertLessThanOrEquals("string", (int)1, (int)0);
        
        try {
            assertNotEquals((int)0, (int)0);
            fail("0 is equal to 0");
        } catch(AssertionFailedError ignored) {}
        
        assertNotEquals((int)0, (int)1);        
    }
    
    
    public void testDoubleComparisons() {
        
        try {
            assertGreaterThan((double)1, (double)0);
            fail("0 is not greater than 1.");
        } catch (AssertionFailedError ignored) {}
        
        try {
            assertGreaterThan((double)1, (double)1);
            fail("1 is not greater than 1.");
        } catch (AssertionFailedError ignored) {}        
        
        try {
            assertGreaterThan("string", (double)1, (double)0);
            fail("0 is not greater than 1.");
        } catch (AssertionFailedError ignored) {}
        
        try {
            assertGreaterThan("string", (double)1, (double)1);
            fail("1 is not greater than 1.");
        } catch (AssertionFailedError ignored) {}        
        
        assertGreaterThan((double)0, (double)1);
        assertGreaterThan("string", (double)0, (double)1);
        
        try {
            assertLessThan((double)0, (double)1);
            fail("1 is not less than 0.");
        } catch (AssertionFailedError ignored) {}
        
        try {
            assertLessThan("string", (double)0, (double)1);
            fail("1 is not less than 0.");
        } catch(AssertionFailedError ignored) {}
        
        assertLessThan((double)1, (double)0);
        assertLessThan("string", (double)1, (double)0);
        
        try {
            assertGreaterThanOrEquals((double)1, (double)0);
            fail("0 is not greater than or equal to 1");
        } catch(AssertionFailedError ignored) {}
        
        try {
            assertGreaterThanOrEquals((double)1, (double)0);
            fail("0 is not greater than or equal to 1");
        } catch(AssertionFailedError ignored) {}
        
        assertGreaterThanOrEquals((double)0, (double)0);
        assertGreaterThanOrEquals((double)0, (double)1);
        assertGreaterThanOrEquals("string", (double)0, (double)0);
        assertGreaterThanOrEquals("string", (double)0, (double)1);
        
        try {
            assertLessThanOrEquals((double)0, (double)1);
            fail("1 is not less than or equal to 0");
        } catch(AssertionFailedError ignored) {}
        
        try {
            assertLessThanOrEquals("string", (double)0, (double)1);
            fail("1 is not less than or equal to 0");
        } catch(AssertionFailedError ignored) {}
        
        assertLessThanOrEquals((double)0, (double)0);
        assertLessThanOrEquals((double)1, (double)0);
        assertLessThanOrEquals("string", (double)0, (double)0);
        assertLessThanOrEquals("string", (double)1, (double)0);
        
        try {
            assertNotEquals((double)0, (double)0);
            fail("0 is equal to 0");
        } catch(AssertionFailedError ignored) {}
        
        assertNotEquals((double)0, (double)1);        
    }
    
    public void testShortComparisons() {
        
        try {
            assertGreaterThan((short)1, (short)0);
            fail("0 is not greater than 1.");
        } catch (AssertionFailedError ignored) {}
        
        try {
            assertGreaterThan((short)1, (short)1);
            fail("1 is not greater than 1.");
        } catch (AssertionFailedError ignored) {}        
        
        try {
            assertGreaterThan("string", (short)1, (short)0);
            fail("0 is not greater than 1.");
        } catch (AssertionFailedError ignored) {}
        
        try {
            assertGreaterThan("string", (short)1, (short)1);
            fail("1 is not greater than 1.");
        } catch (AssertionFailedError ignored) {}        
        
        assertGreaterThan((short)0, (short)1);
        assertGreaterThan("string", (short)0, (short)1);
        
        try {
            assertLessThan((short)0, (short)1);
            fail("1 is not less than 0.");
        } catch (AssertionFailedError ignored) {}
        
        try {
            assertLessThan("string", (short)0, (short)1);
            fail("1 is not less than 0.");
        } catch(AssertionFailedError ignored) {}
        
        assertLessThan((short)1, (short)0);
        assertLessThan("string", (short)1, (short)0);
        
        try {
            assertGreaterThanOrEquals((short)1, (short)0);
            fail("0 is not greater than or equal to 1");
        } catch(AssertionFailedError ignored) {}
        
        try {
            assertGreaterThanOrEquals((short)1, (short)0);
            fail("0 is not greater than or equal to 1");
        } catch(AssertionFailedError ignored) {}
        
        assertGreaterThanOrEquals((short)0, (short)0);
        assertGreaterThanOrEquals((short)0, (short)1);
        assertGreaterThanOrEquals("string", (short)0, (short)0);
        assertGreaterThanOrEquals("string", (short)0, (short)1);
        
        try {
            assertLessThanOrEquals((short)0, (short)1);
            fail("1 is not less than or equal to 0");
        } catch(AssertionFailedError ignored) {}
        
        try {
            assertLessThanOrEquals("string", (short)0, (short)1);
            fail("1 is not less than or equal to 0");
        } catch(AssertionFailedError ignored) {}
        
        assertLessThanOrEquals((short)0, (short)0);
        assertLessThanOrEquals((short)1, (short)0);
        assertLessThanOrEquals("string", (short)0, (short)0);
        assertLessThanOrEquals("string", (short)1, (short)0);
        
        try {
            assertNotEquals((short)0, (short)0);
            fail("0 is equal to 0");
        } catch(AssertionFailedError ignored) {}
        
        assertNotEquals((short)0, (short)1);        
    }
    
    public void testLongComparisons() {
        
        try {
            assertGreaterThan((long)1, (long)0);
            fail("0 is not greater than 1.");
        } catch (AssertionFailedError ignored) {}
        
        try {
            assertGreaterThan((long)1, (long)1);
            fail("1 is not greater than 1.");
        } catch (AssertionFailedError ignored) {}        
        
        try {
            assertGreaterThan("string", (long)1, (long)0);
            fail("0 is not greater than 1.");
        } catch (AssertionFailedError ignored) {}
        
        try {
            assertGreaterThan("string", (long)1, (long)1);
            fail("1 is not greater than 1.");
        } catch (AssertionFailedError ignored) {}        
        
        assertGreaterThan((long)0, (long)1);
        assertGreaterThan("string", (long)0, (long)1);
        
        try {
            assertLessThan((long)0, (long)1);
            fail("1 is not less than 0.");
        } catch (AssertionFailedError ignored) {}
        
        try {
            assertLessThan("string", (long)0, (long)1);
            fail("1 is not less than 0.");
        } catch(AssertionFailedError ignored) {}
        
        assertLessThan((long)1, (long)0);
        assertLessThan("string", (long)1, (long)0);
        
        try {
            assertGreaterThanOrEquals((long)1, (long)0);
            fail("0 is not greater than or equal to 1");
        } catch(AssertionFailedError ignored) {}
        
        try {
            assertGreaterThanOrEquals((long)1, (long)0);
            fail("0 is not greater than or equal to 1");
        } catch(AssertionFailedError ignored) {}
        
        assertGreaterThanOrEquals((long)0, (long)0);
        assertGreaterThanOrEquals((long)0, (long)1);
        assertGreaterThanOrEquals("string", (long)0, (long)0);
        assertGreaterThanOrEquals("string", (long)0, (long)1);
        
        try {
            assertLessThanOrEquals((long)0, (long)1);
            fail("1 is not less than or equal to 0");
        } catch(AssertionFailedError ignored) {}
        
        try {
            assertLessThanOrEquals("string", (long)0, (long)1);
            fail("1 is not less than or equal to 0");
        } catch(AssertionFailedError ignored) {}
        
        assertLessThanOrEquals((long)0, (long)0);
        assertLessThanOrEquals((long)1, (long)0);
        assertLessThanOrEquals("string", (long)0, (long)0);
        assertLessThanOrEquals("string", (long)1, (long)0);
        
        try {
            assertNotEquals((long)0, (long)0);
            fail("0 is equal to 0");
        } catch(AssertionFailedError ignored) {}
        
        assertNotEquals((long)0, (long)1);        
    }
    
    public void testFloatComparisons() {
        
        try {
            assertGreaterThan((float)1, (float)0);
            fail("0 is not greater than 1.");
        } catch (AssertionFailedError ignored) {}
        
        try {
            assertGreaterThan((float)1, (float)1);
            fail("1 is not greater than 1.");
        } catch (AssertionFailedError ignored) {}        
        
        try {
            assertGreaterThan("string", (float)1, (float)0);
            fail("0 is not greater than 1.");
        } catch (AssertionFailedError ignored) {}
        
        try {
            assertGreaterThan("string", (float)1, (float)1);
            fail("1 is not greater than 1.");
        } catch (AssertionFailedError ignored) {}        
        
        assertGreaterThan((float)0, (float)1);
        assertGreaterThan("string", (float)0, (float)1);
        
        try {
            assertLessThan((float)0, (float)1);
            fail("1 is not less than 0.");
        } catch (AssertionFailedError ignored) {}
        
        try {
            assertLessThan("string", (float)0, (float)1);
            fail("1 is not less than 0.");
        } catch(AssertionFailedError ignored) {}
        
        assertLessThan((float)1, (float)0);
        assertLessThan("string", (float)1, (float)0);
        
        try {
            assertGreaterThanOrEquals((float)1, (float)0);
            fail("0 is not greater than or equal to 1");
        } catch(AssertionFailedError ignored) {}
        
        try {
            assertGreaterThanOrEquals((float)1, (float)0);
            fail("0 is not greater than or equal to 1");
        } catch(AssertionFailedError ignored) {}
        
        assertGreaterThanOrEquals((float)0, (float)0);
        assertGreaterThanOrEquals((float)0, (float)1);
        assertGreaterThanOrEquals("string", (float)0, (float)0);
        assertGreaterThanOrEquals("string", (float)0, (float)1);
        
        try {
            assertLessThanOrEquals((float)0, (float)1);
            fail("1 is not less than or equal to 0");
        } catch(AssertionFailedError ignored) {}
        
        try {
            assertLessThanOrEquals("string", (float)0, (float)1);
            fail("1 is not less than or equal to 0");
        } catch(AssertionFailedError ignored) {}
        
        assertLessThanOrEquals((float)0, (float)0);
        assertLessThanOrEquals((float)1, (float)0);
        assertLessThanOrEquals("string", (float)0, (float)0);
        assertLessThanOrEquals("string", (float)1, (float)0);
        
        try {
            assertNotEquals((float)0, (float)0);
            fail("0 is equal to 0");
        } catch(AssertionFailedError ignored) {}
        
        assertNotEquals((float)0, (float)1);        
    }
    
    public void testByteComparisons() {
        
        try {
            assertGreaterThan((byte)1, (byte)0);
            fail("0 is not greater than 1.");
        } catch (AssertionFailedError ignored) {}
        
        try {
            assertGreaterThan((byte)1, (byte)1);
            fail("1 is not greater than 1.");
        } catch (AssertionFailedError ignored) {}        
        
        try {
            assertGreaterThan("string", (byte)1, (byte)0);
            fail("0 is not greater than 1.");
        } catch (AssertionFailedError ignored) {}
        
        try {
            assertGreaterThan("string", (byte)1, (byte)1);
            fail("1 is not greater than 1.");
        } catch (AssertionFailedError ignored) {}        
        
        assertGreaterThan((byte)0, (byte)1);
        assertGreaterThan("string", (byte)0, (byte)1);
        
        try {
            assertLessThan((byte)0, (byte)1);
            fail("1 is not less than 0.");
        } catch (AssertionFailedError ignored) {}
        
        try {
            assertLessThan("string", (byte)0, (byte)1);
            fail("1 is not less than 0.");
        } catch(AssertionFailedError ignored) {}
        
        assertLessThan((byte)1, (byte)0);
        assertLessThan("string", (byte)1, (byte)0);
        
        try {
            assertGreaterThanOrEquals((byte)1, (byte)0);
            fail("0 is not greater than or equal to 1");
        } catch(AssertionFailedError ignored) {}
        
        try {
            assertGreaterThanOrEquals((byte)1, (byte)0);
            fail("0 is not greater than or equal to 1");
        } catch(AssertionFailedError ignored) {}
        
        assertGreaterThanOrEquals((byte)0, (byte)0);
        assertGreaterThanOrEquals((byte)0, (byte)1);
        assertGreaterThanOrEquals("string", (byte)0, (byte)0);
        assertGreaterThanOrEquals("string", (byte)0, (byte)1);
        
        try {
            assertLessThanOrEquals((byte)0, (byte)1);
            fail("1 is not less than or equal to 0");
        } catch(AssertionFailedError ignored) {}
        
        try {
            assertLessThanOrEquals("string", (byte)0, (byte)1);
            fail("1 is not less than or equal to 0");
        } catch(AssertionFailedError ignored) {}
        
        assertLessThanOrEquals((byte)0, (byte)0);
        assertLessThanOrEquals((byte)1, (byte)0);
        assertLessThanOrEquals("string", (byte)0, (byte)0);
        assertLessThanOrEquals("string", (byte)1, (byte)0);
        
        try {
            assertNotEquals((byte)0, (byte)0);
            fail("0 is equal to 0");
        } catch(AssertionFailedError ignored) {}
        
        assertNotEquals((byte)0, (byte)1);        
    }
    
    public void testStringComparisons() {
        
        try {
            assertGreaterThan("b", "a");
            fail("a is not greater than b.");
        } catch (AssertionFailedError ignored) {}
        
        try {
            assertGreaterThan("b", "b");
            fail("b is not greater than b.");
        } catch (AssertionFailedError ignored) {}        
        
        try {
            assertGreaterThan("string", "b", "a");
            fail("a is not greater than b.");
        } catch (AssertionFailedError ignored) {}
        
        try {
            assertGreaterThan("string", "b", "b");
            fail("b is not greater than b.");
        } catch (AssertionFailedError ignored) {}        
        
        assertGreaterThan("a", "b");
        assertGreaterThan("string", "a", "b");
        
        try {
            assertLessThan("a", "b");
            fail("b is not less than a.");
        } catch (AssertionFailedError ignored) {}
        
        try {
            assertLessThan("string", "a", "b");
            fail("b is not less than a.");
        } catch(AssertionFailedError ignored) {}
        
        assertLessThan("b", "a");
        assertLessThan("string", "b", "a");
        
        try {
            assertGreaterThanOrEquals("b", "a");
            fail("a is not greater than or equal to b");
        } catch(AssertionFailedError ignored) {}
        
        try {
            assertGreaterThanOrEquals("b", "a");
            fail("a is not greater than or equal to b");
        } catch(AssertionFailedError ignored) {}
        
        assertGreaterThanOrEquals("a", "a");
        assertGreaterThanOrEquals("a", "b");
        assertGreaterThanOrEquals("string", "a", "a");
        assertGreaterThanOrEquals("string", "a", "b");
        
        try {
            assertLessThanOrEquals("a", "b");
            fail("b is not less than or equal to a");
        } catch(AssertionFailedError ignored) {}
        
        try {
            assertLessThanOrEquals("string", "a", "b");
            fail("b is not less than or equal to a");
        } catch(AssertionFailedError ignored) {}
        
        assertLessThanOrEquals("a", "a");
        assertLessThanOrEquals("b", "a");
        assertLessThanOrEquals("string", "a", "a");
        assertLessThanOrEquals("string", "b", "a");
        
        try {
            assertNotEquals("a", "a");
            assertNotEquals("string", "a", "a");
            fail("a is equal to a");
        } catch(AssertionFailedError ignored) {}
        
        assertNotEquals("a", "b");
        assertNotEquals("string", "a", "b");
    }
    
    public void testInstanceof() {
        try {
            assertInstanceof(Number.class, new Object());
            assertInstanceof("string", Number.class, new Object());
            fail("object is not an instance of number");
        } catch(AssertionFailedError ignored) {}
        
        try {
            assertInstanceof(java.lang.Comparable.class, new Object());
            assertInstanceof("stg", java.lang.Comparable.class, new Object());
            fail("object is not an instance of comparable");
        } catch(AssertionFailedError ignored) {}
        
        assertInstanceof(Object.class, new Object());
        assertInstanceof("string", Object.class, new Object());
        assertInstanceof(Number.class, new Integer(1));
        assertInstanceof("string", Number.class, new Integer(1));
        assertInstanceof(java.lang.Comparable.class, new Integer(1));
        assertInstanceof("string", java.lang.Comparable.class, new Integer(1));
    }
    
    public void testNotInstanceof() {
        try {
            assertNotInstanceof(Object.class, new Object());
            assertNotInstanceof("string", Object.class, new Object());
            fail("object is an instanceof object");
        } catch(AssertionFailedError ignored) {}
        
        try {
            assertNotInstanceof(Number.class, new Integer(1));
            assertNotInstanceof("string", Number.class, new Integer(1));
            fail("integer is an instanceof number");
        } catch(AssertionFailedError ignored) {}
        
        assertNotInstanceof(Number.class, new Object());
        assertNotInstanceof("string", Number.class, new Object());
        assertNotInstanceof(java.lang.Comparable.class, new Object());
        assertNotInstanceof("stg", java.lang.Comparable.class, new Object());
    }

    public void testNullChecks() {
        try {
            assertNull(new Object());
            assertNull("string", new Object());
            fail("an object isn't null.");
        } catch (AssertionFailedError ignored) {}
        
        try {
            assertNotNull(null);
            assertNotNull("string", null);
            fail("null isn't not null.");
        } catch (AssertionFailedError ignored) {}
        
        assertNotNull(new Object());
        assertNotNull("string", new Object());
        assertNull(null);
        assertNull("string", null);
    }
    
    public void testBadComparisons() {
        try {
            assertGreaterThan(null, null);
            assertGreaterThan("string", null, null);
            fail("should not have been able to compare nulls.");
        } catch (AssertionFailedError ignored) {}
        
        try {
            assertGreaterThan(new Object(), new Object());
            assertGreaterThan("string", new Object(), new Object());
            fail("should not have been able to compare nonComparable objects");
        } catch(AssertionFailedError ignored) {}
    }
}