/*
 * $HeadURL: http://svn.apache.org/repos/asf/jakarta/httpcomponents/httpcore/trunk/module-nio/src/test/java/org/apache/http/nio/mockup/TestHttpServer.java $
 * $Revision: 1.7 $
 * $Date: 2008/03/20 15:29:54 $
 *
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package com.limegroup.gnutella.http;

import java.io.IOException;

import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.nio.DefaultServerIOEventDispatch;
import org.apache.http.nio.protocol.AsyncNHttpServiceHandler;
import org.apache.http.nio.protocol.EventListener;
import org.apache.http.nio.protocol.NHttpRequestHandlerRegistry;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.limewire.http.reactor.DefaultDispatchedIOReactor;
import org.limewire.nio.NIODispatcher;

public class HttpTestServer {

    private final NHttpRequestHandlerRegistry registry;

    private DefaultDispatchedIOReactor reactor;

    private HttpParams params = new BasicHttpParams();

    public HttpTestServer(HttpParams params) throws IOException {
        this.params = params;
        this.registry = new NHttpRequestHandlerRegistry();
    }

    public void execute(EventListener listener) throws IOException {
        BasicHttpProcessor processor = new BasicHttpProcessor();
        processor.addInterceptor(new ResponseDate());
        processor.addInterceptor(new ResponseServer());
        processor.addInterceptor(new ResponseContent());
        processor.addInterceptor(new ResponseConnControl());

        AsyncNHttpServiceHandler serviceHandler = new AsyncNHttpServiceHandler(
                processor, new DefaultHttpResponseFactory(),
                new DefaultConnectionReuseStrategy(), params);

        serviceHandler.setEventListener(listener);

        serviceHandler.setHandlerResolver(this.registry);

        reactor = new DefaultDispatchedIOReactor(params, NIODispatcher.instance().getScheduledExecutorService());
        IOEventDispatch ioEventDispatch = new DefaultServerIOEventDispatch(
                serviceHandler, params);
        reactor.execute(ioEventDispatch);
    }

    public DefaultDispatchedIOReactor getReactor() {
        return reactor;
    }

}
