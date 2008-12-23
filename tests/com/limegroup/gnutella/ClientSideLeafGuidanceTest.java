package com.limegroup.gnutella;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import junit.framework.Test;

import org.limewire.core.settings.SearchSettings;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.GUID;

import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.Stage;
import com.limegroup.gnutella.connection.BlockingConnection;
import com.limegroup.gnutella.connection.RoutedConnection;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;
import com.limegroup.gnutella.helpers.UrnHelper;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryReplyFactory;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactory;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutella.messages.vendor.QueryStatusResponse;
import com.limegroup.gnutella.search.SearchResultHandler;
import com.limegroup.gnutella.spam.SpamManager;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.DataUtils;

/**
 * Checks whether (multi)leaves avoid forwarding messages to ultrapeers, do
 * redirects properly, etc.  The test includes a leaf attached to 3 
 * Ultrapeers.
 */
@SuppressWarnings("all")
public class ClientSideLeafGuidanceTest extends ClientSideTestCase {
    
    private final int REPORT_INTERVAL = SearchResultHandler.REPORT_INTERVAL;
    private final int MAX_RESULTS = SearchResultHandler.MAX_RESULTS;
    private SearchServices searchServices;
    private QueryReplyFactory queryReplyFactory;
    private ResponseFactory responseFactory;
    private MyActivityCallback callback;
    private SpamManager spamManager;
    private MessagesSupportedVendorMessage messagesSupportedVendorMessage;

    public ClientSideLeafGuidanceTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ClientSideLeafGuidanceTest.class);
    }    

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    protected void setUp() throws Exception {
        Injector injector = LimeTestUtils.createInjector(Stage.PRODUCTION, MyActivityCallback.class);
        super.setUp(injector);
        
        searchServices = injector.getInstance(SearchServices.class);
        queryReplyFactory = injector.getInstance(QueryReplyFactory.class);
        responseFactory = injector.getInstance(ResponseFactory.class);
        callback = (MyActivityCallback) injector.getInstance(ActivityCallback.class);
        spamManager = injector.getInstance(SpamManager.class);
        messagesSupportedVendorMessage = injector.getInstance(MessagesSupportedVendorMessage.class);
        
        for (int i = 0; i < testUP.length; i++) {
            // send a MessagesSupportedMessage
            testUP[i].send(messagesSupportedVendorMessage);
            // does this not flush on purpose?  testBasicGuidance fails if we flush here.
        }
    }
    
    private void establishGuidance() throws Exception {
        CapabilitiesVMFactory cvmf = injector.getInstance(CapabilitiesVMFactory.class);
        for (BlockingConnection up : testUP) {
            up.send(cvmf.getCapabilitiesVM());
            up.flush();
        }
        Thread.sleep(100);
        ConnectionManager cm = injector.getInstance(ConnectionManager.class);
        for (RoutedConnection c : cm.getInitializedConnections())
            assertGreaterThan(0,c.getConnectionCapabilities().remoteHostSupportsLeafGuidance());
    }
    
    /** @return The first QueyrRequest received from this connection.  If null
     *  is returned then it was never recieved (in a timely fashion).
     */
    private QueryStatusResponse getFirstQueryStatus(BlockingConnection c)
                                        throws BadPacketException, IOException {
        return BlockingConnectionUtils.getFirstInstanceOfMessageType(c, QueryStatusResponse.class, TIMEOUT);
    }
    
    public void testBasicGuidance() throws Exception {
        
        // spawn a query and make sure all UPs get it
        GUID queryGuid = new GUID(searchServices.newQueryGUID());
        searchServices.query(queryGuid.bytes(), "susheel");
        Thread.sleep(250);

        for (int i = 0; i < testUP.length; i++) {
            QueryRequest qr = BlockingConnectionUtils.getFirstQueryRequest(testUP[i]);
            assertNotNull(qr);
            assertEquals(new GUID(qr.getGUID()), queryGuid);
        }

        // now send back results and make sure that we get a QueryStatus
        // from the leaf
        Message m = null;
        // ensure that we'll get a QueryStatusResponse from the Responses
        // we're sending.
        assertGreaterThan(REPORT_INTERVAL, 6*testUP.length);
        for (int i = 0; i < testUP.length; i++) {
            Response[] res = new Response[] {
                responseFactory.createResponse(10, 10, "susheel"+i, UrnHelper.SHA1),
                responseFactory.createResponse(10, 10, "susheel smells good"+i, UrnHelper.SHA1),
                responseFactory.createResponse(10, 10, "anita is sweet"+i, UrnHelper.SHA1),
                responseFactory.createResponse(10, 10, "anita is prety"+i, UrnHelper.SHA1),
                responseFactory.createResponse(10, 10, "susheel smells bad" + i, UrnHelper.SHA1),
                responseFactory.createResponse(10, 10, "renu is sweet " + i, UrnHelper.SHA1),
                responseFactory.createResponse(10, 10, "prety is spelled pretty " + i, UrnHelper.SHA1),
                responseFactory.createResponse(10, 10, "go susheel go" + i, UrnHelper.SHA1),
                responseFactory.createResponse(10, 10, "susheel runs fast" + i, UrnHelper.SHA1),
                responseFactory.createResponse(10, 10, "susheel jumps high" + i, UrnHelper.SHA1),
                responseFactory.createResponse(10, 10, "sleepy susheel" + i, UrnHelper.SHA1),
            };
            m = queryReplyFactory.createQueryReply(queryGuid.bytes(), (byte) 1, 6355,
                    myIP(), 0, res, GUID.makeGuid(), new byte[0], false, false,
                    true, true, false, false, null);
            testUP[i].send(m);
            testUP[i].flush();
        }
        
        // all UPs should get a QueryStatusResponse
        for (int i = 0; i < testUP.length; i++) {
            QueryStatusResponse stat = getFirstQueryStatus(testUP[i]);
            assertNotNull("up: " + i + " failed", stat);
            assertEquals("up: " + i + " failed", new GUID(stat.getGUID()), queryGuid);
            assertEquals("up: " + i + " failed", 5, stat.getNumResults());
        }

        // shut off the query....
        searchServices.stopQuery(queryGuid);

        // all UPs should get a QueryStatusResponse with 65535
        for (int i = 0; i < testUP.length; i++) {
            QueryStatusResponse stat = getFirstQueryStatus(testUP[i]);
            assertNotNull("up: " + i + " failed", stat);
            assertEquals("up: " + i + " failed", new GUID(stat.getGUID()), queryGuid);
            assertEquals("up: " + i + " failed", 65535, stat.getNumResults());
        }
    }

    public void testAdvancedGuidance1() throws Exception {
        establishGuidance();
        for (int i = 0; i < testUP.length; i++)
            BlockingConnectionUtils.drain(testUP[i]);
        
        // spawn a query and make sure all UPs get it
        GUID queryGuid = new GUID(searchServices.newQueryGUID());
        searchServices.query(queryGuid.bytes(), "susheel daswanu");

        for (int i = 0; i < testUP.length; i++) {
            QueryRequest qr = BlockingConnectionUtils.getFirstQueryRequest(testUP[i]);
            assertNotNull(qr);
            assertEquals(new GUID(qr.getGUID()), queryGuid);
        }

        // now send back results and make sure that we get a QueryStatus
        // from the leaf
        Message m = null;
        for (int i = 0; i < testUP.length; i++) {
            //send enough responses per ultrapeer to shut off querying.
            Response[] res = new Response[150/testUP.length + 10];
            for (int j = 0; j < res.length; j++)
                res[j] = responseFactory.createResponse(10, 10, "susheel good"+i+j, UrnHelper.SHA1);

            m = queryReplyFactory.createQueryReply(queryGuid.bytes(), (byte) 1, 6355,
                    myIP(), 0, res, GUID.makeGuid(), new byte[0], false, false,
                    true, true, false, false, null);
            testUP[i].send(m);
            testUP[i].flush();
            System.out.println("sent response from "+testUP[i]);
        }
        
        // all UPs should get a QueryStatusResponse
        boolean maxResultsEncountered = false;
        for (int i = 0; i < testUP.length; i++) {
            for (int j = 0; j < testUP.length; j++) {
                QueryStatusResponse stat = getFirstQueryStatus(testUP[j]);
                assertNotNull("failed on up: " + j, stat);
                assertEquals("failed on up: " + j, new GUID(stat.getGUID()), queryGuid);
                // depending on how far along the query is we could have a
                // number or 65535 - the number 11 depends on settings such as
                // REPORT_INTERVAL
                if (stat.getNumResults() == MAX_RESULTS) {
                    maxResultsEncountered = true;
                } else {
                    //assertEquals(11*(i+1), stat.getNumResults());
                    // there is no sane way this can be asserted.
                }   
            }
        }
        assertTrue(maxResultsEncountered);

        // now, even though we send more responses, we shoudl NOT get any more
        // leaf guidance...
        Response[] res = new Response[REPORT_INTERVAL*4];
        for (int j = 0; j < res.length; j++)
            res[j] = responseFactory.createResponse(10, 10, "anita is pretty"+j, UrnHelper.SHA1);
        m = queryReplyFactory.createQueryReply(queryGuid.bytes(), (byte) 1, 6355,
                myIP(), 0, res, GUID.makeGuid(), new byte[0], false, false,
                true, true, false, false, null);
        
        testUP[0].send(m);
        testUP[0].flush();

        // no UPs should get a QueryStatusResponse
        drainQSResponses();
    }


    public void testAdvancedGuidance2() throws Exception {
        establishGuidance();
        Message m = null;

        for (int i = 0; i < testUP.length; i++)
            BlockingConnectionUtils.drain(testUP[i]);
        
        // spawn a query and make sure all UPs get it
        GUID queryGuid = new GUID(searchServices.newQueryGUID());
        searchServices.query(queryGuid.bytes(), "anita kesavan");

        for (int i = 0; i < testUP.length; i++) {
            QueryRequest qr = BlockingConnectionUtils.getFirstQueryRequest(testUP[i]);
            assertNotNull(qr);
            assertEquals(new GUID(qr.getGUID()), queryGuid);
        }

        // now send back results and make sure that we get a QueryStatus
        // from the leaf
        Response[] res = new Response[REPORT_INTERVAL*4];
        for (int j = 0; j < res.length; j++)
            res[j] = responseFactory.createResponse(10, 10, "anita is pretty"+j, UrnHelper.SHA1);

        m = queryReplyFactory.createQueryReply(queryGuid.bytes(), (byte) 1, 6355,
                myIP(), 0, res, GUID.makeGuid(), new byte[0], false, false,
                true, true, false, false, null);
        testUP[0].send(m);
        testUP[0].flush();

        // all UPs should get a QueryStatusResponse
        for (int i = 0; i < testUP.length; i++) {
            QueryStatusResponse stat = getFirstQueryStatus(testUP[i]);
            assertNotNull("failed on up: " + i, stat);
            assertEquals("failed on up: " + i, new GUID(stat.getGUID()), queryGuid);
            assertEquals("failed on up: " + i, REPORT_INTERVAL, stat.getNumResults());
        }


        // now send just a few responses - less than the number of
        // REPORT_INTERVAL - and confirm we don't get messages
        res = new Response[REPORT_INTERVAL-1];
        for (int j = 0; j < res.length; j++)
            res[j] = responseFactory.createResponse(10, 10, "anita is sweet"+j, UrnHelper.SHA1);

        m = queryReplyFactory.createQueryReply(queryGuid.bytes(), (byte) 1, 6355,
                myIP(), 0, res, GUID.makeGuid(), new byte[0], false, false,
                true, true, false, false, null);
        
        testUP[2].send(m);
        testUP[2].flush();

        // no UPs should get a QueryStatusResponse
        for (int i = 0; i < testUP.length; i++) {
            QueryStatusResponse stat = getFirstQueryStatus(testUP[i]);
            assertNull(stat);
        }

        // simply send 2 more responses....
        res = new Response[2];
        for (int j = 0; j < res.length; j++)
            res[j] = responseFactory.createResponse(10, 10, "anita is young"+j, UrnHelper.SHA1);

        m = queryReplyFactory.createQueryReply(queryGuid.bytes(), (byte) 1, 6355,
                myIP(), 0, res, GUID.makeGuid(), new byte[0], false, false,
                true, true, false, false, null);
        
        testUP[1].send(m);
        testUP[1].flush();

        // and all UPs should get a QueryStatusResponse
        for (int i = 0; i < testUP.length; i++) {
            QueryStatusResponse stat = getFirstQueryStatus(testUP[i]);
            assertNotNull(stat);
            assertEquals(new GUID(stat.getGUID()), queryGuid);
            assertEquals(REPORT_INTERVAL+((REPORT_INTERVAL+1)/4), 
                         stat.getNumResults());
        }

        // shut off the query....
        searchServices.stopQuery(queryGuid);

        // all UPs should get a QueryStatusResponse with 65535
        for (int i = 0; i < testUP.length; i++) {
            QueryStatusResponse stat = getFirstQueryStatus(testUP[i]);
            assertNotNull(stat);
            assertEquals(new GUID(stat.getGUID()), queryGuid);
            assertEquals(65535, stat.getNumResults());
        }

        // more results should not result in more status messages...
        res = new Response[REPORT_INTERVAL*2];
        for (int j = 0; j < res.length; j++)
            res[j] = responseFactory.createResponse(10, 10, "anita is pretty"+j, UrnHelper.SHA1);

        m = queryReplyFactory.createQueryReply(queryGuid.bytes(), (byte) 1, 6355,
                myIP(), 0, res, GUID.makeGuid(), new byte[0], false, false,
                true, true, false, false, null);
        
        testUP[0].send(m);
        testUP[0].flush();

        // no UPs should get a QueryStatusResponse
        drainQSResponses();
    }
    
    /**
     * tests that spammy results are shown to the gui but are not counted 
     * in leaf guidance
     */
    public void testSpamFiltering1() throws Exception {
        SearchSettings.ENABLE_SPAM_FILTER.setValue(true);
        SearchSettings.FILTER_SPAM_RESULTS.setValue(0.5f); // Strict
        spamManager.clearFilterData();        
        callback.responses.clear();
        final String query = "badgers";
        final int size = 1234;

        // Spawn a query and make sure the UPs get it
        GUID queryGuid = spawnQuery(query);

        // Mark a result as spam so the later results will be rated as spam
        RemoteFileDescFactory rfdFactory =
            injector.getInstance(RemoteFileDescFactory.class); 
        RemoteFileDesc rfd = rfdFactory.createRemoteFileDesc(
                new ConnectableImpl("127.0.0.1", 6355, false),
                1, query, size, DataUtils.EMPTY_GUID, 3, false,
                3, false, null, URN.NO_URN_SET, false, "ALT", 0l);
        spamManager.handleUserMarkedSpam(new RemoteFileDesc[]{rfd});
        assertTrue(rfd.isSpam());

        // Send back results from the UP - they should be rated as spam
        // because they have the same address and size as the spam result
        Response[] res = new Response[REPORT_INTERVAL*4];
        for (int i = 0; i < res.length; i++)
            res[i] = responseFactory.createResponse(10, size, query + i, UrnHelper.SHA1);

        QueryReply reply = queryReplyFactory.createQueryReply(
                queryGuid.bytes(), (byte) 1, 6355, myIP(), 0, res,
                GUID.makeGuid(), new byte[0], false, false, true, true,
                false, false, null);

        testUP[0].send(reply);
        testUP[0].flush();
        Thread.sleep(1000);

        // the gui should be informed about the results 
        assertEquals(res.length, callback.responses.size());

        // the UP should not get a QueryStatusResponse for spam results
        QueryStatusResponse qsr = getFirstQueryStatus(testUP[0]);
        assertNull(qsr);
    }
    
    /**
     * tests that non-spammy results are shown to the gui and counted in
     * leaf guidance
     */
    public void testSpamFiltering2() throws Exception {
        SearchSettings.ENABLE_SPAM_FILTER.setValue(true);
        SearchSettings.FILTER_SPAM_RESULTS.setValue(0.5f); // Strict
        spamManager.clearFilterData();
        callback.responses.clear();
        
        // spawn a query and make sure all UPs get it
        GUID queryGuid = spawnQuery("anita kesavan");
        
        // now send back results
        Response[] res = new Response[REPORT_INTERVAL*4];
        for (int i = 0; i < res.length; i++)
            res[i] = responseFactory.createResponse(10, 10, "anita kesavan "+i, UrnHelper.SHA1);

        QueryReply reply =
            queryReplyFactory.createQueryReply(queryGuid.bytes(), (byte) 1,
                    6355, myIP(), 0, res, GUID.makeGuid(), new byte[0],
                    false, false, true, true, false, false, null);
        
        testUP[0].send(reply);
        testUP[0].flush();
        Thread.sleep(1000);
        
        // the gui should be informed about the results 
        assertEquals(res.length, callback.responses.size());
        
        // the UP should get a QueryStatusResponse for non-spam results
        QueryStatusResponse qsr = getFirstQueryStatus(testUP[0]);
        assertNotNull(qsr);
        assertEquals(res.length/4, qsr.getNumResults());
    }
    
    /**
     * Spawns a query and makes sure all the UPs have received it
     * 
     * @param query the query string
     * @returns the GUID of the query
     */
    private GUID spawnQuery(String query) throws Exception {
        GUID queryGuid = new GUID(searchServices.newQueryGUID());
        searchServices.query(queryGuid.bytes(), query);
        Thread.sleep(250);
        for(BlockingConnection up : testUP) {
            QueryRequest qr = BlockingConnectionUtils.getFirstQueryRequest(up);
            assertNotNull(qr);
            assertEquals(new GUID(qr.getGUID()), queryGuid);
        }
        return queryGuid;
    }
    
    /**
     * drains the ultrapeers for any QueryStatusResponses and fails if one is received.
     * @throws Exception
     */
    private void drainQSResponses() throws Exception {
        BlockingConnectionUtils.failIfAnyArrive(testUP, QueryStatusResponse.class);
    }

    private static byte[] myIP() {
        return new byte[] { (byte)127, (byte)0, (byte)0, (byte)1 };
    }
    
    public int getNumberOfPeers() {
        return 3;
    }
    
    @Singleton
    public static class MyActivityCallback extends ActivityCallbackStub {
        
        public List responses = new ArrayList();
        

        public void handleQueryResult(RemoteFileDesc rfdParam,
                                      QueryReply queryReply,
                                      Set locs) {
            responses.add(rfdParam);
        }
    }


}
