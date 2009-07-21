package org.limewire.util;

import junit.framework.Test;

public final class VersionTest extends BaseTestCase {

    public VersionTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(VersionTest.class);
    }

    /**
     * Runs this test individually.
     */
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testInvalidVersions() throws Exception {
        try {
            new Version("@version@");
            fail("invalid v.");
        } catch (VersionFormatException expected) {
        }

        try {
            new Version("1a");
            fail("invalid v");
        } catch (VersionFormatException expected) {
        }

        try {
            new Version("1.a");
            fail("invalid v");
        } catch (VersionFormatException expected) {
        }

        try {
            new Version("1.2a");
            fail("invalid v");
        } catch (VersionFormatException expected) {
        }

        try {
            new Version("1.2.a");
            fail("invalid v");
        } catch (VersionFormatException expected) {
        }

        try {
            new Version("1.a.3");
            fail("invalid v");
        } catch (VersionFormatException expected) {
        }
    }

    private void check(String v, int M, int m, int s, int r) throws Exception {
        Version vrs = new Version(v);
        assertEquals(M, vrs.getMajor());
        assertEquals(m, vrs.getMinor());
        assertEquals(s, vrs.getService());
        assertEquals(r, vrs.getRevision());
    }

    public void testValidVersion() throws Exception {
        check("0.0.0", 0, 0, 0, 0);
        check("1.2.3", 1, 2, 3, 0);
        check("10.2.3052", 10, 2, 3052, 0);
        check("10.2.3_05", 10, 2, 3, 5);
        check("2.1.81", 2, 1, 81, 0);
        check("4.6.0 Pro", 4, 6, 0, 0);
        check("4.6.0jum", 4, 6, 0, 0);
        check("4.6.0jum233", 4, 6, 0, 233);
        check("4.6.0_01", 4, 6, 0, 1);
        check("4.6.0_01abc", 4, 6, 0, 1);
        check("1.5.0b56", 1, 5, 0, 56);
        check("1.5.0_01", 1, 5, 0, 1);
        check("1.5.0_02", 1, 5, 0, 2);
        check("1", 1, 0, 0, 0);
        check("1.2", 1, 2, 0, 0);
        check("1.2.3.4a", 1, 2, 3, 4);
        check("1.2.3.4a5", 1, 2, 3, 4);
    }

    public void testComparingVersions() throws Exception {
        Version v1, v2;

        v1 = new Version("0.0.0");
        v2 = new Version("0.0.0");
        assertEquals(v1, v2);

        v2 = new Version("0.0.0_1");
        // make sure aGT a aLT are doing it the way we expect ...
        // param 2 is 'X than' param 1
        assertGreaterThan(0, v2.compareTo(v1));
        assertLessThan(0, v1.compareTo(v2));

        assertGreaterThan(v1, v2);
        assertLessThan(v2, v1);

        v2 = new Version("1.0.0");
        assertGreaterThan(v1, v2);
        assertLessThan(v2, v1);

        v2 = new Version("0.1.0");
        assertGreaterThan(v1, v2);
        assertLessThan(v2, v1);

        v2 = new Version("0.0.1");
        assertGreaterThan(v1, v2);
        assertLessThan(v2, v1);

        // try with a leading zero.
        v2 = new Version("0.0.0_01");
        assertGreaterThan(v1, v2);
        assertLessThan(v2, v1);

        // try with words
        v2 = new Version("0.0.0 PRO");
        assertEquals(v1, v2);

        v2 = new Version("0.0.1 PRO");
        assertGreaterThan(v1, v2);
        assertLessThan(v2, v1);
    }

    public void testCompareBeta() throws Exception {
        Version v1, v2;

        v1 = new Version("1.6.0_01");
        v2 = new Version("1.6.0-beta");
        assertGreaterThan(0, v1.compareTo(v2));
    }
}