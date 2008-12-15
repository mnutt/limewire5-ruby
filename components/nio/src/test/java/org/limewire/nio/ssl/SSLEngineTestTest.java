package org.limewire.nio.ssl;

import junit.framework.Test;

import org.limewire.nio.ByteBufferCache;
import org.limewire.util.BaseTestCase;

public class SSLEngineTestTest extends BaseTestCase {
    
    public SSLEngineTestTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(SSLEngineTestTest.class);
    }
    
    public void testGo() {
        SSLEngineTest test = new SSLEngineTest(SSLUtils.getTLSContext(), new String[] { "TLS_DH_anon_WITH_AES_128_CBC_SHA" }, new ByteBufferCache());
        assertTrue(test.go());
    }
    
    public void testGoFailsAndNotifiesErrorService() {
        SSLEngineTest test = new SSLEngineTest(SSLUtils.getTLSContext(), new String[] { "la de da" }, new ByteBufferCache());
        assertFalse(test.go());
        Throwable cause = test.getLastFailureCause();
        assertNotNull(cause);
        assertEquals("Unsupported ciphersuite la de da", cause.getMessage());
    }

}
