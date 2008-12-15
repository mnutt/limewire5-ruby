package com.limegroup.gnutella.gui;

import java.util.Locale;

import org.limewire.core.settings.ApplicationSettings;

import junit.framework.Test;


public class ResourceManagerTest extends GUIBaseTestCase {

	public ResourceManagerTest(String name) {
		super(name);
	}
	
    public static Test suite() {
        return buildTestSuite(ResourceManagerTest.class);
    }
	
	public void testResetLocaleOptions() {
		setLocaleSettings(Locale.US);
		assertEquals(Locale.US, ResourceManager.getLocale());
		setLocaleSettings(Locale.GERMAN);
		assertEquals(Locale.GERMAN, ResourceManager.getLocale());
		setLocaleSettings(Locale.GERMANY);
		assertEquals(Locale.GERMANY, ResourceManager.getLocale());
	}
	
	public void testResetLocaleOptionsWithDifferentOptions() {
	    String[][] data = new String[][] {
	            new String[] { "en", "US" },
	            new String[] { "es", "GT" },
	            new String[] { "es", "ES" },
	            new String[] { "pl", "PL" },
	            new String[] { "es", "VE" },
	            new String[] { "es", "MX" },
	            new String[] { "es", "CO" },
	            new String[] { "en" },
	            new String[] { "es" },
	            new String[] { "en", "GB" },
	            new String[] { "tr", "TR" },
	            // nonsensical 
	            new String[] { "fx", "BR" }
	    };
	    for (String[] entry : data) {
	        setLocaleOptions(entry);
	        ResourceManager.resetLocaleOptions();
	    }
	}
	
	private void setLocaleOptions(String... args) {
	    ApplicationSettings.LANGUAGE.setValue(args[0]);
	    if (args.length > 1) {
	        ApplicationSettings.COUNTRY.setValue(args[1]);
	    }
	    if (args.length > 2) {
	        ApplicationSettings.LOCALE_VARIANT.setValue(args[2]);
	    }
	}

}
