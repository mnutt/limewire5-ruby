package com.limegroup.gnutella.http;

import java.io.IOException;

import junit.framework.Test;

import com.limegroup.gnutella.util.LimeTestCase;

/**
 * Tests the HTTPUtils class.
 */
public final class HttpUtilsTest extends LimeTestCase {

	/**
	 * Constructs a new HttpUtilsTest.
	 */
	public HttpUtilsTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(HttpUtilsTest.class);
	}

	/**
	 * Runs this test individually.
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
        
    
	/**
	 * Tests the method to extract a header value from an HTTP header.
	 */
	public void testExtractHeaderValue() {
		String value = "value";
		String[] headers = {
			HTTPHeaderName.CONTENT_RANGE+":"   +value,
			HTTPHeaderName.CONTENT_RANGE+": "  +value,
			HTTPHeaderName.CONTENT_LENGTH+":  "+value,
			HTTPHeaderName.CONTENT_TYPE+":  "  +value
		};
		for(int i=0; i<headers.length; i++) {
			String curValue = HttpTestUtils.extractHeaderValue(headers[i]);
			assertEquals("values should be equal", value, curValue);
		}
	}
    
    public void testParseValue() throws Exception {
        assertEquals("value", HTTPUtils.parseValue("key=value"));
        assertEquals("value", HTTPUtils.parseValue("key= value"));
        assertEquals("value", HTTPUtils.parseValue("key= value "));
        assertEquals("VAlue", HTTPUtils.parseValue("key = VAlue"));
        
        try {
            fail("got: " + HTTPUtils.parseValue("key="));
        } catch(IOException expected) {}
        
        try {
            fail("got: " + HTTPUtils.parseValue("key"));
        } catch(IOException expected) {}
        
        try {
            fail("got: " + HTTPUtils.parseValue(""));
        } catch(IOException expected) {}
    }
}
