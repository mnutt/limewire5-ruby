package com.limegroup.gnutella.version;

import junit.framework.Test;

import org.limewire.core.api.updates.UpdateStyle;
import org.limewire.util.OSUtils;
import org.limewire.util.PrivilegedAccessor;
import org.limewire.util.Version;

import com.google.inject.Injector;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.util.LimeTestCase;

public final class UpdateCollectionTest extends LimeTestCase {

    UpdateCollectionFactory updateCollectionFactory;
    
	public UpdateCollectionTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(UpdateCollectionTest.class);
	}
	
	@Override
	protected void setUp() throws Exception {
		Injector injector = LimeTestUtils.createInjector();
		updateCollectionFactory = injector.getInstance(UpdateCollectionFactory.class);
	}

	/**
	 * Runs this test individually.
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	public void testBasicCreation() throws Exception {
	    
	    UpdateCollection uc = updateCollectionFactory.createUpdateCollection("<update id='42' timestamp=\"150973213135\">" +
            "<msg for='4.6.0' url='http://www.limewire.com/update' style='2'>" +
                "<lang id='en'>" +
                    "<![CDATA[<html><body>This is the text</body></html>]]>" +
                "</lang>" +
                "<lang id='es' button1='b1' button2='b2'>" +
                    "Hola, no habla espanol." +
                "</lang>" +	                
                "<lang id='notext'></lang>" +
            "</msg>" +
            "<msg/> " +
            "<msg for='4.1.2' url='http://limewire.com/hi'>" +
                "<lang id='en'>" + 
                    "This didn't have a style, it should be ignored." +
                "</lang>" +
            "</msg>" +
            "<msg for='4.1.2' style='3'>" +
                "<lang id='en'>" + 
                    "This didn't have a URL, it should be ignored." +
                "</lang>" +
            "</msg>" +
            "<msg style='3' url='nostyle'>" +
                "<lang id='en'>" + 
                    "This didn't have a 'for', it should be ignored." +
                "</lang>" +
            "</msg>" +	            	            
        "</update>");
	        
        // First make sure it ignored the invalid msgs.
        assertEquals(uc.getUpdateData().toString(), 2, uc.getUpdateData().size());
        assertEquals(42, uc.getId());
        assertEquals(150973213135L, uc.getTimestamp());
	    
	    UpdateData data;
	    
        // if we already have 4.6.0, this should find nothing.     
	    data = uc.getUpdateDataFor(new Version("4.6.0"), "en", false, UpdateStyle.STYLE_MAJOR, null);
	    assertNull(data);
	    
	    // if we're above 4.6.0, this should find nothing.
	    data = uc.getUpdateDataFor(new Version("4.7.0"), "en", false, UpdateStyle.STYLE_MAJOR, null);
	    assertNull(data);
	    
	    // if we only want critical updates, this should find nothing.
	    data = uc.getUpdateDataFor(new Version("0.0.0"), "en", false, UpdateStyle.STYLE_CRITICAL, null);
	    assertNull(data);
	    
	    // find the english version.
	    data = uc.getUpdateDataFor(new Version("0.0.0"), "en", false, UpdateStyle.STYLE_MAJOR, null);
	    assertEquals("en", data.getLanguage());
	    assertEquals("<html><body>This is the text</body></html>", data.getUpdateText());
	    assertEquals("4.6.0", data.getUpdateVersion());
	    assertEquals(UpdateStyle.STYLE_MAJOR, data.getUpdateStyle());
	    assertNull(data.getButton1Text());
	    assertNull(data.getButton2Text());
	    
	    // find the spanish version.
	    data = uc.getUpdateDataFor(new Version("4.5.123509781 Pro"), "es", true, UpdateStyle.STYLE_MINOR, null);
	    assertEquals("es", data.getLanguage());
	    assertEquals("Hola, no habla espanol.", data.getUpdateText());
	    assertEquals("4.6.0", data.getUpdateVersion());
	    assertEquals(UpdateStyle.STYLE_MAJOR, data.getUpdateStyle());
	    assertEquals("b1", data.getButton1Text());
	    assertEquals("b2", data.getButton2Text());
	    
	    // can't find deutch, so defaults to english.
	    data = uc.getUpdateDataFor(new Version("4.0.0"), "de", false, UpdateStyle.STYLE_BETA, null);
	    assertEquals("en", data.getLanguage());
	    assertEquals("<html><body>This is the text</body></html>", data.getUpdateText());
	    assertEquals("4.6.0", data.getUpdateVersion());
	    assertEquals(UpdateStyle.STYLE_MAJOR, data.getUpdateStyle());
	    assertNull(data.getButton1Text());
	    assertNull(data.getButton2Text());
    }
    
    public void testRanges() throws Exception {
	    UpdateCollection uc = updateCollectionFactory.createUpdateCollection("<update id='42'>" +
            "<msg to='3.0.0' for='4.6.0' url='http://www.limewire.com/update/force' style='4'>" +
                "<lang id='en'>FORCED Text</lang>" +
            "</msg>" +
            "<msg from='3.0.0' to='4.0.0' for='4.6.0' url='http://www.limewire.com/update/old' style='2'>" +
                "<lang id='en'>Major Text (really old version)</lang>" +
            "</msg>" +
            "<msg from='4.0.0' for='4.6.0' url='http://www.limewire.com/update' style='2'>" +
                "<lang id='en'>Major Text</lang>" +
            "</msg>" +
            "<msg from='4.6.0' for='4.6.5' url='http://www.limewire.com/update' style='1'>" +
                "<lang id='en'>Text</lang>" +
            "</msg>" +
            "<msg from='4.6.5' to='4.7.2' for='4.7.3' url='http://www.limewire.com/beta' style='0'>" +
                "<lang id='en'>Text</lang>" +
            "</msg>" +
            "<msg from='4.8.0' for='4.8.3' url='http://www.limewire.com/beta' style='3'>" +
                "<lang id='en'>Text</lang>" +
            "</msg>" +	            
        "</update>");
	        
        assertEquals(uc.getUpdateData().toString(), 6, uc.getUpdateData().size());
        assertEquals(42, uc.getId());
	    
	    // Idea:
	    // People who have [0.0.0, 3.0.0) are told about a FORCED update to 4.6.0 (with one set of text)
	    // People who have [3.0.0, 4.0.0) are told about a MAJOR update to 4.6.0  (with another set of text)
	    // People who have [4.0.0, 4.6.0) are told about a MAJOR update to 4.6.0  (with yet another set of text)
	    // People who have [4.6.0, 4.6.5) are told about a SERVICE update to 4.6.5
	    // People who have [4.6.5, 4.7.2) are told about a BETA update to 4.7.3
	    // People who have [4.8.0, 4.8.3) are told about a CRITICAL update to 4.8.3
	    // Note that the upper boundary is always exlusive, whereas the lower boundary is inclusive.
	    UpdateData data;
	    
	    data = uc.getUpdateDataFor(new Version("2.0.0"), "en", false, UpdateStyle.STYLE_BETA, null);
	    assertEquals("4.6.0", data.getUpdateVersion());
	    assertEquals("FORCED Text", data.getUpdateText());
	    data = uc.getUpdateDataFor(new Version("2.0.0"), "en", false, UpdateStyle.STYLE_MINOR, null);
	    assertEquals("4.6.0", data.getUpdateVersion());
	    assertEquals("FORCED Text", data.getUpdateText());
	    data = uc.getUpdateDataFor(new Version("2.0.0"), "en", false, UpdateStyle.STYLE_MAJOR, null);
	    assertEquals("4.6.0", data.getUpdateVersion());
	    assertEquals("FORCED Text", data.getUpdateText());
	    
	    data = uc.getUpdateDataFor(new Version("3.0.0"), "en", false, UpdateStyle.STYLE_BETA, null);
	    assertEquals("4.6.0", data.getUpdateVersion());
	    assertEquals("Major Text (really old version)", data.getUpdateText());
	    data = uc.getUpdateDataFor(new Version("3.0.0"), "en", false, UpdateStyle.STYLE_MINOR, null);
	    assertEquals("4.6.0", data.getUpdateVersion());
	    assertEquals("Major Text (really old version)", data.getUpdateText());
	    data = uc.getUpdateDataFor(new Version("3.0.0"), "en", false, UpdateStyle.STYLE_MAJOR, null);
	    assertEquals("4.6.0", data.getUpdateVersion());
	    assertEquals("Major Text (really old version)", data.getUpdateText());

	    data = uc.getUpdateDataFor(new Version("4.0.0"), "en", false, UpdateStyle.STYLE_BETA, null);
	    assertEquals("4.6.0", data.getUpdateVersion());
	    assertEquals("Major Text", data.getUpdateText());
	    data = uc.getUpdateDataFor(new Version("4.0.0"), "en", false, UpdateStyle.STYLE_MINOR, null);
	    assertEquals("4.6.0", data.getUpdateVersion());
	    assertEquals("Major Text", data.getUpdateText());
	    data = uc.getUpdateDataFor(new Version("4.0.0"), "en", false, UpdateStyle.STYLE_MAJOR, null);
	    assertEquals("4.6.0", data.getUpdateVersion());
	    assertEquals("Major Text", data.getUpdateText());

	    data = uc.getUpdateDataFor(new Version("4.6.0"), "en", false, UpdateStyle.STYLE_BETA, null);
	    assertEquals("4.6.5", data.getUpdateVersion());
	    data = uc.getUpdateDataFor(new Version("4.6.0"), "en", false, UpdateStyle.STYLE_MINOR, null);
	    assertEquals("4.6.5", data.getUpdateVersion());
	    data = uc.getUpdateDataFor(new Version("4.6.0"), "en", false, UpdateStyle.STYLE_MAJOR, null);
	    assertNull(data);
	    
	    data = uc.getUpdateDataFor(new Version("4.6.5"), "en", false, UpdateStyle.STYLE_BETA, null);
	    assertEquals("4.7.3", data.getUpdateVersion());
	    data = uc.getUpdateDataFor(new Version("4.6.5"), "en", false, UpdateStyle.STYLE_MINOR, null);
	    assertNull(data);
	    data = uc.getUpdateDataFor(new Version("4.6.5"), "en", false, UpdateStyle.STYLE_MAJOR, null);
	    assertNull(data);
	    
	    data = uc.getUpdateDataFor(new Version("4.7.1"), "en", false, UpdateStyle.STYLE_BETA, null);
	    assertEquals("4.7.3", data.getUpdateVersion());
	    data = uc.getUpdateDataFor(new Version("4.7.2"), "en", false, UpdateStyle.STYLE_BETA, null);
	    assertNull(data);
	    
	    data = uc.getUpdateDataFor(new Version("4.8.0"), "en", false, UpdateStyle.STYLE_BETA, null);
	    assertEquals("4.8.3", data.getUpdateVersion());
	    data = uc.getUpdateDataFor(new Version("4.8.0"), "en", false, UpdateStyle.STYLE_MINOR, null);
	    assertEquals("4.8.3", data.getUpdateVersion());
	    data = uc.getUpdateDataFor(new Version("4.8.0"), "en", false, UpdateStyle.STYLE_MAJOR, null);
	    assertEquals("4.8.3", data.getUpdateVersion());
	    data = uc.getUpdateDataFor(new Version("4.8.3"), "en", false, UpdateStyle.STYLE_MAJOR, null);
	    assertNull(data);
    }
    
    public void testProFree() throws Exception {
        UpdateCollection uc = updateCollectionFactory.createUpdateCollection("<update id='42'>" +
            "<msg for='4.6.0' url='http://www.limewire.com/update' style='2' pro='1'>" +
                "<lang id='en'>Pro Text</lang>" +
            "</msg>" +
            "<msg for='4.6.0' url='http://www.limewire.com/update' style='2' free='1'>" +
                "<lang id='en'>Free Text</lang>" +
            "</msg>" +
        "</update>");
	        
        
        UpdateData data;
        
	    data = uc.getUpdateDataFor(new Version("4.0.0"), "en", true, UpdateStyle.STYLE_BETA, null);
	    assertEquals("Pro Text", data.getUpdateText());
	    data = uc.getUpdateDataFor(new Version("4.0.0"), "en", false, UpdateStyle.STYLE_BETA, null);
	    assertEquals("Free Text", data.getUpdateText());
    }
    
    public void testOSRange() throws Exception {
        String defaultOS = OSUtils.getOS();
        String OSVersion = OSUtils.getOSVersion();
        
        try {
            
        boolean _w = false, _m = false, _l = false, _u = false, _o = false;
    
        for(int i = 0; i < 5; i++) {
            switch(i) {
            case 0: setOSName("Windows");  setOsVersion("1"); break;
            case 1: setOSName("Mac OS X"); setOsVersion("2"); break;
            case 2: setOSName("Linux");    setOsVersion("3"); break;
            case 3: setOSName("Solaris");  setOsVersion("4"); break;
            case 4: setOSName("OS/2");     setOsVersion("5"); break;
            }
            
            String currentOS = OSUtils.getOS() + " (on iteration: " + i + ")";        
        
            UpdateCollection uc = updateCollectionFactory.createUpdateCollection("<update id='42'>" +
                "<msg for='4.6.0' url='http://www.limewire.com/update' style='2' os='Windows'>" +
                    "<lang id='en'>Windows Text</lang>" +
                "</msg>" +
                "<msg for='4.6.0' url='http://www.limewire.com/update' style='2' os='Linux'>" +
                    "<lang id='en'>Linux Text</lang>" +
                "</msg>" +
                "<msg for='4.6.0' url='http://www.limewire.com/update' style='2' os='Mac'>" +
                    "<lang id='en'>Mac Text</lang>" +
                "</msg>" +
                "<msg for='4.6.0' url='http://www.limewire.com/update' style='2' os='Unix'>" +
                    "<lang id='en'>Unix Text</lang>" +
                "</msg>" +
                "<msg for='4.6.0' url='http://www.limewire.com/update' style='2' os='Other'>" +
                    "<lang id='en'>Other Text</lang>" +
                "</msg>" +
                "<msg from='4.8.0' for='4.8.3' url='http://www.limewire.com/beta' style='0' os='Mac, Linux, Windows'>" +
                    "<lang id='en'>Windows, Mac, Linux Text</lang>" +
                "</msg>" +
                "<msg from='4.8.0' for='4.8.3' url='http://www.limewire.com/beta' style='0' os='Other, Unix'>" +
                    "<lang id='en'>Other, Unix Text</lang>" +
                "</msg>" +
                "<msg from='4.9.0' for='4.9.3' url='http://www.limewire.com/beta' style='0' os='Mac, Linux, Windows' osv='0.5,1.5,4.0,5.0,0.5,2.0'>" +
                "<lang id='en'>only Windows version</lang>" +
                "</msg>" +
                "<msg from='4.10.0' for='4.10.3' url='http://www.limewire.com/beta' style='0' os='Mac, Linux, Windows' osv='0.5,1.5,*,5.0,2.0,*'>" +
                "<lang id='en'>only Linux version</lang>" +
                "</msg>" +
                "<msg from='4.11.0' for='4.11.3' url='http://www.limewire.com/beta' style='0' os='Mac, Linux, Windows' osv='0.5,1.5,*,5.0,*'>" +
                "<lang id='en'>wrong number of versions</lang>" +
                "</msg>" +
                "<msg from='4.12.0' for='4.12.3' url='http://www.limewire.com/beta' style='0' os='Mac, Linux, Windows' osv='0.5,1.5,*,5.0,asdf,*'>" +
                "<lang id='en'>malformed version</lang>" +
            "</msg>" +
            "</update>");
    	        
            boolean windows = OSUtils.isWindows();
            boolean mac = OSUtils.isMacOSX();
            boolean linux = OSUtils.isLinux();
            boolean unix = OSUtils.isUnix() && !linux;
            boolean other = !windows && !mac && !linux && !unix;
            // make sure only one of these values is true.
            int set = 0;
            if(windows) {
                set++;
                _w = true;
            }
            if(mac) {
                set++;
                _m = true;
            }
            if(linux) {
                set++;
                _l = true;
            }
            if(unix) {
                set++;
                _u = true;
            }
            if(other) {
                set++;
                _o = true;
            }
            assertEquals(1, set);
            
            UpdateData data;
            
    	    data = uc.getUpdateDataFor(new Version("4.0.0"), "en", false, UpdateStyle.STYLE_BETA, null);
    	    assertNotNull(currentOS, data);
    	    if(windows)
    	        assertEquals(currentOS, "Windows Text", data.getUpdateText());
            if(mac)
    	        assertEquals(currentOS, "Mac Text", data.getUpdateText());
    	    if(linux)
    	        assertEquals(currentOS, "Linux Text", data.getUpdateText());
    	    if(unix)
    	        assertEquals(currentOS, "Unix Text", data.getUpdateText());
    	    if(other)
    	        assertEquals(currentOS, "Other Text", data.getUpdateText());
    	        
    	    data = uc.getUpdateDataFor(new Version("4.8.0"), "en", false, UpdateStyle.STYLE_BETA, null);
    	    assertNotNull(currentOS, data);
            if(windows || mac || linux)
                assertEquals(currentOS, "Windows, Mac, Linux Text", data.getUpdateText());
            if(unix || other)
                assertEquals(currentOS, "Other, Unix Text", data.getUpdateText());
            
            // Check the osv limits things
            data = uc.getUpdateDataFor(new Version("4.9.0"), "en", false, UpdateStyle.STYLE_BETA, null);
            if (windows)
                assertEquals(currentOS, "only Windows version", data.getUpdateText());
            else
                assertNull(currentOS, data);
            
            data = uc.getUpdateDataFor(new Version("4.10.0"), "en", false, UpdateStyle.STYLE_BETA, null);
            if (linux)
                assertEquals(currentOS, "only Linux version", data.getUpdateText());
            else
                assertNull(currentOS,data);
            
            assertNull(uc.getUpdateDataFor(new Version("4.11.0"), "en", false, UpdateStyle.STYLE_BETA, null));
            assertNull(uc.getUpdateDataFor(new Version("4.12.0"), "en", false, UpdateStyle.STYLE_BETA, null));
                
        } 
        
        assertTrue("w", _w);
        assertTrue("m", _m);
        assertTrue("l", _l);
        assertTrue("o", _o);
        assertTrue("u", _u);

        } finally {
            setOSName(defaultOS);
            setOsVersion(OSVersion);
        }
    }
    
    public void testJavaRanges() throws Exception {
	    UpdateCollection uc = updateCollectionFactory.createUpdateCollection("<update id='42'>" +
            "<msg for='9.9.9' url='http://www.limewire.com/whyupgradejava' style='4' javato='1.4.2'>" +
                "<lang id='en'>Your Java Sucks.</lang>" +
            "</msg>" +
            "<msg for='9.9.9' url='http://www.limewire.com/whyupgradejava' style='4' javafrom='1.4.2' javato='1.5.0_2'>" +
                "<lang id='en'>Your Java Doesn't Suck.</lang>" +
            "</msg>" +
            "<msg for='9.9.9' url='http://www.limewire.com/whyupgradejava' style='4' javafrom='1.5.0_2'>" +
                "<lang id='en'>Your Java Is Mysterious.</lang>" +
            "</msg>" +
        "</update>");
	    
	    // Idea:
	    // People who have Java [0.0.0, 1.4.2) are told their java sucks.
	    // People who have Java [1.4.2, 1.5.0_02) are told their java doesn't suck.
	    // People who have Java [1.5.0_2, ~) are told their java is mysterious.
	    UpdateData data;
	    
	    data = uc.getUpdateDataFor(new Version("2.0.0"), "en", false, UpdateStyle.STYLE_MAJOR, new Version("1.3.0"));
	    assertEquals("Your Java Sucks.", data.getUpdateText());
	    
	    data = uc.getUpdateDataFor(new Version("2.0.0"), "en", false, UpdateStyle.STYLE_BETA, new Version("1.4.2"));
	    assertEquals("Your Java Doesn't Suck.", data.getUpdateText());
	    
	    data = uc.getUpdateDataFor(new Version("2.0.0"), "en", false, UpdateStyle.STYLE_BETA, new Version("1.5.0_02"));
	    assertEquals("Your Java Is Mysterious.", data.getUpdateText());
	    
	    data = uc.getUpdateDataFor(new Version("9.9.9"), "en", false, UpdateStyle.STYLE_BETA, new Version("1.5.0_02"));	    
	    assertNull(data);
    }
    
    public void testQuotesAroundUCommand() throws Exception {
        UpdateCollection uc = updateCollectionFactory.createUpdateCollection("<update id='42'>" +
            "<msg for='9.9.9' url='http://www.limewire.com/update' style='4' ucommand='\"name with spaces\" after quote'>" +
                "<lang id='en'>WTG Quotes.</lang>" +
            "</msg>" +
        "</update>");
        
        UpdateData data = uc.getUpdateDataFor(new Version("9.9.0"), "en", false, UpdateStyle.STYLE_BETA, null);       
        assertEquals("WTG Quotes.", data.getUpdateText());
        assertEquals("\"name with spaces\" after quote", data.getUpdateCommand());
        
    }
    
    
    private static void setOSName(String name) throws Exception {
        System.setProperty("os.name", name);
        PrivilegedAccessor.invokeMethod(OSUtils.class, "setOperatingSystems");
    }
    
    private static void setOsVersion(String version) {
        System.setProperty("os.version",version);
    }
            
}