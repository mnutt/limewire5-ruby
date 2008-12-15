package com.limegroup.gnutella;

import java.util.List;

import junit.framework.Test;

import org.limewire.core.settings.SearchSettings;
import org.limewire.util.PrivilegedAccessor;

import com.google.inject.Injector;
import com.google.inject.Stage;
import com.limegroup.gnutella.connection.RoutedConnection;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.messages.StaticMessages;
import com.limegroup.gnutella.routing.PatchTableMessage;
import com.limegroup.gnutella.routing.QRPUpdater;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.simpp.SimppListener;
import com.limegroup.gnutella.simpp.SimppManager;

@SuppressWarnings("all")
public class LimeResponsesTest extends ClientSideTestCase {

    private ConnectionManager connectionManager;
    private QueryRequestFactory queryRequestFactory;
    private SimppManager simppManager;
    private StaticMessages staticMessages;
    public LimeResponsesTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(LimeResponsesTest.class);
    }
    
    @Override
    public int getNumberOfPeers() {
        return 1;
    }
    
    @Override
    public void setSettings() throws Exception {
        // TODO change this, by either introducing a setter or overriding FileManagerController
        PrivilegedAccessor.setValue(QRPUpdater.class, "QRP_DELAY", 1000);
        SearchSettings.LIME_QRP_ENTRIES.setValue(new String[]{"badger"});
        SearchSettings.LIME_SEARCH_TERMS.setValue(new String[]{"badger"});
        SearchSettings.SEND_LIME_RESPONSES.setValue(1f);
    }
    
    @Override
    protected void setUp() throws Exception {
		Injector injector = LimeTestUtils.createInjector(Stage.PRODUCTION);
        super.setUp(injector);
		connectionManager = injector.getInstance(ConnectionManager.class);
		queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
		simppManager = injector.getInstance(SimppManager.class);
		staticMessages = injector.getInstance(StaticMessages.class);
    }
    
    public void testResponse() throws Exception {
        QueryRequest qr = queryRequestFactory.createNonFirewalledQuery("badger", (byte)1);
        testUP[0].send(qr);
        testUP[0].flush();
        Thread.sleep(1000);
        QueryReply r = BlockingConnectionUtils.getFirstQueryReply(testUP[0]);
        assertNotNull(r);
        QueryReply expected = staticMessages.getLimeReply();
        assertTrue(expected.getResultsAsList().containsAll(r.getResultsAsList()));
        assertTrue(r.getResultsAsList().containsAll(expected.getResultsAsList()));
        
        // change the words to something else
        SearchSettings.LIME_SEARCH_TERMS.setValue(new String[]{"mushroom"});
        qr = queryRequestFactory.createNonFirewalledQuery("badger", (byte)1);
        testUP[0].send(qr);
        testUP[0].flush();
        Thread.sleep(1000);
        r = BlockingConnectionUtils.getFirstQueryReply(testUP[0]);
        assertNull(r);
        
        qr = queryRequestFactory.createNonFirewalledQuery("mushroom", (byte)1);
        testUP[0].send(qr);
        testUP[0].flush();
        Thread.sleep(1000);
        r = BlockingConnectionUtils.getFirstQueryReply(testUP[0]);
        assertNotNull(r);
        assertTrue(expected.getResultsAsList().containsAll(r.getResultsAsList()));
        assertTrue(r.getResultsAsList().containsAll(expected.getResultsAsList()));
        
        // turn off responding completely
        SearchSettings.SEND_LIME_RESPONSES.setValue(0);
        qr = queryRequestFactory.createNonFirewalledQuery("mushroom", (byte)1);
        testUP[0].send(qr);
        testUP[0].flush();
        Thread.sleep(1000);
        r = BlockingConnectionUtils.getFirstQueryReply(testUP[0]);
        assertNull(r);
    }
    
    public void testQRP() throws Exception {
        RoutedConnection c = connectionManager.getInitializedConnections().get(0);
        c.getRoutedConnectionStatistics().incrementNextQRPForwardTime(0);
        PatchTableMessage ptm = BlockingConnectionUtils.getFirstInstanceOfMessageType(testUP[0], PatchTableMessage.class, 22000);
        assertNotNull(ptm);
        QueryRouteTable qrt = new QueryRouteTable();
        qrt.patch(ptm);
        
        // initially, the qrp words should be included
        assertTrue(qrt.contains(queryRequestFactory.createQuery("badger")));
        
        // change some words, an updated qrp should be sent shortly
        SearchSettings.LIME_QRP_ENTRIES.setValue(new String[]{"mushroom"});
        c.getRoutedConnectionStatistics().incrementNextQRPForwardTime(0);
        triggerSimppUpdate();
        ptm = BlockingConnectionUtils.getFirstInstanceOfMessageType(testUP[0], PatchTableMessage.class, 12000);
        assertNotNull(ptm);
        qrt.patch(ptm);
        
        // the new word should be there, the old one gone.
        assertTrue(qrt.contains(queryRequestFactory.createQuery("mushroom")));
        assertFalse(qrt.contains(queryRequestFactory.createQuery("badger")));
    }
    
    private static int simppVersion;
    private void triggerSimppUpdate() throws Exception {
        List<SimppListener> l = (List<SimppListener>) 
            PrivilegedAccessor.getValue(simppManager, "listeners");
        for (SimppListener s : l)
            s.simppUpdated(simppVersion++);
    }
}
