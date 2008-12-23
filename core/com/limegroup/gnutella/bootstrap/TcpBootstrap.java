package com.limegroup.gnutella.bootstrap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
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

public class TcpBootstrap {
    
    private static final Log LOG = LogFactory.getLog(TcpBootstrap.class);
    
    /** The total number of hosts we want to retrieve from web caches at a time. */
    private final int WANTED_HOSTS = 15;
    
    private final ExecutorService bootstrapQueue = ExecutorsHelper.newProcessingQueue("TCP Bootstrap");
    
    /**
     * A list of host caches, to allow easy sorting & randomizing.
     * For convenience, a Set is also maintained, to easily look up duplicates.
     * INVARIANT: hosts contains no duplicates and contains exactly
     *  the same elements and hostsSet
     * LOCKING: obtain this' monitor before modifying either */
    private final List<URI> hosts = new ArrayList<URI>();
    private final Set<URI> hostsSet = new HashSet<URI>();
    
    /**
     * A set of hosts who we've recently contacted, so we don't contact them
     * again.
     */
    private final Set<URI> attemptedHosts;
    
    /**
     * Whether or not we need to resort the hosts by failures.
     */
    private boolean dirty = false;
    
    private final HttpExecutor httpExecutor;
    private final Provider<HttpParams> defaultParams;
    private final ConnectionServices connectionServices;
    
    /**
     * Constructs a new TcpBootstrap that remembers attempting hosts for 10
     * minutes.
     */
    @Inject
    protected TcpBootstrap(HttpExecutor httpExecutor, @Named("defaults")
    Provider<HttpParams> defaultParams, ConnectionServices connectionServices) {
        this(10 * 60 * 1000, httpExecutor, defaultParams, connectionServices);
    }

    /**
     * Constructs a new TcpBootstrap that remembers attempting hosts for the
     * given amount of time, in msecs.
     * 
     * @param connectionServices
     */
    protected TcpBootstrap(long expiryTime, HttpExecutor httpExecutor,
            Provider<HttpParams> defaultParams, ConnectionServices connectionServices) {
        this.httpExecutor = httpExecutor;
        this.defaultParams = defaultParams;
        this.connectionServices = connectionServices;
        this.attemptedHosts = new FixedSizeExpiringSet<URI>(100, expiryTime);
    }
    
    /**
     * Returns the number of Host Caches this knows about.
     */
    public synchronized int getSize() {
        return hostsSet.size();
    }
    
    /**
     * Erases the attempted hosts & decrements the failure counts.
     */
    public synchronized void resetData() {
        LOG.debug("Clearing attempted TCP host caches");
        attemptedHosts.clear();
    }
    
    /**
     * Attempts to contact a host cache to retrieve endpoints.
     */
    public synchronized boolean fetchHosts(TcpBootstrapListener listener) {
        // If the hosts have been used, shuffle them
        if(dirty) {
            LOG.debug("Shuffling TCP host caches");
            Collections.shuffle(hosts);
            dirty = false;
        }
        
        return doFetch(listener);
    }
    
    private boolean doFetch(TcpBootstrapListener listener) {
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
        HttpConnectionParams.setConnectionTimeout(params, 5000);
        HttpConnectionParams.setSoTimeout(params, 5000);
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

    private int parseResponse(HttpResponse response, TcpBootstrapListener listener) {
        if(response.getEntity() == null) {
            LOG.warn("No response entity!");
            return 0;
        }
        
        String line = null;
        List<Endpoint> endpoints = new ArrayList<Endpoint>();
        try {
            InputStream in = response.getEntity().getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            while((line = reader.readLine()) != null) {
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
        private final TcpBootstrapListener listener;
        private int totalAdded = 0;
        
        Listener(Map<HttpUriRequest, URI> hosts, TcpBootstrapListener listener) {
            this.hosts = hosts;
            this.listener = listener;
        }
        
        @Override
        public boolean requestComplete(HttpUriRequest request, HttpResponse response) {
            if(LOG.isDebugEnabled())
                LOG.debug("Completed request: " + request.getRequestLine());
            synchronized(TcpBootstrap.this) {
                attemptedHosts.add(hosts.remove(request));
            }
            totalAdded += parseResponse(response, listener);
            httpExecutor.releaseResources(response);
            return totalAdded < WANTED_HOSTS;
        }
        
        @Override
        public boolean requestFailed(HttpUriRequest request, HttpResponse response, IOException exc) {
            if(LOG.isDebugEnabled())
                LOG.debug("Failed request: " + request.getRequestLine());
            synchronized (TcpBootstrap.this) {
                attemptedHosts.add(hosts.remove(request));                
            }
            httpExecutor.releaseResources(response);
            return true;
        }

        @Override
        public boolean allowRequest(HttpUriRequest request) {
            // Do not allow the request if we don't know about it or it was already attempted.
            synchronized(TcpBootstrap.this) {
                return hosts.containsKey(request) && !attemptedHosts.contains(hosts.get(request));
            }
        }
    }
    
    /**
     * Adds a new hostcache to this.
     */
    public synchronized boolean add(URI e) {
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
    
    public void loadDefaults() {
        // ADD DEFAULT HOST CACHES HERE.
    }
    
    public static interface TcpBootstrapListener {
        /** Notification that some number of hosts were found.  Returns the number that are used. */
        int handleHosts(Collection<? extends Endpoint> hosts);
    }
}
