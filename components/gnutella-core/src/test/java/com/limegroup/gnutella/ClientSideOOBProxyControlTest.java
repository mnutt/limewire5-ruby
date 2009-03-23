package com.limegroup.gnutella;

import java.net.InetAddress;

import org.limewire.core.settings.SearchSettings;

import junit.framework.Test;

import com.limegroup.gnutella.connection.BlockingConnection;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessageStubHelper;

/**
 * First ultrapeer supports new out-of-band proxying control protocol.
 * 
 * Methods that are tested by this integration test include:
 * 
 * {@link MessageRouterImpl#originateLeafQuery(QueryRequest)} and
 * {@link ManagedConnection#originateQuery(QueryRequest)}.
 */
public class ClientSideOOBProxyControlTest extends ClientSideTestCase {

    private MessagesSupportedVendorMessage messagesSupportedVendorMessage;
    private QueryRequestFactory queryRequestFactory;
    private MessageRouter messageRouter;

    public ClientSideOOBProxyControlTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ClientSideOOBProxyControlTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        Injector injector = LimeTestUtils.createInjector(Stage.PRODUCTION);
        super.setUp(injector);
        messagesSupportedVendorMessage = injector.getInstance(MessagesSupportedVendorMessage.class);
        queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
        messageRouter = injector.getInstance(MessageRouter.class);
        
        setUpProxyControlSupport();
    }
    
    private void setUpProxyControlSupport() throws Exception {
        drainAll();
        
        // send client a message that we support proxy control for OOB v3
        testUP[0].send(messagesSupportedVendorMessage);
        testUP[0].flush();
        
        // send client a message that we support leaf guidance and everything
        // but the proxy control protocol for OOB v3
        testUP[1].send(MessagesSupportedVendorMessageStubHelper.makeMSVMWithoutOOBProxyControl());
        testUP[1].flush();
        
        Thread.sleep(250);
    }
    
    @Override
    public void setSettings() {
        SearchSettings.DISABLE_OOB_V2.setBoolean(false);
    }
    
    @Override
    public int getNumberOfPeers() {
        return 2;
    }

    public void testQueryV2Disabled() throws Exception {
        
        QueryRequest query = queryRequestFactory.createQuery("txt");
        assertFalse(query.desiresOutOfBandReplies());
        assertFalse(query.doNotProxy());
        
        SearchSettings.DISABLE_OOB_V2.setBoolean(true);
        
        // send query through messagerouter
        messageRouter.sendDynamicQuery(query);
        
        Thread.sleep(250);
        
        // new ultrapeer should not get do_not_proxy
        assertQueryNoOOB(testUP[0], query, false);
        
        // old ultrapper should get do_not_proxy
        assertQueryNoOOB(testUP[1], query, true);
    }
    
    
    public void testOOBQueryV2Disabled()  throws Exception {
        
        QueryRequest query = queryRequestFactory.createOutOfBandQuery("txt", InetAddress.getLocalHost().getAddress(), 1340);
        
        assertTrue(query.desiresOutOfBandReplies());
        assertFalse(query.doNotProxy());
        
        SearchSettings.DISABLE_OOB_V2.setBoolean(true);
        
        // send query through message router
        messageRouter.sendDynamicQuery(query);
        Thread.sleep(250);
        
        // new ultrapeer should not get do_not_proxy
        assertQuery(testUP[0], query, false, true);
        
        // old ultrapper get do_not_proxy 
        assertQuery(testUP[1], query, true, true);
    }
    
    public void testOOBQueryV2Enabled()  throws Exception {

        QueryRequest query = queryRequestFactory.createOutOfBandQuery("txt", InetAddress.getLocalHost().getAddress(), 1340);
        
        assertTrue(query.desiresOutOfBandReplies());
        assertFalse(query.doNotProxy());
        
        SearchSettings.DISABLE_OOB_V2.setBoolean(false);
        
        // send query through message router
        messageRouter.sendDynamicQuery(query);
        Thread.sleep(250);
        
        // new ultrapeer should not get do_not_proxy
        assertQuery(testUP[0], query, false, true);
        
        // old ultrapper should not get do_not_proxy either
        assertQuery(testUP[1], query, false, true);
    }
    
    public void testQueryV2Enabled() throws Exception {
        
        QueryRequest query = queryRequestFactory.createQuery("txt");
        assertFalse(query.desiresOutOfBandReplies());
        assertFalse(query.doNotProxy());
        
        SearchSettings.DISABLE_OOB_V2.setBoolean(false);
        
        // send query through message router
        messageRouter.sendDynamicQuery(query);
        Thread.sleep(250);
        
        // new ultrapeer should not get do_not_proxy
        assertQueryNoOOB(testUP[0], query, false);
        
        // old ultrapper shouldn't get do_not_proxy either
        assertQueryNoOOB(testUP[1], query, false);
    }
    
    private QueryRequest assertQuery(BlockingConnection c, QueryRequest query, boolean doNotProxy,
            boolean desiresOOB) throws BadPacketException {
        QueryRequest qr = BlockingConnectionUtils.getFirstInstanceOfMessageType(c, QueryRequest.class);
        assertNotNull(qr);
        assertEquals(query.getGUID(), qr.getGUID());
        assertEquals(doNotProxy, qr.doNotProxy());
        assertEquals(desiresOOB, qr.desiresOutOfBandReplies());
        return qr;
    }
    
    private QueryRequest assertQueryNoOOB(BlockingConnection c, QueryRequest query, boolean doNotProxy) throws BadPacketException {
        return assertQuery(c, query, doNotProxy, false);
    }

}
