package org.limewire.security.certificate;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

public class CertificateToolsTest extends BaseTestCase {
    public CertificateToolsTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(CertificateToolsTest.class);
    }

    public void testEncodeBytesToString() {
        assertEquals("00017F80FF", CertificateTools.encodeBytesToString(new byte[] { 0, 1, 127, -128, -1 }));
    }
}
