package com.limegroup.bittorrent;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.limewire.core.settings.BittorrentSettings;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.SharingSettings;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.inject.GuiceUtils;
import org.limewire.listener.EventListener;
import org.limewire.util.AssertComparisons;
import org.limewire.util.FileUtils;
import org.limewire.util.TestUtils;
import org.limewire.bittorrent.TorrentManager;
import org.limewire.bittorrent.TorrentEvent;
import org.limewire.inspection.InspectionUtils;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.limegroup.gnutella.ActivityCallbackAdapter;
import com.limegroup.gnutella.Downloader.DownloadState;
import com.limegroup.gnutella.LimeWireCoreModule;
import com.limegroup.gnutella.downloader.CoreDownloaderFactory;
import com.limegroup.gnutella.downloader.DownloadStateEvent;

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
    private File torrentDir = null;

    /**
     * A directory containing the download data for this unit test.
     */
    private File fileDir = null;

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
        torrentDir = TestUtils
                .extractResourceDirectory("org/limewire/swarm/bittorrent/public_html/torrents");
        fileDir = TestUtils.extractResourceDirectory("org/limewire/swarm/bittorrent/public_html");

        localIsPrivateBackup = ConnectionSettings.LOCAL_IS_PRIVATE.getValue();
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        forceIPAddressBackup = ConnectionSettings.FORCE_IP_ADDRESS.getValue();
        ConnectionSettings.FORCE_IP_ADDRESS.setValue(true);
        forceIPAddressStringBackup = ConnectionSettings.FORCED_IP_ADDRESS_STRING.get();
        ConnectionSettings.FORCED_IP_ADDRESS_STRING.set("127.0.0.1");
        BittorrentSettings.LIBTORRENT_ENABLED.set(true);
        fileServer = new FileServer(TEST_PORT, fileDir);
        fileServer.start();
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(localIsPrivateBackup);
        ConnectionSettings.FORCE_IP_ADDRESS.setValue(forceIPAddressBackup);
        ConnectionSettings.FORCED_IP_ADDRESS_STRING.set(forceIPAddressStringBackup);
        BittorrentSettings.LIBTORRENT_ENABLED.set(false);
        fileServer.stop();
        fileServer.destroy();
        FileUtils.deleteRecursive(torrentDir);
        FileUtils.deleteRecursive(fileDir);
        super.tearDown();
    }
    
    public void testInspections() throws Exception {
        File torrentFile = createFile("test-peer-dl-single-file.torrent");

        Injector injector = Guice.createInjector(Stage.DEVELOPMENT, new LimeWireCoreModule(
                ActivityCallbackAdapter.class));
        GuiceUtils.loadEagerSingletons(injector);
        injector.getInstance(TorrentManager.class).isValid();
        BTDownloaderImpl downloader = createBTDownloader(torrentFile, injector);
        try {
            // TODO more InspectionTool code into common so it can generate the mapping
            assertEquals("0", InspectionUtils.inspectValue("com.limegroup.bittorrent.BTDownloaderImpl:torrentsStarted", injector, true));
            assertEquals("0", InspectionUtils.inspectValue("com.limegroup.bittorrent.BTDownloaderImpl:torrentsFinished", injector, true));
            //downloader.startDownload();
            downloader.handleEvent(TorrentEvent.STARTED);
            Thread.sleep(500);
            assertEquals("1", InspectionUtils.inspectValue("com.limegroup.bittorrent.BTDownloaderImpl:torrentsStarted", injector, true));
//            downloader.handleEvent(TorrentEvent.COMPLETED);
//            Thread.sleep(500);
//            assertEquals("1", InspectionUtils.inspectValue("com.limegroup.bittorrent.BTDownloaderImpl:torrentsFinished", injector, true));
        } finally {
            cleanup(downloader);    
        }
    }

    /**
     * This test tries to download a single file torrent from a tracker/peer
     * setup on the www.limewire.org server.
     */
    public void testSingleFilePeer() throws Exception {
        File torrentFile = createFile("test-peer-dl-single-file.torrent");

        BTDownloaderImpl downloader = createBTDownloader(torrentFile);

        File completeFile = downloader.getSaveFile();
        try {
            downloader.startDownload();
            finishDownload(downloader);

            assertDownload("8055d620ba0c507c1af957b43648c99f", completeFile, 44425);
        } finally {
            cleanup(downloader);
        }
    }

    /**
     * This test tries to download a multi file torrent from a tracker/peer
     * setup on the www.limewire.org server.
     */
    public void testMultipleFilePeer() throws Exception {
        File torrentFile = createFile("test-peer-dl-multiple-file.torrent");

        BTDownloaderImpl downloader = createBTDownloader(torrentFile);

        try {
            File completeFile1 = downloader.getCompleteFiles().get(0);
            File completeFile2 = downloader.getCompleteFiles().get(1);

            downloader.startDownload();
            finishDownload(downloader);

            assertDownload("8055d620ba0c507c1af957b43648c99f", completeFile1, 44425);
            assertDownload("db1dc452e77d30ce14acca6bac8c66bc", completeFile2, 411090);
        } finally {
            cleanup(downloader);
        }
    }

    public void testSingleWebSeedSingleFileNoPeer() throws Exception {
        File torrentFile = createFile("test-single-webseed-single-file-no-peer.torrent");

        BTDownloaderImpl downloader = createBTDownloader(torrentFile);
        File completeFile = downloader.getSaveFile();
        try {
            downloader.startDownload();
            finishDownload(downloader);
            assertDownload("8055d620ba0c507c1af957b43648c99f", completeFile, 44425);
        } finally {
            cleanup(downloader);
        }
    }

    public void testMultiWebSeedSingleFileNoPeer() throws Exception {
        File torrentFile = createFile("test-multiple-webseed-single-file-no-peer.torrent");

        BTDownloaderImpl downloader = createBTDownloader(torrentFile);
        File completeFile = downloader.getSaveFile();

        try {
            downloader.startDownload();
            finishDownload(downloader);

            assertDownload("8055d620ba0c507c1af957b43648c99f", completeFile, 44425);
        } finally {
            cleanup(downloader);
        }
    }

    public void testSingleWebSeedMultipleFileNoPeer() throws Exception {
        File torrentFile = createFile("test-single-webseed-multiple-file-no-peer.torrent");

        BTDownloaderImpl downloader = createBTDownloader(torrentFile);

        try {
            File completeFile1 = downloader.getCompleteFiles().get(0);
            File completeFile2 = downloader.getCompleteFiles().get(1);

            downloader.startDownload();
            finishDownload(downloader);

            assertDownload("8055d620ba0c507c1af957b43648c99f", completeFile1, 44425);
            assertDownload("db1dc452e77d30ce14acca6bac8c66bc", completeFile2, 411090);
        } finally {
            cleanup(downloader);
        }
    }

    public void testMultiWebSeedMultipleFileNoPeer() throws Exception {
        File torrentFile = createFile("test-multiple-webseed-multiple-file-no-peer.torrent");

        BTDownloaderImpl downloader = createBTDownloader(torrentFile);

        try {
            File completeFile1 = downloader.getCompleteFiles().get(0);
            File completeFile2 = downloader.getCompleteFiles().get(1);

            downloader.startDownload();
            finishDownload(downloader);

            assertDownload("8055d620ba0c507c1af957b43648c99f", completeFile1, 44425);
            assertDownload("db1dc452e77d30ce14acca6bac8c66bc", completeFile2, 411090);
        } finally {
            cleanup(downloader);
        }
    }

    public void testSingleWebSeedSingleFilePeers() throws Exception {
        // TODO force peers and webseed to not have all pieces
        // only when used together have all the pieces

        File torrentFile = createFile("test-single-webseed-single-file-peer.torrent");

        BTDownloaderImpl downloader = createBTDownloader(torrentFile);

        try {
            File completeFile1 = downloader.getCompleteFiles().get(0);

            downloader.startDownload();
            finishDownload(downloader);

            assertDownload("8055d620ba0c507c1af957b43648c99f", completeFile1, 44425);
        } finally {
            cleanup(downloader);
        }
    }

    private File createFile(String fileName) {
        String torrentfilePath = torrentDir.getAbsolutePath() + "/" + fileName;
        File torrentFile = new File(torrentfilePath);
        return torrentFile;
    }
    
    private BTDownloaderImpl createBTDownloader(File torrentFile) throws IOException {
        Injector injector = Guice.createInjector(Stage.DEVELOPMENT, new LimeWireCoreModule(
                ActivityCallbackAdapter.class));
        GuiceUtils.loadEagerSingletons(injector);
        return createBTDownloader(torrentFile, injector);
    }

    private BTDownloaderImpl createBTDownloader(File torrentFile, Injector injector) throws IOException {
        AssertComparisons.assertTrue(torrentFile.exists());        

        CoreDownloaderFactory coreDownloaderFactory = injector
                .getInstance(CoreDownloaderFactory.class);
        BTDownloaderImpl downloader = (BTDownloaderImpl) coreDownloaderFactory
                .createBTDownloader(torrentFile, SharingSettings.getSaveDirectory());
        
        File incompleteFile = downloader.getIncompleteFile();
        FileUtils.deleteRecursive(incompleteFile);
        
        File completeFile = downloader.getSaveFile();
        FileUtils.deleteRecursive(completeFile);
        
        downloader.registerTorrentWithTorrentManager();
        return downloader;
    }

    private void finishDownload(BTDownloader downloader) throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);

        downloader.addListener(new EventListener<DownloadStateEvent>() {
           @Override
            public void handleEvent(DownloadStateEvent event) {
               if(DownloadState.COMPLETE == event.getType()) {
                   countDownLatch.countDown();
               }
            } 
        });
        
        if(downloader.getState() != DownloadState.COMPLETE) {
            assertTrue("Time ran out before download completed.", countDownLatch.await(100, TimeUnit.SECONDS));
        }
        assertEquals(DownloadState.COMPLETE, downloader.getState());
    }

    /**
     * Asserts that the given file has the correct size, and matches the given
     * md5sum.
     */
    private void assertDownload(String md5, File file, long fileSize) throws InterruptedException,
            NoSuchAlgorithmException, IOException {
        AssertComparisons.assertTrue(file.exists());
        AssertComparisons.assertEquals(fileSize, file.length());
        String testmd5 = FileUtils.getMD5(file);
        AssertComparisons.assertEquals(md5, testmd5);
    }
    
    private void cleanup(BTDownloaderImpl downloader) {
        downloader.finish();
        downloader.getTorrent().stop();
        
        File incompleteFile = downloader.getIncompleteFile();
        FileUtils.deleteRecursive(incompleteFile);
        
        File completeFile = downloader.getSaveFile();
        FileUtils.deleteRecursive(completeFile);
    }
}
