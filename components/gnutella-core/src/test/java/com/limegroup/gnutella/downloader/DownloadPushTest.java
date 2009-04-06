package com.limegroup.gnutella.downloader;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Test;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.DownloadSettings;
import org.limewire.core.settings.FilterSettings;
import org.limewire.core.settings.SpeedConstants;
import org.limewire.gnutella.tests.NetworkManagerStub;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.io.IpPortSet;
import org.limewire.rudp.RUDPUtils;

import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.PushEndpointCache;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.AlternateLocationCollection;
import com.limegroup.gnutella.altlocs.PushAltLoc;

public class DownloadPushTest extends DownloadTestCase {

    /* ports for the various push proxies */
    private final int PPORT_1 = 10001;

    private final int PPORT_2 = 10002;

    private final int PPORT_3 = 10003;

    private TestUDPAcceptorFactoryImpl testUDPAcceptorFactoryImpl;

    public DownloadPushTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(DownloadPushTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.set(new String[]{"127.*.*.*",
                "1.1.1.1","1.2.3.4","6.7.8.9"});
        super.setUp();
        testUDPAcceptorFactoryImpl = injector.getInstance(TestUDPAcceptorFactoryImpl.class);
    }

    public void testSimplePushDownload() throws Exception {
        int successfulPushes = ((AtomicInteger)((Map)statsTracker.inspect()).get("push connect success")).intValue();
        LOG.info("-Testing non-swarmed push download");

        GUID guid = new GUID();
        AlternateLocation pushLoc = alternateLocationFactory.create(guid.toHexString()
                + ";127.0.0.1:" + PPORT_1, TestFile.hash());
        ((PushAltLoc) pushLoc).updateProxies(true);

        RemoteFileDesc rfd = newRFDPush(guid, PPORT_1, 1);

        assertTrue(rfd.getAddress() instanceof PushEndpoint);

        RemoteFileDesc[] rfds = { rfd };
        TestUploader uploader = injector.getInstance(TestUploader.class);
        uploader.start("push uploader");
        testUDPAcceptorFactoryImpl.createTestUDPAcceptor(PPORT_1, networkManager.getPort(), savedFile.getName(), uploader, guid, _currentTestName);

        tGeneric(rfds);
        assertEquals(successfulPushes + 1, ((AtomicInteger)((Map)statsTracker.inspect()).get("push connect success")).intValue());        
    }

    public void testSimpleSwarmPush() throws Exception {
        LOG.info("-Testing swarming from two sources, one push...");

        GUID guid = new GUID();
        RemoteFileDesc rfd1 = newRFDWithURN(PORTS[0], false);
        AlternateLocation pushLoc = alternateLocationFactory.create(guid.toHexString()
                + ";127.0.0.1:" + PPORT_2, TestFile.hash());
        ((PushAltLoc) pushLoc).updateProxies(true);
        RemoteFileDesc rfd2 = pushLoc.createRemoteFileDesc(TestFile.length(), remoteFileDescFactory);

        TestUploader uploader = injector.getInstance(TestUploader.class);
        uploader.start("push uploader");
        uploader.setRate(100);
        testUploaders[0].setRate(100);

        RemoteFileDesc[] rfds = { rfd1, rfd2 };

        testUDPAcceptorFactoryImpl.createTestUDPAcceptor(PPORT_2, networkManager.getPort(), savedFile.getName(), uploader, guid, _currentTestName);

        tGeneric(rfds);

        assertLessThan("u1 did all the work", TestFile.length(), testUploaders[0]
                .fullRequestsUploaded());

        assertGreaterThan("pusher did all the work ", 0, testUploaders[0].fullRequestsUploaded());
    }

    /**
     * tests a generic swarm from a lot of sources with thex.  Meant to be run repetitevely
     * to find weird scheduling issues
     */
    public void testBigSwarm() throws Exception {
        LOG.info(" Testing swarming from many sources");

        int capacity = ConnectionSettings.CONNECTION_SPEED.getValue();
        ConnectionSettings.CONNECTION_SPEED.setValue(SpeedConstants.T3_SPEED_INT);
        final int RATE = 10; // slow to allow swarming
        RemoteFileDesc rfd1 = newRFDWithURN(PORTS[0], false);
        RemoteFileDesc rfd2 = newRFDWithURN(PORTS[1], false);
        RemoteFileDesc rfd3 = newRFDWithURN(PORTS[2], false);
        RemoteFileDesc rfd4 = newRFDWithURN(PORTS[3], false);
        RemoteFileDesc rfd5 = newRFDWithURN(PORTS[4], false);
        GUID guid1 = new GUID();
        GUID guid2 = new GUID();
        GUID guid3 = new GUID();
        RemoteFileDesc pushRFD1 = newRFDPush(guid1, PPORT_1, 1);
        RemoteFileDesc pushRFD2 = newRFDPush(guid2, PPORT_2, 2);
        RemoteFileDesc pushRFD3 = newRFDPush(guid3, PPORT_3, 3);

        TestUploader first = injector.getInstance(TestUploader.class);
        first.start("first pusher");
        TestUploader second = injector.getInstance(TestUploader.class);
        second.start("second pusher");
        TestUploader third = injector.getInstance(TestUploader.class);
        third.start("third pusher");

        testUDPAcceptorFactoryImpl.createTestUDPAcceptor(PPORT_1, networkManager.getPort(), savedFile.getName(), first, guid1, _currentTestName);
        testUDPAcceptorFactoryImpl.createTestUDPAcceptor(PPORT_2, networkManager.getPort(), savedFile.getName(), second, guid2, _currentTestName);
        testUDPAcceptorFactoryImpl.createTestUDPAcceptor(PPORT_3, networkManager.getPort(), savedFile.getName(), third, guid3, _currentTestName);

        testUploaders[0].setRate(RATE);
        testUploaders[1].setRate(RATE);
        testUploaders[2].setRate(RATE);
        testUploaders[3].setRate(RATE);
        testUploaders[4].setRate(RATE);
        first.setRate(RATE);
        second.setRate(RATE);
        third.setRate(RATE);

        testUploaders[0].setSendThexTreeHeader(true);
        testUploaders[0].setSendThexTree(true);

        RemoteFileDesc[] rfds = new RemoteFileDesc[] { rfd1, rfd2, rfd3, rfd4, rfd5, pushRFD1,
                pushRFD2, pushRFD3, };

        tGeneric(rfds);

        // no assesrtions really - just test completion and observe behavior in logs

        ConnectionSettings.CONNECTION_SPEED.setValue(capacity);

    }

    /**
     * tests that an uploader will pass a push loc which will be included in the swarm
     */
    public void testUploaderPassesPushLoc() throws Exception {
        LOG.info("-Testing swarming from two sources one based on a push alt...");
        final int RATE = 500;
        testUploaders[0].setRate(RATE);
        testUploaders[0].stopAfter(800000);

        TestUploader pusher = injector.getInstance(TestUploader.class);
        pusher.start("push uploader");
        pusher.setRate(RATE);

        GUID guid = new GUID();
        AlternateLocation pushLoc = alternateLocationFactory.create(guid.toHexString()
                + ";127.0.0.1:" + PPORT_1, TestFile.hash());

        AlternateLocationCollection<AlternateLocation> alCol = AlternateLocationCollection
                .create(TestFile.hash());
        alCol.add(pushLoc);

        testUploaders[0].setGoodAlternateLocations(alCol);

        RemoteFileDesc rfd = newRFDWithURN(PORTS[0], TestFile.hash().toString(), false);

        RemoteFileDesc[] rfds = { rfd };

        testUDPAcceptorFactoryImpl.createTestUDPAcceptor(PPORT_1, networkManager.getPort(), savedFile.getName(), pusher, guid, _currentTestName);

        tGeneric(rfds);

        assertGreaterThan("u1 didn't do enough work ", 100 * 1024, testUploaders[0]
                .fullRequestsUploaded());
        assertGreaterThan("pusher didn't do enough work ", 100 * 1024, pusher
                .fullRequestsUploaded());
    }

    /**
     * tests that a push uploader passes push loc and the new push loc receives
     * the first uploader as an altloc.
     */
    public void testPushUploaderPassesPushLoc() throws Exception {
        LOG.info("Test push uploader passes push loc");
        final int RATE = 500;

        TestUploader first = injector.getInstance(TestUploader.class);
        first.start("first pusher");
        first.setRate(RATE / 3);
        first.stopAfter(700000);

        TestUploader second = injector.getInstance(TestUploader.class);
        second.start("second pusher");
        second.setRate(RATE);
        second.stopAfter(700000);
        second.setInterestedInFalts(true);

        GUID guid = new GUID();
        GUID guid2 = new GUID(GUID.makeGuid());

        AlternateLocation firstLoc = alternateLocationFactory.create(guid.toHexString()
                + ";127.0.0.2:" + PPORT_1, TestFile.hash());

        AlternateLocation pushLoc = alternateLocationFactory.create(guid2.toHexString()
                + ";127.0.0.2:" + PPORT_2, TestFile.hash());

        AlternateLocationCollection<AlternateLocation> alCol = AlternateLocationCollection
                .create(TestFile.hash());
        alCol.add(pushLoc);

        first.setGoodAlternateLocations(alCol);

        testUDPAcceptorFactoryImpl.createTestUDPAcceptor(PPORT_1, networkManager.getPort(), savedFile.getName(), first, guid, _currentTestName);
        testUDPAcceptorFactoryImpl.createTestUDPAcceptor(PPORT_2, networkManager.getPort(), savedFile.getName(), second, guid2, _currentTestName);

        RemoteFileDesc[] rfd = { newRFDPush(guid, PPORT_1, 1, 2) };

        tGeneric(rfd);

        assertGreaterThan("first pusher did no work", 100000, first.fullRequestsUploaded());
        assertGreaterThan("second pusher did no work", 100000, second.fullRequestsUploaded());

        assertEquals(1, second.getIncomingGoodAltLocs().size());

        assertTrue("interested uploader didn't get first loc", second.getIncomingGoodAltLocs()
                .contains(firstLoc));
    }

    /**
     * tests that a download from a push location becomes an alternate location.
     * 
     * It creates a push uploader from which we must create a PushLoc.  
     * After a while, two open uploaders join the swarm  -one which is interested 
     * in receiving push locs and one which isn't.  The interested one should
     * receive the push loc, the other one should not.
     */
    public void testPusherBecomesPushLocAndSentToInterested() throws Exception {
        LOG.info("-Testing push download creating a push location...");
        final int RATE = 200;
        testUploaders[0].setRate(RATE);
        testUploaders[0].setInterestedInFalts(true);
        testUploaders[0].stopAfter(600000);
        testUploaders[1].setRate(RATE);
        testUploaders[1].setInterestedInFalts(false);
        testUploaders[1].stopAfter(300000);

        TestUploader pusher = injector.getInstance(TestUploader.class);
        pusher.start("push uploader");
        pusher.setRate(RATE);
        pusher.stopAfter(200000);

        GUID guid = new GUID();
        AlternateLocation pushLoc = alternateLocationFactory.create(guid.toHexString()
                + ";127.0.0.2:" + PPORT_1, TestFile.hash());

        RemoteFileDesc pushRFD = newRFDPush(guid, PPORT_1, 1, 2);

        PushEndpoint pushEndpoint = (PushEndpoint) pushRFD.getAddress();
        assertEquals(0, pushEndpoint.getFWTVersion());

        RemoteFileDesc openRFD1 = newRFDWithURN(PORTS[0], TestFile.hash().toString(), false);
        RemoteFileDesc openRFD2 = newRFDWithURN(PORTS[1], TestFile.hash().toString(), false);

        RemoteFileDesc[] now = { pushRFD };
        HashSet<RemoteFileDesc> later = new HashSet<RemoteFileDesc>();
        later.add(openRFD1);
        later.add(openRFD2);

        testUDPAcceptorFactoryImpl.createTestUDPAcceptor(PPORT_1, networkManager.getPort(), savedFile.getName(), pusher, guid, _currentTestName);

        ManagedDownloader download = (ManagedDownloader) downloadServices.download(now,
                RemoteFileDesc.EMPTY_LIST, null, false);
        Thread.sleep(1000);
        download.addDownload(later, false);

        waitForComplete();

        assertGreaterThan("u1 did no work", 100000, testUploaders[0].getAmountUploaded());

        assertGreaterThan("u2 did no work", 100000, testUploaders[1].getAmountUploaded());
        assertLessThan("u2 did too much work", 550 * 1024, testUploaders[1].getAmountUploaded());

        assertGreaterThan("pusher did no work", 100 * 1024, pusher.getAmountUploaded());

        List alc = testUploaders[0].getIncomingGoodAltLocs();
        assertTrue("interested uploader did not get pushloc", alc.contains(pushLoc));

        alc = testUploaders[1].getIncomingGoodAltLocs();
        assertFalse("not interested uploader got pushloc", alc.contains(pushLoc));

        alc = pusher.getIncomingGoodAltLocs();
        assertFalse("not interested uploader got pushloc", alc.contains(pushLoc));

    }

    /**
     * tests that a pushloc which we thought did not support FWT 
     * but actually does updates its status through the headers,
     * as well as that the set of push proxies is getting updated.
     * 
     * This test that the X-FWTP is parsed and the push endpoint address and port
     * are updated with the value
     */
    // problem is:
    // old code ranked as follows: GUID:C72D25808E87DE738FD57BCF51F5FF00, address: 1.1.1.1:6346,
    // proxies:{ /127.0.0.2:10002
    // }]
    // 
    // GUID:C72D25808E87DE738FD57BCF51F5FF00, address: 127.0.0.1:7498,
    // proxies:{ /1.2.3.4:5
    // /6.7.8.9:10
    //
    // new code picks:
    // GUID:7ECD430C3F5E93BB76B5CF3706C40700, address: 127.0.0.1:6346,
    // proxies:{ /127.0.0.2:10002
    //
    // GUID:7ECD430C3F5E93BB76B5CF3706C40700, address: 127.0.0.1:7498,
    // proxies:{ /1.2.3.4:5
    //    /6.7.8.9:10
    //
    // GUID:7ECD430C3F5E93BB76B5CF3706C40700, address: 127.0.0.1:7498,
    // proxies:{ /1.2.3.4:5
    //    /6.7.8.9:10
    //    /127.0.0.2:10002
    //    }]
    public void testPushLocUpdatesStatus() throws Exception {
        int successfulPushes = ((AtomicInteger)((Map)statsTracker.inspect()).get("push connect success")).intValue();
        LOG.info("testing that a push loc updates its status");
        final int RATE = 100;
        final int FWTPort = 7498;

        NetworkManagerStub uploaderNetworkManager = new NetworkManagerStub();
        uploaderNetworkManager.setAcceptedIncomingConnection(false);
        uploaderNetworkManager.setCanDoFWT(true);
        udpService.setReceiveSolicited(true);
        testUploaders[0].setRate(RATE);
        testUploaders[0].stopAfter(900000);
        testUploaders[0].setInterestedInFalts(true);

        TestUploader pusher2 = new TestUploader(uploaderNetworkManager);
        pusher2.start("firewalled pusher");
        pusher2.setRate(RATE);
        pusher2.stopAfter(200000);
        pusher2.setFirewalled(true);
        pusher2.setProxiesString("1.2.3.4:5,6.7.8.9:10");
        pusher2.setInterestedInFalts(true);
        pusher2.setFWTPort(FWTPort);
        
        GUID guid = new GUID();

        // register proxies for GUID, this will add 127.0.0.2:10002 to proxies
        PushEndpoint cachedPE = pushEndpointFactory.createPushEndpoint(guid.bytes(), new IpPortSet(new IpPortImpl("127.0.0.2", PPORT_2)));
        PushEndpointCache cache = injector.getInstance(PushEndpointCache.class);
        GUID retGuid = cache.updateProxiesFor(guid, cachedPE, true);
        assertSame(retGuid, guid);
        assertEquals(1, cachedPE.getProxies().size());

        RemoteFileDesc openRFD = newRFDWithURN(PORTS[0], false);
        RemoteFileDesc pushRFD2 = newRFDPush(guid, PPORT_2, 1, 2);
        PushEndpoint pushEndpoint = (PushEndpoint) pushRFD2.getAddress();
        assertEquals(0, pushEndpoint.getFWTVersion());

        testUDPAcceptorFactoryImpl.createTestUDPAcceptor(PPORT_2, networkManager.getPort(), savedFile.getName(), pusher2, guid, _currentTestName);

        // start download with rfd that needs udp push request
        ManagedDownloader download = (ManagedDownloader) downloadServices.download(
                new RemoteFileDesc[] { pushRFD2 }, RemoteFileDesc.EMPTY_LIST, null, false);
        Thread.sleep(2000);
        LOG.debug("adding regular downloader");
        // also download from uploader1, so it gets the proxy headers from pusher2
        download.addDownload(openRFD, false);
        waitForComplete();

        List<AlternateLocation> alc = testUploaders[0].getIncomingGoodAltLocs();
        assertEquals(1, alc.size());

        PushAltLoc pushLoc = (PushAltLoc) alc.iterator().next();

        assertEquals(RUDPUtils.VERSION, pushLoc.supportsFWTVersion());

        RemoteFileDesc readRFD = pushLoc.createRemoteFileDesc(1, remoteFileDescFactory);
        pushEndpoint = (PushEndpoint) readRFD.getAddress();
        assertTrue(pushEndpoint.getFWTVersion() > 0);
        assertEquals(pushEndpoint.getPort(), FWTPort);

        Set<IpPort> expectedProxies = new IpPortSet(new IpPortImpl("1.2.3.4:5"), new IpPortImpl("6.7.8.9:10"));
        assertEquals("expected: " + expectedProxies + ", actual: " + pushEndpoint.getProxies(), 
                expectedProxies.size(), pushEndpoint.getProxies().size());
        assertTrue(expectedProxies.containsAll(pushEndpoint.getProxies()));
        
        assertEquals(successfulPushes + 1, ((AtomicInteger)((Map)statsTracker.inspect()).get("push connect success")).intValue());
    }

    /**
     * tests that when we receive a headpong claiming that it doesn't have the file,
     * we send out an NAlt for that source
     */
    public void testHeadPongNAlts() throws Exception {

        testUploaders[0].setRate(100);
        testUploaders[1].setRate(100);

        int sleep = DownloadSettings.WORKER_INTERVAL.getValue();

        // make sure we use the ping ranker
        networkManager.setCanReceiveSolicited(true);
        assertTrue(networkManager.canReceiveSolicited());
        assertTrue(sourceRankerFactory.getAppropriateRanker() instanceof FriendsFirstSourceRanker);

        // create one source that will actually download and another one to which a headping should be sent 
        RemoteFileDesc rfd = newRFDWithURN(PORTS[0], false);
        RemoteFileDesc noFile = newRFDWithURN(PORTS[1], false);

        AlternateLocation toBeDemoted = alternateLocationFactory.create(noFile);

        // create a listener for the headping
        TestUDPAcceptor l =  testUDPAcceptorFactoryImpl.createTestUDPAcceptor(PORTS[1], _currentTestName);

        ManagedDownloaderImpl download = (ManagedDownloaderImpl) downloadServices.download(
                new RemoteFileDesc[] { rfd }, RemoteFileDesc.EMPTY_LIST, null, false);
        SourceRanker ranker = download.getCurrentSourceRanker();
        assertTrue(ranker instanceof FriendsFirstSourceRanker);
        LOG.debug("started download");

        // after a while clear the ranker and add the second host.
        Thread.sleep((int) (sleep * 1.5));
        ranker.stop();
        ranker.setMeshHandler(download);
        download.addDownload(noFile, false);

        LOG.debug("waiting for download to complete");
        waitForComplete();

        // the first downloader should have received an NAlt
        assertTrue(testUploaders[0].getIncomingBadAltLocs().contains(toBeDemoted));
        // the first uploader should have uploaded the whole file
        assertGreaterThan(0, testUploaders[0].getConnections());
        assertEquals(TestFile.length(), testUploaders[0].fullRequestsUploaded());

        // the second downloader should not be contacted
        assertEquals(0, testUploaders[1].getConnections());
        assertEquals(0, testUploaders[1].getAmountUploaded());
        // only one ping should have been sent to the second uploader
        assertEquals(1, l.pings);

        l.shutdown();
    }
    

    
    /**
     * tests that bad push locs get removed
     */
    public void testBadPushLocGetsDemotedNotAdvertised() throws Exception {
        setDownloadWaitTime(2 * DOWNLOAD_WAIT_TIME);
        LOG.info("test that bad push loc gets demoted and not advertised");
        // this test needs to go slowly so that the push attempt may time out
        final int RATE=15;
        
        testUploaders[0].setInterestedInFalts(true);
        testUploaders[1].setInterestedInFalts(true);
        testUploaders[0].setRate(RATE);
        testUploaders[1].setRate(RATE);
        testUploaders[0].stopAfter(550000);
        testUploaders[1].stopAfter(550000);
        
        GUID guid = new GUID();
        AlternateLocation badPushLoc=alternateLocationFactory.create(
                guid.toHexString()+";1.2.3.4:5",TestFile.hash());
        ((PushAltLoc)badPushLoc).updateProxies(true);
        
        AlternateLocationCollection<AlternateLocation> alc = 
            AlternateLocationCollection.create(TestFile.hash());
        
        alc.add(badPushLoc);
        
        testUploaders[0].setGoodAlternateLocations(alc);
        
        RemoteFileDesc rfd1 = newRFDWithURN(PORTS[0], false);
        RemoteFileDesc rfd2 = newRFDWithURN(PORTS[1], false);
        
        RemoteFileDesc [] rfds = {rfd1,rfd2};
        
        tGeneric(rfds);
        
        assertGreaterThan("u1 did no work",100*1024,testUploaders[0].fullRequestsUploaded());
        assertGreaterThan("u2 did no work",100*1024,testUploaders[1].fullRequestsUploaded());
        
        assertFalse("bad pushloc got advertised",
                testUploaders[1].getIncomingGoodAltLocs().contains(badPushLoc));
        assertEquals(1,testUploaders[0].getIncomingGoodAltLocs().size());
        assertTrue(testUploaders[0].getIncomingGoodAltLocs().contains(alternateLocationFactory.create(rfd2)));
        
        assertEquals(1,testUploaders[0].getIncomingBadAltLocs().size());
        AlternateLocation current = testUploaders[0].getIncomingBadAltLocs().get(0);        
        assertTrue(current instanceof PushAltLoc);
        PushAltLoc pcurrent = (PushAltLoc)current;
        assertEquals(guid.bytes(), pcurrent.getPushAddress().getClientGUID());
        
        // Now get the PE from our cache and make sure no proxies exist & its demoted.
        PushEndpoint pe = injector.getInstance(PushEndpointCache.class).getPushEndpoint(guid);
        assertTrue("pe: " + pe, pe.getProxies().isEmpty());
        assertTrue("pe: " + pe, badPushLoc.isDemoted());
        
    }
}
