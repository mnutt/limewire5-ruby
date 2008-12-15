package org.limewire.lws.server;

import org.limewire.lws.server.LWSDispatcherSupport;
import org.limewire.lws.server.LWSServerUtil;
import org.limewire.util.BaseTestCase;

import junit.framework.Test;
import junit.textui.TestRunner;

public class RemoveCallbackTest extends BaseTestCase {
    
    public RemoveCallbackTest(String s) { super(s); }
    
    public static Test suite() {
        return buildTestSuite(RemoveCallbackTest.class);
    }
    
    public static void main(String[] args) {
        TestRunner.run(suite());
    }
	
	final static char Q = LWSDispatcherSupport.Constants.CALLBACK_QUOTE;
	
	public void testNull() {
		String s = null;
		assertEquals(s, LWSServerUtil.removeCallback(s));
	}
	
	public void testEmpty() {
		String s = "";
		assertEquals(s, LWSServerUtil.removeCallback(s));
	}

	public void testNone() {
		String s = "test";
		assertEquals(s, LWSServerUtil.removeCallback(s));
	}
	
	public void testNoRightParen() {
		String s = "test(" + Q + "insides" + Q;
		assertEquals(s, LWSServerUtil.removeCallback(s));
	}
	
	public void testNoLeftParen() {
		String s = "test" + Q + "insides" + Q + ")";
		assertEquals(s, LWSServerUtil.removeCallback(s));
	}
	
	public void testNoRightQuote() {
		String s = "test(" + Q + "insides" + ")";
		assertEquals(s, LWSServerUtil.removeCallback(s));
	}
	
	public void testNoLeftQuote() {
		String s = "test(" + "insides" + Q + ")";
		assertEquals(s, LWSServerUtil.removeCallback(s));
	}
	
	public void testOK() {
		String s = "test(" + Q + "insides" + Q + ")";
		assertEquals("insides", LWSServerUtil.removeCallback(s));
	}
}
