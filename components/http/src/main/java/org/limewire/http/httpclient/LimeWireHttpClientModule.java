package org.limewire.http.httpclient;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.http.client.HttpClient;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.limewire.concurrent.AbstractLazySingletonProvider;
import org.limewire.net.SocketsManager;
import org.limewire.nio.NBSocket;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

public class LimeWireHttpClientModule extends AbstractModule {
    
    @Override
    protected void configure() {        
        bind(ReapingClientConnectionManager.class).annotatedWith(Names.named("nonBlockingConnectionManager")).toProvider(LimeClientConnectionManagerProvider.class).in(Scopes.SINGLETON);
        bind(ReapingClientConnectionManager.class).annotatedWith(Names.named("socketWrappingConnectionManager")).toProvider(SocketWrappingClientConnectionManagerProvider.class).in(Scopes.SINGLETON);
        bind(LimeHttpClient.class).toProvider(NonBlockingLimeHttpClientProvider.class);
        bind(HttpClient.class).toProvider(NonBlockingLimeHttpClientProvider.class);
        bind(SocketWrappingHttpClient.class).toProvider(SocketWrappingLimeHttpClientProvider.class);
        bind(SchemeRegistry.class).annotatedWith(Names.named("limeSchemeRegistry")).toProvider(LimeSchemeRegistryProvider.class);
        bind(SchemeRegistry.class).annotatedWith(Names.named("socketWrappingSchemeRegistry")).toProvider(SocketWrappingSchemeRegistryProvider.class);
        bind(SocketWrapperProtocolSocketFactory.class);
        bind(HttpParams.class).annotatedWith(Names.named("defaults")).toProvider(DefaultHttpParamsProvider.class);
    }
    
    private abstract static class AbstractLimeHttpClientProvider implements Provider<SocketWrappingHttpClient> {
        private ReapingClientConnectionManager manager;
        private final Provider<HttpParams> defaultParams;

        public AbstractLimeHttpClientProvider(ReapingClientConnectionManager manager, Provider<HttpParams> defaultParams) {
            this.manager = manager;
            this.defaultParams = defaultParams;
        }

        public SocketWrappingHttpClient get() {
            return new LimeHttpClientImpl(manager, defaultParams);
        }
    }
    
    @Singleton
    private static class NonBlockingLimeHttpClientProvider extends AbstractLimeHttpClientProvider {
        @Inject
        public NonBlockingLimeHttpClientProvider(@Named("nonBlockingConnectionManager") ReapingClientConnectionManager manager, @Named("defaults") Provider<HttpParams> defaultParams) {
            super(manager, defaultParams);
        }        
    }
    
    @Singleton
    private static class SocketWrappingLimeHttpClientProvider extends AbstractLimeHttpClientProvider {
        @Inject
        public SocketWrappingLimeHttpClientProvider(@Named("socketWrappingConnectionManager") ReapingClientConnectionManager manager, @Named("defaults") Provider<HttpParams> defaultParams) {
            super(manager, defaultParams);
        }        
    }
    
    @Singleton
    private static class LimeSchemeRegistryProvider extends AbstractLazySingletonProvider<SchemeRegistry> {
        private final Provider<SocketsManager> socketsManager;
        
        @Inject
        public LimeSchemeRegistryProvider(Provider<SocketsManager> socketsManager) {
            this.socketsManager = socketsManager;
        }

        @Override
        protected SchemeRegistry createObject() {
            SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme("http", new LimeSocketFactory(socketsManager, SocketsManager.ConnectType.PLAIN), 80));
            registry.register(new Scheme("tls", new LimeSocketFactory(socketsManager, SocketsManager.ConnectType.TLS),80));
            registry.register(new Scheme("https", new LimeSocketFactory(socketsManager, SocketsManager.ConnectType.TLS),80));
            return registry;
        }
    }
    
    @Singleton
    private static class SocketWrappingSchemeRegistryProvider extends AbstractLazySingletonProvider<SchemeRegistry> {

        @Override
        protected SchemeRegistry createObject() {
            SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme("http", new SocketWrapperProtocolSocketFactory(), 80));
            registry.register(new Scheme("tls", new SocketWrapperProtocolSocketFactory(),80));
            registry.register(new Scheme("https", new SocketWrapperProtocolSocketFactory(),80));
            return registry;
        }
    }
    
    private abstract static class AbstractClientConnectionManagerProvider extends AbstractLazySingletonProvider<ReapingClientConnectionManager> {
        private final Provider<SchemeRegistry> registry;
        private final Provider<ScheduledExecutorService> scheduler;
        private final Provider<HttpParams> defaultParams;

        public AbstractClientConnectionManagerProvider(Provider<SchemeRegistry> registry, Provider<ScheduledExecutorService> scheduler, Provider<HttpParams> defaultParams) {
            this.registry = registry;
            this.scheduler = scheduler;
            this.defaultParams = defaultParams;
        }

        @Override
        public ReapingClientConnectionManager createObject() {
            return new ReapingClientConnectionManager(registry, scheduler, defaultParams);
        }
    }
    
    @Singleton
    private static class LimeClientConnectionManagerProvider extends AbstractClientConnectionManagerProvider {

        @Inject
        public LimeClientConnectionManagerProvider(@Named("limeSchemeRegistry")Provider<SchemeRegistry> registry, @Named("backgroundExecutor") Provider<ScheduledExecutorService> scheduler, @Named("defaults") Provider<HttpParams> defaultParams) {
            super(registry, scheduler, defaultParams);
        }
    }
    
    @Singleton
    private static class SocketWrappingClientConnectionManagerProvider extends AbstractClientConnectionManagerProvider {

        @Inject
        public SocketWrappingClientConnectionManagerProvider(@Named("socketWrappingSchemeRegistry")Provider<SchemeRegistry> registry, @Named("backgroundExecutor") Provider<ScheduledExecutorService> scheduler, @Named("defaults") Provider<HttpParams> defaultParams) {
            super(registry, scheduler, defaultParams);
        }
    }
    
    private static class LimeSocketFactory implements SocketFactory {
        final Provider<SocketsManager> socketsManager;
        final SocketsManager.ConnectType type;
        
        public LimeSocketFactory(Provider<SocketsManager> socketsManager, SocketsManager.ConnectType type) {
            this.socketsManager = socketsManager;
            this.type = type;
        }

        public Socket createSocket() throws IOException {
            return socketsManager.get().create(type);
        }

        public Socket connectSocket(Socket socket, String targetHost, int targetPort, InetAddress localAddress, int localPort, HttpParams httpParams) throws IOException, UnknownHostException, ConnectTimeoutException {
            if(socket == null) {
                socket = createSocket();
            }
            InetSocketAddress localSocketAddr = null;
            if((localAddress != null && !localAddress.isAnyLocalAddress()) || localPort > 0) {
                localSocketAddr = new InetSocketAddress(localAddress, localPort);
            }
            return socketsManager.get().connect((NBSocket)socket, localSocketAddr, new InetSocketAddress(targetHost,targetPort), HttpConnectionParams.getConnectionTimeout(httpParams), type);
        }

        public boolean isSecure(Socket socket) throws IllegalArgumentException {
            return false; // TODO type.equals(SocketsManager.ConnectType.TLS);  // TODO use socket instead?
        }        
    }
    
    @Singleton
    private static class DefaultHttpParamsProvider implements Provider<HttpParams>{
        /**
         * The amount of time to wait while trying to connect to a specified
         * host via TCP.  If we exceed this value, an IOException is thrown
         * while trying to connect.
         */
        private static final int CONNECTION_TIMEOUT = 5000;
        
        /**
         * The amount of time to wait while receiving data from a specified
         * host.  Used as an SO_TIMEOUT.
         */
        private static final int TIMEOUT = 8000;
        
        /**
         * The maximum number of times to allow redirects from hosts.
         */
        private static final int MAXIMUM_REDIRECTS = 10;
        
        @Inject
        private DefaultHttpParamsProvider(){}
        
        public HttpParams get() {
            BasicHttpParams params = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);
            HttpConnectionParams.setSoTimeout(params, TIMEOUT);
            HttpClientParams.setRedirecting(params, true);
            params.setIntParameter(ClientPNames.MAX_REDIRECTS, MAXIMUM_REDIRECTS);
            return params;
        }
    }

}
