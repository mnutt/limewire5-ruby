package com.limegroup.gnutella;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.nio.entity.ConsumingNHttpEntity;
import org.apache.http.nio.protocol.SimpleNHttpRequestHandler;
import org.apache.http.protocol.HttpContext;
import org.limewire.core.api.friend.feature.features.AddressFeature;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.NetworkSettings;
import org.limewire.io.Address;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.GUID;
import org.limewire.net.SocketsManager;
import org.limewire.security.SecureMessageVerifier;
import org.limewire.util.TestUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.library.FileManagerTestUtils;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.search.SearchResultHandler;
import com.limegroup.gnutella.util.LimeTestCase;
import com.limegroup.gnutella.xml.LimeXMLDocumentHelper;

public class BrowseHostHandlerTest extends LimeTestCase {

    private final int PORT = 6668;

    private FileManager fileManager;

    private BrowseHostHandler browseHostHandler;

    private SocketsManager socketsManager;

    private QueryReplyHandler queryReplyHandler;
    private Injector injector;

    public BrowseHostHandlerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(BrowseHostHandlerTest.class);
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    protected void setUp() throws Exception {
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        NetworkSettings.PORT.setValue(PORT);

        injector = LimeTestUtils.createInjectorAndStart(new AbstractModule() {
            @Override
            protected void configure() {
                bind(ForMeReplyHandler.class).to(QueryReplyHandler.class);
            }
        });

        fileManager = injector.getInstance(FileManager.class);

        FileManagerTestUtils.waitForLoad(fileManager, 5000);
        
        File dir = TestUtils.getResourceFile("com/limegroup/gnutella");
        File[] testFiles = dir.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return !file.isDirectory() && (file.getName().endsWith(".class"));
            }
        });
        for(File file : testFiles) {
            assertNotNull(fileManager.getGnutellaFileList().add(file).get(1, TimeUnit.SECONDS));
        }
        
        File testMp3 = TestUtils.getResourceFile("com/limegroup/gnutella/resources/berkeley.mp3");
        assertNotNull(fileManager.getGnutellaFileList().add(testMp3).get(1, TimeUnit.SECONDS));
        
        assertGreaterThan("Not enough files to test against", 50, testFiles.length);
        assertGreaterThan(0, fileManager.getGnutellaFileList().size());

        browseHostHandler = injector.getInstance(BrowseHostHandlerManager.class).createBrowseHostHandler(new GUID(), new GUID());
        socketsManager = injector.getInstance(SocketsManager.class);
        queryReplyHandler = (QueryReplyHandler) injector.getInstance(Key.get(ReplyHandler.class, Names.named("forMeReplyHandler")));
    }

    @Override
    protected void tearDown() throws Exception {
        injector.getInstance(LifecycleManager.class).shutdown();
    }
    
    public void testBrowseHostBadHTTPStatus() throws Exception {
        HTTPAcceptor httpAcceptor = injector.getInstance(HTTPAcceptor.class);
        httpAcceptor.registerHandler("/", new SimpleNHttpRequestHandler() {
            public ConsumingNHttpEntity entityRequest(HttpEntityEnclosingRequest request,
                    HttpContext context) throws HttpException, IOException {
                return null;
            }
            
            @Override
            public void handle(HttpRequest httpRequest, HttpResponse httpResponse, HttpContext httpContext) throws HttpException, IOException {
                httpResponse.setStatusCode(400);
            }
        });

        Connectable host = new ConnectableImpl("localhost", PORT, false); // TODO true
        SocketsManager.ConnectType type = SocketsManager.ConnectType.PLAIN;  // TODO ConnectType.TLS
        Socket socket = socketsManager.connect(new InetSocketAddress(host.getAddress(), host.getPort()),
                                                BrowseHostHandler.DIRECT_CONNECT_TIME, type);

        try {
            browseHostHandler.browseHost(socket, new MockFriendPresence());
            fail();
        } catch (IOException ioe) {
            // expected result
        }
    }

    public void testBrowseHost() throws Exception {
        Connectable host = new ConnectableImpl("localhost", PORT, false); // TODO true
        SocketsManager.ConnectType type = SocketsManager.ConnectType.PLAIN;  // TODO ConnectType.TLS
        Socket socket = socketsManager.connect(new InetSocketAddress(host.getAddress(), host.getPort()),
                                                BrowseHostHandler.DIRECT_CONNECT_TIME, type);

        MockFriendPresence mockFriendPresence = new MockFriendPresence();
        mockFriendPresence.addFeature(new AddressFeature(host));
        browseHostHandler.browseHost(socket, mockFriendPresence);

        List<String> files = new ArrayList<String>();
        for(QueryReply reply : queryReplyHandler.replies) {
            Response[] results = reply.getResultsArray();
            for (Response result : results) {
                files.add(result.getName());
                assertTrue("Expected .class, .mp3 or LimeWire file, got: " + result.getName(),
                        result.getName().endsWith(".class") || result.getName().endsWith(".mp3") || result.getName().toLowerCase().startsWith("limewire"));
            }
        }

        assertEquals(fileManager.getGnutellaFileList().size(), files.size());

        fileManager.getGnutellaFileList().getReadLock().lock();
        try {
            for(FileDesc result : fileManager.getGnutellaFileList()) {
                boolean contained = files.remove(result.getFileName());
                assertTrue("File is missing in browse response: "
                    + result.getFileName(), contained);
            }
        } finally {
            fileManager.getGnutellaFileList().getReadLock().unlock();
        }
        assertTrue("Browse returned more results than shared: " + files,
                files.isEmpty());
    }
    
    public void testBrowseHostWithLimeXml() throws Exception {
        Connectable host = new ConnectableImpl("localhost", PORT, false); // TODO true
        SocketsManager.ConnectType type = SocketsManager.ConnectType.PLAIN;  // TODO ConnectType.TLS
        Socket socket = socketsManager.connect(new InetSocketAddress(host.getAddress(), host.getPort()),
                                                BrowseHostHandler.DIRECT_CONNECT_TIME, type);

        MockFriendPresence friendPresence = new MockFriendPresence();
        friendPresence.addFeature(new AddressFeature(host));
        browseHostHandler.browseHost(socket, friendPresence);

        boolean mp3Found = false;
        List<String> files = new ArrayList<String>();
        for(QueryReply reply : queryReplyHandler.replies) {
            Response[] results = reply.getResultsArray();
            for (Response result : results) {
                String fileName = result.getName();
                files.add(fileName);
                if(fileName.endsWith(".mp3")) {
                    mp3Found = true;
                }
            }
        }

        assertTrue(mp3Found);
        assertEquals(fileManager.getGnutellaFileList().size(), files.size());

        fileManager.getGnutellaFileList().getReadLock().lock();
        boolean limeXmlFound = false;
        try {
            for(FileDesc result : fileManager.getGnutellaFileList()) {
                boolean contained = files.remove(result.getFileName());
                assertTrue("File is missing in browse response: "
                    + result.getFileName(), contained);
                if(result.getFileName().endsWith(".mp3") && result.getXMLDocument() != null ) {
                    limeXmlFound = true;
                }
            }
            
            assertTrue(limeXmlFound);
        } finally {
            fileManager.getGnutellaFileList().getReadLock().unlock();
        }
        assertTrue("Browse returned more results than shared: " + files,
                files.isEmpty());
    }



    public void testBrowseHostBadContentType() throws Exception {
        HTTPAcceptor httpAcceptor = injector.getInstance(HTTPAcceptor.class);
        httpAcceptor.registerHandler("/", new SimpleNHttpRequestHandler() {
            public ConsumingNHttpEntity entityRequest(HttpEntityEnclosingRequest request,
                    HttpContext context) throws HttpException, IOException {
                return null;
            }
            
            @Override
            public void handle(HttpRequest httpRequest, HttpResponse httpResponse, HttpContext httpContext) throws HttpException, IOException {
                httpResponse.setHeader("Content-Type", "application/foo-bar");
            }
        });

        Connectable host = new ConnectableImpl("localhost", PORT, false); // TODO true
        SocketsManager.ConnectType type = SocketsManager.ConnectType.PLAIN;  // TODO ConnectType.TLS
        Socket socket = socketsManager.connect(new InetSocketAddress(host.getAddress(), host.getPort()),
                                                BrowseHostHandler.DIRECT_CONNECT_TIME, type);
        try {
            browseHostHandler.browseHost(socket, new MockFriendPresence());
            fail();
        } catch (IOException ioe) {
            // expected result
        }
    }

    public void testBrowseHostBadContentEncoding() throws Exception {
        HTTPAcceptor httpAcceptor = injector.getInstance(HTTPAcceptor.class);
        httpAcceptor.registerHandler("/", new SimpleNHttpRequestHandler() {
            public ConsumingNHttpEntity entityRequest(HttpEntityEnclosingRequest request,
                    HttpContext context) throws HttpException, IOException {
                return null;
            }
            
            @Override
            public void handle(HttpRequest httpRequest, HttpResponse httpResponse, HttpContext httpContext) throws HttpException, IOException {
                httpResponse.setHeader("Content-Encoding", "zip");
            }
        });

        Connectable host = new ConnectableImpl("localhost", PORT, false); // TODO true
        SocketsManager.ConnectType type = SocketsManager.ConnectType.PLAIN;  // TODO ConnectType.TLS
        Socket socket = socketsManager.connect(new InetSocketAddress(host.getAddress(), host.getPort()),
                                                BrowseHostHandler.DIRECT_CONNECT_TIME, type);

        try {
            browseHostHandler.browseHost(socket, new MockFriendPresence());
            fail();
        } catch (IOException ioe) {
            // expected result
        }
    }
    
    public void testGetPathForNonAnonymousFriend() {
        assertEquals("/friend/browse/me%40you.com/", browseHostHandler.getPath(new MockFriendPresence(new MockFriend("me@you.com", false))));
        assertEquals("/friend/browse/Hello+There/", browseHostHandler.getPath(new MockFriendPresence(new MockFriend("Hello There", false))));
    }

    @Singleton
    public static class QueryReplyHandler extends ForMeReplyHandler {

        @Inject
        QueryReplyHandler(NetworkManager networkManager,
                SecureMessageVerifier secureMessageVerifier,
                Provider<ConnectionManager> connectionManager,
                Provider<SearchResultHandler> searchResultHandler,
                Provider<DownloadManager> downloadManager, Provider<Acceptor> acceptor,
                Provider<PushManager> pushManager, @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
                ApplicationServices applicationServices, ConnectionServices connectionServices,
                LimeXMLDocumentHelper limeXMLDocumentHelper, Provider<IPFilter> ipFilterProvider) {
            super(networkManager, secureMessageVerifier, connectionManager, searchResultHandler,
                    downloadManager, acceptor, pushManager, backgroundExecutor, applicationServices,
                    connectionServices, limeXMLDocumentHelper, ipFilterProvider);
            
        }

        ArrayList<QueryReply> replies = new ArrayList<QueryReply>();

        @Override
        public void handleQueryReply(QueryReply reply, ReplyHandler handler, Address address) {
            replies.add(reply);
        }

    }
}
