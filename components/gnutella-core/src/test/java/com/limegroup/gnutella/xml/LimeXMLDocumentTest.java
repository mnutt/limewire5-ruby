package com.limegroup.gnutella.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.limewire.collection.KeyValue;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;

import com.google.inject.Injector;

import junit.framework.Test;

public class LimeXMLDocumentTest extends LimeTestCase {
            
	private LimeXMLDocumentFactory limeXMLDocumentFactory;

    public LimeXMLDocumentTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(LimeXMLDocumentTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	@Override
	protected void setUp() throws Exception {
		Injector injector = LimeTestUtils.createInjector();
		limeXMLDocumentFactory = injector.getInstance(LimeXMLDocumentFactory.class);
	}
	
    public void testHashcode() throws Exception {
        LimeXMLDocumentFactory factory = limeXMLDocumentFactory;
    	List<KeyValue<String, String>> map = new ArrayList<KeyValue<String, String>>();
    	map.add(new KeyValue<String, String>(LimeXMLNames.APPLICATION_NAME, "value"));
    	LimeXMLDocument doc1 = factory.createLimeXMLDocument(map,
                LimeXMLNames.APPLICATION_SCHEMA);
    	LimeXMLDocument doc2 = factory.createLimeXMLDocument(map,
                LimeXMLNames.APPLICATION_SCHEMA);
    	assertEquals(doc1, doc2);
    	assertEquals(doc1.hashCode(), doc2.hashCode());

    	doc1.initIdentifier(new File("file"));
    	assertEquals(doc1, doc2);
    	assertEquals(doc1.hashCode(), doc2.hashCode());
    }

    public void testGetXMLString() throws Exception {
        String xml = "<?xml version=\"1.0\"?><images xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/image.xsd\"><image title=\"hello world\"/></images>";
        LimeXMLDocument document = limeXMLDocumentFactory.createLimeXMLDocument(xml);
        assertEquals(LimeXMLNames.IMAGE_SCHEMA, document.getSchemaURI());
        assertEquals(xml, document.getXMLString());
        
        // make sure comments are stripped out.
        String comments = "comments=\"woah!\" ";
        xml = "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio title=\"Hello World\" artist=\"Me and you\" album=\"Testing the waters\" genre=\"Rock\" track=\"5/11\" year=\"1999\" seconds=\"956\" bitrate=\"128\" " + comments + "license=\"me and you\"/></audios>";
        document = limeXMLDocumentFactory.createLimeXMLDocument(xml);
        assertEquals(LimeXMLNames.AUDIO_SCHEMA, document.getSchemaURI());
        assertEquals(xml.replace(comments, ""), document.getXMLString());
    }

    public void testNullValues() {
        String schemaURI = "http://www.limewire.com/schemas/audio.xsd";
        Map<String, String> map = new HashMap<String, String>();
        map.put("audios__audio__title__", "foo");
        map.put("audios__audio__artist__", null);
        LimeXMLDocument document = limeXMLDocumentFactory.createLimeXMLDocument(map.entrySet(), schemaURI);
        assertEquals("foo", document.getValue("audios__audio__title__"));
        assertNull(document.getValue("audios__audio__artist__"));
    }

    public void testCachedValues() {
        String schemaURI = "http://www.limewire.com/schemas/audio.xsd";
        Map<String, String> map = new HashMap<String, String>();
        String foo_1 = new String(new char[]{'f','o','o'});
        String foo_2 = new String(new char[]{'f','o','o'});
        map.put("audios__audio__title__", foo_1);
        map.put("audios__audio__artist__", foo_2);
        LimeXMLDocument document = limeXMLDocumentFactory.createLimeXMLDocument(map.entrySet(), schemaURI);
        assertSame(document.getValue("audios__audio__title__"), document.getValue("audios__audio__artist__"));
    }
}	
