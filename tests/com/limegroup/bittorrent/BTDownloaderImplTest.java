package com.limegroup.bittorrent;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import junit.framework.Test;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.util.AssertComparisons;
import org.limewire.util.FileUtils;
import org.limewire.util.TestUtils;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.limegroup.gnutella.ActivityCallbackAdapter;
import com.limegroup.gnutella.LimeWireCoreModule;
import com.limegroup.gnutella.downloader.CoreDownloaderFactory;
import com.limegroup.gnutella.util.LimeTestCase;

/**
 * Test cases for the BTDownloader.
 * 
 * It takes a torrent file and downloads it, then checks the files contents
 * against an MD5.
 * 
 */
public class BTDownloaderImplTest extends LimeTestCase {
    private static final int TEST_PORT = 8080;

    /**
     * A directory containing the torrent data for this unit test.
     */
    public static final File TORRENT_DIR = TestUtils
            .getResourceFile("org/limewire/swarm/bittorrent/public_html/torrents");

    /**
     * A directory containing the download data for this unit test.
     */
    public static final File FILE_DIR = TestUtils
            .getResourceFile("org/limewire/swarm/bittorrent/public_html");

    private boolean localIsPrivateBackup = false;

    private boolean forceIPAddressBackup = false;

    private String forceIPAddressStringBackup = null;

    private FileServer fileServer = null;

    public BTDownloaderImplTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(BTDownloaderImplTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        localIsPrivateBackup = ConnectionSettings.LOCAL_IS_PRIVATE.getValue();
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        forceIPAddressBackup = ConnectionSettings.FORCE_IP_ADDRESS.getValue();
        ConnectionSettings.FORCE_IP_ADDRESS.setValue(true);
        forceIPAddressStringBackup = ConnectionSettings.FORCED_IP_ADDRESS_STRING.getValue();
        ConnectionSettings.FORCED_IP_ADDRESS_STRING.setValue("127.0.0.1");
        fileServer = new FileServer(TEST_PORT, FILE_DIR);
        fileServer.start();
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(localIsPrivateBackup);
        ConnectionSettings.FORCE_IP_ADDRESS.setValue(forceIPAddressBackup);
        ConnectionSettings.FORCED_IP_ADDRESS_STRING.setValue(forceIPAddressStringBackup);
        fileServer.stop();
        fileServer.destroy();
        super.tearDown();
    }

    /**
     * This test tries to download a single file torrent from a tracker/peer
     * setup on the www.limewire.org server.
     */
    public void testSingleFilePeer() throws Exception {
        File torrentFile = createFile("test-peer-dl-single-file.torrent");

        BTDownloaderImpl downloader = createBTDownloader(torrentFile);
        TorrentContext torrentContext = downloader.getTorrentContext();
        TorrentFileSystem torrentFileSystem = torrentContext.getFileSystem();

        File completeFile = torrentFileSystem.getCompleteFile();
        completeFile.delete();
        completeFile.deleteOnExit();

        File incompleteFile = torrentFileSystem.getIncompleteFiles().get(0);
        incompleteFile.delete();
        incompleteFile.deleteOnExit();

        downloader.startDownload();
        finishDownload(downloader);

        assertDownload("8055d620ba0c507c1af957b43648c99f", completeFile, 44425);
    }

    /**
     * This test tries to download a multi file torrent from a tracker/peer
     * setup on the www.limewire.org server.
     */
    public void testMultipleFilePeer() throws Exception {
        File torrentFile = createFile("test-peer-dl-multiple-file.torrent");

        BTDownloaderImpl downloader = createBTDownloader(torrentFile);
        TorrentContext torrentContext = downloader.getTorrentContext();
        TorrentFileSystem torrentFileSystem = torrentContext.getFileSystem();

        File rootFile = torrentFileSystem.getCompleteFile();
        try {
            FileUtils.deleteRecursive(rootFile);
            rootFile.deleteOnExit();

            File incompleteFile1 = torrentFileSystem.getIncompleteFiles().get(0);
            incompleteFile1.delete();
            incompleteFile1.deleteOnExit();

            File completeFile1 = torrentFileSystem.getFiles().get(0);
            completeFile1.delete();
            completeFile1.deleteOnExit();

            File incompleteFile2 = torrentFileSystem.getIncompleteFiles().get(1);
            incompleteFile2.delete();
            incompleteFile2.deleteOnExit();

            File completeFile2 = torrentFileSystem.getFiles().get(1);
            completeFile2.delete();
            completeFile2.deleteOnExit();

            downloader.startDownload();
            finishDownload(downloader);

            assertDownload("8055d620ba0c507c1af957b43648c99f", completeFile1, 44425);
            assertDownload("db1dc452e77d30ce14acca6bac8c66bc", completeFile2, 411090);
        } finally {
            if (rootFile != null) {
                FileUtils.deleteRecursive(rootFile);
            }
        }
    }

    public void testSingleWebSeedSingleFileNoPeer() throws Exception {
        File torrentFile = createFile("test-single-webseed-single-file-no-peer.torrent");

        BTDownloaderImpl downloader = createBTDownloader(torrentFile);
        TorrentContext torrentContext = downloader.getTorrentContext();
        TorrentFileSystem torrentFileSystem = torrentContext.getFileSystem();

        File rootFile = torrentFileSystem.getCompleteFile();
        rootFile.delete();
        rootFile.deleteOnExit();

        File incompleteFile1 = torrentFileSystem.getIncompleteFiles().get(0);
        incompleteFile1.delete();
        incompleteFile1.deleteOnExit();

        File completeFile1 = torrentFileSystem.getFiles().get(0);
        completeFile1.delete();
        completeFile1.deleteOnExit();

        downloader.startDownload();
        finishDownload(downloader);
        assertDownload("8055d620ba0c507c1af957b43648c99f", completeFile1, 44425);
    }

    public void testMultiWebSeedSingleFileNoPeer() throws Exception {
        File torrentFile = createFile("test-multiple-webseed-single-file-no-peer.torrent");

        BTDownloaderImpl downloader = createBTDownloader(torrentFile);
        TorrentContext torrentContext = downloader.getTorrentContext();
        TorrentFileSystem torrentFileSystem = torrentContext.getFileSystem();

        File rootFile = torrentFileSystem.getCompleteFile();
        rootFile.delete();
        rootFile.deleteOnExit();

        File incompleteFile1 = torrentFileSystem.getIncompleteFiles().get(0);
        incompleteFile1.delete();
        incompleteFile1.deleteOnExit();

        File completeFile1 = torrentFileSystem.getFiles().get(0);
        completeFile1.delete();
        completeFile1.deleteOnExit();

        downloader.startDownload();
        finishDownload(downloader);

        assertDownload("8055d620ba0c507c1af957b43648c99f", completeFile1, 44425);
    }

    public void testSingleWebSeedMultipleFileNoPeer() throws Exception {
        File torrentFile = createFile("test-single-webseed-multiple-file-no-peer.torrent");

        BTDownloaderImpl downloader = createBTDownloader(torrentFile);
        TorrentContext torrentContext = downloader.getTorrentContext();
        TorrentFileSystem torrentFileSystem = torrentContext.getFileSystem();

        File rootFile = torrentFileSystem.getCompleteFile();
        try {

            FileUtils.deleteRecursive(rootFile);
            rootFile.deleteOnExit();

            File incompleteFile1 = torrentFileSystem.getIncompleteFiles().get(0);
            incompleteFile1.delete();
            incompleteFile1.deleteOnExit();

            File completeFile1 = torrentFileSystem.getFiles().get(0);
            completeFile1.delete();
            completeFile1.deleteOnExit();

            File incompleteFile2 = torrentFileSystem.getIncompleteFiles().get(1);
            incompleteFile2.delete();
            incompleteFile2.deleteOnExit();

            File completeFile2 = torrentFileSystem.getFiles().get(1);
            completeFile2.delete();
            completeFile2.deleteOnExit();

            downloader.startDownload();
            finishDownload(downloader);

            assertDownload("8055d620ba0c507c1af957b43648c99f", completeFile1, 44425);
            assertDownload("db1dc452e77d30ce14acca6bac8c66bc", completeFile2, 411090);
        } finally {
            if (rootFile != null) {
                FileUtils.deleteRecursive(rootFile);
            }
        }
    }

    public void testMultiWebSeedMultipleFileNoPeer() throws Exception {
        File torrentFile = createFile("test-multiple-webseed-multiple-file-no-peer.torrent");

        BTDownloaderImpl downloader = createBTDownloader(torrentFile);
        TorrentContext torrentContext = downloader.getTorrentContext();
        TorrentFileSystem torrentFileSystem = torrentContext.getFileSystem();

        File rootFile = torrentFileSystem.getCompleteFile();
        try {
            FileUtils.deleteRecursive(rootFile);
            rootFile.deleteOnExit();

            File incompleteFile1 = torrentFileSystem.getIncompleteFiles().get(0);
            incompleteFile1.delete();
            incompleteFile1.deleteOnExit();

            File completeFile1 = torrentFileSystem.getFiles().get(0);
            completeFile1.delete();
            completeFile1.deleteOnExit();

            File incompleteFile2 = torrentFileSystem.getIncompleteFiles().get(1);
            incompleteFile2.delete();
            incompleteFile2.deleteOnExit();

            File completeFile2 = torrentFileSystem.getFiles().get(1);
            completeFile2.delete();
            completeFile2.deleteOnExit();

            downloader.startDownload();
            finishDownload(downloader);

            assertDownload("8055d620ba0c507c1af957b43648c99f", completeFile1, 44425);
            assertDownload("db1dc452e77d30ce14acca6bac8c66bc", completeFile2, 411090);
        } finally {
            if (rootFile != null) {
                FileUtils.deleteRecursive(rootFile);
            }
        }
    }

    public void testSingleWebSeedSingleFilePeers() throws Exception {
        // TODO force peers and webseed to not have all pieces
        // only when used together have all the pieces

        File torrentFile = createFile("test-single-webseed-single-file-peer.torrent");

        BTDownloaderImpl downloader = createBTDownloader(torrentFile);
        TorrentContext torrentContext = downloader.getTorrentContext();
        TorrentFileSystem torrentFileSystem = torrentContext.getFileSystem();

        File rootFile = torrentFileSystem.getCompleteFile();
        try {
            FileUtils.deleteRecursive(rootFile);
            rootFile.deleteOnExit();

            File incompleteFile1 = torrentFileSystem.getIncompleteFiles().get(0);
            incompleteFile1.delete();
            incompleteFile1.deleteOnExit();

            File completeFile1 = torrentFileSystem.getFiles().get(0);
            completeFile1.delete();
            completeFile1.deleteOnExit();

            downloader.startDownload();
            finishDownload(downloader);

            assertDownload("8055d620ba0c507c1af957b43648c99f", completeFile1, 44425);
        } finally {
            if (rootFile != null) {
                FileUtils.deleteRecursive(rootFile);
            }
        }
    }
    
    /**
     * This test has a peer and a bad webseed address. 
     * The bad address should be ignored and the download will happen from the peer. 
     */
    public void testSingleBadWebSeedSingleFilePeers() throws Exception {
        File torrentFile = createFile("test-single-badwebseed-single-file-peer.torrent");

        BTDownloaderImpl downloader = createBTDownloader(torrentFile);
        TorrentContext torrentContext = downloader.getTorrentContext();
        TorrentFileSystem torrentFileSystem = torrentContext.getFileSystem();

        File rootFile = torrentFileSystem.getCompleteFile();
        try {
            FileUtils.deleteRecursive(rootFile);
            rootFile.deleteOnExit();

            File incompleteFile1 = torrentFileSystem.getIncompleteFiles().get(0);
            incompleteFile1.delete();
            incompleteFile1.deleteOnExit();

            File completeFile1 = torrentFileSystem.getFiles().get(0);
            completeFile1.delete();
            completeFile1.deleteOnExit();

            downloader.startDownload();
            finishDownload(downloader);

            assertDownload("8055d620ba0c507c1af957b43648c99f", completeFile1, 44425);
        } finally {
            if (rootFile != null) {
                FileUtils.deleteRecursive(rootFile);
            }
        }
    }

    private File createFile(String fileName) {
        String torrentfilePath = TORRENT_DIR.getAbsolutePath() + "/" + fileName;
        File torrentFile = new File(torrentfilePath);
        return torrentFile;
    }

    private BTDownloaderImpl createBTDownloader(File torrentFile) throws IOException {
        AssertComparisons.assertTrue(torrentFile.exists());
        Injector injector = Guice.createInjector(Stage.PRODUCTION, new LimeWireCoreModule(
                ActivityCallbackAdapter.class));
        final BTMetaInfo metaInfo  = injector.getInstance(BTMetaInfoFactory.class).createMetaInfo(torrentFile);

        CoreDownloaderFactory coreDownloaderFactory = injector
                .getInstance(CoreDownloaderFactory.class);
        BTDownloaderImpl downloader = (BTDownloaderImpl) coreDownloaderFactory
                .createBTDownloader(metaInfo);
        downloader.initBtMetaInfo(metaInfo);
        return downloader;
    }

    private void finishDownload(BTDownloader downloader) throws InterruptedException {
        int maxIterations = 100;
        int index = 0;
        while (!downloader.isCompleted()) {
            if (index++ > maxIterations) {
                AssertComparisons.fail("Failure downloading the file. Taking too long.");
            }
            Thread.sleep(1000);
        }
    }

    /**
     * Asserts that the given file has the correct size, and matches the given
     * md5sum.
     */
    private void assertDownload(String md5, File file, long fileSize) throws InterruptedException,
            NoSuchAlgorithmException, IOException {
        long sleepTime = (long) ((fileSize * 0.0001) + 3000);
        Thread.sleep(sleepTime);
        AssertComparisons.assertTrue(file.exists());
        AssertComparisons.assertEquals(fileSize, file.length());
        String testmd5 = FileUtils.getMD5(file);
        AssertComparisons.assertEquals(md5, testmd5);
    }
}
