package com.limegroup.bittorrent;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;

import junit.framework.Assert;
import junit.framework.Test;

import org.limewire.util.AssertComparisons;
import org.limewire.util.FileUtils;
import org.limewire.util.TestUtils;

import com.limegroup.gnutella.util.LimeTestCase;

public class BTMetaInfoTest extends LimeTestCase {
    /**
     * A directory containing the test data for this unit test.
     */
    public static final File TEST_DATA_DIR = TestUtils
            .getResourceFile("org/limewire/swarm/bittorrent/public_html/torrents");

    public BTMetaInfoTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(BTMetaInfoTest.class);
    }
    

    public void testBasics() throws Exception {
        File file = getFile("test-basics.torrent");
        byte[] bytes = FileUtils.readFileFully(file);
        BTMetaInfoFactory btm = new BTMetaInfoFactoryImpl();
        BTMetaInfo metaInfo = btm.createBTMetaInfoFromBytes(bytes);

        AssertComparisons.assertEquals(44425, metaInfo.getFileSystem().getTotalSize());
        AssertComparisons.assertEquals("gnutella_protocol_0.4.pdf", metaInfo.getName());
        AssertComparisons.assertEquals(262144, metaInfo.getPieceLength());
        AssertComparisons.assertEquals((int) Math.ceil((double) metaInfo.getFileSystem().getTotalSize()
                / metaInfo.getPieceLength()), metaInfo.getNumBlocks());
        AssertComparisons.assertEquals("http://localhost:8080/tracker/announce", metaInfo.getTrackers()[0]
                .toString());
        AssertComparisons.assertEquals(false, metaInfo.isPrivate());
        AssertComparisons.assertEquals(new URI[0],metaInfo.getWebSeeds());
    }

    public void testSingleWebSeedSingleFile() throws Exception {
        File file = getFile("test-single-webseed-single-file-no-peer.torrent");
        byte[] bytes = FileUtils.readFileFully(file);
        BTMetaInfoFactory btm = new BTMetaInfoFactoryImpl();
        BTMetaInfo metaInfo = btm.createBTMetaInfoFromBytes(bytes);
        AssertComparisons.assertEquals("gnutella_protocol_0.4.pdf", metaInfo.getName());
        AssertComparisons.assertEquals(1, metaInfo.getNumBlocks());
        AssertComparisons.assertEquals(262144, metaInfo.getPieceLength());
        AssertComparisons.assertEquals("http://localhost/~pvertenten/tracker/announce.php", metaInfo
                .getTrackers()[0].toString());
        AssertComparisons.assertEquals(false, metaInfo.isPrivate());
        AssertComparisons.assertNotNull(metaInfo.getWebSeeds());
        AssertComparisons.assertEquals(1, metaInfo.getWebSeeds().length);
        AssertComparisons.assertEquals("http://localhost:8080/pub/gnutella_protocol_0.4.pdf", metaInfo
                .getWebSeeds()[0].toString());
    }

    public void testMultipleWebSeedSingleFile() throws Exception {
        File file = getFile("test-multiple-webseed-single-file-no-peer.torrent");
        byte[] bytes = FileUtils.readFileFully(file);
        BTMetaInfoFactory btm = new BTMetaInfoFactoryImpl();
        BTMetaInfo metaInfo = btm.createBTMetaInfoFromBytes(bytes);
        AssertComparisons.assertEquals("gnutella_protocol_0.4.pdf", metaInfo.getName());
        AssertComparisons.assertEquals(2, metaInfo.getNumBlocks());
        AssertComparisons.assertEquals(32768, metaInfo.getPieceLength());
        AssertComparisons.assertEquals("http://localhost:3456/tracker/announce", metaInfo.getTrackers()[0]
                .toString());
        AssertComparisons.assertEquals(true, metaInfo.isPrivate());
        AssertComparisons.assertNotNull(metaInfo.getWebSeeds());
        AssertComparisons.assertEquals(3, metaInfo.getWebSeeds().length);
        AssertComparisons.assertEquals("http://localhost:8080/pub/", metaInfo.getWebSeeds()[0].toString());
        AssertComparisons.assertEquals("http://localhost:8080/pub2/", metaInfo.getWebSeeds()[1].toString());
        AssertComparisons.assertEquals("http://localhost:8080/pub/gnutella_protocol_0.4.pdf", metaInfo
                .getWebSeeds()[2].toString());
    }

    public void testSingleWebSeedMultipleFile() throws Exception {
        File file = getFile("test-single-webseed-multiple-file-no-peer.torrent");
        byte[] bytes = FileUtils.readFileFully(file);
        BTMetaInfoFactory btm = new BTMetaInfoFactoryImpl();
        BTMetaInfo metaInfo = btm.createBTMetaInfoFromBytes(bytes);
        AssertComparisons.assertEquals("test", metaInfo.getName());

        AssertComparisons.assertEquals(2, metaInfo.getNumBlocks());
        AssertComparisons.assertEquals(262144, metaInfo.getPieceLength());
        AssertComparisons.assertEquals("http://localhost:8080/tracker/announce", metaInfo.getTrackers()[0]
                .toString());
        AssertComparisons.assertEquals(true, metaInfo.isPrivate());
        AssertComparisons.assertNotNull(metaInfo.getWebSeeds());
        AssertComparisons.assertEquals(1, metaInfo.getWebSeeds().length);
        AssertComparisons.assertEquals("http://localhost:8080/pub2/", metaInfo.getWebSeeds()[0].toString());
    }

    public void testMultipleWebSeedMultipleFile() throws Exception {
        File file = getFile("test-multiple-webseed-multiple-file-no-peer.torrent");
        byte[] bytes = FileUtils.readFileFully(file);
        BTMetaInfoFactory btm = new BTMetaInfoFactoryImpl();
        BTMetaInfo metaInfo = btm.createBTMetaInfoFromBytes(bytes);
        AssertComparisons.assertEquals("test", metaInfo.getName());
        AssertComparisons.assertEquals(2, metaInfo.getNumBlocks());
        AssertComparisons.assertEquals(262144, metaInfo.getPieceLength());
        AssertComparisons.assertEquals("http://localhost:8080/tracker/announce", metaInfo.getTrackers()[0]
                .toString());
        AssertComparisons.assertEquals(true, metaInfo.isPrivate());
        AssertComparisons.assertNotNull(metaInfo.getWebSeeds());
        AssertComparisons.assertEquals(2, metaInfo.getWebSeeds().length);
        AssertComparisons.assertEquals("http://localhost:8080/pub/", metaInfo.getWebSeeds()[0].toString());
        AssertComparisons.assertEquals("http://localhost:8080/pub2/", metaInfo.getWebSeeds()[1].toString());
    }
    
    /**
     * Testing using a bad torrent file. Using a random file name that should not exist.
     * Testing to make sure that the createMetaInfo method throws an IOException when a bad
     * file is used.
     */
    public void testBadFileFileDoesNotExist() {
        File nonExistingFile = new File(UUID.randomUUID().toString() + UUID.randomUUID().toString());
        Assert.assertFalse(nonExistingFile.exists());
        BTMetaInfoFactory btm = new BTMetaInfoFactoryImpl();
        try {
            btm.createMetaInfo(nonExistingFile);
            Assert.fail("There should have been an IOException creating the metaInfo.");
        } catch (IOException e) {
            //The exception is expected
        }
    }
    
    
    /**
     * Testing using a bad torrent file. Using a file with bad data.
     * Testing to make sure that the createMetaInfo method throws an IOException 
     * when a bad file is used.
     */
    public void testBadFileInvalidTorrentFile() throws Exception {
        File file = getFile("test-bad-torrent.torrent");
        Assert.assertTrue(file.exists());
        BTMetaInfoFactory btm = new BTMetaInfoFactoryImpl();
        try {
            btm.createMetaInfo(file);
            Assert.fail("There should have been an IOException creating the metaInfo.");
        } catch (IOException e) {
            //The exception is expected
        }
    }

    /**
     * Returns a file in the TEST_DATA_DIR by the given filename.
     * 
     * @param fileName
     * @return
     */
    private File getFile(String fileName) {
        File file = new File(TEST_DATA_DIR.getAbsolutePath() + "/" + fileName);
        return file;
    }
}
