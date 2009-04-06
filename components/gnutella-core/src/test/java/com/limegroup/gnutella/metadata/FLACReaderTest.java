package com.limegroup.gnutella.metadata;

import java.io.File;

import junit.framework.Test;

import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.util.TestUtils;

import com.google.inject.Injector;
import com.limegroup.gnutella.metadata.audio.AudioMetaData;

/**
 *  Tests FLAC meta-data reader
 */
public class FLACReaderTest extends AudioTestBase {
    
    private MetaDataFactory metaDataFactory;
    
    public FLACReaderTest(String name) {
        super(name);
    }
    
    @Override
    public void setUp(){
        Injector injector = LimeTestUtils.createInjector();
        metaDataFactory = injector.getInstance(MetaDataFactory.class);
    }

    public static Test suite() {
        return buildTestSuite(FLACReaderTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testFlac() throws Exception {
        File file = TestUtils.getResourceFile(dir+"Flac.flac");
        assertTrue("file should exist", file.exists());
        
        AudioMetaData data = (AudioMetaData) metaDataFactory.parse(file);
        validateTag(data);
    }
}