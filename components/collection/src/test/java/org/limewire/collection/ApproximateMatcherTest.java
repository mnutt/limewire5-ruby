package org.limewire.collection;


import junit.framework.Test;

import org.limewire.util.BaseTestCase;

/**
 * Unit tests for ApproximateMatcher
 */
public class ApproximateMatcherTest extends BaseTestCase {

    ApproximateMatcher matcher=null;
        
	public ApproximateMatcherTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(ApproximateMatcherTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	public void testBasic() {
        //1. Basic tests.  Try with and without compareBackwards.
        scan("vintner", "writers", 5);
        scan("", "", 0); 
        scan("a", "", 1);
        scan("", "a", 1);
        scan("a", "b", 1);
        scan("abc", "abbd", 2);
        scan("abcd", "bd", 2);
        scan("abcd", "abcd", 0);
        scan("k", "abcdefghiklmnopqrst", 18);
        scan("l", "abcdefghiklmnopqrst", 18);
    }
    
    public void testCaseInsensitive() {

        //2. Case insensitive tests.
        matcher=new ApproximateMatcher();
        assertEquals(3, matcher.match("AbcD", "ABcdx"));
        matcher.setIgnoreCase(true);
        assertEquals(1, matcher.match(matcher.process("AbcD"),
                                  matcher.process("ABcdx")));
    }
    
    public void testFractionalMatching() {
        
        //3. Fractional matching.
        matcher=new ApproximateMatcher();
        assertTrue(matcher.matches("abcd", "abxy", 0.f));
        assertTrue(matcher.matches("abcd", "abxy", 0.5f));
        assertTrue(! matcher.matches("abcd", "abxy", 1.f));
        assertTrue(matcher.matches("01234567890123456789",    
                                    "01234X67890123456789",
                                    0.5f));
        assertTrue(matcher.matches("01234567890123456789",    
                                    "01234X67890123456789",
                                    0.9f));
        assertTrue(! matcher.matches("01234567890123456789",    
                                      "01234X67890123456789",
                                      1.f));
    }
    
    public void testWhitespace() {

        //4. Whitespace
        matcher=new ApproximateMatcher();
        assertEquals(2, matcher.match(" a_", "_a "));
        matcher.setIgnoreWhitespace(true);
        assertEquals(0, matcher.match(matcher.process(" a_"),
                                  matcher.process("_a ")));

        matcher=new ApproximateMatcher();
        assertEquals(1, matcher.match("a b", "ab"));
        matcher.setIgnoreWhitespace(true);
        assertEquals(0, matcher.match(matcher.process("a b"),
                                  matcher.process("ab")));

        matcher=new ApproximateMatcher();
        assertEquals(1, matcher.match("ab", "a_b"));
        matcher.setIgnoreWhitespace(true);
        assertEquals(0, matcher.match(matcher.process("ab"),
                                  matcher.process("a_b")));
    }


    private static void scan(String s1, String s2, int expected) {
        //Match forward and backwards
        ApproximateMatcher matcher=null;
        matcher=new ApproximateMatcher(5);  //reuse buffer for some,not all
        matcher.setCompareBackwards(true);
        assertEquals(expected, matcher.match(matcher.process(s1),
                                  matcher.process(s2)));
        matcher.setCompareBackwards(false);
        assertEquals(expected, matcher.match(s1, s2));

        //Bounded match
        for (int i=-1; i<expected; i++) {
            assertTrue( "i="+i+
                        ", expected="+expected+
                        " match="+matcher.match(s1, s2),
                        ! matcher.matches(s1, s2, i) );
        }
        assertTrue(matcher.matches(s1, s2, expected));
        assertTrue(matcher.matches(s1, s2, expected+1));
    }
    
}