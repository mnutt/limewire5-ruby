package org.limewire.lws.server;

import java.util.HashMap;
import java.util.Map;

import java.util.regex.Pattern;

import org.limewire.lws.server.LWSServerUtil;
import org.limewire.util.BaseTestCase;

import junit.framework.Test;
import junit.textui.TestRunner;

public class LWSServerUtilTest extends BaseTestCase {
    
    public LWSServerUtilTest(String s) { super(s); }
    
    public static Test suite() {
        return buildTestSuite(LWSServerUtilTest.class);
    }
    
    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    public void testNull() {
        String cmd = null;
        Map<String, String> args = new HashMap<String, String>();
        String have = LWSServerUtil.addURLEncodedArguments(cmd, args);
        assertNull(have);
    }
    
    public void testSimple() {
        String cmd = "cmd";
        String wantString = "cmd";
        Map<String, String> wantArgs = new HashMap<String, String>();
        Map<String, String> haveArgs = new HashMap<String, String>();
        String haveString = LWSServerUtil.addURLEncodedArguments(cmd, haveArgs);
        assertEquals(wantString, haveString);
        assertEquals(wantArgs, haveArgs);
    }    
    
    public void testSimple2() {
        String cmd = "cmd?";
        String wantString = "cmd";
        Map<String, String> wantArgs = new HashMap<String, String>();
        Map<String, String> haveArgs = new HashMap<String, String>();
        String haveString = LWSServerUtil.addURLEncodedArguments(cmd, haveArgs);
        assertEquals(wantString, haveString);
        assertEquals(wantArgs, haveArgs);
    } 
    
    public void testSimpleOneArg() {
        String cmd = "cmd?one=1";
        String wantString = "cmd";
        Map<String, String> wantArgs = new HashMap<String, String>();
        wantArgs.put("one", "1");
        Map<String, String> haveArgs = new HashMap<String, String>();
        String haveString = LWSServerUtil.addURLEncodedArguments(cmd, haveArgs);
        assertEquals(wantString, haveString);
        assertEquals(wantArgs, haveArgs);
    } 
    
    public void testSimpleOneArgNull() {
        String cmd = "cmd?one";
        String wantString = "cmd";
        Map<String, String> wantArgs = new HashMap<String, String>();
        wantArgs.put("one", null);
        Map<String, String> haveArgs = new HashMap<String, String>();
        String haveString = LWSServerUtil.addURLEncodedArguments(cmd, haveArgs);
        assertEquals(wantString, haveString);
        assertEquals(wantArgs, haveArgs);
    } 
    
    public void testSimpleOneArgEmpty() {
        String cmd = "cmd?one=";
        String wantString = "cmd";
        Map<String, String> wantArgs = new HashMap<String, String>();
        wantArgs.put("one", "");
        Map<String, String> haveArgs = new HashMap<String, String>();
        String haveString = LWSServerUtil.addURLEncodedArguments(cmd, haveArgs);
        assertEquals(wantString, haveString);
        assertEquals(wantArgs, haveArgs);
    } 
    
    public void testSimpleTwoArgs() {
        String cmd = "cmd?one=1&two=2";
        String wantString = "cmd";
        Map<String, String> wantArgs = new HashMap<String, String>();
        wantArgs.put("one", "1");
        wantArgs.put("two", "2");
        Map<String, String> haveArgs = new HashMap<String, String>();
        String haveString = LWSServerUtil.addURLEncodedArguments(cmd, haveArgs);
        assertEquals(wantString, haveString);
        assertEquals(wantArgs, haveArgs);
    } 
    
    public void testSimpleTwoArgsNull() {
        String cmd = "cmd?one&two";
        String wantString = "cmd";
        Map<String, String> wantArgs = new HashMap<String, String>();
        wantArgs.put("one", null);
        wantArgs.put("two", null);
        Map<String, String> haveArgs = new HashMap<String, String>();
        String haveString = LWSServerUtil.addURLEncodedArguments(cmd, haveArgs);
        assertEquals(wantString, haveString);
        assertEquals(wantArgs, haveArgs);
    } 
    
	public void testCookiesTasteGood() {
		String str = LWSServerUtil.createCookieDate();
		//
		// Wdy, DD-Mon-YYYY HH:MM:SS GMT
		// Thu, 10-05-2007 15:53:12 GMT
		//
		boolean b = Pattern.matches(
                "[A-Z][a-z][a-z], \\d\\d-\\d\\d-\\d\\d\\d\\d \\d\\d:\\d\\d:\\d\\d GMT", 
                str);
		assertTrue(str, b);
	}    
}
