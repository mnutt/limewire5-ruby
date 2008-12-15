package org.limewire.lws.server;

import junit.framework.Test;
import junit.textui.TestRunner;

import org.limewire.lws.server.LWSServerUtil;

/**
 * Tests that we can send a 'StartCom' message to start a communication session.
 */
public class StartComTest extends AbstractCommunicationSupport {

    public StartComTest(String s) {
        super(s);
    }

    public static Test suite() {
        return buildTestSuite(StartComTest.class);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }
    public void testStartCom() {
        KeyPair kp = getPublicAndSharedKeys();
        assertTrue(kp.getPublicKey(), LWSServerUtil.isValidPublicKey(kp.getPublicKey()));
        assertTrue(kp.getSharedKey(), LWSServerUtil.isValidPublicKey(kp.getSharedKey()));
    }

}
