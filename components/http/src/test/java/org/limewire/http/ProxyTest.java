package org.limewire.http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import junit.framework.Test;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.limewire.common.LimeWireCommonModule;
import org.limewire.concurrent.SimpleTimer;
import org.limewire.http.httpclient.LimeHttpClient;
import org.limewire.io.IOUtils;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.io.SimpleNetworkInstanceUtils;
import org.limewire.net.EmptySocketBindingSettings;
import org.limewire.net.LimeWireNetModule;
import org.limewire.net.ProxySettings;
import org.limewire.net.ProxySettings.ProxyType;
import org.limewire.net.ProxySettingsStub;
import org.limewire.net.SocketBindingSettings;
import org.limewire.net.SocketsManager;
import org.limewire.util.BaseTestCase;
import org.limewire.util.StringUtils;
import org.limewire.nio.NIOTestUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;

public class ProxyTest extends BaseTestCase {

    private static final int PROXY_PORT = 9990;

    private final int DEST_PORT = 9999;

    private FakeProxyServer fps;

    private ProxySettingsStub proxySettings;
    private SocketsManager socketsManager;
    private LimeHttpClient httpClient;

    public ProxyTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(ProxyTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    @Override
    public void setUp() throws Exception {
        proxySettings = new ProxySettingsStub();
        
        fps = new FakeProxyServer(PROXY_PORT, DEST_PORT);
        fps.setMakeError(false);
        
        Injector injector = Guice.createInjector(new LimeWireCommonModule(), new LimeWireNetModule(), new LimeWireHttpModule(), new AbstractModule() {
            @Override
            protected void configure() {
                bind(ProxySettings.class).toInstance(proxySettings);
                bind(SocketBindingSettings.class).to(EmptySocketBindingSettings.class);
                SimpleTimer timer = new SimpleTimer(true);
                bind(ScheduledExecutorService.class).annotatedWith(Names.named("backgroundExecutor")).toInstance(timer);
                bind(ExecutorService.class).annotatedWith(Names.named("backgroundExecutor")).toInstance(timer);
                bind(Executor.class).annotatedWith(Names.named("backgroundExecutor")).toInstance(timer);
                bind(NetworkInstanceUtils.class).to(SimpleNetworkInstanceUtils.class);
            }
        });        
        
        socketsManager = injector.getInstance(SocketsManager.class);
        httpClient = injector.getInstance(LimeHttpClient.class);
        
        
        proxySettings.setProxyForPrivate(true);
        proxySettings.setProxyHost("127.0.0.1");
        proxySettings.setProxyPort(PROXY_PORT);
        proxySettings.setProxyUser("");
        proxySettings.setProxyPass("");
    }

    @Override
    public void tearDown() throws Exception {
        fps.killServers();
        
        NIOTestUtils.waitForNIO();
    }

    /**
     * If Proxy is off we should connect directly
     */
    public void testConnectionsWithProxyOff() throws Exception {
        proxySettings.setProxyType(ProxyType.NONE);
        fps.setProxyOn(false);
        fps.setAuthentication(false);
        fps.setProxyVersion(ProxyType.NONE);

        Socket s = socketsManager.connect(new InetSocketAddress("localhost", DEST_PORT), 0);
        // we should be connected to something, NPE is an error
        s.close();
    }

    /**
     * connect with socks 5
     */
    public void testSOCKS5Connection() throws Exception {
        runSOCKS5Connection(false);
    }

    public void testSOCKS5ConnectionNB() throws Exception {
        runSOCKS5Connection(true);
    }

    private void runSOCKS5Connection(boolean nb) throws Exception {
        proxySettings.setProxyType(ProxyType.SOCKS5);
        fps.setProxyOn(true);
        fps.setAuthentication(false);
        fps.setProxyVersion(ProxyType.SOCKS5);
        connect(nb, true);
    }

    /**
     * connect with socks 5 connection fails with bad code.
     */
    public void testSOCKS5ConnectionWithError() throws Exception {
        runSOCKS5ConnectionWithError(false);
    }

    public void testSOCKS5ConnectionWithErrorNB() throws Exception {
        runSOCKS5ConnectionWithError(true);
    }

    private void runSOCKS5ConnectionWithError(boolean nb) throws Exception {
        proxySettings.setProxyType(ProxyType.SOCKS5);
        fps.setProxyOn(true);
        fps.setAuthentication(false);
        fps.setProxyVersion(ProxyType.SOCKS5);
        fps.setMakeError(true);
        connect(nb, false);
        fps.setMakeError(false);
    }

    /**
     * connect with socks5 with auth
     */
    public void testSOCKS5WithAuth() throws Exception {
        runSOCKS5WithAuth(false);
    }

    public void testSOCKS5WithAuthNB() throws Exception {
        runSOCKS5WithAuth(true);
    }

    private void runSOCKS5WithAuth(boolean nb) throws Exception {
        proxySettings.setProxyType(ProxyType.SOCKS5);
        proxySettings.setProxyAuthRequired(true);
        proxySettings.setProxyUser(FakeProxyServer.USER);
        proxySettings.setProxyPass(FakeProxyServer.PASS);

        fps.setProxyOn(true);
        fps.setAuthentication(true);
        fps.setProxyVersion(ProxyType.SOCKS5);
        connect(nb, true);
    }

    public void testSOCKS4Connection() throws Exception {
        runSOCKS4Connection(false);
    }

    public void testSOCKS4ConnectionNB() throws Exception {
        runSOCKS4Connection(true);
    }

    private void runSOCKS4Connection(boolean nb) throws Exception {
        proxySettings.setProxyType(ProxyType.SOCKS4);
        fps.setProxyOn(true);
        fps.setAuthentication(false);
        fps.setProxyVersion(ProxyType.SOCKS4);
        connect(nb, true);
    }

    public void testSOCKS4ConnectionWithError() throws Exception {
        runSOCKS4ConnectionWithError(false);
    }

    public void testSOCKS4ConnectionWithErrorNB() throws Exception {
        runSOCKS4ConnectionWithError(true);
    }

    private void runSOCKS4ConnectionWithError(boolean nb) throws Exception {
        proxySettings.setProxyType(ProxyType.SOCKS4);
        fps.setProxyOn(true);
        fps.setAuthentication(false);
        fps.setProxyVersion(ProxyType.SOCKS4);
        fps.setMakeError(true);
        connect(nb, false);
        fps.setMakeError(false);
    }

    public void testSOCKS4WithAuth() throws Exception {
        runSOCKS4WithAuth(false);
    }

    public void testSOCKS4WithAuthNB() throws Exception {
        runSOCKS4WithAuth(true);
    }

    private void runSOCKS4WithAuth(boolean nb) throws Exception {
        proxySettings.setProxyType(ProxyType.SOCKS4);
        proxySettings.setProxyAuthRequired(true);
        proxySettings.setProxyUser(FakeProxyServer.USER);
        proxySettings.setProxyPass(FakeProxyServer.PASS);

        fps.setProxyOn(true);
        fps.setAuthentication(true);
        fps.setProxyVersion(ProxyType.SOCKS4);
        connect(nb, true);
    }

    /**
     * Tests that HTTP proxies are used correctly
     */
    public void testHTTPProxy() throws Exception {
        runHTTPProxy(false);
    }

    public void testHTTPProxyNB() throws Exception {
        runHTTPProxy(true);
    }

    private void runHTTPProxy(boolean nb) throws Exception {
        proxySettings.setProxyType(ProxyType.HTTP);

        fps.setProxyOn(true);
        fps.setAuthentication(false);
        fps.setProxyVersion(ProxyType.HTTP);
        connect(nb, true);
    }

    public void testHTTPProxyWithError() throws Exception {
        runHTTPProxyWithError(false);
    }

    public void testHTTPProxyWithErrorNB() throws Exception {
        runHTTPProxyWithError(true);
    }

    private void runHTTPProxyWithError(boolean nb) throws Exception {
        proxySettings.setProxyType(ProxyType.HTTP);

        fps.setProxyOn(true);
        fps.setAuthentication(false);
        fps.setProxyVersion(ProxyType.HTTP);
        fps.setMakeError(true);
        connect(nb, false);
    }

    private void connect(boolean nb, boolean success) throws Exception {
        if (success) {
            Socket s;
            if (!nb) {
                s = socketsManager.connect(new InetSocketAddress("localhost", DEST_PORT), 0);
            } else {
                ConnectObserverStub o = new ConnectObserverStub();
                s = socketsManager.connect(new InetSocketAddress("localhost", DEST_PORT), 0, o);
                o.waitForResponse(5000);
                assertEquals(s, o.getSocket());
                assertNull(o.getIoException());
                assertFalse(o.isShutdown());
            }
            // Must connect somewhere, NPE is an error
            s.close();
        } else {
            if (!nb) {
                try {
                    socketsManager.connect(new InetSocketAddress("localhost", DEST_PORT), 0);
                    fail("acceptedConnection from a bad proxy server");
                } catch (IOException iox) {
                    // Good -- expected behaviour
                }
            } else {
                ConnectObserverStub o = new ConnectObserverStub();
                socketsManager.connect(new InetSocketAddress("localhost", DEST_PORT), 0, o);
                o.waitForResponse(5000);
                assertNull(o.getSocket());
                assertNull(o.getIoException());
                assertTrue(o.isShutdown());
            }
        }
    }
    
    public void testHTTPClientWithProxyOff() throws Exception {
        proxySettings.setProxyType(ProxyType.NONE);
        fps.setProxyOn(false);
        fps.setAuthentication(false);
        fps.setProxyVersion(ProxyType.NONE);
        fps.setHttpRequest(true);

        String connectTo = "http://" + "localhost:" + DEST_PORT + "/myFile.txt";
        HttpGet get = new HttpGet(connectTo);
        get.addHeader("connection", "close");
        HttpResponse response = httpClient.execute(get);
        byte[] resp = IOUtils.readFully(response.getEntity().getContent());
        assertEquals("invalid response from server", "hello", StringUtils.getUTF8String(resp));
        get.abort();
    }

    public void testHTTPClientProxiesWithHttpProxy() throws Exception {
        proxySettings.setProxyType(ProxyType.HTTP);
        fps.setProxyOn(true);
        fps.setAuthentication(false);
        fps.setProxyVersion(ProxyType.HTTP);
        fps.setHttpRequest(true);

        String connectTo = "http://" + "localhost:" + DEST_PORT + "/myFile2.txt";
        HttpGet get = new HttpGet(connectTo);
        get.addHeader("connection", "close");
        // release the connections.
        // TODO client.setHttpConnectionManager(new SimpleHttpConnectionManager());
        HttpResponse response = httpClient.execute(get);
        String resp = StringUtils.getUTF8String(IOUtils.readFully(response.getEntity().getContent()));
        assertEquals("invalid response from server", "hello", resp);
        get.abort();
    }

    public void testHTTPClientProxiesWithSocks4() throws Exception {
        proxySettings.setProxyType(ProxyType.SOCKS4);
        fps.setProxyOn(true);
        fps.setAuthentication(false);
        fps.setProxyVersion(ProxyType.SOCKS4);
        fps.setHttpRequest(true);

        String connectTo = "http://" + "localhost:" + DEST_PORT + "/myFile3.txt";
        HttpGet get = new HttpGet(connectTo);
        get.addHeader("connection", "close");
        // release the connections.
        // TODO client.setHttpConnectionManager(new SimpleHttpConnectionManager());
        HttpResponse response = httpClient.execute(get);
        String resp = StringUtils.getUTF8String(IOUtils.readFully(response.getEntity().getContent()));
        assertEquals("invalid response from server", "hello", resp);
        get.abort();
    }

    public void testHTTPClientProxiesWithSocks5() throws Exception {
        proxySettings.setProxyType(ProxyType.SOCKS5);
        fps.setProxyOn(true);
        fps.setAuthentication(false);
        fps.setProxyVersion(ProxyType.SOCKS5);
        fps.setHttpRequest(true);

        String connectTo = "http://" + "localhost:" + DEST_PORT + "/myFile4.txt";
        HttpGet get = new HttpGet(connectTo);
        get.addHeader("connection", "close");
        // release the connections.
        // TODO client.setHttpConnectionManager(new SimpleHttpConnectionManager());
        HttpResponse response = httpClient.execute(get);
        String resp = StringUtils.getUTF8String(IOUtils.readFully(response.getEntity().getContent()));
        assertEquals("invalid response from server", "hello", resp);
        get.abort();
    }

    public void testHTTPClientProxiesWithAuthSocks4() throws Exception {
        proxySettings.setProxyType(ProxyType.SOCKS4);
        proxySettings.setProxyAuthRequired(true);
        proxySettings.setProxyUser(FakeProxyServer.USER);
        proxySettings.setProxyPass(FakeProxyServer.PASS);

        fps.setProxyOn(true);
        fps.setAuthentication(true);
        fps.setProxyVersion(ProxyType.SOCKS4);
        fps.setHttpRequest(true);

        String connectTo = "http://" + "localhost:" + DEST_PORT + "/myFile3.txt";
        HttpGet get = new HttpGet(connectTo);
        get.addHeader("connection", "close");
        // release the connections.
        // TODO client.setHttpConnectionManager(new SimpleHttpConnectionManager());
        HttpResponse response = httpClient.execute(get);
        String resp = StringUtils.getUTF8String(IOUtils.readFully(response.getEntity().getContent()));
        assertEquals("invalid response from server", "hello", resp);
        get.abort();
    }

    public void testHTTPClientProxiesWithAuthSocks5() throws Exception {
        proxySettings.setProxyType(ProxyType.SOCKS5);
        proxySettings.setProxyAuthRequired(true);
        proxySettings.setProxyUser(FakeProxyServer.USER);
        proxySettings.setProxyPass(FakeProxyServer.PASS);

        fps.setProxyOn(true);
        fps.setAuthentication(true);
        fps.setProxyVersion(ProxyType.SOCKS5);
        fps.setHttpRequest(true);

        String connectTo = "http://" + "localhost:" + DEST_PORT + "/myFile4.txt";
        HttpGet get = new HttpGet(connectTo);
        get.addHeader("connection", "close");
        // release the connections.
        // TODO client.setHttpConnectionManager(new SimpleHttpConnectionManager());
        HttpResponse response = httpClient.execute(get);
        String resp = StringUtils.getUTF8String(IOUtils.readFully(response.getEntity().getContent()));
        assertEquals("invalid response from server", "hello", resp);
        get.abort();
    }

}
