package org.limewire.setting;

import java.util.Properties;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;


public class PowerOfTwoSettingTest extends BaseTestCase {

    private static final long BIG_POWER_OF_TWO = 1L << 62;
    
    public PowerOfTwoSettingTest(String name) {
        super(name);
    }
    
    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public static Test suite() {
        return buildTestSuite(PowerOfTwoSettingTest.class);
    }
    
    public void testConstructors() throws Exception {
        // As of early 2006, {64, 128, 256} are the only legal values we use
        PowerOfTwoSetting set64 = new PowerOfTwoSetting(new Properties(), new Properties(),
                "", 64, 1, BIG_POWER_OF_TWO);
        assertEquals("Created setting has unexpected value", 64, set64.getValue());
        PowerOfTwoSetting set128 = new PowerOfTwoSetting(new Properties(), new Properties(),
                "", 128, 1, BIG_POWER_OF_TWO);
        assertEquals("Created setting has unexpected value", 128, set128.getValue());
        PowerOfTwoSetting set256 = new PowerOfTwoSetting(new Properties(), new Properties(),
                "", 256, 1, BIG_POWER_OF_TWO);
        assertEquals("Created setting has unexpected value", 256, set256.getValue());
        
        // Now test with the real max and min from QRT_TABLE_SIZE_IN_KILOBYTES
        set64 = new PowerOfTwoSetting(new Properties(), new Properties(),
                "", 64, 64, 256);
        assertEquals("Created setting has unexpected value", 64, set64.getValue());
        set128 = new PowerOfTwoSetting(new Properties(), new Properties(),
                "", 128, 64, 256);
        assertEquals("Created setting has unexpected value", 128, set128.getValue());
        set256 = new PowerOfTwoSetting(new Properties(), new Properties(),
                "", 256, 64, 256);
        assertEquals("Created setting has unexpected value", 256, set256.getValue());
    }
    
    public void testDefaultValueChecks() throws Exception {
        try {
            new PowerOfTwoSetting(new Properties(), new Properties(), 
                    "", 65, 1, BIG_POWER_OF_TWO);
            fail("PowerOfTwoSetting has a default value that isn't a power of two.");
        } catch (IllegalArgumentException expectedException) {}
        try {
            new PowerOfTwoSetting(new Properties(), new Properties(), 
                    "", 192, 1, BIG_POWER_OF_TWO);
            fail("PowerOfTwoSetting has a default value that isn't a power of two.");
        } catch (IllegalArgumentException expectedException) {        }
        try {
            new PowerOfTwoSetting(new Properties(), new Properties(), 
                    "", 0, 1, BIG_POWER_OF_TWO);
            fail("PowerOfTwoSetting has a default value that isn't a power of two.");
        } catch (IllegalArgumentException expectedException) {}
        try {
            new PowerOfTwoSetting(new Properties(), new Properties(), 
                    "", -2, 1, BIG_POWER_OF_TWO);
            fail("PowerOfTwoSetting has a default value that isn't a power of two.");
        } catch (IllegalArgumentException expectedException) {}
    }
    
    public void testSetValue() throws Exception {
        long min = 64;
        long max = 256;
        long defaultValue = 64;
        PowerOfTwoSetting setting = new PowerOfTwoSetting(new Properties(), new Properties(),
                "", defaultValue, min, max);
        assertEquals("Created setting has unexpected value", defaultValue, setting.getValue());
        
        setting.setValue(128);
        assertEquals("Setting did not accept a legal value.", 128, setting.getValue());
        
        setting.setValue(256);
        assertEquals("Setting did not accept a legal value.", 256, setting.getValue());
        
        // Set value from String
//        setting.setValue("64");
//        assertEquals("Setting did not accept a legal value.", 64, setting.getValue());
        
        
        // Test powers of two that are too big or too small
        setting.setValue(2*max);
        assertEquals("Setting not clipped to max value.", max, setting.getValue());
        
        setting.setValue(min/2);
        assertEquals("Setting not clipped to min value.", min, setting.getValue());
        
        // Test values likely to fool naive implementations of power-of-two checking
        setting.setValue(192);
        assertEquals("Setting didn't properly round value.", 128, setting.getValue());
        
        setting.setValue(96);
        assertEquals("Setting didn't prorely round value.", 64, setting.getValue());
        
        setting.setValue(127);
        assertEquals("Setting didn't properly round value.", 64, setting.getValue());
        
        setting.setValue(129);
        assertEquals("Setting didn't properly round value.", 128, setting.getValue());
        
        setting.setValue(0);
        assertEquals("Setting accepted an illegal value.", min, setting.getValue());
        
        setting.setValue(-2);
        assertEquals("Setting accepted an illegal value.", min, setting.getValue());
    }
    
    public void testNormalizeValue() {
        long testValue = BIG_POWER_OF_TWO;
        PowerOfTwoSetting setting = new PowerOfTwoSetting(new Properties(), new Properties(),
                "", 8, 1, BIG_POWER_OF_TWO);
        while (testValue > 1) {
            setting.setValue(testValue-1);
            testValue >>= 1;
            assertEquals("Setting failed to properly round "+ ((testValue<<1)-1), testValue, setting.getValue());
        }
    }
}
