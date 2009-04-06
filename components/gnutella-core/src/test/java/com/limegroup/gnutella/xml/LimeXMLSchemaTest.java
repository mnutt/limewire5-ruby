package com.limegroup.gnutella.xml;

import java.util.Iterator;

import org.limewire.gnutella.tests.LimeTestCase;

import junit.framework.Test;



/**
 * Unit tests for LimeXMLSchema
 */
public class LimeXMLSchemaTest extends LimeTestCase {
            
    public LimeXMLSchemaTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(LimeXMLSchemaTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	/**
	 * Tests getCanonicalizedFieldName and SchemaFieldInfo.<p>
	 *
	 * This test is much stricter than necessary, but there's
	 * really no other way to test it.  If the xsd file changes 
	 * in any way (order, addition, removal), this test will fail.
	 * Alter the checks to fix it.
	 */
    public static void testGetFieldNames() throws Exception {
        LimeXMLSchema schema = new LimeXMLSchema(LimeXMLSchemaTest.class.getClassLoader().getResource("org/limewire/xml/schema/audio.xsd"));
        
        //get the fields and print those
        Iterator iterator = schema.getCanonicalizedFields().iterator();
        
        assertTrue( iterator.hasNext() );
        
        check( iterator, "title", true);
        check( iterator, "artist", true);
        check( iterator, "album", true);
        check( iterator, "genre", true);
        check( iterator, "licensetype", false );
        check( iterator, "track", true);
        check( iterator, "type", true);
        check( iterator, "year", true);
        check( iterator, "seconds", false);
        check( iterator, "language", true);
        check( iterator, "bitrate", false);
        check( iterator, "comments", true);
        check( iterator, "license", true);
        check( iterator, "action", true);
        
        assertTrue( !iterator.hasNext() );
    }
    
    private static void check( Iterator i, String name, boolean editable) {
        String fullName = "audios__audio__" + name + "__";
        
        assertTrue(i.hasNext());
        SchemaFieldInfo fieldInfo = (SchemaFieldInfo)i.next();
        assertEquals(fullName, fieldInfo.getCanonicalizedFieldName());
        assertEquals(editable, fieldInfo.isEditable());
    }
}	
