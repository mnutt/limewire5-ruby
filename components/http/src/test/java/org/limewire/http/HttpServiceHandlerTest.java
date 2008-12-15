package org.limewire.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;

import junit.framework.Test;

import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseFactory;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpRequestFactory;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.nio.DefaultServerIOEventDispatch;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.nio.entity.ConsumingNHttpEntity;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.nio.protocol.NHttpRequestHandlerRegistry;
import org.apache.http.nio.protocol.SimpleNHttpRequestHandler;
import org.apache.http.nio.reactor.EventMask;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpContext;
import org.limewire.http.protocol.ExtendedAsyncNHttpServiceHandler;
import org.limewire.http.reactor.HttpChannel;
import org.limewire.http.reactor.HttpIOSession;
import org.limewire.util.BaseTestCase;
import org.limewire.util.BufferUtils;

public class HttpServiceHandlerTest extends BaseTestCase {

    private HttpParams parms;

    private ConnectionReuseStrategy connStrategy;

    private HttpResponseFactory responseFactory;

    private DefaultHttpRequestFactory requestFactory;

    private MockHttpNIOEntity nioEntity;

    private NStringEntity stringEntity;

    private DefaultServerIOEventDispatch eventDispatch;

    private ExtendedAsyncNHttpServiceHandler serviceHandler;

    private NHttpRequestHandlerRegistry registry;

    public HttpServiceHandlerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(HttpServiceHandlerTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        parms = new BasicHttpParams();
        HttpConnectionParams.setSocketBufferSize(parms, 512);
        connStrategy = new DefaultConnectionReuseStrategy();
        responseFactory = new DefaultHttpResponseFactory();
        requestFactory = new DefaultHttpRequestFactory();

        BasicHttpProcessor httpProcessor = new BasicHttpProcessor();
        registry = new NHttpRequestHandlerRegistry();
        stringEntity = new NStringEntity("abc");
        registry.register("/get/string", new SimpleNHttpRequestHandler() {
            public ConsumingNHttpEntity entityRequest(HttpEntityEnclosingRequest request,
                    HttpContext context) throws HttpException, IOException {
                return null;
            }
            
            @Override
            public void handle(HttpRequest request, HttpResponse response,
                    HttpContext context) throws HttpException, IOException {
                response.setEntity(stringEntity);
            }
        });
        nioEntity = new MockHttpNIOEntity("abc");
        registry.register("/get/nio", new SimpleNHttpRequestHandler() {
            public ConsumingNHttpEntity entityRequest(HttpEntityEnclosingRequest request,
                    HttpContext context) throws HttpException, IOException {
                return null;
            }
            
            @Override
            public void handle(HttpRequest request, HttpResponse response,
                    HttpContext context) throws HttpException, IOException {
                response.setEntity(nioEntity);
            }
        });
        serviceHandler = new ExtendedAsyncNHttpServiceHandler(
                httpProcessor, responseFactory, connStrategy, parms);
        serviceHandler.setHandlerResolver(registry);
        eventDispatch = new DefaultServerIOEventDispatch(serviceHandler, parms);
    }

    public void testEventListener() throws Exception {
        MockHttpServiceEventListener listener = new MockHttpServiceEventListener();
        serviceHandler.setEventListener(listener);

        StubSocket socket = new StubSocket();
        StubIOSession session = new StubIOSession(socket);
        MockHttpChannel channel = new MockHttpChannel(session, eventDispatch);
        session.setHttpChannel(channel);
        MockHttpServerConnection conn = new MockHttpServerConnection(session,
                requestFactory, parms);
        
        serviceHandler.connected(conn);
        assertTrue(listener.connectionOpened);

        HttpRequest request = new BasicHttpRequest("GET", "/get/nio");
        conn.setHttpRequest(request);
        serviceHandler.requestReceived(conn);
        
        conn.produceOutput(serviceHandler);
        MockContentEncoder encoder = new MockContentEncoder();
        conn.setContentEncoder(encoder);
        assertFalse(listener.responseSent);
        conn.produceOutput(serviceHandler);
        conn.produceOutput(serviceHandler);
        conn.produceOutput(serviceHandler);
        assertEquals("abc", encoder.data.toString());
        assertTrue(listener.responseSent);
        assertEquals(EventMask.READ, session.getEventMask());
    }

    public void testEventListenerException() throws Exception {
        MockHttpServiceEventListener listener = new MockHttpServiceEventListener();
        serviceHandler.setEventListener(listener);

        StubSocket socket = new StubSocket();
        StubIOSession session = new StubIOSession(socket);
        MockHttpChannel channel = new MockHttpChannel(session, eventDispatch);
        session.setHttpChannel(channel);
        MockHttpServerConnection conn = new MockHttpServerConnection(session,
                requestFactory, parms);
        
        serviceHandler.connected(conn);
        assertTrue(listener.connectionOpened);

        HttpRequest request = new BasicHttpRequest("GET", "/get/nio");
        conn.setHttpRequest(request);
        serviceHandler.requestReceived(conn);
        
        nioEntity.exception = new IOException();
        conn.produceOutput(serviceHandler);
        conn.produceOutput(serviceHandler);
        
        assertNotNull(listener.exception);
        assertTrue(session.isClosed());
        
        serviceHandler.closed(conn);
        assertTrue(listener.connectionClosed);
    }

    public void testEventListenerTimeout() throws Exception {
        MockHttpServiceEventListener listener = new MockHttpServiceEventListener();
        serviceHandler.setEventListener(listener);

        StubSocket socket = new StubSocket();
        StubIOSession session = new StubIOSession(socket);
        MockHttpChannel channel = new MockHttpChannel(session, eventDispatch);
        session.setHttpChannel(channel);
        MockHttpServerConnection conn = new MockHttpServerConnection(session,
                requestFactory, parms);
        
        serviceHandler.connected(conn);
        serviceHandler.closed(conn);
        assertTrue(listener.connectionClosed);
    }

    public void testGetRequestNIOEntity() throws Exception {
        StubSocket socket = new StubSocket();
        StubIOSession session = new StubIOSession(socket);
        MockHttpChannel channel = new MockHttpChannel(session, eventDispatch);
        session.setHttpChannel(channel);
        MockHttpServerConnection conn = new MockHttpServerConnection(session,
                requestFactory, parms);
        
        serviceHandler.connected(conn);

        HttpRequest request = new BasicHttpRequest("GET", "/get/nio");
        conn.setHttpRequest(request);
        serviceHandler.requestReceived(conn);
       
        conn.produceOutput(serviceHandler);
        assertEquals(nioEntity, conn.getHttpResponse().getEntity());
        MockContentEncoder encoder = new MockContentEncoder();
        conn.setContentEncoder(encoder);
        conn.produceOutput(serviceHandler);
        assertEquals("a", encoder.data.toString());
        assertFalse(encoder.isCompleted());
        assertTrue(nioEntity.initialized);
        assertFalse(nioEntity.finished);
        conn.produceOutput(serviceHandler);
        conn.produceOutput(serviceHandler);
        assertEquals("abc", encoder.data.toString());
        assertTrue(encoder.isCompleted());
        assertTrue(nioEntity.finished);
    }

    public void testGetRequestNIOEntityThrowingException() throws Exception {
        StubSocket socket = new StubSocket();
        StubIOSession session = new StubIOSession(socket);
        MockHttpChannel channel = new MockHttpChannel(session, eventDispatch);
        session.setHttpChannel(channel);
        MockHttpServerConnection conn = new MockHttpServerConnection(session,
                requestFactory, parms);
        
        serviceHandler.connected(conn);

        HttpRequest request = new BasicHttpRequest("GET", "/get/nio");
        conn.setHttpRequest(request);
        serviceHandler.requestReceived(conn);
        conn.produceOutput(serviceHandler);
        
        MockContentEncoder encoder = new MockContentEncoder();
        nioEntity.exception = new IOException();
        conn.setContentEncoder(encoder);
        conn.produceOutput(serviceHandler);
        assertTrue(session.isClosed());
        assertTrue(nioEntity.finished);
    }

    public void testGetRequestNIOEntityTimeout() throws Exception {
        StubSocket socket = new StubSocket();
        StubIOSession session = new StubIOSession(socket);
        MockHttpChannel channel = new MockHttpChannel(session, eventDispatch);
        session.setHttpChannel(channel);
        MockHttpServerConnection conn = new MockHttpServerConnection(session,
                requestFactory, parms);
        
        serviceHandler.connected(conn);

        HttpRequest request = new BasicHttpRequest("GET", "/get/nio");
        conn.setHttpRequest(request);
        serviceHandler.requestReceived(conn);
        conn.produceOutput(serviceHandler);
        
        assertFalse(nioEntity.finished);
        serviceHandler.closed(conn);
        assertTrue(nioEntity.finished);
    }

    public void testGetRequestStringEntity() throws Exception {
        StubSocket socket = new StubSocket();
        StubIOSession session = new StubIOSession(socket);
        MockHttpChannel channel = new MockHttpChannel(session, eventDispatch);
        session.setHttpChannel(channel);
        MockHttpServerConnection conn = new MockHttpServerConnection(session,
                requestFactory, parms);
        
        serviceHandler.connected(conn);

        HttpRequest request = new BasicHttpRequest("GET", "/get/string");
        conn.setHttpRequest(request);
        serviceHandler.requestReceived(conn);
        conn.produceOutput(serviceHandler);
        assertEquals(stringEntity, conn.getHttpResponse().getEntity());
        
        MockContentEncoder encoder = new MockContentEncoder();
        conn.setContentEncoder(encoder);
        conn.produceOutput(serviceHandler);
        assertEquals("abc", encoder.data.toString());
        assertTrue(encoder.isCompleted());
    }

    public void testHeadRequest() throws Exception {
        MockHttpServiceEventListener listener = new MockHttpServiceEventListener();
        serviceHandler.setEventListener(listener);

        StubSocket socket = new StubSocket();
        StubIOSession session = new StubIOSession(socket);
        MockHttpChannel channel = new MockHttpChannel(session, eventDispatch);
        session.setHttpChannel(channel);
        MockHttpServerConnection conn = new MockHttpServerConnection(session,
                requestFactory, parms);
        
        serviceHandler.connected(conn);

        HttpRequest request = new BasicHttpRequest("HEAD", "/get/string");
        conn.setHttpRequest(request);
        serviceHandler.requestReceived(conn);
        conn.produceOutput(serviceHandler);
        assertNotNull(listener.response);
        assertNull(listener.response.getEntity());
    }

    public void testPostRequest() throws Exception {
        MockHttpServiceEventListener listener = new MockHttpServiceEventListener();
        serviceHandler.setEventListener(listener);

        StubSocket socket = new StubSocket();
        StubIOSession session = new StubIOSession(socket);
        MockHttpChannel channel = new MockHttpChannel(session, eventDispatch);
        session.setHttpChannel(channel);
        MockHttpServerConnection conn = new MockHttpServerConnection(session,
                requestFactory, parms);
        
        serviceHandler.connected(conn);

        HttpRequest request = new BasicHttpRequest("POST", "/get/string");
        conn.setHttpRequest(request);
        serviceHandler.requestReceived(conn);
        conn.produceOutput(serviceHandler);
        conn.produceOutput(serviceHandler);
        assertNotNull(listener.response);
        assertEquals(HttpStatus.SC_OK, listener.response.getStatusLine().getStatusCode());        
    }

    public void testReadWriteInterestHead() throws Exception {
        StubSocket socket = new StubSocket();
        MockExecutor executor = new MockExecutor();
        StubIOSession session = new StubIOSession(socket, executor);
        MockHttpChannel channel = new MockHttpChannel(session, eventDispatch);
        session.setHttpChannel(channel);
        MockHttpServerConnection conn = new MockHttpServerConnection(session,
                requestFactory, parms);
        assertTrue(channel.isReadInterest());
        assertFalse(channel.isWriteInterest());
        
        serviceHandler.connected(conn);
        assertTrue(channel.isReadInterest());
        assertFalse(channel.isWriteInterest());
        assertNull(executor.command);

        conn.setHttpRequest(new BasicHttpRequest("HEAD", "/get/string"));
        conn.consumeInput(serviceHandler);
        assertFalse(channel.isReadInterest());
        assertTrue(channel.isWriteInterest());
        assertNull(executor.command);
        
        // now that a request is processed & a response is pending,
        // send it.
        conn.produceOutput(serviceHandler);
        assertTrue(channel.isReadInterest());
        assertTrue(channel.isWriteInterest());
        assertNull(executor.command);
        
        // response is already sent now, just waiting for requests...
        conn.produceOutput(serviceHandler);
        assertTrue(channel.isReadInterest());
        assertFalse(channel.isWriteInterest());
        assertNull(executor.command);
        
        conn.setHttpRequest(new BasicHttpRequest("HEAD", "/get/string"));
        conn.consumeInput(serviceHandler);
        assertFalse(channel.isReadInterest());
        assertTrue(channel.isWriteInterest());
        assertNull(executor.command);

        // write out the first response & immediately process the next.
        conn.setHasBufferedInput(true);
        conn.produceOutput(serviceHandler);
        assertTrue(channel.isReadInterest());
        assertTrue(channel.isWriteInterest());
        assertNotNull(executor.command);
        
        // Now run the buffered input consumer.
        executor.command.run();
        executor.command = null;
        assertFalse(channel.isReadInterest());
        assertTrue(channel.isWriteInterest());
        
        // write out the second response, and turn interest on for wanting more
        conn.setHasBufferedInput(false);
        conn.produceOutput(serviceHandler);
        assertTrue(channel.isReadInterest());
        assertTrue(channel.isWriteInterest());
        assertNull(executor.command);
        
        // No more to process, so nothing to write.
        conn.produceOutput(serviceHandler);
        assertTrue(channel.isReadInterest());
        assertFalse(channel.isWriteInterest());
        assertNull(executor.command);
    }

    public void testReadWriteInterestGet() throws Exception {
        StubSocket socket = new StubSocket();
        MockExecutor executor = new MockExecutor();
        StubIOSession session = new StubIOSession(socket, executor);
        MockHttpChannel channel = new MockHttpChannel(session, eventDispatch);
        session.setHttpChannel(channel);
        MockHttpServerConnection conn = new MockHttpServerConnection(session,
                requestFactory, parms);
        assertTrue(channel.isReadInterest());
        assertFalse(channel.isWriteInterest());
        
        serviceHandler.connected(conn);
        assertTrue(channel.isReadInterest());
        assertFalse(channel.isWriteInterest());
        assertNull(executor.command);

        conn.setHttpRequest(new BasicHttpRequest("GET", "/get/string"));
        conn.consumeInput(serviceHandler);
        assertFalse(channel.isReadInterest());
        assertTrue(channel.isWriteInterest());
        assertNull(executor.command);

        // start writing the response headers
        conn.produceOutput(serviceHandler);
        assertFalse(channel.isReadInterest());
        assertTrue(channel.isWriteInterest());
        assertNull(executor.command);
        
        // write the response body
        conn.produceOutput(serviceHandler);
        assertTrue(channel.isReadInterest());
        assertFalse(channel.isWriteInterest());
        assertNull(executor.command);
        
        conn.setHttpRequest(new BasicHttpRequest("GET", "/get/string"));
        conn.consumeInput(serviceHandler);
        assertFalse(channel.isReadInterest());
        assertTrue(channel.isWriteInterest());
        assertNull(executor.command);
        
        // start writing the response headers
        conn.produceOutput(serviceHandler);
        assertFalse(channel.isReadInterest());
        assertTrue(channel.isWriteInterest());
        assertNull(executor.command);
        
        // write the response body, and process the next request immediately...
        conn.setHasBufferedInput(true);
        conn.produceOutput(serviceHandler);
        assertTrue(channel.isReadInterest());
        assertFalse(channel.isWriteInterest());        
        assertNotNull(executor.command);
        
        // Run the buffered input consumer...
        executor.command.run();
        executor.command = null;
        assertFalse(channel.isReadInterest());
        assertTrue(channel.isWriteInterest());

        // start writing the response headers        
        conn.setHasBufferedInput(false);
        conn.produceOutput(serviceHandler);
        assertFalse(channel.isReadInterest());
        assertTrue(channel.isWriteInterest());
        assertNull(executor.command);

        conn.produceOutput(serviceHandler);
        assertTrue(channel.isReadInterest());
        assertFalse(channel.isWriteInterest());
        assertNull(executor.command);
    }

    public void testReadWriteInterestPost() throws Exception {
        StubSocket socket = new StubSocket();
        StubIOSession session = new StubIOSession(socket);
        MockHttpChannel channel = new MockHttpChannel(session, eventDispatch);
        session.setHttpChannel(channel);
        MockHttpServerConnection conn = new MockHttpServerConnection(session,
                requestFactory, parms);
        assertTrue(channel.isReadInterest());
        assertFalse(channel.isWriteInterest());
        
        serviceHandler.connected(conn);
        assertTrue(channel.isReadInterest());
        assertFalse(channel.isWriteInterest());

        HttpEntityEnclosingRequest request = new BasicHttpEntityEnclosingRequest("POST", "/get/string");
        request.setEntity(new BasicHttpEntity());
        conn.setHttpRequest(request);
        conn.consumeInput(serviceHandler);
        assertTrue(channel.isReadInterest());
        assertFalse(channel.isWriteInterest());
        
        MockContentDecoder decoder = new MockContentDecoder("abc");
        conn.setContentDecoder(decoder);
        serviceHandler.inputReady(conn, decoder);
        assertTrue(channel.isReadInterest());
        assertFalse(channel.isWriteInterest());
        serviceHandler.inputReady(conn, decoder);
        assertTrue(channel.isReadInterest());
        assertFalse(channel.isWriteInterest());
        decoder.completed = true;
        serviceHandler.inputReady(conn, decoder);
        assertFalse(channel.isReadInterest());
        assertTrue(channel.isWriteInterest());
        
        // start writing the response headers
        conn.produceOutput(serviceHandler);
        assertFalse(channel.isReadInterest());
        assertTrue(channel.isWriteInterest());
        
        // write the response body
        conn.produceOutput(serviceHandler);
        assertTrue(channel.isReadInterest());
        assertFalse(channel.isWriteInterest());
    }

    public void testReadWriteInterestCloseGet() throws Exception {
        StubSocket socket = new StubSocket();
        StubIOSession session = new StubIOSession(socket);
        MockHttpChannel channel = new MockHttpChannel(session, eventDispatch);
        session.setHttpChannel(channel);
        MockHttpServerConnection conn = new MockHttpServerConnection(session,
                requestFactory, parms);
        assertTrue(channel.isReadInterest());
        assertFalse(channel.isWriteInterest());
        
        serviceHandler.connected(conn);
        assertTrue(channel.isReadInterest());
        assertFalse(channel.isWriteInterest());

        conn.setHttpRequest(new BasicHttpRequest("GET", "/get/string", HttpVersion.HTTP_1_0));
        conn.consumeInput(serviceHandler);
        assertFalse(conn.isClosed());
        assertFalse(session.isClosed());
        assertFalse(channel.isReadInterest());
        assertTrue(channel.isWriteInterest());
        
        // start writing the response headers
        conn.produceOutput(serviceHandler);
        assertFalse(channel.isReadInterest());
        assertTrue(channel.isWriteInterest());

        // write response body
        // expect service handler to send response and close connection since it is HTTP 1.0
        conn.produceOutput(serviceHandler);
        assertTrue(conn.isClosed());
        assertTrue(session.isClosed());
        assertFalse(channel.isReadInterest());
        assertTrue(channel.isWriteInterest());
    }

    public void testReadWriteInterestCloseHead() throws Exception {
        StubSocket socket = new StubSocket();
        StubIOSession session = new StubIOSession(socket);
        MockHttpChannel channel = new MockHttpChannel(session, eventDispatch);
        session.setHttpChannel(channel);
        MockHttpServerConnection conn = new MockHttpServerConnection(session,
                requestFactory, parms);
        assertTrue(channel.isReadInterest());
        assertFalse(channel.isWriteInterest());
        
        serviceHandler.connected(conn);
        assertTrue(channel.isReadInterest());
        assertFalse(channel.isWriteInterest());

        conn.setHttpRequest(new BasicHttpRequest("HEAD", "/get/string", HttpVersion.HTTP_1_0));
        conn.consumeInput(serviceHandler);
        assertFalse(conn.isClosing());
        assertFalse(conn.isClosed());
        assertFalse(session.isClosed());
        assertFalse(channel.isReadInterest());
        assertTrue(channel.isWriteInterest());
        
        // start writing the response headers
        conn.produceOutput(serviceHandler);
        assertTrue(conn.isClosing());
        assertFalse(session.isClosed());
        assertFalse(channel.isReadInterest());
        assertTrue(channel.isWriteInterest());
        
        conn.produceOutput(serviceHandler);
        assertTrue(conn.isClosed());
        assertTrue(session.isClosed());
        assertFalse(channel.isReadInterest());
        assertTrue(channel.isWriteInterest());
    }
    
    public static class MockHttpChannel extends HttpChannel {

        private StringBuilder out = new StringBuilder();

        public MockHttpChannel(HttpIOSession session,
                IOEventDispatch eventDispatch) {
            super(session, eventDispatch);
        }

        @Override
        public int write(ByteBuffer buffer) throws IOException {
            return BufferUtils.transfer(buffer, out);
        }
        
    }
    
    private static class MockExecutor implements Executor {
        private Runnable command;
        
        public void execute(Runnable command) {
            this.command = command;
        }
    }
    
}
