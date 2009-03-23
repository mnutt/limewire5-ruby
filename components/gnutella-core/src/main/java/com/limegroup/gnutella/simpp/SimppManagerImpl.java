package com.limegroup.gnutella.simpp;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.DefaultedHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.limewire.core.settings.UpdateSettings;
import org.limewire.io.IOUtils;
import org.limewire.util.Clock;
import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.NetworkUpdateSanityChecker;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.NetworkUpdateSanityChecker.RequestType;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.http.HttpClientListener;
import com.limegroup.gnutella.http.HttpExecutor;
import com.limegroup.gnutella.settings.SimppSettingsManager;
import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * Used for managing signed messages published by LimeWire, and changing settings
 * as necessary.
 */
@Singleton
public class SimppManagerImpl implements SimppManager {
    
    private static final Log LOG = LogFactory.getLog(SimppManagerImpl.class);
    
    private static int MIN_VERSION = 3;
   
    private static final String FILENAME = "simpp.xml";    
    private static final Random RANDOM = new Random();
    private static final int IGNORE_ID = Integer.MAX_VALUE;
   
    /** Cached Simpp bytes in case we need to sent it out on the wire */
    private volatile byte[] _lastBytes = new byte[0];
    private volatile String _lastString = "";    
    private volatile int _lastId = MIN_VERSION;
    
    /** If an HTTP failover update is in progress */
    private final HttpRequestControl httpRequestControl = new HttpRequestControl();

    private final List<SimppListener> listeners = new CopyOnWriteArrayList<SimppListener>();
    private final CopyOnWriteArrayList<SimppSettingsManager> simppSettingsManagers;    
    private final Provider<NetworkUpdateSanityChecker> networkUpdateSanityChecker;
    private final ApplicationServices applicationServices;    
    private final Clock clock;
    private final Provider<HttpExecutor> httpExecutor;
    private final ScheduledExecutorService backgroundExecutor;
    private final Provider<HttpParams> defaultParams;
    private final SimppDataProvider simppDataProvider;
    
    private volatile List<String> maxedUpdateList = Arrays.asList("http://simpp1.limewire.com/v2/simpp.def",
            "http://simpp2.limewire.com/v2/simpp.def",
            "http://simpp3.limewire.com/v2/simpp.def",
            "http://simpp4.limewire.com/v2/simpp.def",
            "http://simpp5.limewire.com/v2/simpp.def",
            "http://simpp6.limewire.com/v2/simpp.def",
            "http://simpp7.limewire.com/v2/simpp.def",
            "http://simpp8.limewire.com/v2/simpp.def",
            "http://simpp9.limewire.com/v2/simpp.def",
            "http://simpp10.limewire.com/v2/simpp.def");
    private volatile int minMaxHttpRequestDelay = 1000 * 60;
    private volatile int maxMaxHttpRequestDelay = 1000 * 60 * 30;
    private volatile int silentPeriodForMaxHttpRequest = 1000 * 60 * 5;
    
    private static enum UpdateType {
        FROM_NETWORK, FROM_DISK, FROM_HTTP;
    }
    
    @Inject
    public SimppManagerImpl(Provider<NetworkUpdateSanityChecker> networkUpdateSanityChecker, Clock clock,
            ApplicationServices applicationServices, Provider<HttpExecutor> httpExecutor,
            @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
            @Named("defaults") Provider<HttpParams> defaultParams,
            SimppDataProvider simppDataProvider) {
        this.networkUpdateSanityChecker = networkUpdateSanityChecker;
        this.clock = clock;
        this.applicationServices = applicationServices;
        this.simppSettingsManagers = new CopyOnWriteArrayList<SimppSettingsManager>();
        this.httpExecutor = httpExecutor;
        this.backgroundExecutor = backgroundExecutor;
        this.defaultParams = defaultParams;
        this.simppDataProvider = simppDataProvider;
    }
    
    List<String> getMaxUrls() {
        return maxedUpdateList;
    }
    
    void setMaxUrls(List<String> urls) {
        this.maxedUpdateList = urls;
    }
    
    int getMinHttpRequestUpdateDelayForMaxFailover() {
        return minMaxHttpRequestDelay;
    }
    
    int getMaxHttpRequestUpdateDelayForMaxFailover() {
        return maxMaxHttpRequestDelay;
    }
    
    void setMinHttpRequestUpdateDelayForMaxFailover(int min) {
        minMaxHttpRequestDelay = min;
    }
    
    void setMaxHttpRequestUpdateDelayForMaxFailover(int max) {
        maxMaxHttpRequestDelay = max;
    }
    
    int getSilentPeriodForMaxHttpRequest() {
        return silentPeriodForMaxHttpRequest;
    }
    
    void setSilentPeriodForMaxHttpRequest(int silentPeriodForMaxHttpRequest) {
        this.silentPeriodForMaxHttpRequest = silentPeriodForMaxHttpRequest;
    }
    
    
    public void initialize() {
        LOG.trace("Initializing SimppManager");
        backgroundExecutor.execute(new Runnable() {
            public void run() {
                handleDataInternal(FileUtils.readFileFully(new File(CommonUtils
                        .getUserSettingsDir(), FILENAME)), UpdateType.FROM_DISK, null);
                handleDataInternal(simppDataProvider.getDefaultData(), UpdateType.FROM_DISK, null);
            }
        });
    }
        
    public int getVersion() {
        return _lastId;
    }
    
    /**
     * @return the cached value of the simpp bytes. 
     */ 
    public byte[] getSimppBytes() {
        return _lastBytes;
    }

    public String getPropsString() {
        return _lastString;
    }
    
    public void addSimppSettingsManager(SimppSettingsManager simppSettingsManager) {
        simppSettingsManagers.add(simppSettingsManager);
    }

    public List<SimppSettingsManager> getSimppSettingsManagers() {
        return simppSettingsManagers;
    }
    
    public void addListener(SimppListener listener) {
        listeners.add(listener);
    }
    
    public void removeListener(SimppListener listener) {
        listeners.remove(listener);
    }
    
    public void checkAndUpdate(final ReplyHandler handler, final byte[] data) {
        if(data != null) {
            backgroundExecutor.execute(new Runnable() {
                public void run() {
                    LOG.trace("Parsing new data...");
                    handleDataInternal(data, UpdateType.FROM_NETWORK, handler);
                }
            });
        }
    }
    
    private void handleDataInternal(byte[] data, UpdateType updateType, ReplyHandler handler) {
        if (data == null) {
            if (updateType == UpdateType.FROM_NETWORK && handler != null)
                networkUpdateSanityChecker.get().handleInvalidResponse(handler, RequestType.SIMPP);
            LOG.warn("No data to handle.");
            return;
        }
        
        SimppDataVerifier verifier=new SimppDataVerifier(data);
        if(!verifier.verifySource()) {
            if(updateType == UpdateType.FROM_NETWORK && handler != null)
                networkUpdateSanityChecker.get().handleInvalidResponse(handler, RequestType.SIMPP);
            LOG.warn("Couldn't verify signature on data.");
            return;
        }
        
        if(updateType == UpdateType.FROM_NETWORK && handler != null)
            networkUpdateSanityChecker.get().handleValidResponse(handler, RequestType.SIMPP);
        
        SimppParser parser = null;
        try {
            parser = new SimppParser(verifier.getVerifiedData());
        } catch(IOException iox) {
            LOG.error("IOX parsing simpp data", iox);
            return;
        }
        
        if(LOG.isDebugEnabled()) {
            LOG.debug("Got data with version: " + parser.getVersion() + " from: " + updateType + ", current version is: " + _lastId);
        }
        
        switch(updateType) {
        case FROM_NETWORK:
            if(parser.getVersion() == IGNORE_ID) {
                if(_lastId != IGNORE_ID)
                    doHttpMaxFailover();
            } else if(parser.getVersion() > _lastId) {
                storeAndUpdate(data, parser, updateType);
            }
            break;
        case FROM_DISK:
            if(parser.getVersion() > _lastId) {
                storeAndUpdate(data, parser, updateType);
            }
            break;
        case FROM_HTTP:
            if(parser.getVersion() >= _lastId) {
                storeAndUpdate(data, parser, updateType);
            }
            break;
        }
    }
    
    private void storeAndUpdate(byte[] data, SimppParser parser, UpdateType updateType) {
        if(LOG.isTraceEnabled())
            LOG.trace("Retrieved new data from: " + updateType + ", storing & updating");
        if(parser.getVersion() == IGNORE_ID && updateType == UpdateType.FROM_NETWORK)
            throw new IllegalStateException("shouldn't be here!");
        
        if(updateType == UpdateType.FROM_NETWORK && httpRequestControl.isRequestPending())
            return;
        
        _lastId = parser.getVersion();
        _lastBytes = data;
        _lastString = parser.getPropsData();
        
        if(updateType != UpdateType.FROM_DISK) {
            FileUtils.verySafeSave(CommonUtils.getUserSettingsDir(), FILENAME, data);
        }
        
        for(SimppSettingsManager ssm : simppSettingsManagers)
            ssm.updateSimppSettings(_lastString);
        for (SimppListener listener : listeners)
            listener.simppUpdated(_lastId);
    }
    
    private void doHttpMaxFailover() {
        long maxTimeAgo = clock.now() - silentPeriodForMaxHttpRequest; 
        if(!httpRequestControl.requestQueued(HttpRequestControl.RequestReason.MAX) &&
                UpdateSettings.LAST_SIMPP_FAILOVER.getValue() < maxTimeAgo) {
            int rndDelay = RANDOM.nextInt(maxMaxHttpRequestDelay) + minMaxHttpRequestDelay;
            final String rndUri = maxedUpdateList.get(RANDOM.nextInt(maxedUpdateList.size()));
            LOG.debug("Scheduling http max failover in: " + rndDelay + ", to: " + rndUri);
            backgroundExecutor.schedule(new Runnable() {
                public void run() {
                    String url = rndUri;
                    try {
                        launchHTTPUpdate(url);
                    } catch (URISyntaxException e) {
                        httpRequestControl.requestFinished();
                        httpRequestControl.cancelRequest();
                        LOG.warn("uri failure", e);
                    }
                }
            }, rndDelay, TimeUnit.MILLISECONDS);
        } else {
            LOG.debug("Ignoring http max failover.");
        }
    }
    
    /**
     * Launches an http update to the failover url.
     */
    private void launchHTTPUpdate(String url) throws URISyntaxException {
        if (!httpRequestControl.isRequestPending())
            return;
        LOG.debug("about to issue http request method");
        HttpGet get = new HttpGet(LimeWireUtils.addLWInfoToUrl(url, applicationServices.getMyGUID()));
        get.addHeader("User-Agent", LimeWireUtils.getHttpServer());
        get.addHeader(HTTPHeaderName.CONNECTION.httpStringValue(),"close");
        httpRequestControl.requestActive();
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, 10000);
        HttpConnectionParams.setSoTimeout(params, 10000);
        params = new DefaultedHttpParams(params, defaultParams.get());
        httpExecutor.get().execute(get, params, new RequestHandler());
    }
    
    public byte[] getOldUpdateResponse() {
        return simppDataProvider.getOldUpdateResponse();
    }

    
    private class RequestHandler implements HttpClientListener {

        public boolean requestComplete(HttpUriRequest request, HttpResponse response) {
            LOG.debug("http request method succeeded");
            
            // remember we made an attempt even if it didn't succeed
            UpdateSettings.LAST_SIMPP_FAILOVER.setValue(clock.now());
            final byte[] inflated;
            try {
                if (response.getStatusLine().getStatusCode() < 200
                        || response.getStatusLine().getStatusCode() >= 300)
                    throw new IOException("bad code " + response.getStatusLine().getStatusCode());
    
                byte [] resp = null;
                if(response.getEntity() != null) {
                    resp = IOUtils.readFully(response.getEntity().getContent());
                }
                if (resp == null || resp.length == 0)
                    throw new IOException("bad body");
    
                // inflate the response and process.
                inflated = IOUtils.inflate(resp);
            } catch (IOException failed) {
                httpRequestControl.requestFinished();
                LOG.warn("couldn't fetch data ",failed);
                return false;
            } finally {
                httpExecutor.get().releaseResources(response);
            }
            
            // Handle the data in the background thread.
            backgroundExecutor.execute(new Runnable() {
                public void run() {
                    httpRequestControl.requestFinished();
                    
                    LOG.trace("Parsing new data...");
                    handleDataInternal(inflated, UpdateType.FROM_HTTP, null);
                }
            });
            
            return false; // no more requests
        }
        
        public boolean requestFailed(HttpUriRequest request, HttpResponse response, IOException exc) {
            LOG.warn("http failover failed",exc);
            httpRequestControl.requestFinished();
            UpdateSettings.LAST_SIMPP_FAILOVER.setValue(clock.now());
            
            httpExecutor.get().releaseResources(response);
            // nothing we can do.
            return false;
        }

        @Override
        public boolean allowRequest(HttpUriRequest request) {
            return true;
        }
    }
    
    /**
     * A simple control to let the flow of HTTP requests happen differently
     * depending on why it was requested.
     */
    private static class HttpRequestControl {
        private static enum RequestReason { MAX };
        
        private final AtomicBoolean requestQueued = new AtomicBoolean(false);
        private final AtomicBoolean requestActive = new AtomicBoolean(false);
        private volatile RequestReason requestReason;
        
        /** Returns true if a request is queued or active. */
        boolean isRequestPending() {
            return requestActive.get() || requestQueued.get();
        }
        
        /** Sets a queued request and returns true if a request is pending or active. */
        boolean requestQueued(RequestReason reason) {
            boolean prior = requestQueued.getAndSet(true);
            if(!prior || reason == RequestReason.MAX) // upgrade reason
                requestReason = reason;
            return prior || requestActive.get();
        }
        
        /** Sets a request to be active. */
        void requestActive() {
            requestActive.set(true);
            requestQueued.set(false);
        }
        
        /** Returns the reason the last request was queueud. */
        RequestReason getRequestReason() {
            return requestReason;
        }
        
        void cancelRequest() {
            requestQueued.set(false);
        }
        
        void requestFinished() {
            requestActive.set(false);
        }
    }
    
}
