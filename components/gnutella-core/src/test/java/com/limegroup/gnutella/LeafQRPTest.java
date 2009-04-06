package com.limegroup.gnutella;

import java.io.InterruptedIOException;

import org.limewire.gnutella.tests.LimeTestUtils;

import junit.framework.Test;

import com.google.inject.Injector;
import com.google.inject.Stage;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.routing.PatchTableMessage;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.routing.ResetTableMessage;

/**
 * Checks whether (multi)leaves avoid forwarding messages to ultrapeers, do
 * redirects properly, etc.  The test includes a leaf attached to 3 
 * Ultrapeers.
 */
public class LeafQRPTest extends ClientSideTestCase {
    private QueryRequestFactory queryRequestFactory;

    public LeafQRPTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(LeafQRPTest.class);
    }    

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    public int getNumberOfPeers() {
        return 1;
    }

    @Override
    protected void setUp() throws Exception {
        Injector injector = LimeTestUtils.createInjector(Stage.PRODUCTION);
        super.setUp(injector);
        queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
    }
    
    // the only test - make sure the QRP table sent by the leaf is send and is
    // valid.
    public void testQRPExchange() throws Exception {
        // set up the connection
        QueryRouteTable qrt = new QueryRouteTable();
        assertEquals(0.0,qrt.getPercentFull());
        Thread.sleep(15000);
        try {
            Message m = null;
            while (true) {
                m = testUP[0].receive(1000);
                if (m instanceof ResetTableMessage)
                    qrt.reset((ResetTableMessage) m);
                else if (m instanceof PatchTableMessage)
                    qrt.patch((PatchTableMessage) m);
            }
        }
        catch (InterruptedIOException bad) {
            // we are waiting for all messages to be processed
        }

        // send a query that should hit in the qrt
        QueryRequest query = queryRequestFactory.createQuery("berkeley");
        QueryRequest query2 = queryRequestFactory.createQuery("susheel");

        assertTrue("qrt did not contain: " + query, qrt.contains(query));
        assertTrue("qrt did not contain: " + query2, qrt.contains(query2));
   }

}
