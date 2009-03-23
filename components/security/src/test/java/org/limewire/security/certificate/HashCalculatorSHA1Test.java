package org.limewire.security.certificate;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;
import org.limewire.util.StringUtils;

public class HashCalculatorSHA1Test extends BaseTestCase {
    public HashCalculatorSHA1Test(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(HashCalculatorSHA1Test.class);
    }

    public void testCalculate() {
        assertEquals("0BEEC7B5EA3F0FDBC95D0DD47F3C5BC275DA8A33", CertificateTools.encodeBytesToString(new HashCalculatorSHA1Impl().calculate(StringUtils.toAsciiBytes("foo"))));
    }
}
