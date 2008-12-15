package com.limegroup.gnutella.licenses;

import java.io.File;
import java.io.InterruptedIOException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.limewire.collection.CollectionUtils;
import org.limewire.util.TestUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.limegroup.gnutella.BlockingConnectionUtils;
import com.limegroup.gnutella.ClientSideTestCase;
import com.limegroup.gnutella.ForMeReplyHandler;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.library.FileManagerTestUtils;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.routing.PatchTableMessage;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.routing.ResetTableMessage;
import com.limegroup.gnutella.stubs.NetworkManagerStub;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLDocumentFactory;
import com.limegroup.gnutella.xml.LimeXMLDocumentHelper;

public final class LicenseSharingTest extends ClientSideTestCase {

    private FileManager fileManager;
    private LimeXMLDocumentFactory limeXMLDocumentFactory;
    private Injector injector;
    private QueryRequestFactory queryRequestFactory;
    private NetworkManagerStub networkManagerStub;
    private LimeXMLDocumentHelper limeXMLDocumentHelper;
    
	public LicenseSharingTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(LicenseSharingTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
    }

	@Override
    public int getNumberOfPeers() {
        return 3;
    }
	
	@Override
	public void setUp() throws Exception {
	    networkManagerStub = new NetworkManagerStub();
	    injector = LimeTestUtils.createInjector(Stage.PRODUCTION, new AbstractModule() {
	        @Override
	        protected void configure() {
	            bind(NetworkManager.class).toInstance(networkManagerStub);
	        }
	    });
        super.setUp(injector);
	    fileManager = injector.getInstance(FileManager.class);
	    limeXMLDocumentFactory = injector.getInstance(LimeXMLDocumentFactory.class);
	    queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
	    limeXMLDocumentHelper = injector.getInstance(LimeXMLDocumentHelper.class);

	    FileManagerTestUtils.waitForLoad(fileManager, 4000);
        // get the resource file for com/limegroup/gnutella
        File cc1 = TestUtils.getResourceFile("com/limegroup/gnutella/licenses/ccverifytest0.mp3");
        File cc2 = TestUtils.getResourceFile("com/limegroup/gnutella/licenses/ccverifytest1.mp3");
        File cc3 = TestUtils.getResourceFile("com/limegroup/gnutella/licenses/cc1.mp3");
        File cc4 = TestUtils.getResourceFile("com/limegroup/gnutella/licenses/ccverifytest0.ogg");
        File wma5 = TestUtils.getResourceFile("com/limegroup/gnutella/licenses/weed-PUSA-LoveEverybody.wma");
        assertNotNull(fileManager.getGnutellaFileList().add(cc1).get(5, TimeUnit.SECONDS));
        assertNotNull(fileManager.getGnutellaFileList().add(cc2).get(5, TimeUnit.SECONDS));
        assertNotNull(fileManager.getGnutellaFileList().add(cc3).get(5, TimeUnit.SECONDS));
        assertNotNull(fileManager.getGnutellaFileList().add(cc4).get(5, TimeUnit.SECONDS));
        assertNotNull(fileManager.getGnutellaFileList().add(wma5).get(5, TimeUnit.SECONDS));
        fileManager.getGnutellaFileList().remove(berkeleyFD);
        fileManager.getGnutellaFileList().remove(susheelFD);
	}
	
	public void testFileDescKnowsLicense() throws Exception {
	    List<FileDesc> fds = CollectionUtils.listOf(fileManager.getGnutellaFileList());
	    assertEquals(5, fds.size());
	    for(FileDesc fd : fds )
	        assertTrue(fd.toString(), fd.isLicensed());
    }
    
    public void testQRPExchange() throws Exception {
        assertEquals(5, fileManager.getGnutellaFileList().size());

        for (int i = 0; i < testUP.length; i++) {
            assertTrue("should be open", testUP[i].isOpen());
            assertTrue("should be up -> leaf", testUP[i].getConnectionCapabilities().isSupernodeClientConnection());
            if (i != testUP.length - 1)
                BlockingConnectionUtils.drain(testUP[i], 500);
        }

        final int upIndex = testUP.length - 1;
        QueryRouteTable qrt = new QueryRouteTable();
        assertEquals(0.0, qrt.getPercentFull(), 0);
        // need to wait for QRP table to be sent
        Thread.sleep(15000);
        try {
            Message m = null;
            while (true) {
                m = testUP[upIndex].receive(500);
                if (m instanceof ResetTableMessage)
                    qrt.reset((ResetTableMessage) m);
                else if (m instanceof PatchTableMessage)
                    qrt.patch((PatchTableMessage) m);
            }
        } catch (InterruptedIOException bad) {}

        assertGreaterThan(0, qrt.getPercentFull());

        // send a query that should hit in the qrt
        // Check CC
        String richQuery = "<?xml version=\"1.0\"?><audios><audio licensetype=\"creativecommons.org/licenses/\"/></audios>";
        limeXMLDocumentFactory.createLimeXMLDocument(richQuery); // make sure it can be constructed.
        QueryRequest query = queryRequestFactory.createQuery("", richQuery);
        assertTrue(qrt.contains(query));
        
        // Check Weed
        richQuery = "<?xml version=\"1.0\"?><audios><audio licensetype=\"http://www.shmedlic.com/license/3play.aspx\"/></audios>";
        limeXMLDocumentFactory.createLimeXMLDocument(richQuery); // make sure it can be constructed.
        query = queryRequestFactory.createQuery("", richQuery);
        assertTrue(qrt.contains(query));
    }
    
    
    public void testCCResultsXMLSearch() throws Exception {
        
        setAcceptedIncoming();

        String richQuery = "<?xml version=\"1.0\"?><audios><audio licensetype=\"creativecommons.org/licenses/\"/></audios>";
        limeXMLDocumentFactory.createLimeXMLDocument(richQuery);
        // we should send a query to the leaf and get results.
        Thread.sleep(5 * 1000);
        
        QueryRequest query = queryRequestFactory.createQuery("", richQuery);
        testUP[1].send(query);
        testUP[1].flush();
        
        Thread.sleep(5 * 1000);

        QueryReply reply = BlockingConnectionUtils.getFirstQueryReply(testUP[1]);
        assertNotNull(reply);
        assertEquals(query.getGUID(), reply.getGUID());
        assertEquals(reply.getResultsAsList().toString(), 4, reply.getResultCount());
        
        if(!addXMLToResponses(reply))
            fail("Couldn't add XML to response. :(");
        
        for(Iterator i = reply.getResults(); i.hasNext(); ) {
            Response r = (Response)i.next();
            assertNotNull(r.getDocument());
            LimeXMLDocument doc = r.getDocument();
            assertTrue(r.toString(), doc.isLicenseAvailable());
            assertNotNull(r.toString(), doc.getLicense());
            assertInstanceof(CCLicense.class, doc.getLicense());
        }
    }
    
    public void testWeedResultsXMLSearch() throws Exception {
        setAcceptedIncoming();

        String richQuery = "<?xml version=\"1.0\"?><audios><audio licensetype=\"http://www.shmedlic.com/license/3play.aspx\"/></audios>";
        limeXMLDocumentFactory.createLimeXMLDocument(richQuery);
        // we should send a query to the leaf and get results.
        QueryRequest query = queryRequestFactory.createQuery("", richQuery);
        testUP[1].send(query);
        testUP[1].flush();

        QueryReply reply = BlockingConnectionUtils.getFirstQueryReply(testUP[1]);
        assertNotNull(reply);
        assertEquals(query.getGUID(), reply.getGUID());
        assertEquals(reply.getResultsAsList().toString(), 1, reply.getResultCount());
        
        if(!addXMLToResponses(reply))
            fail("Couldn't add XML to response. :(");
        
        for(Iterator i = reply.getResults(); i.hasNext(); ) {
            Response r = (Response)i.next();
            LimeXMLDocument doc = r.getDocument();
            assertNotNull(doc);
            assertTrue(r.toString(), doc.isLicenseAvailable());
            assertNotNull(r.toString(), doc.getLicense());
            assertInstanceof(WeedLicense.class, doc.getLicense());
        }
    }
	
	/**
	 * Tests whether a search with a license in the query returns results
	 * without a matching license.
	 */
	public void testLicenseRequiredXMLSearch() throws Exception {
		setAcceptedIncoming();
		
		String richQuery = "<?xml version=\"1.0\"?><audios><audio title=\"love\" licensetype=\"http://www.shmedlic.com/license/3play.aspx\"/></audios>";
        limeXMLDocumentFactory.createLimeXMLDocument(richQuery);

        // we should send a query to the leaf and get results.
        QueryRequest query = queryRequestFactory.createQuery("", richQuery);
        testUP[1].send(query);
        testUP[1].flush();

        QueryReply reply = BlockingConnectionUtils.getFirstQueryReply(testUP[1]);
        assertNotNull(reply);
        assertEquals(query.getGUID(), reply.getGUID());
        assertEquals(reply.getResultsAsList().toString(), 1, reply.getResultCount());
        
        if (!addXMLToResponses(reply))
            fail("Couldn't add XML to response. :(");
        
        for (Iterator i = reply.getResults(); i.hasNext(); ) {
            Response r = (Response)i.next();
			LimeXMLDocument doc = r.getDocument();
            assertNotNull(doc);
            assertTrue(r.toString(), doc.isLicenseAvailable());
            assertNotNull(r.toString(), doc.getLicense());
            assertInstanceof(WeedLicense.class, doc.getLicense());
        }
	}
    
	/**
	 * Tests whether a search without a license returns results
	 * with both licenses.
	 */
	public void testLicenselessXMLSearch() throws Exception {
		setAcceptedIncoming();
		
		String richQuery = "<?xml version=\"1.0\"?><audios><audio title=\"love\"/></audios>";
        limeXMLDocumentFactory.createLimeXMLDocument(richQuery);

        // we should send a query to the leaf and get results.
        QueryRequest query = queryRequestFactory.createQuery("", richQuery);
        testUP[1].send(query);
        testUP[1].flush();

        QueryReply reply = BlockingConnectionUtils.getFirstQueryReply(testUP[1]);
        assertNotNull(reply);
        assertEquals(query.getGUID(), reply.getGUID());
        assertEquals(reply.getResultsAsList().toString(), 2, reply.getResultCount());
        
        if (!addXMLToResponses(reply))
            fail("Couldn't add XML to response. :(");
        
        for (Iterator i = reply.getResults(); i.hasNext(); ) {
            Response r = (Response)i.next();
			LimeXMLDocument doc = r.getDocument();
            assertNotNull(doc);
            assertTrue(r.toString(), doc.isLicenseAvailable());
            assertNotNull(r.toString(), doc.getLicense());
        }
	}
    
    private void setAcceptedIncoming() throws Exception {
        networkManagerStub.setAcceptedIncomingConnection(true);
    }
    
    private boolean addXMLToResponses(QueryReply qr) throws Exception {
        return ForMeReplyHandler.addXMLToResponses(qr, limeXMLDocumentHelper);
    }
}
            
