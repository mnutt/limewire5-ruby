package com.limegroup.gnutella.licenses;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import junit.framework.Test;

import org.limewire.util.NameValue;
import org.limewire.util.TestUtils;

import com.google.inject.Injector;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.metadata.MetaDataFactory;
import com.limegroup.gnutella.metadata.audio.AudioMetaData;
import com.limegroup.gnutella.util.LimeTestCase;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLDocumentFactory;

public final class LicenseReadingTest extends LimeTestCase {

	private LimeXMLDocumentFactory limeXMLDocumentFactory;
	private MetaDataFactory metaDataFactory;

    public LicenseReadingTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(LicenseReadingTest.class);
	}

	/**
	 * Runs this test individually.
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	@Override
	protected void setUp() throws Exception {
		Injector injector = LimeTestUtils.createInjector();
		limeXMLDocumentFactory = injector.getInstance(LimeXMLDocumentFactory.class);
		metaDataFactory = injector.getInstance(MetaDataFactory.class);
	}
	
	public void testReadID3AndXML() throws Exception {
	    File f = TestUtils.getResourceFile("com/limegroup/gnutella/licenses/cc1.mp3");
	    assertTrue(f.exists());
	    
	    AudioMetaData amd= (AudioMetaData) metaDataFactory.parse(f);
	    assertNotNull(amd);
	    
	    boolean foundLicense = false;
	    List<NameValue<String>> nvList = amd.toNameValueList();
	    for(Iterator i = nvList.iterator(); i.hasNext(); ) {
	        NameValue nv = (NameValue)i.next();
	        assertFalse(AudioMetaData.isNonLimeAudioField(nv.getName()));
	        foundLicense |= nv.getName().equals("audios__audio__license__");
	    }
	    assertTrue(foundLicense);
	    assertEquals("2004 David Byrne Licensed to the " +
	                 "public under http://creativecommons.org/licenses/sampling+/1.0/ " +
	                 "verify at http://ccmixter.org/file/Wired/61", 
	                 amd.getLicense());
        
	    
	    LimeXMLDocument doc = limeXMLDocumentFactory.createLimeXMLDocument(nvList, amd.getSchemaURI());
	    assertTrue(doc.isLicenseAvailable());
	    assertEquals(amd.getLicense(), doc.getLicenseString());
	    assertEquals("<?xml version=\"1.0\"?>" +
"<audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\">" +
"<audio title=\"My Fair Lady\" artist=\"David Byrne\" album=" +
"\"The Wired CD: Rip. Sample. Mash. Share.\" genre=\"Other\" licensetype=" +
"\"creativecommons.org/licenses/\" track=\"2\" year=\"2004\" seconds=\"208\"" +
" bitrate=\"138\" license=\"2004 David Byrne Licensed to the public under " +
"http://creativecommons.org/licenses/sampling+/1.0/ verify at " +
"http://ccmixter.org/file/Wired/61\"/></audios>", doc.getXMLString());
	    
	    List<String> indivList = new LinkedList<String>();
	    indivList.add("creativecommons.org/licenses/");
	    assertEquals(indivList, doc.getKeyWordsIndivisible());
	    
	    boolean licenseTypeFound = false;
	    for(Iterator i = doc.getNameValueSet().iterator(); i.hasNext(); ) {
	        Map.Entry next = (Map.Entry)i.next();
	        String name = (String)next.getKey();
	        if(name.equals("audios__audio__licensetype__")) {
	            licenseTypeFound = true;
	            assertEquals("creativecommons.org/licenses/", next.getValue());
	        }
        }
        assertTrue(licenseTypeFound);
        
        License l = doc.getLicense();
        assertNotNull(l);
        assertEquals(CCLicense.class, l.getClass());
        assertFalse(l.isVerified());
        // don't validate -- don't wanna hit the web.
        assertEquals("http://creativecommons.org/licenses/sampling+/1.0/",
                     l.getLicenseDeed(null).toString());
        assertEquals("http://ccmixter.org/file/wired/61", l.getLicenseURI().toString());
    }
    
    public void testReadOGG() throws Exception {
 	    File f = TestUtils.getResourceFile("com/limegroup/gnutella/licenses/ccverifytest0.ogg");
	    assertTrue(f.exists());
	    
	    AudioMetaData amd= (AudioMetaData) metaDataFactory.parse(f);
        assertNotNull(amd);
        
	    boolean foundLicense = false;
	    List nvList = amd.toNameValueList();
	    for(Iterator i = nvList.iterator(); i.hasNext(); ) {
	        NameValue nv = (NameValue)i.next();
	        assertFalse(AudioMetaData.isNonLimeAudioField(nv.getName()));
	        foundLicense |= nv.getName().equals("audios__audio__license__");
	    }
	    assertTrue(foundLicense);
	    assertEquals("2002 BM Relocation Program. Licensed to the public under " +
	                 "http://creativecommons.org/licenses/by-sa/1.0/ verify at " +
	                 "http://creativecommons.org/technology/verifytest/", 
	                 amd.getLicense());


 	    f = TestUtils.getResourceFile("com/limegroup/gnutella/licenses/ccverifytest1.ogg");
	    assertTrue(f.exists());
	    
	    amd = (AudioMetaData) metaDataFactory.parse(f);
	    assertNotNull(amd);
	    
	    foundLicense = false;
	    nvList = amd.toNameValueList();
	    for(Iterator i = nvList.iterator(); i.hasNext(); ) {
	        NameValue nv = (NameValue)i.next();
	        assertFalse(AudioMetaData.isNonLimeAudioField(nv.getName()));
	        foundLicense |= nv.getName().equals("audios__audio__license__");
	    }
	    assertTrue(foundLicense);
	    assertEquals("2003 Okapi Guitars. Licensed to the public under " +
	                 "http://creativecommons.org/licenses/by-nc-sa/1.0/ verify at " +
	                 "http://creativecommons.org/technology/verifytest/", 
	                 amd.getLicense());
    }
    
    public void testReadWeed() throws Exception {
	    File f = TestUtils.getResourceFile("com/limegroup/gnutella/licenses/weed-PUSA-LoveEverybody.wma");
	    assertTrue(f.exists());
	    
	    AudioMetaData amd= (AudioMetaData) metaDataFactory.parse(f);
        assertNotNull(amd);
	    
	    LimeXMLDocument doc = limeXMLDocumentFactory.createLimeXMLDocument(amd.toNameValueList(), amd.getSchemaURI());
	    assertTrue(doc.isLicenseAvailable());
	    assertEquals(amd.getLicenseType(), doc.getLicenseString());
	    assertEquals("<?xml version=\"1.0\"?>" +
"<audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\">" +
"<audio title=\"Love Everybody\" artist=\"The Presidents of the United States of America\" album=" +
"\"Love Everybody\" genre=\"Rock\" licensetype=" +
"\"http://www.shmedlic.com/license/3play.aspx cid: 214324 vid: 0000000000001370651\" " +
"track=\"1\" year=\"2004\" seconds=\"158\" bitrate=\"192\" license=\"2004 PUSA Inc.\"/></audios>",
                    doc.getXMLString());
	    
	    List<String> indivList = new LinkedList<String>();
	    indivList.add("http://www.shmedlic.com/license/3play.aspx");
	    assertEquals(indivList, doc.getKeyWordsIndivisible());
	    
	    boolean licenseTypeFound = false;
	    for(Iterator i = doc.getNameValueSet().iterator(); i.hasNext(); ) {
	        Map.Entry next = (Map.Entry)i.next();
	        String name = (String)next.getKey();
	        if(name.equals("audios__audio__licensetype__")) {
	            licenseTypeFound = true;
	            assertEquals("http://www.shmedlic.com/license/3play.aspx cid: 214324 vid: 0000000000001370651", next.getValue());
	        }
        }
        assertTrue(licenseTypeFound);
        
        License l = doc.getLicense();
        assertNotNull(l);
        assertEquals(WeedLicense.class, l.getClass());
        assertFalse(l.isVerified());
        // don't validate -- don't wanna hit the web.
        assertEquals("http://weedshare.com/company/policies/summary_usage_rights.aspx",
                     l.getLicenseDeed(null).toString());
        assertEquals("http://www.weedshare.com/license/verify_usage_rights.aspx?" +
                     "versionid=0000000000001370651&contentid=214324", l.getLicenseURI().toString());
    }
}
