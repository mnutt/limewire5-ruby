package com.limegroup.gnutella.downloader;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.settings.DHTSettings;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.GUID;
import org.limewire.nio.observer.Shutdownable;
import org.limewire.util.PrivilegedAccessor;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UrnSet;
import com.limegroup.gnutella.Downloader.DownloadStatus;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.dht.DHTEventListener;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.DHTManagerStub;
import com.limegroup.gnutella.dht.db.AltLocFinder;
import com.limegroup.gnutella.dht.db.SearchListener;
import com.limegroup.gnutella.stubs.ScheduledExecutorServiceStub;
import com.limegroup.gnutella.util.LimeTestCase;
import com.limegroup.gnutella.util.LimeWireUtils;

public class RequeryBehaviorTest extends LimeTestCase {

    private static final Log LOG = LogFactory.getLog(RequeryBehaviorTest.class);
    public RequeryBehaviorTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(RequeryBehaviorTest.class);
    }
    
    
    private RemoteFileDescFactory remoteFileDescFactory;
    private DownloadManager downloadManager;
    private MyAltLocFinder myAltFinder;
    private MyDHTManager myDHTManager;
    private Runnable pump;
    
    @Override
    public void setUp() throws Exception {
      DHTSettings.ENABLE_DHT_ALT_LOC_QUERIES.setValue(true);
      DHTSettings.MAX_DHT_ALT_LOC_QUERY_ATTEMPTS.setValue(2);
      DHTSettings.TIME_BETWEEN_DHT_ALT_LOC_QUERIES.setValue(31*1000);
        RequeryManager.TIME_BETWEEN_REQUERIES = 5000;
        
        myDHTManager = new MyDHTManager();
        Module m = new AbstractModule() {
            @Override
            protected void configure() {
                bind(DHTManager.class).toInstance(myDHTManager);
                bind(AltLocFinder.class).to(MyAltLocFinder.class);
                bind(ScheduledExecutorService.class).annotatedWith(Names.named("backgroundExecutor")).to(MyExecutor.class);
            }
        };
        Injector injector = LimeTestUtils.createInjector(m);
        remoteFileDescFactory = injector.getInstance(RemoteFileDescFactory.class);
        downloadManager = injector.getInstance(DownloadManager.class);
        downloadManager.start();
        myAltFinder = (MyAltLocFinder) injector.getInstance(AltLocFinder.class);
        MyExecutor myExecutor = (MyExecutor) injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("backgroundExecutor")));
        myExecutor.latch.await();
        assertNotNull(myExecutor.r);
        pump = myExecutor.r;
        // load up
        TestFile.length();
        RequeryManager.NO_DELAY = true;
        // TODO: fix this 
        PrivilegedAccessor.setValue(DownloadWorker.class,"NORMAL_CONNECT_TIME",100);
        PrivilegedAccessor.setValue(DownloadWorker.class,"PUSH_CONNECT_TIME",100);
    }
    
    /**
     * tests that if a result arrives and fails while we're
     * waiting for results from a gnet or dht query, we'll
     * fall back into the appropriate state.
     */
    public void testWaitingForResults() throws Exception {
        
        ManagedDownloaderImpl downloader = (ManagedDownloaderImpl)
            downloadManager.download(new RemoteFileDesc[] {fakeRFD()}, new ArrayList<RemoteFileDesc>(), new GUID() , true, new File("."), "asdf");
        LOG.debug("starting downloader");
        assertSame(DownloadStatus.QUEUED, downloader.getState());
        waitForStateToEnd(DownloadStatus.QUEUED, downloader);
        assertSame(DownloadStatus.CONNECTING, downloader.getState());
        waitForStateToEnd(DownloadStatus.CONNECTING, downloader);
        waitForStateToEnd(DownloadStatus.GAVE_UP, downloader); // quick cycle through GAVE_UP
        assertSame(DownloadStatus.WAITING_FOR_USER, downloader.getState());
        
        // click resume, launch a dht query
        LOG.debug("clicking resume");
        myDHTManager.on = true;
        downloader.resume();
        assertSame(DownloadStatus.QUEUED, downloader.getState());
        waitForStateToEnd(DownloadStatus.QUEUED, downloader);
        assertSame(DownloadStatus.CONNECTING, downloader.getState());
        waitForStateToEnd(DownloadStatus.CONNECTING, downloader);
        waitForStateToEnd(DownloadStatus.GAVE_UP, downloader);
        long dhtQueryTime = System.currentTimeMillis();
        assertSame(DownloadStatus.QUERYING_DHT, downloader.getState());
        
        // while we're querying the dht, a search result arrives
        LOG.debug("adding source");
        downloader.addDownload(fakeRFD(), false);
        waitForStateToEnd(DownloadStatus.QUERYING_DHT, downloader);
        assertSame(DownloadStatus.CONNECTING, downloader.getState());
        waitForStateToEnd(DownloadStatus.CONNECTING, downloader);
        waitForStateToEnd(DownloadStatus.GAVE_UP, downloader);
        dhtQueryTime = System.currentTimeMillis() - dhtQueryTime;
        
        // after the result fails, we should fall back in querying state
        assertSame("time spent querying dht"+dhtQueryTime,DownloadStatus.QUERYING_DHT, downloader.getState());
        
        LOG.debug("dht query fails");
        myAltFinder.listener.searchFailed();
        assertSame(DownloadStatus.GAVE_UP,downloader.getState());
        waitForStateToEnd(DownloadStatus.GAVE_UP, downloader);
        assertSame(DownloadStatus.QUEUED,downloader.getState());
        waitForStateToEnd(DownloadStatus.QUEUED, downloader);
        assertSame(DownloadStatus.CONNECTING,downloader.getState());
        waitForStateToEnd(DownloadStatus.CONNECTING, downloader);
        waitForStateToEnd(DownloadStatus.GAVE_UP, downloader);
         
        // now we should try a gnet query after a pump
        pump.run();
        Thread.sleep(100);
        assertSame(DownloadStatus.WAITING_FOR_GNET_RESULTS, downloader.getState());
        
        // another result arrives
        LOG.debug("adding another source");
        downloader.addDownload(fakeRFD(), false);
        waitForStateToEnd(DownloadStatus.WAITING_FOR_GNET_RESULTS, downloader);
        assertSame(DownloadStatus.CONNECTING, downloader.getState());
        waitForStateToEnd(DownloadStatus.CONNECTING, downloader);
        waitForStateToEnd(DownloadStatus.GAVE_UP, downloader);
        
        // should be waiting for sources again
        assertSame(DownloadStatus.WAITING_FOR_GNET_RESULTS, downloader.getState());
    }   
    
    /**
     * Tests the following scenario:
     * 1. a downloader starts and fails
     * 2. dht is off, so downloader stays in NMS state 
     * 3. dht comes on, downloader stays in NMS state
     * 5. user clicks FMS, downloader queries DHT
     * 6. DHT doesn't find anything, downloader goes to QUEUED->GNET
     * 7. gnet doesn't find anything, goes to AWS
     * 8. stays there until timeout expires
     * 8. after that happens it does a dht query.
     * 9. after the dht query expires it goes back to AWS.
     * 
     * Prior to a dht or gnet query it goes to QUEUED and very quickly to CONNECTED.
     */
    public void testBasicRequeryBehavior() throws Exception {
        
        ManagedDownloaderImpl downloader = (ManagedDownloaderImpl)
        downloadManager.download(new RemoteFileDesc[] {fakeRFD()}, new ArrayList<RemoteFileDesc>(), new GUID() , true, new File("."), "asdf");
        LOG.debug("starting downloader");
        assertSame(DownloadStatus.QUEUED, downloader.getState());
        waitForStateToEnd(DownloadStatus.QUEUED, downloader);
        assertSame(DownloadStatus.CONNECTING, downloader.getState());
        waitForStateToEnd(DownloadStatus.CONNECTING, downloader);
        waitForStateToEnd(DownloadStatus.GAVE_UP, downloader);
        assertSame(DownloadStatus.WAITING_FOR_USER, downloader.getState());
        
        LOG.debug("waiting for a few handleInactivity() calls");
        pump.run();pump.run();pump.run();
        // should still be waiting for user
        assertSame(DownloadStatus.WAITING_FOR_USER, downloader.getState());
        
        // now turn dht on 
        myDHTManager.on = true;
        
        // a few pumps go by but we're still waiting for user
        pump.run();
        assertSame(DownloadStatus.WAITING_FOR_USER, downloader.getState());
        pump.run();
        assertSame(DownloadStatus.WAITING_FOR_USER, downloader.getState());
        
        // user hits FMS
        LOG.debug("hit resume");
        downloader.resume();
        assertSame(DownloadStatus.QUEUED, downloader.getState());
        waitForStateToEnd(DownloadStatus.QUEUED, downloader);
        assertSame(DownloadStatus.CONNECTING, downloader.getState());
        waitForStateToEnd(DownloadStatus.CONNECTING, downloader);
        waitForStateToEnd(DownloadStatus.GAVE_UP, downloader);
        assertSame(DownloadStatus.QUERYING_DHT, downloader.getState());
        
        // few pumps, still querying dht
        pump.run();pump.run();pump.run();
        assertSame(DownloadStatus.QUERYING_DHT, downloader.getState());
        
        // now tell them the dht query failed
        LOG.debug("dht query fails");
        assertNotNull(myAltFinder.listener);
        myAltFinder.listener.searchFailed();
        assertSame(DownloadStatus.GAVE_UP,downloader.getState());
        waitForStateToEnd(DownloadStatus.GAVE_UP, downloader);
        assertSame(DownloadStatus.QUEUED,downloader.getState());
        waitForStateToEnd(DownloadStatus.QUEUED, downloader);
        assertSame(DownloadStatus.CONNECTING,downloader.getState());
        waitForStateToEnd(DownloadStatus.CONNECTING, downloader);
        waitForStateToEnd(DownloadStatus.GAVE_UP, downloader);
        
        // now we should try a gnet query
        assertSame(DownloadStatus.WAITING_FOR_GNET_RESULTS, downloader.getState());
        
        // wait for gnet to return fail
        waitForStateToEnd(DownloadStatus.WAITING_FOR_GNET_RESULTS, downloader);
        // should have given up
        assertSame(DownloadStatus.GAVE_UP, downloader.getState());
        
        // stays given up for a while
        pump.run();
        assertSame(DownloadStatus.GAVE_UP, downloader.getState());
        pump.run();
        assertSame(DownloadStatus.GAVE_UP, downloader.getState());
        waitForStateToEnd(DownloadStatus.GAVE_UP, downloader);
        // eventually we can query the dht again
        assertSame(DownloadStatus.QUEUED, downloader.getState());
        waitForStateToEnd(DownloadStatus.QUEUED, downloader);
        assertSame(DownloadStatus.CONNECTING, downloader.getState());
        waitForStateToEnd(DownloadStatus.CONNECTING, downloader);
        waitForStateToEnd(DownloadStatus.GAVE_UP, downloader);
        assertSame(DownloadStatus.QUERYING_DHT, downloader.getState());
        
        // lets say this query times out instead of receiving callback
        waitForStateToEnd(DownloadStatus.QUERYING_DHT, downloader);
        assertSame(DownloadStatus.GAVE_UP, downloader.getState());
        
        // back to awaiting sources
        pump.run();
        assertSame(DownloadStatus.GAVE_UP, downloader.getState());
        pump.run();
        assertSame(DownloadStatus.GAVE_UP, downloader.getState());
        
        // we stay there for good.
    }
    
    
    /**
     * Tests the following scenario:
     * 1. a downloader starts and fails
     * 2. dht is off, so downloader stays in NMS state 
     * 3. dht comes on, downloader goes looking for sources
     * 4. it doesn't find anything, goes back to NMS
     * 5. user clicks FMS, downloader goes to WFS
     * 6. FMS doesn't find anything, downloader goes to AWS
     * 7. it stays there until the timeout for a dht query elapses
     * 8. after that happens it does a dht query.
     * 
     * Prior to a dht or gnet query it goes to QUEUED and very quickly to CONNECTED.
     */
    public void testProRequeryBehavior() throws Exception {
        PrivilegedAccessor.setValue(LimeWireUtils.class,"_isPro",Boolean.TRUE);
        assertTrue(LimeWireUtils.isPro());
        
        ManagedDownloaderImpl downloader = (ManagedDownloaderImpl)
        downloadManager.download(new RemoteFileDesc[] {fakeRFD()}, new ArrayList<RemoteFileDesc>(), new GUID() , true, new File("."), "asdf");
        LOG.debug("starting downloader");
        assertSame(DownloadStatus.QUEUED, downloader.getState());
        waitForStateToEnd(DownloadStatus.QUEUED, downloader);
        assertSame(DownloadStatus.CONNECTING, downloader.getState());
        waitForStateToEnd(DownloadStatus.CONNECTING, downloader);
        waitForStateToEnd(DownloadStatus.GAVE_UP, downloader);
        assertSame(DownloadStatus.WAITING_FOR_USER, downloader.getState());
        
        LOG.debug("waiting for a few handleInactivity() calls");
        pump.run();pump.run();
        // should still be waiting for user
        assertSame(DownloadStatus.WAITING_FOR_USER, downloader.getState());
        
        // now turn dht on 
        LOG.debug("turning dht on");
        myDHTManager.on = true;
        
        // in another pump or two we should be looking for sources
        pump.run();
        Thread.sleep(100);
        assertSame(DownloadStatus.QUEUED, downloader.getState());
        waitForStateToEnd(DownloadStatus.QUEUED, downloader);
        assertSame(DownloadStatus.CONNECTING, downloader.getState());
        waitForStateToEnd(DownloadStatus.CONNECTING, downloader);
        waitForStateToEnd(DownloadStatus.GAVE_UP, downloader);
        assertSame(DownloadStatus.QUERYING_DHT, downloader.getState());
        
        // now tell them the dht query failed
        LOG.debug("dht query fails");
        assertNotNull(myAltFinder.listener);
        myAltFinder.listener.searchFailed();
        assertSame(DownloadStatus.GAVE_UP, downloader.getState());
        waitForStateToEnd(DownloadStatus.GAVE_UP, downloader);
        assertSame(DownloadStatus.QUEUED, downloader.getState());
        waitForStateToEnd(DownloadStatus.QUEUED, downloader);
        assertSame(DownloadStatus.CONNECTING, downloader.getState());
        waitForStateToEnd(DownloadStatus.CONNECTING, downloader);
        waitForStateToEnd(DownloadStatus.GAVE_UP, downloader);
        assertSame(DownloadStatus.WAITING_FOR_USER, downloader.getState());
        
        pump.run();pump.run(); // few more pumps
        // should still be waiting for user
        assertSame(DownloadStatus.WAITING_FOR_USER, downloader.getState());
        
        // hit resume()
        LOG.debug("hitting resume");
        downloader.resume();
        
        assertSame(DownloadStatus.QUEUED, downloader.getState());
        waitForStateToEnd(DownloadStatus.QUEUED, downloader);
        assertSame(DownloadStatus.CONNECTING, downloader.getState());
        waitForStateToEnd(DownloadStatus.CONNECTING, downloader); 
        waitForStateToEnd(DownloadStatus.GAVE_UP, downloader);
        assertSame(DownloadStatus.WAITING_FOR_GNET_RESULTS, downloader.getState());
        
        // if we find nothing, we give up
        waitForStateToEnd(DownloadStatus.WAITING_FOR_GNET_RESULTS, downloader);
        assertSame(DownloadStatus.GAVE_UP, downloader.getState());
        
        // we stay given up for a while but eventually launch another DHT_QUERY
        pump.run();
        Thread.sleep(100);
        pump.run();
        Thread.sleep(100);
        assertSame(DownloadStatus.GAVE_UP, downloader.getState());
        waitForStateToEnd(DownloadStatus.GAVE_UP, downloader);
        
        // we should be making another dht attempt now
        assertSame(DownloadStatus.QUEUED, downloader.getState());
        waitForStateToEnd(DownloadStatus.QUEUED, downloader);
        assertSame(DownloadStatus.CONNECTING, downloader.getState());
        waitForStateToEnd(DownloadStatus.CONNECTING, downloader);
        waitForStateToEnd(DownloadStatus.GAVE_UP, downloader);
        assertSame(DownloadStatus.QUERYING_DHT, downloader.getState());
        
    }
    
    private RemoteFileDesc fakeRFD() throws Exception {
        return remoteFileDescFactory.createRemoteFileDesc(new ConnectableImpl("0.0.0.1", (int)(Math.random() * Short.MAX_VALUE +1000), false), 13l, "badger", 1024, new byte[16],
                56, false, 4, true, null, new UrnSet(URN.createSHA1Urn("urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB")), false, "", -1);
    }
    
    
    private int waitForStateToEnd(DownloadStatus status, Downloader downloader) throws Exception {
        LOG.debug("waiting for "+status+" to end");
        int pumps = 0;
        // causes busy-loop but this is a test so its ok.
        while(downloader.getState() == status) {
            Thread.sleep(100);
            pump.run();
            pumps++;
            LOG.debug("pump "+pumps);
        }
        LOG.debug("out of "+status+" now "+downloader.getState());
        return pumps;
    }
        
    @Singleton
    private static class MyDHTManager extends DHTManagerStub {

        private volatile DHTEventListener listener;
        private volatile boolean on;
        
        @Override
        public void addEventListener(DHTEventListener listener) {
            this.listener = listener;
        }

        @Override
        public boolean isMemberOfDHT() {
            return on;
        }

        @Override
        public void removeEventListener(DHTEventListener listener) {
            if (this.listener == listener)
                this.listener = null;
        }
    }
    
    @Singleton
    private static class MyAltLocFinder implements AltLocFinder {
        private volatile SearchListener<AlternateLocation> listener;
        
        volatile boolean cancelled;
        
        public Shutdownable findAltLocs(URN urn, SearchListener<AlternateLocation> listener) {
            this.listener = listener;
            return new Shutdownable() {
                public void shutdown() {
                    cancelled = true;
                }
            };
        }

        public boolean findPushAltLocs(GUID guid, URN urn, SearchListener<AlternateLocation> listener) {
            return true;
        }
    }
    
    @Singleton
    private static class MyExecutor extends ScheduledExecutorServiceStub {
        final CountDownLatch latch = new CountDownLatch(1);
        public volatile Runnable r;
        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
            if (initialDelay == delay && delay == 1000) {
                r = command;
                latch.countDown();
            }
            return null;
        }
        
    }
}
