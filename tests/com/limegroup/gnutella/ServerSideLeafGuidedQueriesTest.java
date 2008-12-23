package com.limegroup.gnutella;

import java.util.Iterator;
import java.util.Random;

import org.limewire.io.GUID;

import junit.framework.Test;

import com.google.inject.Injector;
import com.google.inject.Stage;
import com.limegroup.gnutella.connection.BlockingConnection;
import com.limegroup.gnutella.helpers.UrnHelper;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryReplyFactory;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutella.messages.vendor.QueryStatusResponse;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.routing.RouteTableMessage;

/**
 *  Tests that an Ultrapeer correctly handle Probe queries.  Essentially tests
 *  the following methods of MessageRouter: handleQueryRequestPossibleDuplicate
 *  and handleQueryRequest
 *
 *  ULTRAPEER_1  ----  CENTRAL TEST ULTRAPEER  ----  ULTRAPEER_2
 *                              |
 *                              |
 *                              |
 *                             LEAF
 */
@SuppressWarnings("null")
public final class ServerSideLeafGuidedQueriesTest extends ServerSideTestCase {

    private final int TIMEOUT = 300;
    private QueryRequestFactory queryRequestFactory;
    private ResponseFactory responseFactory;
    private QueryReplyFactory queryReplyFactory;
    
    public ServerSideLeafGuidedQueriesTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ServerSideLeafGuidedQueriesTest.class);
    }    
   
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	@Override
	public int getNumberOfUltrapeers() {
	    return 29;
    }

	@Override
	public int getNumberOfLeafpeers() {
	    return 1;
    }
	
    @Override
    public void setSettings() throws Exception {
    }

    public void quickDrainAll() throws Exception {
        BlockingConnectionUtils.drainAll(ULTRAPEER, TIMEOUT);
        BlockingConnectionUtils.drainAll(LEAF, TIMEOUT);
    }

    @Override
    public void setUpQRPTables() throws Exception {
        QueryRouteTable qrt = new QueryRouteTable();
        qrt.add("berkeley");
        qrt.add("susheel");
        qrt.addIndivisible(UrnHelper.UNIQUE_SHA1.toString());
        for (Iterator iter=qrt.encode(null).iterator(); iter.hasNext(); ) {
            LEAF[0].send((RouteTableMessage)iter.next());
			LEAF[0].flush();
        }

        // for Ultrapeers
        for (int i = 0; i < ULTRAPEER.length; i++) {
            qrt = new QueryRouteTable();
            qrt.add("leehsus");
            qrt.add("berkeley");
            for (Iterator iter=qrt.encode(null).iterator(); iter.hasNext(); ) {
                ULTRAPEER[i].send((RouteTableMessage)iter.next());
                ULTRAPEER[i].flush();
            }
        }
    }
    
    @Override
    protected void setUp() throws Exception {
        Injector injector = LimeTestUtils.createInjector(Stage.PRODUCTION);
        super.setUp(injector);
        queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
        responseFactory = injector.getInstance(ResponseFactory.class);
        queryReplyFactory = injector.getInstance(QueryReplyFactory.class);
    }


    public void testConfirmSupport() throws Exception {
        Message m = BlockingConnectionUtils.getFirstMessageOfType(LEAF[0],
                        MessagesSupportedVendorMessage.class);
        assertNotNull(m);
        
        MessagesSupportedVendorMessage msvm =
            (MessagesSupportedVendorMessage)m;

        assertGreaterThan(0, msvm.supportsLeafGuidance());
    }


    public void testLeafIsDone() throws Exception {
        quickDrainAll();
        // we want to make sure that the leaf correctly guides the queries to
        // stop.

        // send a query from the leaf
        QueryRequest query = queryRequestFactory.createQuery("berkeley");
        LEAF[0].send(query);
        LEAF[0].flush();

        // one or more of the UPs should get it....
        Thread.sleep(3000);
        QueryRequest nQuery = null;
        for (int i = 0; (i < ULTRAPEER.length); i++)
            if (nQuery == null)
                nQuery = BlockingConnectionUtils.getFirstQueryRequest(ULTRAPEER[i], TIMEOUT);

        assertTrue(nQuery != null);

        // now tell the main UP that you have got enough results
        QueryStatusResponse sResp = 
             new QueryStatusResponse(new GUID(query.getGUID()), 250);
        LEAF[0].send(sResp);
        LEAF[0].flush();

        quickDrainAll();
        // UPs should not get any more queries
        Thread.sleep(3000);
        nQuery = null;
        for (int i = 0; (i < ULTRAPEER.length); i++) {
            nQuery = BlockingConnectionUtils.getFirstQueryRequest(ULTRAPEER[i], TIMEOUT);
            assertNull(nQuery);
        }
        
    }


    public void testUPRoutedEnough() throws Exception {
        quickDrainAll();

        // we want to make sure that the UP stops the query when enough results
        // have been routed

        // send a query from the leaf
        QueryRequest query = queryRequestFactory.createQuery("berkeley");
        LEAF[0].send(query);
        LEAF[0].flush();

        // one or more of the UPs should get it....
        Thread.sleep(3000);
        QueryRequest nQuery = null;
        for (int i = 0; (i < ULTRAPEER.length); i++) {
            QueryRequest local = BlockingConnectionUtils.getFirstQueryRequest(ULTRAPEER[i], TIMEOUT);
            if ((nQuery == null) && (local != null))
                nQuery = local;
            // send 10 results from this Ultrapeer....
            for (int j = 0; (j < 10) && (local != null); j++)
                routeResultsToUltrapeer(nQuery.getGUID(), ULTRAPEER[i]);
        }

        assertTrue(nQuery != null);

        // UPs should get more queries
        Thread.sleep(5000);
        nQuery = null;
        for (int i = 0; (i < ULTRAPEER.length); i++) {
            QueryRequest local  = BlockingConnectionUtils.getFirstQueryRequest(ULTRAPEER[i], 
                                                       TIMEOUT*2);
            if ((nQuery == null) && (local != null))
                nQuery = local;
        }

        assertNotNull(nQuery);

        // now send enough results, we shouldn't get no more queries yo
        routeResultsToUltrapeers(query.getGUID(), 200);
        quickDrainAll(); // do this to make sure no queries were sent while executing

        // we shouldn't get no more queries yo
        Thread.sleep(3000);
        nQuery = null;
        for (int i = 0; (i < ULTRAPEER.length); i++) {
            nQuery = BlockingConnectionUtils.getFirstQueryRequest(ULTRAPEER[i], TIMEOUT);
            assertNull(nQuery);
        }
    }


    public void testUPRoutedMax() throws Exception {
        quickDrainAll();

        // we want to make sure that the UP stops the query when enough results
        // have been routed AND the leaf still wants more

        // send a query from the leaf
        QueryRequest query = queryRequestFactory.createQuery("berkeley");
        LEAF[0].send(query);
        LEAF[0].flush();

        // one or more of the UPs should get it....
        Thread.sleep(3000);
        QueryRequest nQuery = null;
        for (int i = 0; (i < ULTRAPEER.length) && (nQuery == null); i++) {
            QueryRequest local = BlockingConnectionUtils.getFirstQueryRequest(ULTRAPEER[i], TIMEOUT);
            if ((nQuery == null) && (local != null))
                nQuery = local;
            // send 10 results from this Ultrapeer....
            for (int j = 0; (j < 10) && (local != null); j++)
                routeResultsToUltrapeer(nQuery.getGUID(), ULTRAPEER[i]);
        }

        assertTrue(nQuery != null);

        // send a QueryStatus, but not one that will make the UP stop
        QueryStatusResponse sResp = 
             new QueryStatusResponse(new GUID(query.getGUID()), 25);
        LEAF[0].send(sResp);
        LEAF[0].flush();

        // UPs should get more queries
        Thread.sleep(3000);
        nQuery = null;
        for (int i = 0; (i < ULTRAPEER.length) && (nQuery == null); i++) {
            QueryRequest local  = BlockingConnectionUtils.getFirstQueryRequest(ULTRAPEER[i], TIMEOUT);
            if ((nQuery == null) && (local != null))
                nQuery = local;
        }

        assertNotNull(nQuery);

        // now send enough results, we shouldn't get no more queries yo
        routeResultsToUltrapeers(query.getGUID(), 200);
        quickDrainAll(); // do this to make sure no queries were sent while executing

        // we shouldn't get no more queries yo
        Thread.sleep(4000);
        nQuery = null;
        for (int i = 0; (i < ULTRAPEER.length); i++) {
            nQuery = BlockingConnectionUtils.getFirstQueryRequest(ULTRAPEER[i], TIMEOUT);
            assertNull(nQuery);
        }
    }


    public void testLongLivedLeafGuidance() throws Exception {
        quickDrainAll();

        // we want to make sure that the UP stops the query when enough results
        // have been routed AND the leaf still wants more

        // send a query from the leaf
        QueryRequest query = queryRequestFactory.createQuery("berkeley");
        LEAF[0].send(query);
        LEAF[0].flush();

        // one or more of the UPs should get it....
        Thread.sleep(3000);
        QueryRequest nQuery = null;
        for (int i = 0; (i < ULTRAPEER.length) && (nQuery == null); i++) {
            QueryRequest local = BlockingConnectionUtils.getFirstQueryRequest(ULTRAPEER[i], TIMEOUT);
            if ((nQuery == null) && (local != null))
                nQuery = local;
        }

        assertTrue(nQuery != null);

        // send a QueryStatus, but not one that will make the UP stop
        QueryStatusResponse sResp = 
             new QueryStatusResponse(new GUID(query.getGUID()), 10);
        LEAF[0].send(sResp);
        LEAF[0].flush();

        // UPs should get more queries
        Thread.sleep(3000);
        nQuery = null;
        for (int i = 0; (i < ULTRAPEER.length) && (nQuery == null); i++) {
            QueryRequest local = BlockingConnectionUtils.getFirstQueryRequest(ULTRAPEER[i], TIMEOUT);
            if ((nQuery == null) && (local != null))
                nQuery = local;
        }

        assertNotNull(nQuery);

        // send a QueryStatus, but not one that will make the UP stop
        sResp = new QueryStatusResponse(new GUID(query.getGUID()), 30);
        LEAF[0].send(sResp);
        LEAF[0].flush();

        // UPs should get more queries
        Thread.sleep(4000);
        nQuery = null;
        for (int i = 0; (i < ULTRAPEER.length) && (nQuery == null); i++) {
            QueryRequest local = BlockingConnectionUtils.getFirstQueryRequest(ULTRAPEER[i], TIMEOUT);
            if ((nQuery == null) && (local != null))
                nQuery = local;
        }

        assertNotNull(nQuery);

        // send a QueryStatus that will make UP stop
        sResp = new QueryStatusResponse(new GUID(query.getGUID()), 50);
        LEAF[0].send(sResp);
        LEAF[0].flush();
        quickDrainAll(); // do this to make sure no queries were sent while executing

        // we shouldn't get no more queries yo
        Thread.sleep(3000);
        nQuery = null;
        for (int i = 0; (i < ULTRAPEER.length); i++) {
            nQuery = BlockingConnectionUtils.getFirstQueryRequest(ULTRAPEER[i], TIMEOUT);
            assertNull(nQuery);
        }
    }


    private void routeResultsToUltrapeers(byte[] guid, int numResults) 
        throws Exception {
        Random rand = new Random();
        for (int i = 0; i < numResults; i++) {
            int index = rand.nextInt(ULTRAPEER.length);
            routeResultsToUltrapeer(guid, ULTRAPEER[index]);
        }
    }

    private void routeResultsToUltrapeer(byte[] guid, BlockingConnection source) 
        throws Exception {
        byte[] ip = new byte[] {(byte)127, (byte)0, (byte)0, (byte)1};
        byte[] clientGUID = GUID.makeGuid();
        Response[] resp = new Response[] {responseFactory.createResponse(0, 10, "berkeley", UrnHelper.SHA1)};
        QueryReply reply = queryReplyFactory.createQueryReply(guid, (byte)3, 6346,
                ip, 0, resp, clientGUID, false);
        source.send(reply);
        source.flush();
    }

    

}
