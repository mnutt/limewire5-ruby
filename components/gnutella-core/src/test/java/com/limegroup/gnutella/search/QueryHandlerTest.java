package com.limegroup.gnutella.search;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;

import junit.framework.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.connection.GnutellaConnection;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.util.NewConnection;
import com.limegroup.gnutella.util.TestConnectionFactory;
import com.limegroup.gnutella.util.TestConnectionManager;
import com.limegroup.gnutella.util.TestResultCounter;


/**
 * Tests the functionality of the <tt>QueryHandlerTest</tt> class.
 */
public final class QueryHandlerTest extends LimeTestCase {

	private QueryRequestFactory queryRequestFactory;
    private QueryHandlerFactory queryHandlerFactory;
    private TestConnectionFactory testConnectionFactory;


    public QueryHandlerTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(QueryHandlerTest.class);
	}

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    private Injector createInjector(Module... modules) {
        Injector injector = LimeTestUtils.createInjector(modules);
        queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
        queryHandlerFactory = injector.getInstance(QueryHandlerFactory.class);
        testConnectionFactory = injector.getInstance(TestConnectionFactory.class);
        return injector;
    }
    
    /**
     * Tests the method for sending an individual query to a 
     * single connections.
     */
    public void testSendQueryToHost() throws Exception {
        
        createInjector();
        
        ReplyHandler rh = testConnectionFactory.createUltrapeerConnection();        
        QueryRequest query = queryRequestFactory.createQuery("test", (byte)1);
        QueryHandlerImpl qh = (QueryHandlerImpl)queryHandlerFactory.createHandler(query, rh, new TestResultCounter());
        GnutellaConnection mc = testConnectionFactory.createUltrapeerConnection();

        qh.sendQueryToHost(query, mc);

        List hostsQueried = qh.getQueriedConnections();
        assertEquals("should not have added host", 0, hostsQueried.size());

        // make sure we add the connection to the queries handlers
        // if it doesn't support probe queries and the TTL is 1
        qh.sendQueryToHost(query, testConnectionFactory.createLeafConnection(false));
        hostsQueried = qh.getQueriedConnections();
        assertEquals("should have added host", 1, hostsQueried.size());

        // make sure it adds the connection when the ttl is higher
        // than one and the connection supports probe queries
        qh.sendQueryToHost(queryRequestFactory.createQuery("test"), testConnectionFactory.createUltrapeerConnection());
        hostsQueried = qh.getQueriedConnections();
        assertEquals("should have added host", 2, hostsQueried.size());
    }    

    /**
     * Tests the method for calculating the next TTL.
     */
    public void testCalculateNewTTL() throws Exception {
        byte ttl = QueryHandlerImpl.calculateNewTTL(2000, 15, (byte)5);
        assertEquals("unexpected ttl", 3, ttl);

        ttl = QueryHandlerImpl.calculateNewTTL(200, 15, (byte)5);
        assertEquals("unexpected ttl", 2, ttl);


        ttl = QueryHandlerImpl.calculateNewTTL(200000, 15, (byte)4);
        assertEquals("unexpected ttl", 4, ttl);

        ttl = QueryHandlerImpl.calculateNewTTL(20000, 5, (byte)2);
        assertEquals("unexpected ttl", 2, ttl);
    }

    /**
     * Tests the public sendQuery method to make sure it's working as
     * expected.
     */
    public void testPublicSendQuery() throws Exception {
        
        Injector injector = createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(ConnectionManager.class).to(TestConnectionManager.class);
            }
        });
        
        TestConnectionManager testConnectionManager = injector.getInstance(TestConnectionManager.class);
        testConnectionManager.resetAndInitialize();
        
        QueryHandler handler = queryHandlerFactory.createHandler(queryRequestFactory.createQuery("test"),
                                       testConnectionFactory.createNewConnection(),
                                       new TestResultCounter(0));

        assertTrue(testConnectionManager.getInitializedConnections().size() > 0);

        handler.sendQuery();
        
        int numQueries = testConnectionManager.getNumUltrapeerQueries();
        assertEquals("unexpected number of probe queries sent", 3, numQueries);

        // these calls should not go through, as it's too soon after the probe
        // was sent!
        handler.sendQuery();
        handler.sendQuery();
        handler.sendQuery();

        assertEquals("unexpected number of probe queries sent", 3, 
            testConnectionManager.getNumUltrapeerQueries());        
        
        Thread.sleep(8000);

        handler.sendQuery();

        Thread.sleep(1000);

        // after the sleep, it should go through!
        assertEquals("unexpected number of queries sent", 4, testConnectionManager.getNumUltrapeerQueries());

        // these calls should not go through, as it's too soon after the last query
        // was sent!
        handler.sendQuery();
        handler.sendQuery();
        handler.sendQuery();

        assertEquals("unexpected number of queries sent", 4, testConnectionManager.getNumUltrapeerQueries());        
        
        Thread.sleep(8000);
        handler.sendQuery();

        // after the sleep, it should go through!
        assertEquals("unexpected number of queries sent", 5, testConnectionManager.getNumUltrapeerQueries());

        // now, send out a bunch of queries to make sure that, eventually, 
        // new queries don't go out because we've reached too high a theoretical
        // horizon.  These queries aren't returning any results, so the TTLs
        // should be relatively high, causing us to hit the theoretical 
        // horizon limit!

        for(int i=0; i<testConnectionManager.getInitializedConnections().size()-2; i++) {
            Thread.sleep(2000);
            handler.sendQuery();
        }        

        assertTrue("too many hosts queried! -- theoretical horizon too high", 
                   (testConnectionManager.getNumUltrapeerQueries() <= 12));
    }


    /**
     * Test to make sure that the theoretical horizon we've reached
     * is being calculated correctly.
     */
    public void testCalculateNewHosts() throws Exception {
        createInjector();
        
        // test for a degree 19, ttl 4 network
        GnutellaConnection mc = testConnectionFactory.createNewConnection(19);
        int horizon = 0;
        for(int i=0; i<19; i++) {
            horizon += QueryHandlerImpl.calculateNewHosts(mc, (byte)4);
        }                
        assertEquals("incorrect horizon", 117325, horizon);

        // test for a degree 30, ttl 3 network
        mc = testConnectionFactory.createNewConnection(30);
        horizon = 0;
        for(int i=0; i<30; i++) {
            horizon += QueryHandlerImpl.calculateNewHosts(mc, (byte)3);
        }                
        assertEquals("incorrect horizon", 26130, horizon);
    }

    
    /**
     * Tests the private method for sending queries to make sure the
     * basics of query dispatching are working correctly.
     */
    public void testPrivateSendQuery() throws Exception {
        
        createInjector();
        
		int numConnections = 15;
        List<NewConnection> connections = new ArrayList<NewConnection>();
        for(int i=0; i<numConnections; i++) {
            connections.add(testConnectionFactory.createNewConnection(10));
        }   

        QueryHandler handler = queryHandlerFactory.createHandler(queryRequestFactory.createQuery("test"),
                                       testConnectionFactory.createNewConnection(8),
                                       new TestResultCounter(0));
        
        // just send queries to all connections
        for(int i=0; i<numConnections; i++) {
            ((QueryHandlerImpl)handler).sendQuery(connections);
        }
        // just make sure all of the connections received the query
        Iterator<NewConnection> iter = connections.iterator();
        while(iter.hasNext()) {
            NewConnection tc = iter.next();
            assertTrue("should have received the query", tc.receivedQuery());
        }
    }
}
