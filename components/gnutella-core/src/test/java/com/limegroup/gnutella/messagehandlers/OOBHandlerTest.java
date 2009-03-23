package com.limegroup.gnutella.messagehandlers;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Random;
import java.util.Set;

import junit.framework.Test;

import org.limewire.core.settings.MessageSettings;
import org.limewire.core.settings.SearchSettings;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.security.AddressSecurityToken;
import org.limewire.security.InvalidSecurityTokenException;
import org.limewire.security.MACCalculatorRepositoryManager;
import org.limewire.security.SecurityToken;
import org.limewire.util.BaseTestCase;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;

import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.MessageListener;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.ResponseFactory;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.connection.RoutedConnection;
import com.limegroup.gnutella.guess.GUESSEndpoint;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryReplyFactory;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.vendor.InspectionRequest;
import com.limegroup.gnutella.messages.vendor.LimeACKVendorMessage;
import com.limegroup.gnutella.messages.vendor.ReplyNumberVendorMessage;
import com.limegroup.gnutella.messages.vendor.ReplyNumberVendorMessageFactory;
import com.limegroup.gnutella.messages.vendor.ReplyNumberVendorMessageFactoryImpl;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.stubs.NetworkManagerStub;
import com.limegroup.gnutella.stubs.ReplyHandlerStub;

public class OOBHandlerTest extends BaseTestCase {

    public OOBHandlerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(OOBHandlerTest.class);
    }

    private Random random = new Random();
    private MyMessageRouter router;
    private OOBHandler handler;
    private GUID g;
    private InetAddress address;
    private MyReplyHandler replyHandler;
    private ReplyNumberVendorMessageFactory factory;
    private MACCalculatorRepositoryManager macManager;
    private volatile boolean canReceiveUnsolicited;
    
    ResponseFactory responseFactory;
    QueryReplyFactory queryReplyFactory;
    
    @Override
    public void setUp() throws Exception {
        router = new MyMessageRouter();
        router.start();
        macManager = new MACCalculatorRepositoryManager();
        g = new GUID(GUID.makeGuid());
        address = InetAddress.getByName("1.2.3.4");
        replyHandler = new MyReplyHandler(address, 1);
        canReceiveUnsolicited = false;
        factory = new ReplyNumberVendorMessageFactoryImpl(new NetworkManagerStub() {
            @Override
            public boolean canReceiveUnsolicited() {
                return canReceiveUnsolicited;
            }
        });
        
        Module module = new AbstractModule() {
            @Override
            protected void configure() {
                bind(ReplyNumberVendorMessageFactory.class).toInstance(factory);
                bind(MessageRouter.class).toInstance(router);
                bind(MACCalculatorRepositoryManager.class).toInstance(macManager);
            }
        };
        
        Injector injector = LimeTestUtils.createInjector(module);
        responseFactory = injector.getInstance(ResponseFactory.class);
        queryReplyFactory = injector.getInstance(QueryReplyFactory.class);
        handler = injector.getInstance(OOBHandler.class);
    }

    public void testRedundantAcks() throws Exception {
        MessageSettings.OOB_REDUNDANCY.setValue(true);
        // a host claims to have 10 results
        ReplyNumberVendorMessage rnvm = factory.create(g, 10);
        router.numToRequest = 10;
        handler.handleMessage(rnvm, null, replyHandler);
        
        assertNotNull(replyHandler.m);
        assertTrue(replyHandler.m instanceof LimeACKVendorMessage);
        LimeACKVendorMessage ack = (LimeACKVendorMessage) replyHandler.m;
        assertEquals(10, ack.getNumResults());
        assertNotNull(ack.getSecurityToken());
        
        // 100 ms later a second ack should be sent
        replyHandler.m = null;
        Thread.sleep(120);
        assertSame(ack, replyHandler.m);
    }
    
    public void testNoRedundantAcks() throws Exception {
        MessageSettings.OOB_REDUNDANCY.setValue(false);
        // a host claims to have 10 results
        ReplyNumberVendorMessage rnvm = factory.create(g, 10);
        router.numToRequest = 10;
        handler.handleMessage(rnvm, null, replyHandler);
        
        assertNotNull(replyHandler.m);
        assertTrue(replyHandler.m instanceof LimeACKVendorMessage);
        LimeACKVendorMessage ack = (LimeACKVendorMessage) replyHandler.m;
        assertEquals(10, ack.getNumResults());
        assertNotNull(ack.getSecurityToken());
        
        // no more acks
        replyHandler.m = null;
        Thread.sleep(120);
        assertNull(replyHandler.m);
    }
    
    public void testDuplicateRNVMs() throws Exception {
        MessageSettings.OOB_REDUNDANCY.setValue(false);
        
        // a host claims to have 10 results
        ReplyNumberVendorMessage rnvm = factory.create(g, 10);
        router.numToRequest = 10;
        handler.handleMessage(rnvm, null, replyHandler);
        
        assertNotNull(replyHandler.m);
        assertTrue(replyHandler.m instanceof LimeACKVendorMessage);
        LimeACKVendorMessage ack = (LimeACKVendorMessage) replyHandler.m;
        assertEquals(10, ack.getNumResults());
        assertNotNull(ack.getSecurityToken());
        
        replyHandler.m = null;
        
        // a duplicate RNVM arrives
        handler.handleMessage(rnvm, null, replyHandler);
        
        // but we don't send an ack
        assertNull(replyHandler.m);
        
        // ever
        replyHandler.m = null;
        Thread.sleep(50);
        handler.handleMessage(rnvm, null, replyHandler);
        assertNull(replyHandler.m);
        Thread.sleep(50);
        handler.handleMessage(rnvm, null, replyHandler);
        assertNull(replyHandler.m);
        
    }
    /**
     * tests that duplicate RNVMs will send only the correct # of acks
     */
    public void testDuplicateRNVMsRedundantACKs() throws Exception {
        MessageSettings.OOB_REDUNDANCY.setValue(true);
        
        // a host claims to have 10 results
        ReplyNumberVendorMessage rnvm = factory.create(g, 10);
        router.numToRequest = 10;
        handler.handleMessage(rnvm, null, replyHandler);
        
        assertNotNull(replyHandler.m);
        assertTrue(replyHandler.m instanceof LimeACKVendorMessage);
        LimeACKVendorMessage ack = (LimeACKVendorMessage) replyHandler.m;
        assertEquals(10, ack.getNumResults());
        assertNotNull(ack.getSecurityToken());
        
        replyHandler.m = null;
        
        // a duplicate RNVM arrives
        handler.handleMessage(rnvm, null, replyHandler);
        
        // but we don't send an ack
        assertNull(replyHandler.m);
        
        // a little later another duplicate RNVM arrives
        // we don't ack
        Thread.sleep(50);
        handler.handleMessage(rnvm, null, replyHandler);
        assertNull(replyHandler.m);
        
        // after a while the redundancy interval hits and we send the
        // redundant ACK
        Thread.sleep(70);
        assertSame(ack, replyHandler.m);
        
        // any further RNVMs do nothing
        replyHandler.m = null;
        Thread.sleep(50);
        handler.handleMessage(rnvm, null, replyHandler);
        assertNull(replyHandler.m);
        Thread.sleep(50);
        handler.handleMessage(rnvm, null, replyHandler);
        assertNull(replyHandler.m);
    }
    
    public void testDropsLargeResponses() throws Exception {

        // a host claims to have 10 results
        ReplyNumberVendorMessage rnvm = factory.create(g, 10);

        // and we request all of them
        router.numToRequest = 10;
        handler.handleMessage(rnvm, null, replyHandler);
        SecurityToken token = assertACKSent(replyHandler, 10);

        // first send back only 8 results - those should be accepted
        QueryReply reply = getReplyWithResults(g.bytes(), 8, address
                .getAddress(), token);
        handler.handleMessage(reply, null, replyHandler);
        assertSame(reply, router.reply);

        // then send back 4 more results - those should not be accepted.
        router.reply = null;
        reply = getReplyWithResults(g.bytes(), 4, address.getAddress(), token);
        handler.handleMessage(reply, null, replyHandler);
        assertNull(router.reply);
    }

    public void testMessagesWithoutEchoedTokenAreDiscardedForDisabledOOBV2() {
        SearchSettings.DISABLE_OOB_V2.setBoolean(true);
        testMessagesWithoutEchoedTokenAreHandled(true);
    }
    
    public void testMessagesWithoutEchoedTokenAreNotDiscardedForEnabledOOBV2() {
        SearchSettings.DISABLE_OOB_V2.setBoolean(false);
        testMessagesWithoutEchoedTokenAreHandled(false);
    }
    
    public void testMessagesWithoutEchoedTokenAreHandled(boolean disableOOBV2) {
        // a host claims to have 10 results
        ReplyNumberVendorMessage rnvm = factory.create(g, 10);

        // and we request all of them
        router.numToRequest = 10;
        handler.handleMessage(rnvm, null, replyHandler);
        assertACKSent(replyHandler, 10);

        // first send back only 10 results without token - those should be
        // discarded
        QueryReply reply = getReplyWithResults(g.bytes(), 10, address
                .getAddress(), null);
        router.reply = null;
        handler.handleMessage(reply, null, replyHandler);
        if (disableOOBV2) {
            assertNotSame(reply, router.reply);
        }
        else {
            assertSame(reply, router.reply);
        }
    }

    public void testMessagesWithInvalidTokenAreDiscardedForDisabledOOBV2() throws Exception {
        SearchSettings.DISABLE_OOB_V2.setBoolean(true);
        testMessagesWithDifferentTokenAreHandled(true);
    }
    
    public void testMessagesWithInvalidTokenAreDiscardedForEnabledOOBV2() throws Exception {
        SearchSettings.DISABLE_OOB_V2.setBoolean(false);
        testMessagesWithDifferentTokenAreHandled(false);
    }
    
    public void testMessagesWithDifferentTokenAreHandled(boolean disableOOBV2)
            throws InvalidSecurityTokenException {
        // a host claims to have 10 results
        ReplyNumberVendorMessage rnvm = factory.create(g, 10);

        // and we request all of them
        router.numToRequest = 10;
        handler.handleMessage(rnvm, null, replyHandler);
        assertACKSent(replyHandler, 10);

        byte[] bytes = new byte[8];
        random.nextBytes(bytes);
        SecurityToken tokenFake = new AddressSecurityToken(bytes, macManager);

        // send back messages with fake token
        QueryReply reply = getReplyWithResults(g.bytes(), 10, address
                .getAddress(), tokenFake);
        
        router.reply = null;
        handler.handleMessage(reply, null, replyHandler);
        assertNull(router.reply);
    }

    
    public void testDropsUnAckedForDisabledOOBV2() throws Exception {
        SearchSettings.DISABLE_OOB_V2.setBoolean(true);
        testHandlesUnAcked(true);
    }
    
    public void testAcceptsUnAckedForEnabledOOBV2() throws Exception {
        SearchSettings.DISABLE_OOB_V2.setBoolean(false);
        testHandlesUnAcked(false);
    }

    public void testHandlesUnAcked(boolean disableOOBV2) throws Exception {

        // send some results w/o an RNVM
        QueryReply reply = getReplyWithResults(g.bytes(), 10, address
                .getAddress(), null);
        
        router.reply = null;
        handler.handleMessage(reply, null, replyHandler);

        if (disableOOBV2) {
            // should be ignored.
            assertNull(router.reply);
        }
        else {
            assertSame(reply, router.reply);
        }
            
        // send an RNVM now
        ReplyNumberVendorMessage rnvm = factory.create(g, 10);
        router.numToRequest = 10;
        handler.handleMessage(rnvm, null, replyHandler);

        // double check we sent back an ack
        SecurityToken token = assertACKSent(replyHandler, 10);

        // and resend the almost same results
        reply = getReplyWithResults(g.bytes(), 5, address.getAddress(), token);
        handler.handleMessage(reply, null, replyHandler);

        // they should be forwarded to the router now
        assertSame(reply, router.reply);
    }

    public void testResultsIgnoredWhenSessionIsFull() throws Exception {
        // a host claims to have 10 results
        ReplyNumberVendorMessage rnvm = factory.create(g, 10);
        router.numToRequest = 10;
        handler.handleMessage(rnvm, null, replyHandler);

        // double check we sent back an ack
        SecurityToken token = assertACKSent(replyHandler, 10);

        // send some results
        router.reply = null;
        QueryReply reply = getReplyWithResults(g.bytes(), 5,
                address.getAddress(), token);
        handler.handleMessage(reply, null, replyHandler);
        assertNotNull(router.reply);

        // and some more
        router.reply = null;
        reply = getReplyWithResults(g.bytes(), 5,
                address.getAddress(), token, 5);
        handler.handleMessage(reply, null, replyHandler);
        assertNotNull(router.reply);

        // further results should be ignored since the session is full
        router.reply = null;
        reply = getReplyWithResults(g.bytes(), 5,
                address.getAddress(), token, 10);
        handler.handleMessage(reply, null, replyHandler);
        assertNull(router.reply);
    }
    
    public void testResultsIgnoredWhenQueryIsDead() throws Exception {
        // a host claims to have 10 results
        ReplyNumberVendorMessage rnvm = factory.create(g, 10);
        router.numToRequest = 10;
        handler.handleMessage(rnvm, null, replyHandler);

        // double check we sent back an ack
        SecurityToken token = assertACKSent(replyHandler, 10);

        // send some results
        router.reply = null;
        QueryReply reply = getReplyWithResults(g.bytes(), 5,
                address.getAddress(), token);
        handler.handleMessage(reply, null, replyHandler);
        assertNotNull(router.reply);

        // eventually the query dies
        router.isAlive = false;
        
        // further results should be ignored since the query is dead
        router.reply = null;
        reply = getReplyWithResults(g.bytes(), 5,
                address.getAddress(), token, 5);
        handler.handleMessage(reply, null, replyHandler);
        assertNull(router.reply);
    }

    public void testQueryIsAliveOverridesSessionTimeout()
            throws InterruptedException {
        router.isAlive = true;
        router.timeToExpire = 50;
        router.reply = null;

        // send reply number message
        ReplyNumberVendorMessage rnvm = factory.create(g, 10);
        router.numToRequest = 10;
        handler.handleMessage(rnvm, null, replyHandler);

        // double check we sent back an ack
        SecurityToken token = assertACKSent(replyHandler, 10);

        router.reply = null;
        QueryReply reply = getReplyWithResults(g.bytes(), 8, address
                .getAddress(), token);
        handler.handleMessage(reply, null, replyHandler);
        assertSame(reply, router.reply);

        router.reply = null;
        reply = getReplyWithResults(g.bytes(), 4, address.getAddress(), token,
                8);
        handler.handleMessage(reply, null, replyHandler);
        assertNull(router.reply);

        // run timeout, but should be still discarded, since query is alive
        Thread.sleep(100);
        handler.run();

        handler.handleMessage(reply, null, replyHandler);
        assertNull(router.reply);
    }

    public void testAddsRNVMs() throws Exception {

        // a host claims to have 10 results at first
        ReplyNumberVendorMessage first = factory.create(g, 10);
        // and then 20 afterwards
        ReplyNumberVendorMessage second = factory.create(g, 10);

        // we would like to get 10
        router.numToRequest = 10;

        // first we receive the message saying they have 10
        handler.handleMessage(first, null, replyHandler);

        // we sould respond we want them
        SecurityToken token1 = assertACKSent(replyHandler, 10);

        // then we receive the second rnvm and want only 15/20
        replyHandler.m = null;
        router.numToRequest = 5;
        handler.handleMessage(second, null, replyHandler);
        SecurityToken token2 = assertACKSent(replyHandler, 5);

        // if we get back a reply with 25 results we should accept it.
        QueryReply reply = getReplyWithResults(g.bytes(), 10, address
                .getAddress(), token1);
        handler.handleMessage(reply, null, replyHandler);
        assertSame(reply, router.reply);

        reply = getReplyWithResults(g.bytes(), 5, address.getAddress(), token2,
                10);
        handler.handleMessage(reply, null, replyHandler);
        assertSame(reply, router.reply);
    }

    public void testDiscardedWithoutAliveQuery() throws InterruptedException {
        // a host claims to have 10 results
        ReplyNumberVendorMessage rnvm = factory.create(g, 10);
        router.numToRequest = 10;
        handler.handleMessage(rnvm, null, replyHandler);

        // double check we sent back an ack
        SecurityToken token = assertACKSent(replyHandler, 10);

        // send reply, only then session objects are created
        QueryReply reply = getReplyWithResults(g.bytes(), 5, address
                .getAddress(), token);
        handler.handleMessage(reply, null, replyHandler);
        assertNotNull(router.reply);
        router.reply = null;

        try {
            router.isAlive = false;

            handler.handleMessage(reply, null, replyHandler);

            // should be ignored.
            assertNull(router.reply);
        } finally {
            router.isAlive = true;
        }

        // send 5 other ones, marked by new offset again, now that search is
        // alive again
        reply = getReplyWithResults(g.bytes(), 5, address.getAddress(), token,
                5);
        handler.handleMessage(reply, null, replyHandler);
        assertSame(reply, router.reply);

        // and now send the old ones again after expiration that does nothing
        handler.run();

        router.reply = null;
        handler.handleMessage(reply, null, replyHandler);
        assertNull(router.reply);
    }

    public void testDuplicatePacketsIgnored() {
        // a host claims to have 10 results
        ReplyNumberVendorMessage rnvm = factory.create(g, 10);
        router.numToRequest = 10;
        handler.handleMessage(rnvm, null, replyHandler);

        // double check we sent back an ack
        SecurityToken token = assertACKSent(replyHandler, 10);

        // send reply, only then session objects are created
        QueryReply reply = getReplyWithResults(g.bytes(), 5, address
                .getAddress(), token);
        handler.handleMessage(reply, null, replyHandler);
        assertNotNull(router.reply);

        router.reply = null;

        // send same five and message should be discarded
        handler.handleMessage(reply, null, replyHandler);
        assertNull(router.reply);
    }

    public void testStoresBypassedResultsNoRouteBack() throws Exception {
        // set this for this test
        canReceiveUnsolicited = true;
        
        // 1. case there's no route back
        GUID guid = new GUID();
        ReplyNumberVendorMessage rnvm = factory.create(guid, 10);
        assertTrue(rnvm.canReceiveUnsolicited());
        router.numToRequest = 10;
        router.isAlive = false;
        router.reply = null;
        
        handler.handleMessage(rnvm, null, replyHandler);
        
        assertNull(router.reply);
        assertSame(rnvm, router.bypassedReply);
    }

    
    public void testStoresBypassedResultsEnoughResultsReceived() throws Exception {
        // set this for this test
        canReceiveUnsolicited = true;
        
        // 2. case we received enough results, i.e, it's more than 150 results
        GUID guid = new GUID();
        ReplyNumberVendorMessage rnvm = factory.create(guid, 10);
        assertTrue(rnvm.canReceiveUnsolicited());
        router.numToRequest = -1; // signals enough results have been received
        router.isAlive = true;
        router.reply = null;
        
        handler.handleMessage(rnvm, null, replyHandler);
        
        assertNull(router.reply);
        assertSame(rnvm, router.bypassedReply);
    }
    
    public void testStoresBypassedResultsFirstQueryReplyButQueryDead() throws Exception {
        // set this for this test
        canReceiveUnsolicited = true;

        // 3. case the first query reply comes in but the query is not alive
        GUID guid = new GUID();
        ReplyNumberVendorMessage rnvm = factory.create(guid, 10);
        router.numToRequest = 10;
        router.isAlive = true;
        router.reply = null;
        
        handler.handleMessage(rnvm, null, replyHandler);
        // source is not remembered yet
        assertNull(router.bypassedReply);
        
        // double check we sent back an ack
        SecurityToken token = assertACKSent(replyHandler, 10);

        // send reply, only then session objects are created
        QueryReply reply = getReplyWithResults(guid.bytes(), 10, address
                .getAddress(), token);
        
        // timeout query
        router.isAlive = false;

        try {
            // send same five and message should be discarded
            handler.handleMessage(reply, null, replyHandler);
            assertNull(router.reply);
            assertSame(reply, router.bypassedQueryReply);
        }
        finally {
            router.isAlive = true;
        }
    }
    
    /**
     * Tests if the ip address of a query reply is overwritten with the address
     * from the reply handler if they don't match.
     */
    public void testMismatchingPublicAddressIsOverwritten() throws Exception {
        // a host claims to have 10 results
        ReplyNumberVendorMessage rnvm = factory.create(g, 10);
        router.numToRequest = 10;
        handler.handleMessage(rnvm, null, replyHandler);

        // double check we sent back an ack
        SecurityToken token = assertACKSent(replyHandler, 10);

        // use different non-private address in reply
        QueryReply reply = getReplyWithResults(g.bytes(), 10, new byte[] { (byte)129, (byte) 168, 0, 5}, token);
        assertNotEquals(replyHandler.getInetAddress().getAddress(), reply.getIPBytes());
        assertFalse(reply.getNeedsPush());
        
        router.reply = null;
        handler.handleMessage(reply, null, replyHandler);
        assertNotNull(router.reply);
     
        QueryReply handled = router.reply;
        assertEquals(replyHandler.getInetAddress().getAddress(), handled.getIPBytes());
    }


    /**
     * Test case where ip is private, but needs push.
     */
    public void testMismatchingPrivateAddressIsOverwrittenForNeedsPush() throws Exception {
        router.numToRequest = 10;
        handler.handleMessage(factory.create(g, 10), null, replyHandler);
        
        SecurityToken token = assertACKSent(replyHandler, 10);
        
        QueryReply reply = getReplyWithResults(g.bytes(), 10, new byte[] { (byte)192, (byte) 168, 0, 5}, token, true);
        assertNotEquals(replyHandler.getInetAddress().getAddress(), reply.getIPBytes());
        assertTrue(reply.getNeedsPush());
        
        router.reply = null;
        handler.handleMessage(reply, null, replyHandler);
        assertNotNull(router.reply);

        QueryReply handled = router.reply;
        assertEquals(replyHandler.getInetAddress().getAddress(), handled.getIPBytes());
    }

    /**
     * Case were ip is private and no need for push indicated, should not change address.
     */
    public void testMismatchingPrivateAddressIsNotOverwrittenForNoPush() throws Exception {
        router.numToRequest = 10;
        handler.handleMessage(factory.create(g, 10), null, replyHandler);
        
        SecurityToken token = assertACKSent(replyHandler, 10);
        
        QueryReply reply = getReplyWithResults(g.bytes(), 10, new byte[] { (byte)192, (byte) 168, 0, 5}, token, false);
        assertNotEquals(replyHandler.getInetAddress().getAddress(), reply.getIPBytes());
        assertFalse(reply.getNeedsPush());

        router.reply = null;
        handler.handleMessage(reply, null, replyHandler);
        assertNotNull(router.reply);

        QueryReply handled = router.reply;
        assertNotEquals(replyHandler.getInetAddress().getAddress(), handled.getIPBytes());
        assertEquals(new byte[] { (byte)192, (byte) 168, 0, 5 }, handled.getIPBytes());
    }

    /**
     *  Tests that the mismatching address of v2 replies is overwritten. 
     */
    public void testOOBv2MismatchingPublicAddressIsOverwritten() throws Exception {
        ReplyNumberVendorMessage rnvm = factory.createV2ReplyNumberVendorMessage(g, 10);
        router.numToRequest = 10;
        handler.handleMessage(rnvm, null, replyHandler);
        
        QueryReply reply = getReplyWithResults(g.bytes(), 10, new byte[] { (byte)129, (byte) 168, 0, 5}, null);
        assertNotEquals(replyHandler.getInetAddress().getAddress(), reply.getIPBytes());
        
        router.reply = null;
        handler.handleMessage(reply, null, replyHandler);
        assertNotNull(router.reply);

        QueryReply handled = router.reply;
        assertEquals(replyHandler.getInetAddress().getAddress(), handled.getIPBytes());
    }
    
    /**
     * Tests that if two RNVMs are received from the same address but
     * different ports, the address is ignored 
     */
    public void testMultiplePortsCauseAddressToBeIgnored() throws Exception {
        InetAddress addr1 = InetAddress.getByName("1.2.3.4");
        InetAddress addr2 = InetAddress.getByName("2.3.4.5");
        int port1 = 1234, port2 = 2345;
        
        // RNVM from addr1:port1 should be acked
        ReplyNumberVendorMessage rnvm = factory.create(g, 10);
        replyHandler = new MyReplyHandler(addr1, port1);
        router.numToRequest = 10;
        handler.handleMessage(rnvm, null, replyHandler);
        assertACKSent(replyHandler, 10);
        
        // RNVM from addr1:port2 should be ignored
        rnvm = factory.create(g, 10);
        replyHandler = new MyReplyHandler(addr1, port2);
        router.numToRequest = 10;
        handler.handleMessage(rnvm, null, replyHandler);
        assertNull(replyHandler.m);
        
        // RNVM from addr1:port1 should now be ignored as well
        rnvm = factory.create(g, 10);
        replyHandler = new MyReplyHandler(addr1, port1);
        router.numToRequest = 10;
        handler.handleMessage(rnvm, null, replyHandler);
        assertNull(replyHandler.m);
        
        // RNVM from addr2:port2 should be acked
        rnvm = factory.create(g, 10);
        replyHandler = new MyReplyHandler(addr2, port2);
        router.numToRequest = 10;
        handler.handleMessage(rnvm, null, replyHandler);
        assertACKSent(replyHandler, 10);
    }
    
    /**
     * Tests that if too many results are received from an address,
     * subsequent RNVMs from that address are ignored
     */
    public void testTooManyResultsCauseAddressToBeIgnored() throws Exception {
        InetAddress addr1 = InetAddress.getByName("1.2.3.4");
        InetAddress addr2 = InetAddress.getByName("2.3.4.5");
        int port1 = 1234, port2 = 2345;
        MyReplyHandler replyHandler1 = new MyReplyHandler(addr1, port1);
        MyReplyHandler replyHandler2 = new MyReplyHandler(addr2, port2);
        
        // RNVM from addr1 should be acked
        ReplyNumberVendorMessage rnvm = factory.create(g, 10);
        router.numToRequest = 10;
        handler.handleMessage(rnvm, null, replyHandler1);
        SecurityToken token1 = assertACKSent(replyHandler1, 10);
        
        // RNVM from addr2 should be acked
        rnvm = factory.create(g, 10);
        router.numToRequest = 10;
        handler.handleMessage(rnvm, null, replyHandler2);
        SecurityToken token2 = assertACKSent(replyHandler2, 10);
        
        // Replies from addr1 should be routed
        QueryReply reply = getReplyWithResults(g.bytes(), 10, addr1.getAddress(), token1);
        router.reply = null;
        handler.handleMessage(reply, null, replyHandler1);
        assertNotNull(router.reply);
        
        // Replies from addr2 should be routed
        reply = getReplyWithResults(g.bytes(), 10, addr2.getAddress(), token2);
        router.reply = null;
        handler.handleMessage(reply, null, replyHandler2);
        assertNotNull(router.reply);
        
        // Too many replies from addr2 should be ignored
        reply = getReplyWithResults(g.bytes(), 1, addr2.getAddress(), token2, 10);
        router.reply = null;
        handler.handleMessage(reply, null, replyHandler2);
        assertNull(router.reply);
        
        // New query
        g = new GUID(GUID.makeGuid());
        replyHandler1.m = null;
        replyHandler2.m = null;
        
        // RNVM from addr1 should be acked
        rnvm = factory.create(g, 10);
        router.numToRequest = 10;
        handler.handleMessage(rnvm, null, replyHandler1);
        token1 = assertACKSent(replyHandler1, 10);
        
        // RNVM from addr2 should be ignored
        rnvm = factory.create(g, 10);
        router.numToRequest = 10;
        handler.handleMessage(rnvm, null, replyHandler2);
        assertNull(replyHandler2.m);
        
        // Replies from addr1 should be routed
        reply = getReplyWithResults(g.bytes(), 10, addr1.getAddress(), token1);
        router.reply = null;
        handler.handleMessage(reply, null, replyHandler1);
        assertNotNull(router.reply);
    }
    
    private static SecurityToken assertACKSent(MyReplyHandler rhandler,
            int numExpected) {
        assertNotNull(rhandler.m);
        assertTrue(rhandler.m instanceof LimeACKVendorMessage);
        LimeACKVendorMessage ack = (LimeACKVendorMessage) rhandler.m;
        assertEquals(numExpected, ack.getNumResults());
        assertNotNull(ack.getSecurityToken());
        return ack.getSecurityToken();
    }

    private QueryReply getReplyWithResults(byte[] guid, int numResults,
            byte[] addr, SecurityToken token, boolean needsPush) {
        return getReplyWithResults(guid, numResults, addr, token, 0, needsPush);
    }
    
    private QueryReply getReplyWithResults(byte[] guid, int numResults,
            byte[] addr, SecurityToken token) {
        return getReplyWithResults(guid, numResults, addr, token, 0, false);
    }

    private QueryReply getReplyWithResults(byte[] guid, int numResults,
            byte[] addr, SecurityToken token, int offset) {
       return getReplyWithResults(guid, numResults, addr, token, offset, false);
    }
    
    private QueryReply getReplyWithResults(byte[] guid, int numResults,
            byte[] addr, SecurityToken token, int offset, boolean needsPush) {
        Response[] res = new Response[numResults];
        byte[] randomBytes = new byte[20];
        try {
            for(int i = 0; i < res.length; i++) {
                random.nextBytes(randomBytes);
                URN urn = URN.createSHA1UrnFromBytes(randomBytes);
                res[i] = responseFactory.createResponse(10, 10,
                        "susheel" + i + offset, urn);
            }
        } catch(IOException misused) {}

        return queryReplyFactory.createQueryReply(guid, (byte) 1, 1,
                addr, 0, res, GUID.makeGuid(), new byte[0], needsPush, false,
                true, true, false, false, true, null, token);
    }

    private static class MyMessageRouter implements MessageRouter {
        volatile QueryReply reply;
        volatile int numToRequest;
        volatile long timeToExpire;
        volatile boolean isAlive = true;

        private ReplyNumberVendorMessage bypassedReply;
        private QueryReply bypassedQueryReply;

        public int getNumOOBToRequest(ReplyNumberVendorMessage reply) {
            return numToRequest;
        }

        public long getOOBExpireTime() {
            return timeToExpire;
        }

        public void handleQueryReply(QueryReply queryReply, ReplyHandler handler) {
            this.reply = queryReply;
        }

        public boolean isQueryAlive(GUID guid) {
            return isAlive;
        }

        public boolean addBypassedSource(ReplyNumberVendorMessage reply,
                ReplyHandler handler) {
            bypassedReply = reply;  
            return true;
        }

        public boolean addBypassedSource(QueryReply reply, ReplyHandler handler) {
            bypassedQueryReply = reply;
            return true;
        }

        public void addMessageHandler(Class<? extends Message> clazz,
                MessageHandler handler) {
        }

        public void addMulticastMessageHandler(Class<? extends Message> clazz,
                MessageHandler handler) {
        }

        public void addUDPMessageHandler(Class<? extends Message> clazz,
                MessageHandler handler) {
        }

        public void broadcastPingRequest(PingRequest ping) {
        }

        public void downloadFinished(GUID guid) throws IllegalArgumentException {
        }

        public void forwardInspectionRequestToLeaves(InspectionRequest ir) {
        }

        public void forwardQueryRequestToLeaves(QueryRequest query,
                ReplyHandler handler) {
        }

        public MessageHandler getMessageHandler(Class<? extends Message> clazz) {
            return null;
        }

        public MessageHandler getMulticastMessageHandler(
                Class<? extends Message> clazz) {
            return null;
        }

        public String getPingRouteTableDump() {
            return null;
        }

        public String getPushRouteTableDump() {
            return null;
        }

        public Set<GUESSEndpoint> getQueryLocs(GUID guid) {
            return null;
        }

        public QueryRouteTable getQueryRouteTable() {
            return null;
        }

        public String getQueryRouteTableDump() {
            return null;
        }

        public MessageHandler getUDPMessageHandler(
                Class<? extends Message> clazz) {
            return null;
        }

        public void handleMessage(Message msg,
                ReplyHandler receivingConnection) {
        }

        public void handleMulticastMessage(Message msg, InetSocketAddress addr) {
        }

        public void handleUDPMessage(Message msg, InetSocketAddress addr) {
        }

        public void start() {
        }

        public boolean isHostUnicastQueried(GUID guid, IpPort host) {
            return false;
        }

        public boolean sendInitialQuery(QueryRequest query, RoutedConnection mc) {
            return false;
        }

        public void originateQueryGUID(byte[] guid) {
        }

        public void queryKilled(GUID guid) throws IllegalArgumentException {
        }

        public void registerMessageListener(byte[] guid, MessageListener ml) {
        }

        public void removeConnection(ReplyHandler rh) {
        }

        public Iterable<QueryReply> responsesToQueryReplies(
                Response[] responses, QueryRequest queryRequest) {
            return null;
        }
        
        public Iterable<QueryReply> responsesToQueryReplies(
                Response[] responses, QueryRequest queryRequest,
                int num, SecurityToken tok) {
            return null;
        }

        public void sendDynamicQuery(QueryRequest query) {
        }

        public void sendMulticastPushRequest(PushRequest push) {
        }

        public void sendPingRequest(PingRequest request,
                RoutedConnection connection) {
        }

        public void sendPushRequest(PushRequest push) throws IOException {
        }

        public void sendQueryRequest(QueryRequest request,
                RoutedConnection connection) {
        }

        public void setMessageHandler(Class<? extends Message> clazz,
                MessageHandler handler) {
        }

        public void setMulticastMessageHandler(Class<? extends Message> clazz,
                MessageHandler handler) {
        }

        public void setUDPMessageHandler(Class<? extends Message> clazz,
                MessageHandler handler) {
        }

        public void unregisterMessageListener(byte[] guid, MessageListener ml) {
        }

        public ReplyHandler getPushHandler(byte[] guid) {
            return null;
        }

        public void stop() {
        }
        
        public void initialize() {
        }
        
        public String getServiceName() {
            // TODO Auto-generated method stub
            return null;
        }
    }
    
    private static class MyReplyHandler extends ReplyHandlerStub {
        volatile Message m;
        final InetAddress addr;
        final int port;

        MyReplyHandler(InetAddress addr, int port) {
            this.addr = addr;
            this.port = port;
        }
        
        @Override
        public InetAddress getInetAddress() {
            return addr;
        }
        
        @Override
        public String getAddress() {
            return addr.getHostAddress();
        }
        
        @Override
        public int getPort() {
            return port;
        }

        @Override
        public void reply(Message m) {
            this.m = m;
        }
    }
}
