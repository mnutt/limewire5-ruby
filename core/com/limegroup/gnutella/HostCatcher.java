package com.limegroup.gnutella;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.limewire.collection.BucketQueue;
import org.limewire.collection.Cancellable;
import org.limewire.collection.FixedSizeSortedList;
import org.limewire.collection.IntSet;
import org.limewire.collection.ListPartitioner;
import org.limewire.collection.RandomAccessMap;
import org.limewire.collection.RandomOrderHashMap;
import org.limewire.core.settings.ApplicationSettings;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.FilterSettings;
import org.limewire.inspection.Inspectable;
import org.limewire.inspection.InspectableContainer;
import org.limewire.inspection.InspectionPoint;
import org.limewire.io.Connectable;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortSet;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.io.NetworkUtils;
import org.limewire.lifecycle.Service;
import org.limewire.util.CommonUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.limegroup.gnutella.bootstrap.TcpBootstrap;
import com.limegroup.gnutella.bootstrap.UDPHostCache;
import com.limegroup.gnutella.bootstrap.UDPHostCacheFactory;
import com.limegroup.gnutella.bootstrap.TcpBootstrap.TcpBootstrapListener;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.DHTManager.DHTMode;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.PingRequestFactory;
import com.limegroup.gnutella.util.ClassCNetworks;

/**
 * The host catcher.  This peeks at pong messages coming on the
 * network and snatches IP addresses of other Gnutella peers.  IP
 * addresses may also be added to it from a file (usually
 * "gnutella.net").  The servent may then connect to these addresses
 * as necessary to maintain full connectivity.<p>
 *
 * The HostCatcher currently prioritizes pongs as follows.  Note that Ultrapeers
 * with a private address is still highest priority; hopefully this may allow
 * you to find local Ultrapeers.
 * <ol>
 * <li> Ultrapeers.  Ultrapeers are identified because the number of files they
 *      are sharing is an exact power of two--a dirty but effective hack.
 * <li> Normal pongs.
 * <li> Private addresses.  This means that the host catcher will still 
 *      work on private networks, although we will normally ignore private
 *      addresses.        
 * </ol> 
 *
 * Finally, HostCatcher maintains a list of "permanent" locations, based on
 * average daily uptime.  These are stored in the gnutella.net file.  They
 * are NOT bootstrap servers like router.limewire.com; LimeWire doesn't
 * use those anymore.
 */
@Singleton
public class HostCatcher implements Service {
    
    /**
     * Log for logging this class.
     */
    private static final Log LOG = LogFactory.getLog(HostCatcher.class);
    
    /**
     * The number of ultrapeer pongs to store.
     */
    static final int GOOD_SIZE=1000;
    
    /**
     * The number of normal pongs to store.
     * This must be large enough to store all permanent addresses, 
     * as permanent addresses when read from disk are stored as
     * normal priority.
     */    
    static final int NORMAL_SIZE=400;

    /**
     * The number of permanent locations to store in gnutella.net 
     * This MUST NOT BE GREATER THAN NORMAL_SIZE.  This is because when we read
     * in endpoints, we add them as NORMAL_PRIORITY.  If we have written
     * out more than NORMAL_SIZE hosts, then we guarantee that endpoints
     * will be ejected from the ENDPOINT_QUEUE upon startup.
     * Because we write out best first (and worst last), and thus read in
     * best first (and worst last) this means that we will be ejecting
     * our best endpoints and using our worst ones when starting.
     * 
     */
    static final int PERMANENT_SIZE = NORMAL_SIZE;

    /**
     * Constant for the index of good priority hosts (Ultrapeers)
     */
    public static final int GOOD_PRIORITY = 1;

    /**
     * Constant for the index of non-Ultrapeer hosts.
     */
    public static final int NORMAL_PRIORITY = 0;
    
    /**
     * netmask for pongs that we accept and send.
     */
    public static final int PONG_MASK = 0xFFFFFF00;
    
    private static final Comparator<ExtendedEndpoint> DHT_COMPARATOR = 
        new Comparator<ExtendedEndpoint>() {
        public int compare(ExtendedEndpoint e1, ExtendedEndpoint e2) {
            DHTMode mode1 = e1.getDHTMode();
            DHTMode mode2 = e2.getDHTMode();
            if ((mode1.equals(DHTMode.ACTIVE) && !mode2.equals(DHTMode.ACTIVE)) ||
                    (mode1.equals(DHTMode.PASSIVE) && mode2.equals(DHTMode.INACTIVE))) {
                return -1;
            } else if ((mode2.equals(DHTMode.ACTIVE) && !mode1.equals(DHTMode.ACTIVE)) ||
                    (mode2.equals(DHTMode.PASSIVE) && mode1.equals(DHTMode.INACTIVE))) {
                return 1;
            } else {
                return 0;
            }
        }
    };

    // NOTE: ENDPOINT_SET, FREE_ULTRAPEER_SLOTS_SET & FREE_LEAF_SLOTS_SET
    //       are actually Maps that point to themselves so that we can
    //       retrieve Endpoints from them based on an ip/port.
        

    /** The list of hosts to try.  These are sorted by priority: ultrapeers,
     * normal, then private addresses.  Within each priority level, recent hosts
     * are prioritized over older ones.  Our representation consists of a set
     * and a queue, both bounded in size.  The set lets us quickly check if
     * there are duplicates, while the queue provides ordering--a classic
     * space/time tradeoff.
     *
     * INVARIANT: queue contains no duplicates and contains exactly the
     *  same elements as set.
     * LOCKING: obtain this' monitor before modifying either.  */
    private final BucketQueue<ExtendedEndpoint> ENDPOINT_QUEUE = 
        new BucketQueue<ExtendedEndpoint>(new int[] {NORMAL_SIZE,GOOD_SIZE});
    private final Map<ExtendedEndpoint, ExtendedEndpoint> ENDPOINT_SET =
        new HashMap<ExtendedEndpoint, ExtendedEndpoint>();
    
    /**
     * <tt>Set</tt> of hosts advertising free Ultrapeer connection slots.
     */
    private final RandomAccessMap<ExtendedEndpoint, ExtendedEndpoint> FREE_ULTRAPEER_SLOTS_SET = 
        new RandomOrderHashMap<ExtendedEndpoint, ExtendedEndpoint>(200);
    
    /**
     * <tt>Set</tt> of hosts advertising free leaf connection slots.
     */
    private final RandomAccessMap<ExtendedEndpoint, ExtendedEndpoint> FREE_LEAF_SLOTS_SET = 
        new RandomOrderHashMap<ExtendedEndpoint, ExtendedEndpoint>(200);
    
    /**
     * map of locale (string) to sets (of endpoints).
     */
    private final Map<String, Set<ExtendedEndpoint>> LOCALE_SET_MAP =
        new HashMap<String, Set<ExtendedEndpoint>>();
    
    /**
     * number of endpoints to keep in the locale set
     */
    private static final int LOCALE_SET_SIZE = 100;
    
    /** The list of pongs with the highest average daily uptimes.  Each host's
     * weight is set to the uptime.  These are most likely to be reachable
     * during the next session, though not necessarily likely to have slots
     * available now.  In this way, they act more like bootstrap hosts than
     * normal pongs.  This list is written to gnutella.net and used to
     * initialize queue on startup.  To prevent duplicates, we also maintain a
     * set of all addresses, like with queue/set.
     *
     * INVARIANT: permanentHosts contains no duplicates and contains exactly
     *  the same elements and permanentHostsSet
     * LOCKING: obtain this' monitor before modifying either */
    private final FixedSizeSortedList<ExtendedEndpoint> permanentHosts =
        new FixedSizeSortedList<ExtendedEndpoint>(
                ExtendedEndpoint.priorityComparator(),
                PERMANENT_SIZE);
    private final Set<ExtendedEndpoint> permanentHostsSet =
        new HashSet<ExtendedEndpoint>();
    
    /**
     * List of the hosts that were restored from disk.
     * INVARIANT: a subset of permanentHosts.  
     * LOCKING: this
     */
    private final List<ExtendedEndpoint> restoredHosts =
        new FixedSizeSortedList<ExtendedEndpoint>(
                ExtendedEndpoint.priorityComparator(),
                PERMANENT_SIZE);
    
    /** 
     * Partition view of the list of restored hosts.
     */
    private final ListPartitioner<ExtendedEndpoint> uptimePartitions = 
        new ListPartitioner<ExtendedEndpoint>(restoredHosts, 3);
            
    /** The UDPHostCache bootstrap system. */
    private UDPHostCache udpHostCache;
    
    /**
     * Count for the number of hosts that we have not been able to connect to.
     * This is used for degenerate cases where we ultimately have to hit the 
     * bootstrap hosts.
     */
    private int _failures;
    
    /**
     * <tt>Set</tt> of hosts we were unable to create TCP connections with
     * and should therefore not be tried again.  Fixed size.
     * 
     * LOCKING: obtain this' monitor before modifying/iterating
     */
    private final Set<Endpoint> EXPIRED_HOSTS = new HashSet<Endpoint>();
    
    /**
     * <tt>Set</tt> of hosts we were able to create TCP connections with but 
     * did not accept our Gnutella connection, and are therefore put on 
     * "probation".  Fixed size.
     * 
     * LOCKING: obtain this' monitor before modifying/iterating
     */    
    private final Set<Endpoint> PROBATION_HOSTS = new HashSet<Endpoint>();
    
    /**
     * Constant for the number of milliseconds to wait before periodically
     * recovering hosts on probation.  Non-final for testing.
     */
    private static long PROBATION_RECOVERY_WAIT_TIME = 60*1000;

    /**
     * Constant for the number of milliseconds to wait between calls to 
     * recover hosts that have been placed on probation.  
     * Non-final for testing.
     */
    private static long PROBATION_RECOVERY_TIME = 60*1000;
    
    /**
     * Constant for the size of the set of hosts put on probation.  Public for
     * testing.
     */
    public static final int PROBATION_HOSTS_SIZE = 500;

    /**
     * Constant for the size of the set of expired hosts.  Public for
     * testing.  
     */
    public static final int EXPIRED_HOSTS_SIZE = 500;
    
    /**
     * The scheduled runnable that fetches hosts from bootstrappers if we need them.
     */
    public final Bootstrapper FETCHER = new Bootstrapper();
    
    /**
     * All EndpointObservers waiting on getting an Endpoint.
     */
    private List<EndpointObserver> _catchersWaiting = new LinkedList<EndpointObserver>();
    
    /**
     * The last allowed time that we can continue ranking pongs.
     */
    private long lastAllowedPongRankTime = 0;
    
    /**
     * The amount of time we're allowed to do pong ranking after
     * we click connect.
     */
    private final long PONG_RANKING_EXPIRE_TIME = 20 * 1000;
    
    /**
     * Stop ranking if we have this many connections.
     */
    private static final int MAX_CONNECTIONS = 5;
    
    /** A RND to share to find random hosts. */
    private final Random RND = new Random();
    
    /**
     * Whether or not hosts have been added since we wrote to disk.
     */
    private boolean dirty = false;
    
    private final ScheduledExecutorService backgroundExecutor;
    private final ConnectionServices connectionServices;
    private final Provider<ConnectionManager> connectionManager;
    private final Provider<UDPService> udpService;
    private final Provider<DHTManager> dhtManager;
    private final Provider<QueryUnicaster> queryUnicaster;
    private final Provider<IPFilter> ipFilter;
    private final Provider<MulticastService> multicastService;
    private final UniqueHostPinger uniqueHostPinger;
    private final NetworkInstanceUtils networkInstanceUtils;

    private final PingRequestFactory pingRequestFactory;
    private final TcpBootstrap tcpBootstrap;
    
    @Inject
	public HostCatcher(
	        @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
            ConnectionServices connectionServices,
            Provider<ConnectionManager> connectionManager,
            Provider<UDPService> udpService, Provider<DHTManager> dhtManager,
            Provider<QueryUnicaster> queryUnicaster,
            Provider<IPFilter> ipFilter,
            Provider<MulticastService> multicastService,
            UniqueHostPinger uniqueHostPinger,
            UDPHostCacheFactory udpHostCacheFactory,
            PingRequestFactory pingRequestFactory,
            NetworkInstanceUtils networkInstanceUtils,
            TcpBootstrap tcpBootstrap) {
        this.backgroundExecutor = backgroundExecutor;
        this.connectionServices = connectionServices;
        this.connectionManager = connectionManager;
        this.udpService = udpService;
        this.dhtManager = dhtManager;
        this.queryUnicaster = queryUnicaster;
        this.ipFilter = ipFilter;
        this.multicastService = multicastService;
        this.uniqueHostPinger = uniqueHostPinger;
        this.pingRequestFactory = pingRequestFactory;
        this.networkInstanceUtils = networkInstanceUtils;
        this.tcpBootstrap = tcpBootstrap;
        
        // TODO: this could also be solved with a named injection to get the
        //       UniqHostPinger and not its super class
        this.udpHostCache = udpHostCacheFactory.createUDPHostCache(uniqueHostPinger);
    }
    
    UDPHostCache getUdpHostCache() {
        return udpHostCache;
    }

    /**
     * Initializes any components required for HostCatcher.
     * Currently, this schedules occasional services.
     */
    public void start() {
        scheduleServices();
    }
    
    public String getServiceName() {
        return org.limewire.i18n.I18nMarker.marktr("Peer Locator");
    }
    
    public void initialize() {
    }
    
    public void stop() {
        try {
            write();
        } catch(IOException ignored) {}
    }
    
    @Inject
    void register(org.limewire.lifecycle.ServiceRegistry registry) {
        registry.register(this);
    }
    
    protected void scheduleServices() {        
        Runnable probationRestorer = new Runnable() {
            public void run() {
                LOG.trace("restoring hosts on probation");
                List<Endpoint> toAdd;
                synchronized(HostCatcher.this) {
                    if(LOG.isTraceEnabled()) {
                        LOG.trace("Restoring " + PROBATION_HOSTS.size() +
                                " probated hosts");
                    }
                    toAdd = new ArrayList<Endpoint>(PROBATION_HOSTS);
                    PROBATION_HOSTS.clear();
                }
                
                for(Endpoint e : toAdd)
                    add(e, false);
            } 
        };
        // Recover hosts on probation every minute.
        backgroundExecutor.scheduleWithFixedDelay(probationRestorer, 
                PROBATION_RECOVERY_WAIT_TIME, PROBATION_RECOVERY_TIME,
                TimeUnit.MILLISECONDS);
            
        // Try to fetch hosts whenever we need them.
        // Start it immediately, so that if we have no hosts
        // (because of a fresh installation) we will connect.
        backgroundExecutor.scheduleWithFixedDelay(FETCHER, 0, 2*1000,
                TimeUnit.MILLISECONDS);
        LOG.trace("STOP scheduling");
    }

    /**
     * Sends UDP pings to hosts read from disk.
     */
    public void sendUDPPings() {
        // We need the lock on this so that we can copy the set of endpoints.
        List<Endpoint> l; 
        synchronized(this) {
            l = new ArrayList<Endpoint>(ENDPOINT_SET.size() + restoredHosts.size());
            l.addAll(ENDPOINT_SET.keySet());
            l.addAll(restoredHosts);
        }
        Collections.shuffle(l);
        rank(l); 
    }
    
    /**
     * Rank the collection of hosts.
     */
    private void rank(Collection<? extends IpPort> hosts) {
        if(needsPongRanking()) {
            if(LOG.isTraceEnabled())
                LOG.trace("Sending " + hosts.size() + " hosts to pinger");
            uniqueHostPinger.rank(
                hosts,
                // cancel when connected -- don't send out any more pings
                new Cancellable() {
                    public boolean isCancelled() {
                        return !needsPongRanking();
                    }
                }
            );
        }
    }
    
    public void sendMessageToAllHosts(Message m, MessageListener listener, Cancellable c) {
        uniqueHostPinger.rank(getAllHosts(), listener, c, m);
    }
    
    private synchronized Collection<ExtendedEndpoint> getAllHosts() {
        //keep them ordered -- TODO: Why?
        Collection<ExtendedEndpoint> hosts = new LinkedHashSet<ExtendedEndpoint>(getNumHosts());
        hosts.addAll(FREE_ULTRAPEER_SLOTS_SET.keySet());
        hosts.addAll(FREE_LEAF_SLOTS_SET.keySet());
        hosts.addAll(ENDPOINT_SET.keySet());
        hosts.addAll(restoredHosts);
        return hosts;
    }
    
    /**
     * Gets a List of hosts that support the DHT. If <tt>this</tt> knows any active nodes,
     * return them at the head of the list.
     * Note: this method is slow and is not meant to be used often.
     * 
     * @param minVersion The minimum DHT Version. Should be 0 to return all versions
     * 
     * @return A Collection of ExtendedEndpoints that support the DHT.
     */
    public synchronized List<ExtendedEndpoint> getDHTSupportEndpoint(int minVersion) {
        List<ExtendedEndpoint> hosts = new ArrayList<ExtendedEndpoint>();
        IntSet masked = new IntSet();
        boolean filter = ConnectionSettings.FILTER_CLASS_C.getValue();
        for(ExtendedEndpoint host : getAllHosts()) {
            if(host.supportsDHT() && host.getDHTVersion() >= minVersion) {
                int ip = NetworkUtils.getMaskedIP(host.getInetAddress(), PONG_MASK);
                if(!filter || masked.add(ip))
                    hosts.add(host);
            }
        }
        Collections.sort(hosts, DHT_COMPARATOR);
        return hosts;
    }
    
    /**
     * Determines if UDP Pongs need to be sent out.
     */
    private synchronized boolean needsPongRanking() {
        if(connectionServices.isFullyConnected()) {
            if(LOG.isTraceEnabled())
                LOG.trace("Pong ranking not needed - fully connected");
            return false;
        }
        int have = connectionManager.get().getInitializedConnections().size();
        if(have >= MAX_CONNECTIONS) {
            if(LOG.isTraceEnabled())
                LOG.trace("Pong ranking not needed - have max connections");
            return false;
        }
        long now = System.currentTimeMillis();
        if(now > lastAllowedPongRankTime) {
            if(LOG.isTraceEnabled())
                LOG.trace("Pong ranking not allowed - last time has passed");
            return false;
        }
        int size;
        if(connectionServices.isSupernode()) {
            synchronized(this) {
                size = FREE_ULTRAPEER_SLOTS_SET.size();
            }
        } else {
            synchronized(this) {
                size = FREE_LEAF_SLOTS_SET.size();
            }
        }
        int preferred = connectionManager.get().getPreferredConnectionCount();        
        boolean needsPongRanking =  size < preferred - have;
        if(!needsPongRanking)
            LOG.trace("Pong ranking not needed - have enough hosts to try");
        return needsPongRanking;
    }
    
    /**
     * Reads in endpoints from the given file.  This is called by initialize, so
     * you don't need to call it manually.  It is package access for
     * testability.
     *
     * @modifies this
     * @effects read hosts from the given file.  
     */
    void read(File hostFile) throws FileNotFoundException, IOException {
        LOG.trace("Reading host file");
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(hostFile));
            while(true) {
                String line = in.readLine();
                if(line == null)
                    break;
                try {
                    ExtendedEndpoint e = ExtendedEndpoint.read(line); 
                    if(e.isUDPHostCache()) {
                        addUDPHostCache(e);
                    } else if(isValidHost(e)) {
                        synchronized(this) {
                            addPermanent(e);
                            restoredHosts.add(e);
                        }
                        endpointAdded();
                    } else {
                        if(LOG.isTraceEnabled())
                            LOG.trace("File contains invalid host: " + line);
                    }
                } catch (ParseException pe) {
                    LOG.debug("Exception parsing host file", pe);
                    continue;
                }
            }
        } finally {
            try {
                if(in != null)
                    in.close();
            } catch(IOException e) {
                LOG.debug("Exception closing host file", e);
            }
        }
    }

	/**
	 * Writes the host file to the default location.
	 *
	 * @throws <tt>IOException</tt> if the file cannot be written
	 */
	synchronized void write() throws IOException {
		write(getHostsFile());
	}

    /**
     * @modifies the file named filename
     * @effects writes this to the given file.  The file
     *  is prioritized by rough probability of being good.
     */
    synchronized void write(File hostFile) throws IOException {
        checkInvariants();
        LOG.trace("Writing host file");
        if(dirty || udpHostCache.isWriteDirty()) {
            FileWriter out = new FileWriter(hostFile);
                
            //Write udp hostcache endpoints.
            udpHostCache.write(out);
    
            //Write elements of permanent from worst to best.  Order matters, as it
            //allows read() to put them into queue in the right order without any
            //difficulty.
            for(ExtendedEndpoint e : permanentHosts)
                e.write(out);
            
            out.close();
        }
    }

    ///////////////////////////// Add Methods ////////////////////////////


    /**
     * Attempts to add a pong to this, possibly ejecting other elements from the
     * cache.  This method used to be called "spy".
     *
     * @param pr the pong containing the address/port to add
     * @param receivingConnection the connection on which we received
     *  the pong.
     * @return true iff pr was actually added 
     */
    public boolean add(PingReply pr) {
        // Discard UDP pongs with unknown GUIDs, unless they're from local
        // sources, in which case they might be replies to multicast pings 
        byte[] source = pr.getInetAddress().getAddress();
        boolean isLocalOrPrivate = networkInstanceUtils.isVeryCloseIP(source)
                || networkInstanceUtils.isPrivateAddress(source);
        if(pr.isUDP() && !isLocalOrPrivate) {
            GUID g = new GUID(pr.getGUID());
            if(!g.equals(PingRequest.UDP_GUID)
                    && !g.equals(udpService.get().getSolicitedGUID())) {
                LOG.debug("Discarding UDP pong with unknown GUID");
                return false;
            }
        } 
        
        //Convert to endpoint
        ExtendedEndpoint endpoint;
        
        if(pr.getDailyUptime() != -1) {
            endpoint = new ExtendedEndpoint(pr.getAddress(), pr.getPort(), 
											pr.getDailyUptime());
        } else {
            endpoint = new ExtendedEndpoint(pr.getAddress(), pr.getPort());
        }
        
        //if the PingReply had locale information then set it in the endpoint
        if(!pr.getClientLocale().equals(""))
            endpoint.setClientLocale(pr.getClientLocale());
            
        if(pr.isUDPHostCache()) {
            endpoint.setHostname(pr.getUDPCacheAddress());            
            endpoint.setUDPHostCache(true);
        }
        
        if(!isValidHost(endpoint) && !isLocalOrPrivate) {
            return false;
        }
        
        int dhtVersion = pr.getDHTVersion();
        if(dhtVersion > -1) {
            DHTMode mode = pr.getDHTMode();
            endpoint.setDHTVersion(dhtVersion);
            endpoint.setDHTMode(mode);
            
            if(dhtManager.get().isRunning()) {
                // Send active and passive DHT endpoints to the DHT manager
                if(mode.equals(DHTMode.ACTIVE)) {
                    SocketAddress address = new InetSocketAddress(
                            endpoint.getAddress(), endpoint.getPort());
                    dhtManager.get().addActiveDHTNode(address);
                } else if(mode.equals(DHTMode.PASSIVE)) {
                    SocketAddress address = new InetSocketAddress(
                            endpoint.getAddress(), endpoint.getPort());
                    dhtManager.get().addPassiveDHTNode(address);
                }
            }
        }
        
        if(pr.supportsUnicast()) {
            queryUnicaster.get().addUnicastEndpoint(pr.getInetAddress(), pr.getPort());
        }
        
        // if the pong carried packed IP/Ports, add those as their own
        // endpoints.
        Collection<IpPort> packed = pr.getPackedIPPorts();
        if(ConnectionSettings.FILTER_CLASS_C.getValue())
            packed = NetworkUtils.filterOnePerClassC(packed);
        rank(packed);
        
        for(IpPort ipp : packed) {            
            ExtendedEndpoint ep;
            if(ipp instanceof ExtendedEndpoint) {
                ep = (ExtendedEndpoint)ipp;
            } else {
                ep = new ExtendedEndpoint(ipp.getAddress(), ipp.getPort());
                if(ipp instanceof Connectable) {
                    // When more items other than TLS are added to HostInfo,
                    // it would make more sense to make this something like:
                    // ep.addHostInfo(ipp);
                    ep.setTLSCapable(((Connectable)ipp).isTLSCapable());
                }
            }
            
            if(isValidHost(ep))
                add(ep, GOOD_PRIORITY);
        }
        
        // if the pong carried packed UDP host caches, add those as their
        // own endpoints.
        for(IpPort ipp : pr.getPackedUDPHostCaches()) {
            ExtendedEndpoint ep = new ExtendedEndpoint(ipp.getAddress(), ipp.getPort());
            ep.setUDPHostCache(true);
            addUDPHostCache(ep);
        }
        
        // if it was a UDPHostCache pong, just add it as that.
        if(endpoint.isUDPHostCache())
            return addUDPHostCache(endpoint);
        
        if(pr.isTLSCapable())
            endpoint.setTLSCapable(true);

        //Add the endpoint, forcing it to be high priority if marked pong from 
        //an ultrapeer.
            
        if(pr.isUltrapeer()) {
            // Add it to our free leaf slots list if it has free leaf slots
            if(pr.hasFreeLeafSlots()) {
                LOG.trace("Adding host to free leaf slot set");
                addToFreeSlotSet(endpoint, FREE_LEAF_SLOTS_SET);
                // Return now if the pong is not also advertising free 
                // ultrapeer slots.
                if(!pr.hasFreeUltrapeerSlots()) {
                    LOG.trace("Host has no free UP slots");
                    return true;
                }
            } 
            
            // Add it to our free UP slots list if it has UP leaf slots, or if
            // the locales match and it has free locale preferencing slots
            String myLocale = ApplicationSettings.LANGUAGE.getValue();
            if(pr.hasFreeUltrapeerSlots() || 
                    (myLocale.equals(pr.getClientLocale()) &&
                            pr.getNumFreeLocaleSlots() > 0)) {
                LOG.trace("Adding host to free UP slot set");
                addToFreeSlotSet(endpoint, FREE_ULTRAPEER_SLOTS_SET);
                return true;
            } 
            LOG.trace("Adding host with good priority");
            return add(endpoint, GOOD_PRIORITY); 
        } else {
            LOG.trace("Adding host with normal priority");
            return add(endpoint, NORMAL_PRIORITY);
        }
    }
    
    /**
     * Adds an endpoint to the udp host cache, returning true
     * if it succesfully added.
     */
    private boolean addUDPHostCache(ExtendedEndpoint host) {
        return udpHostCache.add(host);
    }
    
    /**
     * Utility method for adding the specified host to the specified 
     * <tt>Set</tt> of hosts with free slots. 
     * 
     * @param host the host to add
     * @param hosts the <tt>Set</tt> to add it to
     */
    private void addToFreeSlotSet(ExtendedEndpoint host, Map<? super ExtendedEndpoint, ? super ExtendedEndpoint> hosts) {
        synchronized(this) {
            hosts.put(host, host);
            
            // Also add it to the list of permanent hosts stored on disk.
            addPermanent(host);
        }
        
        endpointAdded();
    }

    /**
     * add the endpoint to the map which matches locales to a set of 
     * endpoints
     */
    private synchronized void addToLocaleMap(ExtendedEndpoint endpoint) {
        String loc = endpoint.getClientLocale();
        if(LOCALE_SET_MAP.containsKey(loc)) { //if set exists for ths locale
            Set<ExtendedEndpoint> s = LOCALE_SET_MAP.get(loc);
            if(s.add(endpoint) && s.size() > LOCALE_SET_SIZE)
                s.remove(s.iterator().next());
        }
        else { //otherwise create new set and add it to the map
            Set<ExtendedEndpoint> s = new HashSet<ExtendedEndpoint>();
            s.add(endpoint);
            LOCALE_SET_MAP.put(loc, s);
        }
    }
    
    /**
     * Adds a collection of addresses to this.
     */
    public int add(Collection<? extends Endpoint> endpoints) {
        rank(endpoints);
        int added = 0;
        for(Endpoint e: endpoints) {
            if(add(e, true)) {
                added++;
            }
        }
        return added;
            
    }


    /**
     * Adds an address to this, possibly ejecting other elements from the cache.
     * This method is used when getting an address from headers instead of the
     * normal ping reply.
     *
     * @param pr the pong containing the address/port to add.
     * @param forceHighPriority true if this should always be of high priority
     * @return true iff e was actually added
     */
    public boolean add(Endpoint e, boolean forceHighPriority) {
        if(!isValidHost(e))
            return false;
        if (forceHighPriority)
            return add(e, GOOD_PRIORITY);
        else
            return add(e, NORMAL_PRIORITY);
    }

    

    /**
     * Adds an endpoint.  Use this method if the locale of endpoint is known
     * (used by ConnectionManager.disconnect())
     */
    public boolean add(Endpoint e, boolean forceHighPriority, String locale) {
        if(!isValidHost(e))
            return false;        
        //need ExtendedEndpoint for the locale
        if(forceHighPriority)
            return add(new ExtendedEndpoint(e.getAddress(), 
                    e.getPort(), locale), GOOD_PRIORITY);
        else
            return add(new ExtendedEndpoint(e.getAddress(),
                    e.getPort(), locale), NORMAL_PRIORITY);
    }

    /**
     * Adds the specified host to the host catcher with the specified priority.
     * 
     * @param host the endpoint to add
     * @param priority the priority of the endpoint
     * @return <tt>true</tt> if the endpoint was added, otherwise <tt>false</tt>
     */
    public boolean add(Endpoint host, int priority) {
        if(host instanceof ExtendedEndpoint)
            return add((ExtendedEndpoint)host, priority);
        //need ExtendedEndpoint for the locale
        return add(new ExtendedEndpoint(host.getAddress(), 
                host.getPort()), priority);
    }

    /**
     * Adds the passed endpoint to the set of hosts maintained, temporary and
     * permanent. The endpoint may not get added due to various reasons
     * (including it might be our address itself, we might be connected to it
     * etc.). Also adding this endpoint may lead to the removal of some other
     * endpoint from the cache.
     *
     * @param e Endpoint to be added
     * @param priority the priority to use for e, one of GOOD_PRIORITY 
     *  (ultrapeer) or NORMAL_PRIORITY
     * @param uptime the host's uptime (or our best guess)
     *
     * @return true iff e was actually added 
     */
    private boolean add(ExtendedEndpoint e, int priority) {
        checkInvariants();
        
        if(e.isUDPHostCache()) {
            LOG.trace("Adding host as UHC");
            return addUDPHostCache(e);
        }
        
        boolean added = false;
        synchronized(this) {
            //Add to permanent list, regardless of whether it's actually in queue.
            //Note that this modifies e.
            addPermanent(e);            
            if(!(ENDPOINT_SET.containsKey(e))) {
                added = true;
                //Add to temporary list. Adding e may eject an older point from
                //queue, so we have to cleanup the set to maintain
                //rep. invariant.
                ENDPOINT_SET.put(e, e);
                ExtendedEndpoint ejected = ENDPOINT_QUEUE.insert(e, priority);
                if(ejected != null) {
                    ENDPOINT_SET.remove(ejected);
                    if(LOG.isTraceEnabled())
                        LOG.trace("Ejected host " + ejected);
                }
            }
        }
        endpointAdded();
        checkInvariants();
        return added;
    }

    /**
     * Adds an address to the permanent list of this without marking it for
     * immediate fetching.  This method is when connecting to a host and reading
     * its Uptime header.  If e is already in the permanent list, it is not
     * re-added, though its key may be adjusted.
     *
     * @param e the endpoint to add
     * @return true iff e was actually added 
     */
    private synchronized boolean addPermanent(ExtendedEndpoint e) {
        if(networkInstanceUtils.isPrivateAddress(e.getInetAddress())) {
            LOG.trace("Not permanently adding host with private address");
            return false;
        }
        if(permanentHostsSet.contains(e)) {
            //TODO: we could adjust the key
            LOG.trace("Not permanently adding duplicate host");
            return false;
        }

        addToLocaleMap(e); //add e to locale mapping 
        
        ExtendedEndpoint removed = permanentHosts.insert(e);
        if(removed != e) {
            //Was actually added...
            if(LOG.isTraceEnabled())
                LOG.trace("Permanently adding host " + e);
            permanentHostsSet.add(e);
            if(removed != null)
                //...and something else was removed.
                permanentHostsSet.remove(removed);
            dirty = true;
            return true;
        } else {
            //Uptime not good enough to add.  (Note that this is 
            //really just an optimization of the above case.)
            LOG.trace("Not permanently adding host with low uptime");
            return false;
        }
    }
    
    /** Removes e from permanentHostsSet and permanentHosts. 
     *  @return true iff this was modified */
    private synchronized boolean removePermanent(ExtendedEndpoint e) {
        boolean removed1 = permanentHosts.remove(e);
        boolean removed2 = permanentHostsSet.remove(e);
        assert removed1 == removed2 : "Queue "+removed1+" but set "+removed2;
        if(removed1) {
            dirty = true;
            if(LOG.isTraceEnabled())
                LOG.trace("Removed permanent host " + e);
        }
        return removed1;
    }

    /**
     * Utility method for verifying that the given host is a valid host to add
     * to the group of hosts to try.  This verifies that the host does not have
     * a private address, is not banned, is not this node, is not in the
     * expired or probated hosts set, etc.
     * 
     * @param host the host to check
     * @return <tt>true</tt> if the host is valid and can be added, otherwise
     *  <tt>false</tt>
     */
    private boolean isValidHost(Endpoint host) {
        if(LOG.isTraceEnabled())
            LOG.trace("Validating host " + host);
        // caches will validate for themselves.
        if(host.isUDPHostCache()) {
            LOG.trace("Host is valid: UHC");
            return true;
        }
        
        byte[] addr;
        try {
            addr = host.getHostBytes();
        } catch(UnknownHostException uhe) {
            LOG.trace("Host is invalid: unknown host");
            return false;
        }
        
        if(networkInstanceUtils.isPrivateAddress(addr)) {
            LOG.trace("Host is invalid: private address");
            return false;
        }

        //We used to check that we're not connected to e, but now we do that in
        //ConnectionFetcher after a call to getAnEndpoint.  This is not a big
        //deal, since the call to "set.contains(e)" below ensures no duplicates.
        //Skip if this would connect us to our listening port.  TODO: I think
        //this check is too strict sometimes, which makes testing difficult.
        if(networkInstanceUtils.isMe(addr, host.getPort())) {
            LOG.trace("Host is invalid: own address");
            return false;
        }

        //Skip if this host is banned.
        if(!ipFilter.get().allow(addr)) {
            if(FilterSettings.USE_NETWORK_FILTER.getValue())
                LOG.trace("Using network filter");
            LOG.trace("Host is invalid: blacklisted");
            return false;
        }
        
        synchronized(this) {
            // Don't add this host if it has previously failed.
            if(EXPIRED_HOSTS.contains(host)) {
                LOG.trace("Host is invalid: expired");
                return false;
            }
            
            // Don't add this host if it has previously rejected us.
            if(PROBATION_HOSTS.contains(host)) {
                LOG.trace("Host is invalid: on probation");
                return false;
            }
        }
        
        LOG.trace("Host is valid");
        return true;
    }
    
    /** Returns true if the given IpPort is TLS-capable. */
    public boolean isHostTLSCapable(IpPort ipp) {
        if(ipp instanceof Connectable) {
            boolean capable = ((Connectable)ipp).isTLSCapable();
            if(LOG.isTraceEnabled())
                LOG.trace(ipp + (capable ? " is" : " is not") + " TLS capable");
            return capable;
        }
        
        // No need to check if it's an endpoint already, because all Endpoints
        // already implement HostInfo.
        Endpoint p = new Endpoint(ipp.getAddress(), ipp.getPort());
        
        ExtendedEndpoint ee;
        synchronized(this) {
            ee = ENDPOINT_SET.get(p);
            if(ee == null)
                ee = FREE_ULTRAPEER_SLOTS_SET.get(p);
            if(ee == null)
                ee = FREE_LEAF_SLOTS_SET.get(p);
        }
        
        if(ee == null) {
            if(LOG.isTraceEnabled())
                LOG.trace(ipp + " is not known to be TLS capable");
            return false;
        } else {
            boolean capable = ee.isTLSCapable();
            if(LOG.isTraceEnabled())
                LOG.trace(ipp + (capable ? " is" : " is not") + " TLS capable");
            return capable;
        }
    }
    
    ///////////////////////////////////////////////////////////////////////
    
    /**
     * Notification that endpoints now exist.
     * If something was waiting on getting endpoints, this will notify them
     * about the new endpoint.
     */
    private void endpointAdded() {
        // No loop is actually necessary here because this method is called
        // each time an endpoint is added.  Each new endpoint will trigger its
        // own check.
        Endpoint p;
        EndpointObserver observer;
        synchronized (this) {
            if(_catchersWaiting.isEmpty()) {
                LOG.trace("No observers waiting");
                return; // no one waiting.
            }
            
            p = getAnEndpointInternal();
            if(p == null) {
                LOG.trace("No hosts available");
                return; // no more endpoints to give.
            }
            
            observer = _catchersWaiting.remove(0);
        }
        
        // It is important that this is outside the lock.  Otherwise HostCatcher's lock
        // is exposed to the outside world.
        LOG.trace("Returning a host to a waiting observer");
        observer.handleEndpoint(p);
    }

    /** Passes the next available endpoint to the EndpointObserver. */
    public void getAnEndpoint(EndpointObserver observer) {
        Endpoint p;
        
        // We can only lock around endpoint retrieval & _catchersWaiting,
        // we don't want to expose our lock to the observer.
        synchronized(this) {
            p = getAnEndpointInternal();
            if(p == null) {
                LOG.trace("Couldn't get a host immediately; waiting");
                _catchersWaiting.add(observer);
            }
        }
        
        if(p != null)
            observer.handleEndpoint(p);
    }
    
    /**
     * Passes the next available endpoint to the EndpointObserver.
     * If an Endpoint is immediately available, it is returned immediately
     * instead of using the observer's callback.  That is, the callback will
     * only be used if an endpoint is not immediately available.
     * This is useful to prevent stack overflows when many endpoints are
     * attempted in response to endpoints not being usable.
     * 
     * If the observer is null and no endpoint is available, this will
     * simply return null and schedule no future callback.
     */
    public Endpoint getAnEndpointImmediate(EndpointObserver observer) {
        Endpoint p;
        
        synchronized(this) {
            p = getAnEndpointInternal();
            if(p == null && observer != null) {
                LOG.trace("Couldn't get a host immediately; waiting");
                _catchersWaiting.add(observer);
            }
        }
        
        return p;
    }
    
    /** Removes an oberserver from wanting to get an endpoint. */
    public synchronized void removeEndpointObserver(EndpointObserver observer) {
        LOG.trace("Removing waiting observer");
        _catchersWaiting.remove(observer);
    }

    /**
     * @modifies this
     * @effects atomically removes and returns the highest priority host in
     *          this. If no host is available, blocks until one is. If the
     *          calling thread is interrupted during this process, throws
     *          InterruptedException. The caller should call doneWithConnect and
     *          doneWithMessageLoop when done with the returned value.
     */
    public Endpoint getAnEndpoint() throws InterruptedException {
        BlockingObserver observer = new BlockingObserver();

        getAnEndpoint(observer);
        try {
            synchronized (observer) {
                if(observer.getEndpoint() == null) {
                    // only stops waiting when handleEndpoint is called.
                    observer.wait();
                }
                return observer.getEndpoint();
            }
        } catch (InterruptedException ie) {
            // If we got interrupted, we must remove the waiting observer.
            synchronized (this) {
                LOG.trace("Removing waiting observer");
                _catchersWaiting.remove(observer);
                throw ie;
            }
        }
    }
  
    /**
     * Notifies this that the fetcher has finished attempting a connection to
     * the given host. This exists primarily to update the permanent host list
     * with connection history.
     * 
     * @param e
     *            the address/port, which should have been returned by
     *            getAnEndpoint
     * @param success
     *            true if we successfully established a messaging connection to
     *            e, at least temporarily; false otherwise
     */
    public synchronized void doneWithConnect(Endpoint e, boolean success) {
        //Normal host: update key.  TODO: adjustKey() operation may be more
        //efficient.
        if(!(e instanceof ExtendedEndpoint)) {
            //Should never happen, but I don't want to update public
            //interface of this to operate on ExtendedEndpoint.
            LOG.warn("Endpoint in doneWithConnect() is not ExtendedEndpoint");
            return;
        }
        
        ExtendedEndpoint ee = (ExtendedEndpoint)e;

        removePermanent(ee);
        if(success) {
            ee.recordConnectionSuccess();
        } else {
            _failures++;
            ee.recordConnectionFailure();
        }
        addPermanent(ee);
    }

    /**
     * @requires this' monitor held
     * @modifies this
     * @effects returns the highest priority endpoint in queue, regardless
     *  of quick-connect settings, etc.  Returns null if this is empty.
     */
    protected ExtendedEndpoint getAnEndpointInternal() {
        // If we're already an ultrapeer and we know about hosts with free
        // ultrapeer slots, try them.
        if(connectionServices.isSupernode() &&
                !FREE_ULTRAPEER_SLOTS_SET.isEmpty()) {
            LOG.trace("UP: returning host with free UP slots");
            return preferenceWithLocale(FREE_ULTRAPEER_SLOTS_SET);
                                    
        } 
        // Otherwise, if we're already a leaf and we know about ultrapeers with
        // free leaf slots, try those.
        else if(connectionServices.isShieldedLeaf() && 
                !FREE_LEAF_SLOTS_SET.isEmpty()) {
            LOG.trace("Leaf: returning host with free leaf slots");
            return preferenceWithLocale(FREE_LEAF_SLOTS_SET);
        } 
        // Otherwise, assume we'll be a leaf and we're trying to connect, since
        // this is more common than wanting to become an ultrapeer and because
        // we want to fill any remaining leaf slots if we can.
        else if(!FREE_ULTRAPEER_SLOTS_SET.isEmpty()) {
            LOG.trace("Returning host with free UP slots");
            return preferenceWithLocale(FREE_ULTRAPEER_SLOTS_SET);
        } 
        // Otherwise, might as well use the leaf slots hosts up as well
        // since we added them to the size and they can give us other info
        else if(!FREE_LEAF_SLOTS_SET.isEmpty()) {
            LOG.trace("Returning host with free leaf slots");
            Iterator<ExtendedEndpoint> iter = FREE_LEAF_SLOTS_SET.keySet().iterator();
            ExtendedEndpoint ee = iter.next();
            FREE_LEAF_SLOTS_SET.remove(ee);
            return ee;
        } 
        else if(!ENDPOINT_QUEUE.isEmpty()) {
            LOG.trace("Returning ordinary host");
            //pop e from queue and remove from set.
            ExtendedEndpoint e= ENDPOINT_QUEUE.extractMax();
            ExtendedEndpoint removed=ENDPOINT_SET.remove(e);
            //check that e actually was in set.
            assert removed == e : "Rep. invariant for HostCatcher broken.";
            return e;
        } 
        else if(!restoredHosts.isEmpty()) {
            LOG.trace("Returning restored host with long uptime");
            // highest partition has highest uptimes
            List<ExtendedEndpoint> best = uptimePartitions.getLastPartition();
            ExtendedEndpoint e = best.remove((int)(Math.random() * best.size()));
            return e;
        }
        else {
            LOG.trace("No hosts to return");
            return null;
        }
    }

    
    /**
     * tries to return an endpoint that matches the locale of this client
     * from the passed in set.
     */
    private ExtendedEndpoint preferenceWithLocale(
            RandomAccessMap<ExtendedEndpoint, ExtendedEndpoint> base) {

        String loc = ApplicationSettings.LANGUAGE.getValue();
        ExtendedEndpoint ret = null;
        // preference a locale host if we haven't matched any locales yet
        if(!connectionManager.get().isLocaleMatched()) {
            if(LOCALE_SET_MAP.containsKey(loc)) {
                Set<ExtendedEndpoint> locales = LOCALE_SET_MAP.get(loc);
                for(ExtendedEndpoint e : base.keySet()) {
                    if(locales.contains(e)) {
                        LOG.trace("Found a host with matching locale");
                        locales.remove(e);
                        ret = e;
                        break;
                    }
                }
            }
        }
        
        // TODO: why do we choose deterministically if we're matching
        // locales and randomly if we're not?
        if(ret == null) {
            LOG.trace("Did not find a host with matching locale");
            ret = base.getKeyAt(RND.nextInt(base.size()));
        }
        
        Object removed = base.remove(ret);
        assert ret == removed : "Key: " + ret + ", value: " + removed;
        return ret;
    }

    /**
     * Accessor for the total number of hosts stored, including Ultrapeers and
     * leaves.
     * 
     * @return the total number of hosts stored 
     */
    public synchronized int getNumHosts() {
        return ENDPOINT_QUEUE.size()+FREE_LEAF_SLOTS_SET.size()+
            FREE_ULTRAPEER_SLOTS_SET.size()+restoredHosts.size();
    }

    /**
     * Returns the number of marked ultrapeer hosts.
     */
    public synchronized int getNumUltrapeerHosts() {
        return ENDPOINT_QUEUE.size(GOOD_PRIORITY)+FREE_LEAF_SLOTS_SET.size()+
            FREE_ULTRAPEER_SLOTS_SET.size();
    }

    /**
     * Returns an iterator of this' "permanent" hosts, from worst to best.
     * This method exists primarily for testing.  THIS MUST NOT BE MODIFIED
     * WHILE ITERATOR IS IN USE.
     */
    Iterator<ExtendedEndpoint> getPermanentHosts() {
        return permanentHosts.iterator();
    }

    
    /**
     * Accessor for the <tt>Collection</tt> of 10 Ultrapeers that have 
     * advertised free Ultrapeer slots.  The returned <tt>Collection</tt> is a 
     * new <tt>Collection</tt> and can therefore be modified in any way.
     * 
     * @return a <tt>Collection</tt> containing 10 <tt>IpPort</tt> hosts that 
     *  have advertised they have free ultrapeer slots
     */
    public synchronized Collection<IpPort>
    getUltrapeersWithFreeUltrapeerSlots(int num) {
        return getPreferencedCollection(FREE_ULTRAPEER_SLOTS_SET,
                ApplicationSettings.LANGUAGE.getValue(), num);
    }

    public synchronized Collection<IpPort>
    getUltrapeersWithFreeUltrapeerSlots(String locale, int num) {
        return getPreferencedCollection(FREE_ULTRAPEER_SLOTS_SET, locale, num);
    }
    

    /**
     * Accessor for the <tt>Collection</tt> of 10 Ultrapeers that have 
     * advertised free leaf slots.  The returned <tt>Collection</tt> is a 
     * new <tt>Collection</tt> and can therefore be modified in any way.
     * 
     * @return a <tt>Collection</tt> containing 10 <tt>IpPort</tt> hosts that 
     *  have advertised they have free leaf slots
     */
    public synchronized Collection<IpPort>
    getUltrapeersWithFreeLeafSlots(int num) {
        return getPreferencedCollection(FREE_LEAF_SLOTS_SET,
                ApplicationSettings.LANGUAGE.getValue(), num);
    }
    
    public synchronized Collection<IpPort>
    getUltrapeersWithFreeLeafSlots(String locale, int num) {
        return getPreferencedCollection(FREE_LEAF_SLOTS_SET, locale, num);
    }

    /**
     * preference the set so we try to return those endpoints that match
     * passed in locale "loc"
     */
    private Collection<IpPort> getPreferencedCollection(
            Map<? extends ExtendedEndpoint, ? extends ExtendedEndpoint> base,
            String loc, int num) {
        if(loc == null || loc.equals(""))
            loc = ApplicationSettings.DEFAULT_LOCALE.getValue();

        Set<IpPort> hosts = new HashSet<IpPort>(num);
        IntSet masked = new IntSet();
        
        Set<ExtendedEndpoint> locales = LOCALE_SET_MAP.get(loc);
        boolean filter = ConnectionSettings.FILTER_CLASS_C.getValue();
        if(locales != null) {
            for(ExtendedEndpoint e : locales) {
                if(hosts.size() >= num)
                    break;
                if(base.containsKey(e)) { 
                    int ip = NetworkUtils.getMaskedIP(e.getInetAddress(), PONG_MASK);
                    if(!filter || masked.add(ip))
                        hosts.add(e);
                }
            }
            if(LOG.isTraceEnabled())
                LOG.trace("Found " + hosts.size() + " locale-matched hosts");
        }
        
        for(IpPort ipp : base.keySet()) {
            if(hosts.size() >= num)
                break;
            int ip = NetworkUtils.getMaskedIP(ipp.getInetAddress(), PONG_MASK);
            if(!filter || masked.add(ip))
                hosts.add(ipp);
        }
        if(LOG.isTraceEnabled())
            LOG.trace("Found " + hosts.size() + " hosts");
        
        return hosts;
    }


    /**
     * Notifies this that connect() has been called.  This may decide to give
     * out bootstrap pongs if necessary.
     */
    public void expire() {
        synchronized(this) {
            long now = System.currentTimeMillis();
            lastAllowedPongRankTime = now + PONG_RANKING_EXPIRE_TIME;
        }
        
        recoverHosts();
        
        // schedule new runnable to clear the set of endpoints that
        // were pinged while trying to connect
        backgroundExecutor.schedule(
                new Runnable() {
                    public void run() {
                        uniqueHostPinger.resetData();
                    }
                },
                PONG_RANKING_EXPIRE_TIME, TimeUnit.MILLISECONDS);
    }

    /**
     * @modifies this
     * @effects removes all entries from this
     */
    public synchronized void clear() {
        LOG.trace("Clearing hosts");
        FREE_LEAF_SLOTS_SET.clear();
        FREE_ULTRAPEER_SLOTS_SET.clear();
        LOCALE_SET_MAP.clear();
        ENDPOINT_QUEUE.clear();
        ENDPOINT_SET.clear();
        restoredHosts.clear();
        PROBATION_HOSTS.clear();
        EXPIRED_HOSTS.clear();
        permanentHosts.clear();
        permanentHostsSet.clear();
    }
    
    public UDPPinger getPinger() {
        return uniqueHostPinger;
    }

    /** Enable very slow rep checking?  Package access for use by
     *  HostCatcherTest. */
    static boolean DEBUG = false;

    
    /** Checks invariants. Very slow; method body should be enabled for testing
     *  purposes only. */
    protected void checkInvariants() {
        if(DEBUG) {
            synchronized(this) {
                // Check ENDPOINT_SET == ENDPOINT_QUEUE
                for(ExtendedEndpoint ee : ENDPOINT_QUEUE) {
                    assert ENDPOINT_SET.containsKey(ee);
                }
                assert ENDPOINT_QUEUE.size() == ENDPOINT_SET.size();
                // Check permanentHostsSet === permanentHosts
                for(ExtendedEndpoint ee : permanentHosts) {
                    assert permanentHostsSet.contains(ee);
                }
                assert permanentHosts.size() == permanentHostsSet.size();
            }
        }
    }
    
    /**
     * Reads the gnutella.net file.
     */
    private void readHostsFile() {
        LOG.trace("Reading Hosts File");
        // Just gnutella.net
        try {
            read(getHostsFile());
        } catch (IOException e) {
            LOG.debug(getHostsFile(), e);
        }
    }

    private File getHostsFile() {
        return new File(CommonUtils.getUserSettingsDir(), "gnutella.net");
    }
    
    /**
     * Recovers any hosts that we have put in the set of hosts "pending" 
     * removal from our hosts list.
     */
    public void recoverHosts() {
        LOG.debug("Recovering hosts file");
        
        synchronized(this) {
            PROBATION_HOSTS.clear();
            EXPIRED_HOSTS.clear();
            _failures = 0;
            FETCHER.resetFetchTime();
            udpHostCache.resetData();
            udpHostCache.loadDefaults();
            tcpBootstrap.resetData();
            tcpBootstrap.loadDefaults();
            restoredHosts.clear();
            uniqueHostPinger.resetData();
        }
        
        // Read the hosts file again.  This will also notify any waiting 
        // connection fetchers from previous connection attempts.
        readHostsFile();
    }

    /**
     * Adds the specified host to the group of hosts currently on "probation."
     * These are hosts that are on the network but that have rejected a 
     * connection attempt.  They will periodically be re-activated as needed.
     * 
     * @param host the <tt>Endpoint</tt> to put on probation
     */
    public synchronized void putHostOnProbation(Endpoint host) {
        LOG.trace("Putting a host on probation");
        PROBATION_HOSTS.add(host);
        if(PROBATION_HOSTS.size() > PROBATION_HOSTS_SIZE) {
            PROBATION_HOSTS.remove(PROBATION_HOSTS.iterator().next());
        }
    }
    
    /**
     * Adds the specified host to the group of expired hosts.  These are hosts
     * that we have been unable to create a TCP connection to, let alone a 
     * Gnutella connection.
     * 
     * @param host the <tt>Endpoint</tt> to expire
     */
    public synchronized void expireHost(Endpoint host) {
        LOG.trace("Expiring a host");
        EXPIRED_HOSTS.add(host);
        if(EXPIRED_HOSTS.size() > EXPIRED_HOSTS_SIZE) {
            EXPIRED_HOSTS.remove(EXPIRED_HOSTS.iterator().next());
        }
    }
    
    /**
     * Runnable that looks for UDPHostCache or multicast hosts.
     * This tries, in order:
     * 1) Multicasting a ping.
     * 2) Sending UDP pings to UDPHostCaches.
     * 3) Sending TCP pings to TcpHostCaches.
     */
    private class Bootstrapper implements Runnable {
        
        /**
         * The next allowed multicast time.
         * Incremented after each attempted multicast fetch.
         */
        private long nextAllowedMulticastTime = 0; // Immediately
        
        /**
         * The next allowed UDP fetch time.
         * Incremented after each attempted UDP fetch.
         */
        private long nextAllowedUdpTime = 0; // Immediately
        
        /**
         * The next allowed TCP fetch time.
         * Incremented after each attempted TCP fetch.
         */
        private long nextAllowedTcpTime = Long.MAX_VALUE; // Not just yet
        
        /** Milliseconds to wait between multicast fetches. */
        private static final int MULTICAST_INTERVAL = 50 * 1000;

        /** Milliseconds to wait between UDP fetches. */
        private static final int UDP_INTERVAL = 30 * 1000;
        
        /** Milliseconds to wait after trying UDP before falling back to TCP. */
        private static final int TCP_FALLBACK_DELAY = 40 * 1000;
        
        /** Milliseconds to wait between TCP fetches. */
        private static final int TCP_INTERVAL = 60 * 1000;        
        
        /**
         * Determines whether or not it is time to get more hosts,
         * and if we need them, gets them.
         */
        public synchronized void run() {            
            if(ConnectionSettings.DO_NOT_BOOTSTRAP.getValue()) {
                LOG.trace("Not bootstrapping");
                return;
            }

            // If no one's waiting for an endpoint, don't get any.
            if(_catchersWaiting.isEmpty()) {
                LOG.trace("No observers waiting");
                return;
            }

            // If we need endpoints, try any bootstrapping methods that
            // haven't been tried too recently
            if(needsHosts()) {
                long now = System.currentTimeMillis();
                multicastFetch(now);
                udpHostCacheFetch(now);
                tcpHostCacheFetch(now);
            }
        }
        
        /**
         * Resets the nextAllowedFetchTime, so that after we regain a
         * connection to the internet, we can fetch if needed.
         */
        void resetFetchTime() {
            nextAllowedUdpTime = 0;
            nextAllowedMulticastTime = 0;
            nextAllowedTcpTime = Long.MAX_VALUE;
        }
        
        /**
         * Determines whether or not we need more hosts.
         */
        private synchronized boolean needsHosts() {
            synchronized(HostCatcher.this) { 
                if(getNumHosts() == 0) {
                    LOG.trace("Need hosts: none known");
                    return true;
                }
                if(!connectionServices.isConnected() && _failures > 100) {
                    LOG.trace("Need hosts: not connected after 100 failures");
                    return true;
                }
            }
            LOG.trace("Do not need hosts");
            return false;
        }
        
        /**
         * Attempts to fetch via multicast, returning true
         * if it was able to.
         */
        private boolean multicastFetch(long now) {
            if(nextAllowedMulticastTime < now && 
                    !ConnectionSettings.DO_NOT_MULTICAST_BOOTSTRAP.getValue()) {
                LOG.trace("Fetching via multicast");
                PingRequest pr = pingRequestFactory.createMulticastPing();
                multicastService.get().send(pr);
                nextAllowedMulticastTime = now + MULTICAST_INTERVAL;
                return true;
            }
            LOG.trace("Not fetching via multicast");
            return false;
        }
        
        /**
         * Attempts to fetch via udp host caches, returning true
         * if it was able to.
         */
        private boolean udpHostCacheFetch(long now) {
            if(nextAllowedUdpTime < now && udpHostCache.fetchHosts()) {
                LOG.trace("Fetching via UDP");
                nextAllowedUdpTime = now + UDP_INTERVAL;
                // If this is the first UDP fetch, set the TCP fallback time
                if(nextAllowedTcpTime == Long.MAX_VALUE)
                    nextAllowedTcpTime = now + TCP_FALLBACK_DELAY;
                return true;
            }
            LOG.trace("Not fetching via UDP");
            return false;
        }
        
        private boolean tcpHostCacheFetch(long now) {
            if(nextAllowedTcpTime < now
                    && tcpBootstrap.fetchHosts(new BootstrapListener())) {
                LOG.trace("Fetching via TCP");
                nextAllowedTcpTime = now + TCP_INTERVAL;
                return true;
            }
            LOG.trace("Not fetching via TCP");
            return false;
        }
    }
    
    private class BootstrapListener implements TcpBootstrapListener {
        @Override
        public int handleHosts(Collection<? extends Endpoint> hosts) {
            return add(hosts);
        }
    }
    
    /** Simple callback for having an endpoint added. */
    public static interface EndpointObserver {
        public void handleEndpoint(Endpoint p);
    }
    
    /** A blocking implementation of EndpointObserver. */
    private static class BlockingObserver implements EndpointObserver {
        private Endpoint endpoint;
        
        public synchronized void handleEndpoint(Endpoint p) {
            endpoint = p;
            notify();
        }
        
        public Endpoint getEndpoint() {
            return endpoint;
        }
    }

    //Unit test: tests/com/.../gnutella/HostCatcherTest.java   
    @SuppressWarnings("unused")
    @InspectableContainer
    private class HCInspectables {
        
        @InspectionPoint("known hosts by class C")
        public final Inspectable top10classC = new Inspectable() {
            public Object inspect() {
                Map<String, Object> ret = new HashMap<String, Object>();
                ret.put("ver",1);
                ClassCNetworks permanent = new ClassCNetworks();
                ClassCNetworks restored = new ClassCNetworks();
                ClassCNetworks freeLeaf = new ClassCNetworks();
                ClassCNetworks freeUp = new ClassCNetworks();
                ClassCNetworks all = new ClassCNetworks();
                synchronized(HostCatcher.this) {
                    IpPortSet everybody = new IpPortSet();
                    everybody.addAll(permanentHostsSet);
                    everybody.addAll(restoredHosts);
                    everybody.addAll(FREE_LEAF_SLOTS_SET.keySet());
                    everybody.addAll(FREE_ULTRAPEER_SLOTS_SET.keySet());
                    everybody.addAll(ENDPOINT_SET.keySet());
                    for(IpPort ip : permanentHostsSet) 
                        permanent.add(ip.getInetAddress(), 1);
                    for(IpPort ip : restoredHosts) 
                        restored.add(ip.getInetAddress(), 1);
                    for(IpPort ip : FREE_LEAF_SLOTS_SET.keySet()) 
                        freeLeaf.add(ip.getInetAddress(), 1);
                    for(IpPort ip : FREE_ULTRAPEER_SLOTS_SET.keySet()) 
                        freeUp.add(ip.getInetAddress(), 1);
                    for(IpPort ip : everybody) 
                        all.add(ip.getInetAddress(), 1);
                }
                
                ret.put("perm", permanent.getTopInspectable(10));
                ret.put("rest", restored.getTopInspectable(10));
                ret.put("fl", freeLeaf.getTopInspectable(10));
                ret.put("fu", freeUp.getTopInspectable(10));
                ret.put("all", all.getTopInspectable(10));
                return ret;
            }
        };
        
        /** Inspectable with some tls stats */
        @InspectionPoint("tls stats of known hosts")
        public final Inspectable tlsStats = new Inspectable() {
            public Object inspect() {
                Map<String, Object> ret = new HashMap<String, Object>();
                ret.put("ver",1);
                int perm, permtls, rest,resttls,fl, fltls, fu,futls;
                synchronized(HostCatcher.this) {
                    perm = permanentHostsSet.size();
                    rest = restoredHosts.size();
                    fl = FREE_LEAF_SLOTS_SET.size();
                    fu = FREE_ULTRAPEER_SLOTS_SET.size();
                    permtls = 0;
                    for (ExtendedEndpoint e : permanentHostsSet)
                        permtls += e.isTLSCapable() ? 1 : 0;
                    resttls = 0;
                    for (ExtendedEndpoint e : restoredHosts)
                        resttls += e.isTLSCapable() ? 1 : 0;
                    fltls = 0;
                    for (ExtendedEndpoint e : FREE_LEAF_SLOTS_SET.keySet())
                        fltls += e.isTLSCapable() ? 1 : 0;
                    futls = 0;
                    for (ExtendedEndpoint e : FREE_ULTRAPEER_SLOTS_SET.keySet())
                        futls += e.isTLSCapable() ? 1 : 0;
                }
                ret.put("perm",perm);
                ret.put("permtls",permtls);
                ret.put("rest",rest);
                ret.put("resttls",resttls);
                ret.put("fl",fl);
                ret.put("fltls",fltls);
                ret.put("fu",fu);
                ret.put("futls",futls);
                return ret;
            }
        };
        
        /** Inspectable with some dht stats */
        @InspectionPoint("dht stats of known hosts")
        public final Inspectable dhtStats = new Inspectable() {
            public Object inspect() {
                Map<String, Object> ret = new HashMap<String, Object>();
                ret.put("ver",1);
                int perm, permdht, rest,restdht,fl, fldht, fu,fudht;
                synchronized(HostCatcher.this) {
                    perm = permanentHostsSet.size();
                    rest = restoredHosts.size();
                    fl = FREE_LEAF_SLOTS_SET.size();
                    fu = FREE_ULTRAPEER_SLOTS_SET.size();
                    permdht = 0;
                    
                    for (ExtendedEndpoint e : permanentHostsSet) {
                        permdht += e.supportsDHT() ? 1 : 0;
                    }
                    restdht = 0;
                    for (ExtendedEndpoint e : restoredHosts)
                        restdht += e.supportsDHT() ? 1 : 0;
                    fldht = 0;
                    for (ExtendedEndpoint e : FREE_LEAF_SLOTS_SET.keySet())
                        fldht += e.supportsDHT() ? 1 : 0;
                    fudht = 0;
                    for (ExtendedEndpoint e : FREE_ULTRAPEER_SLOTS_SET.keySet())
                        fudht += e.supportsDHT() ? 1 : 0;
                }
                ret.put("perm",perm);
                ret.put("permdht",permdht);
                ret.put("rest",rest);
                ret.put("restdht",restdht);
                ret.put("fl",fl);
                ret.put("fldht",fldht);
                ret.put("fu",fu);
                ret.put("fudht",fudht);
                return ret;
            }
        };
    }
}
