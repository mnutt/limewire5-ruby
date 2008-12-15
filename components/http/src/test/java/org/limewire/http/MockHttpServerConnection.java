/**
 * 
 */
package org.limewire.http;

import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestFactory;
import org.apache.http.HttpResponse;
import org.apache.http.impl.nio.DefaultNHttpServerConnection;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.NHttpServiceHandler;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.util.HeapByteBufferAllocator;
import org.apache.http.params.HttpParams;
import org.limewire.http.reactor.DefaultDispatchedIOReactor;

public class MockHttpServerConnection extends DefaultNHttpServerConnection {

    public MockHttpServerConnection(IOSession session,
            HttpRequestFactory requestFactory, HttpParams params) {
        super(session, requestFactory, new HeapByteBufferAllocator(), params);
        getContext().setAttribute(DefaultDispatchedIOReactor.IO_SESSION_KEY, session);
    }

    public void setHttpRequest(HttpRequest request) {
        this.request = request;
    }

    public void setHttpResponse(HttpResponse response) {
        this.response = response;
    }

    public void setContentEncoder(ContentEncoder encoder) {
        this.contentEncoder = encoder;
    }

    @Override
    public void consumeInput(NHttpServiceHandler handler) {
        if (this.request != null) {
            handler.requestReceived(this);
        } else {
            super.consumeInput(handler);
        }
    }

    public boolean isClosing() {
        return status == CLOSING;
    }

    public boolean isClosed() {
        return this.status == CLOSED;
    }
    
    public void setHasBufferedInput(boolean hasBufferedInput) {
        this.hasBufferedInput = hasBufferedInput;
    }

    public void setHasBufferedOutput(boolean hasBufferedOutput) {
        this.hasBufferedOutput = hasBufferedOutput;
    }

    public void setContentDecoder(MockContentDecoder contentDecoder) {
        this.contentDecoder = contentDecoder;
    }

}