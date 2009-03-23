package com.limegroup.gnutella.http;

public class HttpTestUtils {

    /**
     * Parses out the header value from the HTTP header string.
     * Given a string of "X-Header: MyValue", this will return "MyValue".
     *
     * @return the header value for the specified full header string, or
     *  <tt>null</tt> if the value could not be extracted
     */
    public static String extractHeaderValue(final String header) {
    	int index = header.indexOf(HTTPUtils.COLON);
    	if(index <= 0) return null;
    	return header.substring(index+1).trim();
    }

}
