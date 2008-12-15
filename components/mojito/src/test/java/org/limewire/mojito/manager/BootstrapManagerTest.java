package org.limewire.mojito.manager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import junit.framework.TestSuite;

import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.MojitoFactory;
import org.limewire.mojito.MojitoTestCase;
import org.limewire.mojito.exceptions.DHTTimeoutException;
import org.limewire.mojito.io.MessageDispatcher;
import org.limewire.mojito.io.MessageDispatcherFactory;
import org.limewire.mojito.io.MessageDispatcherImpl;
import org.limewire.mojito.io.Tag;
import org.limewire.mojito.messages.DHTMessage;
import org.limewire.mojito.messages.FindNodeRequest;
import org.limewire.mojito.messages.PingRequest;
import org.limewire.mojito.result.BootstrapResult;
import org.limewire.mojito.result.PingResult;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.ContactFactory;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.settings.BootstrapSettings;
import org.limewire.mojito.settings.NetworkSettings;

public class BootstrapManagerTest extends MojitoTestCase {
    
    /*static {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
    }*/
    
    protected static final int BOOTSTRAP_DHT_PORT = 3000;

    protected static MojitoDHT BOOTSTRAP_DHT;
    
    protected static MojitoDHT TEST_DHT;
    
    private TestMessageDispatcherFactory tmf, bmf;

    public BootstrapManagerTest(String name){
        super(name);
    }
    
    public static TestSuite suite() {
        return buildTestSuite(BootstrapManagerTest.class);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    private static void setSettings() {
        NetworkSettings.DEFAULT_TIMEOUT.setValue(200);
        NetworkSettings.MIN_TIMEOUT_RTT.setValue(200);
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setSettings();
        setLocalIsPrivate(false);
        
        //setup bootstrap node
        BOOTSTRAP_DHT = MojitoFactory.createDHT("bootstrapNode");
        bmf = new TestMessageDispatcherFactory((Context)BOOTSTRAP_DHT);
        BOOTSTRAP_DHT.setMessageDispatcher(bmf);
        BOOTSTRAP_DHT.bind(BOOTSTRAP_DHT_PORT);
        BOOTSTRAP_DHT.start();
        
        //setup test node
        TEST_DHT = MojitoFactory.createDHT("dht-test");
        tmf = new TestMessageDispatcherFactory((Context)TEST_DHT);
        TEST_DHT.setMessageDispatcher(tmf);
        TEST_DHT.bind(2000);
        TEST_DHT.start();
    }

    @Override
    protected void tearDown() throws Exception {
        if (BOOTSTRAP_DHT != null) {
            BOOTSTRAP_DHT.close();
        }
        
        if (TEST_DHT != null) {
            TEST_DHT.close();
        }
    }
    
    public void testBootstrapFailure() throws Exception {
        ((Context)BOOTSTRAP_DHT).setExternalAddress(
                new InetSocketAddress("localhost", BOOTSTRAP_DHT_PORT));
        
        // try failure first
        BOOTSTRAP_DHT.stop();
        
        try {
            ((Context)TEST_DHT).ping(BOOTSTRAP_DHT.getLocalNode()).get();
            fail("BOOTSTRAP_DHT should not respond");
        } catch (ExecutionException err) {
            assertTrue(err.getCause() instanceof DHTTimeoutException);
        }
        
        // made sure we tried to ping them
        assertGreaterThan(1, tmf.tm.sent.size());
        Tag sent = tmf.tm.sent.get(0);
        assertEquals(sent.getNodeID(), BOOTSTRAP_DHT.getLocalNodeID());
        assertInstanceof(PingRequest.class, sent.getMessage());
        
        BootstrapResult result = TEST_DHT.bootstrap(BOOTSTRAP_DHT.getLocalNode()).get();
        assertEquals(BootstrapResult.ResultType.BOOTSTRAP_FAILED, result.getResultType());
        assertEquals(BOOTSTRAP_DHT.getLocalNodeID(), result.getContact().getNodeID());
        
        BOOTSTRAP_DHT.close();
    }

    public void testBootstrapFromList() throws Exception{
        //try pings to a bootstrap list
        //add some bad hosts
        Set<SocketAddress> bootstrapSet = new LinkedHashSet<SocketAddress>();
        bootstrapSet.clear();
        for(int i= 1; i < 5; i++) {
            bootstrapSet.add(new InetSocketAddress("localhost", BOOTSTRAP_DHT_PORT+i));
        }
        //add good host
        bootstrapSet.add(BOOTSTRAP_DHT.getContactAddress());
        
        PingResult pong = ((Context)TEST_DHT).ping(bootstrapSet).get();
        BootstrapResult result = TEST_DHT.bootstrap(pong.getContact()).get();
        assertEquals(result.getResultType(), BootstrapResult.ResultType.BOOTSTRAP_SUCCEEDED);
        assertEquals(BOOTSTRAP_DHT.getLocalNodeID(), result.getContact().getNodeID());
    }
    
    public void testBootstrapFromRouteTable() throws Exception{
        //try ping from RT
        RouteTable rt = TEST_DHT.getRouteTable();
        Contact node;
        for(int i= 1; i < 5; i++) {
            node = ContactFactory.createLiveContact(null, Vendor.UNKNOWN, Version.ZERO, 
                    KUID.createRandomID(),
                    new InetSocketAddress("localhost", 3000+i), 
                    0, Contact.DEFAULT_FLAG);
            rt.add(node);
        }
        //add good node
        node = ContactFactory.createLiveContact(null, Vendor.UNKNOWN, Version.ZERO,
                BOOTSTRAP_DHT.getLocalNodeID(),
                BOOTSTRAP_DHT.getContactAddress(), 0, Contact.DEFAULT_FLAG);
        rt.add(node);
        
        // Start pinging Nodes from the RouteTable
        PingResult pong = TEST_DHT.findActiveContact().get();
        // And bootstrap from the first Node that responds
        BootstrapResult evt = TEST_DHT.bootstrap(pong.getContact()).get();
        assertEquals(evt.getResultType(), BootstrapResult.ResultType.BOOTSTRAP_SUCCEEDED);
    }
    
    public void testBootstrapPoorRatio() throws Exception{
        //fill RT with bad nodes
        RouteTable rt = TEST_DHT.getRouteTable();
        Contact node;
        for(int i = 0; i < 100; i++) {
            node = ContactFactory.createUnknownContact(Vendor.UNKNOWN, Version.ZERO, 
                    KUID.createRandomID(),
                    new InetSocketAddress("localhost", 3000+i));
            rt.add(node);
        }
        node = ContactFactory.createUnknownContact(Vendor.UNKNOWN, Version.ZERO, 
                KUID.createRandomID(),
                new InetSocketAddress("localhost", 7777));
        rt.add(node);
        
        assertEquals(102, rt.size());
        PingResult pong = TEST_DHT.ping(BOOTSTRAP_DHT.getContactAddress()).get();
        BootstrapResult result = TEST_DHT.bootstrap(pong.getContact()).get();
        
        // See if RT was purged
        assertNotContains(rt.getActiveContacts(), node);
        assertEquals(result.getResultType(), BootstrapResult.ResultType.BOOTSTRAP_SUCCEEDED);
    }
    
    public void testBootstrapPoorRatioFails() throws Exception{
        BootstrapSettings.IS_BOOTSTRAPPED_RATIO.setValue(0.7f);
        //fill RT with bad nodes
        RouteTable rt = TEST_DHT.getRouteTable();
        Contact node;
        for(int i = 0; i < 100; i++) {
            node = ContactFactory.createUnknownContact(Vendor.UNKNOWN, Version.ZERO, 
                    KUID.createRandomID(),
                    new InetSocketAddress("localhost", 3000+i));
            rt.add(node);
        }
        node = ContactFactory.createUnknownContact(Vendor.UNKNOWN, Version.ZERO, 
                KUID.createRandomID(),
                new InetSocketAddress("localhost", 7777));
        rt.add(node);
        
        // we lose the find node requests
        bmf.tm.filter = new FindNodeFilter(1.0);
        assertEquals(102, rt.size());
        PingResult pong = TEST_DHT.ping(BOOTSTRAP_DHT.getContactAddress()).get();
        BootstrapResult result = TEST_DHT.bootstrap(pong.getContact()).get();
        
        assertEquals(result.getResultType(), BootstrapResult.ResultType.BOOTSTRAP_FAILED);
    }
    
    public void testBootstrapPoorRatioSucceeds() throws Exception{
        BootstrapSettings.IS_BOOTSTRAPPED_RATIO.setValue(0.1f);
        //fill RT with bad nodes
        RouteTable rt = TEST_DHT.getRouteTable();
        Contact node;
        for(int i = 0; i < 100; i++) {
            node = ContactFactory.createUnknownContact(Vendor.UNKNOWN, Version.ZERO, 
                    KUID.createRandomID(),
                    new InetSocketAddress("localhost", 3000+i));
            rt.add(node);
        }
        node = ContactFactory.createUnknownContact(Vendor.UNKNOWN, Version.ZERO, 
                KUID.createRandomID(),
                new InetSocketAddress("localhost", 7777));
        rt.add(node);
        
        // we lose the find node requests
        bmf.tm.filter = new FindNodeFilter(1.0);
        assertEquals(102, rt.size());
        PingResult pong = TEST_DHT.ping(BOOTSTRAP_DHT.getContactAddress()).get();
        BootstrapResult result = TEST_DHT.bootstrap(pong.getContact()).get();
        
        assertEquals(result.getResultType(), BootstrapResult.ResultType.BOOTSTRAP_SUCCEEDED);
    }
    
    private static class TestMessageDispatcherFactory implements MessageDispatcherFactory {
        final TestMessageDispatcher tm;
        public TestMessageDispatcherFactory(Context context) {
            tm = new TestMessageDispatcher(context);
        }
        public MessageDispatcher create(Context context) {
            return tm;
        }
        
    }
    private static class TestMessageDispatcher extends MessageDispatcherImpl {

        List<Tag> sent = new ArrayList<Tag>();
        List<Tag> notSent = new ArrayList<Tag>();
        List<DHTMessage> received = new ArrayList<DHTMessage>();
        List<DHTMessage> notReceived = new ArrayList<DHTMessage>();
        volatile TestFilter filter;
        
        public TestMessageDispatcher(Context context) {
            super(context);
        }
        @Override
        protected boolean send(Tag tag) throws IOException {
            TestFilter f = filter;
            if (f != null && !f.allow(tag.getMessage())) {
                notSent.add(tag);
                return false;
            }
            sent.add(tag);
            return super.send(tag);
        }
        
        @Override
        protected void handleMessage(DHTMessage m) {
            received.add(m);
            super.handleMessage(m);
        }
    }
    
    private static interface TestFilter {
        boolean allow(DHTMessage m);
    }
    
    private static class FindNodeFilter implements TestFilter {
        private final double prob;
        FindNodeFilter(double prob) {
            this.prob = prob;
        }
        public boolean allow(DHTMessage m) {
            return !(m instanceof FindNodeRequest && Math.random() < prob);
        }
    }
}
