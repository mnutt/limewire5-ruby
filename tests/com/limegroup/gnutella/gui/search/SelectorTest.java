package com.limegroup.gnutella.gui.search;

import com.limegroup.gnutella.gui.GUIBaseTestCase;

public class SelectorTest extends GUIBaseTestCase {

    String[] keys = new String[] {
            "RESULT_PANEL_TYPE", "RESULT_PANEL_SPEED", "RESULT_PANEL_VENDOR"
    };
    
    String[] titles = new String[] {
            "Type", "Speed", "Vendor"
    };
    
    String[] properties = new String[] {
            "property, RESULT_PANEL_TYPE | false",
            "property, RESULT_PANEL_SPEED | false",
            "property, RESULT_PANEL_VENDOR | true"
    };
    
    public SelectorTest(String name) {
        super(name);
        
    }

    public void testCreatePropertySelector() {
        for (int i = 0; i < keys.length; i++) {
            Selector selector = Selector.createPropertySelector(keys[i]);
            assertEquals(keys[i], selector.getValue());
            // this assumes the english resource bundle is used for tests
            assertEquals(titles[i], selector.getTitle());
        }
    }
    
    public void testCreatePropertySelectorFromInvalidKey() {
        try {
            Selector.createPropertySelector("blabh");
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException iae) {
        }
    }
    
    public void testCreateFromStringPropertySelector() {
        for (int i = 0; i < properties.length; i++) {
            Selector selector = Selector.createFromString(properties[i]);
            assertEquals(keys[i], selector.getValue());
            // this assumes the english resource bundle is used for tests
            assertEquals(titles[i], selector.getTitle());
        }
    }

}
