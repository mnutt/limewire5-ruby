package com.limegroup.gnutella;

import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.limewire.core.settings.FilterSettings;
import org.limewire.io.ByteReader;
import org.limewire.io.GUID;
import org.limewire.util.FileUtils;

import junit.framework.Test;

import com.google.inject.Injector;
import com.google.inject.Stage;
import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.library.FileManagerTestUtils;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.routing.RouteTableMessage;

/**
 *  Tests that an Ultrapeer correctly handles all aspects of PushProxy.  For
 *  example:
 *  1) handles the VendorMessage exchange as expected
 *  2) handles HTTP requests as expected, forwarding on a PushRequest
 *
 *  This class tests a lot of different pieces of code.
 *
 *  ULTRAPEER_1  ----  CENTRAL TEST ULTRAPEER  ----  ULTRAPEER_2
 *                              |
 *                              |
 *                              |
 *                             LEAF
 *
 *  The leaf must be connected in the first test.
 */
public final class ServerSideBrowseHostTest extends ServerSideTestCase {

    protected static int TIMEOUT = 2000;
    private FileManager fileManager;
    private QueryRequestFactory queryRequestFactory;
    private MessageFactory messageFactory;

    public ServerSideBrowseHostTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ServerSideBrowseHostTest.class);
    }    
   
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
    @Override
    public int getNumberOfUltrapeers() {
        return 1;
    }

    @Override
    public int getNumberOfLeafpeers() {
        return 1;
    }
	
    @Override
    public void setUpQRPTables() throws Exception {
        // for Ultrapeer 1
        QueryRouteTable qrt = new QueryRouteTable();
        qrt.add("leehsus");
        qrt.add("berkeley");
        for (Iterator iter=qrt.encode(null).iterator(); iter.hasNext(); ) {
            ULTRAPEER[0].send((RouteTableMessage)iter.next());
            assertTrue(ULTRAPEER[0].isOpen());
			ULTRAPEER[0].flush();
        }
    }

    @Override
    protected void setUp() throws Exception {
        Injector injector = LimeTestUtils.createInjector(Stage.PRODUCTION);
        super.setUp(injector);
        fileManager = injector.getInstance(FileManager.class);
        queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
        messageFactory = injector.getInstance(MessageFactory.class);
    }
    
    // BEGIN TESTS
    // ------------------------------------------------------

    public void testResultsIndicateBrowseHostSupport() throws Exception {
        drainAll();

        // make sure leaf is sharing
        assertEquals(2, fileManager.getGnutellaFileList().size());

        // send a query that should be answered
        QueryRequest query = queryRequestFactory.createQueryRequest(GUID.makeGuid(), (byte) 1,
                "berkeley", null, null, null, false, Network.UNKNOWN, false, 0);
        ULTRAPEER[0].send(query);
        ULTRAPEER[0].flush();

        // await a response
        Message m = null;
        do {
            m = ULTRAPEER[0].receive(TIMEOUT);
        } while (!(m instanceof QueryReply)) ;

        // confirm it supports browse host
        QueryReply reply = (QueryReply) m;
        assertTrue(reply.getSupportsBrowseHost());
    }

    public void testHTTPRequest() throws Exception {
        FilterSettings.MAX_RESPONSES_PER_REPLY.setValue(10);
        
        
        FileManager fm = injector.getInstance(FileManager.class);
        FileManagerTestUtils.waitForLoad(fm,2000);
        // make sure more than FilterSettings.MAX_RESPONSES_PER_REPLY files
        // are shared
        for (int i = 0; i < FilterSettings.MAX_RESPONSES_PER_REPLY.getValue() * 2; i++) {
            File f = new File(_scratchDir, "sharedFile"+i+".txt");
            f.deleteOnExit();
            FileUtils.writeObject(f, new Integer(i));
            assertNotNull(fm.getGnutellaFileList().add(f).get(1, TimeUnit.SECONDS));
        }

        assertEquals(2 * FilterSettings.MAX_RESPONSES_PER_REPLY.getValue() + 2, fm.getGnutellaFileList().size());
        
        String result = null;

        Socket s = new Socket("localhost", PORT);
        ByteReader in = new ByteReader(s.getInputStream());
        BufferedWriter out = 
            new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));

        // first test a GET
        out.write("GET /  HTTP/1.1\r\n");
        out.write("Accept: application/x-gnutella-packets\r\n");
        out.write("\r\n");
        out.flush();

        // check opcode
        result = in.readLine();
        assertGreaterThan(result, -1, result.indexOf("200"));

        // get to the replies....
        String currLine = null;
        do {
            currLine = in.readLine();
        } while ((currLine != null) && !currLine.equals(""));

        // 10 in the first
        QueryReply qr = (QueryReply) messageFactory.read(s.getInputStream(), Network.TCP);
        assertEquals(10, qr.getResultCount());
        
        // 10 in the second
        qr = (QueryReply) messageFactory.read(s.getInputStream(), Network.TCP);
        assertEquals(10, qr.getResultCount());
        
        // 2 in the 3rd.
        qr = (QueryReply) messageFactory.read(s.getInputStream(), Network.TCP);
        assertEquals(2, qr.getResultCount());

        assertNull(in.readLine());
        s.close();
        in.close();
    }


    public void testBadHTTPRequest1() throws Exception {
        String result = null;

        Socket s = new Socket("localhost", PORT);
        ByteReader in = new ByteReader(s.getInputStream());
        BufferedWriter out = 
            new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));

        // first test a GET
        out.write("GET /  HTTP/1.1\r\n");
        out.write("\r\n");
        out.flush();

        // check opcode
        result = in.readLine();
        assertGreaterThan(result, -1, result.indexOf("406"));
        s.close();
        in.close();
    }

    
}

