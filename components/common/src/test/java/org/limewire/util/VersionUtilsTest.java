package org.limewire.util;

import junit.framework.Test;

public final class VersionUtilsTest extends BaseTestCase {

    private String storedJavaVersion;
    
    public VersionUtilsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(VersionUtilsTest.class);
    }

    /**
     * Runs this test individually.
     */
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    public void setUp() {
        storedJavaVersion = System.getProperty("java.version");
    }
    
    @Override
    public void tearDown() {
        setJavaVersion(storedJavaVersion);
    }
    
    private void setJavaVersion(String v) {
        System.setProperty("java.version", v);
        assertEquals(v, System.getProperty("java.version"));
    }
    
    public void testIsJava15OrAbove() {
        setJavaVersion("1.4");
        assertFalse(VersionUtils.isJava15OrAbove());
        setJavaVersion("1.4.0");
        assertFalse(VersionUtils.isJava15OrAbove());
        setJavaVersion("1.5");
        assertTrue(VersionUtils.isJava15OrAbove());
        setJavaVersion("1.5.0");
        assertTrue(VersionUtils.isJava15OrAbove());
        setJavaVersion("1.5.1");
        assertTrue(VersionUtils.isJava15OrAbove());
        setJavaVersion("1.5.0_01");
        assertTrue(VersionUtils.isJava15OrAbove());
        setJavaVersion("1.6");
        assertTrue(VersionUtils.isJava15OrAbove());
        setJavaVersion("1.6.0");
        assertTrue(VersionUtils.isJava15OrAbove());
        setJavaVersion("1.7");
        assertTrue(VersionUtils.isJava15OrAbove());
        setJavaVersion("1.7.0");
        assertTrue(VersionUtils.isJava15OrAbove());
    }
    
    public void testIsJava16OrAbove() {
        setJavaVersion("1.4");
        assertFalse(VersionUtils.isJava16OrAbove());
        setJavaVersion("1.4.0");
        assertFalse(VersionUtils.isJava16OrAbove());
        setJavaVersion("1.5");
        assertFalse(VersionUtils.isJava16OrAbove());
        setJavaVersion("1.5.0");
        assertFalse(VersionUtils.isJava16OrAbove());
        setJavaVersion("1.5.1");
        assertFalse(VersionUtils.isJava16OrAbove());
        setJavaVersion("1.5.0_01");
        assertFalse(VersionUtils.isJava16OrAbove());
        setJavaVersion("1.6");
        assertTrue(VersionUtils.isJava16OrAbove());
        setJavaVersion("1.6.0");
        assertTrue(VersionUtils.isJava16OrAbove());
        setJavaVersion("1.7");
        assertTrue(VersionUtils.isJava16OrAbove());
        setJavaVersion("1.7.0");
        assertTrue(VersionUtils.isJava16OrAbove());
    }
    
    public void testIsJavaVersionAbove() {
        setJavaVersion("1.6");
        assertTrue(VersionUtils.isJavaVersionAbove("1.4"));
        assertTrue(VersionUtils.isJavaVersionAbove("1.5"));
        assertFalse(VersionUtils.isJavaVersionAbove("1.6"));
        assertFalse(VersionUtils.isJavaVersionAbove("1.7"));
        
        setJavaVersion("1.6.0");
        assertTrue(VersionUtils.isJavaVersionAbove("1.4"));
        assertTrue(VersionUtils.isJavaVersionAbove("1.5"));
        assertFalse(VersionUtils.isJavaVersionAbove("1.6"));
        assertFalse(VersionUtils.isJavaVersionAbove("1.7"));
    }
    
    public void testIsJavaVersionOrAbove() {
        setJavaVersion("1.6");
        assertTrue(VersionUtils.isJavaVersionOrAbove("1.4"));
        assertTrue(VersionUtils.isJavaVersionOrAbove("1.5"));
        assertTrue(VersionUtils.isJavaVersionOrAbove("1.6"));
        assertFalse(VersionUtils.isJavaVersionOrAbove("1.7"));
        
        setJavaVersion("1.6.0");
        assertTrue(VersionUtils.isJavaVersionOrAbove("1.4"));
        assertTrue(VersionUtils.isJavaVersionOrAbove("1.5"));
        assertTrue(VersionUtils.isJavaVersionOrAbove("1.6"));
        assertFalse(VersionUtils.isJavaVersionOrAbove("1.7"));
    }
    
}