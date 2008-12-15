package com.limegroup.gnutella.uploader;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.NetworkSettings;
import org.limewire.http.httpclient.HttpClientUtils;
import org.limewire.io.LocalSocketAddressProvider;
import org.limewire.net.ConnectionDispatcher;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import com.limegroup.gnutella.Acceptor;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.HTTPAcceptor;
import com.limegroup.gnutella.HTTPUploadManager;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.Uploader;
import com.limegroup.gnutella.Uploader.UploadStatus;
import com.limegroup.gnutella.library.FileDescStub;
import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.library.FileManagerStub;
import com.limegroup.gnutella.library.GnutellaFileListStub;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.stubs.LocalSocketAddressProviderStub;
import com.limegroup.gnutella.util.LimeTestCase;

public class HTTPUploaderTest extends LimeTestCase {

    private static final int PORT = 6668;

    private static MyActivityCallback cb;

    private HTTPAcceptor httpAcceptor;

    private HttpClient client;

    private URN urn1;

    private FileManagerStub fm;

    private HTTPUploadManager uploadManager;

    private FileDescStub fd1;

    private Acceptor acceptor;
    
    private String host;

    public HTTPUploaderTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(HTTPUploaderTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        NetworkSettings.PORT.setValue(PORT);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);


        final LocalSocketAddressProviderStub localSocketAddressProvider = new LocalSocketAddressProviderStub();
        localSocketAddressProvider.setTLSCapable(true);
        Injector injector = LimeTestUtils.createInjector(MyActivityCallback.class, new AbstractModule() {
            @Override
            protected void configure() {
                bind(FileManager.class).to(FileManagerStub.class);
                bind(LocalSocketAddressProvider.class).toInstance(localSocketAddressProvider);
            } 
        });        

        fm = (FileManagerStub) injector.getInstance(FileManager.class);
        GnutellaFileListStub sharedList = fm.getGnutellaFileList();
        urn1 = URN.createSHA1Urn("urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFG");
        fd1 = new FileDescStub("abc1.txt", urn1, 0);
        sharedList.add(fd1);

        cb = (MyActivityCallback) injector.getInstance(ActivityCallback.class);

        acceptor = injector.getInstance(Acceptor.class);
        httpAcceptor = injector.getInstance(HTTPAcceptor.class);
        uploadManager = injector.getInstance(HTTPUploadManager.class);

        acceptor.setListeningPort(PORT);
        acceptor.start();
        
        httpAcceptor.start();
        uploadManager.start();

        ConnectionDispatcher connectionDispatcher = injector.getInstance(Key.get(ConnectionDispatcher.class, Names.named("global")));
        connectionDispatcher.addConnectionAcceptor(httpAcceptor, false, httpAcceptor.getHttpMethods());

        client = new DefaultHttpClient();
        host = "http://localhost:" + PORT;
    }

    @Override
    protected void tearDown() throws Exception {
        uploadManager.stop();
        httpAcceptor.stop();

        acceptor.setListeningPort(0);
        acceptor.shutdown();

        LimeTestUtils.waitForNIO();
    }

    public void testChatAndBrowseEnabled() throws Exception {
        HttpGet method = new HttpGet(host + "/uri-res/N2R?" + urn1);
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            assertEquals(1, cb.uploads.size());
            HTTPUploader uploader = (HTTPUploader) cb.uploads.get(0);
            assertFalse(uploader.isChatEnabled());
            assertFalse(uploader.isBrowseHostEnabled());
            assertEquals("127.0.0.1", uploader.getHost());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }

        method = new HttpGet(host + "/uri-res/N2R?" + urn1);
        method.addHeader("X-Features", "chat/0.1");
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            assertEquals(1, cb.uploads.size());
            HTTPUploader uploader = (HTTPUploader) cb.uploads.get(0);
            assertFalse(uploader.isChatEnabled());
            assertFalse(uploader.isBrowseHostEnabled());
            assertEquals("127.0.0.1", uploader.getHost());

        } finally {
            HttpClientUtils.releaseConnection(response);
        }

        method = new HttpGet(host + "/uri-res/N2R?" + urn1);
        method.addHeader("X-Node", "123.123.123.123:456");
        method.addHeader("X-Features", "chat/0.1");
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            assertEquals(1, cb.uploads.size());
            HTTPUploader uploader = (HTTPUploader) cb.uploads.get(0);
            assertTrue(uploader.isChatEnabled());
            assertFalse(uploader.isBrowseHostEnabled());
            assertEquals(456, uploader.getGnutellaPort());
            assertEquals("123.123.123.123", uploader.getHost());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }

        method = new HttpGet(host + "/uri-res/N2R?" + urn1);
        method.addHeader("Chat", "123.123.123.123:456");
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            assertEquals(1, cb.uploads.size());
            HTTPUploader uploader = (HTTPUploader) cb.uploads.get(0);
            assertTrue(uploader.isChatEnabled());
            assertTrue(uploader.isBrowseHostEnabled());
            assertEquals(456, uploader.getGnutellaPort());
            assertEquals("123.123.123.123", uploader.getHost());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }

    public void testAmountRead() throws Exception {
        HTTPUploader uploader;
        HttpGet method = new HttpGet(host + "/uri-res/N2R?" + urn1);
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            assertEquals(1, cb.uploads.size());

            uploader = (HTTPUploader) cb.uploads.get(0);
            assertEquals(UploadType.SHARED_FILE, uploader.getUploadType());

            InputStream in = response.getEntity().getContent();

            LimeTestUtils.readBytes(in, 500);
            Thread.sleep(500);
            assertGreaterThanOrEquals(500, uploader.amountUploaded());

            LimeTestUtils.readBytes(in, 500);
            Thread.sleep(500);
            assertGreaterThanOrEquals(1000, uploader.amountUploaded());
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
        LimeTestUtils.waitForNIO();
        assertEquals(UploadStatus.COMPLETE, uploader.getState());
    }

    @Singleton
    private static class MyActivityCallback extends ActivityCallbackStub {

        List<Uploader> uploads = new ArrayList<Uploader>();

        @Override
        public void addUpload(Uploader u) {
            uploads.add(u);
        }

        @Override
        public void removeUpload(Uploader u) {
            boolean removed = uploads.remove(u);
            assertTrue("Upload has been added before", removed);
        }

    }

}
