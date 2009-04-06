package com.limegroup.gnutella.metadata;

import java.io.File;

import junit.framework.Test;

import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.util.TestUtils;

import com.google.inject.Injector;
import com.limegroup.gnutella.metadata.audio.AudioMetaData;

/**
 *  Tests M4A meta-data reader
 */
public class M4AReaderTest extends AudioTestBase {

    private MetaDataFactory metaDataFactory;
    
    public M4AReaderTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(M4AReaderTest.class);
    }
    
    @Override
    public void setUp(){
        Injector injector = LimeTestUtils.createInjector();
        metaDataFactory = injector.getInstance(MetaDataFactory.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testM4A() throws Exception {
        File file = TestUtils.getResourceFile(dir+"M4A.m4a");
        assertTrue("file should exist", file.exists());
        
        AudioMetaData data = (AudioMetaData) metaDataFactory.parse(file);
        validateTag(data);
    }
}

