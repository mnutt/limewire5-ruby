package com.limegroup.gnutella.util;

import junit.framework.Test;

/**
 * Tests for UUID.
 */
public final class UUIDTest extends org.limewire.gnutella.tests.LimeTestCase {

	/**
	 * Constructs a new test instance for responses.
	 */
	public UUIDTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(UUIDTest.class);
	}

	/**
	 * Runs this test individually.
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	public void testUUID() throws Exception {
	    UUID uuid = UUID.nextUUID();
	    String string = uuid.toString();
	    assertEquals(36, string.length());
	    for(int i = 0; i < 36; i++) {
	        char c = string.charAt(i);
	        if(i == 8 || i == 13 || i == 18 || i == 23)
	            assertEquals('-', c);
	        else {
	            switch(c) {
	            case 'a': case 'b': case 'c': case 'd': case 'e': case 'f':
	            case '0': case '1': case '2': case '3': case '4': case '5':
	            case '6': case '7': case '8': case '9': break;
	            default:
	                fail("invalid uuid: " + string);
                }
            }
        }

        String one = "abcdef12-3456-789a-bcde-f1234567890a";
        String two = "cbcdef12-3456-789a-bcde-f1234567890a";
        UUID a = new UUID(one);
        UUID b = new UUID(two);
        UUID c = new UUID(one);
        assertEquals(a, c);
        assertNotEquals(b, a);
        assertNotEquals(b, c);
        assertEquals(c, a);
        
        assertEquals(a.hashCode(), c.hashCode());
        
        String bad = one + "x";
        try {
            new UUID(bad);
            fail("expected exception");
        } catch(IllegalArgumentException expected) {}
        
        String badTwo = one.substring(0, 35);
        try {
            new UUID(badTwo);
            fail("expected exception");
        } catch(IllegalArgumentException expected) {}
    }
}