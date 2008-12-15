package org.limewire.http;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import junit.framework.Test;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.DefaultHttpClient;
import org.limewire.http.auth.AuthenticationInterceptorImpl;
import org.limewire.http.auth.AuthenticatorRegistryImpl;
import org.limewire.http.handler.BasicMimeTypeProvider;
import org.limewire.http.handler.FileRequestHandler;
import org.limewire.http.httpclient.HttpClientUtils;
import org.limewire.io.SimpleNetworkInstanceUtils;
import org.limewire.net.ConnectionDispatcher;
import org.limewire.net.ConnectionDispatcherImpl;
import org.limewire.net.SocketAcceptor;
import org.limewire.util.BaseTestCase;

public class BasicHttpAcceptorTest extends BaseTestCase {

    private static final int PORT = 6668;

    private static final int TIMEOUT = 1000;

    private HttpClient client;

    private BasicHttpAcceptor httpAcceptor;

    private SocketAcceptor acceptor;

    private ConnectionDispatcher connectionDispatcher;

    public BasicHttpAcceptorTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(BasicHttpAcceptorTest.class);
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }

    @Override
    protected void setUp() throws Exception {
        client = new DefaultHttpClient();
    }

    @Override
    protected void tearDown() throws Exception {
        stopAcceptor();
    }

    private void initializeAcceptor(int timeout, String... methods) throws Exception {
        connectionDispatcher = new ConnectionDispatcherImpl(new SimpleNetworkInstanceUtils());
        
        acceptor = new SocketAcceptor(connectionDispatcher);
        acceptor.bind(PORT);

        httpAcceptor = new BasicHttpAcceptor(BasicHttpAcceptor
                .createDefaultParams("agent", timeout),
                new AuthenticationInterceptorImpl(new AuthenticatorRegistryImpl()),
                methods);
        httpAcceptor.start();
        
        connectionDispatcher.addConnectionAcceptor(httpAcceptor, true, httpAcceptor.getHttpMethods());
    }
    
    private void stopAcceptor() throws Exception {
        if (httpAcceptor != null) {
            httpAcceptor.stop();
            httpAcceptor = null;
        }
        if (acceptor != null) {
            acceptor.unbind();
            acceptor = null;
        }
        HttpTestUtils.waitForNIO();
    }

    public void testDefaultHandlerHead() throws IOException, Exception {
        initializeAcceptor(TIMEOUT, "HEAD");

        HttpHead method = new HttpHead("http://localhost:" + PORT + "/");
        HttpResponse result = null;
        try {
            result = client.execute(method);
            assertEquals(HttpStatus.SC_NOT_IMPLEMENTED, result.getStatusLine().getStatusCode());
        } finally {
            HttpClientUtils.releaseConnection(result);
        }
    }
    
    public void testDefaultHandlerGet() throws IOException, Exception {
        initializeAcceptor(TIMEOUT, "GET");

        HttpGet method = new HttpGet("http://localhost:" + PORT + "/");
        HttpResponse result = null;
        try {
            result = client.execute(method);
            assertEquals(HttpStatus.SC_NOT_IMPLEMENTED, result.getStatusLine().getStatusCode());
        } finally {
            HttpClientUtils.releaseConnection(result);
        }
    }

    public void testWatchdogTriggeredTimeout() throws IOException, Exception {
        initializeAcceptor(TIMEOUT, "GET");

        File file = File.createTempFile("lime", null);
        byte[] data = new byte[1 * 1000 * 1000];
        Arrays.fill(data, (byte) 'a');
        HttpTestUtils.writeData(file, data);

        FileRequestHandler handler = new FileRequestHandler(file.getParentFile(), new BasicMimeTypeProvider());
        handler.setTimeout(100);
        httpAcceptor.registerHandler("*", handler);

        HttpGet method = new HttpGet("http://localhost:" + PORT + "/" + file.getName());
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            InputStream in = response.getEntity().getContent();
            assertEquals('a', in.read());
            Thread.sleep(200);
            int i = 1;
            while (in.read() != -1) {
                i++;
            }
            // TODO assertFalse(client.getHttpConnectionManager().getConnection(hostConfig).isOpen());
            assertLessThan("Expected connection close", data.length, i);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }
    
    public void testWatchdogDoesNotTimeout() throws Exception {
        initializeAcceptor(TIMEOUT, "GET");

        File file = File.createTempFile("lime", null);
        byte[] data = new byte[1 * 1000 * 1000];
        Arrays.fill(data, (byte) 'a');
        HttpTestUtils.writeData(file, data);

        FileRequestHandler handler = new FileRequestHandler(file.getParentFile(), new BasicMimeTypeProvider());
        handler.setTimeout(100);
        httpAcceptor.registerHandler("*", handler);

        // check that it doesn't timeout
        HttpGet method = new HttpGet("http://localhost:" + PORT + "/" + file.getName());
        HttpResponse response = null;
        try {
            response = client.execute(method);
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            InputStream in = response.getEntity().getContent();
            assertEquals('a', in.read());
            int i = 1;
            byte[] buffer = new byte[1024];
            int l;
            while ((l = in.read(buffer)) != -1) {
                i += l;
            }
            assertEquals(data.length, i);
        } finally {
            HttpClientUtils.releaseConnection(response);
        }
    }
    
    public void testInvalidMethod() throws IOException, Exception {
        initializeAcceptor(TIMEOUT, "GET");

        HttpHead method = new HttpHead("http://localhost:" + PORT + "/");
        HttpResponse result = null;
        try {
            result = client.execute(method);
            fail("Expected IOException, got: " + result.getStatusLine().getStatusCode());
        } catch (IOException expected) {
        } finally {
            HttpClientUtils.releaseConnection(result);
        }
    }

}
