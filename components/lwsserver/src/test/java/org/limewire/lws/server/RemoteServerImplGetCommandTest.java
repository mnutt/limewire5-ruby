package org.limewire.lws.server;

import org.limewire.util.BaseTestCase;

import junit.framework.Test;
import junit.textui.TestRunner;

public class RemoteServerImplGetCommandTest extends BaseTestCase {
    
    public RemoteServerImplGetCommandTest(String s) { super(s); }
    
    public static Test suite() {
        return buildTestSuite(RemoteServerImplGetCommandTest.class);
    }
    
    public static void main(String[] args) {
        TestRunner.run(suite());
    }
	
	public void testBackSlashes() {
	    runTest("store\\app\\pages\\client\\ClientCom\\command\\StoreKey\\public\\PCURJKKTXE\\private\\BMBTVRVCSX\\ip\\127.0.0.1", "StoreKey");
    }
    
    public void testSlashes() {
        runTest("store/app/pages/client/ClientCom/command/StoreKey/public/PCURJKKTXE/private/BMBTVRVCSX/ip/127.0.0.1", "StoreKey");
    }
    
    public void testMixed() {
        runTest("store/app/pages/client\\ClientCom/command/StoreKey\\public/PCURJKKTXE/private/BMBTVRVCSX/ip/127.0.0.1", "StoreKey");
    }
    
    public void testNoArg() {
        runTest("store/app/pages/client\\ClientCom/command", null);
    }
    
    public void testEmptyArg() {
        runTest("store/app/pages/client\\ClientCom/command/", null);
    }
    
    public void testMissing() {
        runTest("store/app/pages/client\\ClientCom/comffffmand", null);
    }
    
    /**
     * Makes sure that a call to {@link RemoteServerImpl#getCommand(String)}}
     * is <code>StoreKey</code>.
     * 
     * @param request the request
     * @param want expected result
     */
    private void runTest(String request, String want) {
        String have = RemoteServerImpl.getCommand(request);
        assertEquals(want, have);
    }
}
