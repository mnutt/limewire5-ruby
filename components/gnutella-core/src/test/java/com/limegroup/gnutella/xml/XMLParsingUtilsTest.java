package com.limegroup.gnutella.xml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.Test;

import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.util.NameValue;
import org.xml.sax.SAXException;

import com.google.inject.Injector;

/**
 * Tests for the XMLParsingUtils class
 */
@SuppressWarnings("unchecked")
public class XMLParsingUtilsTest extends LimeTestCase {
    
    private static final String AUDIO_SCHEMA = "http://www.limewire.com/schemas/audio.xsd";
    private LimeXMLDocumentFactory limeXMLDocumentFactory;
    
    public XMLParsingUtilsTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(XMLParsingUtilsTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        Injector injector = LimeTestUtils.createInjector();
		limeXMLDocumentFactory = injector.getInstance(LimeXMLDocumentFactory.class);
    }
    
    public void testSplit() throws Exception {
        String xml1 = "<?xml version='1.0'><text>one</text>";
        String xml2 = "<?xml version='1.0'><text>two</text>";
        String xml3 = "<?xml version='1.0'><text>three</text>";
        Iterator i = XMLParsingUtils.split(xml1+xml2+xml3).iterator();
        assertEquals(xml1, i.next());
        assertEquals(xml2, i.next());
        assertEquals(xml3, i.next());
        assertFalse(i.hasNext());
        
        assertEquals(3,XMLParsingUtils.split("<?xml<?xml<?xml").size());
        assertEquals(1,XMLParsingUtils.split("<xml?xml<?xml<?xm").size());
        assertTrue(XMLParsingUtils.split("<xml ?><<><?xm>l").isEmpty());
    }
    
    public void testParseValid() throws Exception {
        String xml = "<?xml version=\"1.0\"?>"+
        "<audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\">"+
        "<audio genre=\"Rock\" identifier=\"def1.txt\" bitrate=\"190\"/>"+
        "<audio genre=\"Classical\" identifier=\"def2.txt\" bitrate=\"2192\"/>"+
        "<audio genre=\"Blues\" identifier=\"def.txt\" bitrate=\"192\"/></audios>";
        XMLParsingUtils.ParseResult r = XMLParsingUtils.parse(xml,3);
        assertEquals("http://www.limewire.com/schemas/audio.xsd", r.schemaURI);
        assertEquals("audio", r.type);
        assertEquals("audios__audio__", r.canonicalKeyPrefix);
        List list = new ArrayList();
        HashMap map = new HashMap();
        map.put("audios__audio__genre__","Rock");
        map.put("audios__audio__identifier__","def1.txt");
        map.put("audios__audio__bitrate__","190");
        list.add(map);
        map = new HashMap();
        map.put("audios__audio__genre__","Classical");
        map.put("audios__audio__identifier__","def2.txt");
        map.put("audios__audio__bitrate__","2192");
        list.add(map);
        map = new HashMap();
        map.put("audios__audio__genre__","Blues");
        map.put("audios__audio__identifier__","def.txt");
        map.put("audios__audio__bitrate__","192");
        list.add(map);
        assertEquals(list, r);
        
        String invalid = "<?xml version=\"1.0\"?>"+
        "<audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\">"+
        "<video something=\"something\"/></audios>";
        
        r = XMLParsingUtils.parse(invalid,1);
        assertFalse(r.isEmpty());
        assertNotNull(r.canonicalKeyPrefix);
        assertNotNull(r.schemaURI);
        
        String nested ="<?xml version=\"1.0\"?>"+
        "<audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\">"+
        "<audio genre=\"Rock\" identifier=\"def1.txt\" bitrate=\"190\">"+
        "<audio genre=\"Classical\" identifier=\"def2.txt\" bitrate=\"2192\">"+
        "<audio genre=\"Blues\" identifier=\"def.txt\" bitrate=\"192\"/></audio></audio></audios>";
        
        r = XMLParsingUtils.parse(nested,3);
        assertFalse(r.isEmpty());
        assertNotNull(r.canonicalKeyPrefix);
        assertNotNull(r.schemaURI);
        assertEquals(list, r);
        
        String empty1 = "<?xml version=\"1.0\"?>"+
        "<audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\">"+
        "</audios>";
        
        r = XMLParsingUtils.parse(empty1,1);
        assertTrue(r.isEmpty());
        assertNotNull(r.canonicalKeyPrefix);
        assertNull(r.schemaURI);
        
        String empty2 = "<?xml version=\"1.0\"?>"+
        "<audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"/>";
        
        r = XMLParsingUtils.parse(empty2,1);
        assertTrue(r.isEmpty());
        assertNotNull(r.canonicalKeyPrefix);
        assertNull(r.schemaURI);
        
    }
    
    public void testParseEncodings() throws Exception {
        List<NameValue<String>> list = new ArrayList<NameValue<String>>();
        list.add(new NameValue<String>("audios__audio__title__", "Rock 'n Roll"));
        list.add(new NameValue<String>("audios__audio__album__", "m&alformed"));
        list.add(new NameValue<String>("audios__audio__comments__", "Sung by \"Joe Smith\""));
        list.add(new NameValue<String>("audios__audio__artist__", "also <malformed"));
        list.add(new NameValue<String>("audios__audio__license__", ">still malformed"));
        list.add(new NameValue<String>("audios__audio__language__", "s\tpace"));
        list.add(new NameValue<String>("audios__audio__seconds__", "even[ m�r�] c�a��cters"));
        list.add(new NameValue<String>("audios__audio__bitrate__", "a\u0002\u0003\u0004b"));
        String xml = limeXMLDocumentFactory.createLimeXMLDocument(list, AUDIO_SCHEMA).getXMLString();
        XMLParsingUtils.ParseResult r = XMLParsingUtils.parse(xml,1);
        assertEquals("http://www.limewire.com/schemas/audio.xsd", r.schemaURI);
        assertEquals("audio", r.type);
        Map<String, String> result = r.get(0);
        assertEquals(result.toString(), "m&alformed", result.get("audios__audio__album__"));
        assertEquals(result.toString(), "Rock 'n Roll", result.get("audios__audio__title__"));
        assertEquals(result.toString(), null, result.get("audios__audio__comments__")); // comment is not kept
//        assertEquals(result.toString(), "Sung by \"Joe Smith\"", result.get("audios__audio__comments__"));
        assertEquals(result.toString(), "also <malformed", result.get("audios__audio__artist__"));
        assertEquals(result.toString(), ">still malformed", result.get("audios__audio__license__"));
        assertEquals(result.toString(), "s pace", result.get("audios__audio__language__"));
        assertEquals(result.toString(), "even[ m�r�] c�a��cters", result.get("audios__audio__seconds__"));
        assertEquals(result.toString(), "a   b", result.get("audios__audio__bitrate__"));
    }
    
    public void testParseInvalid() throws Exception {
        String invalid =
            "<?xml asdf asdf>";
        
        try {
            XMLParsingUtils.parse(invalid,1);
            fail("parsed invalid xml");
        }catch(SAXException expected){}
        
        
        invalid = "<?xml version=\"1.0\"?>"+
        "<audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\">"+
        "<audio genre=\"Rock\" identifier=\"def1.txt\" bitrate=\"190\"/>";
        
        try {
            XMLParsingUtils.parse(invalid,1);
            fail("parsed w/o closing audios tag");
        }catch(SAXException expected){}
        
        invalid = "<?xml version=\"1.0\"?>"+
        "<audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\">"+
        "<audio genre=\"Rock\" identifier=\"def1.txt\" bitrate=\"190\"></audios>";
        
        try {
            XMLParsingUtils.parse(invalid,1);
            fail("parsed with non-null not-closed audio element");
        }catch(SAXException expected){}
        
        invalid = "<?xml version=\"1.0\"?>"+
        "<audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\">"+
        "<audio genre=\"Rock\" identifier=\"def1.txt bitrate=\"190\"/></audios>";
        
        try {
            XMLParsingUtils.parse(invalid,1);
            fail("parsed with malformed attribute");
        }catch(SAXException expected){}
    }

}
