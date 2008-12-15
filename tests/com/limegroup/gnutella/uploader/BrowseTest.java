package com.limegroup.gnutella.uploader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.params.HttpProtocolParams;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.NetworkSettings;
import org.limewire.http.httpclient.HttpClientUtils;
import org.limewire.http.httpclient.LimeHttpClient;
import org.limewire.util.TestUtils;

import com.google.inject.Injector;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.library.FileManagerTestUtils;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.util.LimeTestCase;

/**
 * Test that a client uploads a list of files correctly.
 */
public class BrowseTest extends LimeTestCase {

    private final int PORT = 6668;

    private HttpClient client;

    protected String protocol;

    private FileManager fileManager;

    private MessageFactory messageFactory;
    
    private String host;
    
    public BrowseTest(String name) {
        super(name);
        protocol = "http";
    }

    public static Test suite() {
        return buildTestSuite(BrowseTest.class);
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }

    @Override
    protected void setUp() throws Exception {
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        NetworkSettings.PORT.setValue(PORT);

        Injector injector = LimeTestUtils.createInjectorAndStart();
        
        fileManager = injector.getInstance(FileManager.class);
        messageFactory = injector.getInstance(MessageFactory.class);
        client = injector.getInstance(LimeHttpClient.class);
        
        FileManagerTestUtils.waitForLoad(fileManager,2000);
        File shareDir = TestUtils.getResourceFile("com/limegroup/gnutella");
        File[] testFiles = shareDir.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return !file.isDirectory() && file.getName().endsWith(".class");
            }
        });
        assertGreaterThan("Not enough files to test against", 50, testFiles.length);
        for(File file : testFiles) {
            assertNotNull(fileManager.getGnutellaFileList().add(file).get(1, TimeUnit.SECONDS));
        }
        
        host = protocol + "://localhost:" + PORT;
    }

    public void testBrowse() throws Exception {
        HttpGet method = new HttpGet(host + "/");
        method.addHeader("Accept", "application/x-gnutella-packets");
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

            InputStream in = new BufferedInputStream(response.getEntity().getContent());
            List<String> files = new ArrayList<String>();
            while (true) {
                Message m;
                try {
                    m = messageFactory.read(in, Network.TCP);
                } catch (IOException e) {
                    // no other way to tell if we have received all messages
                    if (!"Connection closed.".equals(e.getMessage())) {
                        throw e;
                    }
                    break;
                }
                assertTrue(m instanceof QueryReply);
                QueryReply q = (QueryReply) m;
                Response[] results = q.getResultsArray();
                for (Response result : results) {
                    files.add(result.getName());
                    assertTrue("Expected .class or LimeWire file, got: " + result.getName(),
                            result.getName().endsWith(".class") || result.getName().toLowerCase().startsWith("limewire"));
                }
            }

            assertEquals(fileManager.getGnutellaFileList().size(), files.size());
            fileManager.getGnutellaFileList().getReadLock().lock();
            try {
                for(FileDesc result : fileManager.getGnutellaFileList()) {
                    boolean contained = files.remove(result.getFileName());
                    assertTrue("File is missing in browse response: " + result.getFileName(), contained);
                }
            } finally {
                fileManager.getGnutellaFileList().getReadLock().unlock();
            }
            assertTrue("Browse returned more results than shared: " + files, files.isEmpty());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testBrowseHead() throws Exception {
        HttpHead method = new HttpHead(host + "/");
        method.addHeader("Accept", "application/x-gnutella-packets");
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            assertNull(response.getEntity());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testBrowseNoAcceptHeader() throws Exception {
        HttpUriRequest method = new HttpGet(host + "/");
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_NOT_ACCEPTABLE, response.getStatusLine().getStatusCode());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }

        method = new HttpHead(host + "/");
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_NOT_ACCEPTABLE, response.getStatusLine().getStatusCode());
            assertNull(response.getEntity());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testBrowseNoAcceptHeaderHttp10() throws Exception {
        HttpUriRequest method = new HttpGet(host + "/");
        HttpProtocolParams.setVersion(client.getParams(), HttpVersion.HTTP_1_1);
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_NOT_ACCEPTABLE, response.getStatusLine().getStatusCode());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }

        method = new HttpHead(host + "/");
        HttpProtocolParams.setVersion(client.getParams(), HttpVersion.HTTP_1_1);
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_NOT_ACCEPTABLE, response.getStatusLine().getStatusCode());
            assertNull(response.getEntity());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

}
