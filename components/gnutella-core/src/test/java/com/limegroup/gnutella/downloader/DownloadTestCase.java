package com.limegroup.gnutella.downloader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.DownloadSettings;
import org.limewire.core.settings.SharingSettings;
import org.limewire.gnutella.tests.ActivityCallbackStub;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.gnutella.tests.NetworkManagerStub;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.GUID;
import org.limewire.io.IpPortImpl;
import org.limewire.io.IpPortSet;
import org.limewire.io.LimeWireIOTestModule;
import org.limewire.io.NetworkUtils;
import org.limewire.net.SocketsManager;
import org.limewire.util.FileUtils;
import org.limewire.util.PrivilegedAccessor;
import org.limewire.util.TestUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Stage;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.limegroup.gnutella.Acceptor;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.DownloadServices;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.LifecycleManager;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.PushEndpointFactory;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.Downloader.DownloadState;
import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;
import com.limegroup.gnutella.auth.ContentManager;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.library.FileCollection;
import com.limegroup.gnutella.library.FileView;
import com.limegroup.gnutella.library.GnutellaFiles;
import com.limegroup.gnutella.library.IncompleteFileCollection;
import com.limegroup.gnutella.library.Library;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.vendor.HeadPongFactory;
import com.limegroup.gnutella.stubs.ConnectionManagerStub;
import com.limegroup.gnutella.tigertree.HashTreeCache;

public abstract class DownloadTestCase extends LimeTestCase {

    protected final Log LOG = LogFactory.getLog(getClass());

    protected static final String filePath = "com/limegroup/gnutella/downloader/DownloadTestData/";

    protected File dataDir = TestUtils.getResourceFile(filePath);

    protected File saveDir = (TestUtils.getResourceFile(filePath + "saved")).getAbsoluteFile();

    // a random name for the saved file
    protected final String savedFileName = "DownloadTester2834343.out";

    protected File savedFile;

    protected TestUploader[] testUploaders = new TestUploader[5];

    protected int[] PORTS = { 6321, 6322, 6323, 6324, 6325 };

    protected final Object COMPLETE_LOCK = new Object();

    protected boolean REMOVED = false;

    // default to waiting for 2 defaults.
    protected final long DEFAULT_WAIT_TIME = 1000 * 60 * 1;
    protected long DOWNLOAD_WAIT_TIME = DEFAULT_WAIT_TIME;

    protected boolean saveAltLocs = false;

    protected Set validAlts = null;

    protected Set invalidAlts = null;

    @Inject protected Injector injector;

    @Inject protected DownloadManager downloadManager;

    protected ActivityCallbackStub activityCallback;

    protected ManagedDownloaderImpl managedDownloader;

    @Inject protected HashTreeCache tigerTreeCache;

    @Inject protected DownloadServices downloadServices;

    @Inject protected AlternateLocationFactory alternateLocationFactory;

    protected NetworkManagerStub networkManager;

    @Inject protected UDPService udpService;

    @Inject protected PushEndpointFactory pushEndpointFactory;

    @Inject protected Acceptor acceptor;

    @Inject protected VerifyingFileFactory verifyingFileFactory;

    @Inject protected AltLocManager altLocManager;

    @Inject protected Library library;

    @Inject protected SourceRankerFactory sourceRankerFactory;

    @Inject protected ContentManager contentManager;

    @Inject protected HeadPongFactory headPongFactory;

    @Inject protected SocketsManager socketsManager;

    @Inject protected MessageFactory messageFactory;

    @Inject private LifecycleManager lifecycleManager;
    
    @Inject protected RemoteFileDescFactory remoteFileDescFactory;
    @Inject protected DownloadStatsTracker statsTracker;
    @Inject @GnutellaFiles protected FileView gnutellaFileView;
    @Inject @GnutellaFiles protected FileCollection gnutellaFileCollection;
    @Inject protected IncompleteFileCollection incompleteFileCollection;
    @Inject @Named("backgroundExecutor") ScheduledExecutorService scheduledExecutorService;

    protected DownloadTestCase(String name) {
        super(name);
    }

    protected void setDownloadWaitTime(long time) {
        DOWNLOAD_WAIT_TIME = time;
    }
    
    @Override
    protected void setUp() throws Exception {
        setDownloadWaitTime(DEFAULT_WAIT_TIME);
        // raise the download-bytes-per-sec so stealing is easier
        DownloadSettings.MAX_DOWNLOAD_BYTES_PER_SEC.setValue(10);

        activityCallback = new MyCallback();
        injector = LimeTestUtils.createInjector(Stage.PRODUCTION, new LimeWireIOTestModule(), NetworkManagerStub.MODULE, 
                new AbstractModule() {
            @Override
            protected void configure() {
                bind(ActivityCallback.class).toInstance(activityCallback);
                bind(ConnectionManager.class).to(ConnectionManagerStub.class);
            }
        }, LimeTestUtils.createModule(this));

        networkManager = (NetworkManagerStub) injector.getInstance(NetworkManager.class);
        networkManager.setAcceptedIncomingConnection(true);
        networkManager.setAddress(NetworkUtils.getLocalAddress().getAddress());
        networkManager.setTls(true);
        networkManager.setOutgoingTLSEnabled(false);
        networkManager.setIncomingTLSEnabled(true);

        ConnectionManagerStub connectionManager = (ConnectionManagerStub) injector
                .getInstance(ConnectionManager.class);
        connectionManager.setConnected(true);

        downloadManager.start();

        Runnable click = new Runnable() {
            public void run() {
                downloadManager.measureBandwidth();
            }
        };

        scheduledExecutorService.scheduleWithFixedDelay(click, 0, 1000, TimeUnit.MILLISECONDS);
        lifecycleManager.start();
        networkManager.setPort(acceptor.getPort(false));

        managedDownloader = null;

        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);

        // Don't wait for network connections for testing
        RequeryManager.NO_DELAY = true;

        for (int i = 0; i < testUploaders.length; i++) {
            testUploaders[i] = injector.getInstance(TestUploader.class);
            testUploaders[i].start("PORT_" + i, PORTS[i], false);
        }

        deleteAllFiles();

        dataDir.mkdirs();
        saveDir.mkdirs();

        SharingSettings.setSaveDirectory(saveDir);

        //Pick random name for file.
        savedFile = new File(saveDir, savedFileName);
        savedFile.delete();
        ConnectionSettings.CONNECTION_SPEED.setValue(1000);

        tigerTreeCache = injector.getInstance(HashTreeCache.class);
        tigerTreeCache.purgeTree(TestFile.hash());
    }

    @Override
    protected void tearDown() throws Exception {
        if (lifecycleManager != null) lifecycleManager.shutdown();
        
        for (int i = 0; i < testUploaders.length; i++) {
            if (testUploaders[i] != null) {
                testUploaders[i].reset();
                testUploaders[i].stopThread();
            }
        }

        deleteAllFiles();

        if (injector != null)
            injector.getInstance(
                    Key.get(ScheduledExecutorService.class, Names.named("backgroundExecutor")))
                    .shutdownNow();
        
        if (lifecycleManager != null) {
            lifecycleManager.shutdown();
        }
    }

    private void deleteAllFiles() {
        if (!dataDir.exists())
            return;

        File[] files = dataDir.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                if (files[i].getName().equalsIgnoreCase("incomplete"))
                    FileUtils.deleteRecursive(files[i]);
                else if (files[i].getName().equals(saveDir.getName()))
                    FileUtils.deleteRecursive(files[i]);
            }
        }
        dataDir.delete();
    }

    protected void tGeneric(RemoteFileDesc[] rfds) throws Exception {
        tGeneric(rfds, new RemoteFileDesc[0]);
    }

    protected void tGeneric(RemoteFileDesc[] rfds, List<? extends RemoteFileDesc> alts)
            throws Exception {
        tGeneric(rfds, null, alts);
    }

    protected void tGeneric(RemoteFileDesc[] now, RemoteFileDesc[] later) throws Exception {
        tGeneric(now, later, RemoteFileDesc.EMPTY_LIST);
    }

    /**
     * Performs a generic download of the file specified in <tt>rfds</tt>.
     */
    protected void tGeneric(RemoteFileDesc[] rfds, RemoteFileDesc[] later,
            List<? extends RemoteFileDesc> alts) throws Exception {
        Downloader download;
        download = downloadServices.download(rfds, alts, null, false);
        tGeneric(download, later, rfds);
    }
    
    protected void tGeneric(MagnetOptions magnet) throws Exception {
        Downloader download = downloadServices.download(magnet, false);
        tGeneric(download, null, null);
    }
    
    /**
     * Performs a generic download of the file specified in <tt>rfds</tt>.
     * 
     * @param later can be null
     * @param rfds can be null
     */
    protected void tGeneric(Downloader download, RemoteFileDesc[] later,
            RemoteFileDesc[] rfds) throws Exception {
        if (later != null) {
            Thread.sleep(100);
            for (int i = 0; i < later.length; i++)
                ((ManagedDownloader) download).addDownload(later[i], true);
        }

        waitForComplete();
        if (isComplete())
            LOG.debug("pass" + "\n");
        else
            fail("FAILED: complete corrupt");

        IncompleteFileManager ifm = downloadManager.getIncompleteFileManager();
        for (int i = 0; rfds != null && i < rfds.length; i++) {
            File incomplete = ifm.getFile(rfds[i]);
            VerifyingFile vf = ifm.getEntry(incomplete);
            assertNull("verifying file should be null", vf);
        }
    }

    /**
     * Performs a generic download of the file specified in <tt>rfds</tt>.
     */
    protected void tGenericCorrupt(RemoteFileDesc[] rfds, RemoteFileDesc[] later) throws Exception {
        Downloader download;

        download = downloadServices.download(rfds, false, null);
        if (later != null) {
            Thread.sleep(100);
            for (int i = 0; i < later.length; i++)
                ((ManagedDownloader) download).addDownload(later[i], true);
        }

        waitForCorrupt();
        if (isComplete())
            fail("should be corrupt");
        else
            LOG.debug("pass");

        IncompleteFileManager ifm = downloadManager.getIncompleteFileManager();
        for (int i = 0; i < rfds.length; i++) {
            File incomplete = ifm.getFile(rfds[i]);
            VerifyingFile vf = ifm.getEntry(incomplete);
            assertNull("verifying file should be null", vf);
        }
    }
    
    protected RemoteFileDesc newRFDPush(GUID guid, int port, int suffix) throws Exception {
        return newRFDPush(guid, port, suffix, 1);
    }
    
    protected RemoteFileDesc newRFDPush(GUID guid, int port, int rfdSuffix, int proxySuffix) throws Exception {
        Set<URN> urns = new HashSet<URN>();
        urns.add(TestFile.hash());
        
        PushEndpoint pe = pushEndpointFactory.createPushEndpoint(guid.bytes(),
                new IpPortSet(new IpPortImpl("127.0.0." + proxySuffix, port)), (byte) 0, (byte) 0,
                new IpPortImpl("127.0.0." + rfdSuffix, 6346));
        pe.updateProxies(true);
        
        return remoteFileDescFactory.createRemoteFileDesc(pe, 0, savedFile.getName(),
                TestFile.length(), pe.getClientGUID(), 100, 1, false, null, urns, false, "ALT", 0);
    }

    protected RemoteFileDesc newRFD(int port, boolean useTLS) throws Exception {
        return remoteFileDescFactory.createRemoteFileDesc(new ConnectableImpl("127.0.0.1", port, useTLS), 0, savedFile.getName(), TestFile.length(),
                GUID.makeGuid(), 100, 4, false, null, URN.NO_URN_SET, false, "", -1);
    }

    protected RemoteFileDesc newRFDWithURN(int port, boolean useTLS) throws Exception {
        return newRFDWithURN(port, null, useTLS);
    }

    protected RemoteFileDesc newRFDWithURN(int port, String urn, boolean useTLS) throws Exception {
        Set<URN> set = new HashSet<URN>();
        try {
            // for convenience, don't require that they pass the urn.
            // assume a null one is the TestFile's hash.
            if (urn == null)
                set.add(TestFile.hash());
            else
                set.add(URN.createSHA1Urn(urn));
        } catch (Exception e) {
            fail("SHA1 not created for: " + savedFile, e);
        }
        return remoteFileDescFactory.createRemoteFileDesc(new ConnectableImpl("127.0.0.1", port, useTLS), 0, savedFile.getName(), TestFile.length(),
                GUID.makeGuid(), 100, 4, false, null, set, false, "", -1);
    }

    /** Returns true if the complete file exists and is complete */
    protected final boolean isComplete() {
        return isComplete(savedFile, TestFile.length());
    }
    
    protected final boolean isComplete(File f, long length) {
        LOG.debug("file is " + f.getPath());
        if (f.length() < length) {
            LOG.debug("File too small by: " + (length - f.length()));
            return false;
        } else if (savedFile.length() > TestFile.length()) {
            LOG.debug("File too large by: " + (length - f.length()));
            return false;
        }
        FileInputStream stream = null;
        try {
            stream = new FileInputStream(f);
            for (int i = 0;; i++) {
                int c = stream.read();
                if (c == -1)//eof
                    break;
                if ((byte) c != TestFile.getByte(i)) {
                    LOG.debug("Bad byte at " + i + "\n");
                    return false;
                }
            }
        } catch (IOException ioe) {
            //ioe.printStackTrace();
            return false;
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ignored) {
                }
            }
        }
        return true;
    }

    protected final int CORRUPT = 1;

    protected final int COMPLETE = 2;

    protected final int INVALID = 3;

    protected void waitForComplete(boolean corrupt) {
        waitForCompleteImpl(corrupt ? CORRUPT : COMPLETE);
    }

    protected void waitForCorrupt() {
        waitForCompleteImpl(CORRUPT);
    }

    protected void waitForInvalid() {
        waitForCompleteImpl(INVALID);
    }

    protected void waitForComplete() {
        waitForCompleteImpl(COMPLETE);
    }

    protected void waitForCompleteImpl(int state) {
        synchronized (COMPLETE_LOCK) {
            try {
                REMOVED = false;
                LOG.debug("starting wait");
                COMPLETE_LOCK.wait(DOWNLOAD_WAIT_TIME);
                LOG.debug("finished waiting");
            } catch (InterruptedException e) {
                LOG.debug("interrupted", e);
                //good.
            }
        }

        if (!REMOVED) {
            downloadManager.remove(managedDownloader, false);
            fail("download did not finish, last state was: " + managedDownloader.getState());
        }

        if (state == CORRUPT)
            assertEquals("unexpected state", DownloadState.CORRUPT_FILE, managedDownloader
                    .getState());
        else if (state == INVALID)
            assertEquals("unexpected state", DownloadState.INVALID, managedDownloader.getState());
        else if (state == COMPLETE)
            assertEquals("unexpected state", DownloadState.COMPLETE, managedDownloader.getState());
        else
            fail("bad expectation: " + state);
    }

    protected void waitForBusy(Downloader downloader) {
        for (int i = 0; i < 12; i++) { //wait 12 seconds
            if (downloader.getState() == DownloadState.BUSY)
                return;
            try {
                Thread.sleep(1000);// try again after a second
            } catch (InterruptedException e) {
                fail("downloader unexpecteted interrupted", e);
                return;
            }
        }
    }

    protected final class MyCallback extends ActivityCallbackStub {

        @Override
        public void addDownload(Downloader d) {
            managedDownloader = (ManagedDownloaderImpl) d;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void removeDownload(Downloader d) {
            synchronized (COMPLETE_LOCK) {
                REMOVED = true;
                COMPLETE_LOCK.notify();
            }

            if (saveAltLocs) {
                try {
                    validAlts = new HashSet();
                    validAlts.addAll((Set) PrivilegedAccessor.getValue(d, "validAlts"));

                    invalidAlts = new HashSet();
                    invalidAlts.addAll((Set) PrivilegedAccessor.getValue(d, "invalidAlts"));
                } catch (Exception err) {
                    throw new RuntimeException(err);
                }
            }
        }
    }

}
