package com.limegroup.bittorrent.swarm;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.limewire.collection.Range;
import org.limewire.gnutella.tests.ActivityCallbackStub;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.http.reactor.LimeConnectingIOReactorFactory;
import org.limewire.swarm.SwarmCoordinator;
import org.limewire.swarm.SwarmCoordinatorListener;
import org.limewire.swarm.SwarmFileSystem;
import org.limewire.swarm.SwarmSourceType;
import org.limewire.swarm.Swarmer;
import org.limewire.swarm.http.SwarmHttpSource;
import org.limewire.swarm.http.SwarmHttpSourceDownloader;
import org.limewire.swarm.impl.EchoSwarmCoordinatorListener;
import org.limewire.swarm.impl.SwarmerImpl;
import org.limewire.util.AssertComparisons;
import org.limewire.util.FileUtils;
import org.limewire.util.TestUtils;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.limegroup.bittorrent.BTContext;
import com.limegroup.bittorrent.BTMetaInfo;
import com.limegroup.bittorrent.BTMetaInfoFactory;
import com.limegroup.bittorrent.FileServer;
import com.limegroup.bittorrent.TorrentContext;
import com.limegroup.bittorrent.TorrentFileSystem;
import com.limegroup.bittorrent.disk.DiskManagerFactory;
import com.limegroup.bittorrent.disk.LoggingDiskListener;
import com.limegroup.bittorrent.disk.TorrentDiskManager;
import com.limegroup.bittorrent.handshaking.piecestrategy.LargestGapStartPieceStrategy;
import com.limegroup.bittorrent.handshaking.piecestrategy.PieceStrategy;
import com.limegroup.bittorrent.handshaking.piecestrategy.RandomGapStrategy;
import com.limegroup.bittorrent.handshaking.piecestrategy.RandomPieceStrategy;
import com.limegroup.gnutella.LimeWireCoreModule;

public class BTSwarmCoordinatorTest extends LimeTestCase {

    private static final int TEST_PORT = 8080;

    /**
     * A directory containing the torrent data for this unit test.
     */
    private File torrentDir = null;

    /**
     * A directory containing the download data for this unit test.
     */
    private File fileDir = null;
    

    private FileServer fileServer = null;

    private Injector injector;

    private BTMetaInfoFactory metaInfoFactory;

    public BTSwarmCoordinatorTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(BTSwarmCoordinatorTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        torrentDir = TestUtils
        .extractResourceDirectory("org/limewire/swarm/bittorrent/public_html/torrents");
        fileDir = TestUtils.extractResourceDirectory("org/limewire/swarm/bittorrent/public_html");
        
        injector = Guice.createInjector(new LimeWireCoreModule(ActivityCallbackStub.class));
        metaInfoFactory = injector.getInstance(BTMetaInfoFactory.class);
        fileServer = new FileServer(TEST_PORT, fileDir);
        fileServer.start();
    }

    @Override
    protected void tearDown() throws Exception {
        fileServer.stop();
        fileServer.destroy();
        FileUtils.deleteRecursive(torrentDir);
        FileUtils.deleteRecursive(fileDir);
    }

    public void testSingleFileTorrent() throws Exception {

        File torrentFile = getFile("test-single-webseed-single-file-no-peer.torrent");

        final BTMetaInfo metaInfo = metaInfoFactory.createMetaInfo(torrentFile);
        final TorrentContext torrentContext = new BTContext(metaInfo, new DiskManagerFactory());
        TorrentFileSystem torrentFileSystem = torrentContext.getFileSystem();
        File completeFile = torrentFileSystem.getCompleteFile();
        completeFile.delete();
        File downloadedFile = torrentFileSystem.getIncompleteFiles().get(0);
        downloadedFile.delete();
        completeFile.deleteOnExit();
        downloadedFile.deleteOnExit();
        final Swarmer swarmer = createSwarmer(torrentContext, null);

        swarmer.start();
        long totalSize = torrentFileSystem.getTotalSize();
        URI uri = metaInfo.getWebSeeds()[0];
        swarmer.addSource(new SwarmHttpSource(uri, totalSize));

        assertDownload(swarmer, "8055d620ba0c507c1af957b43648c99f", downloadedFile, 44425);

    }

    public void testMultiFileTorrentDefaultPieceStrategy() throws Exception {

        File torrentFile = getFile("test-single-webseed-multiple-file-no-peer.torrent");

        final BTMetaInfo metaInfo = metaInfoFactory.createMetaInfo(torrentFile);
        final TorrentContext torrentContext = new BTContext(metaInfo, new DiskManagerFactory());
        TorrentFileSystem torrentFileSystem = torrentContext.getFileSystem();
        File completeFile = torrentFileSystem.getCompleteFile();
        completeFile.delete();
        File downloadedFile1 = torrentFileSystem.getIncompleteFiles().get(0);
        File downloadedFile2 = torrentFileSystem.getIncompleteFiles().get(1);
        downloadedFile1.delete();
        downloadedFile2.delete();
        completeFile.deleteOnExit();
        downloadedFile1.deleteOnExit();
        downloadedFile2.deleteOnExit();
        final Swarmer swarmer = createSwarmer(torrentContext, null);

        swarmer.start();

        long totalSize = torrentFileSystem.getTotalSize();
        URI uri = metaInfo.getWebSeeds()[0];
        swarmer.addSource(new SwarmHttpSource(uri, totalSize));

        assertDownload(swarmer, "8055d620ba0c507c1af957b43648c99f", downloadedFile1, 44425);
        assertDownload(swarmer, "db1dc452e77d30ce14acca6bac8c66bc", downloadedFile2, 411090);

    }

    public void testMultiFileTorrentRandomGapStrategy() throws Exception {

        File torrentFile = getFile("test-single-webseed-multiple-file-no-peer.torrent");

        final BTMetaInfo metaInfo = metaInfoFactory.createMetaInfo(torrentFile);
        final TorrentContext torrentContext = new BTContext(metaInfo, new DiskManagerFactory());
        TorrentFileSystem torrentFileSystem = torrentContext.getFileSystem();
        File completeFile = torrentFileSystem.getCompleteFile();
        completeFile.delete();
        File downloadedFile1 = torrentFileSystem.getIncompleteFiles().get(0);
        File downloadedFile2 = torrentFileSystem.getIncompleteFiles().get(1);
        downloadedFile1.delete();
        downloadedFile2.delete();
        completeFile.deleteOnExit();
        downloadedFile1.deleteOnExit();
        downloadedFile2.deleteOnExit();

        final Swarmer swarmer = createSwarmer(torrentContext, new RandomGapStrategy(metaInfo));

        swarmer.start();

        long totalSize = torrentFileSystem.getTotalSize();
        URI uri = metaInfo.getWebSeeds()[0];
        swarmer.addSource(new SwarmHttpSource(uri, totalSize));

        assertDownload(swarmer, "8055d620ba0c507c1af957b43648c99f", downloadedFile1, 44425);
        assertDownload(swarmer, "db1dc452e77d30ce14acca6bac8c66bc", downloadedFile2, 411090);

    }

    public void testMultiFileTorrentLargestGapStartPieceStrategy() throws Exception {

        File torrentFile = getFile("test-single-webseed-multiple-file-no-peer.torrent");

        final BTMetaInfo metaInfo = metaInfoFactory.createMetaInfo(torrentFile);
        final TorrentContext torrentContext = new BTContext(metaInfo, new DiskManagerFactory());
        TorrentFileSystem torrentFileSystem = torrentContext.getFileSystem();
        File completeFile = torrentFileSystem.getCompleteFile();
        completeFile.delete();
        File downloadedFile1 = torrentFileSystem.getIncompleteFiles().get(0);
        File downloadedFile2 = torrentFileSystem.getIncompleteFiles().get(1);
        downloadedFile1.delete();
        downloadedFile2.delete();
        completeFile.deleteOnExit();
        downloadedFile1.deleteOnExit();
        downloadedFile2.deleteOnExit();

        final Swarmer swarmer = createSwarmer(torrentContext, new LargestGapStartPieceStrategy(
                metaInfo));

        swarmer.start();

        long totalSize = torrentFileSystem.getTotalSize();
        URI uri = metaInfo.getWebSeeds()[0];
        swarmer.addSource(new SwarmHttpSource(uri, totalSize));

        assertDownload(swarmer, "8055d620ba0c507c1af957b43648c99f", downloadedFile1, 44425);
        assertDownload(swarmer, "db1dc452e77d30ce14acca6bac8c66bc", downloadedFile2, 411090);

    }

    public void testVuzeCreatedTorrent() throws Exception {

        File torrentFile = getFile("test_vuze_getright.torrent");

        final BTMetaInfo metaInfo = metaInfoFactory.createMetaInfo(torrentFile);
        final TorrentContext torrentContext = new BTContext(metaInfo, new DiskManagerFactory());
        TorrentFileSystem torrentFileSystem = torrentContext.getFileSystem();
        File completeFile = torrentFileSystem.getCompleteFile();
        completeFile.delete();
        File downloadedFile1 = torrentFileSystem.getIncompleteFiles().get(0);

        downloadedFile1.delete();
        completeFile.deleteOnExit();
        downloadedFile1.deleteOnExit();

        final Swarmer swarmer = createSwarmer(torrentContext, new LargestGapStartPieceStrategy(
                metaInfo));

        swarmer.start();

        long totalSize = torrentFileSystem.getTotalSize();
        URI uri1 = metaInfo.getWebSeeds()[0];
        URI uri2 = metaInfo.getWebSeeds()[1];
        swarmer.addSource(new SwarmHttpSource(uri1, totalSize));
        swarmer.addSource(new SwarmHttpSource(uri2, totalSize));

        assertDownload(swarmer, "8055d620ba0c507c1af957b43648c99f", downloadedFile1, 44425);

    }

    public void testMultipleWebseedSingleFileTorrent() throws Exception {

        File torrentFile = getFile("test-multiple-webseed-single-file-no-peer.torrent");

        final BTMetaInfo metaInfo = metaInfoFactory.createMetaInfo(torrentFile);
        final TorrentContext torrentContext = new BTContext(metaInfo, new DiskManagerFactory());
        TorrentFileSystem torrentFileSystem = torrentContext.getFileSystem();
        File completeFile = torrentFileSystem.getCompleteFile();
        completeFile.delete();
        File downloadedFile1 = torrentFileSystem.getIncompleteFiles().get(0);

        downloadedFile1.delete();
        completeFile.deleteOnExit();
        downloadedFile1.deleteOnExit();

        final Swarmer swarmer = createSwarmer(torrentContext, new LargestGapStartPieceStrategy(
                metaInfo));

        swarmer.start();

        long totalSize = torrentFileSystem.getTotalSize();
        URI uri1 = metaInfo.getWebSeeds()[0];
        URI uri2 = metaInfo.getWebSeeds()[1];
        URI uri3 = metaInfo.getWebSeeds()[2];
        swarmer.addSource(new SwarmHttpSource(uri1, totalSize));
        swarmer.addSource(new SwarmHttpSource(uri2, totalSize));
        swarmer.addSource(new SwarmHttpSource(uri3, totalSize));

        assertDownload(swarmer, "8055d620ba0c507c1af957b43648c99f", downloadedFile1, 44425);

    }

    private File getFile(String fileName) {
        File torrentFile = new File(torrentDir.getAbsoluteFile() + "/" + fileName);
        return torrentFile;
    }

    private Swarmer createSwarmer(TorrentContext torrentContext, PieceStrategy pieceStrategy)
            throws IOException {

        if (pieceStrategy == null) {
            pieceStrategy = new RandomPieceStrategy(torrentContext.getMetaInfo());
        }

        TorrentFileSystem torrentFileSystem = torrentContext.getFileSystem();
        TorrentDiskManager torrentDiskManager = torrentContext.getDiskManager();
        torrentDiskManager.open(new LoggingDiskListener());

        final BTSwarmCoordinator btCoordinator = new BTSwarmCoordinator(torrentContext
                .getMetaInfo(), torrentFileSystem, torrentDiskManager, pieceStrategy);
        btCoordinator.addListener(new EchoSwarmCoordinatorListener());

        Swarmer swarmer = new SwarmerImpl(btCoordinator);
        swarmer.register(SwarmSourceType.HTTP, new SwarmHttpSourceDownloader(injector
                .getInstance(LimeConnectingIOReactorFactory.class), btCoordinator, "LimeTest/1.1"));

        return swarmer;
    }

    /**
     * Asserts that the given file has the correct size, and matches the given
     * md5sum.
     */
    private void assertDownload(Swarmer swarmer, String md5, File file, long fileSize)
            throws InterruptedException, NoSuchAlgorithmException, IOException {

        SwarmCoordinator swarmCoordinator = swarmer.getCoordinator();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        swarmCoordinator.addListener(new SwarmCoordinatorListener() {

            @Override
            public void blockLeased(SwarmCoordinator swarmCoordinator, Range block) {

            }

            @Override
            public void blockPending(SwarmCoordinator swarmCoordinator, Range block) {
            }

            @Override
            public void blockUnleased(SwarmCoordinator swarmCoordinator, Range block) {
            }

            @Override
            public void blockUnpending(SwarmCoordinator swarmCoordinator, Range block) {
            }

            @Override
            public void blockVerificationFailed(SwarmCoordinator swarmCoordinator, Range block) {

            }

            @Override
            public void blockVerified(SwarmCoordinator swarmCoordinator, Range block) {
            }

            @Override
            public void blockWritten(SwarmCoordinator swarmCoordinator, Range block) {
            }

            @Override
            public void downloadCompleted(SwarmCoordinator swarmCoordinator,
                    SwarmFileSystem fileSystem) {
                countDownLatch.countDown();
            }

        });

        countDownLatch.await(5, TimeUnit.SECONDS);

        AssertComparisons.assertTrue(file.exists());
        AssertComparisons.assertEquals(fileSize, file.length());
        String testmd5 = FileUtils.getMD5(file);
        AssertComparisons.assertEquals(md5, testmd5);
    }

    // TODO test better variety of torrent files.
    // TODO test larger torrent files.
}