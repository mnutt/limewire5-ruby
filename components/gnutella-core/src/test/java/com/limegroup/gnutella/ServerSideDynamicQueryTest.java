package com.limegroup.gnutella;

import java.util.Arrays;
import java.util.Iterator;

import junit.framework.Test;

import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.io.GUID;

import com.google.inject.Injector;
import com.google.inject.Stage;
import com.limegroup.gnutella.helpers.UrnHelper;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryReplyFactory;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.routing.RouteTableMessage;

/**
 *  Tests that an Ultrapeer correctly handle Probe queries.  Essentially tests
 *  the following methods of MessageRouter: handleQueryRequestPossibleDuplicate
 *  and handleQueryRequest
 *
 *  ULTRAPEER[0]  ----  CENTRAL TEST ULTRAPEER  ----  ULTRAPEER_2
 *                              |
 *                              |
 *                              |
 *                             LEAF[0]
 */
public final class ServerSideDynamicQueryTest extends ServerSideTestCase {
    
    private final int TIMEOUT = 2000;
    private QueryRequestFactory queryRequestFactory;
    private ResponseFactory responseFactory;
    private QueryReplyFactory queryReplyFactory;

    public ServerSideDynamicQueryTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ServerSideDynamicQueryTest.class);
    }    
   
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

	@Override
	public int getNumberOfUltrapeers() {
	    return 2;
    }

	@Override
	public int getNumberOfLeafpeers() {
	    return 1;
    }
	
    @Override
    public void setUpQRPTables() throws Exception {
        //3. routed leaf, with route table for "test"
        QueryRouteTable qrt = new QueryRouteTable();
        qrt.add("berkeley");
        qrt.add("susheel");
        for (Iterator iter=qrt.encode(null).iterator(); iter.hasNext(); ) {
            LEAF[0].send((RouteTableMessage)iter.next());
			LEAF[0].flush();
        }

        // for Ultrapeer 1
        qrt = new QueryRouteTable();
        qrt.add("leehsus");
        qrt.add("berkeley");
        for (Iterator iter=qrt.encode(null).iterator(); iter.hasNext(); ) {
            ULTRAPEER[0].send((RouteTableMessage)iter.next());
			ULTRAPEER[0].flush();
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
    
    public void testBasicProbeMechanicsFromUltrapeer() throws Exception {
        drainAll();

        QueryRequest request = queryRequestFactory.createQuery("berkeley");
        request.setTTL((byte)1);

        ULTRAPEER[1].send(request);
        ULTRAPEER[1].flush();

        QueryRequest reqRecvd = (QueryRequest) LEAF[0].receive(TIMEOUT);
        assertEquals("berkeley", reqRecvd.getQuery());
        assertTrue(Arrays.equals(request.getGUID(), reqRecvd.getGUID()));

        // should NOT be forwarded to other Ultrapeer
        assertTrue(BlockingConnectionUtils.noUnexpectedMessages(ULTRAPEER[0]));

        // make sure probes are routed back correctly....
		Response response1=responseFactory.createResponse(0L, 0L, "berkeley rocks", UrnHelper.SHA1);
		byte[] guid1=GUID.makeGuid();
		QueryReply reply1=queryReplyFactory.createQueryReply(request.getGUID(), (byte)2, 6346,
                IP, 56, new Response[] {response1}, guid1, false);
        BlockingConnectionUtils.drain(ULTRAPEER[1]);
		LEAF[0].send(reply1);
		LEAF[0].flush();
		QueryReply qRep = BlockingConnectionUtils.getFirstQueryReply(ULTRAPEER[1]);
        assertNotNull(qRep);
        assertEquals(new GUID(guid1), new GUID(qRep.getClientGUID()));

        Thread.sleep(2*1000);

        // extend the probe....
        request.setTTL((byte)2);
        ULTRAPEER[1].send(request);
        ULTRAPEER[1].flush();

        // leaves don't get any unexpected messages, no use using
        // noUnenexpectedMessages
        reqRecvd = BlockingConnectionUtils.getFirstQueryRequest(LEAF[0], TIMEOUT);
        assertNull("Did not expected a QueryRequest", reqRecvd);

        reqRecvd = BlockingConnectionUtils.getFirstQueryRequest(ULTRAPEER[0]);
        assertNotNull(reqRecvd);
        assertEquals("berkeley", reqRecvd.getQuery());
        assertTrue(Arrays.equals(request.getGUID(), reqRecvd.getGUID()));
        assertEquals((byte)1, reqRecvd.getHops());
    }


    public void testBasicProbeMechanicsFromLeaf() throws Exception {
        drainAll();

        QueryRequest request = queryRequestFactory.createQuery("berkeley");
        request.hop();
        request.setTTL((byte)1);
        assertEquals(1, request.getHops());

        ULTRAPEER[1].send(request);
        ULTRAPEER[1].flush();

        QueryRequest reqRecvd = (QueryRequest) LEAF[0].receive(TIMEOUT);
        assertEquals("berkeley", reqRecvd.getQuery());
        assertTrue(Arrays.equals(request.getGUID(), reqRecvd.getGUID()));

        // should NOT be forwarded to other Ultrapeer
        assertTrue(BlockingConnectionUtils.noUnexpectedMessages(ULTRAPEER[0]));

        // make sure probes are routed back correctly....
		Response response1=responseFactory.createResponse(0L, 0L, "berkeley rocks", UrnHelper.SHA1);
		byte[] guid1=GUID.makeGuid();
		QueryReply reply1=queryReplyFactory.createQueryReply(request.getGUID(), (byte)2, 6346,
                IP, 56, new Response[] {response1}, guid1, false);
        BlockingConnectionUtils.drain(ULTRAPEER[1]);
		LEAF[0].send(reply1);
		LEAF[0].flush();
		QueryReply qRep = BlockingConnectionUtils.getFirstQueryReply(ULTRAPEER[1]);
        assertNotNull(qRep);
        assertEquals(new GUID(guid1), new GUID(qRep.getClientGUID()));

        Thread.sleep(2*1000);

        // extend the probe....
        request.setTTL((byte)2);
        ULTRAPEER[1].send(request);
        ULTRAPEER[1].flush();

        // leaves don't get any unexpected messages, no use using
        // noUnenexpectedMessages
        reqRecvd = BlockingConnectionUtils.getFirstQueryRequest(LEAF[0], TIMEOUT);
        assertNull("Did not expected a QueryRequest", reqRecvd);

        reqRecvd = BlockingConnectionUtils.getFirstQueryRequest(ULTRAPEER[0]);
        assertNotNull(reqRecvd);
        assertEquals("berkeley", reqRecvd.getQuery());
        assertTrue(Arrays.equals(request.getGUID(), reqRecvd.getGUID()));
        assertEquals((byte)2, reqRecvd.getHops());
    }


    public void testDuplicateProbes() throws Exception {
        drainAll();

        QueryRequest request = queryRequestFactory.createQuery("berkeley");
        request.setTTL((byte)1);

        ULTRAPEER[1].send(request);
        ULTRAPEER[1].flush();

        QueryRequest reqRecvd = (QueryRequest) LEAF[0].receive(TIMEOUT);
        assertEquals("berkeley", reqRecvd.getQuery());
        assertTrue(Arrays.equals(request.getGUID(), reqRecvd.getGUID()));

        // should NOT be forwarded to other Ultrapeer
        assertTrue(BlockingConnectionUtils.noUnexpectedMessages(ULTRAPEER[0]));

        Thread.sleep(2*1000);

        // test that the duplicate probe doesn't go anywhere it isn't supposed
        ULTRAPEER[1].send(request);
        ULTRAPEER[1].flush();

        // should NOT be forwarded to leaf again....
        // leaves don't get any unexpected messages, no use using
        // noUnenexpectedMessages
        reqRecvd = BlockingConnectionUtils.getFirstQueryRequest(LEAF[0], TIMEOUT);
        assertNull("Did not expected a QueryRequest", reqRecvd);

        // should NOT be forwarded to other Ultrapeer....
        assertTrue(BlockingConnectionUtils.noUnexpectedMessages(ULTRAPEER[0]));
    }
    
    // makes sure a probe can't be extended twice....
    public void testProbeIsLimited() throws Exception {
        drainAll();

        QueryRequest request = queryRequestFactory.createQuery("berkeley");
        request.setTTL((byte)1);

        ULTRAPEER[1].send(request);
        ULTRAPEER[1].flush();

        QueryRequest reqRecvd = (QueryRequest) LEAF[0].receive(TIMEOUT);
        assertEquals("berkeley", reqRecvd.getQuery());
        assertTrue(Arrays.equals(request.getGUID(), reqRecvd.getGUID()));

        // should NOT be forwarded to other Ultrapeer
        assertTrue(BlockingConnectionUtils.noUnexpectedMessages(ULTRAPEER[0]));

        Thread.sleep(2*1000);

        // extend the probe....
        request.setTTL((byte)3);
        ULTRAPEER[1].send(request);
        ULTRAPEER[1].flush();

        // leaf should not get anything
        assertTrue(BlockingConnectionUtils.noUnexpectedMessages(LEAF[0]));

        reqRecvd = BlockingConnectionUtils.getFirstQueryRequest(ULTRAPEER[0]);
        assertNotNull(reqRecvd);
        assertEquals("berkeley", reqRecvd.getQuery());
        assertTrue(Arrays.equals(request.getGUID(), reqRecvd.getGUID()));
        assertEquals((byte)1, reqRecvd.getHops());

        Thread.sleep(2*1000);

        // extend it again but make sure it doesn't get through...
        request.setTTL((byte)4);
        ULTRAPEER[1].send(request);
        ULTRAPEER[1].flush();

        // should NOT be forwarded to leaf again....
        // leaves don't get any unexpected messages, no use using
        // noUnenexpectedMessages
        reqRecvd = BlockingConnectionUtils.getFirstQueryRequest(LEAF[0], TIMEOUT);
        assertNull("Did not expected a QueryRequest", reqRecvd);

        // should NOT be forwarded to other Ultrapeer....
        assertTrue(BlockingConnectionUtils.noUnexpectedMessages(ULTRAPEER[0]));
    }

    // tries to extend queries with original TTL > 1, should fail...
    public void testProbeIsTTL1Only() throws Exception {
        for (int i = 2; i < 5; i++) {
            drainAll();

            QueryRequest request = queryRequestFactory.createQuery("berkeley");
            request.setTTL((byte)i);

            ULTRAPEER[1].send(request);
            ULTRAPEER[1].flush();

            QueryRequest reqRecvd = (QueryRequest) LEAF[0].receive(TIMEOUT);
            assertEquals("berkeley", reqRecvd.getQuery());
            assertTrue(Arrays.equals(request.getGUID(), reqRecvd.getGUID()));

            // should be forwarded to other Ultrapeer
            reqRecvd = BlockingConnectionUtils.getFirstQueryRequest(ULTRAPEER[0]);
            assertNotNull(reqRecvd);
            assertEquals("berkeley", reqRecvd.getQuery());
            assertTrue(Arrays.equals(request.getGUID(), reqRecvd.getGUID()));
            assertEquals((byte)1, reqRecvd.getHops());
            
            Thread.sleep(2*1000);
            
            // extend the probe....
            request.setTTL((byte)(i+1));
            ULTRAPEER[1].send(request);
            ULTRAPEER[1].flush();

            // should be counted as a duplicate and not forwarded anywhere...
            // leaves don't get any unexpected messages, no use using
            // noUnenexpectedMessages
            reqRecvd = BlockingConnectionUtils.getFirstQueryRequest(LEAF[0], TIMEOUT);
            assertNull("Did not expected a QueryRequest", reqRecvd);

            assertTrue(BlockingConnectionUtils.noUnexpectedMessages(ULTRAPEER[0]));
        }
    }
}
