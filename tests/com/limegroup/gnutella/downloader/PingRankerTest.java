package com.limegroup.gnutella.downloader;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;

import junit.framework.Test;

import org.limewire.collection.Cancellable;
import org.limewire.collection.IntervalSet;
import org.limewire.core.settings.DownloadSettings;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.io.IpPortSet;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.MessageListener;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.PushEndpointFactory;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.UDPPinger;
import com.limegroup.gnutella.UDPPingerImpl;
import com.limegroup.gnutella.UDPReplyHandlerFactory;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.filters.SpamFilterFactory;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingRequestFactory;
import com.limegroup.gnutella.messages.vendor.HeadPing;
import com.limegroup.gnutella.messages.vendor.HeadPong;
import com.limegroup.gnutella.messages.vendor.HeadPongFactory;
import com.limegroup.gnutella.messages.vendor.HeadPongImpl;
import com.limegroup.gnutella.stubs.MessageRouterStub;
import com.limegroup.gnutella.stubs.NetworkManagerStub;
import com.limegroup.gnutella.util.LimeTestCase;


/**
 * tests the functioning of the ping ranker, i.e. how it sends out headpings
 * and how it ranks hosts based on the returned results.
 *
 */
public class PingRankerTest extends LimeTestCase {

    private static final Set<PushEndpoint> EMPTY_PUSH_ENDPOINT_SET = Collections.emptySet();
    
    private MockPinger pinger;
    private PingRanker ranker;
    private MessageRouterStub messageRouter;
    private NetworkManagerStub networkManager;
    private UDPReplyHandlerFactory udpReplyHandlerFactory;
    private SpamFilterFactory spamFilterFactory;
    private PushEndpointFactory pushEndpointFactory;
    private HeadPongFactory headPongFactory;
    private RemoteFileDescFactory remoteFileDescFactory;
    private ConcurrentMap<RemoteFileDesc, RemoteFileDescContext> contexts = new ConcurrentHashMap<RemoteFileDesc, RemoteFileDescContext>(); 
        
    public PingRankerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(PingRankerTest.class);
    }

    @Override
    public void setUp() throws Exception {
        networkManager = new NetworkManagerStub();
        networkManager.setAcceptedIncomingConnection(false);
    
        Module module = new AbstractModule() {
            @Override
            protected void configure() {
                bind(UDPPinger.class).to(MockPinger.class);
                bind(NetworkManager.class).toInstance(networkManager);
                bind(MessageRouter.class).to(MessageRouterStub.class);
            }
            
        };
        
        Injector injector = LimeTestUtils.createInjector(module);

        messageRouter = (MessageRouterStub) injector.getInstance(MessageRouter.class);
        udpReplyHandlerFactory = injector.getInstance(UDPReplyHandlerFactory.class);
        spamFilterFactory = injector.getInstance(SpamFilterFactory.class);
        pushEndpointFactory = injector.getInstance(PushEndpointFactory.class);
        headPongFactory = injector.getInstance(HeadPongFactory.class);
        pinger = (MockPinger)injector.getInstance(UDPPinger.class);
        remoteFileDescFactory = injector.getInstance(RemoteFileDescFactory.class);
        
        ranker = new PingRanker(networkManager, pinger, messageRouter, remoteFileDescFactory);
        ranker.setMeshHandler(new MockMesh(ranker));
        DownloadSettings.WORKER_INTERVAL.setValue(-1);
        DownloadSettings.MAX_VERIFIED_HOSTS.revertToDefault();
        DownloadSettings.PING_BATCH.revertToDefault();
    }
    
    /**
     * Tests that the ranker sends out a HeadPing requesting ranges and alts to given hosts.
     */
    public void testPingsNewHosts() throws Exception {
        
        for (int i =1;i <= 10;i++) 
            ranker.addToPool(newRFDWithURN("1.2.3."+i,3));
        
        
        assertEquals(10,pinger.hosts.size());
        assertEquals(10,pinger.messages.size());
        
        for (int i = 0 ;i < 10; i++) {
            pinger.hosts.contains(newRFDWithURN("1.2.3."+i,3));
            HeadPing ping = (HeadPing) pinger.messages.get(i);
            assertTrue(ping.requestsRanges());
            assertTrue(ping.requestsAltlocs());
        }
    }
    
    /**
     * Tests that the ranker stops looking for new hosts once it has found enough.
     */
    public void testStopsPinging() throws Exception {
        DownloadSettings.MAX_VERIFIED_HOSTS.setValue(1);
        ranker.addToPool(newRFDWithURN("1.2.3.4",3));
        assertEquals(1,pinger.hosts.size());
        
        // get a reply from that host
        MockPong pong = new MockPong(true,true,-1,false,false,true,null);
        ranker.processMessage(pong, udpReplyHandlerFactory.createUDPReplyHandler(InetAddress.getByName("1.2.3.4"),1,  spamFilterFactory.createPersonalFilter()));
        
        // add some more hosts
        for (int i =1;i <= 10;i++) 
            ranker.addToPool(newRFDWithURN("1.2.3."+i,3));
        
        
        // no more pings should have been sent out.
        assertEquals(1,pinger.hosts.size());
        
        // consume the host we know about
        ranker.getBest();
        
        // we should send out some more pings
        assertGreaterThan(1,pinger.hosts.size());
    }
    
    /**
     * Tests that the ranker learns about new hosts from altlocs
     */
    public void testLearnsFromAltLocs() throws Exception {
        networkManager.setAcceptedIncomingConnection(true);
        RemoteFileDescContext original = newRFDWithURN("1.2.3.4",3); 
        ranker.addToPool(original);
        assertEquals(1,pinger.hosts.size());
        pinger.hosts.clear();
        
        // send two altlocs, one containing the node itself 
        IpPort ip = new IpPortImpl("1.2.3.5",1);
        
        Set<IpPort> alts = new IpPortSet();
        alts.add(ip);
        
        //and one push loc
        PushEndpoint pe = pushEndpointFactory.createPushEndpoint((new GUID(GUID.makeGuid())).toHexString()+";1.2.3.6:7");
        Set<PushEndpoint> push = new HashSet<PushEndpoint>();
        push.add(pe);
        
        MockPong pong = new MockPong(true,true,1,false,false,false,null,alts,push);
        MockMesh mesh = new MockMesh(ranker);
        ranker.setMeshHandler(mesh);
        ranker.processMessage(pong, udpReplyHandlerFactory.createUDPReplyHandler(InetAddress.getByName("1.2.3.4"),1, spamFilterFactory.createPersonalFilter()));
        assertNotNull(mesh.sources);
        ranker.addToPool(toContexts(mesh.sources));
        
        // now the ranker should know about more than one host.
        // the best host should be the one that actually replied.
        RemoteFileDescContext best = ranker.getBest();
        assertEquals(original,best);
        
        // the ranker should have more available hosts, even if we haven't
        // pinged any.
        assertTrue(ranker.hasMore());
        
        // the ranker should have pinged the other two hosts.
        assertEquals(2,pinger.hosts.size());
    }
    
    /**
     * Tests that the ranker does not add altlocs it already knows about 
     * either from other altlocs or from direct addition
     */
    public void testIgnoresDuplicateAlts() throws Exception {
        networkManager.setAcceptedIncomingConnection(true);
        RemoteFileDescContext original = newRFDWithURN("1.2.3.4",3);
        GUID g = new GUID(GUID.makeGuid());
        RemoteFileDescContext original2 = newPushRFD(g.bytes(),"2.2.2.2:2;3.3.3.3:3","1.2.3.6:7");
        ranker.addToPool(original);
        ranker.addToPool(original2);
        
        assertEquals(3,pinger.hosts.size());
        pinger.hosts.clear();

        // make one of the hosts send an altloc of itself (spammer?) and the pushloc 
        IpPort ip = new IpPortImpl("1.2.3.4",1);
        PushEndpoint pe = pushEndpointFactory.createPushEndpoint(g.toHexString()+";7:1.2.3.6;4.4.4.4:4");
        Set<IpPort> alts = new IpPortSet();
        alts.add(ip);
        Set<PushEndpoint> push = new HashSet<PushEndpoint>();
        push.add(pe);
        
        MockPong pong = new MockPong(true,true,-1,false,false,false,null,alts,push);
        ranker.processMessage(pong, udpReplyHandlerFactory.createUDPReplyHandler(InetAddress.getByName("1.2.3.4"),1, spamFilterFactory.createPersonalFilter()));
        
        // both of the carried altlocs are dupes, so we should not have pinged anybody
        assertTrue(pinger.hosts.isEmpty());
    }
    
    /**
     * Tests that the ranker sends a HeadPing to the push proxies of a firewalled
     * source as long as we are not firewalled or can do FWT
     */
    public void testPingsFirewalledHosts() throws Exception {
        networkManager.setAcceptedIncomingConnection(true);
        GUID g = new GUID(GUID.makeGuid());
        ranker.addToPool(newPushRFD(g.bytes(),"1.2.2.2:3","2.2.2.3:5"));
        assertEquals(1,pinger.hosts.size());
        assertIpPortEquals(new IpPortImpl("1.2.2.2",3),pinger.hosts.get(0));
        HeadPing ping = (HeadPing)pinger.messages.get(0);
        assertEquals(g,ping.getClientGuid());
    }
    
    /**
     * tests that we do not ping firewalled hosts if we cannot do FWT and are firewalled
     */
    public void testSkipsFirewalledHosts() throws Exception {
        assertFalse(networkManager.acceptedIncomingConnection());
        assertFalse(networkManager.canDoFWT());
        GUID g = new GUID(GUID.makeGuid());
        ranker.addToPool(newPushRFD(g.bytes(),"1.2.2.2:3","2.2.2.3:5"));
        assertEquals(0,pinger.hosts.size());
        assertEquals(0,pinger.hosts.size());
    }
    
    /**
     * We should drop unsolicited pongs.
     */
    public void testIgnoresUnsolicitedPongs() throws Exception {
        // add a host
        ranker.addToPool(newRFDWithURN("1.2.3.4",3));
        
        // receive a pong from another host
        MockPong pong = new MockPong(true,true,0,true,false,true,null);
        ranker.processMessage(pong, udpReplyHandlerFactory.createUDPReplyHandler(InetAddress.getByName("1.2.3.5"),1, spamFilterFactory.createPersonalFilter()));
        
        // consume the first guy
        assertTrue(ranker.hasMore());
        ranker.getBest();
        
        // we shouldn't have any more hosts available
        assertFalse(ranker.hasMore());
    }
    
    /**
     * When sending a ping to several push proxies, we may get replies
     * from more than one - only one should be processed. 
     */
    public void testMultipleProxyReplies() throws Exception {
        networkManager.setAcceptedIncomingConnection(true);
        GUID g = new GUID(GUID.makeGuid());
        ranker.addToPool(newPushRFD(g.bytes(),"1.2.2.2:3;1.3.3.3:4","2.2.2.3:5"));
        
        // two pings should be sent out
        assertEquals(2,pinger.hosts.size());
        Set<IpPort> s = new TreeSet<IpPort>(IpPort.COMPARATOR);
        s.addAll(pinger.hosts);
        assertTrue(s.contains(new IpPortImpl("1.2.2.2",3)));
        assertTrue(s.contains(new IpPortImpl("1.3.3.3",4)));
        
        // receive one pong from each proxy
        MockPong pong = new MockPong(true,true,-1,true,false,true,null);
        ranker.processMessage(pong, udpReplyHandlerFactory.createUDPReplyHandler(InetAddress.getByName("1.3.3.3"),4, spamFilterFactory.createPersonalFilter()));
        ranker.processMessage(pong, udpReplyHandlerFactory.createUDPReplyHandler(InetAddress.getByName("1.2.2.2"),3, spamFilterFactory.createPersonalFilter()));
        
        // there should be only one host available to try
        assertTrue(ranker.hasMore());
        ranker.getBest();
        assertFalse(ranker.hasMore());
    }
    
    /**
     * Tests that the ranker prefers hosts that have sent a pong back but in 
     * case it runs out of verified hosts it returns a non-verified one.
     */
    public void testPrefersPongedHost() throws Exception {
        assertFalse(ranker.hasMore());
        List<RemoteFileDescContext> l = new ArrayList<RemoteFileDescContext>(10);
        for (int i =0;i < 10;i++) 
            l.add(newRFDWithURN("1.2.3."+i,3));
        ranker.addToPool(l);
        
        assertTrue(ranker.hasMore());
        
        
        // send a pong back from a single host
        MockPong pong = new MockPong(true,true,0,true,false,true,null);
        ranker.processMessage(pong, udpReplyHandlerFactory.createUDPReplyHandler(InetAddress.getByName("1.2.3.5"),1, spamFilterFactory.createPersonalFilter()));
        
        // now this host should be prefered over other hosts.
        RemoteFileDescContext rfd = ranker.getBest();
        assertEquals("1.2.3.5", ((Connectable)rfd.getAddress()).getAddress());
     
        // but if we ask for more hosts we'll get some of the unverified ones
        assertTrue(ranker.hasMore());
        rfd = ranker.getBest();
        assertNotEquals("1.2.3.5", ((Connectable)rfd.getAddress()).getAddress());
        assertTrue(((Connectable)rfd.getAddress()).getAddress().startsWith("1.2.3."));
    }
    
    /**
     * Tests that the ranker discards sources that claim they do not have the file.
     * and informs the mesh handler if such exists
     */
    public void testDiscardsNoFile() throws Exception {
        RemoteFileDescContext noFile = newRFDWithURN("1.2.3.4",3); 
        ranker.addToPool(noFile);
        MockMesh handler = new MockMesh(ranker);
        ranker.setMeshHandler(handler);
        assertTrue(ranker.hasMore());
        MockPong pong = new MockPong(false,true,0,true,false,true,null);
        assertFalse(pong.hasFile());
        
        ranker.processMessage(pong, udpReplyHandlerFactory.createUDPReplyHandler(InetAddress.getByName("1.2.3.4"),1, spamFilterFactory.createPersonalFilter()));
        assertFalse(ranker.hasMore());
        assertEquals(noFile.getRemoteFileDesc(), handler.rfd);
        assertFalse(handler.good);
        ranker.setMeshHandler(null);
    }
    
    /**
     * Tests that the ranker offers hosts that indicated they were busy last
     */
    public void testBusyOfferedLast() throws Exception {
        List<RemoteFileDescContext> l = new ArrayList<RemoteFileDescContext>();
        l.add(newRFDWithURN("1.2.3.4",3));
        l.add(newRFDWithURN("1.2.3.5",3));
        ranker.addToPool(l);
        
        MockPong busy = new MockPong(true,true,20,true,true,true,null);
        MockPong notBusy = new MockPong(true,true,0,true,false,true,null);
        
        ranker.processMessage(busy, udpReplyHandlerFactory.createUDPReplyHandler(InetAddress.getByName("1.2.3.4"),1, spamFilterFactory.createPersonalFilter()));
        ranker.processMessage(notBusy, udpReplyHandlerFactory.createUDPReplyHandler(InetAddress.getByName("1.2.3.5"),1, spamFilterFactory.createPersonalFilter()));
        
        RemoteFileDescContext best = ranker.getBest();
        assertEquals("1.2.3.5", ((Connectable)best.getAddress()).getAddress()); // not busy
        best = ranker.getBest();
        assertEquals("1.2.3.4", ((Connectable)best.getAddress()).getAddress()); // busy
    }
    
    /**
     * Tests that the ranker offers hosts that have more free slots first 
     */
    public void testSortedByQueueRank() throws Exception {
        List<RemoteFileDescContext> l = new ArrayList<RemoteFileDescContext>();
        
        l.add(newRFDWithURN("1.2.3.4",3));
        l.add(newRFDWithURN("1.2.3.5",3));
        l.add(newRFDWithURN("1.2.3.6",3));
        ranker.addToPool(l);
        
        MockPong oneFree = new MockPong(true,true,-1,true,false,true,null);
        MockPong noFree = new MockPong(true,true,0,true,false,true,null);
        MockPong oneQueue = new MockPong(true,true,1,true,false,true,null);
        
        ranker.processMessage(oneQueue,udpReplyHandlerFactory.createUDPReplyHandler(InetAddress.getByName("1.2.3.4"),1, spamFilterFactory.createPersonalFilter()));
        ranker.processMessage(oneFree,udpReplyHandlerFactory.createUDPReplyHandler(InetAddress.getByName("1.2.3.5"),1, spamFilterFactory.createPersonalFilter()));
        ranker.processMessage(noFree,udpReplyHandlerFactory.createUDPReplyHandler(InetAddress.getByName("1.2.3.6"),1, spamFilterFactory.createPersonalFilter()));
        
        RemoteFileDescContext best = ranker.getBest();
        assertEquals("1.2.3.5", ((Connectable)best.getAddress()).getAddress()); // one free slot
        best = ranker.getBest();
        assertEquals("1.2.3.6", ((Connectable)best.getAddress()).getAddress()); // no free slots
        best = ranker.getBest();
        assertEquals("1.2.3.4", ((Connectable)best.getAddress()).getAddress()); // one queued
    }
    
    /**
     * tests that within the same queue rank the firewalled hosts are preferred
     * if we can't do fwt
     */
    public void testFirewalledPreferred() throws Exception {
        networkManager.setAcceptedIncomingConnection(true);
        RemoteFileDescContext open = newRFDWithURN("1.2.3.4",3);
        RemoteFileDescContext openMoreSlots = newRFDWithURN("1.2.3.5",3);
        RemoteFileDescContext push = newPushRFD(GUID.makeGuid(),"1.2.3.6:6",null);
        List<RemoteFileDescContext> l = new ArrayList<RemoteFileDescContext>();
        l.add(open);l.add(openMoreSlots);l.add(push);
        ranker.addToPool(l);
        
        MockPong openPong = new MockPong(true,true,-1,false,false,true,null);
        MockPong pushPong = new MockPong(true,true,-1,true,false,true,null);
        MockPong openMorePong = new MockPong(true,true,-2,false,false,true,null);
        
        ranker.processMessage(openPong,udpReplyHandlerFactory.createUDPReplyHandler(InetAddress.getByName("1.2.3.4"),1, spamFilterFactory.createPersonalFilter()));
        ranker.processMessage(openMorePong,udpReplyHandlerFactory.createUDPReplyHandler(InetAddress.getByName("1.2.3.5"),1, spamFilterFactory.createPersonalFilter()));
        ranker.processMessage(pushPong,udpReplyHandlerFactory.createUDPReplyHandler(InetAddress.getByName("1.2.3.6"),6, spamFilterFactory.createPersonalFilter()));
        
        RemoteFileDescContext best = ranker.getBest();
        assertEquals("1.2.3.5", ((Connectable)best.getAddress()).getAddress()); // open with more slots
        best = ranker.getBest();
        assertTrue(((PushEndpoint)best.getAddress()).getProxies().contains(new IpPortImpl("1.2.3.6",6))); // firewalled
        best = ranker.getBest();
        assertEquals("1.2.3.4", ((Connectable)best.getAddress()).getAddress()); // open
        
    }
    
    /**
     * tests that within the same rank and firewall status, partial sources are preferred
     */
    public void testPartialPreferred() throws Exception {
        networkManager.setAcceptedIncomingConnection(true);
        List<RemoteFileDescContext> l = new ArrayList<RemoteFileDescContext>();
        l.add(newRFDWithURN("1.2.3.4",3));
        l.add(newRFDWithURN("1.2.3.5",3));
        l.add(newRFDWithURN("1.2.3.6",3));
        l.add(newPushRFD(GUID.makeGuid(),"1.2.3.7:7",null));
        ranker.addToPool(l);
        
        MockPong oneFree = new MockPong(true,true,-1,true,false,true,null);
        MockPong oneFreePartial = new MockPong(true,false,-1,true,false,true,new IntervalSet());
        MockPong noSlotsFull = new MockPong(true,true,0,true,false,true,null);
        MockPong oneFreeOpen= new MockPong(true,true,-1,false,false,true,null);
        
        ranker.processMessage(noSlotsFull,udpReplyHandlerFactory.createUDPReplyHandler(InetAddress.getByName("1.2.3.4"),1, spamFilterFactory.createPersonalFilter()));
        ranker.processMessage(oneFreeOpen,udpReplyHandlerFactory.createUDPReplyHandler(InetAddress.getByName("1.2.3.5"),1, spamFilterFactory.createPersonalFilter()));
        ranker.processMessage(oneFreePartial,udpReplyHandlerFactory.createUDPReplyHandler(InetAddress.getByName("1.2.3.6"),1, spamFilterFactory.createPersonalFilter()));
        ranker.processMessage(oneFree,udpReplyHandlerFactory.createUDPReplyHandler(InetAddress.getByName("1.2.3.7"),7, spamFilterFactory.createPersonalFilter()));
        
        RemoteFileDescContext best = ranker.getBest();
        assertTrue(((PushEndpoint)best.getAddress()).getProxies().contains(new IpPortImpl("1.2.3.7",7))); // full, firewalled , one slot
        best = ranker.getBest();
        assertEquals("1.2.3.6", ((Connectable)best.getAddress()).getAddress()); // partial, open, one slot
        best = ranker.getBest();
        assertEquals("1.2.3.5", ((Connectable)best.getAddress()).getAddress()); // full, open, one slot 
        best = ranker.getBest();
        assertEquals("1.2.3.4", ((Connectable)best.getAddress()).getAddress()); // full, no slots, firewalled
    }
    
    /**
     * Tests that the ranker passes on to other rankers all the hosts it has been
     * told or learned about.
     */
    public void testGetShareable() throws Exception {
        RemoteFileDescContext rfd1, rfd2;
        rfd1 = newRFDWithURN("1.2.3.4",3);
        rfd2 = newRFDWithURN("1.2.3.5",3);
        ranker.addToPool(rfd1);
        ranker.addToPool(rfd2);
        
        Collection<RemoteFileDescContext> c = ranker.getShareableHosts();
        assertTrue(c.contains(rfd1));
        assertTrue(c.contains(rfd2));
        assertEquals(2,c.size());
        
        
        // tell the ranker about some altlocs through a headpong
        IpPort ip1, ip2;
        ip1 = new IpPortImpl("1.2.3.6",3);
        ip2 = new IpPortImpl("1.2.3.7",3);
        Set<IpPort> alts = new IpPortSet();
        alts.add(ip1);
        alts.add(ip2);
        MockPong oneFreeOpen= new MockPong(true,true,-1,false,false,true,null,alts, EMPTY_PUSH_ENDPOINT_SET);
        ranker.processMessage(oneFreeOpen,udpReplyHandlerFactory.createUDPReplyHandler(InetAddress.getByName("1.2.3.4"),1, spamFilterFactory.createPersonalFilter()));
        
        // the ranker should pass on the altlocs it discovered as well.
        c = ranker.getShareableHosts();
        assertEquals(4,c.size());
        Set<IpPort> s = new IpPortSet();
        for (RemoteFileDescContext context : c) {
            s.add(((Connectable)context.getAddress()));
        }
        assertEquals(4,s.size());
        assertTrue(s.contains(ip1));
        assertTrue(s.contains(ip2));
        assertTrue(s.contains(rfd1.getAddress()));
        assertTrue(s.contains(rfd2.getAddress()));
    }

    private  RemoteFileDescContext newRFDWithURN(String host, int speed) throws Exception {
        Set<URN> set = new HashSet<URN>();
        try {
            // for convenience, don't require that they pass the urn.
            // assume a null one is the TestFile's hash.
            set.add(TestFile.hash());
        } catch(Exception e) {
            fail("SHA1 not created");
        }
        return toContext(remoteFileDescFactory.createRemoteFileDesc(new ConnectableImpl(host, 1, false), 0, "asdf", TestFile.length(), new byte[16],
                speed, false, 4, false, null, set, false, "", -1));
    }
    
    /**
     * constructs an rfd for testing.  if the host parameter is not null, the 
     * rfd indicates FWT capability
     */
    private RemoteFileDescContext newPushRFD(byte [] guid,String proxy, String hostPort) throws Exception{
        GUID g = new GUID(guid);
        String s = g.toHexString();
        if (hostPort != null)
            s = s+";fwt/1.0;" +hostPort.substring(hostPort.indexOf(":")+1)+":"+hostPort.substring(0,hostPort.indexOf(":"));
        else 
            hostPort = "1.1.1.1";
         s =s+ ";"+proxy;
        
        PushEndpoint pe = pushEndpointFactory.createPushEndpoint(s);
        if (hostPort.contains(":")) {
            hostPort = hostPort.substring(0, hostPort.indexOf(":"));
        }
        RemoteFileDescContext ret = newRFDWithURN(hostPort,3);
        ret = new RemoteFileDescContext(remoteFileDescFactory.createRemoteFileDesc(ret.getRemoteFileDesc(), pe));
        return ret;
    }
    
    /**
     * a mock pinger.  Note that the base code will still register messsage listeners
     * but we don't care because they will never be used.
     */
    private static class MockPinger extends UDPPingerImpl {
        
        @Inject
        public MockPinger(Provider<MessageRouter> messageRouter, @Named("backgroundExecutor") ScheduledExecutorService scheduledExecutorService,
                Provider<UDPService> udpService, PingRequestFactory pingRequestFactory) {
            super(messageRouter, scheduledExecutorService, udpService, pingRequestFactory);
        }

        /**
         * the list of messages that was sent
         */
        public List<Message> messages = new ArrayList<Message>();
        
        /**
         * the list of hosts that we pinged, same order as messages
         */
        public List<IpPort> hosts = new ArrayList<IpPort>();
        
        @Override
        public void rank(Collection<? extends IpPort> hosts, MessageListener listener,
                Cancellable canceller, Message message) {
            for(IpPort next : hosts) {
                this.hosts.add(next);
                messages.add(message);
            }
        }
        
        
    }
    
    /**
     * a very customizable HeadPong
     */
    private class MockPong extends HeadPongImpl {
        
        private Set<IpPort> altLocs;
        private Set<PushEndpoint> pushLocs;
        private boolean have, full, firewalled, busy, downloading;
        private IntervalSet ranges;
        private int queueStatus;

        
        
        public MockPong(boolean have, boolean full, int queueStatus, 
                boolean firewalled, boolean busy, boolean downloading,
                IntervalSet ranges) 
        throws IOException {
            this(have, full, queueStatus, firewalled, busy, downloading, ranges, IpPort.EMPTY_SET, EMPTY_PUSH_ENDPOINT_SET);
        }
        
        public MockPong(boolean have, boolean full, int queueStatus, 
                boolean firewalled, boolean busy, boolean downloading,
                IntervalSet ranges, Set<IpPort> altLocs, Set<PushEndpoint> pushLocs) 
        throws IOException{
            super(new GUID(), HeadPong.VERSION, headPongFactory.create(new HeadPing(URN.createSHA1Urn("urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFE"))).getPayload());
            this.altLocs = altLocs;
            this.pushLocs = pushLocs;
            this.queueStatus = queueStatus;
            this.have = have;
            this.full = full;
            this.firewalled = firewalled;
            this.busy = busy;
            this.downloading = downloading;
            this.ranges = ranges;
        }

        @Override
        public Set<RemoteFileDesc> getAllLocsRFD(RemoteFileDesc original, RemoteFileDescFactory remoteFileDescFactory) {
            Set<RemoteFileDesc> ret = new HashSet<RemoteFileDesc>();
            
            if (altLocs!=null)
                for(Iterator iter = altLocs.iterator();iter.hasNext();) {
                    IpPort current = (IpPort)iter.next();
                    ret.add(remoteFileDescFactory.createRemoteFileDesc(original, current));
                }
            
            if (pushLocs!=null){
                for(Iterator iter = pushLocs.iterator();iter.hasNext();) {
                    PushEndpoint current = (PushEndpoint)iter.next();
                    ret.add(remoteFileDescFactory.createRemoteFileDesc(original, current));
                }
            }
            return ret;
        }

        @Override
        public Set<IpPort> getAltLocs() {
            return this.altLocs;
        }

        @Override
        public Set<PushEndpoint> getPushLocs() {
            return pushLocs;
        }
        
        @Override
        public int getQueueStatus() {
            return queueStatus;
        }
        
        @Override
        public IntervalSet getRanges() {
            return ranges;
        }
        
        @Override
        public boolean hasCompleteFile() {
            return full;
        }

        @Override
        public boolean hasFile() {
            return have;
        }

        @Override
        public boolean isBusy() {
            return busy;
        }

        @Override
        public boolean isDownloading() {
            return downloading;
        }

        @Override
        public boolean isFirewalled() {
            return firewalled;
        }

        @Override
        public boolean isRoutingBroken() {
            return true;
        }

    }

    private static void assertIpPortEquals(IpPort a, IpPort b) {
        assertTrue(IpPort.COMPARATOR.compare(a,b) == 0);
    }

    private RemoteFileDescContext toContext(RemoteFileDesc rfd) {
        RemoteFileDescContext newContext = new RemoteFileDescContext(rfd);
        RemoteFileDescContext oldContext = contexts.putIfAbsent(rfd, newContext);
        return oldContext != null ? oldContext : newContext;
    }
    
    private Collection<RemoteFileDescContext> toContexts(Collection<? extends RemoteFileDesc> hosts) {
        List<RemoteFileDescContext> list = new ArrayList<RemoteFileDescContext>();
        for (RemoteFileDesc host : hosts) {
            list.add(toContext(host));
        }
        return list;
    }

    
    private class MockMesh implements MeshHandler {
        private final SourceRanker ranker;
        public MockMesh(SourceRanker ranker) {
            this.ranker = ranker;
        }
        public boolean good;
        public RemoteFileDesc rfd;
        public Collection<? extends RemoteFileDesc> sources;
        public void informMesh(RemoteFileDesc rfd, boolean good) {
            this.rfd = rfd;
            this.good = good;
        }

        @Override
        public void addPossibleSources(Collection<? extends RemoteFileDesc> c) {
            sources = c;
            ranker.addToPool(toContexts(c));
        }
        

    }
}
