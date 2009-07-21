package com.limegroup.gnutella.uploader;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.conn.ManagedClientConnection;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;
import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.FilterSettings;
import org.limewire.core.settings.NetworkSettings;
import org.limewire.core.settings.SharingSettings;
import org.limewire.core.settings.UltrapeerSettings;
import org.limewire.core.settings.UploadSettings;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.http.httpclient.HttpClientUtils;
import org.limewire.http.httpclient.LimeHttpClient;
import org.limewire.io.LimeWireIOTestModule;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.listener.EventListener;
import org.limewire.net.ConnectionDispatcher;
import org.limewire.nio.NIOTestUtils;
import org.limewire.util.CommonUtils;
import org.limewire.util.PrivilegedAccessor;
import org.limewire.util.TestUtils;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Stage;
import com.google.inject.name.Names;
import com.limegroup.gnutella.Acceptor;
import com.limegroup.gnutella.HTTPAcceptor;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UploadManager;
import com.limegroup.gnutella.dime.DIMEParser;
import com.limegroup.gnutella.dime.DIMERecord;
import com.limegroup.gnutella.downloader.VerifyingFile;
import com.limegroup.gnutella.downloader.VerifyingFileFactory;
import com.limegroup.gnutella.library.CreationTimeCache;
import com.limegroup.gnutella.library.FileCollection;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.FileManagerTestUtils;
import com.limegroup.gnutella.library.FileView;
import com.limegroup.gnutella.library.FileViewChangeEvent;
import com.limegroup.gnutella.library.GnutellaFiles;
import com.limegroup.gnutella.library.IncompleteFileCollection;
import com.limegroup.gnutella.library.Library;
import com.limegroup.gnutella.security.Tiger;
import com.limegroup.gnutella.tigertree.HashTree;
import com.limegroup.gnutella.tigertree.HashTreeCache;
import com.limegroup.gnutella.tigertree.HashTreeCacheImpl;
import com.limegroup.gnutella.tigertree.HashTreeUtils;

/**
 * Test that a client uploads a file correctly. Depends on a file containing the
 * lowercase characters a-z.
 */
//ITEST
public class UploadTest extends LimeTestCase {

    private static final int PORT = 6668;

    /** The file name, plain and encoded. */
    private static final String testDirName = "com/limegroup/gnutella/uploader/data";

    private static final String incName = "partial alphabet.txt";

    private static final String fileName = "alphabet test file#2.txt";

    private static final String otherFileName = "upperAlphabet.txt";

    /** The hash of the file contents. */
    private static final String baseHash = "GLIQY64M7FSXBSQEZY37FIM5QQSA2OUJ";

    private static final String hash = "urn:sha1:" + baseHash;

    private static final String badHash = "urn:sha1:GLIQY64M7FSXBSQEZY37FIM5QQSA2SAM";

    private static final String incompleteHash = "urn:sha1:INCOMCPLETEXBSQEZY37FIM5QQSA2OUJ";
    

    /** The verifying file for the shared incomplete file */
    private VerifyingFile vf;

    @Inject private LimeHttpClient client;

    protected String protocol;

    @Inject private Injector injector;

    @Inject private UploadManager uploadManager;

    @Inject private Library library;
    @Inject @GnutellaFiles private FileView gnutellaFileView;
    @Inject @GnutellaFiles private FileCollection gnutellaFileCollection;
    @Inject private IncompleteFileCollection incompleteFileCollection;

    private String fileNameUrl;
    private String relativeFileNameUrl;

    private String otherFileNameUrl;
        
    private String incompleteHashUrl;
    
    private String badHashUrl;

    public UploadTest(String name) {
        super(name);
        
        this.protocol = "http";
    }

    public static Test suite() {
        return buildTestSuite(UploadTest.class);
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }

    @Override
    protected void setUp() throws Exception {
        doSettings();

        // initialize services
        LimeTestUtils.createInjector(Stage.PRODUCTION, new LimeWireIOTestModule(), LimeTestUtils.createModule(this));
        
        startServices();
        File testDir = TestUtils.getResourceFile(testDirName);
        Future<FileDesc> fd1 = gnutellaFileCollection.add(new File(testDir, fileName));
        Future<FileDesc> fd2 = gnutellaFileCollection.add(new File(testDir, otherFileName));

        // get urls from file manager
        FileDesc fd = fd1.get();
        assertNotNull("File not loaded", fd);
        fileNameUrl = LimeTestUtils.getRequest("localhost", PORT, fd.getSHA1Urn());
        relativeFileNameUrl = LimeTestUtils.getRelativeRequest(fd.getSHA1Urn());
        
        fd = fd2.get();
        assertNotNull("File not loaded", fd);
        otherFileNameUrl = LimeTestUtils.getRequest("localhost", PORT, fd.getSHA1Urn());
        
        // add incomplete file to file manager
        File incFile = new File(_incompleteDir, incName);
        library.remove(incFile);
        CommonUtils.copyResourceFile(testDirName + "/" + incName, incFile, false);
        URN urn = URN.createSHA1Urn(incompleteHash);
        Set<URN> urns = new HashSet<URN>();
        urns.add(urn);
        vf = injector.getInstance(VerifyingFileFactory.class).createVerifyingFile(252450);
        incompleteFileCollection.addIncompleteFile(incFile, urns, incName, 1981, vf);
        incompleteHashUrl = LimeTestUtils.getRequest("localhost", PORT, incompleteHash);
        
        badHashUrl = LimeTestUtils.getRequest("localhost", PORT, badHash);

        assertEquals(1, incompleteFileCollection.size());
        assertEquals(2, gnutellaFileCollection.size());
        assertEquals("Unexpected uploads in progress", 0, uploadManager.uploadsInProgress());
        assertEquals("Unexpected queued uploads", 0, uploadManager.getNumQueuedUploads());

        //client = new DefaultHttpClient();
        //Scheme https = client.getConnectionManager().getSchemeRegistry().getScheme("https");
        //Scheme tls = new Scheme("tls", https.getSocketFactory(), https.getDefaultPort());
        //client.getConnectionManager().getSchemeRegistry().register(tls);
        HttpConnectionParams.setConnectionTimeout(client.getParams(), 500);
        HttpConnectionParams.setSoTimeout(client.getParams(), 2000);        
    }

    @Override
    public void tearDown() throws Exception {
        stopServices();
        NIOTestUtils.waitForNIO();
        injector = null;
        library = null;
        uploadManager = null;
    }

    private void doSettings() throws UnknownHostException {
        SharingSettings.ADD_ALTERNATE_FOR_SELF.setValue(false);
        FilterSettings.BLACK_LISTED_IP_ADDRESSES
                .set(new String[] { "*.*.*.*" });
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.set(new String[] {
                "127.*.*.*", InetAddress.getLocalHost().getHostAddress() });
        NetworkSettings.PORT.setValue(PORT);
        UploadSettings.HARD_MAX_UPLOADS.setValue(10);
        UploadSettings.UPLOADS_PER_PERSON.setValue(10);
        UploadSettings.MAX_PUSHES_PER_HOST.setValue(9999);

        FilterSettings.FILTER_DUPLICATES.setValue(false);

        ConnectionSettings.NUM_CONNECTIONS.setValue(8);
        ConnectionSettings.CONNECT_ON_STARTUP.setValue(true);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(true);
        UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(true);
    }

    private void startServices() throws Exception {
        HTTPAcceptor httpAcceptor = injector.getInstance(HTTPAcceptor.class);
        httpAcceptor.start();
        
        uploadManager.start();
        
        // make sure the FileDesc objects in file manager are up-to-date
        injector.getInstance(ServiceRegistry.class).initialize();
        FileManagerTestUtils.waitForLoad(library, 4000);
        
        ConnectionDispatcher connectionDispatcher = injector.getInstance(Key.get(ConnectionDispatcher.class, Names.named("global")));
        connectionDispatcher.addConnectionAcceptor(httpAcceptor, false, httpAcceptor.getHttpMethods());
        
        Acceptor acceptor = injector.getInstance(Acceptor.class);        
        acceptor.setListeningPort(PORT);
        acceptor.start();
        
        NIOTestUtils.waitForNIO();
    }       

    private void stopServices() throws Exception {
        Acceptor acceptor = injector.getInstance(Acceptor.class);
        acceptor.setListeningPort(0);
        acceptor.shutdown();
        
        uploadManager.stop();
    }    
    
    public void testHTTP10Download() throws Exception {
        HttpGet method = new HttpGet(fileNameUrl);
        HttpProtocolParams.setVersion(client.getParams(), HttpVersion.HTTP_1_0);
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("abcdefghijklmnopqrstuvwxyz", result);
        } finally {                                
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testHTTP10DownloadRange() throws Exception {
        HttpGet method = new HttpGet(fileNameUrl);
        method.addHeader("Range", "bytes=2-");
        HttpProtocolParams.setVersion(client.getParams(), HttpVersion.HTTP_1_0);
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response.getStatusLine().getStatusCode());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("cdefghijklmnopqrstuvwxyz", result);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testHTTP10DownloadMissingRange() throws Exception {
        HttpGet method = new HttpGet(fileNameUrl);
        method.addHeader("Range", "bytes 2-");
        HttpProtocolParams.setVersion(client.getParams(), HttpVersion.HTTP_1_0);
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response.getStatusLine().getStatusCode());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("cdefghijklmnopqrstuvwxyz", result);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testHTTP10DownloadMiddleRange() throws Exception {
        HttpGet method = new HttpGet(fileNameUrl);
        method.addHeader("Range", "bytes 2-5");
        HttpProtocolParams.setVersion(client.getParams(), HttpVersion.HTTP_1_0);
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response.getStatusLine().getStatusCode());
            assertNotNull(response.getFirstHeader("Content-range"));
            assertEquals("bytes 2-5/26", response.getFirstHeader(
                    "Content-range").getValue());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("cdef", result);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testHTTP10DownloadRangeNoSpace() throws Exception {
        HttpGet method = new HttpGet(fileNameUrl);
        method.addHeader("Range", "bytes 2-5");
        HttpProtocolParams.setVersion(client.getParams(), HttpVersion.HTTP_1_0);
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response.getStatusLine().getStatusCode());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("cdef", result);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testHTTP10DownloadRangeLastByte() throws Exception {
        HttpGet method = new HttpGet(fileNameUrl);
        method.addHeader("Range", "bytes=-5");
        HttpProtocolParams.setVersion(client.getParams(), HttpVersion.HTTP_1_0);
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response.getStatusLine().getStatusCode());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("vwxyz", result);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testHTTP10DownloadRangeTooBigNegative() throws Exception {
        HttpGet method = new HttpGet(fileNameUrl);
        method.addHeader("Range", "bytes=-30");
        HttpProtocolParams.setVersion(client.getParams(), HttpVersion.HTTP_1_0);
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("abcdefghijklmnopqrstuvwxyz", result);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testHTTP10DownloadRangeExtraSpace() throws Exception {
        HttpGet method = new HttpGet(fileNameUrl);
        method.addHeader("Range", "   bytes=  2  -  5 ");
        HttpProtocolParams.setVersion(client.getParams(), HttpVersion.HTTP_1_0);
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response.getStatusLine().getStatusCode());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("cdef", result);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }
    
    public void testHTTP10DownloadHead() throws Exception {
        HttpHead method = new HttpHead(fileNameUrl);
        HttpProtocolParams.setVersion(client.getParams(), HttpVersion.HTTP_1_0);
        method.addHeader("Range", "bytes 2-5");
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response.getStatusLine().getStatusCode());
            assertNotNull(response.getFirstHeader("Content-range"));
            assertEquals("bytes 2-5/26", response.getFirstHeader(
                    "Content-range").getValue());
            assertNull(response.getEntity());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testHTTP11DownloadNoRangeHeader() throws Exception {
        HttpGet method = new HttpGet(fileNameUrl);
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("abcdefghijklmnopqrstuvwxyz", result);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testHTTP11Download() throws Exception {
        HttpGet method = new HttpGet(fileNameUrl);
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("abcdefghijklmnopqrstuvwxyz", result);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testHTTP11DownloadRange() throws Exception {
        HttpGet method = new HttpGet(fileNameUrl);
        method.addHeader("Range", "bytes=2-");
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response.getStatusLine().getStatusCode());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("cdefghijklmnopqrstuvwxyz", result);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testHTTP11DownloadInvalidRange() throws Exception {
        HttpGet method = new HttpGet(fileNameUrl);
        method.addHeader("Range", "bytes=-0");
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_REQUESTED_RANGE_NOT_SATISFIABLE, response.getStatusLine().getStatusCode());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testHTTP11DownloadMissingRange() throws Exception {
        HttpGet method = new HttpGet(fileNameUrl);
        method.addHeader("Range", "bytes 2-");
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response.getStatusLine().getStatusCode());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("cdefghijklmnopqrstuvwxyz", result);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testHTTP11DownloadMiddleRange() throws Exception {
        HttpGet method = new HttpGet(fileNameUrl);
        method.addHeader("Range", "bytes 2-5");
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response.getStatusLine().getStatusCode());
            assertNotNull(response.getFirstHeader("Content-range"));
            assertEquals("bytes 2-5/26", response.getFirstHeader(
                    "Content-range").getValue());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("cdef", result);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testHTTP11DownloadRangeNoSpace() throws Exception {
        HttpGet method = new HttpGet(fileNameUrl);
        method.addHeader(new BasicHeader("Range", "bytes 2-5") {
            @Override
            public String toString() {
                return "Range:bytes 2-5\r\n";
            }
        });
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response.getStatusLine().getStatusCode());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("cdef", result);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testHTTP11DownloadRangeLastByte() throws Exception {
        HttpGet method = new HttpGet(fileNameUrl);
        method.addHeader("Range", "bytes=-5");
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response.getStatusLine().getStatusCode());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("vwxyz", result);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testHTTP11DownloadRangeTooBigNegative() throws Exception {
        HttpGet method = new HttpGet(fileNameUrl);
        method.addHeader("Range", "bytes=-30");
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("abcdefghijklmnopqrstuvwxyz", result);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testHTTP11IncompleteRange() throws Exception {
        Range iv = Range.createRange(2, 6);
        IntervalSet vb = (IntervalSet) PrivilegedAccessor.getValue(vf,
                "verifiedBlocks");
        vb.add(iv);

        HttpGet method = new HttpGet(incompleteHashUrl);
        method.addHeader("Range", "bytes 2-5");
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response.getStatusLine().getStatusCode());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("cdef", result);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
        method = new HttpGet(incompleteHashUrl);
        method.addHeader("Range", "bytes 1-3");
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response.getStatusLine().getStatusCode());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("cd", result);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }

        method = new HttpGet(incompleteHashUrl);
        method.addHeader("Range", "bytes 3-10");
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response.getStatusLine().getStatusCode());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("defg", result);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
        method = new HttpGet(incompleteHashUrl);
        method.addHeader("Range", "bytes 0-20");
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response.getStatusLine().getStatusCode());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("cdefg", result);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testHTTP11PipeliningDownload() throws Exception {
        HttpUploadClient client = new HttpUploadClient();
        try {
            client.connect("localhost", PORT);
            HttpRequest request = new BasicHttpRequest("GET", relativeFileNameUrl);
            client.writeRequest(request);
            client.writeRequest(request);
            client.writeRequest(request);
            HttpResponse response = client.readResponse();
            assertEquals(HttpStatus.SC_OK, response.getStatusLine()
                    .getStatusCode());
            assertEquals("abcdefghijklmnopqrstuvwxyz", client
                    .readBody(response));
            response = client.readResponse();
            assertEquals(HttpStatus.SC_OK, response.getStatusLine()
                    .getStatusCode());
            assertEquals("abcdefghijklmnopqrstuvwxyz", client
                    .readBody(response));
            response = client.readResponse();
            assertEquals(HttpStatus.SC_OK, response.getStatusLine()
                    .getStatusCode());
            assertEquals("abcdefghijklmnopqrstuvwxyz", client
                    .readBody(response));
        } finally {
            client.close();
        }
    }

    /**
     * Makes sure that a HEAD request followed by a GET request does the right
     * thing.
     */
    public void testHTTP11HeadFollowedByGetDownload() throws Exception {
        http11DownloadPersistentHeadFollowedByGet(fileNameUrl);
    }

    /**
     * Tests persistent connections with URI requests. (Raphael Manfredi claimed
     * this was broken.)
     */
    public void testHTTP11DownloadPersistentURI() throws Exception {
        http11DownloadPersistentHeadFollowedByGet(fileNameUrl);
    }

    private void http11DownloadPersistentHeadFollowedByGet(String url)
            throws IOException, URISyntaxException, HttpException, InterruptedException {
        HttpUriRequest method = new HttpHead(url);
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        } finally {
            client.releaseConnection(response);
        }
        method = new HttpGet(url);
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("abcdefghijklmnopqrstuvwxyz", result);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
        method = new HttpGet(url);
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("abcdefghijklmnopqrstuvwxyz", result);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }

        method = new HttpHead(url);
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
        method = new HttpGet(url);
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("abcdefghijklmnopqrstuvwxyz", result);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testLongHeader() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("1234567890");
        }

        HttpGet method = new HttpGet(fileNameUrl);
        method.addHeader("Header", sb.toString());
        HttpResponse response = null;
        try {
            response = client.execute(method);
            fail("Expected remote end to close connection, got: " + response);
        } catch (IOException expected) {
            // expected result
        } finally {
            HttpClientUtils.releaseConnection(response);
        }

        // request a very long filename
        method = new HttpGet("http://localhost:" + PORT + "/" +  sb.toString());
        try {
            response = client.execute(method);
            fail("Expected remote end to close connection, got: " + response);
        } catch (IOException expected) {
            // expected result
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testLongFoldedHeader() throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("1234567890");
        for (int i = 0; i < 1000; i++) {
            sb.append("\n 1234567890");
        }

        HttpGet method = new HttpGet(fileNameUrl);
        method.addHeader("Header", sb.toString());
        HttpResponse response = null;
        try {
            response = client.execute(method);
            fail("Expected remote end to close connection, got: " + response);
        } catch (IOException expected) {
            // expected result
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testManyHeaders() throws Exception {
        HttpGet method = new HttpGet(fileNameUrl);
        for (int i = 0; i < 100; i++) {
            method.addHeader("Header", "abc");
        }
        HttpResponse response = null;
        try {
            response = client.execute(method);
            fail("Expected remote end to close connection, got: " + response);
        } catch (IOException expected) {
            // expected result
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void test10KBRequest() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("1234567890");
        }

        HttpGet method = new HttpGet(fileNameUrl);
        // add headers with a size of 1000 byte each
        for (int i = 0; i < 10; i++) {
            method.addHeader("Header", sb.toString());
        }

        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }
    
    public void test200KBRequest() throws Exception {
        // 4088 byte, leave some space for "Header: "
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 511; i++) {
            sb.append("1234578");
        }

        HttpGet method = new HttpGet(fileNameUrl);
        // add headers with a size of 4 kb each
        // HttpClient will add a few standard headers, so we can't add the full 50 headers
        for (int i = 0; i < 40; i++) {
            method.addHeader("Header", sb.toString());
        }

        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }

    }
    
    public void testHeaderLengthThatMatchesCharacterBufferLength()
            throws Exception {
        HttpGet method = new HttpGet(fileNameUrl);
        StringBuilder sb = new StringBuilder(512);
        for (int i = 0; i < 512 - 3; i++) {
            sb.append("a");
        }
        method.addHeader("A", sb.toString());
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testInvalidCharactersInRequest() throws Exception {
        // build request with non US-ASCII characters
        String url = new String(new char[] { 0x47, 0x72, 0xFC, 0x65, 0x7A,
                0x69, 0x5F, 0x7A, 0xE4, 0x6D, 0xE4 });

        // use home grown test client since HttpClient will not send theses
        // characters in the first place
        HttpUploadClient client = new HttpUploadClient();
        try {
            client.connect("localhost", PORT);
            HttpRequest request = new BasicHttpRequest("GET", "/get/0/" + url);
            client.writeRequest(request);
            HttpResponse response = client.readResponse();
            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
            // this test used to expect a closed stream but LimeWire now accepts 
            // non US-ASCII characters: CORE-225 
//            InputStream in = client.getSocket().getInputStream();
//            assertEquals("Expected remote end to close socket", -1, in.read());
        } finally {
            client.close();
        }
    }

    public void testInvalidCharactersInHeader() throws Exception {
        final String value = new String(new char[] { 0x31 + 0x32 + 0x38 + 0x2C
                + 0xE9 + 0x5E + 0xB0 + 0xE7 + 0x88 + 0x76 + 0xF1 + 0xCA + 0x51
                + 0x8F + 0x6D + 0xBF + 0xC8 + 0xAA + 0x82 + 0x6C + 0x33 + 0x35
                + 0x25 + 0x25 + 0x86 + 0xCC + 0xBF + 0x7E + 0xC5 + 0xEE + 0x58
                + 0x96 + 0xB6 + 0x2D + 0x7E + 0x9E + 0x21 + 0xD5 });
        
        // use home grown test client since HttpClient will not send theses
        // characters in the first place
        HttpUploadClient client = new HttpUploadClient();
        try {
            client.connect("localhost", PORT);
            HttpRequest request = new BasicHttpRequest("GET", "/uri-res/N2R?" + hash);
            request.addHeader("FP-1a: ", value);
            client.writeRequest(request);
            HttpResponse response = client.readResponse();
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        } finally {
            client.close();
        }
    }
    
    public void testFreeloader() throws Exception {
        HttpGet method = new HttpGet(fileNameUrl);
        method.setHeader("User-Agent", "Mozilla");
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            String body = result;
            if (!body.startsWith("<html>")) {
                fail("Expected free loader page, got: " + body);
            }
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    /**
     * Tests the case of requests for different file over the same HTTP session.
     */
    public void testMultipleUploadSession() throws Exception {
        HttpGet method = new HttpGet(fileNameUrl);
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("abcdefghijklmnopqrstuvwxyz", result);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
        assertConnectionIsOpen(true, method);

        method = new HttpGet(otherFileNameUrl);
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("ABCDEFGHIJKLMNOPQRSTUVWXYZ\n", result);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testIncompleteFileUpload() throws Exception {
        HttpGet method = new HttpGet(incompleteHashUrl);
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_REQUESTED_RANGE_NOT_SATISFIABLE,
                    response.getStatusLine().getStatusCode());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }

        assertConnectionIsOpen(true, method);

        method = new HttpGet(incompleteHashUrl);
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_REQUESTED_RANGE_NOT_SATISFIABLE,
                    response.getStatusLine().getStatusCode());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testIncompleteFileWithRanges() throws Exception {
        // add a range to the incomplete file.
        Range iv = Range.createRange(50, 102500);
        IntervalSet vb = (IntervalSet) PrivilegedAccessor.getValue(vf,
                "verifiedBlocks");
        vb.add(iv);

        HttpGet method = new HttpGet(incompleteHashUrl);
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_REQUESTED_RANGE_NOT_SATISFIABLE,
                    response.getStatusLine().getStatusCode());
            assertNotNull(response.getFirstHeader("X-Available-Ranges"));
            assertEquals("bytes 50-102499", response.getFirstHeader(
                    "X-Available-Ranges").getValue());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }

        assertConnectionIsOpen(true, method);

        // add another range and make sure we display it.
        iv = Range.createRange(150050, 252450);
        vb.add(iv);
        method = new HttpGet(incompleteHashUrl);
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_REQUESTED_RANGE_NOT_SATISFIABLE,
                    response.getStatusLine().getStatusCode());
            assertNotNull(response.getFirstHeader("X-Available-Ranges"));
            assertEquals("bytes 50-102499, 150050-252449", response
                    .getFirstHeader("X-Available-Ranges").getValue());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }

        assertConnectionIsOpen(true, method);

        // add an interval too small to report and make sure we don't report
        iv = Range.createRange(102505, 150000);
        vb.add(iv);
        method = new HttpGet(incompleteHashUrl);
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_REQUESTED_RANGE_NOT_SATISFIABLE,
                    response.getStatusLine().getStatusCode());
            assertNotNull(response.getFirstHeader("X-Available-Ranges"));
            assertEquals("bytes 50-102499, 150050-252449", response
                    .getFirstHeader("X-Available-Ranges").getValue());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }

        assertConnectionIsOpen(true, method);

        // add the glue between the other intervals and make sure we condense
        // the ranges into a single larger range.
        iv = Range.createRange(102500, 102505);
        vb.add(iv);
        iv = Range.createRange(150000, 150050);
        vb.add(iv);
        method = new HttpGet(incompleteHashUrl);
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_REQUESTED_RANGE_NOT_SATISFIABLE,
                    response.getStatusLine().getStatusCode());
            assertNotNull(response.getFirstHeader("X-Available-Ranges"));
            assertEquals("bytes 50-252449", response.getFirstHeader(
                    "X-Available-Ranges").getValue());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testIncompleteFileWithRangeRequest() throws Exception {
        HttpGet method = new HttpGet(incompleteHashUrl);
        method.addHeader("Range", "bytes 20-40");
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_REQUESTED_RANGE_NOT_SATISFIABLE,
                    response.getStatusLine().getStatusCode());
            assertEquals("Requested Range Unavailable", response.getStatusLine().getReasonPhrase());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }

        assertConnectionIsOpen(true, method);
    }

    public void testHTTP11WrongURI() throws Exception {
        HttpGet method = new HttpGet(badHashUrl);
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusLine().getStatusCode());
            assertEquals("Not Found", response.getStatusLine().getReasonPhrase());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }

        assertConnectionIsOpen(true, method);
    }

    public void testHTTP10WrongURI() throws Exception {
        HttpGet method = new HttpGet(badHashUrl);
        HttpProtocolParams.setVersion(client.getParams(), HttpVersion.HTTP_1_0);
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusLine().getStatusCode());
            assertEquals("Not Found", response.getStatusLine().getReasonPhrase());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }

        assertConnectionIsOpen(true, method);
    }

    public void testHTTP11MalformedURI() throws Exception {
        HttpGet method = new HttpGet("http://localhost:" + PORT + "/uri-res/N2R?" + "no%20more%20school");
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
            assertEquals("Malformed Request", response.getStatusLine().getReasonPhrase());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }

        assertConnectionIsOpen(false, method);
    }

    public void testHTTP10MalformedURI() throws Exception {
        HttpGet method = new HttpGet("http://localhost:" + PORT + "/uri-res/N2R?" + "%20more%20school");
        HttpProtocolParams.setVersion(client.getParams(), HttpVersion.HTTP_1_0);
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
            assertEquals("Malformed Request", response.getStatusLine().getReasonPhrase());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }

        assertConnectionIsOpen(false, method);
    }

    public void testHTTP11MalformedGet() throws Exception {
        HttpGet method = new HttpGet("http://localhost:" + PORT + "/get/some/dr/pepper");
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
            assertEquals("Bad Request", response.getStatusLine().getReasonPhrase());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }

        assertConnectionIsOpen(false, method);
    }

    public void testHTTP11MalformedHeader() throws Exception {
        HttpGet method = new HttpGet(fileNameUrl);
        method.addHeader("Range", "2-5");
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
            assertEquals("Malformed Request", response.getStatusLine().getReasonPhrase());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }

        assertConnectionIsOpen(false, method);
    }

    public void testHTTP11Post() throws Exception {
        HttpPost method = new HttpPost(fileNameUrl);
        HttpResponse response = null;
        try {
            response = client.execute(method);
            fail("Expected HttpConnection due to connection close, got: " + response);
        } catch (IOException expected) {
            // the connection was either closed or timed out due to a failed TLS
            // handshake
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
        assertConnectionIsOpen(false, method);
    }

    public void testHTTP11ExpectContinue() throws Exception {
        HttpPost method = new HttpPost(fileNameUrl) {
            @Override
            public String getMethod() {
                return "GET";
             }
        };
        // TODO method.setUseExpectHeader(true);
        method.setEntity(new NStringEntity("Foo"));
        HttpResponse response = null;
        try {
            response = client.execute(method);
            // since this is actually a GET request and not a POST 
            // the Expect header should be ignored
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        } catch (ClientProtocolException expected) {
            // expected result
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    /**
     * Test that creation time header is returned.
     */
    public void testCreationTimeHeaderReturned() throws Exception {
        // assert that creation time exists
        URN urn = URN.createSHA1Urn(hash);
        Long creationTime = injector.getInstance(CreationTimeCache.class).getCreationTime(urn);
        assertNotNull(creationTime);
        assertTrue(creationTime.longValue() > 0);

        HttpGet method = new HttpGet(fileNameUrl);
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            assertNotNull(response.getFirstHeader("X-Create-Time"));
            assertEquals(creationTime + "", response.getFirstHeader(
                    "X-Create-Time").getValue());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testCreationTimeHeaderReturnedForIncompleteFile()
            throws Exception {
        Range iv = Range.createRange(2, 5);
        IntervalSet vb = (IntervalSet) PrivilegedAccessor.getValue(vf,
                "verifiedBlocks");
        vb.add(iv);

        URN urn = URN.createSHA1Urn(incompleteHash);
        Long creationTime = new Long("10776");
        injector.getInstance(CreationTimeCache.class).addTime(urn, creationTime.longValue());

        HttpGet method = new HttpGet(incompleteHashUrl);
        method.addHeader("Range", "bytes 2-5");
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_PARTIAL_CONTENT, response.getStatusLine().getStatusCode());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("cdef", result);
            assertNotNull(response.getFirstHeader("X-Create-Time"));
            assertEquals(creationTime + "", response.getFirstHeader(
                    "X-Create-Time").getValue());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    /**
     * LimeWire doesn't send chat support feature header since version 5.0. 
     */
    public void testChatFeatureHeaderNotSent() throws Exception {
        HttpGet method = new HttpGet(fileNameUrl);
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            assertNotNull(response.getFirstHeader("X-Features"));
            String header = response.getFirstHeader("X-Features").getValue();
            assertTrue(header.contains("fwalt/0.1"));
            assertTrue(header.contains("browse/1.0"));
            assertFalse(header.contains("chat/0.1"));
        } finally {
            HttpClientUtils.releaseConnection(response);
        }

        assertConnectionIsOpen(true, method);

        method = new HttpGet(fileNameUrl);
        method.addHeader("Connection", "close");
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            assertNull(method.getFirstHeader("X-Features"));
        } finally {
            HttpClientUtils.releaseConnection(response);
        }

        assertConnectionIsOpen(false, method);

        // try a new connection
        method = new HttpGet(fileNameUrl);
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            assertNotNull(response.getFirstHeader("X-Features"));
            String header = response.getFirstHeader("X-Features").getValue();
            assertTrue(header.contains("fwalt/0.1"));
            assertTrue(header.contains("browse/1.0"));
            assertFalse(header.contains("chat/0.1"));
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testThexHeader() throws Exception {
        HashTreeCache tigerTreeCache = injector.getInstance(HashTreeCache.class);
        HashTree tree = getThexTree(tigerTreeCache);

        HttpGet method = new HttpGet(fileNameUrl);
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            assertNotNull(response.getFirstHeader("X-Thex-URI"));
            String header = response.getFirstHeader("X-Thex-URI").getValue();
            assertEquals("/uri-res/N2X?" + hash + ";" + tree.getRootHash(), header);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testDownloadFromBitprintUrl() throws Exception {
        HashTreeCache tigerTreeCache = injector.getInstance(HashTreeCache.class);
        HashTree tree = getThexTree(tigerTreeCache);

        HttpGet method = new HttpGet("http://localhost:" + PORT + "/uri-res/N2R?urn:bitprint:"
                + baseHash + "." + tree.getRootHash());
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("abcdefghijklmnopqrstuvwxyz", result);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }

        // the request is checked for a valid bitprint length
        method = new HttpGet("http://localhost:" + PORT + "/uri-res/N2R?urn:bitprint:" + baseHash + "."
                + "asdoihffd");
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }

        // but not for the valid base32 root -- in the future we may
        // and this test will break
        method = new HttpGet("http://localhost:" + PORT + "/uri-res/N2R?urn:bitprint:" + baseHash + "."
                + "SAMUWJUUSPLMMDUQZOWX32R6AEOT7NCCBX6AGBI");
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String result;
            if (response.getEntity() != null) {
                result = EntityUtils.toString(response.getEntity());
            } else {
                result = null;
            }
            assertEquals("abcdefghijklmnopqrstuvwxyz", result);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }

        // make sure "bitprint:" is required for bitprint uploading.
        method = new HttpGet("http://localhost:" + PORT + "/uri-res/N2R?urn:sha1:" + baseHash + "."
                + tree.getRootHash());
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }
    
    public void testBadGetTreeRequest() throws Exception {
        HttpGet method = new HttpGet(badHashUrl);
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusLine().getStatusCode());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }

        method = new HttpGet("http://localhost:" + PORT + "/uri-res/N2X?" + "no_hash");
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testGetTree() throws Exception {
        HashTreeCache tigerTreeCache = injector.getInstance(HashTreeCache.class);
        HashTree tree = getThexTree(tigerTreeCache);

        HttpGet method = new HttpGet("http://localhost:" + PORT + "/uri-res/N2X?" + hash);
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            DIMEParser parser = new DIMEParser(response.getEntity().getContent());
            parser.nextRecord(); // xml
            DIMERecord record = parser.nextRecord();
            assertFalse(parser.hasNext());
            List<List<byte[]>> allNodes = HashTreeUtils.createAllParentNodes(tree.getNodes(), new Tiger());
            byte[] data = record.getData();
            int offset = 0;
            for (Iterator<List<byte[]>> genIter = allNodes.iterator(); genIter
                    .hasNext();) {
                for (Iterator<byte[]> it = genIter.next().iterator(); it
                        .hasNext();) {
                    byte[] current = it.next();
                    for (int j = 0; j < current.length; j++) {
                        assertEquals("offset: " + offset + ", idx: " + j,
                                current[j], data[offset++]);
                    }
                }
            }
            assertEquals(data.length, offset);
            // more extensive validity checks are in HashTreeTest
            // this is just checking to make sure we sent the right tree.
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testGetNonExistingTree() throws Exception {
        HashTreeCache tigerTreeCache = injector.getInstance(HashTreeCache.class);
        
        URN urn = URN.createSHA1Urn(hash);
        tigerTreeCache.purgeTree(urn);
        HttpGet method = new HttpGet("http://localhost:" + PORT + "/uri-res/N2X?" + hash);
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusLine().getStatusCode());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testDownloadChangedFile() throws Exception {
        // modify shared file and make sure it gets new timestamp
        Thread.sleep(1000);
        
        FileDesc fd = gnutellaFileView.getFileDesc(URN.createSHA1Urn(hash));
        fd.getFile().setLastModified(System.currentTimeMillis());
        assertNotEquals(fd.getFile().lastModified(), fd.lastModified());
        final File file = fd.getFile();

        // catch notification when file is reshared
        final List<FileViewChangeEvent> changes = new CopyOnWriteArrayList<FileViewChangeEvent>();
        final CountDownLatch latch = new CountDownLatch(2);
        EventListener<FileViewChangeEvent> listener = new EventListener<FileViewChangeEvent>() {
            public void handleEvent(FileViewChangeEvent event) {
                changes.add(event);
                
                if(event.getFile().equals(file)) {
                    if(changes.size() == 1 && event.getType() == FileViewChangeEvent.Type.FILE_REMOVED) {
                        latch.countDown();
                    } else if(changes.size() == 2 && event.getType() == FileViewChangeEvent.Type.FILE_ADDED) {
                        latch.countDown();
                    }
                }
            }            
        };
        try {
            gnutellaFileView.addListener(listener);

            HttpGet method = new HttpGet(LimeTestUtils.getRequest("localhost", PORT, fd.getSHA1Urn()));
            HttpResponse response = null;
            try {
                response = client.execute(method);
                assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusLine().getStatusCode());
            } finally {
                HttpClientUtils.releaseConnection(response);
            }

            assertTrue("didn't get right changes, got: " + changes, latch.await(500, TimeUnit.MILLISECONDS));

            fd = gnutellaFileView.getFileDesc(URN.createSHA1Urn(hash));
            assertNotNull(fd);
            method = new HttpGet(LimeTestUtils.getRequest("localhost", PORT, fd.getSHA1Urn()));
            try {
                response = client.execute(method);
                assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
                String result;
                if (response.getEntity() != null) {
                    result = EntityUtils.toString(response.getEntity());
                } else {
                    result = null;
                }
                assertEquals("abcdefghijklmnopqrstuvwxyz", result);
            } finally {
                HttpClientUtils.releaseConnection(response);
            }
        } finally {
            gnutellaFileView.removeListener(listener);
        }
        
    }

    private void assertConnectionIsOpen(boolean open, HttpUriRequest request) throws InterruptedException, ConnectionPoolTimeoutException {
        URI uri = request.getURI();
        HttpRoute route = new HttpRoute(new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme()));
        ManagedClientConnection connection = client.getConnectionManager().requestConnection(route, null).getConnection(0, null);
        assertEquals(open, connection.isOpen());
        client.getConnectionManager().releaseConnection(connection, -1, null);
    }

    private HashTree getThexTree(HashTreeCache tigerTreeCache) throws Exception {
        FileDesc fd = gnutellaFileView.getFileDesc(URN.createSHA1Urn(hash));
        return ((HashTreeCacheImpl)tigerTreeCache).getHashTreeAndWait(fd, 1000);
    }

}
