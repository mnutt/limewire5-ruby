package com.limegroup.gnutella.util;

import java.io.File;
import java.util.Properties;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;
import org.limewire.util.FileUtils;

import com.limegroup.gnutella.util.Portable;
import com.limegroup.gnutella.util.PortableImpl;

public class PortableTest extends BaseTestCase {

    public PortableTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public static Test suite() {
        return buildTestSuite(PortableTest.class);
    }
    
    private static final File portableProps = new File("portable.props");

    @Override
    public void tearDown() {
        // Delete the portable.props we may have made
        portableProps.delete();
    }

    public void testNoPortableSettings() throws Exception {
        // when there is no portable.props file, portable.getSettingsLocation() should return null
        portableProps.delete();
        Portable portable = new PortableImpl();
        
        assertFalse(portable.isPortable());
        assertNull(portable.getSettingsLocation());
    }
    
    public void testPortableSettings() throws Exception {
        // portable.getSettingsLocation() should read valid portable settings and return a File
        Properties p = new Properties();
        p.put("SETTINGS", "Settings");
        FileUtils.writeProperties(portableProps, p);
        Portable portable = new PortableImpl();
        
        assertTrue(portable.isPortable());
        assertNotNull(portable.getSettingsLocation());
    }
    
    public void testBadPortableSettings() throws Exception {
        // if the portable settings are bad, portable.getSettingsLocation() should throw an IOException
        Properties p = new Properties();
        p.put("BAD_KEY", "BAD_VALUE");
        FileUtils.writeProperties(portableProps, p);
        Portable portable = new PortableImpl();
        
        assertTrue(portable.isPortable());
        assertNull(portable.getSettingsLocation());
    }
}
