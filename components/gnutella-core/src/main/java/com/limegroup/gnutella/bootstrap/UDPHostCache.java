package com.limegroup.gnutella.bootstrap;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.Cancellable;
import org.limewire.collection.FixedSizeExpiringSet;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.io.NetworkUtils;
import org.limewire.net.address.StrictIpPortSet;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.ExtendedEndpoint;
import com.limegroup.gnutella.MessageListener;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.UDPReplyHandler;
import com.limegroup.gnutella.UniqueHostPinger;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.PingRequestFactory;

/**
 * A collection of UDP Host Caches.
 */
@Singleton
public class UDPHostCache {
    
    private static final Log LOG = LogFactory.getLog(UDPHostCache.class);
    
    /**
     * The maximum number of failures to allow for a given cache.
     */
    private static final int MAXIMUM_FAILURES = 5;
    
    /**
     * The total number of udp host caches to remember between
     * launches, or at any given time.
     */
    public static final int PERMANENT_SIZE = 100;
    
    /**
     * The number of hosts we try to fetch from at once.
     */
    public static final int FETCH_AMOUNT = 5;
    
    /**
     * How many milliseconds to wait before retrying a cache.
     * FIXME: this is too long, we will have given up before retrying
     */
    public static final int EXPIRY_TIME = 10 * 60 * 1000;
    
    /**
     * A list of UDP Host caches, to allow easy sorting & randomizing.
     * For convenience, a Set is also maintained, to easily look up duplicates.
     * INVARIANT: udpHosts contains no duplicates and contains exactly
     *  the same elements and udpHostsSet
     * LOCKING: obtain this' monitor before modifying either */
    private final List<ExtendedEndpoint> udpHosts = new ArrayList<ExtendedEndpoint>(PERMANENT_SIZE);
    private final Set<ExtendedEndpoint> udpHostsSet = new HashSet<ExtendedEndpoint>();
    private final UniqueHostPinger pinger;
    
    /**
     * A set of hosts who we've recently contacted, so we don't contact them
     * again.
     */
    private final Set<ExtendedEndpoint> attemptedHosts;
    
    /**
     * Whether or not we need to resort the udpHosts by failures.
     */
    private boolean dirty = false;
    
    /**
     * Whether or not the set contains data different than when we last wrote.
     */
    private boolean writeDirty = false;

    private final Provider<MessageRouter> messageRouter;

    private final PingRequestFactory pingRequestFactory;

    private final ConnectionServices connectionServices;
    
    private final NetworkInstanceUtils networkInstanceUtils;
    
    /**
     * Constructs a new UDPHostCache that remembers attempting hosts for the
     * default expiry time.
     */
    @Inject
    protected UDPHostCache(UniqueHostPinger pinger,
            Provider<MessageRouter> messageRouter,
            PingRequestFactory pingRequestFactory,
            ConnectionServices connectionServices,
            NetworkInstanceUtils networkInstanceUtils) {
        this(EXPIRY_TIME, pinger, messageRouter, pingRequestFactory,
                connectionServices, networkInstanceUtils);
    }

    /**
     * Constructs a new UDPHostCache that remembers attempting hosts for the
     * given amount of time, in msecs.
     * 
     * @param connectionServices
     */
    UDPHostCache(int expiryTime, UniqueHostPinger pinger,
            Provider<MessageRouter> messageRouter,
            PingRequestFactory pingRequestFactory,
            ConnectionServices connectionServices,
            NetworkInstanceUtils networkInstanceUtils) {
        this.connectionServices = connectionServices;
        attemptedHosts = new FixedSizeExpiringSet<ExtendedEndpoint>(PERMANENT_SIZE, expiryTime);
        this.pinger = pinger;
        this.messageRouter = messageRouter;
        this.pingRequestFactory = pingRequestFactory;
        this.networkInstanceUtils = networkInstanceUtils;
    }
    
    /**
     * Writes this' info out to the stream.
     */
    synchronized void write(Writer out) throws IOException {
        for(ExtendedEndpoint e: udpHosts) {
            e.write(out);
        }
        writeDirty = false;
    }
    
    /**
     * Determines if data has been dirtied since the last time we wrote.
     */
    synchronized boolean isWriteDirty() {
        return writeDirty;
    }
    
    /**
     * Returns the number of UDP Host Caches this knows about.
     * Public for testing.
     */
    public synchronized int getSize() {
        return udpHostsSet.size();
    }
    
    /**
     * Resets the cache to its initial empty state
     */
    synchronized void resetData() {
        udpHosts.clear();
        udpHostsSet.clear();
        attemptedHosts.clear();
        dirty = false;
        writeDirty = false;
    }
    
    /**
     * Attempts to contact a host cache to retrieve endpoints.
     *
     * Contacts 10 UDP hosts at a time.
     */
    synchronized boolean fetchHosts() {
        // If the hosts have been used, shuffle and sort them
        if(dirty) {
            // shuffle then sort, ensuring that we're still going to use
            // hosts in order of failure, but within each of those buckets
            // the order will be random.
            LOG.trace("Shuffling and sorting UHCs");
            Collections.shuffle(udpHosts);
            Collections.sort(udpHosts, FAILURE_COMPARATOR);
            dirty = false;
        }
        
        // Keep only the first FETCH_AMOUNT of the valid hosts.
        List<ExtendedEndpoint> validHosts = new ArrayList<ExtendedEndpoint>(Math.min(FETCH_AMOUNT, udpHosts.size()));
        List<ExtendedEndpoint> invalidHosts = new LinkedList<ExtendedEndpoint>();
        for(ExtendedEndpoint next : udpHosts) {
            if(validHosts.size() >= FETCH_AMOUNT)
                break;
            if(attemptedHosts.contains(next)) {
                if(LOG.isTraceEnabled())
                    LOG.trace("Already attempted " + next);
                continue;
            }
                
            // if it was private (couldn't look up too) drop it.
            if(!networkInstanceUtils.isValidExternalIpPort(next) || 
                    !NetworkUtils.isValidIpPort(next) || // this does explicit resolving.
                    networkInstanceUtils.isPrivateAddress(next.getAddress())) {
                if(LOG.isInfoEnabled())
                    LOG.info("Invalid address for " + next);
                invalidHosts.add(next);
                continue;
            }
            
            validHosts.add(next);
        }
        
        // Remove all invalid hosts.
        for(ExtendedEndpoint next : invalidHosts) {
            remove(next);
        }

        attemptedHosts.addAll(validHosts);
        
        return fetch(validHosts);
     }
     
     /**
      * Fetches endpoints from the given collection of hosts.
      */
     synchronized boolean fetch(Collection<? extends ExtendedEndpoint> hosts) {
        if(hosts.isEmpty()) {
            LOG.info("No UHCs to try");
            return false;
        }

        if(LOG.isInfoEnabled())
            LOG.info("Pinging UHCs " + hosts);

        pinger.rank(
            hosts,
            new HostExpirer(hosts),
            // cancel when connected -- don't send out any more pings
            new Cancellable() {
                public boolean isCancelled() {
                    return connectionServices.isConnected();
                }
            },
            getPing()
        );
        return true;
    }
    
    /**
     * Returns a PingRequest to be used while fetching.
     *
     * Useful as a seperate method for tests to catch the Ping's GUID.
     */
    PingRequest getPing() {
        return pingRequestFactory.createUHCPing();
    }

    /**
     * Removes a given hostcache from this.
     */
    synchronized boolean remove(ExtendedEndpoint e) {
        if(LOG.isInfoEnabled())
            LOG.info("Removing UHC " + e);
        boolean removed1=udpHosts.remove(e);
        boolean removed2=udpHostsSet.remove(e);
        assert removed1==removed2 : "Set "+removed1+" but queue "+removed2;
        if(removed1)
            writeDirty = true;
        return removed1;
    }
    
    /**
     * Adds a new udp hostcache to this.
     */
    synchronized boolean add(ExtendedEndpoint e) {
        assert e.isUDPHostCache();
        
        if(udpHostsSet.contains(e)) {
            if(LOG.isTraceEnabled())
                LOG.trace("Not adding known UHC " + e);
            return false;
        }        
        
        // note that we do not do any comparisons to ensure that
        // this host is "better" than existing hosts.
        // the rationale is that we'll only ever be adding hosts
        // who have a failure count of 0 (unless we're reading
        // from gnutella.net, in which case all will be added),
        // and we always want to try new people.
        
        if(LOG.isInfoEnabled())
            LOG.info("Adding UHC " + e);
        
        // if we've exceeded the maximum size, remove the worst element.
        if(udpHosts.size() >= PERMANENT_SIZE) {
            Object removed = udpHosts.remove(udpHosts.size() - 1);
            udpHostsSet.remove(removed);
            if(LOG.isTraceEnabled())
                LOG.trace("Ejected UHC " + removed);
        }
        
        // just insert.  we'll sort later.
        udpHosts.add(e);
        udpHostsSet.add(e);
        dirty = true;
        writeDirty = true;
        return true;
    }
    
    void loadDefaults() {
        // ADD DEFAULT UDP HOST CACHES HERE.
    }
    
    /**
     * Creates and adds a host/port as a UDP host cache.
     */
    @SuppressWarnings("unused")
    private void createAndAdd(String host, int port) {
        try {
            add(new ExtendedEndpoint(host, port).setUDPHostCache(true));
        } catch(IllegalArgumentException ignored) {}
    }
    
    /**
     * Listener that listens for message from the specified hosts,
     * marking any hosts that did not have a message processed
     * as failed host caches, causing them to increment a failure
     * count.  If hosts exceed the maximum failures, they are
     * removed as potential hostcaches.
     */
    private class HostExpirer implements MessageListener {

        private final Set<ExtendedEndpoint> hosts = new StrictIpPortSet<ExtendedEndpoint>();
        
        // allHosts contains all the hosts, so that we can
        // iterate over successful caches too.
        private final Set<ExtendedEndpoint> allHosts;
        private byte[] guid;
        
        /**
         * Constructs a new HostExpirer for the specified hosts.
         */
        public HostExpirer(Collection<? extends ExtendedEndpoint> hostsToAdd) {
            hosts.addAll(hostsToAdd);
            allHosts = new HashSet<ExtendedEndpoint>(hostsToAdd);
            removeDuplicates(hostsToAdd, hosts);
        }
        
        /**
         * Removes any hosts that exist in 'all' but not in 'some'.
         */
        private void removeDuplicates(Collection<? extends ExtendedEndpoint> all, Collection<? extends ExtendedEndpoint> some) {
            // Iterate through what's in our collection vs whats in our set.
            // If any entries exist in the collection but not in the set,
            // then that means they resolved to the same address.
            // Automatically eject entries that resolve to the same address.
            Set<ExtendedEndpoint> duplicates = new HashSet<ExtendedEndpoint>(all);
            duplicates.removeAll(some); // remove any hosts we're keeping.
            for(ExtendedEndpoint ep : duplicates) {
                if(LOG.isTraceEnabled())
                    LOG.trace("Removing duplicate entry " + ep);
                remove(ep);
            }
        }
        
        /**
         * Notification that a message has been processed.
         */
        public void processMessage(Message m, ReplyHandler handler) {
            // allow only udp replies.
            if(handler instanceof UDPReplyHandler) {
                if(hosts.remove(handler)) {
                    if(LOG.isTraceEnabled())
                        LOG.trace("Recieved: " + m);
                }
                // OPTIMIZATION: if we've gotten succesful responses from
                // each hosts, unregister ourselves early.
                if(hosts.isEmpty()) {
                    LOG.trace("Unregistering message listener");
                    messageRouter.get().unregisterMessageListener(guid, this);
                }
            }
        }
        
        /**
         * Notification that this listener is now registered with the 
		 * specified GUID.
         */
        public void registered(byte[] g) {
            this.guid = g;
        }
        
        /**
         * Notification that this listener is now unregistered for the 
		 * specified guid.
         */
        public void unregistered(byte[] g) {
            synchronized(UDPHostCache.this) {
                // Record the failures...
                for(ExtendedEndpoint ep : hosts) {
                    if(LOG.isInfoEnabled())
                        LOG.info("No response from UHC " + ep);
                    ep.recordUDPHostCacheFailure();
                    dirty = true;
                    writeDirty = true;
                    if(ep.getUDPHostCacheFailures() > MAXIMUM_FAILURES)
                        remove(ep);
                }
                // Then record the successes...
                allHosts.removeAll(hosts);
                for(ExtendedEndpoint ep : allHosts) {
                    if(LOG.isInfoEnabled())
                        LOG.info("Valid response from UHC " + ep);
                    ep.recordUDPHostCacheSuccess();
                    dirty = true;
                    writeDirty = true;
                }
            }
        }
    }
    
    /**
     * The only FailureComparator we'll ever need.
     */
    private static final Comparator<ExtendedEndpoint> FAILURE_COMPARATOR = new FailureComparator();
    private static class FailureComparator implements Comparator<ExtendedEndpoint> {
        public int compare(ExtendedEndpoint e1, ExtendedEndpoint e2) {
            return e1.getUDPHostCacheFailures() - e2.getUDPHostCacheFailures();
        }
    }
}
