package com.limegroup.gnutella.gui.startup;

import org.limewire.util.BaseTestCase;

public class StartupBannerTest extends BaseTestCase {

    public StartupBannerTest(String name) {
        super(name);
    }
    
    public void testNullInput(){
        try {
            new StartupBanner((String[])null);
            fail("should not have accepted null input");
        } catch(IllegalArgumentException expected) {}
    }
    
    public void testEmptyStringInput(){
        try {
            new StartupBanner("");
            fail("should not have accepted empty input");
        } catch(IllegalArgumentException expected) {}
    }
    
    public void testWrongLengthString() {
        try {
            new StartupBanner("1 \t 2 \t 3 \t 4 \t 5 \t 6 \t 7 \t");
            fail("should not have accepted invalid length input");
        } catch(IllegalArgumentException expected) {}
    }
    
    public void testSingleAd() {
        StartupBanner banner = new StartupBanner("1\t2\t3\t4\t5\t6\t7\t8\t9\t10\t11\t12\t13\t14");
        
        assertEquals(1, banner.getAllAds().size());
        
        StartupAd ad = banner.getRandomAd(); 
        
        assertEquals("1", ad.getTitle());
        assertEquals("2", ad.getMessage());
        assertEquals("3", ad.getButtonMessage());
        assertEquals("4", ad.getButton1Text());
        assertEquals("5", ad.getButton1ToolTip());
        assertEquals("6", ad.getButton2Text());
        assertEquals("7", ad.getButton2ToolTip());
        assertEquals("8", ad.getButton3Text());
        assertEquals("9", ad.getButton3ToolTip());
        assertEquals("10", ad.getURLButton1());
        assertEquals("11", ad.getURLButton2());
        assertEquals("12", ad.getURLButton3());
        assertEquals("13", ad.getURLImage());
        assertEquals(14f, ad.getProbability());
    }
    
    public void testTwoAds() {
        StartupBanner banner = new StartupBanner( new String[]{"1\t2\t3\t4\t5\t6\t7\t8\t9\t10\t11\t12\t13\t14",
                    "1\t2\t3\t4\t5\t6\t7\t8\t9\t10\t11\t12\t13\t14"});
        
        assertEquals(2, banner.getAllAds().size());
    }
    
    public void testOneGoodOneBad() {
        StartupBanner banner = new StartupBanner( new String[]{"1\t2\t3\t4\t5\t6\t7\t8\t9\t10\t11\t12\t13\t14",
                    "invalid ad"});
        
        assertEquals(1, banner.getAllAds().size());
    }
    
    public void testNullTabDelimited() {
        StartupBanner banner = new StartupBanner( new String[]{"\t\t\t\t\t\t\t\t\t\t\t\t\t14"});
        
        assertEquals(1, banner.getAllAds().size());
        
        StartupAd ad = banner.getRandomAd(); 
        
        assertEquals(null, ad.getTitle());
        assertEquals(null, ad.getMessage());
        assertEquals(null, ad.getButtonMessage());
        assertEquals(null, ad.getButton1Text());
        assertEquals(null, ad.getButton1ToolTip());
        assertEquals(null, ad.getButton2Text());
        assertEquals(null, ad.getButton2ToolTip());
        assertEquals(null, ad.getButton3Text());
        assertEquals(null, ad.getButton3ToolTip());
        assertEquals(null, ad.getURLButton1());
        assertEquals(null, ad.getURLButton2());
        assertEquals(null, ad.getURLButton3());
        assertEquals(null, ad.getURLImage());
        assertEquals(14f, ad.getProbability());
    }
    
    

}
