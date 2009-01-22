package com.limegroup.gnutella.downloader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import junit.framework.Test;

import org.limewire.collection.Range;
import org.limewire.core.settings.NetworkSettings;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.GUID;
import org.limewire.io.LocalSocketAddressProvider;
import org.limewire.net.SocketsManager;
import org.limewire.security.MACCalculatorRepositoryManager;
import org.limewire.util.PrivilegedAccessor;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.DownloadManagerImpl;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.ForMeReplyHandler;
import com.limegroup.gnutella.GuidMapManager;
import com.limegroup.gnutella.HostCatcher;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.MessageDispatcher;
import com.limegroup.gnutella.MessageHandlerBinder;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.MulticastService;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.PongCacher;
import com.limegroup.gnutella.QueryUnicaster;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.ResponseFactory;
import com.limegroup.gnutella.ResponseFactoryImpl;
import com.limegroup.gnutella.RouteTable;
import com.limegroup.gnutella.Statistics;
import com.limegroup.gnutella.UDPReplyHandlerCache;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UploadManager;
import com.limegroup.gnutella.Downloader.DownloadStatus;
import com.limegroup.gnutella.auth.ContentManager;
import com.limegroup.gnutella.connection.RoutedConnectionFactory;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.filters.URNFilter;
import com.limegroup.gnutella.guess.OnDemandUnicaster;
import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.library.SharedFilesKeywordIndex;
import com.limegroup.gnutella.messagehandlers.InspectionRequestHandler;
import com.limegroup.gnutella.messagehandlers.LimeACKHandler;
import com.limegroup.gnutella.messagehandlers.OOBHandler;
import com.limegroup.gnutella.messagehandlers.UDPCrawlerPingHandler;
import com.limegroup.gnutella.messages.OutgoingQueryReplyFactory;
import com.limegroup.gnutella.messages.PingReplyFactory;
import com.limegroup.gnutella.messages.PingRequestFactory;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryReplyFactory;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.messages.StaticMessages;
import com.limegroup.gnutella.messages.vendor.HeadPongFactory;
import com.limegroup.gnutella.messages.vendor.ReplyNumberVendorMessageFactory;
import com.limegroup.gnutella.routing.QRPUpdater;
import com.limegroup.gnutella.search.QueryDispatcher;
import com.limegroup.gnutella.search.QueryHandlerFactory;
import com.limegroup.gnutella.search.SearchResultHandler;
import com.limegroup.gnutella.simpp.SimppManager;
import com.limegroup.gnutella.stubs.ConnectionManagerStub;
import com.limegroup.gnutella.stubs.LocalSocketAddressProviderStub;
import com.limegroup.gnutella.stubs.MessageRouterStub;
import com.limegroup.gnutella.stubs.NetworkManagerStub;
import com.limegroup.gnutella.util.LimeTestCase;
import com.limegroup.gnutella.util.LimeWireUtils;
import com.limegroup.gnutella.util.QueryUtils;
import com.limegroup.gnutella.version.UpdateHandler;

/**
 * This test makes sure that LimeWire does the following things:
 * 1) Does NOT requery ever.
 * 2) Wakes up from the WAITING_FROM_RESULTS state when a new, valid query comes
 *    in.
 * 3) Wakes up from the GAVE_UP state when a new, valid query comes in.
 */
public class RequeryDownloadTest extends LimeTestCase {

    /** The main test fixture.  Contains the incomplete file and hash below. */
    private DownloadManagerImpl downloadManager; 
    /** Where to send and receive messages */
    private static TestMessageRouter messageRouter;
    /** The simulated downloads.dat file.  Used only to build _mgr. */
    private File snapshot;
    /** The name of the completed file. */
    private String filename="some file.txt";
    /** The incomplete file to resume from. */
    private File incompleteFile;    
    /** The hash of file when complete. */
    private URN hash;
    /** The uploader */
    private TestUploader testUploader;
    /** The TestMessageRouter's queryRouteTable. */
    private RouteTable routeTable;
    
    private Injector injector;
    private ForMeReplyHandler forMeReplyHandler;
    
    private static final int PORT = 6939;

    //////////////////////////// Fixtures /////////////////////////

    public RequeryDownloadTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(RequeryDownloadTest.class);
    }
    
    
    @Override
    public void setUp() throws Exception {
        final LocalSocketAddressProviderStub localSocketAddressProviderStub = new LocalSocketAddressProviderStub();
        localSocketAddressProviderStub.setLocalAddressPrivate(false);
        injector = LimeTestUtils.createInjector(new AbstractModule() {
          @Override
            protected void configure() {
              bind(MessageRouter.class).to(TestMessageRouter.class);
              bind(ConnectionManager.class).to(ConnectionManagerStub.class);
              bind(NetworkManager.class).to(NetworkManagerStub.class);
              bind(LocalSocketAddressProvider.class).toInstance(localSocketAddressProviderStub);
            }  
        });
        
        hash = TestFile.hash();
        NetworkManagerStub networkManager = (NetworkManagerStub) injector
                .getInstance(NetworkManager.class);
        networkManager.setListeningPort(NetworkSettings.PORT.getValue());
        
        messageRouter = (TestMessageRouter)injector.getInstance(MessageRouter.class);
        routeTable = (RouteTable) PrivilegedAccessor.getValue(messageRouter, "_queryRouteTable");
        
        forMeReplyHandler = injector.getInstance(ForMeReplyHandler.class);

        initializeIncompleteFileManager();
        downloadManager = (DownloadManagerImpl)injector.getInstance(DownloadManager.class);
        downloadManager.start();
        downloadManager.scheduleWaitingPump();
        testUploader = injector.getInstance(TestUploader.class);
        testUploader.start("uploader 6666", 6666, false);
        testUploader.setRate(Integer.MAX_VALUE);
        
        RequeryManager.NO_DELAY = true;

        new File(getSaveDirectory(), filename).delete();
    }
    
    /** Creates the incomplete file and returns an IncompleteFileManager with
     *  info for that file. */
    private void initializeIncompleteFileManager() throws Exception {
       IncompleteFileManager ifm= injector.getInstance(IncompleteFileManager.class);
       Set<URN> urns=new HashSet<URN>(1);
       urns.add(hash);
       RemoteFileDesc rfd = injector.getInstance(RemoteFileDescFactory.class).createRemoteFileDesc(new ConnectableImpl("1.2.3.4", PORT, false), 13l, filename, TestFile.length(),
            new byte[16], 56, false, 4, true, null, urns, false, "", -1);

       //Create incompleteFile, write a few bytes
       incompleteFile=ifm.getFile(rfd);
       try {
           incompleteFile.delete();
           incompleteFile.createNewFile();
           OutputStream out=new FileOutputStream(incompleteFile);
           out.write(TestFile.getByte(0));
           out.write(TestFile.getByte(1));
           out.close();
       } catch (IOException e) { 
           fail("Couldn't create incomplete file", e);
       }

       //Record information in IncompleteFileManager.
       VerifyingFileFactory verifyingFileFactory = injector.getInstance(VerifyingFileFactory.class);
       VerifyingFile vf= verifyingFileFactory.createVerifyingFile(TestFile.length());
       vf.addInterval(Range.createRange(0, 1));  //inclusive
       ifm.addEntry(incompleteFile, vf, true);
    }
       
    @Override
    public void tearDown() {
        if(testUploader != null)
            testUploader.stopThread();
        if (incompleteFile != null )
            incompleteFile.delete();
        if (snapshot != null)
           snapshot.delete();
        new File(getSaveDirectory(), filename).delete();           
    }


    /////////////////////////// Actual Tests /////////////////////////////

    /** Gets response with exact match, starts downloading. */
    public void testExactMatch() throws Exception {
        doTest(filename, hash, true);
    }

    /** Gets response with same hash, different name, starts downloading. */
    public void testHashMatch() throws Exception {
        doTest("different name.txt", hash, true);
    }

    /** Gets a response that doesn't match--can't download. */
    public void testNoMatch() throws Exception {
        doTest("some other file.txt", null, false);
    }
    
    /** Runs the tests again with pro set */
    public void testProExact() throws Exception {
        PrivilegedAccessor.setValue(LimeWireUtils.class, "_isPro", Boolean.TRUE);
        testExactMatch();
    }
    
    public void testProHash() throws Exception {
        PrivilegedAccessor.setValue(LimeWireUtils.class, "_isPro", Boolean.TRUE);
        testHashMatch();
    }
    
    public void testProNo() throws Exception {
        PrivilegedAccessor.setValue(LimeWireUtils.class, "_isPro", Boolean.TRUE);
        testNoMatch();
    }


    /**
     * Skeleton method for all tests.
     * @param responseName the file name to send in responses
     * @param responseURN the SHA1 urn to send in responses, or null for none
     * @param shouldDownload true if the downloader should actually start
     *  the download.  False if the response shouldn't satisfy it.
     */     
    private void doTest(String responseName, 
                        URN responseURN,
                        boolean shouldDownload) throws Exception {        
        // we need to seed the MessageRouter with a GUID that it will recognize
        
        byte[] guidToUse = GUID.makeGuid();
        routeTable.routeReply(guidToUse, forMeReplyHandler);

        //Start a download for the given incomplete file.  Give the thread time
        //to start up and send its requery.
        Downloader downloader = null;
        downloader = downloadManager.download(incompleteFile);
        assertTrue(downloader instanceof ResumeDownloader);
        assertEquals(DownloadStatus.QUEUED,downloader.getState());
        
        int counts = 0;
		// Make sure that you are through the QUEUED state.
        while (downloader.getState() == DownloadStatus.QUEUED) {
            Thread.sleep(100);
            if(counts++ > 50)
                fail("took too long, state: " + downloader.getState());
		}
        
        //give the downloading thread time to change states
        Thread.sleep(1000);
        
        assertEquals("downloader isn't waiting for results", 
                DownloadStatus.WAITING_FOR_GNET_RESULTS, downloader.getState());

        // no need to do a dldr.resume() cuz ResumeDownloaders spawn the query
        // automatically

        //Check that we can get query of right type.
        //TODO: try resume without URN
        assertEquals("unexpected router.broadcasts size", 1, messageRouter.broadcasts.size());
        Object m=messageRouter.broadcasts.get(0);
        assertInstanceof("m should be a query request", QueryRequest.class, m);
        QueryRequest qr=(QueryRequest)m;
        // no more requeries
        assertTrue((GUID.isLimeGUID(qr.getGUID())) &&
                   !(GUID.isLimeRequeryGUID(qr.getGUID())));
        // since filename is the first thing ever submitted it should always
        // query for allFiles[0].getFileName()
        String qString =QueryUtils.createQueryString(filename);
        assertEquals("should have queried for filename", qString, 
                     qr.getQuery());
        Set urns=qr.getQueryUrns();
        assertNotNull("urns shouldn't be null", urns);
        assertEquals("should only have NO urn", 0, urns.size());
        // not relevant anymore, we don't send URN queries
        // assertTrue("urns should contain the hash", urns.contains(_hash));

        //Send a response to the query.
        Set<URN> responseURNs = null;
        if (responseURN != null) {
            responseURNs = new HashSet<URN>(1);
            responseURNs.add(responseURN);
        }
        ResponseFactoryImpl responseFactory = (ResponseFactoryImpl)injector.getInstance(ResponseFactory.class);
        Response response = responseFactory.createResponse(0L, TestFile.length(), responseName, -1, responseURNs, null, null, null);
        byte[] ip = {(byte)127, (byte)0, (byte)0, (byte)1};
        QueryReplyFactory queryReplyFactory = injector.getInstance(QueryReplyFactory.class);
        QueryReply reply = queryReplyFactory.createQueryReply(guidToUse, (byte)6, 6666,
                ip, 0l, new Response[] { response }, new byte[16], false, false, true, false,
                false, false);//supports chat, is multicast response....
        RoutedConnectionFactory managedConnectionFactory = injector.getInstance(RoutedConnectionFactory.class);
        messageRouter.handleQueryReply(reply, managedConnectionFactory.createRoutedConnection("1.2.3.4", PORT));

        //Make sure the downloader does the right thing with the response.
        Thread.sleep(2000);
        counts = 0;
        if (shouldDownload) {
            //a) Match: wait for download to start, then complete.
            while (downloader.getState()!=DownloadStatus.COMPLETE) {            
			    if ( downloader.getState() != DownloadStatus.CONNECTING &&
			         downloader.getState() != DownloadStatus.HASHING &&
			         downloader.getState() != DownloadStatus.SAVING )
                    assertEquals(DownloadStatus.DOWNLOADING, downloader.getState());
                Thread.sleep(500);
                if(counts++ > 60)
                    fail("took too long, state: " + downloader.getState());
            }
        } 
        else {
            //b) No match: keep waiting for results
            assertEquals("downloader should wait for user", 
                    DownloadStatus.WAITING_FOR_GNET_RESULTS, downloader.getState());
            downloader.stop(false);
        }
    }
    
    @Singleton
    private static class TestMessageRouter extends MessageRouterStub {        
        private List<QueryRequest> broadcasts=Collections.synchronizedList(new LinkedList<QueryRequest>());
        
        @Inject
        public TestMessageRouter(NetworkManager networkManager,
                QueryRequestFactory queryRequestFactory,
                QueryHandlerFactory queryHandlerFactory,
                OnDemandUnicaster onDemandUnicaster,
                HeadPongFactory headPongFactory, PingReplyFactory pingReplyFactory,
                ConnectionManager connectionManager, @Named("forMeReplyHandler")
                ReplyHandler forMeReplyHandler, QueryUnicaster queryUnicaster,
                FileManager fileManager, ContentManager contentManager,
                DHTManager dhtManager, UploadManager uploadManager,
                DownloadManager downloadManager, UDPService udpService,
                SearchResultHandler searchResultHandler,
                SocketsManager socketsManager, HostCatcher hostCatcher,
                QueryReplyFactory queryReplyFactory, StaticMessages staticMessages,
                Provider<MessageDispatcher> messageDispatcher,
                MulticastService multicastService, QueryDispatcher queryDispatcher,
                Provider<ActivityCallback> activityCallback,
                ConnectionServices connectionServices,
                ApplicationServices applicationServices,
                @Named("backgroundExecutor")
                ScheduledExecutorService backgroundExecutor,
                Provider<PongCacher> pongCacher,
                Provider<SimppManager> simppManager,
                Provider<UpdateHandler> updateHandler,
                GuidMapManager guidMapManager, 
                UDPReplyHandlerCache udpReplyHandlerCache,
                Provider<InspectionRequestHandler> inspectionRequestHandlerFactory,
                Provider<UDPCrawlerPingHandler> udpCrawlerPingHandlerFactory,
                Statistics statistics,
                ReplyNumberVendorMessageFactory replyNumberVendorMessageFactory,
                PingRequestFactory pingRequestFactory,
                MessageHandlerBinder messageHandlerBinder,
                Provider<OOBHandler> oobHandlerFactory,
                Provider<MACCalculatorRepositoryManager> macManager,
                Provider<LimeACKHandler> limeACKHandler,
                OutgoingQueryReplyFactory outgoingQueryReplyFactory,
                SharedFilesKeywordIndex sharedFilesKeywordIndex,
                QRPUpdater qrpUpdater, URNFilter urnFilter) {
            super(networkManager, queryRequestFactory, queryHandlerFactory,
                    onDemandUnicaster, headPongFactory, pingReplyFactory,
                    connectionManager, forMeReplyHandler, queryUnicaster,
                    fileManager, contentManager, dhtManager, uploadManager,
                    downloadManager, udpService, searchResultHandler,
                    socketsManager, hostCatcher, queryReplyFactory,
                    staticMessages, messageDispatcher, multicastService,
                    queryDispatcher, activityCallback, connectionServices,
                    applicationServices, backgroundExecutor, pongCacher,
                    simppManager, updateHandler, guidMapManager,
                    udpReplyHandlerCache, inspectionRequestHandlerFactory,
                    udpCrawlerPingHandlerFactory, statistics,
                    replyNumberVendorMessageFactory, pingRequestFactory,
                    messageHandlerBinder, oobHandlerFactory, macManager,
                    limeACKHandler, outgoingQueryReplyFactory,
                    sharedFilesKeywordIndex, qrpUpdater, urnFilter);
        } 
        
        @Override
        public void sendDynamicQuery(QueryRequest query) {
            broadcasts.add(query);
            super.sendDynamicQuery(query); //add GUID to route table
        }
        
        public void clearBroadcasts() { 
            broadcasts.clear();
        }
    }

    
}
