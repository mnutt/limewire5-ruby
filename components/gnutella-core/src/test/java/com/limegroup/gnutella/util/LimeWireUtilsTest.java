package com.limegroup.gnutella.util;


import org.limewire.gnutella.tests.LimeTestCase;

import com.limegroup.gnutella.util.LimeWireUtils;

import junit.framework.Test;

/**
 * Tests certain features of CommonUtils
 */
public class LimeWireUtilsTest extends LimeTestCase {

    public LimeWireUtilsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(LimeWireUtilsTest.class);
    }  
    
    public void testMajorRevisionMethod() {
        int majorVersion = LimeWireUtils.getMajorVersionNumber();
        assertEquals(2,majorVersion);
        majorVersion = LimeWireUtils.getMajorVersionNumberInternal("3.7.7");
        assertEquals(3,majorVersion);
        majorVersion = LimeWireUtils.getMajorVersionNumberInternal("14.7.7");
        assertEquals(14,majorVersion);
        majorVersion = LimeWireUtils.getMajorVersionNumberInternal("13.34.7");
        assertEquals(13,majorVersion);
        majorVersion = LimeWireUtils.getMajorVersionNumberInternal(".34.7");
        assertEquals(2,majorVersion);
        majorVersion = LimeWireUtils.getMajorVersionNumberInternal("2.7.13");
        assertEquals("unexpected major version number",2, majorVersion); 
    }

    public void testMinorRevisionMethod() {
        int minorVersion = LimeWireUtils.getMinorVersionNumber();
        assertEquals(7,minorVersion);
        minorVersion = LimeWireUtils.getMinorVersionNumberInternal("3.8.7");
        assertEquals(8,minorVersion);
        minorVersion = LimeWireUtils.getMinorVersionNumberInternal("14.13.7");
        assertEquals(13,minorVersion);
        minorVersion = LimeWireUtils.getMinorVersionNumberInternal("2.34.7");
        assertEquals(34,minorVersion);        
        minorVersion = LimeWireUtils.getMinorVersionNumberInternal("..7");
        assertEquals(7,minorVersion);        
        minorVersion = LimeWireUtils.getMinorVersionNumberInternal("2..7");
        assertEquals(7,minorVersion);    

        minorVersion = LimeWireUtils.getMinorVersionNumberInternal("2.7.13");
        assertEquals("unexpected minor version number",7, minorVersion); 
    }

}
