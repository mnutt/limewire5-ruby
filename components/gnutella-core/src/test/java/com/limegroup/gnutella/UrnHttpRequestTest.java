package com.limegroup.gnutella;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHttpRequest;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.UploadSettings;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.util.FileUtils;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.http.HttpTestUtils;
import com.limegroup.gnutella.library.FileCollection;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.FileManagerTestUtils;
import com.limegroup.gnutella.library.FileView;
import com.limegroup.gnutella.library.GnutellaFiles;
import com.limegroup.gnutella.library.Library;

/**
 * This class tests HTTP requests involving URNs, as specified in HUGE v094,
 * utilizing the X-Gnutella-Content-URN header and the
 * X-Gnutella-Alternate-Location header.
 */
public final class UrnHttpRequestTest extends LimeTestCase {

    private static final String STATUS_503 = "HTTP/1.1 503 Service Unavailable";

    private static final String STATUS_404 = "HTTP/1.1 404 Not Found";
    private static final String STATUS_400 = "HTTP/1.1 400 Bad Request";

   @Inject private Library library;
   @Inject @GnutellaFiles private FileView gnutellaFileView;

    private HTTPUploadManager uploadManager;

    @Inject private LifecycleManager lifeCycleManager;
    
    @Inject private NetworkManager networkManager;
    @Inject @GnutellaFiles private FileCollection gnutellaFileCollection;

    public UrnHttpRequestTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(UrnHttpRequestTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    @Override
    protected void setUp() throws Exception {
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);

        // initialize services
        Injector injector = LimeTestUtils.createInjector(Stage.PRODUCTION, LimeTestUtils.createModule(this));

        uploadManager = (HTTPUploadManager) injector.getInstance(UploadManager.class);
        lifeCycleManager.start();
        
        // make sure the FileDesc objects in file manager are up-to-date
        FileManagerTestUtils.waitForLoad(library,2000);

        // create shared files with random content
        Random random = new Random();
        for (int i = 0; i < 5; i++) {
            byte[] data = new byte[random.nextInt(255) + 1];
            random.nextBytes(data);
            File file = new File(_scratchDir, "file" + i + ".tmp");
            FileUtils.writeObject(file, data);
            file.deleteOnExit();
            assertNotNull(gnutellaFileCollection.add(file).get(1, TimeUnit.SECONDS));
        }
        
        assertGreaterThanOrEquals("FileManager should have loaded files", 5, gnutellaFileCollection.size());
    }

    @Override
    protected void tearDown() throws Exception {
        lifeCycleManager.shutdown();
    }

    /**
     * Tests requests that follow the traditional "get" syntax to make sure that
     * the X-Gnutella-Content-URN header is always returned.
     */
    public void testLimitReachedRequests() throws Exception {
        int maxUploads = UploadSettings.HARD_MAX_UPLOADS.getValue();
        UploadSettings.HARD_MAX_UPLOADS.setValue(0);

        try {
            for (FileDesc fd: gnutellaFileView) {
                String uri = LimeTestUtils.getRelativeRequest(fd.getSHA1Urn());

                BasicHttpRequest request = new BasicHttpRequest("GET", uri,
                        HttpVersion.HTTP_1_1);
                request.addHeader(HTTPHeaderName.GNUTELLA_CONTENT_URN.create(fd
                        .getSHA1Urn()));

                sendRequestThatShouldFail(request, STATUS_503);
                // sendRequestThatShouldFail(HTTPRequestMethod.HEAD, request,
                // fd,
                // STATUS_503);
            }
        } finally {
            UploadSettings.HARD_MAX_UPLOADS.setValue(maxUploads);
        }
    }

    /**
     * Test requests by URN.
     */
    public void testHttpUrnRequest() throws Exception {
        for (FileDesc fd : gnutellaFileView) {
            String uri = "/uri-res/N2R?" + fd.getSHA1Urn().httpStringValue();

            BasicHttpRequest request = new BasicHttpRequest("GET", uri,
                    HttpVersion.HTTP_1_1);
            sendRequestThatShouldSucceed(request, fd);

            request = new BasicHttpRequest("HEAD", uri, HttpVersion.HTTP_1_1);
            sendRequestThatShouldSucceed(request, fd);
        }
    }

    /**
     * Test requests by URN that came from LimeWire 2.8.6.
     * /get/0//uri-res/N2R?urn:sha1:AZUCWY54D63______PHN7VSVTKZA3YYT HTTP/1.1
     */
    public void testMalformedHttpUrnRequest() throws Exception {
        for (FileDesc fd : gnutellaFileView) {
            String uri = "/get/0//uri-res/N2R?"
                    + fd.getSHA1Urn().httpStringValue();

            BasicHttpRequest request = new BasicHttpRequest("GET", uri,
                    HttpVersion.HTTP_1_1);
            sendRequestThatShouldFail(request, STATUS_400);

            request = new BasicHttpRequest("HEAD", uri, HttpVersion.HTTP_1_1);
            sendRequestThatShouldFail(request, STATUS_400);
        }
    }

    /**
     * Tests requests that follow the traditional "get" syntax to make sure that
     * the X-Gnutella-Content-URN header is always returned.
     */
    public void testTraditionalGetForReturnedUrn() throws Exception {
        for (FileDesc fd : gnutellaFileView) {
            String uri = LimeTestUtils.getRelativeRequest(fd.getSHA1Urn());

            BasicHttpRequest request = new BasicHttpRequest("GET", uri,
                    HttpVersion.HTTP_1_1);
            request.addHeader(HTTPHeaderName.GNUTELLA_CONTENT_URN.create(fd
                    .getSHA1Urn()));
            sendRequestThatShouldSucceed(request, fd);

            request = new BasicHttpRequest("HEAD", uri, HttpVersion.HTTP_1_1);
            request.addHeader(HTTPHeaderName.GNUTELLA_CONTENT_URN.create(fd
                    .getSHA1Urn()));
            sendRequestThatShouldSucceed(request, fd);
        }
    }

    /**
     * Tests requests that follow the traditional "get" syntax but that also
     * include the X-Gnutella-Content-URN header. In these requests, both the
     * URN and the file name and index are correct, so a valid result is
     * expected.
     */
    public void testTraditionalGetWithContentUrn() throws Exception {
        for (FileDesc fd : gnutellaFileView) {
            String uri = LimeTestUtils.getRelativeRequest(fd.getSHA1Urn());

            BasicHttpRequest request = new BasicHttpRequest("GET", uri,
                    HttpVersion.HTTP_1_1);
            sendRequestThatShouldSucceed(request, fd);

            request = new BasicHttpRequest("HEAD", uri, HttpVersion.HTTP_1_1);
            sendRequestThatShouldSucceed(request, fd);
        }
    }

    /**
     * Tests get requests that follow the traditional Gnutella get format and
     * that include an invalid content URN header -- these should fail with
     * error code 404.
     */
    public void testTraditionalGetWithInvalidContentUrn() throws Exception {
        for (FileDesc fd : gnutellaFileView) {
            String uri = LimeTestUtils.getRelativeRequest(fd.getSHA1Urn());

            BasicHttpRequest request = new BasicHttpRequest("GET", uri,
                    HttpVersion.HTTP_1_1);
            request.addHeader(HTTPHeaderName.GNUTELLA_CONTENT_URN
                    .create("urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB"));
            sendRequestThatShouldFail(request, STATUS_404);

            request = new BasicHttpRequest("HEAD", uri, HttpVersion.HTTP_1_1);
            request.addHeader(HTTPHeaderName.GNUTELLA_CONTENT_URN
                    .create("urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB"));
            sendRequestThatShouldFail(request, STATUS_404);
        }
    }

    /**
     * Tests to make sure that invalid traditional Gnutella get requests with
     * matching X-Gnutella-Content-URN header values also fail with 404.
     */
    public void testInvalidTraditionalGetWithValidContentUrn() throws Exception {
        for (FileDesc fd : gnutellaFileView) {
            String uri = LimeTestUtils.getRelativeRequest(fd.getSHA1Urn());
            uri = uri.substring(0, uri.length() - 2)+ "xx";
            
            BasicHttpRequest request = new BasicHttpRequest("GET", uri,
                    HttpVersion.HTTP_1_1);
            sendRequestThatShouldFail(request, STATUS_404);

            request = new BasicHttpRequest("HEAD", uri, HttpVersion.HTTP_1_1);
            sendRequestThatShouldFail(request, STATUS_404);
        }
    }

    /**
     * Sends an HTTP request that should succeed and send back all of the
     * expected headers.
     */
    private void sendRequestThatShouldSucceed(HttpRequest request, FileDesc fd)
            throws Exception {
        HttpResponse response = getResponse(request);
        assertEquals(200, response.getStatusLine().getStatusCode());

        // clean up any created uploaders
        uploadManager.cleanup();

        boolean contentUrnHeaderPresent = false;
        Header[] headers = response.getAllHeaders();
        assertTrue("HTTP response headers should be present: " + fd,
                headers.length > 0);
        for (Header header : headers) {
            String curString = header.toString();
            if (HTTPHeaderName.ALT_LOCATION.matchesStartOfString(curString)) {
                continue;
            } else if (HTTPHeaderName.GNUTELLA_CONTENT_URN
                    .matchesStartOfString(curString)) {
                URN curUrn = null;
                try {
                    String tmpString = HttpTestUtils.extractHeaderValue(curString);
                    curUrn = URN.createSHA1Urn(tmpString);
                } catch (IOException e) {
                    assertTrue("unexpected exception: " + e, false);
                }
                assertEquals(HTTPHeaderName.GNUTELLA_CONTENT_URN.toString()
                        + "s should be equal for " + fd, fd.getSHA1Urn(),
                        curUrn);
                contentUrnHeaderPresent = true;
            } else if (HTTPHeaderName.CONTENT_RANGE
                    .matchesStartOfString(curString)) {
                continue;
            } else if (HTTPHeaderName.CONTENT_TYPE
                    .matchesStartOfString(curString)) {
                continue;
            } else if (HTTPHeaderName.CONTENT_LENGTH
                    .matchesStartOfString(curString)) {
                String value = HttpTestUtils.extractHeaderValue(curString);
                assertEquals("sizes should match for " + fd, (int) fd
                        .getFileSize(), Integer.parseInt(value));
            } else if (HTTPHeaderName.SERVER.matchesStartOfString(curString)) {
                continue;
            }
        }
        assertTrue("content URN header should always be reported:\r\n" + fd
                + "\r\n" + "reply: " + response, contentUrnHeaderPresent);
    }

    /**
     * Sends an HTTP request that should fail if everything is working
     * correctly.
     */
    private void sendRequestThatShouldFail(HttpRequest request, String error)
            throws Exception {
        HttpResponse response = getResponse(request);
        assertEquals("unexpected HTTP response", error, getStatusLine(response));
    }

    private String getStatusLine(HttpResponse response) {
        return response.getProtocolVersion() + " "
                + response.getStatusLine().getStatusCode() + " "
                + response.getStatusLine().getReasonPhrase();
    }
    
    private HttpResponse getResponse(HttpRequest request) throws HttpException, IOException, InterruptedException {
        HttpClient client = new DefaultHttpClient();
        return client.execute(new HttpHost("127.0.0.1", networkManager.getPort(), "http"), request);
    }

}
