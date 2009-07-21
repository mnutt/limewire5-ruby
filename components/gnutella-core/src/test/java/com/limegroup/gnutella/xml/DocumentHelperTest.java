
package com.limegroup.gnutella.xml;

import java.util.List;

import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;

import junit.framework.Test;

import com.google.inject.Injector;

/**
 * Tests the LimeXMLDocumentHelper logic.
 */
public class DocumentHelperTest extends LimeTestCase {
    
    private LimeXMLDocumentHelper limeXMLDocumentHelper;
    
    public DocumentHelperTest(String name){
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(DocumentHelperTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
		Injector injector = LimeTestUtils.createInjector();
		limeXMLDocumentHelper = injector.getInstance(LimeXMLDocumentHelper.class);
    }
    
    /**
     * tests getting a list of documents from catenated xml
     */
    public void testGetDocuments() throws Exception {
        
        // test short-circuiting
        assertEquals(0,limeXMLDocumentHelper.getDocuments(null,1).size());
        assertEquals(0,limeXMLDocumentHelper.getDocuments("",1).size());
        assertEquals(0,limeXMLDocumentHelper.getDocuments("asfd",0).size());
        
        // test a valid document
        String threeResp = "<?xml version=\"1.0\"?>"+
        "<audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\">"+
        "<audio genre=\"Rock\" identifier=\"def1.txt\" bitrate=\"190\" index=\"0\"/>"+
        "<audio genre=\"Classical\" identifier=\"def2.txt\" bitrate=\"2192\" index=\"1\"/>"+
        "<audio genre=\"Blues\" identifier=\"def.txt\" bitrate=\"192\" index=\"2\"/></audios>";
        
        List l = limeXMLDocumentHelper.getDocuments(threeResp,3);
        assertEquals(1,l.size());
        
        LimeXMLDocument []docs = (LimeXMLDocument [])l.get(0);
        assertEquals(3,docs.length);
        for (int i = 0;i<docs.length;i++)
            assertNotNull(docs[i]);
        
        // test not enough xml
        l = limeXMLDocumentHelper.getDocuments(threeResp,4);
        assertEquals(1,l.size());
        docs = (LimeXMLDocument [])l.get(0);
        assertEquals(4,docs.length);
        assertNull(docs[3]);
        
        // test too much xml
        l = limeXMLDocumentHelper.getDocuments(threeResp,2);
        assertEquals(0,l.size());
        
        //test identical indices
        String identical = "<?xml version=\"1.0\"?>"+
        "<audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\">"+
        "<audio genre=\"Rock\" identifier=\"def1.txt\" bitrate=\"190\" index=\"1\"/>"+
        "<audio genre=\"Classical\" identifier=\"def2.txt\" bitrate=\"2192\" index=\"1\"/>"+
        "<audio genre=\"Blues\" identifier=\"def.txt\" bitrate=\"192\" index=\"0\"/></audios>";
        l = limeXMLDocumentHelper.getDocuments(identical,3);
        assertEquals(1,l.size());
        docs = (LimeXMLDocument [])l.get(0);
        assertEquals(3,docs.length);
        assertNotNull(docs[0]);
        assertNotNull(docs[1]);
        assertNull(docs[2]);
        
        // test invalid indices
        String invalidIndex = 
            "<?xml version=\"1.0\"?>"+
            "<audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\">"+
            "<audio genre=\"Blues\" identifier=\"def.txt\" bitrate=\"192\" index=\"asdf\"/></audios>";
        l = limeXMLDocumentHelper.getDocuments(invalidIndex,1);
        assertEquals(0,l.size());

        invalidIndex = 
            "<?xml version=\"1.0\"?>"+
            "<audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\">"+
            "<audio genre=\"Blues\" identifier=\"def.txt\" bitrate=\"192\" index=\"-2\"/></audios>";
        l = limeXMLDocumentHelper.getDocuments(invalidIndex,1);
        assertEquals(0,l.size());
        
        invalidIndex =
            "<?xml version=\"1.0\"?>"+
            "<audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\">"+
            "<audio genre=\"Blues\" identifier=\"def.txt\" bitrate=\"192\"/></audios>";
        l = limeXMLDocumentHelper.getDocuments(invalidIndex,1);
        assertEquals(0,l.size());
        
        
        // test invalid xml
        String invalidXML =
            "<?xml version=\"1.0\"?>"+
            "<audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\">"+
            "<audio genre=\"Blues\" identifier=\"def.txt\" bitrate=\"192\"/>";
        l = limeXMLDocumentHelper.getDocuments(invalidIndex,1);
        assertEquals(0,l.size());
        
        
        // test valid documented catenated to an invalid one
        String oneValidOneNot = 
            threeResp+invalidXML;
        l = limeXMLDocumentHelper.getDocuments(oneValidOneNot,3);
        assertEquals(1,l.size());
        docs = (LimeXMLDocument [])l.get(0);
        assertEquals(3,docs.length);
        for (int i = 0;i<docs.length;i++)
            assertNotNull(docs[i]);
        
        // test two valid documents
        
        String twoValid = threeResp+identical;
        l = limeXMLDocumentHelper.getDocuments(twoValid,3);
        assertEquals(2,l.size());
        docs = (LimeXMLDocument [])l.get(0);
        assertEquals(3,docs.length);
        for (int i = 0;i<docs.length;i++)
            assertNotNull(docs[i]);
        docs = (LimeXMLDocument [])l.get(1);
        assertEquals(3,docs.length);
        assertNotNull(docs[0]);
        assertNotNull(docs[1]);
        assertNull(docs[2]);
     
        
        // test action 
        String action = "<?xml version=\"1.0\"?>"+
        "<audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\">"+
        "<audio genre=\"Rock\" identifier=\"def1.txt\" bitrate=\"190\" action=\"test.com\" index=\"0\"/></audios>";
        assertEquals("test.com", limeXMLDocumentHelper.getDocuments(action, 1).get(0)[0].getAction());
        
        // test action detail
        action = "<?xml version=\"1.0\"?>"+
        "<audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\">"+
        "<audio genre=\"Rock\" identifier=\"def1.txt\" bitrate=\"190\" action=\"test.com\" addActionDetail=\"False\" index=\"0\"/></audios>";
        assertEquals("test.com", limeXMLDocumentHelper.getDocuments(action, 1).get(0)[0].getAction());
        
        action = "<?xml version=\"1.0\"?>"+
        "<audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\">"+
        "<audio genre=\"Rock\" identifier=\"def1.txt\" bitrate=\"190\" action=\"test.com\" addActionDetail=\"True\" index=\"0\"/></audios>";
        assertEquals("test.com", limeXMLDocumentHelper.getDocuments(action, 1).get(0)[0].getAction());
    }
    
}
