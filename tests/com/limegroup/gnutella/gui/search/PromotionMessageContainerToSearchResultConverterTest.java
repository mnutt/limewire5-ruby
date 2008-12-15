package com.limegroup.gnutella.gui.search;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Test;

import org.limewire.promotion.containers.PromotionMessageContainer;

import com.google.inject.Injector;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.gui.GUIBaseTestCase;
import com.limegroup.gnutella.gui.search.PromotionSearchResultFactory.Attr;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLDocumentFactory;

/**
 * Make sure that the search results created properly can be used in the client,
 * in regards to normal properties and XML properties.
 */
public class PromotionMessageContainerToSearchResultConverterTest extends GUIBaseTestCase {

    public PromotionMessageContainerToSearchResultConverterTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(PromotionMessageContainerToSearchResultConverterTest.class);
    }
    
    // We'll reuse these
    private PromotionMessageContainer con;
    private PromotionMessageContainerToSearchResultConverter converter;
    
    // The properties
    private final String name = "the name";
    private final String url = "http://the.url.com";
    private final String vendor = "the vendor";
    private final int size = 1234;
    private final long creationTime = 123123L;
    private final String query = "the keyword";
    private final String artist = "Jesse Rubenfeld";
    private final String album = "uhh.... ???";
    private final String genre = "rap";  // gansta, that is
    private final String title = "where's my 40?";
    private final String xmlSchemaAudio = "http://www.limewire.com/schemas/audio.xsd";
    private final String xmlSchemaVideo = "http://www.limewire.com/schemas/video.xsd";
    private final String fileType = "mp3";
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        con = new PromotionMessageContainer();
        Injector injector = LimeTestUtils.createInjector();
        LimeXMLDocumentFactory xmlDocFactory = injector.getInstance(LimeXMLDocumentFactory.class);
        ApplicationServices applicationServices = injector.getInstance(ApplicationServices.class);
        converter = new PromotionMessageContainerToSearchResultConverter(xmlDocFactory, applicationServices);
    }
    
    // --------------------------------------------------------------------------------------
    // XML Properties
    // --------------------------------------------------------------------------------------

    /** Tests {@link PromotionSearchResultFactory#Attr#NAME */
    public void testName() {
        con.setDescription(name);
        PromotionSearchResult result = (PromotionSearchResult)converter.convert(con, query);
        assertEquals("name", name, result.getFilenameNoExtension());
    }
    
    /** Tests {@link PromotionSearchResultFactory#Attr#NAME */
    public void testNameByProperties() {
        Map<String,String> props = new HashMap<String,String>();
        props.put(PromotionSearchResultFactory.Attr.NAME.getValue(), name);
        con.setProperties(props);
        PromotionSearchResult result = (PromotionSearchResult)converter.convert(con, query);
        assertEquals("name", name, result.getFilenameNoExtension());
    } 
    
    /** Tests {@link PromotionSearchResultFactory#Attr#URL */
    public void testUrl() {
        con.setURL(url);
        PromotionSearchResult result = (PromotionSearchResult)converter.convert(con, query);
        assertTrue(result.getURL() + " should contain '?url=" + url, result.getURL().contains("?url=" + url));
    }   
    
    /** Tests {@link PromotionSearchResultFactory#Attr#URL */
    public void testLinkUrl() {
        con.setURL(url);
        PromotionSearchResult result = (PromotionSearchResult)converter.convert(con, query);
        assertTrue(result.getURL() + " should contain '?url=" + url, result.getLinkUrl().contains("?url=" + url));
    }
    
    /** Tests {@link PromotionSearchResultFactory#Attr#SIZE */
    public void testSize() {
        Map<String,String> props = new HashMap<String,String>();
        props.put(Attr.SIZE.getValue(), String.valueOf(size));
        con.setProperties(props);
        PromotionSearchResult result = (PromotionSearchResult)converter.convert(con, query);
        assertEquals("size", size, result.getSize());
    }   
    
    /** Tests {@link PromotionSearchResultFactory#Attr#CREATION_TIME */
    public void testCreationTime() {
        Map<String,String> props = new HashMap<String,String>();
        props.put(Attr.CREATION_TIME.getValue(), String.valueOf(creationTime));
        con.setProperties(props);
        PromotionSearchResult result = (PromotionSearchResult)converter.convert(con, query);
        assertEquals("creation time", creationTime, result.getCreationTime());
    }    
    
    /** Tests {@link PromotionSearchResultFactory#Attr#VENDOR */
    public void testVendor() {
        Map<String,String> props = new HashMap<String,String>();
        props.put(Attr.VENDOR.getValue(), vendor);
        con.setProperties(props);
        PromotionSearchResult result = (PromotionSearchResult)converter.convert(con, query);
        assertEquals("vendor", vendor, result.getVendor());
    }
    
    /** Tests {@link PromotionSearchResultFactory#Attr#XML_SCHEMA */
    public void testXmlSchemaAudio() {
        con.setDescription(name);
        Map<String,String> props = new HashMap<String,String>();
        props.put("artist", artist);
        con.setProperties(props);
        PromotionSearchResult result = (PromotionSearchResult)converter.convert(con, query);
        LimeXMLDocument doc = result.getXMLDocument(); 
        assertTrue("should have an audio schema", doc.getXMLString().contains(xmlSchemaAudio));
    }
    
    /** Tests {@link PromotionSearchResultFactory#Attr#XML_SCHEMA */
    public void testXmlSchemaAudioWhileSpecifying() {
        con.setDescription(name);
        Map<String,String> props = new HashMap<String,String>();
        props.put(Attr.XML_SCHEMA.getValue(), "audio");
        props.put("artist", artist);
        con.setProperties(props);
        PromotionSearchResult result = (PromotionSearchResult)converter.convert(con, query);
        LimeXMLDocument doc = result.getXMLDocument();
        assertTrue("should have an audio schema", doc.getXMLString().contains(xmlSchemaAudio));
    }
    
    /** Tests {@link PromotionSearchResultFactory#Attr#XML_SCHEMA */
    public void testXmlSchemaVideoWhileSpecifying() {
        con.setDescription(name);
        Map<String,String> props = new HashMap<String,String>();
        props.put(Attr.XML_SCHEMA.getValue(), "video");
        props.put("director", artist);
        con.setProperties(props);
        PromotionSearchResult result = (PromotionSearchResult)converter.convert(con, query);
        LimeXMLDocument doc = result.getXMLDocument();
        assertTrue("should have a video schema", doc.getXMLString().contains(xmlSchemaVideo));
    }
    
    /** Tests {@link PromotionSearchResultFactory#Attr#FILE_TYPE */
    public void testFileType() {
        Map<String,String> props = new HashMap<String,String>();
        props.put(Attr.FILE_TYPE.getValue(), fileType);
        con.setProperties(props);
        PromotionSearchResult result = (PromotionSearchResult)converter.convert(con, query);
        assertEquals("file type", fileType, result.getExtension());
    }     
    
    
    // --------------------------------------------------------------------------------------
    // Add-hoc Properties
    // --------------------------------------------------------------------------------------    
    
    public void testArtist() {
        Map<String,String> props = new HashMap<String,String>();
        props.put("artist", artist);
        con.setProperties(props);
        PromotionSearchResult result = (PromotionSearchResult)converter.convert(con, query);
        LimeXMLDocument doc = result.getXMLDocument();
        assertEquals("xmlDoc", "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio artist=\"Jesse Rubenfeld\"/></audios>", doc.getXMLString());
        assertEquals("artist", artist, doc.getValue("audios__audio__artist__"));
    } 
    
    public void testAlbum() {
        Map<String,String> props = new HashMap<String,String>();
        props.put("album", album);
        con.setProperties(props);
        PromotionSearchResult result = (PromotionSearchResult)converter.convert(con, query);
        LimeXMLDocument doc = result.getXMLDocument();
        assertEquals("album", album, doc.getValue("audios__audio__album__"));
    } 
    
    public void testGenre() {
        Map<String,String> props = new HashMap<String,String>();
        props.put("genre", genre);
        con.setProperties(props);
        PromotionSearchResult result = (PromotionSearchResult)converter.convert(con, query);
        LimeXMLDocument doc = result.getXMLDocument();
        assertEquals("genre", genre, doc.getValue("audios__audio__genre__"));
    } 
    
    public void testTitle() {
        Map<String,String> props = new HashMap<String,String>();
        props.put("title", title);
        con.setProperties(props);
        PromotionSearchResult result = (PromotionSearchResult)converter.convert(con, query);
        LimeXMLDocument doc = result.getXMLDocument();
        assertEquals("title", title, doc.getValue("audios__audio__title__"));
    } 

}
