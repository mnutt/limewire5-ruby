package org.limewire.lws.server;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Test;
import junit.textui.TestRunner;

import org.limewire.util.BaseTestCase;

public class RemoteServerImplGetArgsTest extends BaseTestCase {
    
    public RemoteServerImplGetArgsTest(String s) { super(s); }
    
    public static Test suite() {
        return buildTestSuite(RemoteServerImplGetArgsTest.class);
    }
    
    public static void main(String[] args) {
        TestRunner.run(suite());
    }
	
	public void testBackSlashes() {
	    runTest("store\\app\\pages\\client\\ClientCom\\command\\StoreKey\\public\\PCURJKKTXE\\private\\BMBTVRVCSX\\ip\\127.0.0.1", 
                new String[]{"command", "StoreKey", "public", "PCURJKKTXE", "private", "BMBTVRVCSX", "ip", "127.0.0.1"});
    }
    
    public void testSlashes() {
        runTest("store/app/pages/client/ClientCom/command/StoreKey/public/PCURJKKTXE/private/BMBTVRVCSX/ip/127.0.0.1",
                new String[]{"command", "StoreKey", "public", "PCURJKKTXE", "private", "BMBTVRVCSX", "ip", "127.0.0.1"});
    }
    
    public void testMixed() {
        runTest("store/app/pages/client\\ClientCom/command/StoreKey\\public/PCURJKKTXE/private/BMBTVRVCSX/ip/127.0.0.1",
                new String[]{"command", "StoreKey", "public", "PCURJKKTXE", "private", "BMBTVRVCSX", "ip", "127.0.0.1"});
    }
    
    public void testNoArg() {
        runTest("store/app/pages/client\\ClientCom/command",
                new String[]{"command", null});
    }
    
    public void testEmptyArg() {
        runTest("store/app/pages/client\\ClientCom/command/",
                new String[]{"command", null});
    }
    
    /**
     * Makes sure that a call to {@link RemoteServerImpl#getArgs(String)}} is
     * <code>StoreKey</code>.
     * 
     * @param request expected result
     * @param expected even-length array representing the expected {@link Map}
     */
    private void runTest(String request, String[] expected) {
        Map<String,String> have = RemoteServerImpl.getArgs(request);
        Map<String,String> want = new HashMap<String,String>();
        for (int i=0; i<expected.length; i += 2) {
            want.put(expected[i], expected[i+1]);
        }
        assertEquals(want,have);
    }
}
