package com.limegroup.gnutella;

import java.net.InetAddress;

import junit.framework.Test;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.FilterSettings;
import org.limewire.core.settings.SearchSettings;
import org.limewire.core.settings.UltrapeerSettings;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.util.MediaType;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.limegroup.gnutella.connection.RoutedConnection;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.xml.LimeXMLDocument;


/**
 * Test to make sure that query routing tables are correctly exchanged between
 * Ultrapeers.
 *
 * ULTRAPEER_1  ----  ULTRAPEER_2
 */
public final class UltrapeerQueryRouteTableTest extends ServerSideTestCase {


	/**
     * A filename that won't match.
     */
    private static final String noMatch = "junkie junk";
    private static final String match = "matchy match";

    private ConnectionServices connectionServices;
    private ConnectionManager connectionManager;
    private SearchServices searchServices;

    public UltrapeerQueryRouteTableTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(UltrapeerQueryRouteTableTest.class);
    }    
   

    @Override
    protected int getNumberOfUltrapeers() {
        return 1;
    }
    
    @Override
    protected int getNumberOfLeafpeers() {
        return 0;
    }
    
    @Override
    protected void setSettings() throws Exception {

        ConnectionSettings.NUM_CONNECTIONS.setValue(4);
        SearchSettings.GUESS_ENABLED.setValue(true);
        UltrapeerSettings.DISABLE_ULTRAPEER_MODE.setValue(false);
        UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(true);
        UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(true);
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        FilterSettings.BLACK_LISTED_IP_ADDRESSES.set(
            new String[] {"*.*.*.*"});
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.set(
                    new String[] {"127.*.*.*",InetAddress.getLocalHost().getHostAddress()});
        
		ConnectionSettings.WATCHDOG_ACTIVE.setValue(false);
        ConnectionSettings.ALLOW_WHILE_DISCONNECTED.setValue(true);
//        ConnectionSettings.PORT.setValue(6332);
        
        UltrapeerSettings.NEED_MIN_CONNECT_TIME.setValue(false);
    }
    
    @Override
    protected void setUpQRPTables() throws Exception {
        QueryRouteTable qrt = new QueryRouteTable();
        qrt.add(match);
        for (Message m : qrt.encode(new QueryRouteTable()))
            ULTRAPEER[0].send(m);
        ULTRAPEER[0].flush();
    }
    
    @Override
    public void setUp() throws Exception {

        final ResponseVerifier testVerifier = new TestResponseVerifier();
        Injector injector = LimeTestUtils.createInjector(Stage.PRODUCTION, new AbstractModule() {
            @Override
            protected void configure() {
                bind(ResponseVerifier.class).toInstance(testVerifier);
            }
        });
        
        super.setUp(injector);
        connectionManager = injector.getInstance(ConnectionManager.class);
        connectionServices = injector.getInstance(ConnectionServices.class);
        searchServices = injector.getInstance(SearchServices.class);

        int tries = 0;
        do {
            Thread.sleep(1000);
            if (connectionServices.isConnected() && 
                    connectionManager.getInitializedConnections().isEmpty() &&
                    connectionManager.getInitializedConnections().get(0).getRoutedConnectionStatistics().getQueryRouteTablePercentFull() > 0)
                break;
        } while (tries++ < 10);
        assertTrue(connectionServices.isConnected());
        assertFalse(connectionManager.getInitializedConnections().isEmpty());
        assertGreaterThan(0, connectionManager.getInitializedConnections().get(0).getRoutedConnectionStatistics().getQueryRouteTablePercentFull());
	}

    /**
     * Test to make sure we will never send with a TTL of 1 to a 
     * Ultrapeer that doesn't have a hit.
     */
    public void testSentQueryIsNotTTL1() throws Exception {
        assertTrue("should be connected", connectionServices.isConnected());
        searchServices.query(searchServices.newQueryGUID(), noMatch);
        Thread.sleep(2000);
        // we will send the query, but with a TTL of 2, not 1, because
        // the ultrapeer doesn't have this query in its qrp table.
        QueryRequest qSent = BlockingConnectionUtils.getFirstInstanceOfMessageType(ULTRAPEER[0], QueryRequest.class);
        assertEquals("wrong ttl", 2, qSent.getTTL());
        assertEquals("wrong hops", 0, qSent.getHops());
	}
    
    /**
     * Test to make sure that dynamic querying sends a query with TTL=1 and 
     * other properties when a neighboring Ultrapeer has a hit in its QRP
     * table for that query.
     */
    public void testDynamicQueryingWithQRPHit() throws Exception {
        assertTrue("should be connected", connectionServices.isConnected());
        
        searchServices.query(searchServices.newQueryGUID(), match);
        Thread.sleep(4000);
        
        QueryRequest qSent = BlockingConnectionUtils.getFirstInstanceOfMessageType(ULTRAPEER[0], QueryRequest.class);
        
        // The TTL on the sent query should be 1 because the other Ultrapeer
        // should have a "hit" in its QRP table.  When there's a hit, we 
        // send with TTL 1 simply because it's likely that it's popular.
        if (qSent.getTTL() != 1) {
            // see if qrp got exchanged properly
            int num = connectionManager.getInitializedConnections().size();
            double totalQrp = 0;
            for (RoutedConnection rc : connectionManager.getInitializedClientConnections())
                totalQrp += rc.getRoutedConnectionStatistics().getQueryRouteTablePercentFull();
            fail("ttl was not 1 but "+qSent.getTTL()+" there were "+num+" connections with qrp total "+totalQrp);
        }
        assertEquals("wrong hops", 0, qSent.getHops());
    }


  
    
    private class TestResponseVerifier implements ResponseVerifier {
        
        public boolean isMandragoreWorm(byte[] guid, Response response) {
            return false;
        }

        public boolean matchesQuery(byte[] guid, Response response) {
            return true;
        }

        public boolean matchesType(byte[] guid, Response response) {
            return true;
        }

        public void record(QueryRequest qr, MediaType type) {
        }

        public void record(QueryRequest qr) {
        }

        public int score(String query, LimeXMLDocument richQuery, RemoteFileDesc response) {
            return FilterSettings.MIN_MATCHING_WORDS.getValue() + 10 ;
        }
        
    }
}
