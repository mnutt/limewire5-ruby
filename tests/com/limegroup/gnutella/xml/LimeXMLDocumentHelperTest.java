package com.limegroup.gnutella.xml;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.xml.sax.SAXException;

import com.google.inject.Injector;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.util.LimeTestCase;

public class LimeXMLDocumentHelperTest extends LimeTestCase {

    LimeXMLDocumentHelper limeXMLDocumentHelper;
    
    public LimeXMLDocumentHelperTest(String name) {
        super(name);  
    }

    @Override
    protected void setUp() throws Exception {
        Injector injector = LimeTestUtils.createInjector();
        limeXMLDocumentHelper = injector.getInstance(LimeXMLDocumentHelper.class);
    }
    
    public void testGetDocuments() throws IOException, SAXException {
        
        /*
         * Capitalitzed XML Tags
         */
        String xmlCapitalTag = "<?xml version=\"1.0\"?>"+
        "<AUDIOS xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\">"+
        "<audio genre=\"rock\" identifier=\"def1.txt\" index=\"0\" action=\"test\" addActionDetail = \"detail\" license = \"DRM\" licensetype = \"licensed: DRM\" bitrate=\"190\"/></AUDIOS>";
        List listXMLDocuments;
        listXMLDocuments = limeXMLDocumentHelper.getDocuments(xmlCapitalTag, 1);
        
        LimeXMLDocument[] newLimeDocArray = (LimeXMLDocument[]) listXMLDocuments.get(0);
        LimeXMLDocument newLimeDoc = newLimeDocArray[0];
        String xml = newLimeDoc.toString();
        
        //parse LimeXMLDocument for the fields       
        XMLParsingUtils.ParseResult r = XMLParsingUtils.parse(xml, 1);
        Map<String, String> result = r.get(0);
        
        //check values
        //canonicalKeyPrefix should be lowercase
        assertEquals("audios__audio__", r.canonicalKeyPrefix);
        assertEquals("audio", r.type);
        
        assertEquals(result.toString(), "rock", result.get("audios__audio__genre__"));
        assertEquals(result.toString(), "DRM", result.get("audios__audio__license__"));
        assertEquals(result.toString(), "licensed: DRM", result.get("audios__audio__licensetype__"));
        assertEquals(result.toString(), "190", result.get("audios__audio__bitrate__"));
        
        
        /*
         * with capitalized attributes
         */
        xmlCapitalTag = "<?xml version=\"1.0\"?>"+
        "<AUDIOS xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\">"+
        "<audio GENRE=\"rock\" identifier=\"def1.txt\" index=\"0\" action=\"test\" addActionDetail = \"detail\" license = \"DRM\" licensetype = \"licensed: DRM\" bitrate=\"190\"/></AUDIOS>";
        listXMLDocuments = limeXMLDocumentHelper.getDocuments(xmlCapitalTag, 1);
        
        newLimeDocArray = (LimeXMLDocument[]) listXMLDocuments.get(0);
        newLimeDoc = newLimeDocArray[0];
        newLimeDoc.toString();
        
        //parse LimeXMLDocument for the fields       
        r = XMLParsingUtils.parse(xml, 1);
        result = r.get(0);
        
        //check values
        //canonicalKeyPrefix should be lowercase
        assertEquals("audios__audio__", r.canonicalKeyPrefix);
        assertEquals("audio", r.type);
        
        assertEquals(result.toString(), "rock", result.get("audios__audio__genre__"));
        assertEquals(result.toString(), "DRM", result.get("audios__audio__license__"));
        assertEquals(result.toString(), "licensed: DRM", result.get("audios__audio__licensetype__"));
        assertEquals(result.toString(), "190", result.get("audios__audio__bitrate__"));
        
        /*
         * with capitalized attributes and SHA1
         */
        xmlCapitalTag = "<?xml version=\"1.0\"?>"+
        "<AUDIOS xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\">"+
        "<audio GENRE=\"rock\" identifier=\"def1.txt\" index=\"0\" action=\"test\" addActionDetail = \"detail\" license = \"DRM\" SHA1 = \"SHA1Test\" licensetype = \"licensed: DRM\" bitrate=\"190\"/></AUDIOS>";
        listXMLDocuments = limeXMLDocumentHelper.getDocuments(xmlCapitalTag, 1);
        
        newLimeDocArray = (LimeXMLDocument[]) listXMLDocuments.get(0);
        newLimeDoc = newLimeDocArray[0];
        newLimeDoc.toString();
        
        //parse LimeXMLDocument for the fields       
        r = XMLParsingUtils.parse(xml, 1);
        result = r.get(0);
        
        //check values
        //canonicalKeyPrefix should be lowercase
        assertEquals("audios__audio__", r.canonicalKeyPrefix);
        assertEquals("audio", r.type);
        
        assertEquals(result.toString(), "rock", result.get("audios__audio__genre__"));
        assertEquals(result.toString(), "DRM", result.get("audios__audio__license__"));
        assertEquals(result.toString(), "licensed: DRM", result.get("audios__audio__licensetype__"));
        assertEquals(result.toString(), "190", result.get("audios__audio__bitrate__"));

    }

}
