package com.limegroup.gnutella.xml;

import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;

import com.google.inject.Injector;


/**
 * Tests the utility class for xml queries.  This performs such functions as
 * the all-important match method.
 */
public class LimeXMLUtilsTest extends LimeTestCase {

    private LimeXMLDocumentFactory limeXMLDocumentFactory;
    
    public LimeXMLUtilsTest(String name) {
        super(name);
    }
    
    @Override
    protected void setUp() throws Exception {
		Injector injector = LimeTestUtils.createInjector();
		limeXMLDocumentFactory = injector.getInstance(LimeXMLDocumentFactory.class);
    }
    
    public void testUnencodeXML() throws Exception {
        assertEquals("this is a test", LimeXMLUtils.unencodeXML("this is a test"));
        assertEquals("this is a &test;", LimeXMLUtils.unencodeXML("this is a &test;"));
        assertEquals("this is a &test", LimeXMLUtils.unencodeXML("this is a &amp;test"));
        assertEquals("a <<>>test", LimeXMLUtils.unencodeXML("a <&lt;&gt;>test"));
        assertEquals("'a\" 't\"e> <<st", LimeXMLUtils.unencodeXML("'a\" &apos;t&quot;e&gt; &lt;<st"));
        String test = "no change";
        assertSame(test, LimeXMLUtils.unencodeXML(test));
    }
    
    public void testScanForBadCharacters() throws Exception {
        assertEquals("this  is a test", LimeXMLUtils.scanForBadCharacters("this\u0002 is\u0003a\u0004test"));
        String test = "no change";
        assertSame(test, LimeXMLUtils.scanForBadCharacters(test));
    }
    
    public void testEncodeXML() throws Exception {
        assertEquals("a&amp; b&gt;&gt; &lt;&apos;&quot;end ", LimeXMLUtils.encodeXML("a&\u0004b>> <'\"end\u0004"));
        String test = "no change";
        assertSame(test, LimeXMLUtils.encodeXML(test));
    }

    public void testParseAction() throws Exception {
        String queryString = 
            "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=" +
            "\"http://www.limewire.com/schemas/audio.xsd\"><audio title=" +
            "\"test\" artist=\"test\" album=\"test\" track=\"test\" type=" +
            "\"song\" year=\"test\" seconds=\"test\" language=\"test\" " +
            "SHA1=\"test\" bitrate=\"test\" price=\"test\" link=\"test\" " +
            "comments=\"test\" action=\"test\"" +
            "></audio></audios>";
        LimeXMLDocument doc = limeXMLDocumentFactory.createLimeXMLDocument(queryString);
        assertTrue(doc.getAction().equals("test"));
        
        queryString = 
            "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=" +
            "\"http://www.limewire.com/schemas/audio.xsd\"><audio title=" +
            "\"test\" artist=\"test\" album=\"test\" track=\"test\" type=" +
            "\"song\" year=\"test\" seconds=\"test\" language=\"test\" " +
            "SHA1=\"test\" bitrate=\"test\" price=\"test\" link=\"test\" " +
            "comments=\"test\" action=\"test\" actionDetailRequested=\"false\"" +
            "></audio></audios>";
        
        doc = limeXMLDocumentFactory.createLimeXMLDocument(queryString);
        assertTrue(doc.getAction().equals("test"));
        
        queryString = 
            "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=" +
            "\"http://www.limewire.com/schemas/audio.xsd\"><audio title=" +
            "\"test\" artist=\"test\" album=\"test\" track=\"test\" type=" +
            "\"song\" year=\"test\" seconds=\"test\" language=\"test\" " +
            "SHA1=\"test\" bitrate=\"test\" price=\"test\" link=\"test\" " +
            "comments=\"test\" action=\"test\" addActionDetail=\"True\"" +
            "></audio></audios>";
        
        doc = limeXMLDocumentFactory.createLimeXMLDocument(queryString);
        assertTrue(doc.getAction().equals("test"));
    }
    
    /**
     * Tests the method for matching two XML documents -- used for incoming
     * searches.
     * 
     * @throws Exception if any error occurs
     */
    public void testDocsThatShouldMatch() throws Exception {
        
        LimeXMLDocument doc = 
            limeXMLDocumentFactory.createLimeXMLDocument(buildXMLString("director=\"francis loopola\""));
        // Make sure that null pointers are thrown properly.
        try {
            LimeXMLUtils.match(doc, null, false);
            fail("should have thrown null pointer");
        } catch(NullPointerException e) {
        }
        
        // Make sure the dummy case works.
        assertTrue("docs should match", LimeXMLUtils.match(doc, doc, false));
        
        // Make sure that searches with many criteria match files on disk with
        // only one criteria, as long as that criteria matches.
        String queryString = 
            "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=" +
            "\"http://www.limewire.com/schemas/audio.xsd\"><audio title=" +
            "\"test\" artist=\"test\" album=\"test\" track=\"test\" type=" +
            "\"song\" year=\"test\" seconds=\"test\" language=\"test\" " +
            "SHA1=\"test\" bitrate=\"test\" price=\"test\" link=\"test\" " +
            "comments=\"test\" action=\"test\"" +
            "></audio></audios>";
        LimeXMLDocument queryDoc = limeXMLDocumentFactory.createLimeXMLDocument(queryString);
        String onDiskString = 
            "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=" +
            "\"http://www.limewire.com/schemas/audio.xsd\"><audio " +
            "title=\"test\"" +
            "></audio></audios>";
        LimeXMLDocument onDiskDoc = limeXMLDocumentFactory.createLimeXMLDocument(onDiskString);
        assertTrue("docs should match", LimeXMLUtils.match(onDiskDoc,queryDoc, false));
        
        
        // Make sure that searches with a single argument match results with
        // lots of other fields.
        queryString = 
            "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=" +
            "\"http://www.limewire.com/schemas/audio.xsd\"><audio title=" +
            "\"test\"></audio></audios>";
        queryDoc = limeXMLDocumentFactory.createLimeXMLDocument(queryString);
        onDiskString = 
            "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=" +
            "\"http://www.limewire.com/schemas/audio.xsd\"><audio title=" +
            "\"test\" artist=\"test\" album=\"test\" track=\"test\" type=" +
            "\"song\" year=\"test\" seconds=\"test\" language=\"test\" " +
            "SHA1=\"test\" bitrate=\"test\" price=\"test\" link=\"test\" " +
            "comments=\"test\" action=\"test\"></audio></audios>";
        onDiskDoc = limeXMLDocumentFactory.createLimeXMLDocument(onDiskString);
        assertTrue("docs should match", LimeXMLUtils.match(onDiskDoc,queryDoc, false));
        
        queryString = 
            "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=" +
            "\"http://www.limewire.com/schemas/audio.xsd\"><audio title=" +
            "\"test\" artist=\"test\"></audio></audios>";
        queryDoc = limeXMLDocumentFactory.createLimeXMLDocument(queryString);
        assertTrue("docs should match", LimeXMLUtils.match(onDiskDoc,queryDoc, false));
        
        queryString = 
            "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=" +
            "\"http://www.limewire.com/schemas/audio.xsd\"><audio title=" +
            "\"test\" artist=\"test\" album=\"test\"></audio></audios>";
        queryDoc = limeXMLDocumentFactory.createLimeXMLDocument(queryString);
        assertTrue("docs should match", LimeXMLUtils.match(onDiskDoc,queryDoc, false));
        
        queryString = 
            "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=" +
            "\"http://www.limewire.com/schemas/audio.xsd\"><audio title=" +
            "\"test\" artist=\"test\" album=\"test\" track=\"test\"" +
            "></audio></audios>";
        queryDoc = limeXMLDocumentFactory.createLimeXMLDocument(queryString);
        assertTrue("docs should match", LimeXMLUtils.match(onDiskDoc,queryDoc, false));
        
        queryString = 
            "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=" +
            "\"http://www.limewire.com/schemas/audio.xsd\"><audio title=" +
            "\"test\" artist=\"test\" album=\"test\" track=\"test\"" +
            " type=\"song\""+
            "></audio></audios>";
        queryDoc = limeXMLDocumentFactory.createLimeXMLDocument(queryString);
        assertTrue("docs should match", LimeXMLUtils.match(onDiskDoc,queryDoc, false));
        
        queryString = 
            "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=" +
            "\"http://www.limewire.com/schemas/audio.xsd\"><audio title=" +
            "\"test\" artist=\"test\" album=\"test\" track=\"test\"" +
            " type=\"song\" year=\"test\""+
            "></audio></audios>";
        queryDoc = limeXMLDocumentFactory.createLimeXMLDocument(queryString);
        assertTrue("docs should match", LimeXMLUtils.match(onDiskDoc,queryDoc, false));
        
        
        queryString = 
            "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=" +
            "\"http://www.limewire.com/schemas/audio.xsd\"><audio title=" +
            "\"test\" artist=\"test\" album=\"test\" track=\"test\" type=" +
            "\"song\" year=\"test\" seconds=\"test\""+
            "></audio></audios>";
        queryDoc = limeXMLDocumentFactory.createLimeXMLDocument(queryString);
        assertTrue("docs should match", LimeXMLUtils.match(onDiskDoc,queryDoc, false));
        
        
        queryString = 
            "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=" +
            "\"http://www.limewire.com/schemas/audio.xsd\"><audio title=" +
            "\"test\" artist=\"test\" album=\"test\" track=\"test\" type=" +
            "\"song\" year=\"test\" seconds=\"test\" language=\"test\"" +
            "></audio></audios>";
        queryDoc = limeXMLDocumentFactory.createLimeXMLDocument(queryString);
        assertTrue("docs should match", LimeXMLUtils.match(onDiskDoc,queryDoc, false));
        
        queryString = 
            "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=" +
            "\"http://www.limewire.com/schemas/audio.xsd\"><audio title=" +
            "\"test\" artist=\"test\" album=\"test\" track=\"test\" type=" +
            "\"song\" year=\"test\" seconds=\"test\" language=\"test\" " +
            "SHA1=\"test\"" +
            "></audio></audios>";
        queryDoc = limeXMLDocumentFactory.createLimeXMLDocument(queryString);
        assertTrue("docs should match", LimeXMLUtils.match(onDiskDoc,queryDoc, false));
        
        queryString = 
            "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=" +
            "\"http://www.limewire.com/schemas/audio.xsd\"><audio title=" +
            "\"test\" artist=\"test\" album=\"test\" track=\"test\" type=" +
            "\"song\" year=\"test\" seconds=\"test\" language=\"test\" " +
            "SHA1=\"test\" bitrate=\"test\"" +
            "></audio></audios>";
        queryDoc = limeXMLDocumentFactory.createLimeXMLDocument(queryString);
        assertTrue("docs should match", LimeXMLUtils.match(onDiskDoc,queryDoc, false));
        queryString = 
            "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=" +
            "\"http://www.limewire.com/schemas/audio.xsd\"><audio title=" +
            "\"test\" artist=\"test\" album=\"test\" track=\"test\" type=" +
            "\"song\" year=\"test\" seconds=\"test\" language=\"test\" " +
            "SHA1=\"test\" bitrate=\"test\" price=\"test\""+
            "></audio></audios>";
        queryDoc = limeXMLDocumentFactory.createLimeXMLDocument(queryString);
        assertTrue("docs should match", LimeXMLUtils.match(onDiskDoc,queryDoc, false));
        
        queryString = 
            "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=" +
            "\"http://www.limewire.com/schemas/audio.xsd\"><audio title=" +
            "\"test\" artist=\"test\" album=\"test\" track=\"test\" type=" +
            "\"song\" year=\"test\" seconds=\"test\" language=\"test\" " +
            "SHA1=\"test\" bitrate=\"test\" price=\"test\" link=\"test\"" +
            "></audio></audios>";
        queryDoc = limeXMLDocumentFactory.createLimeXMLDocument(queryString);
        assertTrue("docs should match", LimeXMLUtils.match(onDiskDoc,queryDoc, false));
        
        queryString = 
            "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=" +
            "\"http://www.limewire.com/schemas/audio.xsd\"><audio title=" +
            "\"test\" artist=\"test\" album=\"test\" track=\"test\" type=" +
            "\"song\" year=\"test\" seconds=\"test\" language=\"test\" " +
            "SHA1=\"test\" bitrate=\"test\" price=\"test\" link=\"test\" " +
            "comments=\"test\"" +
            "></audio></audios>";
        queryString = 
            "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=" +
            "\"http://www.limewire.com/schemas/audio.xsd\"><audio title=" +
            "\"test\" artist=\"test\" album=\"test\" track=\"test\" type=" +
            "\"song\" year=\"test\" seconds=\"test\" language=\"test\" " +
            "SHA1=\"test\" bitrate=\"test\" price=\"test\" link=\"test\" " +
            "comments=\"test\" action=\"test\"" +
            "></audio></audios>";
        queryDoc = limeXMLDocumentFactory.createLimeXMLDocument(queryString);
        assertTrue("docs should match", LimeXMLUtils.match(onDiskDoc,queryDoc, false));
    }
    
    /**
     * Test to make sure that docs that should not match do not, in fact, match.
     * 
     * @throws Exception if an error occurs
     */
    public void testDocsThatShouldNotMatch() throws Exception {
        String onDiskString = 
            "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=" +
            "\"http://www.limewire.com/schemas/audio.xsd\"><audio title=" +
            "\"test\" artist=\"test\" album=\"test\" track=\"test\" type=" +
            "\"song\" year=\"test\" seconds=\"test\" language=\"test\" " +
            "SHA1=\"test\" bitrate=\"test\" price=\"test\" link=\"test\" " +
            "comments=\"test\" action=\"test\"></audio></audios>";
        LimeXMLDocument onDiskDoc = limeXMLDocumentFactory.createLimeXMLDocument(onDiskString);    
        
        String queryString = 
            "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=" +
            "\"http://www.limewire.com/schemas/audio.xsd\"><audio title=" +
            "\"nottest\"" +
            "></audio></audios>";
        LimeXMLDocument queryDoc = limeXMLDocumentFactory.createLimeXMLDocument(queryString);  
        assertFalse("docs should not match", 
            LimeXMLUtils.match(onDiskDoc,queryDoc, false));
        
        queryString = 
            "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=" +
            "\"http://www.limewire.com/schemas/audio.xsd\"><audio title=" +
            "\"test\" artist=\"nottest\"" +
            "></audio></audios>";
        queryDoc = limeXMLDocumentFactory.createLimeXMLDocument(queryString);  
        assertFalse("docs should not match", 
            LimeXMLUtils.match(onDiskDoc,queryDoc, false));
        
        queryString = 
            "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=" +
            "\"http://www.limewire.com/schemas/audio.xsd\"><audio title=" +
            "\"test\" artist=\"nottest\" album=\"test\" track=\"test\" type=" +
            "\"song\" year=\"test\" seconds=\"test\" language=\"test\"" +
            " ></audio></audios>";
        queryDoc = limeXMLDocumentFactory.createLimeXMLDocument(queryString);  
        assertFalse("docs should not match", 
            LimeXMLUtils.match(onDiskDoc,queryDoc, false));
        
        queryString = 
            "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=" +
            "\"http://www.limewire.com/schemas/audio.xsd\"><audio title=" +
            "\"test\" artist=\"nottest\" album=\"test\" track=\"test\" type=" +
            "\"song\" year=\"test\" seconds=\"nottest\" language=\"test\"" +
            " ></audio></audios>";
        queryDoc = limeXMLDocumentFactory.createLimeXMLDocument(queryString);  
        assertFalse("docs should not match", 
            LimeXMLUtils.match(onDiskDoc,queryDoc, false));
    }
    
    
    // build xml string for video
    private String buildXMLString(String keyname) {
        return "<?xml version=\"1.0\"?><videos xsi:noNamespaceSchemaLocation="
            + "\"http://www.limewire.com/schemas/video.xsd\"><video " 
            + keyname 
            + "></video></videos>";
    }
}
