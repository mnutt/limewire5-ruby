package com.limegroup.gnutella.library;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;

import org.limewire.core.settings.SearchSettings;
import org.limewire.io.GUID;
import org.limewire.util.MediaType;

import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * Integration tests for query handling
 */
public class KeywordIndexFileManagerIntegrationTest extends FileManagerTestCase {



    public KeywordIndexFileManagerIntegrationTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(KeywordIndexFileManagerIntegrationTest.class);
    }

    /**
     * Test plaintext searching to ensure that shared files' metadata is searched.
     *
     * This tests the following scenarios with 1 contrived audio file and its
     * corresponding metadata:
     *
     * 1. Exact match of metadata field
     * 2. Prefix match of metadata field
     * 3. Prefix match of metadata field with varying case
     * 4. All keywords in search are in at least 1 field of metadata
     * 5. All keywords in search are NOT found in any metadata NOR file name - should not match
     * 6. Metadata field text + additional text should not match (negative test)
     * 7. Exact or Prefix match of filename should still show up as result (regression)
     * 8. Search term that matches both file name and metadata
     *    should return only 1 result
     * 9. Removing file from file manager results in both exact matches
     *    and prefix matches failing (negative test)
     * 10. Exact or Prefix match of filename should still show up as result (regression)
     *
     */
    public void testMetadataResultsForPlaintextQuerySingleFile() throws Exception {
        waitForLoad();

        // test a query where the filename is meaningless but XML matches.
        File f1 = createNewNamedTestFile(10, "meaningless");
        LimeXMLDocument d1 = limeXMLDocumentFactory.createLimeXMLDocument(FileManagerTestUtils.buildAudioXMLString(
                "artist=\"Sammy B\" album=\"Jazz in A minor\" genre=\"mean Median Standard Deviation\" "));
        List<LimeXMLDocument> l1 = new ArrayList<LimeXMLDocument>();
        l1.add(d1);
        FileListChangedEvent result = addIfShared(f1, l1);
        assertTrue(result.toString(), result.getType() == FileListChangedEvent.Type.ADDED);
        assertEquals(d1, result.getFileDesc().getLimeXMLDocuments().get(0));

        // test exact match of keywords in metadata
        Response[] responses = keywordIndex.query(queryRequestFactory.createRequery("Sammy B"));

        assertEquals(1, responses.length);
        assertEquals(d1.getXMLString(), responses[0].getDocument().getXMLString());

        // test exact match on album in metadata
        responses = keywordIndex.query(queryRequestFactory.createQuery("Jazz in A minor"));

        assertEquals(1, responses.length);
        assertEquals(d1.getXMLString(), responses[0].getDocument().getXMLString());

        // test searching metadata fields with prefixes
        responses = keywordIndex.query(queryRequestFactory.createQuery("Sam"));
        assertEquals(1, responses.length);
        assertEquals(d1.getXMLString(), responses[0].getDocument().getXMLString());

        responses = keywordIndex.query(queryRequestFactory.createQuery("Ja"));
        assertEquals(1, responses.length);
        assertEquals(d1.getXMLString(), responses[0].getDocument().getXMLString());

        // test searching metadata fields with prefixes of varying case
        responses = keywordIndex.query(queryRequestFactory.createQuery("sAm"));
        assertEquals(1, responses.length);
        assertEquals(d1.getXMLString(), responses[0].getDocument().getXMLString());

        responses = keywordIndex.query(queryRequestFactory.createQuery("jaZZ In"));
        assertEquals(1, responses.length);
        assertEquals(d1.getXMLString(), responses[0].getDocument().getXMLString());

        // matching: all of the search term's keywords are contained
        // in at least 1 metadata field
        responses = keywordIndex.query(queryRequestFactory.createQuery("median standard deviation"));
        assertEquals(1, responses.length);
        assertEquals(d1.getXMLString(), responses[0].getDocument().getXMLString());

        responses = keywordIndex.query(queryRequestFactory.createQuery("A Minor"));
        assertEquals(1, responses.length);
        assertEquals(d1.getXMLString(), responses[0].getDocument().getXMLString());
        
        // not matching: none of the metadata fields nor file name
        // match all keywords in search term
        responses = keywordIndex.query(queryRequestFactory.createQuery("variance median deviation"));
        assertEquals(0, responses.length);

        // not matching: String contains full metadata but has additional text
        responses = keywordIndex.query(queryRequestFactory.createQuery("Sammy B XYZ"));
        assertEquals(0, responses.length);

        // not matching: Search contains prefix but also contains additional text
        responses = keywordIndex.query(queryRequestFactory.createQuery("sammm"));
        assertEquals(0, responses.length);

        // search something that is in just the file name prefix
        responses = keywordIndex.query(queryRequestFactory.createQuery("meaningless"));
        assertEquals(1, responses.length);
        assertEquals(d1.getXMLString(), responses[0].getDocument().getXMLString());

        // search something that is in both the file name prefix AND the metadata
        // Make sure there is only 1 search result
        responses = keywordIndex.query(queryRequestFactory.createQuery("mean"));
        assertEquals(1, responses.length);
        assertEquals(d1.getXMLString(), responses[0].getDocument().getXMLString());

        // search something that is neither in the file name NOR the metadata
        responses = keywordIndex.query(queryRequestFactory.createQuery("No Matches"));
        assertEquals(0, responses.length);

        // remove file
        fman.getManagedFileList().remove(f1);

        // no more matches
        responses = keywordIndex.query(queryRequestFactory.createQuery("Sammy B"));
        assertEquals(0, responses.length);

        // test on album
        responses = keywordIndex.query(queryRequestFactory.createQuery("Jazz in A minor"));

        assertEquals(0, responses.length);

        // prefixes
        responses = keywordIndex.query(queryRequestFactory.createQuery("Sam"));
        assertEquals(0, responses.length);
    }


    /**
     * Test plaintext searching with multiple files indexed.  The files are in
     * multiple media types.
     *
     * The following scenarios are tested:
     *
     * 1. All keywords in search match 1 file name (should return 1 match)
     *    Search term "four six" matches "eight four six.mp3
     * 2. All keywords match multiple times for the same document (should return 1 match)
     * 3. All keywords in search match, but not for the same file. (should return 0 matches)
     *    Search term "seven nine", files "eight four six.txt", "seven.txt" and "nine.avi"
     * 4. Search with keyword that matches in different metadata fields of different files (matches)
     * 5. Every keyword in search matches in a metadata field, but not for the same file. (No matches)
     * 6. Every keyword in search matches for the same file, but not in same metadata field. (1 Match)
     * 7. Every keyword in search matches in same metadata field name but for different files (No matches)
     * 8. Search keyword matches prefix of metadata or file name for multiple files.
     *    All files that matched are returned.
     * 9. Removing file from file manager results in both exact matches
     *    and prefix matches (file name or metadata) NO LONGER MATCHING for the file that was removed
     *
     */
    public void testPlaintextQueryMultipleFilesMultipleMediaTypes() throws Exception {
        waitForLoad();

        File eightFourSixFile = createNewNamedTestFile(10, "eight four six.mp3");
        LimeXMLDocument eightFourSixXml = limeXMLDocumentFactory.createLimeXMLDocument(
            FileManagerTestUtils.buildAudioXMLString("artist=\"sixty seven\" album=\"zero one\" genre=\"five numerically\" "));
        List<LimeXMLDocument> l1 = new ArrayList<LimeXMLDocument>();
        l1.add(eightFourSixXml);
        addIfShared(eightFourSixFile, l1);

        File f2 = createNewNamedTestFile(10, "seven.txt");
        LimeXMLDocument seven = limeXMLDocumentFactory.createLimeXMLDocument(
            FileManagerTestUtils.buildDocumentXMLString("title=\"front page stuff\" author=\"special writer\" " +
                        "topic=\"interesting stuff\""));
        List<LimeXMLDocument> l2 = new ArrayList<LimeXMLDocument>();
        l2.add(seven);
        addIfShared(f2, l2);

        File f3 = createNewNamedTestFile(10, "nine.avi");
        LimeXMLDocument nine = limeXMLDocumentFactory.createLimeXMLDocument(
            FileManagerTestUtils.buildVideoXMLString("director=\"one interesting tree\" title=\"Eight Four David\""));
        List<LimeXMLDocument> l3 = new ArrayList<LimeXMLDocument>();
        l3.add(nine);
        addIfShared(f3, l3);
        
        File algebraFile = createNewNamedTestFile(10, "class time.txt");
        LimeXMLDocument algebraXml = limeXMLDocumentFactory.createLimeXMLDocument(
                FileManagerTestUtils.buildDocumentXMLString("title=\"plus minus\" " +
                        "author=\"equals\" " + "topic=\"matrix eigenvalue\""));
        List<LimeXMLDocument> l4 = new ArrayList<LimeXMLDocument>();
        l4.add(algebraXml);
        addIfShared(algebraFile, l4);

        
        // 1. All keywords in search term match file name
        responses = keywordIndex.query(queryRequestFactory.createQuery("four six"));
        assertEquals(1, responses.length);
        assertEquals(eightFourSixXml.getXMLString(), responses[0].getDocument().getXMLString());

        // 2. keyword matches multiple times for the same document
        responses = keywordIndex.query(queryRequestFactory.createQuery("six"));
        assertEquals(1, responses.length);
        assertTrue(responsesContain(eightFourSixXml));

        // 3. Every keyword in search matches, but not for the same file.
        responses = keywordIndex.query(queryRequestFactory.createQuery("seven nine"));
        assertEquals(0, responses.length);

        // 4. search with 1 keyword that matches in different metadata fields of different files
        responses = keywordIndex.query(queryRequestFactory.createQuery("interesting"));
        assertEquals(2, responses.length);
        assertTrue(responsesContain(seven, nine));

        // 5. Every keyword in search matches in a metadata field, but not for the same file.
        responses = keywordIndex.query(queryRequestFactory.createQuery("plus special writer"));
        assertEquals(0, responses.length);

        // 6. Every keyword in search matches for same file, but not in same metadata field
        responses = keywordIndex.query(queryRequestFactory.createQuery("interesting front page stuff"));
        assertEquals(1, responses.length);

        // 7. Every keyword in search matches in same metadata field name but for different files (No matches)
        responses = keywordIndex.query(queryRequestFactory.createQuery("front plus minus"));
        assertEquals(0, responses.length);

        // 8. Search keyword matches prefix of metadata or file name for multiple files. 
        //    All files that matched are returned.
        responses = keywordIndex.query(queryRequestFactory.createQuery("eig"));
        assertEquals(3, responses.length);
        assertTrue(responsesContain(algebraXml, nine, eightFourSixXml));

        // remove a file for which "eig" matches in the metadata
        fman.getManagedFileList().remove(algebraFile);

        // 9. Perform same query as before, and while the other files should still match,
        //    the removed file should no longer match
        responses = keywordIndex.query(queryRequestFactory.createQuery("eig"));
        assertEquals(2, responses.length);
        assertTrue(responsesContain(eightFourSixXml, nine));
        assertFalse(responsesContain(algebraXml));
    }

    /**
     * Tests metadata keyword searching.
     *
     * 1. If a file's metadata has an attribute "title='one six eight'", a metadata search
     *    for "title='six eight'" should succeed because it matches all keywords for that attribute.
     *
     * 2. Given a file with metadata of "title='one six eight'", a metadata search
     *    for "title='six eight ten'" should fail because although 2 keywords match that attribute,
     *    1 keyword does not match.  The keyword that does not match for "title" may match in another metadata
     *    field such as "artist" or "album".
     */
    public void testMetaQueryKeywordMatching() throws Exception {
        waitForLoad();

        File eightFourSixFile = createNewNamedTestFile(10, "eight four six.mp3");
        LimeXMLDocument eightFourSixXml = limeXMLDocumentFactory.createLimeXMLDocument(
            FileManagerTestUtils.buildAudioXMLString("artist=\"sixty seven\" album=\"zero one\" genre=\"five nine sixty\" "));
        List<LimeXMLDocument> l1 = new ArrayList<LimeXMLDocument>();
        l1.add(eightFourSixXml);
        addIfShared(eightFourSixFile, l1);

        responses = keywordIndex.query(queryRequestFactory.createQuery("", FileManagerTestUtils.buildAudioXMLString("genre=\"nine\"")));
        assertEquals(1, responses.length);
        assertTrue(responsesContain(eightFourSixXml));

        responses = keywordIndex.query(queryRequestFactory.createQuery("", FileManagerTestUtils.buildAudioXMLString("genre=\"five nine one\"")));
        assertEquals(0, responses.length);
    }

    /**
     * Given the following shared files:
     *
     * #1. An audio file with title "one two three four"
     * #2. A video file with title "one two three four"
     *
     * A query request with query string "one two three four" and
     * media type "audio" should only match file #1.
     *
     */
    public void testMetadataResultsForPlaintextQueryWithSpecificMimeType() throws Exception {
        waitForLoad();

        File f1 = createNewNamedTestFile(10, "audioFile");
        LimeXMLDocument audioMetaData = limeXMLDocumentFactory.createLimeXMLDocument(FileManagerTestUtils.buildAudioXMLString(
                "artist=\"one two tree\" album=\"Jazz in A minor\" genre=\"mean\" "));
        List<LimeXMLDocument> l1 = new ArrayList<LimeXMLDocument>();

        File f2 = createNewNamedTestFile(10, "videoFile");
        LimeXMLDocument videoMetaData = limeXMLDocumentFactory.createLimeXMLDocument(
                FileManagerTestUtils.buildVideoXMLString("director=\"one two tree\" title=\"Four five Six\""));
        List<LimeXMLDocument> l2 = new ArrayList<LimeXMLDocument>();

        l1.add(audioMetaData);
        l2.add(videoMetaData);

        addIfShared(f1, l1);
        addIfShared(f2, l2);

        // A query request with query string "one two three four" and
        // media type "audio" should only match the file named "audioFile".
        String queryString = "one two tree";
        QueryRequest queryAudio =
                queryRequestFactory.createQuery(GUID.makeGuid(), queryString, "",
                        MediaType.getAudioMediaType());
        Response[] responses = keywordIndex.query(queryAudio);
        assertEquals(1, responses.length);
        assertEquals(audioMetaData.getXMLString(), responses[0].getDocument().getXMLString());

        // A query request with query string "one two three four" and
        // media type "video" should only match the file named "videoFile".
        QueryRequest queryVideo =
                queryRequestFactory.createQuery(GUID.makeGuid(), queryString, "",
                        MediaType.getVideoMediaType());
        responses = keywordIndex.query(queryVideo);
        assertEquals(1, responses.length);
        assertEquals(videoMetaData.getXMLString(), responses[0].getDocument().getXMLString());

        // A query request with query string "one two three four" and
        // media type "video" should only match the file named "videoFile".
        QueryRequest queryPrograms =
                queryRequestFactory.createQuery(GUID.makeGuid(), queryString, "",
                        MediaType.getProgramMediaType());
        responses = keywordIndex.query(queryPrograms);
        assertEquals(0, responses.length);
    }

    /**
     * Test that if the search setting "INCLUDE_METADATA_IN_PLAINTEXT_SEARCH" is set to false,
     * plaintext queries that match the metadata of shared files will not show up as results.
     *
     */
    public void testMetadataResultsForPlaintextQueryWithSearchSettingTurnedOff() throws Exception {
        waitForLoad();

        // test a query where the filename is meaningless but XML matches.
        File f1 = createNewNamedTestFile(10, "meaningless");
        LimeXMLDocument d1 = limeXMLDocumentFactory.createLimeXMLDocument(FileManagerTestUtils.buildAudioXMLString(
                "artist=\"Sammy B\" album=\"Jazz in A minor\" genre=\"mean\" "));
        List<LimeXMLDocument> l1 = new ArrayList<LimeXMLDocument>();
        l1.add(d1);
        addIfShared(f1, l1);


        assertTrue(SearchSettings.INCLUDE_METADATA_IN_PLAINTEXT_SEARCH.getValue());

        SearchSettings.INCLUDE_METADATA_IN_PLAINTEXT_SEARCH.setValue(false);

        // test that with the setting set to false, plaintext queries on the metadata
        // do not succeed, but file name searches still succeed

        // file search should succeed
        responses = keywordIndex.query(queryRequestFactory.createQuery("meaningle"));
        assertEquals(1, responses.length);
        assertEquals(d1.getXMLString(), responses[0].getDocument().getXMLString());

        // plaintext search of metadata should fail
        responses = keywordIndex.query(queryRequestFactory.createQuery("Sammy"));
        assertEquals(0, responses.length);

        // turn the setting back on, and plaintext searches of metadata should succeed.
        SearchSettings.INCLUDE_METADATA_IN_PLAINTEXT_SEARCH.setValue(true);

        // file search should still succeed
        responses = keywordIndex.query(queryRequestFactory.createQuery("meaning"));
        assertEquals(1, responses.length);
        assertEquals(d1.getXMLString(), responses[0].getDocument().getXMLString());

        // plaintext search of metadata should succeed
        responses = keywordIndex.query(queryRequestFactory.createQuery("Sammy"));
        assertEquals(1, responses.length);
        assertEquals(d1.getXMLString(), responses[0].getDocument().getXMLString());

    }

    /**
     * Make sure a "What is New" query does not search the meta data,
     * nor should it search file names.  It should only get the 3 files that were most
     * recently shared.
     */
    public void testWhatIsNewQueryDoesNotSearchMetaData() throws Exception {
        waitForLoad();

        // create and add files to file manager with associated metadata
        //

        // This file contains the "whatisnewxoxo" string in the metadata
        addFileWithMetaDataToFileManager("fileShouldNotShowUpInWhat-Is-New.txt", FileManagerTestUtils.buildAudioXMLString(
                                "artist=\"WhatIsNewXOXO\" album=\"Badd in A minor\" genre=\"mean\" "));
        
        addFileWithMetaDataToFileManager("145random", FileManagerTestUtils.buildAudioXMLString(
                "artist=\"should not show up\" album=\"Radd on A minor\" genre=\"mean\" "));

        addFileWithMetaDataToFileManager("987abctrees", FileManagerTestUtils.buildAudioXMLString(
                "artist=\"should not show up\" album=\"Faze in A minor\" genre=\"mean\" "));

        addFileWithMetaDataToFileManager("search123.txt", FileManagerTestUtils.buildAudioXMLString(
                "artist=\"lll two tree\" album=\"Raze in A minor\" genre=\"mean\" "));

        addFileWithMetaDataToFileManager("456search.txt", FileManagerTestUtils.buildAudioXMLString(
                "artist=\"nnn two tree\" album=\"Daze in A minor\" genre=\"mean\" "));

        addFileWithMetaDataToFileManager("678attempt.txt", FileManagerTestUtils.buildAudioXMLString(
                "artist=\"ooo two tree\" album=\"Paze in A minor\" genre=\"mean\" "));
        
        QueryRequest query = queryRequestFactory.createWhatIsNewQuery(GUID.makeGuid(), (byte)3);

        // sanity check!
        assertTrue(query.isWhatIsNewRequest());

        responses = keywordIndex.query(query);
        assertEquals(3, responses.length);

        // make sure that none of the "what is new" responses correspond to the
        // older file which contained "whatisnewxoxo" in its metadata
        for (Response response : responses) {
            assertFalse(response.getName().startsWith("fileShouldNotShowUpInWhat-Is-New.txt"));
        }

        // create query with "whatisnewxoxo" as query string, and
        // make sure "fileShouldNotShowUpInWhat-Is-New.txt" shows up as a result.
        responses = keywordIndex.query(queryRequestFactory.createQuery("WhatIsNewXOXO"));
        assertEquals(1, responses.length);
        assertTrue(responses[0].getName().startsWith("fileShouldNotShowUpInWhat-Is-New.txt"));
    }

    /**
     * Make sure the response to a "What is New" query contains metadata.
     */
    public void testWhatsNewSearchResponseContainsMetaData() throws Exception {

        waitForLoad();

        addFileWithMetaDataToFileManager("678attempt.txt", FileManagerTestUtils.buildAudioXMLString(
                "artist=\"ooo two tree\" album=\"Paze in A minor\" genre=\"mean\" "));

        QueryRequest query = queryRequestFactory.createWhatIsNewQuery(GUID.makeGuid(), (byte)3);

        responses = keywordIndex.query(query);
        assertEquals(1, responses.length);

        assertNotNull(responses[0].getDocument());
    }


    private void addFileWithMetaDataToFileManager(String name, String xml) throws Exception {
        File f1 = createNewNamedTestFile(10, name);

        LimeXMLDocument metaData = limeXMLDocumentFactory.createLimeXMLDocument(xml);
        List<LimeXMLDocument> l1 = new ArrayList<LimeXMLDocument>();

        l1.add(metaData);
        addIfShared(f1, l1);
        Thread.sleep(500);
    }

}
