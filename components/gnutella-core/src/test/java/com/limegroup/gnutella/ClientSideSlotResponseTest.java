package com.limegroup.gnutella;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.limewire.util.FileUtils;
import org.limewire.util.TestUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.Stage;
import com.limegroup.gnutella.auth.UrnValidator;
import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.library.LibraryUtils;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.statistics.TcpBandwidthStatistics;
import com.limegroup.gnutella.stubs.NetworkManagerStub;
import com.limegroup.gnutella.uploader.HttpRequestHandlerFactory;
import com.limegroup.gnutella.uploader.UploadSlotManager;
import com.limegroup.gnutella.uploader.authentication.GnutellaBrowseFileListProvider;
import com.limegroup.gnutella.uploader.authentication.GnutellaUploadFileListProvider;

/**
 * Tests how the availability of upload slots affects responses, as well
 * as chocking of queries to a leaf.
 */
public class ClientSideSlotResponseTest extends ClientSideTestCase {
    public ClientSideSlotResponseTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ClientSideSlotResponseTest.class);
    }    
    
    private Set<String> someFileMatches = new HashSet<String>(2);
    
    private String SOME_FILE = "somefile";
    private String TEXT_FILE = "somefile.txt";
    private String TORRENT_FILE =  "somefile.torrent";
    private String USER_TORRENT = "somefile2.torrent";
    private String APP_TXT =  "somefile2.txt";
    private String OTHER_TORRENT = "other.torrent";
    
    
    private UploadManagerStub uploadManagerStub;

    private NetworkManagerStub networkManagerStub;

    private QueryRequestFactory queryRequestFactory;
    
    @Override
    public void setUp() throws Exception {
        networkManagerStub = new NetworkManagerStub();
        Injector injector = LimeTestUtils.createInjector(Stage.PRODUCTION, new AbstractModule() {
            @Override
            protected void configure() {
                bind(UploadManager.class).to(UploadManagerStub.class);
                bind(NetworkManager.class).toInstance(networkManagerStub);
            }
        });
        
        super.setUp(injector);
        
        File torrentFile = new File(LibraryUtils.APPLICATION_SPECIAL_SHARE,TORRENT_FILE);
        File appTextFile = new File(LibraryUtils.APPLICATION_SPECIAL_SHARE,APP_TXT);
        File appTorrentFile = new File(LibraryUtils.APPLICATION_SPECIAL_SHARE, OTHER_TORRENT);
        FileUtils.copy(TestUtils.getResourceFile("com/limegroup/gnutella/resources/somefile.torrent"), torrentFile);
        FileUtils.copy(TestUtils.getResourceFile("com/limegroup/gnutella/resources/somefile2.txt"), appTextFile);
        FileUtils.copy(TestUtils.getResourceFile("com/limegroup/gnutella/resources/other.torrent"), appTorrentFile);
        
        File textFile = TestUtils.getResourceFile("com/limegroup/gnutella/resources/somefile.txt");
        File userTorrentFile = TestUtils.getResourceFile("com/limegroup/gnutella/resources/somefile2.torrent");

        someFileMatches.add(TEXT_FILE);
        someFileMatches.add(TORRENT_FILE);
        someFileMatches.add(USER_TORRENT);
        someFileMatches.add(APP_TXT);        
        
        assertNotNull(fileManager.getGnutellaFileList().add(textFile).get(1, TimeUnit.SECONDS));
        assertNotNull(fileManager.getGnutellaFileList().add(torrentFile).get(1, TimeUnit.SECONDS));
        assertNotNull(fileManager.getGnutellaFileList().add(userTorrentFile).get(1, TimeUnit.SECONDS));
        assertNotNull(fileManager.getGnutellaFileList().add(appTextFile).get(1, TimeUnit.SECONDS));
        assertNotNull(fileManager.getGnutellaFileList().add(appTorrentFile).get(1, TimeUnit.SECONDS));
        fileManager.getGnutellaFileList().remove(berkeleyFD);
        fileManager.getGnutellaFileList().remove(susheelFD);
        assertEquals(5, fileManager.getGnutellaFileList().size());
        
        queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
        uploadManagerStub = (UploadManagerStub) injector.getInstance(UploadManager.class);
        
        uploadManagerStub.isServiceable = true;
        uploadManagerStub.mayBeServiceable = true;

        networkManagerStub.setAcceptedIncomingConnection(true);
        
        assertTrue("should be open", testUP[0].isOpen());
        assertTrue("should be up -> leaf",
                testUP[0].getConnectionCapabilities().isSupernodeClientConnection());
        BlockingConnectionUtils.drain(testUP[0], 500);
    }

    
    @Override
    public int getNumberOfPeers() {
        return 3;
    }

    @Singleton
    private static class UploadManagerStub extends HTTPUploadManager {
    	
        boolean isServiceable, mayBeServiceable;
    	
        @Inject
        UploadManagerStub(UploadSlotManager slotManager,
                HttpRequestHandlerFactory httpRequestHandlerFactory,
                Provider<HTTPAcceptor> httpAcceptor,
                Provider<FileManager> fileManager, Provider<ActivityCallback> activityCallback,
                TcpBandwidthStatistics tcpBandwidthStatistics,
                Provider<GnutellaUploadFileListProvider> gnutellaUploadFileListProvider,
                Provider<GnutellaBrowseFileListProvider> gnutellaBrowseFileListProvider,
                UrnValidator urnValidator) {
            super(slotManager, httpRequestHandlerFactory, httpAcceptor,
                    fileManager, activityCallback, tcpBandwidthStatistics, gnutellaUploadFileListProvider,
                    gnutellaBrowseFileListProvider, urnValidator);
        }
		@Override
		public synchronized boolean isServiceable() {
			return isServiceable;
		}
		@Override
		public synchronized boolean mayBeServiceable() {
			return mayBeServiceable;
		}
    }
    
    
    /**
     * tests that if there are enough slots, responses are processed and
     * returned. 
     */
    public void testResponsesSent() throws Exception {
    	uploadManagerStub.isServiceable = true;
    	uploadManagerStub.mayBeServiceable = true;
    	QueryRequest query = queryRequestFactory.createQuery(SOME_FILE);
    	BlockingConnectionUtils.drain(testUP[0]);
    	testUP[0].send(query);
    	testUP[0].flush();
    	Thread.sleep(1000);
    	QueryReply reply = BlockingConnectionUtils.getFirstQueryReply(testUP[0]);
    	List<Response> responses = reply.getResultsAsList();
    	assertEquals(someFileMatches.size(), responses.size());
    	for(Response r: responses)
    		assertTrue(someFileMatches.contains(r.getName()));
    }
    
    /**
     * Tests that if no queries can be serviceable nothing gets returned.
     */
    public void testNothingSent() throws Exception {
    	uploadManagerStub.mayBeServiceable = false;
    	QueryRequest query = queryRequestFactory.createQuery(SOME_FILE);
    	BlockingConnectionUtils.drain(testUP[0]);
    	testUP[0].send(query);
    	testUP[0].flush();
    	Thread.sleep(1000);
    	assertNull(BlockingConnectionUtils.getFirstQueryReply(testUP[0]));
    }
    
    /**
     * Tests that if only metafiles can be serviced but
     * there are no metafile results, nothing gets returned.
     */
    public void testAllFiltered() throws Exception {
    	uploadManagerStub.mayBeServiceable = true;
    	uploadManagerStub.isServiceable = false;
    	QueryRequest query = queryRequestFactory.createQuery(TEXT_FILE);
    	BlockingConnectionUtils.drain(testUP[0]);
    	testUP[0].send(query);
    	testUP[0].flush();
    	Thread.sleep(1000);
    	assertNull(BlockingConnectionUtils.getFirstQueryReply(testUP[0]));
    }
    
    /**
     * tests that if only metafiles can be serviced and
     * all results are for metafiled, nothing gets filetered
     */
    public void testNoneFiltered() throws Exception {
    	uploadManagerStub.mayBeServiceable = true;
    	uploadManagerStub.isServiceable = false;
    	QueryRequest query = queryRequestFactory.createQuery(OTHER_TORRENT);
    	BlockingConnectionUtils.drain(testUP[0]);
    	testUP[0].send(query);
    	testUP[0].flush();
    	Thread.sleep(1000);
    	QueryReply reply = BlockingConnectionUtils.getFirstQueryReply(testUP[0]);
    	List<Response> responses = reply.getResultsAsList();
    	assertEquals(1, responses.size());
    	assertEquals(OTHER_TORRENT, responses.get(0).getName());
    }
    
    /**
     * Tests that if only metafiles can be serviced 
     * only results about application-shared metafiles are returned. 
     */
    public void testMetaFilesSent() throws Exception {
    	uploadManagerStub.isServiceable = false;
    	uploadManagerStub.mayBeServiceable = true;
    	QueryRequest query = queryRequestFactory.createQuery(SOME_FILE);
    	BlockingConnectionUtils.drain(testUP[0]);
    	testUP[0].send(query);
    	testUP[0].flush();
    	Thread.sleep(1000);
    	QueryReply reply = BlockingConnectionUtils.getFirstQueryReply(testUP[0]);
    	List<Response> responses = reply.getResultsAsList();
    	assertEquals(1, responses.size());
    	assertEquals(TORRENT_FILE, responses.get(0).getName());
    }
}





