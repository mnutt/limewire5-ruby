package com.limegroup.gnutella.bootstrap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.DefaultedHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.limewire.collection.Cancellable;
import org.limewire.collection.FixedSizeExpiringSet;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.http.HttpClientListener;
import com.limegroup.gnutella.http.HttpExecutor;
import com.limegroup.gnutella.util.EncodingUtils;
import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * Manages a set of gwebcaches and retrieves hosts from them. 
 */
class TcpBootstrapImpl implements TcpBootstrap {
    
    private static final Log LOG = LogFactory.getLog(TcpBootstrapImpl.class);
    
    /** The number of hosts we want to retrieve from gwebcaches at a time. */
    private static final int WANTED_HOSTS = 15;
    /** The socket timeout for HTTP connections to gwebcaches. */
    private static final int SOCKET_TIMEOUT = 5000;
    /** How many milliseconds to remember that a host has already been tried. */
    private static final int EXPIRY_TIME = 10 * 60 * 1000;
    
    private final ExecutorService bootstrapQueue =
        ExecutorsHelper.newProcessingQueue("TCP Bootstrap");
    
    /**
     * A list of gwebcaches, to allow easy sorting & randomizing.
     * A set is also maintained, to easily look up duplicates.
     * INVARIANT: hosts contains no duplicates and contains exactly
     *  the same elements and hostsSet
     * LOCKING: obtain this' monitor before modifying either */
    private final List<URI> hosts = new ArrayList<URI>();
    private final Set<URI> hostsSet = new HashSet<URI>();
    
    /**
     * A set of gwebcaches that we've recently contacted, so we don't contact
     * them again.
     */
    private final Set<URI> attemptedHosts;
    
    /**
     * Whether or not we need to resort the gwebcaches by failures.
     */
    private boolean dirty = false;
    
    private final HttpExecutor httpExecutor;
    private final Provider<HttpParams> defaultParams;
    private final ConnectionServices connectionServices;
    
    /**
     * Constructs a new TcpBootstrapImpl that remembers attempting gwebcaches
     * for the default expiry time.
     */
    @Inject
    TcpBootstrapImpl(HttpExecutor httpExecutor, @Named("defaults")
    Provider<HttpParams> defaultParams, ConnectionServices connectionServices) {
        this.httpExecutor = httpExecutor;
        this.defaultParams = defaultParams;
        this.connectionServices = connectionServices;
        this.attemptedHosts = new FixedSizeExpiringSet<URI>(100, EXPIRY_TIME);
    }
    
    /**
     * Clears the set of attempted gwebcaches.
     */
    @Override
    public synchronized void resetData() {
        LOG.debug("Clearing attempted TCP host caches");
        attemptedHosts.clear();
    }
    
    /**
     * Attempts to contact a gwebcache to retrieve endpoints.
     */
    @Override
    public synchronized boolean fetchHosts(Bootstrapper.Listener listener) {
        // If the hosts have been used, shuffle them
        if(dirty) {
            LOG.debug("Shuffling TCP host caches");
            Collections.shuffle(hosts);
            dirty = false;
        }
        List<HttpUriRequest> requests = new ArrayList<HttpUriRequest>();
        Map<HttpUriRequest, URI> requestToHost = new HashMap<HttpUriRequest, URI>();
        for(URI host : hosts) {
            if(attemptedHosts.contains(host)) {
                if(LOG.isDebugEnabled())
                    LOG.debug("Already attempted " + host);
                continue;            
            }
            HttpUriRequest request = newRequest(host);
            requests.add(request);
            requestToHost.put(request, host);
        }
        
        if(requests.isEmpty()) {
            LOG.debug("No TCP host caches to try");
            return false;
        }
        
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, SOCKET_TIMEOUT);
        HttpConnectionParams.setSoTimeout(params, SOCKET_TIMEOUT);
        params = new DefaultedHttpParams(params, defaultParams.get());
        
        if(LOG.isDebugEnabled())
            LOG.debug("Trying 1 of " + requests.size() + " TCP host caches");
        
        httpExecutor.executeAny(new Listener(requestToHost, listener),
                bootstrapQueue,
                requests,
                params,
                new Cancellable() {
                      public boolean isCancelled() {
                          return connectionServices.isConnected();
                      }
                });
        return true;        
    }
    
    private HttpUriRequest newRequest(URI host) {
        host = URI.create(host.toString() 
           + "?hostfile=1"
           + "&client=" + LimeWireUtils.QHD_VENDOR_NAME
           + "&version=" + EncodingUtils.encode(LimeWireUtils.getLimeWireVersion()));
        HttpGet get = new HttpGet(host);
        get.addHeader("Cache-Control", "no-cache");
        return get;
    }

    private int parseResponse(HttpResponse response, Bootstrapper.Listener listener) {
        if(response.getEntity() == null) {
            LOG.warn("No response entity!");
            return 0;
        }
        
        String line = null;
        List<Endpoint> endpoints = new ArrayList<Endpoint>();
        try {
            InputStream in = response.getEntity().getContent();
            String charset = EntityUtils.getContentCharSet(response.getEntity());
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, charset != null ? charset : HTTP.DEFAULT_CONTENT_CHARSET));
            while((line = reader.readLine()) != null && line.length() > 0) {
                Endpoint host=new Endpoint(line, true); // only accept numeric addresses.
                endpoints.add(host);
            }
        } catch (IllegalArgumentException bad) {
            LOG.error("IAE", bad);
        } catch (IOException e) {
            LOG.error("IOX", e);
        }
        
        if(!endpoints.isEmpty()) {
            return listener.handleHosts(endpoints);
        } else {
            LOG.debug("no endpoints sent");
            return 0;
        }
    }
    
    private class Listener implements HttpClientListener {
        private final Map<HttpUriRequest, URI> hosts;
        private final Bootstrapper.Listener listener;
        private int totalAdded = 0;
        
        Listener(Map<HttpUriRequest, URI> hosts, Bootstrapper.Listener listener) {
            this.hosts = hosts;
            this.listener = listener;
        }
        
        @Override
        public boolean requestComplete(HttpUriRequest request, HttpResponse response) {
            if(LOG.isDebugEnabled())
                LOG.debug("Completed request: " + request.getRequestLine());
            synchronized(TcpBootstrapImpl.this) {
                attemptedHosts.add(hosts.remove(request));
            }
            totalAdded += parseResponse(response, listener);
            httpExecutor.releaseResources(response);
            return totalAdded < WANTED_HOSTS;
        }
        
        @Override
        public boolean requestFailed(HttpUriRequest request, HttpResponse response, IOException exc) {
            if(LOG.isDebugEnabled()) {
                LOG.debug("Failed request: " + request.getRequestLine());
                if(response != null)
                    LOG.debug("Response " + response);
                if(exc != null)
                    LOG.debug(exc);
            }
            synchronized (TcpBootstrapImpl.this) {
                attemptedHosts.add(hosts.remove(request));                
            }
            httpExecutor.releaseResources(response);
            return true;
        }

        @Override
        public boolean allowRequest(HttpUriRequest request) {
            // Do not allow the request if we don't know about it or it was already attempted.
            synchronized(TcpBootstrapImpl.this) {
                return hosts.containsKey(request) && !attemptedHosts.contains(hosts.get(request));
            }
        }
    }
    
    /**
     * Adds a new gwebcache to the set. Protected for testing.
     */
    protected synchronized boolean add(URI e) {
        if(hostsSet.contains(e)) {
            LOG.debugf("Not adding known TCP host cache {0}", e);
            return false;
        }        
        LOG.debugf("Adding TCP host cache {0}", e);
        hosts.add(e);
        hostsSet.add(e);
        dirty = true; // Shuffle before using
        return true;
    }
    
    /**
     * Loads the default set of gwebcaches.
     */
    @Override
    public void loadDefaults() {
        // ADD DEFAULT HOST CACHES HERE.
    }
}
