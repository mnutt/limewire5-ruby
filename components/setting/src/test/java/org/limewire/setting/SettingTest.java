package org.limewire.setting;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Properties;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;
import org.limewire.util.OSUtils;

/**
 * Tests the setting for character arrays.
 */
public class SettingTest extends BaseTestCase {
    
    private final long EXPIRY_INTERVAL = 15 * 24 * 60 * 60 * 1000l; //15 days
    private final String LAST_EXPIRE_TIME = "LAST_EXPIRE_TIME";
    
    private File settingsFile;
    
    private final char[] chars = {'a','b','c'};
    
    private final String[] strings = new String[]{"string1", "string2", "string3"};
    private File[] files;
    
    class TestSettings {

        Properties DEF_PROPS;
        Properties PROPS;
        SettingsFactory FACTORY;
        BooleanSetting BOOL_SETTING;
        ByteSetting BYTE_SETTING;
        ColorSetting COLOR_SETTING;
        FileSetting FILE_SETTING;
        IntSetting INT_SETTING;
        LongSetting LONG_SETTING;
        StringSetting STRING_SETTING;
        CharArraySetting CHAR_ARRAY_SETTING;
        StringArraySetting STRING_ARRAY_SETTING;
        FileArraySetting FILE_ARRAY_SETTING;
        
        IntSetting EXPIRABLE_INT_SETTING;
        BooleanSetting EXPIRABLE_BOOLEAN_SETTING;
        
		ProxyFileSetting PROXY_FILE_SETTING;
        
        TestSettings(File file) {
            FACTORY = new SettingsFactory(file);
            
            BOOL_SETTING = FACTORY.createBooleanSetting("BOOL_SETTING", true);
            BYTE_SETTING = FACTORY.createByteSetting("BYTE_SETTING", (byte)23);
            COLOR_SETTING = FACTORY.createColorSetting("COLOR_SETTING", 
                                                       new Color(255, 127, 63));
            if(OSUtils.isUnix() || OSUtils.isMacOSX())
                FILE_SETTING = FACTORY.createFileSetting("FILE_SETTING", 
                                                     new File("/temp/turkey.txt"));
            else
                FILE_SETTING = FACTORY.createFileSetting("FILE_SETTING", 
                                                     new File("c:/temp/turkey.txt"));
            INT_SETTING = FACTORY.createIntSetting("INT_SETTING", 143);
            LONG_SETTING = FACTORY.createLongSetting("LONG_SETTING", 666666);
            STRING_SETTING = FACTORY.createStringSetting("STRING_SETTING", 
                                                         "terrific");
            CHAR_ARRAY_SETTING = 
                FACTORY.createCharArraySetting("CHAR_ARRAY_SETTING", chars);
                
            STRING_ARRAY_SETTING =
                FACTORY.createStringArraySetting("STRING_ARRAY_SETTING", strings);
            
            FILE_ARRAY_SETTING =
                FACTORY.createFileArraySetting("FILE_ARRAY_SETTING", files);
                
            EXPIRABLE_INT_SETTING =
                FACTORY.createExpirableIntSetting("EXPIRABLE_INT_SETTING", 12061980);
                
            EXPIRABLE_BOOLEAN_SETTING =
                FACTORY.createExpirableBooleanSetting("EXPIRABLE_BOOLEAN_SETTING", false);
                
			PROXY_FILE_SETTING = FACTORY.createProxyFileSetting
			("PROXY_FILE_SETTING", FILE_SETTING);
                
            FACTORY.getProperties().setProperty(LAST_EXPIRE_TIME, String.valueOf(System.currentTimeMillis()));
        }
    }
    
    
    public SettingTest(java.lang.String testName) {
        super(testName);
    }
    
    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public static Test suite() {
        return buildTestSuite(SettingTest.class);
    }
    
    public void setUp() throws Exception {
        settingsFile = new File("testSettings.props");
        settingsFile.delete();
        settingsFile.deleteOnExit();
    }
        
    
    public void tearDown() {
       settingsFile.delete();
    }

    public void testSetting() throws Exception {
        
        // Necessary to work on OSX without the graphics environment .. 
        // harmless elsewhere.
        System.setProperty("java.awt.headless", "true");
        
        String testFilePath;
        String testFilePath2;
        File[] testFileArray;
        
        if(OSUtils.isUnix() || OSUtils.isMacOSX()) {
            testFilePath = "/mickey-mouse.com";
            testFilePath2 = "/temp/turkey.txt";
            testFileArray = new File[] { 
                    new File("/temp/A"), new File("/temp/B") };
        } else {
            // assume we're on windows
            testFilePath = "c:/mickey-mouse.com";
            testFilePath2 = "c:/temp/turkey.txt";
            testFileArray = new File[] { 
                    new File("c:/temp/A"), new File("c:/temp/B") };            
        }
    
        files = new File[]{ new File(testFilePath), new File(testFilePath2) };
        
        // Delete any existing file and create a new set of settings */
        TestSettings settings = new TestSettings(settingsFile);
        TestSettings settings2 = new TestSettings(settingsFile);
        
        // Confirm all of the default values
        // (Which requires one full conversion to and from their string values 
        assertEquals("Bool default", true, settings.BOOL_SETTING.getValue());
        assertEquals("Byte default", (byte)23, settings.BYTE_SETTING.getValue());
        assertEquals("Color default", new Color(255,127,63), 
                     settings.COLOR_SETTING.getValue());        

        assertEquals("Int default", 143, settings.INT_SETTING.getValue());
        assertEquals("Long default", 666666, settings.LONG_SETTING.getValue());
        assertEquals("String default", "terrific", settings.STRING_SETTING.getValue());
        assertTrue("char arrays should be equal", 
                   Arrays.equals(new char[]{'a','b','c'}, 
                                 settings.CHAR_ARRAY_SETTING.getValue()));
        assertTrue("string arrays should be equal", 
                   Arrays.equals(new String[]{"string1", "string2", "string3"}, 
                                 settings.STRING_ARRAY_SETTING.getValue()));
        
        assertTrue("file arrays should be equal", 
                   Arrays.equals(files, 
                                 settings.FILE_ARRAY_SETTING.getValue()));
        
        assertEquals("Expiring Int default", 12061980, settings.EXPIRABLE_INT_SETTING.getValue());
        assertEquals("Expiring Boolean default", false, settings.EXPIRABLE_BOOLEAN_SETTING.getValue());
        
        // Confirm that we can set everything 
        settings.BOOL_SETTING.setValue(false);
        settings.BYTE_SETTING.setValue((byte)6);
        settings.COLOR_SETTING.setValue(new Color(66, 44, 67));
        settings.FILE_SETTING.setValue(new File(testFilePath));
        settings.INT_SETTING.setValue(234);
        settings.LONG_SETTING.setValue(555555);
        settings.STRING_SETTING.setValue("OK so far");
        settings.CHAR_ARRAY_SETTING.setValue(new char[] {'d', 'e', 'f'});
        settings.STRING_ARRAY_SETTING.setValue(new String[]{"OK", "so", "far"});
        if(OSUtils.isUnix() || OSUtils.isMacOSX())
            settings.FILE_ARRAY_SETTING.setValue(new File[]{new File("/temp/A"), new File("/temp/B")});
        else
            settings.FILE_ARRAY_SETTING.setValue(new File[]{new File("c:/temp/A"), new File("c:/temp/B")});
        settings.EXPIRABLE_INT_SETTING.setValue(0xFFFF);
        settings.EXPIRABLE_BOOLEAN_SETTING.setValue(true);
        
        assertEquals("Bool set", false, settings.BOOL_SETTING.getValue());
        assertEquals("Byte set", (byte)6, settings.BYTE_SETTING.getValue());
        assertEquals("Color set", new Color(66,44,67), settings.COLOR_SETTING.getValue());
        assertEquals("File set", new File(testFilePath), 
                                 settings.FILE_SETTING.getValue());
        assertEquals("Int set", 234, settings.INT_SETTING.getValue());
        assertEquals("Long set", 555555, settings.LONG_SETTING.getValue());
        assertEquals("String set", "OK so far", settings.STRING_SETTING.getValue());
        assertTrue("char arrays should be equal", 
                   Arrays.equals(new char[]{'d','e','f'}, 
                                 settings.CHAR_ARRAY_SETTING.getValue()));
        assertTrue("string arrays should be equal", 
                   Arrays.equals(new String[]{"OK", "so", "far"}, 
                                 settings.STRING_ARRAY_SETTING.getValue()));
        assertTrue("file arrays should be equal", 
                   Arrays.equals(testFileArray, 
                                 settings.FILE_ARRAY_SETTING.getValue()));
                                 
        assertEquals("Expiring Int set", 0xFFFF, settings.EXPIRABLE_INT_SETTING.getValue());
        assertEquals("Expiring Boolean set", true, settings.EXPIRABLE_BOOLEAN_SETTING.getValue());
                                 
        // Write property to file and confirm that everything reloads properly 
        settings.FACTORY.save();
        
        settings = new TestSettings(settingsFile);
        assertEquals("Bool set", false, settings.BOOL_SETTING.getValue());
        assertEquals("Byte set", (byte)6, settings.BYTE_SETTING.getValue());
        assertEquals("Color set", new Color(66,44,67), settings.COLOR_SETTING.getValue());
        assertEquals("File set", new File(testFilePath), 
                                 settings.FILE_SETTING.getValue());
        assertEquals("Int set", 234, settings.INT_SETTING.getValue());
        assertEquals("Long set", 555555, settings.LONG_SETTING.getValue());
        assertEquals("String set", "OK so far", settings.STRING_SETTING.getValue());
        assertTrue("char arrays should be equal", 
                   Arrays.equals(new char[]{'d','e','f'}, 
                                 settings.CHAR_ARRAY_SETTING.getValue()));
        assertTrue("string arrays should be equal", 
                   Arrays.equals(new String[]{"OK", "so", "far"}, 
                                 settings.STRING_ARRAY_SETTING.getValue()));
        assertTrue("file arrays should be equal", 
                   Arrays.equals(testFileArray, 
                                 settings.FILE_ARRAY_SETTING.getValue()));
        
        assertEquals("Expiring Int set", 0xFFFF, settings.EXPIRABLE_INT_SETTING.getValue());
        assertEquals("Expiring Boolean set", true, settings.EXPIRABLE_BOOLEAN_SETTING.getValue());
                                 
        // Confirm that the backup object still has its default settings 
        assertEquals("Bool default", true, settings2.BOOL_SETTING.getValue());
        assertEquals("Byte default", (byte)23, settings2.BYTE_SETTING.getValue());
        assertEquals("Color default", new Color(255,127,63), settings2.COLOR_SETTING.getValue());
        assertEquals("File default", new File(testFilePath2), 
                                     settings2.FILE_SETTING.getValue());
        assertEquals("Int default", 143, settings2.INT_SETTING.getValue());
        assertEquals("Long default", 666666, settings2.LONG_SETTING.getValue());
        assertEquals("String default", "terrific", settings2.STRING_SETTING.getValue());
        assertTrue("char arrays should be equal", 
                   Arrays.equals(new char[]{'a','b','c'}, 
                                 settings2.CHAR_ARRAY_SETTING.getValue()));
        assertTrue("string arrays should be equal", 
                   Arrays.equals(new String[]{ "string1", "string2", "string3" }, 
                                 settings2.STRING_ARRAY_SETTING.getValue()));
        assertTrue("file arrays should be equal", 
                   Arrays.equals(files, 
                                 settings2.FILE_ARRAY_SETTING.getValue()));
        
        assertEquals("Expiring Int default", 12061980, settings2.EXPIRABLE_INT_SETTING.getValue());
        assertEquals("Expiring Boolean default", false, settings2.EXPIRABLE_BOOLEAN_SETTING.getValue());
        
        // reload it from the real file and make sure we got everything 
        settings2.FACTORY.reload();
        assertEquals("Bool set", false, settings2.BOOL_SETTING.getValue());
        assertEquals("Byte set", (byte)6, settings2.BYTE_SETTING.getValue());
        assertEquals("Color set", new Color(66,44,67), settings2.COLOR_SETTING.getValue());
        assertEquals("File set", new File(testFilePath), 
                                 settings2.FILE_SETTING.getValue());
        assertEquals("Int set", 234, settings2.INT_SETTING.getValue());
        assertEquals("Long set", 555555, settings2.LONG_SETTING.getValue());
        assertEquals("String set", "OK so far", settings2.STRING_SETTING.getValue());
        assertTrue("char arrays should be equal", 
                   Arrays.equals(new char[]{'d','e','f'}, 
                                 settings2.CHAR_ARRAY_SETTING.getValue()));
        assertTrue("string arrays should be equal", 
                   Arrays.equals(new String[]{"OK", "so", "far"}, 
                                 settings2.STRING_ARRAY_SETTING.getValue()));
        assertTrue("file arrays should be equal", 
                   Arrays.equals(testFileArray, 
                                 settings2.FILE_ARRAY_SETTING.getValue()));
                                 
		// proxy file settings
		// proxy is still unset so it should return the default setting's file
		assertEquals("Proxy default comparison", 
				settings2.PROXY_FILE_SETTING.getValue(),
				settings2.FILE_SETTING.getValue());
		// set new value
		File proxyFile = new File("proxy-file-test");
		settings2.PROXY_FILE_SETTING.setValue(proxyFile);
		assertNotEquals("Proxy set value comparison",
				settings2.PROXY_FILE_SETTING.getValue(),
				settings2.FILE_SETTING.getValue());
		// revert to default
		settings2.PROXY_FILE_SETTING.revertToDefault();
		assertEquals("Proxy default comparison after revert", 
				settings2.PROXY_FILE_SETTING.getValue(),
				settings2.FILE_SETTING.getValue());
		
        assertEquals("Expiring Int set", 0xFFFF, settings2.EXPIRABLE_INT_SETTING.getValue());
        assertEquals("Expiring Boolean set", true, settings2.EXPIRABLE_BOOLEAN_SETTING.getValue());
        
        settings2.FACTORY.save();
        
        // Change the date to NOW - 14days
        settings = new TestSettings(settingsFile);
        Properties PROPS = settings.FACTORY.getProperties();
        PROPS.setProperty(LAST_EXPIRE_TIME, String.valueOf(System.currentTimeMillis()-EXPIRY_INTERVAL));
        settings.FACTORY.save();
        
        // Do they have their default values now?
        settings2 = new TestSettings(settingsFile);
        assertEquals("Expiring Int default", 12061980, settings2.EXPIRABLE_INT_SETTING.getValue());
        assertEquals("Expiring Int default", true, settings2.EXPIRABLE_INT_SETTING.isDefault());
        
        assertEquals("Expiring Boolean default", false, settings2.EXPIRABLE_BOOLEAN_SETTING.getValue());
        assertEquals("Expiring Boolean default", true, settings2.EXPIRABLE_BOOLEAN_SETTING.isDefault());
    }

    /**
     * Tests the methods of FileArraySetting.
     */
    public void testFileArraySetting() throws Exception {
        File f = new File("testSettings.props");
        f.delete();
        f.deleteOnExit();
        SettingsFactory factory = new SettingsFactory(f);
        FileArraySetting setting = factory.createFileArraySetting("FILE_ARRAY_SETTING", files);
        
        //  test add, remove, contains, indexOf, length
        setting.setValue(new File[0]);
        
        assertEquals("Empty FileArraySetting", setting.length(), 0);
        
        File fileNew = new File("new");
        File fileOld = new File("old");
        setting.add(fileNew);
        
        assertEquals("Expected non-empty FileArraySetting 1", setting.length(), 1);
        assertEquals("Expected index of 0", setting.indexOf(fileNew), 0);
        assertTrue("Expected FileArraySetting to contain new file", setting.contains(fileNew));
        assertFalse("Expected FileArraySetting not to contain old file", setting.contains(fileOld));

        setting.add(fileOld);
        setting.remove(fileNew);
        
        assertEquals("Expected non-empty FileArraySetting 2", setting.length(), 1);
        assertTrue("Expected FileArraySetting to contain old file", setting.contains(fileOld));
        assertFalse("Expected FileArraySetting not to contain new file", setting.contains(fileNew));
        
        assertEquals("Expected index of 0", setting.indexOf(fileOld), 0);
        assertEquals("Expected index of -1", setting.indexOf(fileNew), -1);
        
        //  test adding null
        setting.add(null);
        assertEquals("adding null should not work", setting.length(), 1);
        assertFalse("containing null should be ok", setting.contains(null));
        assertEquals("Expected index of -1", setting.indexOf(null), -1);
        setting.remove(null);
        assertEquals("removing null should be ok", setting.length(), 1);
        assertFalse("containing null should be ok", setting.contains(null));
        assertEquals("Expected index of -1", setting.indexOf(null), -1);
    }
    
    public void testProbabilisticBoolean() throws Exception {
        File f = new File("testSettings.props");
        f.delete();
        f.deleteOnExit();
        SettingsFactory factory = new SettingsFactory(f);
        int yes = 0;
        int no = 0;
        ProbabilisticBooleanSetting s = factory.createProbabilisticBooleanSetting("a", 0);
        for (int i = 0; i < 1000; i++) {
            s.setValue(0f);
            if (s.getBoolean()) yes++; 
            else  no++;
        }
        assertEquals(0, yes);
        assertEquals(1000, no);
        
        yes = 0; no = 0;
        for (int i = 0; i < 1000; i++) {
            s.setValue(1.0f);
            if (s.getBoolean()) yes++; 
            else  no++;
        }
        
        assertEquals(1000, yes);
        assertEquals(0, no);
        
        yes = 0; no = 0;
        for (int i = 0; i < 1000; i++) {
            s.setValue(i % 2 == 0 ? 0.8f : 0.7f);
            if (s.getBoolean()) yes++; 
            else  no++;
        }
        
        // should be more yes than no but still some no's
        assertGreaterThan(0, no);
        assertGreaterThan(no, yes);
        
        // test that setting the value to 0 will never return true
        // this is a best effort and most of the time will pass even 
        // if logic is incorrect, but it if its correct should pass always.
        for (int i = 0; i < 9000000; i++) {
            s.setValue(0.1f);
            s.setValue(0f);
            assertFalse(s.getBoolean());
        }
    }
    
    public void testPBooleanArraySetting() throws Exception {
        File f = new File("testSettings.props");
        f.delete();
        f.deleteOnExit();
        SettingsFactory factory = new SettingsFactory(f);
        
        PBooleanArraySetting set = factory.createPBooleanArraySetting("a", new String[0]);
        
        // test all true
        set.setValue("1.0","1.0","1.0","1.0");
        assertEquals(4,set.length());
        for (int i = 0; i < set.length(); i++)
            assertTrue(set.get(i));
        
        // test all false
        set.setValue("0.0","0.0","0.0","0.0","0.0");
        assertEquals(5,set.length());
        for (int i = 0; i < set.length(); i++)
            assertFalse(set.get(i));
        
        // if all are set to 0.5, about half should be true
        String [] values = new String[1000];
        for (int i = 0; i < 1000; i++)
            values[i] = "0.5";
        set.setValue(values);
        assertEquals(1000, set.length());
        BitSet bitset = new BitSet(1000);
        for (int i = 0; i < 1000; i++) {
            if (set.get(i))
                bitset.set(i);
        }
        // allow +- 50
        assertGreaterThan(450, bitset.cardinality());
        assertLessThan(550, bitset.cardinality());
        
        // but which ones are set should be different every time
        set.setValue("0.0");
        set.setValue(values);
        BitSet other = new BitSet(1000);
        for (int i = 0; i < 1000; i++) {
            if (set.get(i))
                other.set(i);
        }
        bitset.xor(other);
        assertGreaterThan(0, bitset.cardinality());
    }
    
    public void testSetAlwaysSaveTrue() throws Exception {
        File f = new File("testSettings.props");
        f.delete();
        f.deleteOnExit();
        
        SettingsFactory factory = new SettingsFactory(f);        
        StringSetting setting = factory.createStringSetting("KEY", "defaultValue");
        setting.setAlwaysSave(true);
        
        factory.save();
     
        // assert that the default value was written out, because
        // setAlwaysSave(true) was called.
        Properties readProps = new Properties();
        readProps.load(new FileInputStream(f));
        assertEquals("defaultValue", readProps.get("KEY"));
    }
}
