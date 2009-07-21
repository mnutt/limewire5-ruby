package org.limewire.facebook.service;

import java.util.Arrays;
import java.util.concurrent.Callable;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.concurrent.ListeningFuture;
import org.limewire.concurrent.ThreadPoolListeningExecutor;
import org.limewire.facebook.service.livemessage.PresenceHandlerFactory;
import org.limewire.facebook.service.settings.FacebookAuthServerUrls;
import org.limewire.friend.api.FriendConnection;
import org.limewire.friend.api.FriendConnectionConfiguration;
import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.friend.api.FriendConnectionFactory;
import org.limewire.friend.api.FriendConnectionFactoryRegistry;
import org.limewire.friend.api.FriendException;
import org.limewire.friend.api.Network;
import org.limewire.friend.api.feature.FeatureRegistry;
import org.limewire.friend.impl.feature.LimewireFeatureInitializer;
import org.limewire.http.httpclient.HttpClientInstanceUtils;
import org.limewire.http.httpclient.HttpClientUtils;
import org.limewire.lifecycle.Asynchronous;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

/**
 * A <code>FriendConnectionFactory</code> for facebook.  Facebook communication is done via their API + chat protocol.
 * The entry point for logging into facebook.  Also logs out the <code>FacebookFriendConnection</code>
 * on limewire shutdown.
 */
@Singleton
class FacebookFriendService implements FriendConnectionFactory, Service {
    
    private static Log LOG = LogFactory.getLog(FacebookFriendService.class);
    
    private final ThreadPoolListeningExecutor executorService;
    private final FacebookFriendConnectionFactory connectionFactory;

    private final PresenceHandlerFactory presenceHandlerFactory;
    private final FeatureRegistry featureRegistry;
    private volatile FacebookFriendConnection connection;

    private final Provider<String[]> authServerUrls;

    private final HttpClientInstanceUtils httpClientInstanceUtils;
    private final ClientConnectionManager httpConnectionManager;

    @Inject FacebookFriendService(FacebookFriendConnectionFactory connectionFactory,
                                  PresenceHandlerFactory presenceHandlerFactory,
                                  FeatureRegistry featureRegistry,
                                  @FacebookAuthServerUrls Provider<String[]> authServerUrls,
                                  HttpClientInstanceUtils httpClientInstanceUtils,
                                  @Named("sslConnectionManager") ClientConnectionManager httpConnectionManager) {
        this.connectionFactory = connectionFactory;
        this.presenceHandlerFactory = presenceHandlerFactory;
        this.featureRegistry = featureRegistry;
        this.authServerUrls = authServerUrls;
        this.httpClientInstanceUtils = httpClientInstanceUtils;
        this.httpConnectionManager = httpConnectionManager;
        executorService = ExecutorsHelper.newSingleThreadExecutor(ExecutorsHelper.daemonThreadFactory(getClass().getSimpleName()));         
    }
    
    @Inject
    void register(ServiceRegistry registry) {
        registry.register(this);
    }

    @Inject
    public void register(ListenerSupport<FriendConnectionEvent> listenerSupport) {
        listenerSupport.addListener(new EventListener<FriendConnectionEvent>() {
            @Override
            public void handleEvent(FriendConnectionEvent event) {
                switch (event.getType()) {
                    case DISCONNECTED:
                    case CONNECT_FAILED:
                        synchronized (FacebookFriendService.this) {
                            if(connection != null && connection == event.getSource()) {
                                connection = null;
                            }
                        }
                        break;
                    default:
                }
            }
        });
    }

    @Override
    public void start() {
        
    }

    @Override
    @Asynchronous
    public void stop() {
        logoutImpl();
    }

    @Override
    public void initialize() {
        
    }

    @Override
    public String getServiceName() {
        return getClass().getSimpleName();
    }
    
    private void logoutImpl() {
        synchronized (this) {
            if(connection != null) {
                connection.logoutImpl();
                connection = null;
            }
        }
    }

    @Override
    public ListeningFuture<FriendConnection> login(final FriendConnectionConfiguration configuration) {
        return executorService.submit(new Callable<FriendConnection>() {
            @Override
            public FriendConnection call() throws Exception {
                return loginImpl(configuration);
            }
        });
    }

    @Override
    @Inject
    public void register(FriendConnectionFactoryRegistry registry) {
        registry.register(Network.Type.FACEBOOK, this);
    }
    
    FacebookFriendConnection loginImpl(FriendConnectionConfiguration configuration) throws FriendException {
        LOG.debug("creating connection");
        connection = connectionFactory.create(configuration);
        presenceHandlerFactory.create(connection);
        new LimewireFeatureInitializer().register(featureRegistry);
        LOG.debug("logging in to facebook...");
        connection.loginImpl();
        LOG.debug("logged in.");
        return connection;
    }

    @Override
    public ListeningFuture<String> requestLoginUrl(final FriendConnectionConfiguration configuration) {
        return executorService.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                HttpParams params = new BasicHttpParams();
                HttpClientParams.setRedirecting(params, false);
                HttpClient httpClient = new DefaultHttpClient(httpConnectionManager, params); 
                String[] authUrls = authServerUrls.get();
                if (LOG.isDebugEnabled()) {
                    LOG.debugf("auth urls to choose from {0}", Arrays.asList(authUrls));
                }
                String authUrl = httpClientInstanceUtils.addClientInfoToUrl(FacebookUtils.getRandomElement(authUrls) + "getlogin/");
                LOG.debugf("picked auth url: {0}", authUrl);
                HttpGet getMethod = new HttpGet(authUrl);
                HttpResponse response = httpClient.execute(getMethod);                
                int statusCode = response.getStatusLine().getStatusCode(); 
                assert statusCode == 302 : "code: " + statusCode;
                String url = response.getFirstHeader("Location").getValue();
                LOG.debugf("login url: {0}", url);
                String authToken = parseAuthToken(url);
                if (authToken != null) {
                    configuration.setAttribute("auth-token", authToken);
                }
                HttpClientUtils.releaseConnection(response);
                return url;
            }
        });
    }

    private static String parseAuthToken(String url) {
        int authTokenIndex = url.indexOf("auth_token=");
        if(authTokenIndex > -1) {
            String authTokenPart = url.substring(authTokenIndex + "auth_token=".length());
            int nextParamIndex = authTokenPart.indexOf('&');
            if (nextParamIndex > -1) {
                return authTokenPart.substring(0, nextParamIndex);
            } else {
                // last param return it all
                return authTokenPart;
            }
        }
        LOG.debugf("could not parse out auth token: {0}", url);
        return null;
    }
    
        
}
