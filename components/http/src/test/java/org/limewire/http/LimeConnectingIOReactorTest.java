package org.limewire.http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.nio.DefaultClientIOEventDispatch;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.nio.NHttpConnection;
import org.apache.http.nio.entity.BufferingNHttpEntity;
import org.apache.http.nio.entity.ConsumingNHttpEntity;
import org.apache.http.nio.protocol.AsyncNHttpClientHandler;
import org.apache.http.nio.protocol.NHttpRequestExecutionHandler;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.SessionRequest;
import org.apache.http.nio.reactor.SessionRequestCallback;
import org.apache.http.nio.util.HeapByteBufferAllocator;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;
import org.apache.http.util.EntityUtils;
import org.limewire.common.LimeWireCommonModule;
import org.limewire.http.reactor.LimeConnectingIOReactor;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.io.SimpleNetworkInstanceUtils;
import org.limewire.net.EmptyProxySettings;
import org.limewire.net.EmptySocketBindingSettings;
import org.limewire.net.LimeWireNetModule;
import org.limewire.net.ProxySettings;
import org.limewire.net.SocketBindingSettings;
import org.limewire.net.SocketsManager;
import org.limewire.nio.NIODispatcher;
import org.limewire.util.BaseTestCase;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class LimeConnectingIOReactorTest extends BaseTestCase {
    
    private SocketsManager socketsManager;

    public LimeConnectingIOReactorTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(LimeConnectingIOReactorTest.class);
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    protected void setUp() throws Exception {        
        Injector injector = Guice.createInjector(new LimeWireCommonModule(), new LimeWireNetModule(), new AbstractModule() {
            @Override
            protected void configure() {
                bind(ProxySettings.class).to(EmptyProxySettings.class);
                bind(SocketBindingSettings.class).to(EmptySocketBindingSettings.class);
                bind(NetworkInstanceUtils.class).to(SimpleNetworkInstanceUtils.class);
            }
        });
        
        socketsManager = injector.getInstance(SocketsManager.class);
    }
    
    public void testConnect() throws Exception {        
        HttpParams params = new BasicHttpParams();
        params
            .setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 5000)
            .setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 10000)
            .setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024)
            .setParameter(CoreProtocolPNames.USER_AGENT, "HttpComponents/1.1");
        
        final ConnectingIOReactor ioReactor = new LimeConnectingIOReactor(params, NIODispatcher.instance().getScheduledExecutorService(), socketsManager);

        BasicHttpProcessor httpproc = new BasicHttpProcessor();
        httpproc.addInterceptor(new RequestContent());
        httpproc.addInterceptor(new RequestTargetHost());
        httpproc.addInterceptor(new RequestConnControl());
        httpproc.addInterceptor(new RequestUserAgent());
        httpproc.addInterceptor(new RequestExpectContinue());
        
        CountDownLatch requestCount = new CountDownLatch(3);
        
        AsyncNHttpClientHandler handler = new AsyncNHttpClientHandler(
                httpproc,
                new MyHttpRequestExecutionHandler(requestCount),
                new DefaultConnectionReuseStrategy(),
                params);
        
        final IOEventDispatch ioEventDispatch = new DefaultClientIOEventDispatch(handler, params);
        
        ioReactor.execute(ioEventDispatch);

        SessionRequest[] reqs = new SessionRequest[(int)requestCount.getCount()];
        SessionRequestCallback[] callbacks = new SessionRequestCallback[reqs.length];
        for(int i = 0; i < callbacks.length; i++) {
            callbacks[i] = new SessionRequestCallback() {
                public void cancelled(SessionRequest arg0) {
                    fail("couldn't connect!");
                }
                
                public void completed(SessionRequest arg0) {
                }
                
                public void failed(SessionRequest arg0) {
                    fail("couldn't connect!");
                }
                public void timeout(SessionRequest arg0) {
                    fail("couldn't connect!");
                }
            };
        }
        reqs[0] = ioReactor.connect(
                new InetSocketAddress("www.yahoo.com", 80), 
                null, 
                new HttpHost("www.yahoo.com"),
                callbacks[0]);
        reqs[1] = ioReactor.connect(
                new InetSocketAddress("www.google.com", 80), 
                null,
                new HttpHost("www.google.com"),
                callbacks[1]);
        reqs[2] = ioReactor.connect(
                new InetSocketAddress("www.apache.org", 80), 
                null,
                new HttpHost("www.apache.org"),
                callbacks[2]);
     
        assertTrue(requestCount.await(15, TimeUnit.SECONDS));
        
        for(SessionRequest req : reqs) {
            assertTrue(req.isCompleted());
            assertNull(req.getException());
        }
    }
    
    public void testDoesntConnect() throws Exception {        
        HttpParams params = new BasicHttpParams();
        params
            .setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 5000)
            .setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 10000)
            .setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024)
            .setParameter(CoreProtocolPNames.USER_AGENT, "HttpComponents/1.1");
        
        final ConnectingIOReactor ioReactor = new LimeConnectingIOReactor(params, NIODispatcher.instance().getScheduledExecutorService(), socketsManager);
        final CountDownLatch requestCount = new CountDownLatch(3);
        
        AsyncNHttpClientHandler handler = new AsyncNHttpClientHandler(
                new BasicHttpProcessor(),
                new FailingHandler(),
                new DefaultConnectionReuseStrategy(),
                params);
        
        final IOEventDispatch ioEventDispatch = new DefaultClientIOEventDispatch(handler, params);
        
        ioReactor.execute(ioEventDispatch);

        SessionRequest[] reqs = new SessionRequest[(int)requestCount.getCount()];
        SessionRequestCallback[] callbacks = new SessionRequestCallback[reqs.length];
        for(int i = 0; i < callbacks.length; i++) {
            callbacks[i] = new SessionRequestCallback() {
                public void cancelled(SessionRequest arg0) {
                    fail("wrong method");
                }
                
                public void completed(SessionRequest arg0) {
                    fail("wrong method");
                }
                
                public void failed(SessionRequest arg0) {
                    requestCount.countDown();
                }
                public void timeout(SessionRequest arg0) {
                    fail("wrong method");
                }
            };
        }
        reqs[0] = ioReactor.connect(
                new InetSocketAddress("asdfoihasdfoihasdfoihadsf.2395", 80), 
                null, 
                null,
                callbacks[0]);
        reqs[1] = ioReactor.connect(
                new InetSocketAddress("asdfoihasdfoihasdfoihadsf.com", 80), 
                null,
                null,
                callbacks[1]);
        reqs[2] = ioReactor.connect(
                new InetSocketAddress("asdfoihasdfoihasdfoihadsf.au", 80), 
                null,
                null,
                callbacks[2]);
          
        assertTrue(requestCount.await(15, TimeUnit.SECONDS));
        
        for(SessionRequest req : reqs) {
            assertTrue(req.isCompleted());
            assertNotNull(req.getException());
        }
    }
    
    static class FailingHandler implements NHttpRequestExecutionHandler {
        public void finalizeContext(HttpContext arg0) {
            fail("shouldn't be here!");
        }
        public void handleResponse(HttpResponse arg0, HttpContext arg1) throws IOException {
            fail("shouldn't be here!");
        }
        public void initalizeContext(HttpContext arg0, Object arg1) {
            fail("shouldn't be here!");
        }
        public ConsumingNHttpEntity responseEntity(HttpResponse arg0, HttpContext arg1)
                throws IOException {
            fail("shouldn't be here!");
            return null;
        }
        public HttpRequest submitRequest(HttpContext arg0) {
            fail("shouldn't be here!");
            return null;
        }
    }
    
    static class MyHttpRequestExecutionHandler implements NHttpRequestExecutionHandler {

        private final static String REQUEST_SENT       = "request-sent";
        private final static String RESPONSE_RECEIVED  = "response-received";
        
        private final CountDownLatch requestCount;
        
        public MyHttpRequestExecutionHandler(final CountDownLatch requestCount) {
            super();
            this.requestCount = requestCount;
        }
        
        public void initalizeContext(final HttpContext context, final Object attachment) {
            HttpHost targetHost = (HttpHost) attachment;
            context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, targetHost);
        }
        
        public void finalizeContext(final HttpContext context) {
            Object flag = context.getAttribute(RESPONSE_RECEIVED);
            if (flag == null) {
                fail("didn't get a response from host: " + context.getAttribute(ExecutionContext.HTTP_TARGET_HOST));
            }
        }

        public HttpRequest submitRequest(final HttpContext context) {
            Object flag = context.getAttribute(REQUEST_SENT);
            if (flag == null) {
                // Stick some object into the context
                context.setAttribute(REQUEST_SENT, Boolean.TRUE);
                return new BasicHttpRequest("GET", "/");
            } else {
                // close the connections immediately
                try {
                    ((NHttpConnection)context.getAttribute(ExecutionContext.HTTP_CONNECTION)).close();
                } catch(IOException ignored) {}
                return null;
            }
        }
        

        public ConsumingNHttpEntity responseEntity(HttpResponse response, HttpContext context)
                throws IOException {
            return new BufferingNHttpEntity(response.getEntity(), new HeapByteBufferAllocator());
        }
        
        public void handleResponse(final HttpResponse response, final HttpContext context) {
            HttpEntity entity = response.getEntity();
            try {
                String content = EntityUtils.toString(entity);
                assertGreaterThan("failed requesting host: "
                        + context.getAttribute(ExecutionContext.HTTP_TARGET_HOST), 1, content.length());
            } catch (IOException ex) {
                fail(ex);
            }

            context.setAttribute(RESPONSE_RECEIVED, Boolean.TRUE);
            
            requestCount.countDown();
        }
        
    }
}
