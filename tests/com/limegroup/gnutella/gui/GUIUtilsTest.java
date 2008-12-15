package com.limegroup.gnutella.gui;

import java.awt.Color;
import java.io.File;
import java.util.Locale;

import junit.framework.Test;

import org.limewire.util.FileUtils;


public class GUIUtilsTest extends GUIBaseTestCase {

	public GUIUtilsTest(String name) {
		super(name);
	}
	
	public static Test suite() {
        return buildTestSuite(GUIUtilsTest.class);
    }

	public void testToUnitBytesGerman() {
		setLocaleSettings(Locale.GERMAN);
		assertEquals("1,5 KB", GUIUtils.toUnitbytes(1536));
	}
    
    public void testUnitBytesBoundaries() {
        setLocaleSettings(Locale.US);
        assertEquals("0 B", GUIUtils.toUnitbytes(0));
        assertEquals("1 B", GUIUtils.toUnitbytes(1));
        assertEquals("10 B", GUIUtils.toUnitbytes(10));
        assertEquals("51 B", GUIUtils.toUnitbytes(51));
        assertEquals("52 B", GUIUtils.toUnitbytes(52));
        assertEquals("53 B", GUIUtils.toUnitbytes(53));
        assertEquals("99 B", GUIUtils.toUnitbytes(99));
        assertEquals("0.1 KB", GUIUtils.toUnitbytes(100));
        assertEquals("0.2 KB", GUIUtils.toUnitbytes(232));
        
        int TEN_MBS = 10 * 1024 * 1024;
        int TENTH_OF_A_MB = 1024 * 1024 / 10;        
        assertEquals("10,240 KB", GUIUtils.toUnitbytes(TEN_MBS-1));
        assertEquals("10,239 KB", GUIUtils.toUnitbytes(TEN_MBS-1024));
        assertEquals("10.0 MB", GUIUtils.toUnitbytes(TEN_MBS));
        assertEquals("10.1 MB", GUIUtils.toUnitbytes(TEN_MBS + TENTH_OF_A_MB));
        
        long TEN_GBS = TEN_MBS * 1024L;
        long TENTH_OF_A_GB = TENTH_OF_A_MB * 1024L;
        assertEquals("10,240 MB", GUIUtils.toUnitbytes(TEN_GBS-1L));
        assertEquals("10,239 MB", GUIUtils.toUnitbytes(TEN_GBS - 1024L*1024L));
        assertEquals("10.0 GB", GUIUtils.toUnitbytes(TEN_GBS));
        assertEquals("10.1 GB", GUIUtils.toUnitbytes(TEN_GBS + TENTH_OF_A_GB));
        
        long TEN_TBS = TEN_GBS * 1024L;
        long TENTH_OF_A_TB = TENTH_OF_A_GB * 1024L;
        assertEquals("10,240 GB", GUIUtils.toUnitbytes(TEN_TBS-1L));
        assertEquals("10,239 GB", GUIUtils.toUnitbytes(TEN_TBS - 1024L*1024L*1024L));
        assertEquals("10.0 TB", GUIUtils.toUnitbytes(TEN_TBS));
        assertEquals("10.1 TB", GUIUtils.toUnitbytes(TEN_TBS + TENTH_OF_A_TB));
        
        long TEN_PBS = TEN_TBS * 1024L;
        assertEquals("10,240 TB", GUIUtils.toUnitbytes(TEN_PBS));
        assertEquals("10,241 TB", GUIUtils.toUnitbytes(TEN_PBS + 1024L*1024L*1024L*1024L));
    }

    public void testLaunchFileWithoutExtension() {
        File file = new File("extensionless");
        assertEquals("", FileUtils.getFileExtension(file));
        GUIUtils.launchOrEnqueueFile(file, false);
    }
    
    public void testHexToColor(){       
        //test white
        assertEquals(Color.WHITE, GUIUtils.hexToColor("FFFFFF"));
        
        //test blue
        assertEquals(Color.BLUE, GUIUtils.hexToColor("0000FF"));
        
        //test yellow
        assertEquals(Color.YELLOW, GUIUtils.hexToColor("FFFF00"));
    }
    
    public void testColorToHex(){
        //test white
        assertEquals("FFFFFF",GUIUtils.colorToHex(Color.WHITE));
        
        //test blue       
        assertEquals("0000FF",GUIUtils.colorToHex(Color.BLUE));
        
        //test yellow
        assertEquals("FFFF00",GUIUtils.colorToHex(Color.YELLOW));
    }
}
