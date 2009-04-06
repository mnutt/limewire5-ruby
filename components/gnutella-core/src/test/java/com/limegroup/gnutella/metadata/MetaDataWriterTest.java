package com.limegroup.gnutella.metadata;

import java.io.File;
import java.util.List;

import junit.framework.Test;

import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.NameValue;
import org.limewire.util.TestUtils;

import com.google.inject.Injector;
import com.limegroup.gnutella.metadata.audio.AudioMetaData;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLDocumentFactory;
import com.limegroup.gnutella.xml.LimeXMLNames;

/**
 * Tests writing MetaData to disk using the MetaDataWriter.
 */
public class MetaDataWriterTest extends AudioTestBase {

    private LimeXMLDocumentFactory limeXMLDocumentFactory;
    private MetaDataFactory metaDataFactory;
    
    private String newTitle = "new title";
    private String newArtist = "new artist";
    private String newAlbum = "new album";
    
    public MetaDataWriterTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(MetaDataWriterTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    public void setUp(){
        Injector injector = LimeTestUtils.createInjector();
        limeXMLDocumentFactory = injector.getInstance(LimeXMLDocumentFactory.class);
        metaDataFactory = injector.getInstance(MetaDataFactory.class);
    }
    
    /**
     * Tests writing meta-data to an m4a File using MetaDataWriter
     */
    public void testM4AWriting() throws Exception {
        File testFile = getTestFile("M4A.m4a", "test0012321.m4a");
        
        MetaDataWriter metaDataWriter = new MetaDataWriter(testFile.getAbsolutePath(), metaDataFactory);
       
        // create an xml doc from the audio data
        LimeXMLDocument doc = getUpdatedXMLDocument(testFile);
        
        metaDataWriter.populate(doc);
        metaDataWriter.commitMetaData();
        
        testSavedFile(testFile);
    }

    /**
     * Tests writing meta-data to an flac File using MetaDataWriter
     */
    public void testFlacWriting() throws Exception {       
        File testFile = getTestFile("Flac.flac", "test0012321.flac");
        
        MetaDataWriter metaDataWriter = new MetaDataWriter(testFile.getAbsolutePath(), metaDataFactory);
        
        // create an xml doc from the audio data
        LimeXMLDocument doc = getUpdatedXMLDocument(testFile);
        
        metaDataWriter.populate(doc);
        metaDataWriter.commitMetaData();
        
        testSavedFile(testFile);
    }
    
    /**
     * Tests writing meta-data to an ogg File using MetaDataWriter
     */
    public void testOggWriting() throws Exception {
        File testFile = getTestFile("oggAll.ogg", "test0012321.ogg");
        
        MetaDataWriter metaDataWriter = new MetaDataWriter(testFile.getAbsolutePath(), metaDataFactory);
        
        // create an xml doc from the audio data
        LimeXMLDocument doc = getUpdatedXMLDocument(testFile);
        
        metaDataWriter.populate(doc);
        metaDataWriter.commitMetaData();
        
        testSavedFile(testFile);
    }
    
    /**
     * Tests writing meta-data to an mp3 file, ID3V1 tag using MetaDataWriter
     */
    public void testMp3V1Writing() throws Exception {
        File testFile = getTestFile("ID3V1.mp3", "test0012321.mp3");
        
        MetaDataWriter metaDataWriter = new MetaDataWriter(testFile.getAbsolutePath(), metaDataFactory);
        
        // create an xml doc from the audio data
        LimeXMLDocument doc = getUpdatedXMLDocument(testFile);
        
        metaDataWriter.populate(doc);
        metaDataWriter.commitMetaData();
        
        testSavedFile(testFile);
    }
    /**
     * Tests writing meta-data to an mp3 file, ID3V11 tag using MetaDataWriter
     */
    public void testMp3V11Writing() throws Exception {
        File testFile = getTestFile("ID3V11.mp3", "test00123211.mp3");
        
        MetaDataWriter metaDataWriter = new MetaDataWriter(testFile.getAbsolutePath(), metaDataFactory);
        
        // create an xml doc from the audio data
        LimeXMLDocument doc = getUpdatedXMLDocument(testFile);
        
        metaDataWriter.populate(doc);
        metaDataWriter.commitMetaData();
        
        testSavedFile(testFile);
    }
    /**
     * Tests writing meta-data to an mp3 file, ID3V22 tag using MetaDataWriter
     */
    public void testMp3V22Writing() throws Exception {
        File testFile = getTestFile("ID3V22.mp3", "test00123222.mp3");
        
        MetaDataWriter metaDataWriter = new MetaDataWriter(testFile.getAbsolutePath(), metaDataFactory);
        
        // create an xml doc from the audio data
        LimeXMLDocument doc = getUpdatedXMLDocument(testFile);
        
        metaDataWriter.populate(doc);
        metaDataWriter.commitMetaData();
        
        testSavedFile(testFile);
    }
    /**
     * Tests writing meta-data to an mp3 file, ID3V23 tag using MetaDataWriter
     */
    public void testMp3V23Writing() throws Exception {
        File testFile = getTestFile("ID3V23.mp3", "test00123223.mp3");
        
        MetaDataWriter metaDataWriter = new MetaDataWriter(testFile.getAbsolutePath(), metaDataFactory);
        
        // create an xml doc from the audio data
        LimeXMLDocument doc = getUpdatedXMLDocument(testFile);
        
        metaDataWriter.populate(doc);
        metaDataWriter.commitMetaData();
        
        testSavedFile(testFile);
    }
    /**
     * Tests writing meta-data to an mp3 file, ID3V24 tag using MetaDataWriter
     */
    public void testMp3V24Writing() throws Exception {
        File testFile = getTestFile("ID3V24.mp3", "test00123224.mp3");
        
        MetaDataWriter metaDataWriter = new MetaDataWriter(testFile.getAbsolutePath(), metaDataFactory);
        
        // create an xml doc from the audio data
        LimeXMLDocument doc = getUpdatedXMLDocument(testFile);
        
        metaDataWriter.populate(doc);
        metaDataWriter.commitMetaData();
        
        testSavedFile(testFile);
    }

    /**
     * Loads a file, copies it to a new file for writing purposes.
     */
    private File getTestFile(String fileToCopy, String testName) {
        File file = TestUtils.getResourceFile(dir + fileToCopy);
        assertTrue(file.exists());
        File testFile = new File(testName);
        testFile.delete();
        FileUtils.copy(file, testFile);
        assertTrue(testFile.exists());
        testFile.deleteOnExit();
        return testFile;
    }
      
    /**
     * Updates the meta data of an LimeXMLDocument
     */
    private LimeXMLDocument getUpdatedXMLDocument(File file) throws Exception {
        // read the meta data from the current audio file
        AudioMetaData data = (AudioMetaData) metaDataFactory.parse(file);
        
        // get the meta-data and update some of the values
        List<NameValue<String>> nameValList = data.toNameValueList();
        nameValList.add(new NameValue<String>(LimeXMLNames.AUDIO_TITLE, newTitle));
        nameValList.add(new NameValue<String>(LimeXMLNames.AUDIO_ARTIST, newArtist));
        nameValList.add(new NameValue<String>(LimeXMLNames.AUDIO_ALBUM, newAlbum));
        
        // create an xml doc from the audio data
        LimeXMLDocument doc = limeXMLDocumentFactory.createLimeXMLDocument(nameValList, data.getSchemaURI());
        assertNotNull(doc);
        
        return doc;
    }
 
    /**
     * Tests that the modifications to the XML doc have been written to desk
     */
    private void testSavedFile(File file) throws Exception {
        // read the file again
        AudioMetaData amd = (AudioMetaData) metaDataFactory.parse(file);
        
        // test the values, chanages values should be read
        assertEquals(newTitle, amd.getTitle());
        assertEquals(newArtist, amd.getArtist());
        assertEquals(newAlbum, amd.getAlbum());
        assertEquals(COMMENT, amd.getComment());
        assertEquals(TRACK, amd.getTrack());
        assertEquals(YEAR, amd.getYear());
        assertEquals(GENRE, amd.getGenre());
    }
}
