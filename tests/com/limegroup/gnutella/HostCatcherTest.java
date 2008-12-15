package com.limegroup.gnutella;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import junit.framework.Test;

import org.limewire.collection.FixedsizePriorityQueue;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.FilterSettings;
import org.limewire.io.GGEP;
import org.limewire.io.GUID;
import org.limewire.io.IpPortImpl;
import org.limewire.io.LocalSocketAddressProvider;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.util.ByteUtils;
import org.limewire.util.CommonUtils;
import org.limewire.util.PrivilegedAccessor;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.bootstrap.UDPHostCache;
import com.limegroup.gnutella.bootstrap.UDPHostCacheFactory;
import com.limegroup.gnutella.dht.DHTManager.DHTMode;
import com.limegroup.gnutella.messages.GGEPKeys;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingReplyFactory;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.PingRequestFactory;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.stubs.ConnectionManagerStub;
import com.limegroup.gnutella.util.LimeTestCase;


public class HostCatcherTest extends LimeTestCase {
    
    private HostCatcher hostCatcher;
    private Injector injector;

    public HostCatcherTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(HostCatcherTest.class);
    }


    public static void main(String argv[]) {
        junit.textui.TestRunner.run(suite());
    }
    
    /**
     * Returns a new HostCatcher connected to stubs. YOU MAY WANT TO CALL EXPIRE to force bootstrap pongs.
     */
    public void setUp() {
        // explicitly allow all ips to test.
        FilterSettings.BLACK_LISTED_IP_ADDRESSES.setValue(new String[] {});
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.setValue(new String[] { "*.*" });

        HostCatcher.DEBUG = true;
        
        injector = LimeTestUtils.createInjector();
        hostCatcher = injector.getInstance(HostCatcher.class);
        hostCatcher.start();
    }
    
    /**
     * Test the method for putting hosts on probation.
     * 
     * @throws Exception if an error occurs
     */
    public void testPutHostOnProbation() throws Exception {
        String ipStart = "34.56.";
        int penultimatetByte;
        for(int i=0; i<HostCatcher.PROBATION_HOSTS_SIZE; i++) {
            
            // Add a bunch of unique endpoints.
            if(i >= 512) {
                penultimatetByte = 2;
            } else if(i >= 255) {
                penultimatetByte = 1;
            } else {
                penultimatetByte = 0;
            }
            
            int lastByte = i%256;
            Endpoint curHost = 
                new Endpoint(ipStart+penultimatetByte+"."+lastByte, 6346);
            hostCatcher.putHostOnProbation(curHost);
        }
        
        Set probatedHosts =
            (Set)PrivilegedAccessor.getValue(hostCatcher, "PROBATION_HOSTS");
        
        assertEquals("unexpected size", HostCatcher.PROBATION_HOSTS_SIZE,
            probatedHosts.size());
        
        // Start adding slightly different IPs
        ipStart = "35.56.5.";
        for(int i=0; i<10; i++) {
            Endpoint curHost = new Endpoint(ipStart+i, 6346);
            hostCatcher.putHostOnProbation(curHost);
            assertEquals("unexpected size", HostCatcher.PROBATION_HOSTS_SIZE,
            probatedHosts.size());
        }
    }
    
    
    /**
     * Test the method for expiring hosts
     * 
     * @throws Exception if an error occurs
     */
    public void testExpireHosts() throws Exception {
        String ipStart = "34.56.";
        int penultimatetByte;
        for(int i=0; i<HostCatcher.EXPIRED_HOSTS_SIZE; i++) {
            
            // Add a bunch of unique endpoints.
            if(i >= 512) {
                penultimatetByte = 2;
            } else if(i >= 255) {
                penultimatetByte = 1;
            } else {
                penultimatetByte = 0;
            }
            
            int lastByte = i%256;
            Endpoint curHost = 
                new Endpoint(ipStart+penultimatetByte+"."+lastByte, 6346);
            hostCatcher.expireHost(curHost);
        }
        
        Set expiredHosts =
            (Set)PrivilegedAccessor.getValue(hostCatcher, "EXPIRED_HOSTS");
        
        assertEquals("unexpected size", HostCatcher.EXPIRED_HOSTS_SIZE,
            expiredHosts.size());
        
        // Start adding slightly different IPs
        ipStart = "35.56.5.";
        for(int i=0; i<10; i++) {
            Endpoint curHost = new Endpoint(ipStart+i, 6346);
            hostCatcher.putHostOnProbation(curHost);
            assertEquals("unexpected size", HostCatcher.EXPIRED_HOSTS_SIZE,
            expiredHosts.size());
        }
    }
    
    /**
     * Tests to make sure that the UDP Host Cache is used  
     * if we know of any host caches.
     */
    public void testUDPCachesUsed() throws Exception {
        // Use a different setup...
        injector = LimeTestUtils.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(UDPHostCacheFactory.class).to(UDPHostCacheFactoryStub.class);               
            }
        });
        hostCatcher = injector.getInstance(HostCatcher.class);
        hostCatcher.start();
        
        assertEquals(0, hostCatcher.getNumHosts());   
        
        StubUDPBootstrapper udp = (StubUDPBootstrapper)injector.getInstance(HostCatcher.class).getUdpHostCache();        
        Endpoint firstHost = hostCatcher.getAnEndpoint();
        assertTrue(udp.fetched);
        assertEquals(udp.host, firstHost.getAddress());
        udp.fetched = false;
        
        // Since udp was done quickly and only gave us one host (and we
        // just used it), the next request will spark a GW request.
        Endpoint second = hostCatcher.getAnEndpointImmediate(null);
        assertNull(second);
        Thread.sleep(5000); // just to make sure it doesn't trigger a fetch later
        assertFalse(udp.fetched);
        
        udp.expired = false;
        
        // Now another fetch will wait until time passes enough to retry
        // udp (too long before retrying a GW)
        Endpoint thirdHost = hostCatcher.getAnEndpoint();
        assertTrue(udp.fetched);
        assertEquals(udp.host, thirdHost.getAddress());
    }
        
    
    /**
     * Tests to make sure that we ignore hosts that have expired.
     * 
     * @throws Exception if any error occurs
     */
    public void testIgnoreExpiredHosts() throws Exception {
        Endpoint expiredHost = new Endpoint("20.4.5.7", 6346);
        hostCatcher.start();
        hostCatcher.add(expiredHost,true);
        assertEquals("unexpected number of hosts", 1, hostCatcher.getNumHosts());
        Endpoint accessedHost = hostCatcher.getAnEndpoint();
        assertNotNull("host should not be null", accessedHost);
        assertEquals("unexpected number of hosts", 0, hostCatcher.getNumHosts());
        
        hostCatcher.expireHost(expiredHost);
        hostCatcher.add(expiredHost, true);
        assertEquals("unexpected number of hosts", 0, hostCatcher.getNumHosts());        
    }
    
    /**
     * Tests to make sure that we ignore hosts that have been put on probation.
     * 
     * @throws Exception if any error occurs
     */
    public void testIgnoreProbatedHosts() throws Exception {
        Endpoint probatedHost = new Endpoint("20.4.5.7", 6346);
        hostCatcher.start();
        hostCatcher.add(probatedHost,true);
        assertEquals("unexpected number of hosts", 1, hostCatcher.getNumHosts());
        Endpoint accessedHost = hostCatcher.getAnEndpoint();
        assertNotNull("host should not be null", accessedHost);
        assertEquals("unexpected number of hosts", 0, hostCatcher.getNumHosts());
        
        hostCatcher.putHostOnProbation(probatedHost);
        hostCatcher.add(probatedHost, true);
        assertEquals("unexpected number of hosts", 0, hostCatcher.getNumHosts());        
    }
    
    /**
     * Tests to make sure that hosts that are put on probation are properly 
     * recovered.
     * 
     * @throws Exception if any error occurs
     */
    public void testRecoveryOfHostsOnProbation() throws Exception {
        long waitTime = 100;
        PrivilegedAccessor.setValue(HostCatcher.class, 
            "PROBATION_RECOVERY_WAIT_TIME", new Long(waitTime));
        long interval = 20000;
        PrivilegedAccessor.setValue(HostCatcher.class, 
            "PROBATION_RECOVERY_TIME", new Long(interval));
        
        Endpoint probatedHost = new Endpoint("20.4.5.7", 6346);
        
        // Put the host on probation.
        hostCatcher.putHostOnProbation(probatedHost);
        
        hostCatcher.add(probatedHost, true);
        
        // And make sure that it did not get added
        assertEquals("unexpected number of hosts", 0, hostCatcher.getNumHosts());
        
        // Start the probation recovery sequence...
        hostCatcher.start();        
        
        // Sleep until the recovery operation takes place...
        Thread.sleep(waitTime+200);
        
        // Finally, make sure we are then able to add the host that was 
        // formerly on probation.
        hostCatcher.add(probatedHost, true);
        assertEquals("unexpected number of hosts", 1, hostCatcher.getNumHosts());        
    }
    
    /**
     * Tests to make sure that recovering used hosts works as expected.  This
     * method is used when the user's network connection goes down.
     *
     * @throws Exception if any error occurs
     */    
    public void testRecoversUsedHosts() throws Exception {
        // write data to gnutella.net
        hostCatcher.add(new Endpoint("18.239.0.1"), false);
        hostCatcher.add(new Endpoint("18.239.0.2"), false);
        hostCatcher.add(new Endpoint("18.239.0.3"), false);
        hostCatcher.add(new Endpoint("18.239.0.4"), false);
        hostCatcher.add(new Endpoint("18.239.0.5"), false);
        hostCatcher.add(new Endpoint("18.239.0.6"), false);
        hostCatcher.add(new Endpoint("18.239.0.7"), false);
        hostCatcher.add(new Endpoint("18.239.0.8"), false);
        hostCatcher.add(new Endpoint("18.239.0.9"), false);
        hostCatcher.add(new Endpoint("18.239.0.10"), false);

        hostCatcher.write();
        int numHosts = hostCatcher.getNumHosts();

        for(int i=0; i<10; i++) {
            hostCatcher.getAnEndpoint();
        }
        
        assertEquals("hosts should be 0", 0, hostCatcher.getNumHosts());
        hostCatcher.recoverHosts();
        assertEquals("hosts should have been recovered", 
            numHosts, hostCatcher.getNumHosts());
    }
    
    /** Tests that FixedsizePriorityQueue can hold two endpoints with same
     *  priority but different ip's.  This was a problem at one point. */
    public void testEndpointPriorities() {
        ExtendedEndpoint e1=new ExtendedEndpoint("18.239.0.146", 6346);
        ExtendedEndpoint e2=new ExtendedEndpoint("18.239.0.147", 6347);
        assertNotEquals("e1 vs e2", e1, e2);
        assertNotEquals("e2 vs e1", e2, e1);
        
        FixedsizePriorityQueue<ExtendedEndpoint> queue=new FixedsizePriorityQueue<ExtendedEndpoint>(
            ExtendedEndpoint.priorityComparator(),
            10);
        assertNull(queue.insert(e1));
        assertNull(queue.insert(e2));
        assertEquals(2, queue.size());
    }


    public void testAddPriorities() {
        PingReplyFactory pingReplyFactory = injector.getInstance(PingReplyFactory.class);
        
        // HostCatcher should ignore attempts to add hosts with private 
        // addresses.
        hostCatcher.add(new Endpoint("192.168.0.1"), false);
        assertEquals("private endpoint added as ultrapeer",
					 0, hostCatcher.getNumUltrapeerHosts());

        assertEquals("private endpoint added at all",
					 0, hostCatcher.getNumHosts());

        setUp();
        // Adding a normal host should add 1 more to numNormalHosts
        hostCatcher.add(new Endpoint("18.239.0.1"), false);
        assertEquals("normal endpoint added as ultrapeer",
                0, hostCatcher.getNumUltrapeerHosts());

        setUp();
        // Adding a ultrapeer should add 1 more to numUltrapeerHosts
        hostCatcher.add(new Endpoint("18.239.0.1"), true);
        assertEquals("ultrapeer endpoint not added as ultrapeer",
                1, hostCatcher.getNumUltrapeerHosts());

        //PingReply's.
        setUp();
        // Adding a private should add 1 more to numPrivateHosts
        hostCatcher.add(pingReplyFactory.createExternal(new byte[16], (byte)3, 6346, 
            new byte[] {(byte)192,(byte)168,(byte)0,(byte)1}, false));
        assertEquals("private PingReply added as ultrapeer",
					 0 ,hostCatcher.getNumUltrapeerHosts());

        setUp();
        hostCatcher.add(pingReplyFactory.createExternal(new byte[16], (byte)3, 6346, 
            new byte[] {(byte)18,(byte)239,(byte)0,(byte)1}, false));
        assertEquals("normal PingReply added as ultrapeer",
                0, hostCatcher.getNumUltrapeerHosts());


        setUp();
        hostCatcher.add(pingReplyFactory.createExternal(new byte[16], (byte)3, 6346, 
            new byte[] {(byte)18,(byte)239,(byte)0,(byte)1}, true));
        assertEquals("ultrapeer PingReply not added as ultrapeer",
                1, hostCatcher.getNumUltrapeerHosts());
    }


    public void testPermanent() throws Exception {
        PingReplyFactory pingReplyFactory = injector.getInstance(PingReplyFactory.class);
        
        //Systm.out.println("-Testing write of permanent nodes to Gnutella.net");
        //1. Create HC, add entries, write to disk.
        hostCatcher.add(new Endpoint("18.239.0.141", 6341), false);//default time=345
        hostCatcher.add(pingReplyFactory.createExternal(GUID.makeGuid(), (byte)7, 6342,
            new byte[] {(byte)18, (byte)239, (byte)0, (byte)142}, 1000, false));
        
        // duplicate
        hostCatcher.add(pingReplyFactory.createExternal(GUID.makeGuid(), (byte)7, 6342,
            new byte[] {(byte)18, (byte)239, (byte)0, (byte)142}, 1000, false));  
        hostCatcher.add(pingReplyFactory.createExternal(GUID.makeGuid(), (byte)7, 6343,
            new byte[] {(byte)18, (byte)239, (byte)0, (byte)143}, 30, false));
        // duplicate (well, with lower uptime)
        hostCatcher.add(pingReplyFactory.createExternal(GUID.makeGuid(), (byte)7, 6343,
            new byte[] {(byte)18, (byte)239, (byte)0, (byte)143}, 30, false));
        
        // private address (ignored)
        hostCatcher.add(pingReplyFactory.createExternal(GUID.makeGuid(), (byte)7, 6343,
            new byte[] {(byte)192, (byte)168, (byte)0, (byte)1}, 3000, false));
            
        // udp host caches ..
        hostCatcher.add(new ExtendedEndpoint("1.2.3.4", 6346).setUDPHostCache(true), false);
        hostCatcher.add(new ExtendedEndpoint("1.2.3.5", 6341).setUDPHostCache(true), false);
        
        // dht capable node
        ExtendedEndpoint ep = new ExtendedEndpoint("18.239.0.100", 6323, 3);
        ep.setDHTVersion(2);
        ep.setDHTMode(DHTMode.INACTIVE);
        hostCatcher.add(ep, false);
        // dht active node
        ep = new ExtendedEndpoint("18.239.0.101", 6322, 2);
        ep.setDHTVersion(1);
        ep.setDHTMode(DHTMode.ACTIVE);
        hostCatcher.add(ep, false);
            
        File tmp=File.createTempFile("hc_test", ".net" );
        hostCatcher.write(tmp);

        //2. read HC from file.
        setUp(); // make sure we clear from memory the stuff we just added.
        UDPHostCache uhc = (UDPHostCache)PrivilegedAccessor.getValue(hostCatcher, "udpHostCache");
        assertEquals(0, uhc.getSize());
        assertEquals(0, hostCatcher.getNumHosts());
        hostCatcher.read(tmp);
        assertEquals(2, uhc.getSize());        
        assertEquals(5, hostCatcher.getNumHosts());
        // the highest uptime hosts should be given out first,
        // but not in order
        Set<Endpoint> s = new HashSet<Endpoint>();
        s.add(hostCatcher.getAnEndpoint());
        s.add(hostCatcher.getAnEndpoint());
        s.add(hostCatcher.getAnEndpoint());
        assertTrue(s.contains(new Endpoint("18.239.0.142", 6342)));
        assertTrue(s.contains(new Endpoint("18.239.0.141", 6341)));
        assertTrue(s.contains(new Endpoint("18.239.0.143", 6343)));
        
        ExtendedEndpoint xep = (ExtendedEndpoint) hostCatcher.getAnEndpoint();
        assertTrue(xep.supportsDHT());
        assertEquals(xep.getDHTVersion(), 2);
        xep = (ExtendedEndpoint) hostCatcher.getAnEndpoint();
        assertTrue(xep.supportsDHT());
        assertEquals(xep.getDHTVersion(), 1);
        assertEquals(DHTMode.ACTIVE, xep.getDHTMode());
        assertEquals(0, hostCatcher.getNumHosts());
        //Cleanup.
        tmp.delete();
    }

    /** Tests that only the best hosts are remembered.  */
    public void testBestPermanent() throws Exception  {  
        PingReplyFactory pingReplyFactory = injector.getInstance(PingReplyFactory.class);
        
        HostCatcher.DEBUG=false;  //Too darn slow
        //1. Fill up host catcher with PERMANENT_SIZE+1 mid-level pongs
        //(various uptimes).
        final int N=HostCatcher.PERMANENT_SIZE;
        for (int i=0; i<=N; i++) {            
            hostCatcher.add(pingReplyFactory.createExternal(GUID.makeGuid(), (byte)7, i+1,
                new byte[] {(byte)18, (byte)239, (byte)0, (byte)142},
                    i+10, false));
        }
        //Now add bad pong--which isn't really added
        hostCatcher.add(pingReplyFactory.createExternal(GUID.makeGuid(), (byte)7, N+2,
            new byte[] {(byte)18, (byte)239, (byte)0, (byte)142}, 0, false));
        //Now re-add port 1 (which was kicked out earlier).
        hostCatcher.add(pingReplyFactory.createExternal(GUID.makeGuid(), (byte)7, 1,
            new byte[] {(byte)18, (byte)239, (byte)0, (byte)142}, N+101,false));

        File tmp=File.createTempFile("hc_test", ".net" );
        hostCatcher.write(tmp);            

        //2. Read
        setUp();
        HostCatcher.DEBUG=false;  //Too darn slow
        hostCatcher.read(tmp);
        assertEquals(0, hostCatcher.getNumUltrapeerHosts());

        Set<Endpoint> s = new HashSet<Endpoint>();
        // Note that we only go to 1 (not 0) because we already extracted
        // a host in the line before this.
        for (int i=N; i > 0; i--) 
            s.add(hostCatcher.getAnEndpoint());
        
        // the bad host should not be in there
        assertFalse(s.contains(new Endpoint("18.239.0.142",N+2)));
        // the good one should
        assertTrue(s.contains(new Endpoint("18.239.0.142",1)));
        
        assertEquals("some hosts leftover", 0, hostCatcher.getNumHosts());

        //Cleanup.
        tmp.delete();
    }
    
    /**
     * tests that when reading from disk, the best third of the hosts
     * will be given out first
     */
    public void testBestRestored() throws Exception {
        // add a bunch of bad hosts
        for (int i = 0; i < 10; i++)
            hostCatcher.add(new ExtendedEndpoint("1.1.1.1", 100+i, i), false);
        // a bunch of average hosts
        for (int i = 0; i < 10; i++)
            hostCatcher.add(new ExtendedEndpoint("2.2.2.2", 200+i, 200+i), false);
        // a bunch of good hosts
        for (int i = 0; i < 20; i++)
            hostCatcher.add(new ExtendedEndpoint("3.3.3.3", 300+i, 300+i), false);

        // reload
        File tmp=File.createTempFile("hc_test", ".net" );
        hostCatcher.write(tmp);            
        setUp();
        hostCatcher.read(tmp);
        tmp.delete();
        
        // the first 10 hosts should all be good
        for (int i = 0; i < 10; i++) 
            assertEquals("3.3.3.3",hostCatcher.getAnEndpoint().getAddress());
        
        // the next 10 should not have any bad ones
        Set<Endpoint> s = new HashSet<Endpoint>();
        for (int i = 0; i < 10; i++) 
            s.add(hostCatcher.getAnEndpoint());
        for (int i = 0; i < 10; i++) 
            assertFalse(s.contains(new Endpoint("1.1.1.1", 100+i)));
    }

    /** Test that connection history is recorded. */
    public void testDoneWithConnect() throws Exception {
        hostCatcher.add(new Endpoint("18.239.0.1"), true);  
        hostCatcher.add(new Endpoint("18.239.0.2"), true);  //will succeed
        hostCatcher.add(new Endpoint("18.239.0.3"), true);  //will fail

        ExtendedEndpoint e3=(ExtendedEndpoint)hostCatcher.getAnEndpoint();
        assertEquals(new Endpoint("18.239.0.3"), e3);
        ExtendedEndpoint e2=(ExtendedEndpoint)hostCatcher.getAnEndpoint();
        assertEquals(new Endpoint("18.239.0.2"), e2);

        //record success (increases priority)
        hostCatcher.doneWithConnect(e2, true); 
        //record failure (lowers priority) with alternate form of method
        hostCatcher.doneWithConnect(e3, false);
        //Garbage (ignored)
        hostCatcher.doneWithConnect(new Endpoint("1.2.3.4", 6346), false);  
        hostCatcher.doneWithConnect(new Endpoint("18.239.0.3", 6349), true); //port

        //Check that permanent hosts are re-arranged.
        //Note that iterator yields worst to best.
        Iterator iter=hostCatcher.getPermanentHosts();
        ExtendedEndpoint e=(ExtendedEndpoint)iter.next();
        assertEquals(new Endpoint("18.239.0.3"), e);
        assertTrue(!e.getConnectionSuccesses().hasNext());
        assertTrue(e.getConnectionFailures().hasNext());

        e=(ExtendedEndpoint)iter.next();
        assertEquals(new Endpoint("18.239.0.1"), e);
        assertTrue(!e.getConnectionSuccesses().hasNext());
        assertTrue(!e.getConnectionFailures().hasNext());

        e=(ExtendedEndpoint)iter.next();
        assertEquals(new Endpoint("18.239.0.2"), e);
        assertTrue(e.getConnectionSuccesses().hasNext());
        assertTrue(!e.getConnectionFailures().hasNext());
    }

    public void testBadGnutellaDotNet() throws Exception {
        //System.out.println("-Testing bad Gnutella.net");
        //1. Write (mostly) corrupt file
        File tmp=File.createTempFile("hc_test", ".net" );
        FileWriter out=new FileWriter(tmp);
        out.write("18.239.0.141\n");                  //GOOD: port optional
        out.write("\n");                              //blank line
        out.write("18.239.0.144:A\n");                //bad port
        out.write("18.239.0.141:6347 A\n");           //bad uptime
        out.write("<html>total crap\n");              //not even close!
        out.write("  some garbage,1000,a,b,c,d,e,f,g\n");   //bad address
        out.write("18.239.0.142:6342,1000,a,b,c,d,e,f,g\n");//GOOD: ignore extra
        out.flush();
        out.close();

        //2. Read and verify
        setUp();
        hostCatcher.read(tmp);
        Set<Endpoint> s = new HashSet<Endpoint>();
        s.add(hostCatcher.getAnEndpoint());
        s.add(hostCatcher.getAnEndpoint());
        assertTrue(s.contains(new Endpoint("18.239.0.142", 6342)));
        assertTrue(s.contains(new Endpoint("18.239.0.141", 6346)));
        assertEquals("unexpected number of hosts", 0, hostCatcher.getNumHosts());
        assertEquals("unexpected number of ultrapeers",
            0, hostCatcher.getNumUltrapeerHosts());

        //Clean up
        tmp.delete();
    }
    
    public void testUDPHostCacheAdded() throws Exception {
        PingReplyFactory pingReplyFactory = injector.getInstance(PingReplyFactory.class);
        
        UDPHostCache uhc = (UDPHostCache)PrivilegedAccessor.getValue(hostCatcher, "udpHostCache");
        assertEquals(0, hostCatcher.getNumHosts());
        assertEquals(0, uhc.getSize());
        
        // Test with UDPHC pongs.
        GGEP ggep = new GGEP();
        ggep.put(GGEPKeys.GGEP_HEADER_UDP_HOST_CACHE);
        PingReply pr = pingReplyFactory.create(GUID.makeGuid(), (byte)1, 1,
                    new byte[] { 1, 1, 1, 1 },
                    0, 0, false, ggep);
        hostCatcher.add(pr);
        
        assertEquals(0, hostCatcher.getNumHosts());
        assertEquals(1, uhc.getSize());
        
        // Test with an endpoint.
        ExtendedEndpoint ep = new ExtendedEndpoint("3.2.3.4", 6346);
        ep.setUDPHostCache(true);
        hostCatcher.add(ep, false);
        assertEquals(0, hostCatcher.getNumHosts());
        assertEquals(2, uhc.getSize());
        
        // Test with a name in the cache.
        ggep = new GGEP();
        ggep.put(GGEPKeys.GGEP_HEADER_UDP_HOST_CACHE, "www.limewire.org");
        pr = pingReplyFactory.create(GUID.makeGuid(), (byte)1, 1,
                    new byte[] { 5, 4, 3, 2 },
                    0, 0, false, ggep);
        hostCatcher.add(pr);
        assertEquals(0, hostCatcher.getNumHosts());
        assertEquals(3, uhc.getSize());
        
        Set s = (Set)PrivilegedAccessor.getValue(uhc, "udpHostsSet");
        // assert that it had all our endpoints.
        assertContains(s, new ExtendedEndpoint("1.1.1.1", 1));
        assertContains(s, new ExtendedEndpoint("3.2.3.4", 6346));
        assertContains(s, new ExtendedEndpoint("www.limewire.org", 1));
    }
    
    public void testPackedIPPongsAreUsed() throws Exception {
        PingReplyFactory pingReplyFactory = injector.getInstance(PingReplyFactory.class);
        
        ConnectionSettings.FILTER_CLASS_C.setValue(true);
        assertEquals(0, hostCatcher.getNumHosts());
        
        GGEP ggep = new GGEP();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(new byte[] { 1, 1, 1, 1, 1, 0 } );
        out.write(new byte[] { 1, 1, 1, 2, 1, 0 } ); // same class C - filtered
        out.write(new byte[] { 1, 2, 3, 4, 2, 0 } );
        out.write(new byte[] { 3, 4, 2, 3, 3, 0 } );
        out.write(new byte[] { (byte)0xFE, 0, 0, 3, 4, 0 } );
        ggep.put(GGEPKeys.GGEP_HEADER_PACKED_IPPORTS, out.toByteArray());
        PingReply pr = pingReplyFactory.create(
            GUID.makeGuid(), (byte)1, 1, new byte[] { 4, 3, 2, 1 },
            0, 0, false, ggep);
        
        hostCatcher.add(pr);
        assertEquals(5, hostCatcher.getNumHosts());
    }
    
    public void testPackedIPsWithUHC() throws Exception {
        PingReplyFactory pingReplyFactory = injector.getInstance(PingReplyFactory.class);
        
        UDPHostCache uhc = (UDPHostCache)PrivilegedAccessor.getValue(hostCatcher, "udpHostCache");
        assertEquals(0, hostCatcher.getNumHosts());
        assertEquals(0, uhc.getSize());
        
        GGEP ggep = new GGEP();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(new byte[] { 1, 1, 1, 1, 1, 0 } );
        out.write(new byte[] { 1, 2, 3, 4, 2, 0 } );
        out.write(new byte[] { 3, 4, 2, 3, 3, 0 } );
        out.write(new byte[] { (byte)0xFE, 0, 0, 3, 4, 0 } );
        ggep.put(GGEPKeys.GGEP_HEADER_PACKED_IPPORTS, out.toByteArray());
        ggep.put(GGEPKeys.GGEP_HEADER_UDP_HOST_CACHE);
        PingReply pr = pingReplyFactory.create(
            GUID.makeGuid(), (byte)1, 1, new byte[] { 4, 3, 2, 1 },
            0, 0, false, ggep);
        
        hostCatcher.add(pr);
        assertEquals(4, hostCatcher.getNumHosts());
        assertEquals(1, uhc.getSize());
    }
    
    public void testPackedHostCachesAreStored() throws Exception {
        PingReplyFactory pingReplyFactory = injector.getInstance(PingReplyFactory.class);
        
        UDPHostCache uhc = (UDPHostCache)PrivilegedAccessor.getValue(hostCatcher, "udpHostCache");
        assertEquals(0, hostCatcher.getNumHosts());
        assertEquals(0, uhc.getSize());
        
        GGEP ggep = new GGEP();
        String addrs ="1.2.3.4:81\n" +
        	"www.limewire.com:6379\n"+
        	"www.eff.org\n"+
            "www.test.org:1";
        ggep.putCompressed(GGEPKeys.GGEP_HEADER_PACKED_HOSTCACHES, addrs.getBytes());
        PingReply pr = pingReplyFactory.create(
            GUID.makeGuid(), (byte)1, 1, new byte[] { 4, 3, 2, 1 },
            0, 0, false, ggep);
        
        hostCatcher.add(pr);
        assertEquals(1, hostCatcher.getNumHosts());
        assertEquals(4, uhc.getSize());
    }
    
    public void testImmediateEndpointObserverNoHosts() throws Exception {
        StubEndpointObserver observer = new StubEndpointObserver();
        assertEquals(0, hostCatcher.getNumHosts());
        assertNull(observer.getEndpoint());
        assertNull(hostCatcher.getAnEndpointImmediate(observer));
        assertNull(observer.getEndpoint());
        Endpoint p = new Endpoint("231.123.254.1", 1);
        hostCatcher.add(p, true);
        assertEquals(p, observer.getEndpoint());
    }
    
    public void testImmediateEndpointObserverWithHosts() throws Exception {
        Endpoint p = new Endpoint("231.123.254.1", 1);
        hostCatcher.add(p, true);
        StubEndpointObserver observer = new StubEndpointObserver();
        assertEquals(1, hostCatcher.getNumHosts());
        assertNull(observer.getEndpoint());
        assertEquals(p, hostCatcher.getAnEndpointImmediate(observer));
        assertNull(observer.getEndpoint());
    }
    
    public void testAsyncEndpointObserver() throws Exception {
        StubEndpointObserver observer = new StubEndpointObserver();
        assertEquals(0, hostCatcher.getNumHosts());
        assertNull(observer.getEndpoint());
        hostCatcher.getAnEndpoint(observer);
        assertNull(observer.getEndpoint());
        Endpoint p = new Endpoint("231.123.254.1", 1);
        hostCatcher.add(p, true);
        assertEquals(p, observer.getEndpoint());
    }
    
    public void testMultipleAsyncEndpointObservers() throws Exception {
        StubEndpointObserver o1 = new StubEndpointObserver();
        StubEndpointObserver o2 = new StubEndpointObserver();
        
        assertEquals(0, hostCatcher.getNumHosts());
        hostCatcher.getAnEndpoint(o1);
        hostCatcher.getAnEndpoint(o2);
        assertNull(o1.getEndpoint());
        assertNull(o2.getEndpoint());
        Endpoint p1 = new Endpoint("231.123.254.1", 1);
        Endpoint p2 = new Endpoint("231.123.254.1", 2);
        hostCatcher.add(p1, true);
        hostCatcher.add(p2, true);
        assertEquals(p1, o1.getEndpoint());
        assertEquals(p2, o2.getEndpoint());
    }
    
    public void testAsyncEndpointObserverEndpointAlreadyAdded() throws Exception {
        StubEndpointObserver observer = new StubEndpointObserver();
        assertEquals(0, hostCatcher.getNumHosts());
        assertNull(observer.getEndpoint());
        Endpoint p = new Endpoint("231.123.254.1", 1);
        hostCatcher.add(p, true);
        hostCatcher.getAnEndpoint(observer);
        assertEquals(p, observer.getEndpoint());       
    }
    
    public void testGetDHTSupportEndpoint() throws Exception {
        ConnectionSettings.FILTER_CLASS_C.setValue(true);
        assertEquals(0, hostCatcher.getDHTSupportEndpoint(0).size());
        
        // add a bunch of nodes from the same class C network
        ExtendedEndpoint ep;
        //dht nodes
        for(int i=6300; i < 6309 ; i++) {
            ep = new ExtendedEndpoint("18.239.0.100", i);
            ep.setDHTVersion(2);
            if((i % 2) == 0) {
                ep.setDHTMode(DHTMode.INACTIVE);
            } else {
                ep.setDHTMode(DHTMode.PASSIVE);
            }
            hostCatcher.add(ep, false);
        }
        
        List<ExtendedEndpoint> hostList = hostCatcher.getDHTSupportEndpoint(0);
        assertEquals(1, hostList.size());
        
        // add nodes from different class C networks
        for(int i=1; i < 9 ; i++) {
            ep = new ExtendedEndpoint("18.239."+i+".100", 6000);
            ep.setDHTVersion(2);
            if((i % 2) == 0) {
                ep.setDHTMode(DHTMode.INACTIVE);
            } else {
                ep.setDHTMode(DHTMode.PASSIVE);
            }
            hostCatcher.add(ep, false);
        }
        //dht active node
        ep = new ExtendedEndpoint("18.239.10.101", 6322);
        ep.setDHTVersion(1);
        ep.setDHTMode(DHTMode.ACTIVE);
        hostCatcher.add(ep, false);
        
        hostList = hostCatcher.getDHTSupportEndpoint(0);
        ep = hostList.get(0);
        assertEquals("18.239.10.101", ep.getAddress());
        assertTrue(ep.getDHTMode().equals(DHTMode.ACTIVE));
        
        hostList.remove(0);
        ep = hostList.iterator().next();
        assertTrue(ep.getDHTMode().equals(DHTMode.PASSIVE));
        ep = hostList.get(hostList.size()-1);
        assertFalse(ep.getDHTMode().equals(DHTMode.PASSIVE));
        assertTrue(ep.getDHTMode().equals(DHTMode.INACTIVE));
        
        //try excluding version
        hostList = hostCatcher.getDHTSupportEndpoint(2);
        assertEquals(9, hostList.size());
        assertTrue(hostList.get(0).getAddress().endsWith(".100"));
    }

    @SuppressWarnings("unchecked")
    public void testPingTagging() throws Exception {
        injector = LimeTestUtils.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(ConnectionManager.class).to(ConnectionManagerStub.class);
                bind(LocalSocketAddressProvider.class).toInstance(new LocalSocketAddressProvider() {
                    public byte[] getLocalAddress() {
                        return null;
                    }

                    public int getLocalPort() {
                        return 0;
                    }

                    public boolean isLocalAddressPrivate() {
                        return false;
                    };

                    public boolean isTLSCapable() {
                        return false;
                    }
                });
            }
        });
        
        hostCatcher = injector.getInstance(HostCatcher.class);
        hostCatcher.start();       
        
        PingReplyFactory pingReplyFactory = injector.getInstance(PingReplyFactory.class);
        Acceptor acceptor = injector.getInstance(Acceptor.class);
        MessageRouter messageRouter = injector.getInstance(MessageRouter.class);
        MessageFactory messageFactory = injector.getInstance(MessageFactory.class);
        UDPService udpService = injector.getInstance(UDPService.class);
        ConnectionManagerStub connectionManager = (ConnectionManagerStub)injector.getInstance(ConnectionManager.class);
        
        connectionManager.setFullyConnected(false);
        connectionManager.setInitializedConnections(Collections.EMPTY_LIST);
        connectionManager.setPreferredConnectionCount(1);
        
        ConnectionSettings.FILTER_CLASS_C.setValue(true);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        
        // tell hostcatcher about one node
        File tmp=new File(CommonUtils.getUserSettingsDir(), "gnutella.net" );
        FileWriter out=new FileWriter(tmp);
        out.write("127.0.0.1:6000,26694,1176133322250,1176133334390,,en,,,");    
        out.flush();
        out.close();
        DatagramSocket s = new DatagramSocket(6000);
        s.setSoTimeout(3000);
                
        // make it send udp pings
        acceptor.bindAndStartUpnp();
        acceptor.start();
        messageRouter.start();
        hostCatcher.expire();
        hostCatcher.sendUDPPings();
        
        // empty the hostcatcher
        StubEndpointObserver seo = new StubEndpointObserver();
        assertNotNull(hostCatcher.getAnEndpointImmediate(seo));
        assertNull(hostCatcher.getAnEndpointImmediate(seo));
        hostCatcher.removeEndpointObserver(seo);
        
        // receive the ping
        DatagramPacket p = new DatagramPacket(new byte[1000], 1000);
        s.receive(p);
        PingRequest ping = (PingRequest)messageFactory.read(new ByteArrayInputStream(p.getData()), Network.TCP);
        assertNotNull(ping);

        
        // this ping should be tagged
        byte [] expectedGUID = udpService.getSolicitedGUID().bytes();
        assertNotEquals(expectedGUID, ping.getGUID());
        
        // if a pong is sent from the same host, it will be processed
        byte [] payload = new byte[14];
        ByteUtils.short2leb((short)6000, payload, 0);
        System.arraycopy(InetAddress.getByName("127.0.0.1").getAddress(),0,payload,2,4);
        PingReply pong = 
            pingReplyFactory.createFromNetwork(ping.getGUID().clone(), (byte)1, (byte)1, payload, Message.Network.UDP);
        udpService.processMessage(pong, 
                new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 6000));
        
        // the pong guid should now be restored to the solicited guid
        assertEquals(expectedGUID,pong.getGUID());
        
        Thread.sleep(1000);
        Endpoint e = hostCatcher.getAnEndpointImmediate(new StubEndpointObserver());
        assertNotNull(e);
        assertEquals("127.0.0.1",e.getAddress());
        assertEquals(6000, e.getPort());
        
        // however if a pong with the same guid arrives from
        // another address, it will be ignored
        System.arraycopy(InetAddress.getByName("127.0.0.2").getAddress(),0,payload,2,4);
        pong = 
            pingReplyFactory.createFromNetwork(ping.getGUID().clone(), (byte)1, (byte)1, payload, Message.Network.UDP);
        udpService.processMessage(pong, 
                new InetSocketAddress(InetAddress.getByName("127.0.0.2"), 6000));
        
        // the guid of this pong will not be restored correctly
        assertNotEquals(expectedGUID,pong.getGUID());
        Thread.sleep(1000);
        assertEquals(0,hostCatcher.getNumHosts());

        tmp.delete();
    }
    
    public void testIsTLSCapable() throws Exception {
        PingReplyFactory pingReplyFactory = injector.getInstance(PingReplyFactory.class);
        
        assertEquals(0, hostCatcher.getNumHosts());
        ExtendedEndpoint p = new ExtendedEndpoint("231.123.254.1", 1);
        hostCatcher.add(p, true);
        assertFalse(hostCatcher.isHostTLSCapable(new IpPortImpl("231.123.254.1", 1)));
        assertFalse(hostCatcher.isHostTLSCapable(p));
        
        p = new ExtendedEndpoint("21.81.1.1", 1);
        p.setTLSCapable(true);
        hostCatcher.add(p, true);
        assertTrue(hostCatcher.isHostTLSCapable(new IpPortImpl("21.81.1.1", 1)));
        assertTrue(hostCatcher.isHostTLSCapable(p));
        
        p = new ExtendedEndpoint("1.2.3.101", 1);
        p.setTLSCapable(true);
        assertTrue(hostCatcher.isHostTLSCapable(p));
        
        // Hand-craft a PingReply w/ TLS IPPs to see if they're added as
        // TLS capable hosts.
        assertEquals(2, hostCatcher.getNumHosts());
        GGEP ggep = new GGEP();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(new byte[] { 1, 1, 1, 1, 1, 0 } );
        out.write(new byte[] { 1, 2, 3, 4, 2, 0 } );
        out.write(new byte[] { 3, 4, 2, 3, 3, 0 } );
        out.write(new byte[] { (byte)0xFE, 0, 0, 3, 4, 0 } );
        ggep.put(GGEPKeys.GGEP_HEADER_PACKED_IPPORTS, out.toByteArray());
        // mark the second & third items as TLS
        ggep.put(GGEPKeys.GGEP_HEADER_PACKED_IPPORTS_TLS, (0x40 | 0x20));
        ggep.put(GGEPKeys.GGEP_HEADER_TLS_CAPABLE); // mark this guy as TLS capable.
        PingReply pr = pingReplyFactory.create(
            GUID.makeGuid(), (byte)1, 1, new byte[] { 1, 0, 1, 0 },
            0, 0, false, ggep);
        hostCatcher.add(pr);
        assertEquals(7, hostCatcher.getNumHosts());
        assertFalse(hostCatcher.isHostTLSCapable(new IpPortImpl("1.1.1.1:1")));
        assertTrue(hostCatcher.isHostTLSCapable(new IpPortImpl("1.2.3.4:2")));
        assertTrue(hostCatcher.isHostTLSCapable(new IpPortImpl("3.4.2.3:3")));
        assertFalse(hostCatcher.isHostTLSCapable(new IpPortImpl("254.0.0.3:4")));
        assertTrue(hostCatcher.isHostTLSCapable(new IpPortImpl("1.0.1.0:1")));

        // The order of these checks are a little stricter than necessary
        Endpoint ep = hostCatcher.getAnEndpointImmediate(null);
        assertEquals("254.0.0.3", ep.getAddress());
        assertFalse(ep.isTLSCapable());
        
        ep = hostCatcher.getAnEndpointImmediate(null);
        assertEquals("3.4.2.3", ep.getAddress());
        assertTrue(ep.isTLSCapable());
        
        ep = hostCatcher.getAnEndpointImmediate(null);
        assertEquals("1.2.3.4", ep.getAddress());
        assertTrue(ep.isTLSCapable());
        
        ep = hostCatcher.getAnEndpointImmediate(null);
        assertEquals("1.1.1.1", ep.getAddress());
        assertFalse(ep.isTLSCapable());
        
        ep = hostCatcher.getAnEndpointImmediate(null);
        assertEquals("21.81.1.1", ep.getAddress());
        assertTrue(ep.isTLSCapable());
        
        ep = hostCatcher.getAnEndpointImmediate(null);
        assertEquals("231.123.254.1", ep.getAddress());
        assertFalse(ep.isTLSCapable());
        
        ep = hostCatcher.getAnEndpointImmediate(null);
        assertEquals("1.0.1.0", ep.getAddress());
        assertTrue(ep.isTLSCapable());
        
        assertNull(hostCatcher.getAnEndpointImmediate(null));
    }
    
    @Singleton
    private static class UDPHostCacheFactoryStub implements UDPHostCacheFactory {
        private final Provider<MessageRouter> messageRouter;
        private final PingRequestFactory pingRequestFactory;
        private final ConnectionServices connectionServices;
        private final Provider<HostCatcher> hostCatcher;
        private final NetworkInstanceUtils networkInstanceUtils;

        @Inject
        public UDPHostCacheFactoryStub(Provider<MessageRouter> messageRouter,
                PingRequestFactory pingRequestFactory, ConnectionServices connectionServices,
                Provider<HostCatcher> hostCatcher, NetworkInstanceUtils networkInstanceUtils) {
            this.messageRouter = messageRouter;
            this.pingRequestFactory = pingRequestFactory;
            this.connectionServices = connectionServices;
            this.hostCatcher = hostCatcher;
            this.networkInstanceUtils = networkInstanceUtils;
        }

        public UDPHostCache createUDPHostCache(UDPPinger pinger) {
            return new StubUDPBootstrapper(pinger, messageRouter, pingRequestFactory,
                    connectionServices, hostCatcher, networkInstanceUtils);
        }

        public UDPHostCache createUDPHostCache(long expiryTime, UDPPinger pinger) {
            throw new UnsupportedOperationException();
        }
        
    }
    
    private static class StubUDPBootstrapper extends UDPHostCache {
        
        private final Provider<HostCatcher> hostCatcher;
        
        public StubUDPBootstrapper(UDPPinger pinger, Provider<MessageRouter> messageRouter,
                PingRequestFactory pingRequestFactory, ConnectionServices connectionServices,
                Provider<HostCatcher> hostCatcher, NetworkInstanceUtils networkInstanceUtils) {
            super(pinger, messageRouter, pingRequestFactory, connectionServices,
                    networkInstanceUtils);
            this.hostCatcher = hostCatcher;
        }

        private boolean fetched = false;
        private String host = "143.123.234.132";
        private boolean expired = false;
        
        
        public boolean fetchHosts() {
            if(expired)
                return false;
            expired = true;
            fetched = true;
            Endpoint ep = new Endpoint(host, 6346);
            hostCatcher.get().add(ep, false);
            return true;
        }
        
        public int getSize() {
            if(expired)
                return 0;
            else
                return 1;
        }
    }
}
