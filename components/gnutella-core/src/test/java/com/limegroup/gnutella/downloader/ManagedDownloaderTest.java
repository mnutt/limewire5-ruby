package com.limegroup.gnutella.downloader;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ScheduledExecutorService;

import junit.framework.Test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.collection.Range;
import org.limewire.concurrent.ListeningExecutorService;
import org.limewire.core.api.download.DownloadException;
import org.limewire.core.api.download.SaveLocationManager;
import org.limewire.core.api.download.DownloadException.ErrorCode;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.gnutella.tests.NetworkManagerStub;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.io.LocalSocketAddressProvider;
import org.limewire.io.LocalSocketAddressProviderStub;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.listener.EventListener;
import org.limewire.net.SocketsManager;
import org.limewire.net.TLSManager;
import org.limewire.util.ExecuteRunnableAction;
import org.limewire.util.FileUtils;
import org.limewire.util.OSUtils;
import org.limewire.util.PrivilegedAccessor;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.BandwidthManager;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.DownloadManagerImpl;
import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.PushEndpointCache;
import com.limegroup.gnutella.PushEndpointFactory;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.Downloader.DownloadState;
import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;
import com.limegroup.gnutella.downloader.serial.DownloadMemento;
import com.limegroup.gnutella.library.CreationTimeCache;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.FileDescStub;
import com.limegroup.gnutella.library.IncompleteFileCollection;
import com.limegroup.gnutella.library.IncompleteFileDescStub;
import com.limegroup.gnutella.library.LibraryStubModule;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.statistics.TcpBandwidthStatistics;
import com.limegroup.gnutella.stubs.ConnectionManagerStub;
import com.limegroup.gnutella.stubs.MessageRouterStub;
import com.limegroup.gnutella.tigertree.ThexReaderFactory;

public class ManagedDownloaderTest extends LimeTestCase {
    
    private static final Log LOG = LogFactory.getLog(ManagedDownloaderTest.class);
    
    private final static int PORT=6666;
    private DownloadManagerImpl downloadManager;
    @Inject private CoreDownloaderFactory gnutellaDownloaderFactory;
    private NetworkManagerStub networkManager;
    @Inject private Injector injector;
    @Inject @Named("backgroundExecutor") private ScheduledExecutorService background;
    @Inject private IncompleteFileCollection incompleteFileCollection;

    public ManagedDownloaderTest(String name) {
        super(name);
    }
    
    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public static Test suite() {
        return buildTestSuite(ManagedDownloaderTest.class);
    }
    
    @Override
    public void setUp() throws Exception {
        doSetUp();
    }
    
    private void doSetUp(Module... modules) throws Exception {
        
        List<Module> allModules = new LinkedList<Module>();
        final LocalSocketAddressProviderStub localSocketAddressProvider = new LocalSocketAddressProviderStub();
        localSocketAddressProvider.setLocalAddressPrivate(false);
        allModules.add(new AbstractModule() {
           @Override
            protected void configure() {
               bind(ConnectionManager.class).to(ConnectionManagerStub.class);
               bind(MessageRouter.class).to(MessageRouterStub.class);
               bind(NetworkManager.class).to(NetworkManagerStub.class);
               bind(LocalSocketAddressProvider.class).toInstance(localSocketAddressProvider);
            } 
        });
        allModules.add(new LibraryStubModule());
        allModules.add(LimeTestUtils.createModule(this));
        allModules.addAll(Arrays.asList(modules));
        injector = LimeTestUtils.createInjector(allModules.toArray(new Module[allModules.size()]));
        ConnectionManagerStub connectionManager = (ConnectionManagerStub)injector.getInstance(ConnectionManager.class);
        connectionManager.setConnected(true);
                
        downloadManager = (DownloadManagerImpl)injector.getInstance(DownloadManager.class);
        networkManager = (NetworkManagerStub)injector.getInstance(NetworkManager.class);
        
        downloadManager.start();
        downloadManager.scheduleWaitingPump();
        RequeryManager.NO_DELAY = false;
    }

    @Override
    protected void tearDown() throws Exception {
        background.shutdown();
    }
    
    /**
     * tests if firewalled altlocs are added to the file descriptor
     * but not sent to uploaders.  (i.e. the informMesh method)
     */
    public void testFirewalledLocs() throws Exception {
        doSetUp(new AbstractModule() {
           @Override
            protected void configure() {
               bind(HTTPDownloaderFactory.class).to(AltLocDownloaderStubFactory.class);
               bind(DownloadWorkerFactory.class).to(AltLocWorkerStubFactory.class);
            } 
        });
        networkManager.setAcceptedIncomingConnection(true);
        
    	//first make sure we are sharing an incomplete file
    	
    	URN partialURN = URN.createSHA1Urn("urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFE");
        
    	IncompleteFileDescStub partialDesc = new IncompleteFileDescStub("incomplete",partialURN,3);
    	incompleteFileCollection.add(partialDesc);
    	
    	// then create an rfd from a firewalled host
    	RemoteFileDesc rfd = newPushRFD("incomplete","urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFE",GUID.makeGuid());
    	
    	//test that currently we have no altlocs for the incomplete file
    	
    	FileDesc test = incompleteFileCollection.getFileDesc(partialURN);
    	assertNotNull(test);
    	AltLocManager altLocManager = injector.getInstance(AltLocManager.class);    	
    	assertEquals(0, altLocManager.getNumLocs(test.getSHA1Urn()));
    	
    	//add one fake downloader to the downloader list
    	Endpoint e = new Endpoint("1.2.3.5",12345);
    	RemoteFileDesc other = injector.getInstance(RemoteFileDescFactory.class).createRemoteFileDesc(newRFD("incomplete"), e);
    	HTTPDownloaderFactory httpDownloaderFactory =injector.getInstance(HTTPDownloaderFactory.class);
    	AltLocDownloaderStub fakeDownloader = (AltLocDownloaderStub)httpDownloaderFactory.create(null, new RemoteFileDescContext(other), null, false);
    	
    	ManagedDownloaderImpl md = (ManagedDownloaderImpl)gnutellaDownloaderFactory.createManagedDownloader(new RemoteFileDesc[] { rfd}, null, null, null, false);
        DownloadWorkerFactory downloadWorkerFactory = injector.getInstance(DownloadWorkerFactory.class);
        AltLocWorkerStub worker = (AltLocWorkerStub)downloadWorkerFactory.create(md, new RemoteFileDescContext(rfd), null);
        worker.setHTTPDownloader(fakeDownloader);
        
        List<DownloadWorker> l = new LinkedList<DownloadWorker>();
        l.add(worker);
    	md.initialize();
    	setDloaders(md, l);
    	setSHA1(md, partialURN);
    	setIncompleteFile(md, partialDesc.getFile());
    	
    	
    	//and see if it behaves correctly
    	md.informMesh(rfd,true);
    	
    	//make sure the downloader did not get notified
    	assertFalse(fakeDownloader._addedFailed);
    	assertFalse(fakeDownloader._addedSuccessfull);
    	
    	//the altloc should have been added to the file descriptor
    	assertEquals(1, altLocManager.getNumLocs(partialURN));
    	
    	//now repeat the test, pretending the uploader wants push altlocs
    	fakeDownloader.setWantsFalts(true);
    	
    	//and see if it behaves correctly
    	md.informMesh(rfd,true);
    	
    	//make sure the downloader did get notified
    	assertFalse(fakeDownloader._addedFailed);
    	assertTrue(fakeDownloader._addedSuccessfull);
    	
    	//make sure the file was added to the file descriptor
    	assertEquals(1, altLocManager.getNumLocs(partialURN));
    	
    	//rince and repeat, saying this was a bad altloc. 
    	//it should be sent to the other downloaders. 
    	fakeDownloader._addedSuccessfull=false;
    	
    	md.informMesh(rfd,false);
    	
    	//make sure the downloader did get notified
    	assertTrue(fakeDownloader._addedFailed);
    	assertFalse(fakeDownloader._addedSuccessfull);
    	
    	//make sure the altloc is demoted now
    	assertEquals(0, altLocManager.getNumLocs(test.getSHA1Urn()));
    }
    
    // requeries are gone now - now we only have user-driven queries (sort of
    // like requeries but not automatic.
    public void testNewRequery() throws Exception {
        LOG.info("testing new requery");
        RemoteFileDesc[] rfds={
            newRFD("Susheel_Daswani_Neil_Daswani.txt",
                   "urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB"),
            newRFD("Susheel/cool\\Daswani.txt",
                   "urn:sha1:LSTHGIPQGSSZTS5FJUPAKPZWUGYQYPFB"),
            newRFD("Sumeet (Susheel) Anurag (Daswani)Chris.txt"),
            newRFD("Susheel/cool\\Daswani.txt",
                   "urn:sha1:XXXXGIPQGXXXXS5FJUPAKPZWUGYQYPFB")   //ignored
        };
        ManagedDownloaderImpl downloader= (ManagedDownloaderImpl)gnutellaDownloaderFactory.createManagedDownloader(rfds, null, null, null, false);
        downloader.initialize();
        QueryRequest qr=downloader.newRequery();
        assertNotNull("Couldn't make query", qr);
        
        assertTrue(qr.getQuery().equals("neil daswani susheel") ||
                   qr.getQuery().equals("neil susheel daswani") ||
                   qr.getQuery().equals("daswani neil susheel") ||
                   qr.getQuery().equals("daswani susheel neil") ||
                   qr.getQuery().equals("susheel neil daswani") ||
                   qr.getQuery().equals("susheel daswani neil"));
        // minspeed mask | firewalled | xml = 224
        assertEquals(224, qr.getMinSpeed());
        // the guid should be a lime guid but not a lime requery guid
        
        assertTrue((GUID.isLimeGUID(qr.getGUID())) && 
                   !(GUID.isLimeRequeryGUID(qr.getGUID())));
        assertTrue((qr.getRichQuery() == null) || 
                   (qr.getRichQuery().equals("")));
        Set urns=qr.getQueryUrns();
        assertEquals(0, urns.size());
        
        networkManager.setCanDoFWT(true);
        qr=downloader.newRequery();
        // minspeed mask | firewalled | xml | firewall transfer = 226
        assertEquals(226, qr.getMinSpeed());
    }

    /** Catches a bug with earlier keyword intersection code. */
    public void testNewRequery2() throws Exception {
        LOG.info("testing new requery 2");
        RemoteFileDesc[] rfds={ newRFD("LimeWire again LimeWire the 34") };
        ManagedDownloaderImpl downloader= (ManagedDownloaderImpl) gnutellaDownloaderFactory.createManagedDownloader(rfds, null, null, null, false);
        downloader.initialize();
        QueryRequest qr=downloader.newRequery();
        assertEquals("limewire again", qr.getQuery());
        assertEquals(new HashSet(), qr.getQueryUrns());
    }
    
    /** Tests that the progress is retained for deserialized downloaders. */
    public void testSerializedProgress() throws Exception {
        LOG.info("test serialized progress");
        
        networkManager.setAddress(new byte[] { 127, 0, 0, 1 });
        
        IncompleteFileManager ifm = injector.getInstance(IncompleteFileManager.class);
        RemoteFileDesc rfd=newRFD("some file.txt", FileDescStub.DEFAULT_URN);
        File incompleteFile=ifm.getFile(rfd);
        int amountDownloaded=100;
        
        VerifyingFileFactory verifyingFileFactory = injector.getInstance(VerifyingFileFactory.class);
        
        VerifyingFile vf=verifyingFileFactory.createVerifyingFile(1024);
        vf.addInterval(Range.createRange(0, amountDownloaded-1));  //inclusive
        ifm.addEntry(incompleteFile, vf, true);

        //Start downloader, make it sure requeries, etc.
        ManagedDownloader downloader = 
            gnutellaDownloaderFactory.createManagedDownloader(new RemoteFileDesc[] { rfd },
                null, null, null, false);
        downloader.initialize();
        requestStart(downloader);
        try { Thread.sleep(200); } catch (InterruptedException e) { }
        //assertEquals(Downloader.WAITING_FOR_RESULTS, downloader.getState());
        assertEquals(amountDownloaded, downloader.getAmountRead());
        
        DownloadMemento memento = downloader.toMemento();
        downloader.stop();
        downloader = (ManagedDownloader)downloadManager.prepareMemento(memento);
        downloader.initialize();
        requestStart(downloader);

        //Check same state as before serialization.
        try {Thread.sleep(500);}catch(InterruptedException ignroed){}
        //assertEquals(Downloader.WAITING_FOR_RESULTS, downloader.getState());
        assertEquals(amountDownloaded, downloader.getAmountRead());
        downloader.stop();
        Thread.sleep(1000); 
    }
    
    /** Tests that the progress is not 0% when resume button is hit while
     *  requerying.  This was caused by the call to cleanup() from
     *  tryAllDownloads3() and was reported by Sam Berlin. 
     *  Changed after requeries have been shut off (requery-expunge-branch).
     */
    public void testRequeryProgress() throws Exception {
        LOG.info("test requery progress");
        TestUploader uploader=null;
        ManagedDownloader downloader=null;
    	// TestFile's static initializer does all kinds of expensive stuff
    	// that can cause the http communication to timeout when it's
    	// done lazily
    	TestFile.length();
        try {
            //Start uploader and download.
            uploader = new TestUploader(injector);
            uploader.start("ManagedDownloaderTest", PORT, false);
            uploader.stopAfter(500);
            uploader.setSendThexTreeHeader(false);
            uploader.setSendThexTree(false);
            downloader=
				gnutellaDownloaderFactory.createManagedDownloader(
                        new RemoteFileDesc[] {newRFD("another testfile.txt", FileDescStub.DEFAULT_URN)}, null, null, null, false);
        
            downloader.initialize();
            requestStart(downloader);
            Thread.sleep(1000);
            //Wait for it to download until error, need to wait 
            uploader.waitForUploaderToStop();
        
            try { Thread.sleep(65 * 1000); } catch (InterruptedException ie) { }
            // no more auto requeries - so the download should be waiting for
            // input from the user
            assertEquals("should have read 500 bytes", 500, 
                         downloader.getAmountRead());
            assertEquals("should be waiting for user",
                    DownloadState.WAITING_FOR_USER, 
                         downloader.getState());
            //Hit resume, make sure progress not erased.
            downloader.resume(); 
            try { Thread.sleep(2000); } catch (InterruptedException e) { }
            // you would think we'd expect wating for results here, but instead
            // we will wait for connections (since we have no one to query)
            assertEquals(DownloadState.WAITING_FOR_CONNECTIONS, 
                         downloader.getState());
            assertEquals(500, 
                         downloader.getAmountRead());
        } finally {
            if (uploader!=null)
                uploader.stopThread();
            if (downloader!=null)
                downloader.stop();
        }
    }

    public void testSetSaveFileExceptionPathNameTooLong() throws Exception {
        RemoteFileDesc[] rfds = new RemoteFileDesc[] { newRFD("download") };
        File saveDir = createMaximumPathLengthDirectory();
        
        try {
            gnutellaDownloaderFactory.createManagedDownloader(rfds,
                    new GUID(GUID.makeGuid()), saveDir, "does not matter", false);
            fail("No exception thrown for dir " + saveDir);
        }
        catch (DownloadException e) {
            assertEquals("Parent dir should exceed max path length",
                    ErrorCode.PATH_NAME_TOO_LONG,
                    e.getErrorCode());
        }
        finally {
            File parent = saveDir;
            while (parent.getName().startsWith("zzzzzzz")) {
                saveDir = parent;
                parent = parent.getParentFile();
            }
            FileUtils.deleteRecursive(saveDir);
        }
    }

	public void testSetSaveFileExceptions() throws Exception {
		
		RemoteFileDesc[] rfds = new RemoteFileDesc[] { newRFD("download") };
		File file = File.createTempFile("existing", "file");
		file.deleteOnExit();

		File noWritePermissionDir = null;
		if (OSUtils.isLinux()) {
			noWritePermissionDir = new File("/");
		}
		else if (OSUtils.isWindows()) {
			// doesn't work on 
//			noWritePermissionDir = new File("C:\\WINDOWS\\");
		}
		if (noWritePermissionDir != null) {
			try {
				gnutellaDownloaderFactory.createManagedDownloader(rfds,
                        new GUID(GUID.makeGuid()), noWritePermissionDir, "does not matter", false);
				fail("No exception thrown for dir " + noWritePermissionDir);
			}
			catch (DownloadException e) {
				assertEquals("Should have no write permissions",
						ErrorCode.DIRECTORY_NOT_WRITEABLE,
						e.getErrorCode());
			}
		}
        
		try {
			gnutellaDownloaderFactory.createManagedDownloader(rfds,
                    new GUID(GUID.makeGuid()), new File("/non existent directory"), null, false);
			fail("No exception thrown");
		}
		catch (DownloadException e) {
			assertEquals("Error code should be: directory does not exist",
						 ErrorCode.DIRECTORY_DOES_NOT_EXIST,
						 e.getErrorCode());
		}
		try {
			gnutellaDownloaderFactory.createManagedDownloader(rfds,
                    new GUID(GUID.makeGuid()), file.getParentFile(), file.getName(), false);
			fail("No exception thrown");
		}
		catch (DownloadException e) {
			assertEquals("Error code should be: file exists",
						 ErrorCode.FILE_ALREADY_EXISTS,
						 e.getErrorCode());
		}
		try {
			// should not throw an exception because of overwrite 
			gnutellaDownloaderFactory.createManagedDownloader(rfds,
                    new GUID(GUID.makeGuid()), file.getParentFile(), file.getName(), true); 
		}
		catch (DownloadException e) {
			fail("There shouldn't have been an exception of type " + e.getErrorCode());
		}
		try {
			File f = File.createTempFile("notadirectory", "file");
			f.deleteOnExit();
			gnutellaDownloaderFactory.createManagedDownloader(rfds,
                    new GUID(GUID.makeGuid()), f, null, false);
			fail("No exception thrown");
		}
		catch (DownloadException e) {
			assertEquals("Error code should be: file not a directory", 
						 ErrorCode.NOT_A_DIRECTORY,
						 e.getErrorCode());
		}
        // we force filename normalization
        gnutellaDownloaderFactory.createManagedDownloader(rfds, new GUID(GUID.makeGuid()), null,
                "./", false);
        gnutellaDownloaderFactory.createManagedDownloader(rfds, new GUID(GUID.makeGuid()), null,
                "../myfile.txt", false);

        // should not throw an exception
        gnutellaDownloaderFactory.createManagedDownloader(rfds, new GUID(GUID.makeGuid()), null,
                null, false);

        List<RemoteFileDesc> emptyRFDList = Collections.emptyList();
        // already downloading based on filename and same size

        downloadManager.download(rfds, emptyRFDList, new GUID(GUID.makeGuid()), false, null, null);

        try {
            downloadManager.download(rfds, emptyRFDList, new GUID(GUID.makeGuid()), false, null,
                    null);
            fail("No exception thrown");
        } catch (DownloadException e) {
            assertEquals("Error code should be: already downloading", 
					ErrorCode.FILE_ALREADY_DOWNLOADING,
					e.getErrorCode());
		}
		
		// already downloading based on hash
		RemoteFileDesc[] hashRFDS = new RemoteFileDesc[] {
				newRFD("dl", "urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB")
		};

		downloadManager.download(hashRFDS, emptyRFDList, new GUID(GUID.makeGuid()), false, null,
                null);
		
		try {
			downloadManager.download(hashRFDS, emptyRFDList,
					new GUID(GUID.makeGuid()), false, null, null);
			fail("No exception thrown");
		}
		catch (DownloadException e) {
			assertEquals("Error code should be: already downloading", 
					ErrorCode.FILE_ALREADY_DOWNLOADING,
					e.getErrorCode());
		}
				
		// other download is already being saved to the same file with different hashes
		{
			RemoteFileDesc[] fds = new RemoteFileDesc[] {
					newRFD("savedto", "urn:sha1:QLSTHIPQGSSZTS5FJUPAKOZWUGZQYPFB")
			};
			downloadManager.download(fds, emptyRFDList,
					new GUID(GUID.makeGuid()), false, null, "alreadysavedto");
		}

        try {
			RemoteFileDesc[] fds = new RemoteFileDesc[] {
					newRFD("otherfd", "urn:sha1:PLSTHIPQGSSZTS5FJUPAKOZWUGZQYPFB")
			};
			downloadManager.download(fds, emptyRFDList,
					new GUID(GUID.makeGuid()), false, null, "alreadysavedto");
			fail("should have gotten exception!");
		}
		catch (DownloadException e) {
			assertEquals("Error code should be: already being saved to", 
					ErrorCode.FILE_IS_ALREADY_DOWNLOADED_TO,
					e.getErrorCode());
		}
		
		// TODO DownloadException.FILE_ALREADY_SAVED
		// DownloadException.FILESYSTEM_ERROR is not really reproducible
	}
	
	@SuppressWarnings("unchecked")
    public void testDownloadStatusEventsPublishedToCorrectListeners() {
	    Mockery context = new Mockery(){{
	        setImposteriser(ClassImposteriser.INSTANCE);
	    }};
	    
	    final SaveLocationManager saveLocationManager = context.mock(SaveLocationManager.class);
	    final RequeryManager requeryManager = context.mock(RequeryManager.class);
	    final RequeryManagerFactory requeryManagerFactory = context.mock(RequeryManagerFactory.class);
	    final ListeningExecutorService downloadProcessingQueue = context.mock(ListeningExecutorService.class);
	    
	    final EventListener<DownloadStateEvent> downloadListener1 = context.mock(EventListener.class);
        final EventListener<DownloadStateEvent> downloadListener2 = context.mock(EventListener.class);
        
	    context.checking(new Expectations() {{
	        allowing(requeryManagerFactory).createRequeryManager(with(any(RequeryListener.class)));
	        will(returnValue(requeryManager));
	        allowing(downloadProcessingQueue).execute(with(any(Runnable.class)));
	        will(new ExecuteRunnableAction());
	    }});
	    
	    ManagedDownloaderImpl managedDownloaderImpl1 =
            new ManagedDownloaderImpl(saveLocationManager, null, null, null,
                    null, null, null, requeryManagerFactory, null, null, null,
                    null, null, null, null, null, null, null, background, null,
                    null, null, null, null, null, downloadProcessingQueue,
                    null, null, null);
	    managedDownloaderImpl1.addListener(downloadListener1);
        
        ManagedDownloaderImpl managedDownloaderImpl2 =
            new ManagedDownloaderImpl(saveLocationManager, null, null, null,
                    null, null, null, requeryManagerFactory, null, null, null,
                    null, null, null, null, null, null, null, background, null,
                    null, null, null, null, null, downloadProcessingQueue,
                    null, null, null);
        managedDownloaderImpl2.addListener(downloadListener2);
        
	    context.checking(new Expectations() {{
	        one(downloadListener1).handleEvent(with(any(DownloadStateEvent.class)));
	    }});
	    managedDownloaderImpl1.setState(DownloadState.ABORTED);
	    
	    context.checking(new Expectations() {{
            one(downloadListener2).handleEvent(with(any(DownloadStateEvent.class)));
        }});
        managedDownloaderImpl2.setState(DownloadState.CONNECTING);
	    
	    context.assertIsSatisfied();
	}
    
    private RemoteFileDesc newRFD(String name) throws Exception {
        return newRFD(name, null);
    }

    private RemoteFileDesc newRFD(String name, String hash) throws Exception {
        Set<URN> urns = Collections.emptySet();
        if (hash!=null) {
            urns=new HashSet<URN>(1);
            try {
                urns.add(URN.createSHA1Urn(hash));
            } catch (IOException e) {
                fail("Couldn't create URN", e);
            }
        }        
        return injector.getInstance(RemoteFileDescFactory.class).createRemoteFileDesc(new ConnectableImpl("127.0.0.1", PORT, false), 13l, name, 1024, new byte[16],
                56, 4, true, null, urns, false, "", -1);
    }
    
    
    @SuppressWarnings({"unchecked", "SynchronizeOnNonFinalField"})
    private void requestStart(ManagedDownloader dl) throws Exception {
        List<CoreDownloader> waiting = (List<CoreDownloader>)PrivilegedAccessor.getValue(downloadManager, "waiting");
        List<CoreDownloader> active = (List<CoreDownloader>)PrivilegedAccessor.getValue(downloadManager, "active");
        synchronized(downloadManager) {
            waiting.remove(dl);
            active.add(dl);
            dl.startDownload();
        }
    }

    
    private RemoteFileDesc newPushRFD(String name, String hash, byte[] guid) throws Exception {

        IpPort ppi = new IpPortImpl("1.2.3.10", 2000);

        Set<IpPort> ppis = new TreeSet<IpPort>(IpPort.COMPARATOR);
        ppis.add(ppi);

        PushEndpointFactory pushEndpointFactory = injector.getInstance(PushEndpointFactory.class);
        PushEndpoint pe = pushEndpointFactory.createPushEndpoint(guid, ppis);
        pe.updateProxies(true);

        return injector.getInstance(RemoteFileDescFactory.class).createRemoteFileDesc(newRFD(name, hash), pe);
    }
    
    private File createMaximumPathLengthDirectory() throws IOException {
        File tmpFile = File.createTempFile("temp", "file");
        File tmpDir = tmpFile.getParentFile();
        tmpFile.delete();
        
        char[] dirName = new char[OSUtils.getMaxPathLength() - tmpDir.getAbsolutePath().length()];
        Arrays.fill(dirName, 'z');
        for (int i = 0; i < dirName.length; i += 254) {
            dirName[i] = File.separatorChar;
        }
        File longestDir = new File(tmpDir.getAbsolutePath() + new String(dirName));
        // currently fails due to JRE bug
        if (!longestDir.mkdirs()) {
            // go through path and create them one by one till JRE bug is fixed
            LinkedList<File> dirs = new LinkedList<File>();
            File current = longestDir;
            while (!current.equals(tmpDir)) {
                dirs.add(current);
                current = current.getParentFile();
            }
            while (!dirs.isEmpty()) {
                current = dirs.removeLast();
                assertTrue("Could not create " + current, current.mkdir());
            }
        }
        return longestDir;
    }
    
    

    /**
     * allows overriding of the list of current downloaders
     */
    private void setDloaders(ManagedDownloader managedDownloader, List l) throws Exception {
        PrivilegedAccessor.setValue(managedDownloader,"_activeWorkers",l);
    }
    
    private void setSHA1(ManagedDownloader managedDownloader, URN sha1) throws Exception{
        PrivilegedAccessor.setValue(managedDownloader,"downloadSHA1",sha1);
    }
    
    private void setIncompleteFile(ManagedDownloader managedDownloader, File f) throws Exception {
        PrivilegedAccessor.setValue(managedDownloader,"incompleteFile",f);
    }
    
    
    @Singleton    
    private static class AltLocDownloaderStubFactory implements HTTPDownloaderFactory {

        private final NetworkManager networkManager;
        private final AlternateLocationFactory alternateLocationFactory;
        private final DownloadManager downloadManager;
        private final Provider<CreationTimeCache> creationTimeCache;
        private final BandwidthManager bandwidthManager;
        private final Provider<PushEndpointCache> pushEndpointCache;
        private final PushEndpointFactory pushEndpointFactory;
        private final RemoteFileDescFactory remoteFileDescFactory;
        private final ThexReaderFactory thexReaderFactory;
        private final TcpBandwidthStatistics tcpBandwidthStatistics;
        private final NetworkInstanceUtils networkInstanceUtils;

        @Inject
        public AltLocDownloaderStubFactory(NetworkManager networkManager,
                AlternateLocationFactory alternateLocationFactory, DownloadManager downloadManager,
                Provider<CreationTimeCache> creationTimeCache, BandwidthManager bandwidthManager,
                Provider<PushEndpointCache> pushEndpointCache,
                PushEndpointFactory pushEndpointFactory,
                RemoteFileDescFactory remoteFileDescFactory, ThexReaderFactory thexReaderFactory,
                TcpBandwidthStatistics tcpBandwidthStatistics, NetworkInstanceUtils networkInstanceUtils) {
            this.networkManager = networkManager;
            this.alternateLocationFactory = alternateLocationFactory;
            this.downloadManager = downloadManager;
            this.creationTimeCache = creationTimeCache;
            this.bandwidthManager = bandwidthManager;
            this.pushEndpointCache = pushEndpointCache;
            this.pushEndpointFactory = pushEndpointFactory;
            this.remoteFileDescFactory = remoteFileDescFactory;
            this.thexReaderFactory = thexReaderFactory;
            this.tcpBandwidthStatistics = tcpBandwidthStatistics;
            this.networkInstanceUtils = networkInstanceUtils;
        }
        
        public HTTPDownloader create(Socket socket, RemoteFileDescContext rfdContext,
                VerifyingFile incompleteFile, boolean inNetwork) {
            return new AltLocDownloaderStub(rfdContext, networkManager,
                    alternateLocationFactory, downloadManager, creationTimeCache.get(),
                    bandwidthManager, pushEndpointCache, pushEndpointFactory,
                    remoteFileDescFactory, thexReaderFactory, tcpBandwidthStatistics, networkInstanceUtils);
        }

    }
    
    private static class AltLocDownloaderStub extends HTTPDownloaderStub {
    	private boolean _stubFalts;
        private final RemoteFileDesc rfd;
        
    	public AltLocDownloaderStub(RemoteFileDescContext rfdContext,
                                    NetworkManager networkManager, AlternateLocationFactory alternateLocationFactory,
                                    DownloadManager downloadManager, CreationTimeCache creationTimeCache,
                                    BandwidthManager bandwidthManager, Provider<PushEndpointCache> pushEndpointCache,
                                    PushEndpointFactory pushEndpointFactory,
                                    RemoteFileDescFactory remoteFileDescFactory, ThexReaderFactory thexReaderFactory,
                                    TcpBandwidthStatistics tcpBandwidthStatistics, NetworkInstanceUtils networkInstanceUtils) {
            super(rfdContext, null, networkManager, alternateLocationFactory, downloadManager,
                    creationTimeCache, bandwidthManager, pushEndpointCache, pushEndpointFactory,
                    remoteFileDescFactory, thexReaderFactory, tcpBandwidthStatistics, networkInstanceUtils);
            this.rfd = rfdContext.getRemoteFileDesc();
        }
    	
    	public boolean _addedFailed,_addedSuccessfull;
   		@Override
        public void addFailedAltLoc(AlternateLocation loc) {
   			_addedFailed = true;
		}
		@Override
        public void addSuccessfulAltLoc(AlternateLocation loc) {
			_addedSuccessfull=true;
		}
		
		@Override
        public RemoteFileDesc getRemoteFileDesc() {
			return rfd;
		}
		
		public void setWantsFalts(boolean doesIt) {
			_stubFalts=doesIt;
		}
		
		@Override
        public boolean wantsFalts(){
			return _stubFalts;
		}
    }
    
    @Singleton
    private static class AltLocWorkerStubFactory implements DownloadWorkerFactory {
        private final HTTPDownloaderFactory httpDownloaderFactory;
        private final ScheduledExecutorService backgroundExecutor;
        private final ScheduledExecutorService nioExecutor;
        private final Provider<PushDownloadManager> pushDownloadManager;
        private final SocketsManager socketsManager;
        private final NetworkManager networkManager;
        
        @Inject
        public AltLocWorkerStubFactory(
                HTTPDownloaderFactory httpDownloaderFactory,
                @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
                @Named("nioExecutor") ScheduledExecutorService nioExecutor,
                Provider<PushDownloadManager> pushDownloadManager,
                SocketsManager socketsManager,
                NetworkManager networkManager) {
            this.httpDownloaderFactory = httpDownloaderFactory;
            this.backgroundExecutor = backgroundExecutor;
            this.nioExecutor = nioExecutor;
            this.pushDownloadManager = pushDownloadManager;
            this.socketsManager = socketsManager;
            this.networkManager = networkManager;
        }
        
        public DownloadWorker create(DownloadWorkerSupport manager,
                RemoteFileDescContext rfd, VerifyingFile vf) {
            return new AltLocWorkerStub(manager, rfd, vf, httpDownloaderFactory,
                    backgroundExecutor, nioExecutor, pushDownloadManager,
                    socketsManager, networkManager);
        }
    }
    
    private static class AltLocWorkerStub extends DownloadWorkerStub {
        private volatile HTTPDownloader httpDownloader;
        
        public AltLocWorkerStub(DownloadWorkerSupport manager, RemoteFileDescContext rfd, VerifyingFile vf,
                HTTPDownloaderFactory httpDownloaderFactory,
                ScheduledExecutorService backgroundExecutor, ScheduledExecutorService nioExecutor,
                Provider<PushDownloadManager> pushDownloadManager, SocketsManager socketsManager,
                TLSManager TLSManager) {
            super(manager, rfd, vf, httpDownloaderFactory, backgroundExecutor, nioExecutor,
                    pushDownloadManager, socketsManager, TLSManager);
        }

        public void setHTTPDownloader(HTTPDownloader httpDownloader) {
            this.httpDownloader = httpDownloader;
        }

        @Override
        public HTTPDownloader getDownloader() {
            return httpDownloader;
        }
    }
}
