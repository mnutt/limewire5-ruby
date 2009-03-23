package com.limegroup.gnutella.licenses;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllLicenseTests {

    public static Test suite() {
        TestSuite suite = new TestSuite();
        //$JUnit-BEGIN$
        suite.addTest(CCLicenseTest.suite());
        suite.addTest(LicenseFactoryTest.suite());
        suite.addTest(LicenseReadingTest.suite());
        suite.addTest(LicenseSharingTest.suite());
        suite.addTest(LicenseVerifierTest.suite());
        suite.addTest(WeedLicenseTest.suite());
        //$JUnit-END$
        return suite;
    }

}
