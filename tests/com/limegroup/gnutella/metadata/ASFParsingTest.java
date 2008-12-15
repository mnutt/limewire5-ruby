package com.limegroup.gnutella.metadata;

import java.io.File;
import java.io.IOException;

import junit.framework.Test;

import org.limewire.util.TestUtils;

import com.google.inject.Injector;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.metadata.audio.AudioMetaData;
import com.limegroup.gnutella.util.LimeTestCase;

/**
 * test for the parsing of ASF files.
 */
public class ASFParsingTest extends LimeTestCase {
	
    private MetaDataFactory metaDataFactory;
    
	public ASFParsingTest(String name) {
		super(name);
	}
	
	public static Test suite() {
		return buildTestSuite(ASFParsingTest.class);
	}
	
	@Override
    public void setUp(){
	    Injector injector = LimeTestUtils.createInjector();
	    metaDataFactory = injector.getInstance(MetaDataFactory.class);
	}
	
	public void testWMASimpleDescription() throws IOException {
	    File f = TestUtils.getResourceFile("com/limegroup/gnutella/metadata/simple description.wma");
	    assertTrue(f.exists());
	    
	    AudioMetaData  amd = (AudioMetaData) metaDataFactory.parse(f);
	    
	    assertEquals("Normal Author", amd.getArtist());
	    assertEquals("Normal Title", amd.getTitle());
	    assertEquals("Normal Copyright", amd.getLicense());
	    assertEquals("Normal Description", amd.getComment());
	    assertEquals(315, amd.getBitrate());
	    assertEquals(null, amd.getLicenseType());
	    assertEquals("-1", amd.getTrack());
	    assertEquals(null, amd.getAlbum());
	    assertEquals(null, amd.getYear());
	    assertEquals(null, amd.getGenre());
	    assertEquals(0, amd.getLength());
	    assertEquals(-1, amd.getTotalTracks());
	    assertEquals(-1, amd.getDisk());
	    assertEquals(-1, amd.getTotalDisks());
    }


	public void testWMAExtendedDescription() throws IOException {
	    File f = TestUtils.getResourceFile("com/limegroup/gnutella/metadata/extended description.wma");
	    assertTrue(f.exists());
	    
	    AudioMetaData amd = (AudioMetaData) metaDataFactory.parse(f);
	       
	    assertEquals("An Artist", amd.getArtist());
	    assertEquals("A Title", amd.getTitle());
	    assertEquals("This is a copyright", amd.getLicense());
	    assertEquals("This is a comment", amd.getComment());
	    assertEquals(192, amd.getBitrate());
	    assertEquals(null, amd.getLicenseType());
	    assertEquals("1", amd.getTrack());
	    assertEquals("An Album", amd.getAlbum());
	    assertEquals("2005", amd.getYear());
	    assertEquals("Acoustic", amd.getGenre());
	    assertEquals(0, amd.getLength());
	    assertEquals(-1, amd.getTotalTracks());
	    assertEquals(-1, amd.getDisk());
	    assertEquals(-1, amd.getTotalDisks());
    }

	public void testASFVBR() throws IOException {
	    File f = TestUtils.getResourceFile("com/limegroup/gnutella/metadata/vbr encoding.asf");
	    assertTrue(f.exists());
	    
	    AudioMetaData amd = (AudioMetaData) metaDataFactory.parse(f);
	    
	    assertEquals("Another Artist", amd.getArtist());
	    assertEquals("Another Title", amd.getTitle());
	    assertEquals("None", amd.getLicense());
	    assertEquals("This is a small comment.", amd.getComment());
	    assertEquals(7686, amd.getBitrate());
	    assertEquals(null, amd.getLicenseType());
	    assertEquals("-1", amd.getTrack());
	    assertEquals("Another Album", amd.getAlbum());
	    assertEquals("2001", amd.getYear());
	    assertEquals("My Own", amd.getGenre());
	    assertEquals(0, amd.getLength());
	    assertEquals(-1, amd.getTotalTracks());
	    assertEquals(-1, amd.getDisk());
	    assertEquals(-1, amd.getTotalDisks());
    }
}
