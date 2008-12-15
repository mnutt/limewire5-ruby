package org.limewire.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;
    
/**
 * A set of assert comparisons to check greater than / less than
 * situations, instanceof and not equals.
 */
@SuppressWarnings("unchecked")
public abstract class AssertComparisons extends TestCase {
        
    private static final int BLANK = -1;
    private static final int LESS_THAN = 0;
    private static final int GREATER_THAN = 1;
    private static final int LESS_THAN_OR_EQUALS = 2;
    private static final int GREATER_THAN_OR_EQUALS = 3;
    private static final int INSTANCE_OF = 4;
    private static final int NOT_EQUAL = 5;
    private static final int NOT_SAME = 6;
    private static final int NOT_INSTANCE_OF = 7;
    private static final int CONTAINS = 8;
    private static final int NOT_CONTAINS = 9;
    private static final int COMPARE_EQUALS = 10;
    
    /**
     * Named constructor.
     */
    public AssertComparisons(String name) {
        super(name);
    }
    
    /**
     * Assert that the two arrays are equal.
     */
    static public void assertEquals(boolean[] expected, boolean[] actual) {
        assertEquals(null, expected, actual);
    }
    
    /**
     * Assert that the two arrays are equal.  If not, an AssertionFailedError
     * is thrown with the given messages.
     */
    static public void assertEquals(String msg, boolean[] expected, boolean[] actual) {
        if(!Arrays.equals(expected, actual))
            fail(formatComparison(BLANK, msg, asList(expected), asList(actual)));
    }
    
    /**
     * Assert that the two arrays are equal.
     */
    static public void assertEquals(byte[] expected, byte[] actual) {
        assertEquals(null, expected, actual);
    }
    
    /**
     * Assert that the two arrays are equal.  If not, an AssertionFailedError
     * is thrown with the given messages.
     */
    static public void assertEquals(String msg, byte[] expected, byte[] actual) {
        if(!Arrays.equals(expected, actual))
            fail(formatComparison(BLANK, msg, asList(expected), asList(actual)));
    }
    
    /**
     * Assert that the two arrays are equal.
     */
    static public void assertEquals(byte[] expected, byte[] actual, int actualOff, int actualLen) {
        assertEquals(null, expected, actual, actualOff, actualLen);
    }
    
    /**
     * Assert that the two arrays are equal.  If not, an AssertionFailedError
     * is thrown with the given messages.
     */
    static public void assertEquals(String msg, byte[] expected, byte[] actual, int actualOff, int actualLen) {
        byte[] newActual = new byte[actualLen];
        for(int i = 0; i < actualLen; i++)
            newActual[i] = actual[actualOff + i];
        
        if(!Arrays.equals(expected, newActual))
            fail(formatComparison(BLANK, msg, asList(expected), asList(newActual)));
    }
    
    /**
     * Assert that the two arrays are equal.
     */
    static public void assertEquals(byte[] expected, int expectedOff, int expectedLen, byte[] actual) {
        assertEquals(null, expected, expectedOff, expectedLen, actual);
    }
    
    /**
     * Assert that the two arrays are equal.  If not, an AssertionFailedError
     * is thrown with the given messages.
     */
    static public void assertEquals(String msg, byte[] expected, int expectedOff, int expectedLen, byte[] actual) {
        byte[] newExpected = new byte[expectedLen];
        for(int i = 0; i < expectedLen; i++)
            newExpected[i] = expected[expectedOff + i];
        
        if(!Arrays.equals(newExpected, actual))
            fail(formatComparison(BLANK, msg, asList(newExpected), asList(actual)));
    }       
    
    /**
     * Assert that the two arrays are equal.
     */
    static public void assertEquals(byte[] expected, int expectedOff, int expectedLen, byte[] actual, int actualOff, int actualLen) {
        assertEquals(null, expected, expectedOff, expectedLen, actual, actualOff, actualLen);
    }
    
    /**
     * Assert that the two arrays are equal.  If not, an AssertionFailedError
     * is thrown with the given messages.
     */
    static public void assertEquals(String msg, byte[] expected, int expectedOff, int expectedLen, byte[] actual, int actualOff, int actualLen) {
        byte[] newActual = new byte[actualLen];
        for(int i = 0; i < actualLen; i++)
            newActual[i] = actual[actualOff + i];
        
        byte[] newExpected = new byte[expectedLen];
        for(int i = 0; i < expectedLen; i++)
            newExpected[i] = expected[expectedOff + i];
        
        if(!Arrays.equals(newExpected, newActual))
            fail(formatComparison(BLANK, msg, asList(newExpected), asList(newActual)));
    }   
    

    /**
     * Assert that the two arrays are equal.
     */
    static public void assertEquals(char[] expected, char[] actual) {
        assertEquals(null, expected, actual);
    }
    
    /**
     * Assert that the two arrays are equal.  If not, an AssertionFailedError
     * is thrown with the given messages.
     */
    static public void assertEquals(String msg, char[] expected, char[] actual) {
        if(!Arrays.equals(expected, actual))
            fail(formatComparison(BLANK, msg, asList(expected), asList(actual)));
    }
    
    /**
     * Assert that the two arrays are equal.
     */
    static public void assertEquals(double[] expected, double[] actual) {
        assertEquals(null, expected, actual);
    }
    
    /**
     * Assert that the two arrays are equal.  If not, an AssertionFailedError
     * is thrown with the given messages.
     */
    static public void assertEquals(String msg, double[] expected, double[] actual) {
        if(!Arrays.equals(expected, actual))
            fail(formatComparison(BLANK, msg, asList(expected), asList(actual)));
    }
    
    /**
     * Assert that the two arrays are equal.
     */
    static public void assertEquals(int[] expected, int[] actual) {
        assertEquals(null, expected, actual);
    }
    
    /**
     * Assert that the two arrays are equal.  If not, an AssertionFailedError
     * is thrown with the given messages.
     */
    static public void assertEquals(String msg, int[] expected, int[] actual) {
        if(!Arrays.equals(expected, actual))
            fail(formatComparison(BLANK, msg, asList(expected), asList(actual)));
    }
    
    /**
     * Assert that the two arrays are equal.
     */
    static public void assertEquals(long[] expected, long[] actual) {
        assertEquals(null, expected, actual);
    }
    
    /**
     * Assert that the two arrays are equal.  If not, an AssertionFailedError
     * is thrown with the given messages.
     */
    static public void assertEquals(String msg, long[] expected, long[] actual) {
        if(!Arrays.equals(expected, actual))
            fail(formatComparison(BLANK, msg, asList(expected), asList(actual)));
    }
    
    /**
     * Assert that the two arrays are equal.
     */
    static public void assertEquals(Object[] expected, Object[] actual) {
        assertEquals(null, expected, actual);
    }
    
    /**
     * Assert that the two arrays are equal.  If not, an AssertionFailedError
     * is thrown with the given messages.
     */
    static public void assertEquals(String msg, Object[] expected, Object[] actual) {
        if(!Arrays.equals(expected, actual))
            fail(formatComparison(BLANK, msg, Arrays.asList(expected), Arrays.asList(actual)));
    }
    
    /**
     * Assert that the two arrays are equal.
     */
    static public void assertEquals(short[] expected, short[] actual) {
        assertEquals(null, expected, actual);
    }
    
    /**
     * Assert that the two arrays are equal.  If not, an AssertionFailedError
     * is thrown with the given messages.
     */
    static public void assertEquals(String msg, short[] expected, short[] actual) {
        if(!Arrays.equals(expected, actual))
            fail(formatComparison(BLANK, msg, asList(expected), asList(actual)));
    }                        
    
    /**
     * Assert that the collection contains the object.
     */
    static public void assertContains(Collection col, Object obj) {
        assertContains(null, col, obj);
    }
    
    /**
     * Asserts that the collection contains the object.  If it doesn't,
     * an AssertionFailedError is thrown with the given message.
     */
    static public void assertContains(String msg, Collection col, Object obj) {
        if(!col.contains(obj))
            fail(formatComparison(CONTAINS, msg, obj, col));
    }
    
    /**
     * Assert that the collection does not contain the object.
     */
    static public void assertNotContains(Collection col, Object obj) {
        assertNotContains(null, col, obj);
    }
    
    /**
     * Asserts that the collection does not contain the object.  If it does
     * an AssertionFailedError is thrown with the given message.
     */
    static public void assertNotContains(String msg, Collection col, Object obj) {
        if(col.contains(obj))
            fail(formatComparison(NOT_CONTAINS, msg, obj, col));
    }

    /**
     * Asserts that actual is an instance of class expected. If it isn't,
     * an AssertionFailedError is thrown.
     */
    static public void assertInstanceof(Class expected, Object actual) {
        assertInstanceof(null, expected, actual);
    }
    
    /**
     * Asserts that actual is an instance of the class expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */
    static public void assertInstanceof(String msg, Class expected, Object actual) {
        if (!instanceofCheck(expected, actual))
            fail(formatComparison(INSTANCE_OF, msg, 
                    expected.getName(), actual == null ? "null" : actual.getClass().getName()));
    }
    
    /**
     * Asserts that actual is not an instanceof the expected class.  If it is,
     * an AssertionFailedError is thrown.
     */
    static public void assertNotInstanceof(Class expected, Object actual) {
        assertNotInstanceof(null, expected, actual);
    }
    
    /**
     * Asserts that actual is an not instance of the class expected.  If it is,
     * an AssertionFailedError is thrown with the given message.
     */
    static public void assertNotInstanceof(String msg, Class expected, Object actual) {
        if(instanceofCheck(expected, actual))
            fail(formatComparison(NOT_INSTANCE_OF, msg,
                expected.getName(), actual == null ? "null" : actual.getClass().getName()));
    }
    
    /**
     * Asserts that an object is null.
     */
    static public void assertNull(Object actual) {
        assertNull(null, actual);
    }
    
    /**
     * Asserts that an object is null.  If it isn't, throws an AssertionFailedError
     * with the given message.
     */
    static public void assertNull(String msg, Object actual) {
        if( actual != null)
            fail( formatComparison(BLANK, msg, null, actual) );
    }
    
    /**
     * Asserts that an object is not null.
     */
    static public void assertNotNull(Object actual) {
        assertNotNull(null, actual);
    }
    
    /**
     * Asserts that an object is not null.  If it is, throws an AssertionFailedError
     * with the given message.
     */
    static public void assertNotNull(String msg, Object actual) {
        if(actual == null)
            fail( formatComparison(NOT_EQUAL, msg, null, actual) );
    }
    
    /**
     * Asserts that actual is not equal to expected.  If they are equal,
     * an AssertionFailedError is thrown.
     */
    static public void assertNotEquals(Object expected, Object actual) {
        assertNotEquals(null, expected, actual);
    }
    
    /**
     * Asserts that actual is not equal to expected.  If they are equal,
     * an AssertionFailedError is thrown with the given message.
     */
    @SuppressWarnings("null")
    static public void assertNotEquals(String msg, Object expected, Object actual) {
        if ( actual == null && expected != null )
            return;
        if ( expected == null && actual != null )
            return;            
            
        if ( expected == null && actual == null || actual.equals(expected) )
            fail(formatComparison(NOT_EQUAL, msg, expected, actual));
    }
    
   /**
     * Assertes that actual is not equal to expected.  if they are equal,
     * an AssertionFailedError is thrown.
     */    
    static public void assertNotEquals(long expected, long actual) {
        assertNotEquals( new Long(expected), new Long(actual) );
    }
    
    /**
     * Assertes that actual is not equal to expected.  if they are equal,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertNotEquals(String msg, long expected, long actual) {
        assertNotEquals( msg, new Long(expected), new Long(actual) );
    }
    
    /**
     * Assertes that actual is not equal to expected.  if they are equal,
     * an AssertionFailedError is thrown.
     */    
    static public void assertNotEquals(short expected, short actual) {
        assertNotEquals( new Short(expected), new Short(actual) );
    }
    
    /**
     * Assertes that actual is not equal to expected.  if they are equal,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertNotEquals(String msg, short expected, short actual) {
        assertNotEquals( msg, new Short(expected), new Short(actual) );
    }

    /**
     * Assertes that actual is not equal to expected.  if they are equal,
     * an AssertionFailedError is thrown.
     */    
    static public void assertNotEquals(int expected, int actual) {
        assertNotEquals( new Integer(expected), new Integer(actual) );
    }
    
    /**
     * Assertes that actual is not equal to expected.  if they are equal,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertNotEquals(String msg, int expected, int actual) {
        assertNotEquals( msg, new Integer(expected), new Integer(actual) );
    }
    
    /**
     * Assertes that actual is not equal to expected.  if they are equal,
     * an AssertionFailedError is thrown.
     */    
    static public void assertNotEquals(double expected, double actual) {
        assertNotEquals( new Double(expected), new Double(actual) );
    }
    
    /**
     * Assertes that actual is not equal to expected.  if they are equal,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertNotEquals(String msg, double expected, double actual) {
        assertNotEquals( msg, new Double(expected), new Double(actual) );
    }
    
    /**
     * Assertes that actual is not equal to expected.  if they are equal,
     * an AssertionFailedError is thrown.
     */    
    static public void assertNotEquals(float expected, float actual) {
        assertNotEquals( new Float(expected), new Float(actual) );
    }
    
    /**
     * Assertes that actual is not equal to expected.  if they are equal,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertNotEquals(String msg, float expected, float actual) {
        assertNotEquals( msg, new Float(expected), new Float(actual) );
    }
    
    /**
     * Assertes that actual is not equal to expected.  if they are equal,
     * an AssertionFailedError is thrown.
     */    
    static public void assertNotEquals(byte expected, byte actual) {
        assertNotEquals( new Byte(expected), new Byte(actual) );
    }
    
    /**
     * Assertes that actual is not equal to expected.  if they are equal,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertNotEquals(String msg, byte expected, byte actual) {
        assertNotEquals( msg, new Byte(expected), new Byte(actual) );
    }

    /**
     * Assert that the two arrays are not equal.
     */
    static public void assertNotEquals(boolean[] expected, boolean[] actual) {
        assertNotEquals(null, expected, actual);
    }
    
    /**
     * Assert that the two arrays are not equal.  If not, an AssertionFailedError
     * is thrown with the given messages.
     */
    static public void assertNotEquals(String msg, boolean[] expected, boolean[] actual) {
        if(Arrays.equals(expected, actual))
            fail(formatComparison(NOT_EQUAL, msg, asList(expected), asList(actual)));
    }
    
    /**
     * Assert that the two arrays are not equal.
     */
    static public void assertNotEquals(byte[] expected, byte[] actual) {
        assertNotEquals(null, expected, actual);
    }
    
    /**
     * Assert that the two arrays are not equal.  If not, an AssertionFailedError
     * is thrown with the given messages.
     */
    static public void assertNotEquals(String msg, byte[] expected, byte[] actual) {
        if(Arrays.equals(expected, actual))
            fail(formatComparison(NOT_EQUAL, msg, asList(expected), asList(actual)));
    }
    
    /**
     * Assert that the two arrays are not equal.
     */
    static public void assertNotEquals(byte[] expected, byte[] actual, int actualOff, int actualLen) {
        assertNotEquals(null, expected, actual, actualOff, actualLen);
    }
    
    /**
     * Assert that the two arrays are not equal.  If not, an AssertionFailedError
     * is thrown with the given messages.
     */
    static public void assertNotEquals(String msg, byte[] expected, byte[] actual, int actualOff, int actualLen) {
        byte[] newActual = new byte[actualLen];
        for(int i = 0; i < actualLen; i++)
            newActual[i] = actual[actualOff + i];
        
        if(Arrays.equals(expected, newActual))
            fail(formatComparison(NOT_EQUAL, msg, asList(expected), asList(newActual)));
    }
    
    /**
     * Assert that the two arrays are not equal.
     */
    static public void assertNotEquals(byte[] expected, int expectedOff, int expectedLen, byte[] actual) {
        assertNotEquals(null, expected, expectedOff, expectedLen, actual);
    }
    
    /**
     * Assert that the two arrays are not equal.  If not, an AssertionFailedError
     * is thrown with the given messages.
     */
    static public void assertNotEquals(String msg, byte[] expected, int expectedOff, int expectedLen, byte[] actual) {
        byte[] newExpected = new byte[expectedLen];
        for(int i = 0; i < expectedLen; i++)
            newExpected[i] = expected[expectedOff + i];
        
        if(Arrays.equals(newExpected, actual))
            fail(formatComparison(NOT_EQUAL, msg, asList(newExpected), asList(actual)));
    }       
    
    /**
     * Assert that the two arrays are not equal.
     */
    static public void assertNotEquals(byte[] expected, int expectedOff, int expectedLen, byte[] actual, int actualOff, int actualLen) {
        assertNotEquals(null, expected, expectedOff, expectedLen, actual, actualOff, actualLen);
    }
    
    /**
     * Assert that the two arrays are not equal.  If not, an AssertionFailedError
     * is thrown with the given messages.
     */
    static public void assertNotEquals(String msg, byte[] expected, int expectedOff, int expectedLen, byte[] actual, int actualOff, int actualLen) {
        byte[] newActual = new byte[actualLen];
        for(int i = 0; i < actualLen; i++)
            newActual[i] = actual[actualOff + i];
        
        byte[] newExpected = new byte[expectedLen];
        for(int i = 0; i < expectedLen; i++)
            newExpected[i] = expected[expectedOff + i];
        
        if(Arrays.equals(newExpected, newActual))
            fail(formatComparison(NOT_EQUAL, msg, asList(newExpected), asList(newActual)));
    }

    /**
     * Assert that the two arrays are not equal.
     */
    static public void assertNotEquals(char[] expected, char[] actual) {
        assertNotEquals(null, expected, actual);
    }
    
    /**
     * Assert that the two arrays are not equal.  If not, an AssertionFailedError
     * is thrown with the given messages.
     */
    static public void assertNotEquals(String msg, char[] expected, char[] actual) {
        if(Arrays.equals(expected, actual))
            fail(formatComparison(NOT_EQUAL, msg, asList(expected), asList(actual)));
    }
    
    /**
     * Assert that the two arrays are not equal.
     */
    static public void assertNotEquals(float[] expected, float[] actual) {
        assertNotEquals(null, expected, actual);
    }
    
    /**
     * Assert that the two arrays are not equal.  If not, an AssertionFailedError
     * is thrown with the given messages.
     */
    static public void assertNotEquals(String msg, float[] expected, float[] actual) {
        if(Arrays.equals(expected, actual))
            fail(formatComparison(NOT_EQUAL, msg, asList(expected), asList(actual)));
    }
    
    /**
     * Assert that the two arrays are not equal.
     */
    static public void assertNotEquals(double[] expected, double[] actual) {
        assertNotEquals(null, expected, actual);
    }
    
    /**
     * Assert that the two arrays are not equal.  If not, an AssertionFailedError
     * is thrown with the given messages.
     */
    static public void assertNotEquals(String msg, double[] expected, double[] actual) {
        if(Arrays.equals(expected, actual))
            fail(formatComparison(NOT_EQUAL, msg, asList(expected), asList(actual)));
    }
    
    /**
     * Assert that the two arrays are not equal.
     */
    static public void assertNotEquals(int[] expected, int[] actual) {
        assertNotEquals(null, expected, actual);
    }
    
    /**
     * Assert that the two arrays are not equal.  If not, an AssertionFailedError
     * is thrown with the given messages.
     */
    static public void assertNotEquals(String msg, int[] expected, int[] actual) {
        if(Arrays.equals(expected, actual))
            fail(formatComparison(NOT_EQUAL, msg, asList(expected), asList(actual)));
    }
    
    /**
     * Assert that the two arrays are not equal.
     */
    static public void assertNotEquals(long[] expected, long[] actual) {
        assertNotEquals(null, expected, actual);
    }
    
    /**
     * Assert that the two arrays are not equal.  If not, an AssertionFailedError
     * is thrown with the given messages.
     */
    static public void assertNotEquals(String msg, long[] expected, long[] actual) {
        if(Arrays.equals(expected, actual))
            fail(formatComparison(NOT_EQUAL, msg, asList(expected), asList(actual)));
    }
    
    /**
     * Assert that the two arrays are not equal.
     */
    static public void assertNotEquals(Object[] expected, Object[] actual) {
        assertNotEquals(null, expected, actual);
    }
    
    /**
     * Assert that the two arrays are not equal.  If not, an AssertionFailedError
     * is thrown with the given messages.
     */
    static public void assertNotEquals(String msg, Object[] expected, Object[] actual) {
        if(Arrays.equals(expected, actual))
            fail(formatComparison(NOT_EQUAL, msg, Arrays.asList(expected), Arrays.asList(actual)));
    }
    
    /**
     * Assert that the two arrays are not equal.
     */
    static public void assertNotEquals(short[] expected, short[] actual) {
        assertNotEquals(null, expected, actual);
    }
    
    /**
     * Assert that the two arrays are not equal.  If not, an AssertionFailedError
     * is thrown with the given messages.
     */
    static public void assertNotEquals(String msg, short[] expected, short[] actual) {
        if(Arrays.equals(expected, actual))
            fail(formatComparison(NOT_EQUAL, msg, asList(expected), asList(actual)));
    }
    
    /**
     * Asserts that the actual is compareTo == 0 to another value.
     * If it isn't, an AssertionFailedError is thrown.
     */
    static public void assertCompareToEquals(Object expected, Object actual) {
        assertCompareToEquals(null, expected, actual);
    }
    
    /**
     * Asserts that the actual is compareTo == 0 to another value.
     * If it isn't, an AssertionFailedError is thrown with the given message.
     */
    static public void assertCompareToEquals(String msg, Object expected, Object actual) {
        assertComparison(COMPARE_EQUALS, msg, expected, actual);
    }
    
    /**
     * Assertes that actual is greater than expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */
    static public void assertGreaterThan(Object expected, Object actual) {
        assertGreaterThan(null, expected, actual);
    }
    
    /**
     * Assertes that actual is greater than expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */
    static public void assertGreaterThan(String msg, Object expected, Object actual) {
        assertComparison(GREATER_THAN, msg, expected, actual);
    }
    
    /**
     * Assertes that actual is greater than expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */    
    static public void assertGreaterThan(long expected, long actual) {
        assertGreaterThan( new Long(expected), new Long(actual) );
    }
    
    /**
     * Assertes that actual is greater than expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertGreaterThan(String msg, long expected, long actual) {
        assertGreaterThan( msg, new Long(expected), new Long(actual) );
    }
    
    /**
     * Assertes that actual is greater than expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */    
    static public void assertGreaterThan(short expected, short actual) {
        assertGreaterThan( new Short(expected), new Short(actual) );
    }
    
    /**
     * Assertes that actual is greater than expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertGreaterThan(String msg, short expected, short actual) {
        assertGreaterThan( msg, new Short(expected), new Short(actual) );
    }

    /**
     * Assertes that actual is greater than expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */    
    static public void assertGreaterThan(int expected, int actual) {
        assertGreaterThan( new Integer(expected), new Integer(actual) );
    }
    
    /**
     * Assertes that actual is greater than expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertGreaterThan(String msg, int expected, int actual) {
        assertGreaterThan( msg, new Integer(expected), new Integer(actual) );
    }
    
    /**
     * Assertes that actual is greater than expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */    
    static public void assertGreaterThan(double expected, double actual) {
        assertGreaterThan( new Double(expected), new Double(actual) );
    }
    
    /**
     * Assertes that actual is greater than expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertGreaterThan(String msg, double expected, double actual) {
        assertGreaterThan( msg, new Double(expected), new Double(actual) );
    }
    
    /**
     * Assertes that actual is greater than expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */    
    static public void assertGreaterThan(float expected, float actual) {
        assertGreaterThan( new Float(expected), new Float(actual) );
    }
    
    /**
     * Assertes that actual is greater than expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertGreaterThan(String msg, float expected, float actual) {
        assertGreaterThan( msg, new Float(expected), new Float(actual) );
    }
    
    /**
     * Assertes that actual is greater than expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */    
    static public void assertGreaterThan(byte expected, byte actual) {
        assertGreaterThan( new Byte(expected), new Byte(actual) );
    }
    
    /**
     * Assertes that actual is greater than expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertGreaterThan(String msg, byte expected, byte actual) {
        assertGreaterThan( msg, new Byte(expected), new Byte(actual) );
    }

    /**
     * Assertes that actual is less than expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */    
    static public void assertLessThan(Object expected, Object actual) {
        assertLessThan(null, expected, actual);
    }
    
    /**
     * Assertes that actual is less than expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertLessThan(String msg, Object expected, Object actual) {
        assertComparison(LESS_THAN, msg, expected, actual);
    }
    
    /**
     * Assertes that actual is less than expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */    
    static public void assertLessThan(long expected, long actual) {
        assertLessThan( new Long(expected), new Long(actual) );
    }
    
    /**
     * Assertes that actual is less than expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertLessThan(String msg, long expected, long actual) {
        assertLessThan( msg, new Long(expected), new Long(actual) );
    }
    
    /**
     * Assertes that actual is less than expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */    
    static public void assertLessThan(short expected, short actual) {
        assertLessThan( new Short(expected), new Short(actual) );
    }
    
    /**
     * Assertes that actual is less than expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertLessThan(String msg, short expected, short actual) {
        assertLessThan( msg, new Short(expected), new Short(actual) );
    }
    
    /**
     * Assertes that actual is less than expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */    
    static public void assertLessThan(int expected, int actual) {
        assertLessThan( new Integer(expected), new Integer(actual) );
    }
    
    /**
     * Assertes that actual is less than expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertLessThan(String msg, int expected, int actual) {
        assertLessThan( msg, new Integer(expected), new Integer(actual) );
    }
    
    /**
     * Assertes that actual is less than expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */    
    static public void assertLessThan(double expected, double actual) {
        assertLessThan( new Double(expected), new Double(actual) );
    }
    
    /**
     * Assertes that actual is less than expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertLessThan(String msg, double expected, double actual) {
        assertLessThan( msg, new Double(expected), new Double(actual) );
    }
    
    /**
     * Assertes that actual is less than expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */    
    static public void assertLessThan(float expected, float actual) {
        assertLessThan( new Float(expected), new Float(actual) );
    }
    
    /**
     * Assertes that actual is less than expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertLessThan(String msg, float expected, float actual) {
        assertLessThan( msg, new Float(expected), new Float(actual) );
    }
    
    /**
     * Assertes that actual is less than expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */    
    static public void assertLessThan(byte expected, byte actual) {
        assertLessThan( new Byte(expected), new Byte(actual) );
    }
    
    /**
     * Assertes that actual is less than expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertLessThan(String msg, byte expected, byte actual) {
        assertLessThan( msg, new Byte(expected), new Byte(actual) );
    }

    /**
     * Assertes that actual is greater than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */
    static public void assertGreaterThanOrEquals(Object expected, Object actual) {
        assertGreaterThanOrEquals(null, expected, actual);
    }
    
    /**
     * Assertes that actual is greater than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertGreaterThanOrEquals(String msg, Object expected, Object actual) {
        assertComparison(GREATER_THAN_OR_EQUALS, msg,  expected, actual);
    }
    
    /**
     * Assertes that actual is greater than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */    
    static public void assertGreaterThanOrEquals(long expected, long actual) {
        assertGreaterThanOrEquals( new Long(expected), new Long(actual) );
    }
    
    /**
     * Assertes that actual is greater than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertGreaterThanOrEquals(String msg, long expected, long actual) {
        assertGreaterThanOrEquals( msg, new Long(expected), new Long(actual) );
    }
    
    /**
     * Assertes that actual is greater than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */    
    static public void assertGreaterThanOrEquals(short expected, short actual) {
        assertGreaterThanOrEquals( new Short(expected), new Short(actual) );
    }
    
    /**
     * Assertes that actual is greater than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertGreaterThanOrEquals(String msg, short expected, short actual) {
        assertGreaterThanOrEquals( msg, new Short(expected), new Short(actual) );
    }
    
    /**
     * Assertes that actual is greater than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */    
    static public void assertGreaterThanOrEquals(int expected, int actual) {
        assertGreaterThanOrEquals( new Integer(expected), new Integer(actual) );
    }
    
    /**
     * Assertes that actual is greater than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertGreaterThanOrEquals(String msg, int expected, int actual) {
        assertGreaterThanOrEquals( msg, new Integer(expected), new Integer(actual) );
    }
    
    /**
     * Assertes that actual is greater than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */    
    static public void assertGreaterThanOrEquals(double expected, double actual) {
        assertGreaterThanOrEquals( new Double(expected), new Double(actual) );
    }
    
    /**
     * Assertes that actual is greater than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertGreaterThanOrEquals(String msg, double expected, double actual) {
        assertGreaterThanOrEquals( msg, new Double(expected), new Double(actual) );
    }
    
    /**
     * Assertes that actual is greater than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */    
    static public void assertGreaterThanOrEquals(float expected, float actual) {
        assertGreaterThanOrEquals( new Float(expected), new Float(actual) );
    }
    
    /**
     * Assertes that actual is greater than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertGreaterThanOrEquals(String msg, float expected, float actual) {
        assertGreaterThanOrEquals( msg, new Float(expected), new Float(actual) );
    }
    
    /**
     * Assertes that actual is greater than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */    
    static public void assertGreaterThanOrEquals(byte expected, byte actual) {
        assertGreaterThanOrEquals( new Byte(expected), new Byte(actual) );
    }
    
    /**
     * Assertes that actual is greater than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertGreaterThanOrEquals(String msg, byte expected, byte actual) {
        assertGreaterThanOrEquals( msg, new Byte(expected), new Byte(actual) );
    }

    /**
     * Assertes that actual is less than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */    
    static public void assertLessThanOrEquals(Object expected, Object actual) {
        assertLessThanOrEquals(null, expected, actual);
    }
    
    /**
     * Assertes that actual is less than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertLessThanOrEquals(String msg, Object expected, Object actual) {
        assertComparison(LESS_THAN_OR_EQUALS, msg, expected, actual);
    }
    
    /**
     * Assertes that actual is less than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */    
    static public void assertLessThanOrEquals(long expected, long actual) {
        assertLessThanOrEquals( new Long(expected), new Long(actual) );
    }
    
    /**
     * Assertes that actual is less than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertLessThanOrEquals(String msg, long expected, long actual) {
        assertLessThanOrEquals( msg, new Long(expected), new Long(actual) );
    }
    
    /**
     * Assertes that actual is less than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */    
    static public void assertLessThanOrEquals(short expected, short actual) {
        assertLessThanOrEquals( new Short(expected), new Short(actual) );
    }
    
    /**
     * Assertes that actual is less than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertLessThanOrEquals(String msg, short expected, short actual) {
        assertLessThanOrEquals( msg, new Short(expected), new Short(actual) );
    }
    
    /**
     * Assertes that actual is less than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */    
    static public void assertLessThanOrEquals(int expected, int actual) {
        assertLessThanOrEquals( new Integer(expected), new Integer(actual) );
    }
    
    /**
     * Assertes that actual is less than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertLessThanOrEquals(String msg, int expected, int actual) {
        assertLessThanOrEquals( msg, new Integer(expected), new Integer(actual) );
    }
    
    /**
     * Assertes that actual is less than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */    
    static public void assertLessThanOrEquals(double expected, double actual) {
        assertLessThanOrEquals( new Double(expected), new Double(actual) );
    }
    
    /**
     * Assertes that actual is less than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertLessThanOrEquals(String msg, double expected, double actual) {
        assertLessThanOrEquals( msg, new Double(expected), new Double(actual) );
    }
    
    /**
     * Assertes that actual is less than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */    
    static public void assertLessThanOrEquals(float expected, float actual) {
        assertLessThanOrEquals( new Float(expected), new Float(actual) );
    }
    
    /**
     * Assertes that actual is less than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertLessThanOrEquals(String msg, float expected, float actual) {
        assertLessThanOrEquals( msg, new Float(expected), new Float(actual) );
    }
    
    /**
     * Assertes that actual is less than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */    
    static public void assertLessThanOrEquals(byte expected, byte actual) {
        assertLessThanOrEquals( new Byte(expected), new Byte(actual) );
    }
    
    /**
     * Assertes that actual is less than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertLessThanOrEquals(String msg, byte expected, byte actual) {
        assertLessThanOrEquals( msg, new Byte(expected), new Byte(actual) );
    }
    
    static public void assertEmpty(Collection collection) {
        assertEmpty("should have been empty: " + collection, collection);
    }
    
    static public void assertEmpty(String msg, Collection collection) {
        assertTrue(msg, collection.isEmpty());
    }
    
    static public void assertNotEmpty(Collection collection) {
        assertNotEmpty("should not have been empty: " + collection, collection);
    }
    
    static public void assertNotEmpty(String msg, Collection collection) {
        assertFalse(msg, collection.isEmpty());
    }

    /**
     * Assert that an object actual is either less than, greater than,
     * less than or equal to, or greater than or equals to object expected.
     * Checks java.lang.Comparable.
     * If neither 'expected' or 'actual' implement either interface, an
     * AssertionFailedError is thrown.
     * If both 'expected' and 'actual' are null, an AssertionFailedError is thrown.
     */
    @SuppressWarnings("null")
    static private void assertComparison(int type, String msg, Object expected, Object actual) {
        int ret = 0;
        if ( expected == null && actual == null) {
            fail(formatComparison(type, msg, expected, actual));
        } else if (expected instanceof java.lang.Comparable) {
            ret = ((java.lang.Comparable)expected).compareTo(actual);
        } else if (actual instanceof java.lang.Comparable) {
            ret = -1 * ((java.lang.Comparable)actual).compareTo(expected);
        } else { //neither implement either interface.
            fail("Neither " + expected.getClass().getName() + " nor " + actual.getClass().getName() + 
                 " implement expected Comparable interface.");
        }
        
        // break out of here if the comparison failed.
        switch(type) {
        case LESS_THAN:
            if ( ret <= 0 ) break;
            return;
        case GREATER_THAN:
            if ( ret >= 0 ) break;
            return;
        case LESS_THAN_OR_EQUALS:
            if ( ret < 0 ) break;
            return;
        case GREATER_THAN_OR_EQUALS:
            if ( ret > 0 ) break;
            return;
        case COMPARE_EQUALS:
            if ( ret == 0 ) break;
            return;
        }

        //if we didn't return, we failed.
        fail(formatComparison(type, msg, expected, actual));        
        
    }
            
    static private boolean instanceofCheck(Class expected, Object actual) {
        if ( actual == null || expected == null)
            return false;
            
        return expected.isAssignableFrom(actual.getClass());
      
    }    
    
    static private String formatComparison(int type,
                                         String message,
                                         Object expected, 
                                         Object actual) {
        String compare = "";
        switch(type) {
        case LESS_THAN:
            compare = "less than"; break;
        case GREATER_THAN:
            compare = "greater than"; break;
        case LESS_THAN_OR_EQUALS:
            compare = "less than or equal to"; break;
        case GREATER_THAN_OR_EQUALS:
            compare = "greater than or equal to"; break;
        case INSTANCE_OF:
            compare = "instanceof"; break;
        case NOT_INSTANCE_OF:
            compare = "not instanceof"; break;
        case NOT_EQUAL:
            compare = "something other than"; break;
        case NOT_SAME:
            compare = "something other than"; break;
        case CONTAINS:
            compare = "to contain"; break;
        case NOT_CONTAINS:
            compare = "to not contain"; break;
        case COMPARE_EQUALS:
            compare = "compareTo difference than"; break;
        }
        String formatted = "";
        if ( message != null )
            formatted = message + " ";
        return formatted + "expected " + compare + ":<" + expected+
            "> but was:<" + actual + ">";
    }
    
    private static List asList(boolean[] data) {
        if(data == null)
            return new LinkedList();
            
        List list = new LinkedList();
        for(int i = 0; i < data.length; i++)
            list.add(new Boolean(data[i]));
        return list;
    }    
    
    private static List asList(byte[] data) {
        if(data == null)
            return new LinkedList();
            
        List list = new ArrayList(data.length);
        for(int i = 0; i < data.length; i++)
            list.add(new Byte(data[i]));
        return list;
    }
    
    private static List asList(char[] data) {
        if(data == null)
            return new LinkedList();
            
        List list = new ArrayList(data.length);
        for(int i = 0; i < data.length; i++)
            list.add(new Character(data[i]));
        return list;
    }
    
    private static List asList(double[] data) {
        if(data == null)
            return new LinkedList();
            
        List list = new ArrayList(data.length);
        for(int i = 0; i < data.length; i++)
            list.add(new Double(data[i]));
        return list;
    }
    
    private static List asList(float[] data) {
        if(data == null)
            return new LinkedList();
            
        List list = new ArrayList(data.length);
        for(int i = 0; i < data.length; i++)
            list.add(new Float(data[i]));
        return list;
    }
    
    private static List asList(int[] data) {
        if(data == null)
            return new LinkedList();
            
        List list = new ArrayList(data.length);
        for(int i = 0; i < data.length; i++)
            list.add(new Integer(data[i]));
        return list;
    }
    
    private static List asList(long[] data) {
        if(data == null)
            return new LinkedList();
            
        List list = new ArrayList(data.length);
        for(int i = 0; i < data.length; i++)
            list.add(new Long(data[i]));
        return list;
    }
    
    private static List asList(short[] data) {
        if(data == null)
            return new LinkedList();
            
        List list = new ArrayList(data.length);
        for(int i = 0; i < data.length; i++)
            list.add(new Short(data[i]));
        return list;
    }
        
        
}
