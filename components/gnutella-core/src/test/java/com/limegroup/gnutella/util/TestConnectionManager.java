package com.limegroup.gnutella.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import org.limewire.core.settings.UltrapeerSettings;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.net.ConnectionDispatcher;
import org.limewire.net.SocketsManager;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.ConnectionManagerImpl;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.HostCatcher;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.NodeAssigner;
import com.limegroup.gnutella.QueryUnicaster;
import com.limegroup.gnutella.connection.ConnectionCheckerManager;
import com.limegroup.gnutella.connection.RoutedConnection;
import com.limegroup.gnutella.connection.RoutedConnectionFactory;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.messages.PingRequestFactory;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactory;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.simpp.SimppManager;

/**
 * Helper class that supplies the list of connections for searching.
 */
@Singleton
public class TestConnectionManager extends ConnectionManagerImpl {
    
    /**
     * The list of ultrapeer <tt>Connection</tt> instances
     */
    private final List<RoutedConnection> CONNECTIONS = new LinkedList<RoutedConnection>();
    
    /**
     * The list of leaf <tt>Connection</tt> instances
     */
    private final List<RoutedConnection> LEAF_CONNECTIONS = new LinkedList<RoutedConnection>();
    
    /**
     * Constant for whether or not this should be considered an
     * Ultrapeer.
     */
    private boolean ULTRAPEER;

    /**
     * Constant for the number of Ultrapeer connections that the 
     * test connection manager should maintain.
     */
    private int NUM_CONNECTIONS;

    /**
     * Constant for the number of leaf connections that the test 
     * connection manager should maintain.
     */
    private int NUM_LEAF_CONNECTIONS;

    /**
     * Constant array for the keywords that I should have (this node).
     */
    private String[] MY_KEYWORDS;

    private int numNewConnections;

    private boolean useVaried;

    /**
     * Constant array for the keywords that I should have (this node)
     * by default, unless the caller specifies otherwise.
     */
    private static final String[] DEFAULT_MY_KEYWORDS = {
        "me",
    };

    /**
     * Constant array for the keywords for Ultrapeers to use.
     */
    private static final String[] ULTRAPEER_KEYWORDS = {
        "qwe", "wer", "ert", "rty", "tyu", 
        "yui", "uio", "iop", "opa ", "pas", 
        "asd", "sdf", "dfg", "fgh", "ghj", 
        "hjk", "jkl", "klz", "lzx", "zxc", 
        "xcv", "cvb", "vbn", "bnm", "qwer",
        "wert", "erty", "rtyu", "tyui", "yuio",        
    };


    /**
     * Constant array for the keywords for leaves to use.
     */
    private static final String[] LEAF_KEYWORDS = {
        "this", "is", "a", "test", "for", 
        "query", "routing", "in", "all", "its", 
        "forms", "including", "both", "leaves", "and", 
        "Ultrapeers", "which", "should", "both", "work", 
        "like", "we", "expect", "them", "to", 
        "at", "least", "in", "theory", "and", 
        "hopefully", "in", "fact", "as", "well", 
        "but", "it's", "hard", "to", "know", 
    };


    /**
     * Array of keywords that should not match anything in the routing
     * tables.
     */
    private static final String[] UNMATCHING_KEYWORDS = {
        "miss", "NCWEPHCE", "IEYWHFDSNC", "UIYRIEH", "dfjaivuih",
    };

    private final QueryRequestFactory queryRequestFactory;

    private final TestConnectionFactory testConnectionFactory;

    @Inject
    public TestConnectionManager(NetworkManager networkManager,
            Provider<HostCatcher> hostCatcher,
            @Named("global") Provider<ConnectionDispatcher> connectionDispatcher,
            @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
            Provider<SimppManager> simppManager,
            CapabilitiesVMFactory capabilitiesVMFactory,
            RoutedConnectionFactory managedConnectionFactory,
            Provider<QueryUnicaster> queryUnicaster,
            SocketsManager socketsManager,
            ConnectionServices connectionServices,
            Provider<NodeAssigner> nodeAssigner, 
             Provider<IPFilter> ipFilter,
            ConnectionCheckerManager connectionCheckerManager,
            PingRequestFactory pingRequestFactory, QueryRequestFactory queryRequestFactory,
            TestConnectionFactory testConnectionFactory,
            NetworkInstanceUtils networkInstanceUtils) {
        super(networkManager, hostCatcher, connectionDispatcher, backgroundExecutor, simppManager,
                capabilitiesVMFactory, managedConnectionFactory, queryUnicaster,
                socketsManager, connectionServices, nodeAssigner, ipFilter,
                connectionCheckerManager, pingRequestFactory, networkInstanceUtils);
        this.queryRequestFactory = queryRequestFactory;
        this.testConnectionFactory = testConnectionFactory;
        configureDefaultManager();
    }

    public void configureDefaultManager() {
        setNumNewConnections(20);
        setUltraPeer(true);
        setNumConnections(20);
        setNumLeafConnections(UltrapeerSettings.MAX_LEAVES.getValue());
        setKeywords(DEFAULT_MY_KEYWORDS);
        setUseVaried(false);
    }
    
    public void configureManagerWithVariedLeafs() {
        setNumNewConnections(20);
        setUltraPeer(true);
        setNumConnections(20);
        setNumLeafConnections(UltrapeerSettings.MAX_LEAVES.getValue());
        setKeywords(DEFAULT_MY_KEYWORDS);
        setUseVaried(true);
    }
    
    public void setNumNewConnections(int numNewConnections) {
        this.numNewConnections = numNewConnections;
    }
    
    public void setUltraPeer(boolean ultraPeer) {
        ULTRAPEER = ultraPeer;
    }
     
    public void setNumConnections(int numConnections) {
        NUM_CONNECTIONS = numConnections; 
    }
    
    public void setNumLeafConnections(int numLeafConnections) {
        NUM_LEAF_CONNECTIONS = numLeafConnections;
    }
    
    public void setKeywords(String[] keywords) {
        MY_KEYWORDS = keywords;
    }
    
    public void setUseVaried(boolean useVaried) {
        this.useVaried = useVaried;
    }
    
    public void resetAndInitialize() {
        CONNECTIONS.clear();
        LEAF_CONNECTIONS.clear();
        
        for(int i=0; i<NUM_CONNECTIONS; i++) {
            RoutedConnection curConn = null;
            if(i < numNewConnections) {
                curConn = testConnectionFactory.createUltrapeerConnection(new String[]{ULTRAPEER_KEYWORDS[i]});
            } else {
                curConn = testConnectionFactory.createOldConnection(15); 
            }
            CONNECTIONS.add(curConn);            
        }

        // now, give ourselves the desired number of leaves
        for(int i=0; i<NUM_LEAF_CONNECTIONS; i++) {
            RoutedConnection conn;
            if(useVaried && i >= (NUM_LEAF_CONNECTIONS/2)) {
                conn = testConnectionFactory.createAltLeafConnection();
            } else {
                conn = testConnectionFactory.createWithKeywords(new String[]{LEAF_KEYWORDS[i]});
            }
            LEAF_CONNECTIONS.add(conn);
        }
    }

    /**
     * Test to make sure that the given <tt>QueryRouteTable</tt> has matches
     * for all of the expected keywords and that it doesn't have matches
     * for any of the unexpected keywords.
     *
     * @param qrt the <tt>QueryRouteTable</tt> instance to test
     */
    public boolean runQRPMatch(QueryRouteTable qrt) {
        for(int i=0; i<MY_KEYWORDS.length; i++) {
            QueryRequest qr = queryRequestFactory.createQuery(MY_KEYWORDS[i]);
            if(!qrt.contains(qr)) return false;
        }

        for(int i=0; i<NUM_LEAF_CONNECTIONS; i++) {
            QueryRequest qr = queryRequestFactory.createQuery(LEAF_KEYWORDS[i]);
            if(!qrt.contains(qr)) return false;
        }

        for(int i=0; i<UNMATCHING_KEYWORDS.length; i++) {
            QueryRequest qr = queryRequestFactory.createQuery(UNMATCHING_KEYWORDS[i]);
            if(qrt.contains(qr)) return false;
        }
        return true;
    }

    /**
     * Accessor for the custom list of connections.
     */
    @Override
    public List<RoutedConnection> getInitializedConnections() {
        return CONNECTIONS;
    }
    
    public void setInitializedConnections(List<RoutedConnection> connections) {
        CONNECTIONS.clear();
        CONNECTIONS.addAll(connections);
    }
    
    @Override
    public List<RoutedConnection> getInitializedClientConnections() {
        return LEAF_CONNECTIONS;
    }

    @Override
    public boolean isSupernode() {
        return ULTRAPEER;
    }

    /**
     * Returns the total number of queries received over all Ultrapeer connections.
     */
    public int getNumUltrapeerQueries() {
        return getNumQueries(CONNECTIONS);
    }

    /**
     * Returns the total number of queries received over all leaf connections.
     */
    public int getNumLeafQueries() {
        return getNumQueries(LEAF_CONNECTIONS);
    }

    /**
     * Returns the total number of queries received over all leaf connections.
     */
    private static int getNumQueries(Collection<RoutedConnection> connections) {
        int numQueries = 0;
        Iterator<RoutedConnection> iter = connections.iterator();
        while(iter.hasNext()) {
            TestConnection tc = (TestConnection)iter.next();
            numQueries += tc.getNumQueries();
        }
        return numQueries;
    }

    /**
     * Returns the total number of queries received over all old connections.
     */
    public int getNumOldConnectionQueries() {
        int numQueries = 0;
        Iterator<RoutedConnection> iter = CONNECTIONS.iterator();
        while(iter.hasNext()) {
            TestConnection tc = (TestConnection)iter.next();
            if(tc instanceof OldConnection) {
                numQueries += tc.getNumQueries();
            }
        }
        return numQueries;
    }

    /**
     * Returns the total number of queries received over all old connections.
     */
    public int getNumNewConnectionQueries() {
        int numQueries = 0;
        Iterator<RoutedConnection> iter = CONNECTIONS.iterator();
        while(iter.hasNext()) {
            TestConnection tc = (TestConnection)iter.next();
            if(tc instanceof NewConnection) {
                numQueries += tc.getNumQueries();
            }
        }
        return numQueries;
    }    

    public boolean isClientSupernodeConnection() {
        return false;
    }
}