package com.limegroup.gnutella;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.limewire.core.settings.LibrarySettings;
import org.limewire.io.GUID;
import org.limewire.util.PrivilegedAccessor;
import org.limewire.util.TestUtils;

import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.Stage;
import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.library.FileManagerTestUtils;
import com.limegroup.gnutella.messages.FeatureSearchData;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;

/**
 * Checks whether Meta-Queries are correctly answered, etc.
 */
@SuppressWarnings( { "unchecked", "cast" } )
public class ServerSideMetaQueryTest extends ClientSideTestCase {

    private QueryRequestFactory queryRequestFactory;

    public ServerSideMetaQueryTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ServerSideMetaQueryTest.class);
    }    

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }      
    
    @Override
    public void setSettings() {
        TIMEOUT = 1250;
    }
    

    @Override
    public int getNumberOfPeers() {
        return 3;
    }

    
    @Override
    protected void setUp() throws Exception {
        Injector injector = LimeTestUtils.createInjector(Stage.PRODUCTION, MyCallback.class);
        super.setUp(injector);
        
        queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
        FileManager fm = injector.getInstance(FileManager.class);
        FileManagerTestUtils.waitForLoad(fm, 500);
        LibrarySettings.ALLOW_PROGRAMS.setValue(true);
        assertNotNull(fm.getGnutellaFileList().add(TestUtils.getResourceFile("com/limegroup/gnutella/resources/berkeley.mp3")).get(1, TimeUnit.SECONDS));
        assertNotNull(fm.getGnutellaFileList().add(TestUtils.getResourceFile("com/limegroup/gnutella/resources/meta audio.mp3")).get(1, TimeUnit.SECONDS));
        assertNotNull(fm.getGnutellaFileList().add(TestUtils.getResourceFile("com/limegroup/gnutella/resources/meta video.wmv")).get(1, TimeUnit.SECONDS));
        assertNotNull(fm.getGnutellaFileList().add(TestUtils.getResourceFile("com/limegroup/gnutella/resources/meta doc.txt")).get(1, TimeUnit.SECONDS));
        assertNotNull(fm.getGnutellaFileList().add(TestUtils.getResourceFile("com/limegroup/gnutella/resources/meta image.png")).get(1, TimeUnit.SECONDS));
        assertNotNull(fm.getGnutellaFileList().add(TestUtils.getResourceFile("com/limegroup/gnutella/resources/meta program txt.bin")).get(1, TimeUnit.SECONDS));
    }
    
    @Singleton
    public static class MyCallback extends ActivityCallbackStub {
        public GUID aliveGUID = null;

        public void setGUID(GUID guid) { aliveGUID = guid; }
        public void clearGUID() { aliveGUID = null; }

        @Override
        public boolean isQueryAlive(GUID guid) {
            if (aliveGUID != null)
                return (aliveGUID.equals(guid));
            return false;
        }
    }

    ///////////////////////// Actual Tests ////////////////////////////

    public void testMediaTypeAggregator() throws Exception {
        {
        QueryRequest query = 
            queryRequestFactory.createQueryRequest(GUID.makeGuid(), (byte)3,
                "whatever", "", null, null, true, Network.TCP, false, 0, false, 0);
       
        MediaTypeAggregator.Aggregator filter = MediaTypeAggregator.getAggregator(query);
        assertNull(filter);
        }

        int[] flags = new int[6];
        flags[0] = QueryRequest.AUDIO_MASK;
        flags[1] = QueryRequest.VIDEO_MASK;
        flags[2] = QueryRequest.DOC_MASK;
        flags[3] = QueryRequest.IMAGE_MASK;
        flags[4] = QueryRequest.WIN_PROG_MASK;
        flags[5] = QueryRequest.LIN_PROG_MASK;
        for (int i = 0; i < flags.length; i++) {
            testAggregator(0 | flags[i]);
            for (int j = 0; j < flags.length; j++) {
                if (j == i) continue;
                testAggregator(0 | flags[i] | flags[j]);
                for (int k = 0; k < flags.length; k++) {
                    if (k == j) continue;
                    testAggregator(0 | flags[i] | flags[j] | flags[k]);
                    for (int l = 0; l < flags.length; l++) {
                        if (l == k) continue;
                        testAggregator(0 | flags[i] | flags[j] | flags[k] |
                                       flags[l]);
                        for (int m = 0; m < flags.length; m++) {
                            if (m == l) continue;
                            testAggregator(0 | flags[i] | flags[j] | flags[k] |
                                           flags[l] | flags[m]);
                        }
                    }
                }
            }
        }

    }

    private void testAggregator(int flag) throws Exception {
        QueryRequest query = 
            queryRequestFactory.createQueryRequest(GUID.makeGuid(), (byte)3,
                "whatever", "", null, null, true, Network.TCP, false, 0, false,
                flag);
       
        MediaTypeAggregator.Aggregator filter = MediaTypeAggregator.getAggregator(query);
        assertNotNull(filter);
        List filterList = (List) PrivilegedAccessor.getValue(filter, 
                                                             "_filters");
        int numFilters = 0;
        if ((flag & QueryRequest.AUDIO_MASK) > 0) numFilters++;
        if ((flag & QueryRequest.VIDEO_MASK) > 0) numFilters++;
        if ((flag & QueryRequest.DOC_MASK) > 0) numFilters++;
        if ((flag & QueryRequest.IMAGE_MASK) > 0) numFilters++;
        if ((flag & QueryRequest.WIN_PROG_MASK) > 0) numFilters++;
        if ((flag & QueryRequest.LIN_PROG_MASK) > 0) numFilters++;
        assertEquals(numFilters, filterList.size());
        assertEquals(((flag & QueryRequest.AUDIO_MASK) > 0),
                     filter.allow("susheel.mp3"));
        assertEquals(((flag & QueryRequest.VIDEO_MASK) > 0),
                     filter.allow("susheel.wmv"));
        assertEquals(((flag & QueryRequest.DOC_MASK) > 0),
                     filter.allow("susheel.txt"));
        assertEquals(((flag & QueryRequest.IMAGE_MASK) > 0),
                     filter.allow("susheel.png"));
        assertEquals(((flag & QueryRequest.WIN_PROG_MASK) > 0),
                     filter.allow("susheel.exe"));
        assertEquals(((flag & QueryRequest.LIN_PROG_MASK) > 0),
                     filter.allow("susheel.csh"));
    }

    
    public void testMetaFlagQuery() throws Exception {
        drainAll();
        {
        // first test a normal query with no meta flag info
        QueryRequest query = 
            queryRequestFactory.createQueryRequest(GUID.makeGuid(), (byte)3,
                "berkeley", "", null, null, false, Network.TCP, false, 0, false, 0);
        
        testUP[0].send(query);
        testUP[0].flush();

        Thread.sleep(250);

        // we should get a reply with 2 responses
        QueryReply reply = 
            (QueryReply)BlockingConnectionUtils.getFirstInstanceOfMessageType(testUP[0],
                                                      QueryReply.class);
        assertNotNull(reply);
        List results = reply.getResultsAsList();
        assertEquals(2, results.size());
        }

        {
        // test a query for audio
        QueryRequest query = 
            queryRequestFactory.createQueryRequest(GUID.makeGuid(), (byte)3,
                "berkeley", "", null, null, false, Network.TCP, false, 0, false,
                0 | QueryRequest.AUDIO_MASK);
        
        testUP[1].send(query);
        testUP[1].flush();

        Thread.sleep(250);

        // we should get a reply with 1 response
        QueryReply reply = 
            (QueryReply)BlockingConnectionUtils.getFirstInstanceOfMessageType(testUP[1],
                                                      QueryReply.class);
        assertNotNull(reply);
        List results = reply.getResultsAsList();
        assertEquals(1, results.size());
        Response resp = (Response) results.get(0);
        assertEquals("berkeley.mp3", resp.getName());
        }
        {
        // test a query for documents
        QueryRequest query = 
            queryRequestFactory.createQueryRequest(GUID.makeGuid(), (byte)3,
                "berkeley", "", null, null, false, Network.TCP, false, 0, false,
                0 | QueryRequest.DOC_MASK);
        
        testUP[2].send(query);
        testUP[2].flush();

        Thread.sleep(250);

        // we should get a reply with 1 response
        QueryReply reply = 
            (QueryReply)BlockingConnectionUtils.getFirstInstanceOfMessageType(testUP[2],
                                                      QueryReply.class);
        assertNotNull(reply);
        List results = reply.getResultsAsList();
        assertEquals(1, results.size());
        Response resp = (Response) results.get(0);
        assertEquals("berkeley.txt", resp.getName());
        }
    }

    public void testAdvancedMetaQuery() throws Exception {
        drainAll();
        {
        // first test a normal query with several meta flags
        QueryRequest query = 
            queryRequestFactory.createQueryRequest(GUID.makeGuid(), (byte)3,
                "meta", "", null, null, false, Network.TCP, false, 0, false,
                0 | 
                 QueryRequest.AUDIO_MASK | 
                 QueryRequest.DOC_MASK |
                 QueryRequest.VIDEO_MASK | 
                 QueryRequest.IMAGE_MASK | 
                 QueryRequest.LIN_PROG_MASK);
        
        testUP[0].send(query);
        testUP[0].flush();

        Thread.sleep(250);

        // we should get a reply with 5 responses
        QueryReply reply = 
            (QueryReply)BlockingConnectionUtils.getFirstInstanceOfMessageType(testUP[0],
                                                      QueryReply.class);
        assertNotNull(reply);
        List results = reply.getResultsAsList();
        assertEquals(5, results.size());
        Set names = new HashSet();
        for (Iterator iter = results.iterator(); iter.hasNext(); )
            names.add(((Response)iter.next()).getName());
        assertTrue(names.contains("meta audio.mp3"));
        assertTrue(names.contains("meta video.wmv"));
        assertTrue(names.contains("meta image.png"));
        assertTrue(names.contains("meta doc.txt"));
        assertTrue(names.contains("meta program txt.bin"));
        }

        {
        // first test a normal query with several meta flags
        QueryRequest query = 
            queryRequestFactory.createQueryRequest(GUID.makeGuid(), (byte)3,
                "meta", "", null, null, false, Network.TCP, false, 0, false,
                0 | 
                 QueryRequest.AUDIO_MASK | 
                 QueryRequest.DOC_MASK |
                 QueryRequest.IMAGE_MASK | 
                 QueryRequest.LIN_PROG_MASK);
        
        testUP[1].send(query);
        testUP[1].flush();

        Thread.sleep(250);

        // we should get a reply with 4 responses
        QueryReply reply = 
            (QueryReply)BlockingConnectionUtils.getFirstInstanceOfMessageType(testUP[1],
                                                      QueryReply.class);
        assertNotNull(reply);
        List results = reply.getResultsAsList();
        assertEquals(4, results.size());
        Set names = new HashSet();
        for (Iterator iter = results.iterator(); iter.hasNext(); )
            names.add(((Response)iter.next()).getName());
        assertTrue(names.contains("meta audio.mp3"));
        assertTrue(names.contains("meta image.png"));
        assertTrue(names.contains("meta doc.txt"));
        assertTrue(names.contains("meta program txt.bin"));
        }

        {
        // first test a normal query with several meta flags
        QueryRequest query = 
            queryRequestFactory.createQueryRequest(GUID.makeGuid(), (byte)3,
                "txt", "", null, null, false, Network.TCP, false, 0, false,
                0 | 
                 QueryRequest.DOC_MASK |
                 QueryRequest.IMAGE_MASK);
        
        testUP[2].send(query);
        testUP[2].flush();

        Thread.sleep(250);

        // we should get a reply with 3 responses
        QueryReply reply = 
            (QueryReply)BlockingConnectionUtils.getFirstInstanceOfMessageType(testUP[2],
                                                      QueryReply.class);
        assertNotNull(reply);
        List results = reply.getResultsAsList();
        assertEquals(3, results.size());
        Set names = new HashSet();
        for (Iterator iter = results.iterator(); iter.hasNext(); )
            names.add(((Response)iter.next()).getName());
        assertTrue(names.contains("meta doc.txt"));
        assertTrue(names.contains("berkeley.txt"));
        assertTrue(names.contains("susheel.txt"));
        }

        {
        // first test a normal query with several meta flags
        QueryRequest query = 
            queryRequestFactory.createQueryRequest(GUID.makeGuid(), (byte)3,
                "txt", "", null, null, false, Network.TCP, false, 0, false,
                0 | 
                 QueryRequest.LIN_PROG_MASK |
                 QueryRequest.IMAGE_MASK);
        
        testUP[1].send(query);
        testUP[1].flush();

        Thread.sleep(250);

        // we should get a reply with 1 responses
        QueryReply reply = 
            (QueryReply)BlockingConnectionUtils.getFirstInstanceOfMessageType(testUP[1],
                                                      QueryReply.class);
        assertNotNull(reply);
        List results = reply.getResultsAsList();
        assertEquals(1, results.size());
        Response resp = (Response) results.get(0);
        assertEquals("meta program txt.bin", resp.getName());
        }


    }


    public void testStarvingMetaQuery() throws Exception {
        drainAll();
        {
        // first test a normal query with several meta flags
        QueryRequest query = 
            queryRequestFactory.createQueryRequest(GUID.makeGuid(), (byte)3,
                "susheel", "", null, null, false, Network.TCP, false, 0, false,
                0 | QueryRequest.AUDIO_MASK);
        
        testUP[0].send(query);
        testUP[0].flush();

        Thread.sleep(250);

        // we should get no responses because of the filter
        QueryReply reply = 
            (QueryReply)BlockingConnectionUtils.getFirstInstanceOfMessageType(testUP[0],
                                                      QueryReply.class);
        assertNull(reply);
        }
    }

    public void testWhatIsNewMetaQuery() throws Exception {
        drainAll();
        {
        // first test a normal query with several meta flags
        QueryRequest query = 
            queryRequestFactory.createQueryRequest(GUID.makeGuid(), (byte)3,
                QueryRequest.WHAT_IS_NEW_QUERY_STRING, "", null, null, false, Network.TCP, false,
                FeatureSearchData.WHAT_IS_NEW, false, 0);
        
        testUP[0].send(query);
        testUP[0].flush();

        Thread.sleep(250);

        // we should get no responses because of the filter
        QueryReply reply = 
            (QueryReply)BlockingConnectionUtils.getFirstInstanceOfMessageType(testUP[0],
                                                      QueryReply.class);
        assertNotNull(reply);
        List results = reply.getResultsAsList();
        assertEquals(3, results.size());
        }

        {
        // first test a normal query with several meta flags
        QueryRequest query = 
            queryRequestFactory.createQueryRequest(GUID.makeGuid(), (byte)3,
                QueryRequest.WHAT_IS_NEW_QUERY_STRING, "", null, null, false, Network.TCP, false,
                FeatureSearchData.WHAT_IS_NEW, false, 0 | QueryRequest.AUDIO_MASK);
        
        testUP[1].send(query);
        testUP[1].flush();

        Thread.sleep(250);

        // we should get no responses because of the filter
        QueryReply reply = 
            (QueryReply)BlockingConnectionUtils.getFirstInstanceOfMessageType(testUP[1],
                                                      QueryReply.class);
        assertNotNull(reply);
        List results = reply.getResultsAsList();
        assertEquals(2, results.size());
        Set names = new HashSet();
        for (Iterator iter = results.iterator(); iter.hasNext(); )
            names.add(((Response)iter.next()).getName());
        assertTrue(names.contains("meta audio.mp3"));
        assertTrue(names.contains("berkeley.mp3"));
        }

        {
        // first test a normal query with several meta flags
        QueryRequest query = 
            queryRequestFactory.createQueryRequest(GUID.makeGuid(), (byte)3,
                QueryRequest.WHAT_IS_NEW_QUERY_STRING, "", null, null, false, Network.TCP, false,
                FeatureSearchData.WHAT_IS_NEW, false, 0 | QueryRequest.DOC_MASK);
        
        testUP[2].send(query);
        testUP[2].flush();

        Thread.sleep(250);

        // we should get no responses because of the filter
        QueryReply reply = 
            (QueryReply)BlockingConnectionUtils.getFirstInstanceOfMessageType(testUP[2],
                                                      QueryReply.class);
        assertNotNull(reply);
        List results = reply.getResultsAsList();
        assertEquals(3, results.size());
        Set names = new HashSet();
        for (Iterator iter = results.iterator(); iter.hasNext(); )
            names.add(((Response)iter.next()).getName());
        assertTrue(names.contains("berkeley.txt"));
        assertTrue(names.contains("susheel.txt"));
        assertTrue(names.contains("meta doc.txt"));
        }

        {
        // first test a normal query with several meta flags
        QueryRequest query = 
            queryRequestFactory.createQueryRequest(GUID.makeGuid(), (byte)3,
                QueryRequest.WHAT_IS_NEW_QUERY_STRING, "", null, null, false, Network.TCP, false,
                FeatureSearchData.WHAT_IS_NEW, false, 0 | QueryRequest.LIN_PROG_MASK);
        
        testUP[1].send(query);
        testUP[1].flush();

        Thread.sleep(250);

        // we should get no responses because of the filter
        QueryReply reply = 
            (QueryReply)BlockingConnectionUtils.getFirstInstanceOfMessageType(testUP[1],
                                                      QueryReply.class);
        assertNotNull(reply);
        List results = reply.getResultsAsList();
        assertEquals(1, results.size());
        Set names = new HashSet();
        for (Iterator iter = results.iterator(); iter.hasNext(); )
            names.add(((Response)iter.next()).getName());
        assertTrue(names.contains("meta program txt.bin"));
        }


    }


}
