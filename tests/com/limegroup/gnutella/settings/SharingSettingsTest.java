package com.limegroup.gnutella.settings;

import java.io.File;

import org.limewire.core.settings.SharingSettings;
import org.limewire.util.MediaType;

import junit.framework.Test;

import com.limegroup.gnutella.util.LimeTestCase;

public class SharingSettingsTest extends LimeTestCase {

	public SharingSettingsTest(String name) {
		super(name);
	}
	
	public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public static Test suite() {
        return buildTestSuite(SharingSettingsTest.class);
    }

    /**
     * Tests if all settings are set to the default save directory.
     */
	public void testUnsetMediaTypeDirectories() {
		MediaType[] types = MediaType.getDefaultMediaTypes();
		for (int i = 0; i < types.length; i++) {
			assertEquals("Should be the save directory", 
					SharingSettings.getSaveDirectory(null),
					SharingSettings.getFileSettingForMediaType(types[i]).getValue());
							
		}
	}
	
	public void testSetMediaTypeDirectories() throws Exception {
		
		File tmpFile = File.createTempFile("prefix", "postfix");
		tmpFile.deleteOnExit();
		File dir = new File(tmpFile.getParentFile(), "subdir");
		dir.mkdir();
		dir.deleteOnExit();
		
		// set all mediatype directories
		MediaType[] types = MediaType.getDefaultMediaTypes();
		for (int i = 0; i < types.length; i++) {
			SharingSettings.getFileSettingForMediaType(types[i]).setValue(dir);
		}
		
		// test if they are all set
		for (int i = 0; i < types.length; i++) {
			assertEquals("Should be the set directory", 
					dir,
					SharingSettings.getFileSettingForMediaType(types[i]).getValue());
		}
		
		// revert them
		for (int i = 0; i < types.length; i++) {
			SharingSettings.getFileSettingForMediaType(types[i]).revertToDefault();
		}
		
		// check if they are reverted
		for (int i = 0; i < types.length; i++) {
			assertTrue("Should be default", 
					SharingSettings.getFileSettingForMediaType(
							types[i]).isDefault());
			assertEquals("Should be the save directory", 
					SharingSettings.getSaveDirectory(null),
					SharingSettings.getFileSettingForMediaType(types[i]).getValue());
		}
	}
	
    public void testSetStoreDirectory() throws Exception {
        
        File tmpFile = File.createTempFile("prefix", "postfix");
        tmpFile.deleteOnExit();
        File dir = new File(tmpFile.getParentFile(), "subdir");
        dir.mkdir();
        dir.deleteOnExit();
                
        // test the lws directory
        SharingSettings.setSaveLWSDirectory(dir);
        
        assertEquals("Should be the set directory", 
                dir.getCanonicalPath(),
                SharingSettings.getSaveLWSDirectory().getCanonicalPath());
        
        SharingSettings.setSaveLWSDirectory(SharingSettings.DEFAULT_SAVE_LWS_DIR);
        
        assertEquals("Should be the save directory", 
                SharingSettings.getSaveLWSDirectory(),
                SharingSettings.DEFAULT_SAVE_LWS_DIR);
    }
	
	public void testGetSaveDirectoryForNoFilenameLabel() {
		assertEquals(SharingSettings.getSaveDirectory(null), SharingSettings.getSaveDirectory("No Filename"));
	}
}
