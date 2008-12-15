package org.limewire.setting;

import java.util.Properties;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

public class PropertiesSettingTest extends BaseTestCase {

    public PropertiesSettingTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(PropertiesSettingTest.class);
    }

    public void testToString() {
        Properties props = new Properties();
        props.setProperty("key", "value");
        Properties props2 = new Properties();
        props2.setProperty("props", PropertiesSetting.toString(props));
        Properties props3 = PropertiesSetting.fromString(PropertiesSetting.toString(props2));
        String deserialized = props3.getProperty("props");
        assertNotNull(deserialized);
        Properties deserializedProps = PropertiesSetting.fromString(deserialized);
        assertEquals("value", deserializedProps.getProperty("key"));
    }
}
