package com.limegroup.gnutella.metadata;

import java.io.File;

import junit.framework.Test;

import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.util.TestUtils;

import com.google.inject.Injector;
import com.limegroup.gnutella.metadata.audio.AudioMetaData;

/**
 *  Tests Ogg meta-data reader
 */
public class OGGReaderTest extends AudioTestBase {

    private MetaDataFactory metaDataFactory;
    
    public OGGReaderTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(OGGReaderTest.class);
    }
    
    @Override
    public void setUp(){
        Injector injector = LimeTestUtils.createInjector();
        metaDataFactory = injector.getInstance(MetaDataFactory.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testOgg() throws Exception {
        File file = TestUtils.getResourceFile(dir+"oggAll.ogg");
        assertTrue("file should exist", file.exists());
        
        MetaData data = metaDataFactory.parse(file);
        validateTag((AudioMetaData) data);
    }
    
    public void testNoFieldsOgg() throws Exception {
        File file = TestUtils.getResourceFile(dir+"oggNone.ogg");
        assertTrue("file should exist", file.exists());
        
        MetaData data = metaDataFactory.parse(file);
        AudioMetaData amd = (AudioMetaData) data;
        assertEquals("", amd.getTitle());
        assertEquals("", amd.getArtist());
        assertEquals("", amd.getAlbum());
        assertEquals("", amd.getComment());
        assertEquals("", amd.getTrack());
        assertEquals("", amd.getYear());
        assertEquals("", amd.getGenre());
    }
}