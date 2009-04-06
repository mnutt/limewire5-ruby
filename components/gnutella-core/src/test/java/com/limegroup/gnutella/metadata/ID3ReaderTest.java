package com.limegroup.gnutella.metadata;


import java.io.File;
import java.io.IOException;

import junit.framework.Test;

import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.util.TestUtils;

import com.google.inject.Injector;
import com.limegroup.gnutella.metadata.audio.AudioMetaData;

public class ID3ReaderTest extends AudioTestBase {
    
    private MetaDataFactory metaDataFactory;

	public ID3ReaderTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(ID3ReaderTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

    ////////////
	@Override
    public void setUp(){
	    Injector injector = LimeTestUtils.createInjector();
	    metaDataFactory = injector.getInstance(MetaDataFactory.class);
    }
    
    //////////

   /**
    * Tests ID3v1.1
    */
    public void testID3v10Tags() throws Exception {
        File file = TestUtils.getResourceFile(dir+"ID3V1.mp3");
        assertTrue("file should exist", file.exists());
        
        AudioMetaData data = (AudioMetaData) metaDataFactory.parse(file);
        validateTag(data);
    }

    /**
     * Tests ID3v1.1b
     */
    public void testID3v11Tags() throws Exception {
        File file = TestUtils.getResourceFile(dir+"ID3V11.mp3");
        assertTrue("file should exist", file.exists());

        AudioMetaData data = (AudioMetaData) metaDataFactory.parse(file);
        validateTag(data);
    }
    
    /**
     * Tests ID3v2.2
     */
    public void testID3v22Tags() throws Exception {
        File file = TestUtils.getResourceFile(dir+"ID3V22.mp3");
        assertTrue("file should exist", file.exists());

        AudioMetaData data = (AudioMetaData) metaDataFactory.parse(file);
        validateTag(data);
     }
 
     /**
     * Tests ID3v2.3
      */
    public void testID3v23Tags() throws Exception {
        File file = TestUtils.getResourceFile(dir+"ID3V23.mp3");
        assertTrue("file should exist", file.exists());
        
        AudioMetaData data = (AudioMetaData) metaDataFactory.parse(file);
        validateTag(data);
    }

    /**
     * Tests ID3v2.4
     */
    public void testID3v24Tags() throws Exception {
        File file = TestUtils.getResourceFile(dir+"ID3V24.mp3");
        assertTrue("file should exist", file.exists());

        AudioMetaData data = (AudioMetaData) metaDataFactory.parse(file);
        validateTag(data);
     }
     
    /**
     * tests that only v2 tag is read if both v1 tag exists
     *  and v2 tag exists
     */
    public void testBothTagsExistReadv2() throws Exception {
        File file = TestUtils.getResourceFile(dir+"ID3All.mp3");
        assertTrue("file should exist", file.exists());
        
        AudioMetaData data = (AudioMetaData) metaDataFactory.parse(file);
        validateTag(data);
    }
        
    public void testBadTag() throws Exception {
        File file = TestUtils.getResourceFile(dir+"corruptFileWithBadHeaders.mp3");
        assertTrue("file should exist", file.exists());

        try {
            metaDataFactory.parse(file);
            fail("IOException expected");
        } catch(IOException e) {
            
        }
    }

}
