package org.limewire.lws.server;

import java.util.HashMap;
import java.util.Map;

import org.limewire.util.BaseTestCase;

abstract class AbstractParseTestCase extends BaseTestCase {
    
    public AbstractParseTestCase(String s) { super(s); }
	
	public void allTest() {
		runTest("", new String[]{ });
		runTest("p1=v1", new String[]{ "p1", "v1"});
		runTest("p1=", new String[]{ "p1", ""});
		runTest("p1", new String[]{ "p1", null});
		runTest("p1=v1|p2=v2", new String[]{ "p1", "v1", "p2", "v2"});
		runTest("p1=v1|p2=v2|p3=v3", new String[]{ "p1", "v1", "p2", "v2", "p3", "v3"});
	}	
	
	abstract Map<String, String> parse(String line);
    
    /**
     * Returns the separator between name/value pairs.
     * 
     * @return the separator between name/value pairs
     */
	abstract String getNameValuePairSeparator();
	
    /**
     * Runs a test on a given {@link String}, <code>line</code>, and
     * expected values.
     * 
     * @param subject subject of the test
     * @param expectedNameValuePairs expected name/value pairs, where the evens
     *        are names and the odds are values
     */
	private void runTest(String subject, String[] expectedNameValuePairs) {
		subject = subject.replace("|",getNameValuePairSeparator());
		Map<String, String> want = new HashMap<String, String>();
		for (int i = 0; i<expectedNameValuePairs.length; i += 2) want.put(expectedNameValuePairs[i], expectedNameValuePairs[i+1]);
		Map<String, String> have = parse(subject);
		assertEquals(subject, want, have);
	}	
}
