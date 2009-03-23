package com.limegroup.gnutella.lws.server;

import junit.framework.Test;
import junit.textui.TestRunner;

import org.limewire.lws.server.LWSServerUtil;

/**
 * Tests basic communication.
 */
public class GiveKeyTest extends AbstractCommunicationSupportWithNoLocalServer {

    public GiveKeyTest(String s) {
        super(s);
    }

    public static Test suite() {
        return buildTestSuite(GiveKeyTest.class);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    public void testGiveKey() {
        final String privateKey = getPrivateKey();
        assertTrue("invalid private key '" + privateKey + "'", LWSServerUtil.isValidPrivateKey(privateKey));
    }

}
