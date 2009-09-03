package com.limegroup.gnutella.uploader;

import java.util.Collections;

import junit.framework.Test;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.protocol.BasicHttpContext;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.States;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.gnutella.tests.NetworkManagerStub;
import org.limewire.io.ConnectableImpl;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.library.FileCollection;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.FileDescStub;
import com.limegroup.gnutella.library.GnutellaFiles;
import com.limegroup.gnutella.library.LibraryStubModule;
import com.limegroup.gnutella.stubs.ConnectionManagerStub;
import com.limegroup.gnutella.tigertree.HashTree;
import com.limegroup.gnutella.tigertree.HashTreeCache;
import com.limegroup.gnutella.uploader.authentication.GnutellaUploadFileViewProvider;

public class FileRequestHandlerTest extends LimeTestCase {

    private FileDesc fd = new FileDescStub("filename");

    private URN urn1;

    private MockHTTPUploadSessionManager sessionManager;

    private FileRequestHandler fileRequestHandler;

    @Inject private Injector injector;
    @Inject @GnutellaFiles private FileCollection gnutellaFileCollection;

    private Mockery context;

    private HashTreeCache hashTreeCache;

    private States states;

    public FileRequestHandlerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(FileRequestHandlerTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
        hashTreeCache = context.mock(HashTreeCache.class);
        states = context.states("").startsAs("default");
        context.checking(new Expectations() {{
            allowing(hashTreeCache).getHashTree(with(any(FileDesc.class)));
            will(returnValue(null));
            when(states.is("default"));
        }});
        sessionManager = new MockHTTPUploadSessionManager();
        LimeTestUtils.createInjectorNonEagerly(new AbstractModule() {
            @Override
            protected void configure() {
                bind(ConnectionManager.class).to(ConnectionManagerStub.class);
                bind(HTTPUploadSessionManager.class).toInstance(sessionManager);
                bind(NetworkManager.class).to(NetworkManagerStub.class);
                bind(HashTreeCache.class).toInstance(hashTreeCache);
            }
        }, new LibraryStubModule(), LimeTestUtils.createModule(this));

        ConnectionManagerStub connectionManager = (ConnectionManagerStub) injector
                .getInstance(ConnectionManager.class);
        connectionManager.setPushProxies(Collections.singleton(new ConnectableImpl("127.0.0.1",
                9999, false)));

        urn1 = URN.createSHA1Urn("urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFG");
        FileDesc fd1 = new FileDescStub("abc1.txt", urn1, 0);
        gnutellaFileCollection.add(fd1);
        
        GnutellaUploadFileViewProvider uploadProvider = injector.getInstance(GnutellaUploadFileViewProvider.class);
        fileRequestHandler = injector.getInstance(FileRequestHandlerFactory.class).createFileRequestHandler(uploadProvider, false);
    }

    public void testHandleAccept() throws Exception {
        HttpRequest request = new BasicHttpRequest("GET", "filename");
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "");
        HTTPUploader uploader = new HTTPUploader("filename", null);
        // setting host to a value, so we don't need to specify a session
        uploader.setHost("somehost");
        uploader.setFileDesc(fd);

        NetworkManagerStub networkManager = (NetworkManagerStub) injector
                .getInstance(NetworkManager.class);
        networkManager.setCanDoFWT(true);

        fileRequestHandler.handleAccept(new BasicHttpContext(null), request, response,
                uploader, fd, null);
        Header header = response.getFirstHeader(HTTPHeaderName.FWTPORT.httpStringValue());
        assertNotNull("expected header: " + HTTPHeaderName.FWTPORT.httpStringValue(), header);
        assertEquals(networkManager.getStableUDPPort() + "", header.getValue());
    }

    public void testFeatureHeaderInterceptor() throws Exception {
        HTTPUploadSession session = new HTTPUploadSession(null, null, null);
        HTTPUploader uploader = new HTTPUploader("filename", session);
        // setting host to a value, so we don't need to specify a session
        uploader.setHost("somehost");
        uploader.setFileDesc(fd);
        sessionManager.uploader = uploader;
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "");

        HttpRequest request = new BasicHttpRequest("GET", LimeTestUtils.getRelativeRequest(urn1));
        request.addHeader("X-Features", "fwalt/0.1,browse/1.0,chat/0.1");
        request.addHeader("X-Node", "127.0.0.1:1234");
        request.addHeader("X-Downloaded", "123456");
        request.addHeader("Range", "bytes 1-2");
        request.addHeader("X-Gnutella-Content-URN", urn1.httpStringValue());
        request.addHeader("X-Queue", "1.0");

        fileRequestHandler.handle(request, response, new BasicHttpContext(null));
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals(1234, uploader.getGnutellaPort());
        assertEquals("127.0.0.1", uploader.getHost());
        assertTrue(uploader.isBrowseHostEnabled());
        assertEquals(123456, uploader.getTotalAmountUploaded());
        assertEquals(1, uploader.getUploadBegin());
        assertEquals(3, uploader.getUploadEnd());
        assertEquals(true, uploader.containedRangeRequest());
        assertEquals(urn1, uploader.getRequestedURN());
        assertTrue(uploader.supportsQueueing());
    }

    /**
     * Tests if browse host is enabled on the uploader if the downloader sent
     * its push endpoint information along in the X-FWT-Node header.  
     */
    public void testIsFWTBrowseHostEnabled() throws Exception {
        HTTPUploadSession session = new HTTPUploadSession(null, null, null);
        HTTPUploader uploader = new HTTPUploader("filename", session);
        // setting host to a value, so we don't need to specify a session
        uploader.setHost("somehost");
        uploader.setFileDesc(fd);
        sessionManager.uploader = uploader;
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "");

        HttpRequest request = new BasicHttpRequest("GET", LimeTestUtils.getRelativeRequest(urn1));
        
        String pushEndpoint = "FF9EEA9E8B2E1D737828EFD1B7DAC500;129.168.0.1:5555";
        
        request.addHeader(HTTPHeaderName.FW_NODE_INFO.create(pushEndpoint));
        fileRequestHandler.handle(request, response, new BasicHttpContext(null));
        
        assertTrue(uploader.isBrowseHostEnabled());
        assertEquals(pushEndpoint, uploader.getPushEndpoint().httpStringValue());
    }

    public void testResponseContainsThexUriHeader() throws Exception {
        HttpRequest request = new BasicHttpRequest("GET", "filename");
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "");
        HTTPUploader uploader = new HTTPUploader("filename", null);
        // setting host to a value, so we don't need to specify a session
        uploader.setHost("somehost");
        uploader.setFileDesc(fd);

        final HashTree hashTree = context.mock(HashTree.class);
        context.checking(new Expectations() {{ 
            one(hashTreeCache).getHashTree(fd);
            will(returnValue(hashTree));
            one(hashTree).httpStringValue();
            will(returnValue("/uri-res/N2X?hash-tree-httpstring"));
        }});

        // switch state to allow expectations here to kick in
        states.is("non-default").activate();
        
        fileRequestHandler.handleAccept(new BasicHttpContext(null), request, response,
                uploader, fd, null);
        Header header = response.getFirstHeader("X-Thex-URI");
        assertNotNull(header);
        assertEquals("/uri-res/N2X?hash-tree-httpstring", header.getValue());
        
        context.assertIsSatisfied();
    }
    

    public void testResponseContainsCorrectFriendThexUriHeader() throws Exception {
        HttpRequest request = new BasicHttpRequest("GET", "filename");
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "");
        HTTPUploader uploader = new HTTPUploader("filename", null);
        // setting host to a value, so we don't need to specify a session
        uploader.setHost("somehost");
        uploader.setFileDesc(fd);

        final HashTree hashTree = context.mock(HashTree.class);
        context.checking(new Expectations() {{ 
            one(hashTreeCache).getHashTree(fd);
            will(returnValue(hashTree));
            one(hashTree).httpStringValue();
            will(returnValue("/uri-res/N2X?hash-tree-httpstring"));
        }});

        // switch state to allow expectations here to kick in
        states.is("non-default").activate();
        
        fileRequestHandler.handleAccept(new BasicHttpContext(null), request, response,
                uploader, fd, "friend@id");
        Header header = response.getFirstHeader("X-Thex-URI");
        assertNotNull(header);
        assertEquals("/friend/download/friend%40id/uri-res/N2X?hash-tree-httpstring", header.getValue());
        
        context.assertIsSatisfied();
    }
}