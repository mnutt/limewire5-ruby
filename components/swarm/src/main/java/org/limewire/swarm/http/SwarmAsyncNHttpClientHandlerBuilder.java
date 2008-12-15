package org.limewire.swarm.http;

import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.nio.protocol.AsyncNHttpClientHandler;
import org.apache.http.nio.protocol.NHttpRequestExecutionHandler;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;
import org.limewire.http.protocol.SynchronizedHttpProcessor;

public class SwarmAsyncNHttpClientHandlerBuilder {
    private final AsyncNHttpClientHandler clientHandler;

    public SwarmAsyncNHttpClientHandlerBuilder(HttpParams params,
            NHttpRequestExecutionHandler execHandler) {
        
        SynchronizedHttpProcessor httpProcessor = new SynchronizedHttpProcessor();
        httpProcessor.addInterceptor(new RequestContent());
        httpProcessor.addInterceptor(new RequestTargetHost());
        httpProcessor.addInterceptor(new RequestConnControl());
        httpProcessor.addInterceptor(new RequestUserAgent());
        httpProcessor.addInterceptor(new RequestExpectContinue());

        ConnectionReuseStrategy connectionReuseStrategy = new DefaultConnectionReuseStrategy();
        clientHandler = new AsyncNHttpClientHandler(httpProcessor, execHandler,
                connectionReuseStrategy, params);
    }

    public AsyncNHttpClientHandler get() {
        return clientHandler;
    }

}
