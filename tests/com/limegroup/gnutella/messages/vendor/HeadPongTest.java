package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.collection.BitNumbers;
import org.limewire.collection.Range;
import org.limewire.core.settings.UploadSettings;
import org.limewire.io.BadGGEPBlockException;
import org.limewire.io.BadGGEPPropertyException;
import org.limewire.io.Connectable;
import org.limewire.io.GGEP;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.io.IpPortSet;
import org.limewire.io.NetworkUtils;
import org.limewire.util.ByteUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.PushEndpointFactory;
import com.limegroup.gnutella.UploadManager;
import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;
import com.limegroup.gnutella.altlocs.PushAltLoc;
import com.limegroup.gnutella.helpers.UrnHelper;
import com.limegroup.gnutella.library.FileDescStub;
import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.library.FileManagerStub;
import com.limegroup.gnutella.library.GnutellaFileListStub;
import com.limegroup.gnutella.library.IncompleteFileDescStub;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.stubs.NetworkManagerStub;
import com.limegroup.gnutella.stubs.UploadManagerStub;
import com.limegroup.gnutella.util.LimeTestCase;

public class HeadPongTest extends LimeTestCase {
        
    private FileManagerStub fileManager;
    private HeadPongFactory headPongFactory;
    private Injector injector;
    private Mockery mockery;
    private DownloadManager downloadManager;
    private NetworkManagerStub networkManager;

    public HeadPongTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(HeadPongTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void setUp() throws Exception {
        mockery = new Mockery();
        downloadManager = mockery.mock(DownloadManager.class);
                
        injector = LimeTestUtils.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(FileManager.class).to(FileManagerStub.class);
                bind(UploadManager.class).to(UploadManagerStub.class);
                bind(NetworkManager.class).to(NetworkManagerStub.class);
                bind(DownloadManager.class).toInstance(downloadManager);
            }
        });
        
        headPongFactory = injector.getInstance(HeadPongFactory.class);       
        fileManager = (FileManagerStub)injector.getInstance(FileManager.class);
        networkManager = (NetworkManagerStub) injector.getInstance(NetworkManager.class);
        networkManager.setAcceptedIncomingConnection(true);
        networkManager.setIncomingTLSEnabled(false);
    }
    
    public void tearDown() throws Exception {
        mockery.assertIsSatisfied();
        
        // TODO ?
        //SSLSettings.TLS_INCOMING.revertToDefault();
        //altLocManager.purge();
    }
    
    public void testReadBinary() throws Exception {
        PushEndpointFactory pushEndpointFactory = injector.getInstance(PushEndpointFactory.class);
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        out.write(0x1 | 0x2 | 0x4 | 0x8);              // features (intervals, alt locs, push alt locs, fwt alt locs)        
        out.write(0x2 | 0x8);                          // codes    (partial file, downloading)
        out.write(new byte[] { 'S', 'A', 'M', 'B' });  // vendor
        out.write(-1);                                 // queue status
        
        ByteUtils.short2beb((short)24, out);           // length of forthcoming ranges.
        ByteUtils.int2beb(1, out);                     // ranges....
        ByteUtils.int2beb(1000, out);
        ByteUtils.int2beb(1050, out);
        ByteUtils.int2beb(2001, out);
        ByteUtils.int2beb(2500, out);
        ByteUtils.int2beb(2525, out);                  //...ranges (1-1000, 1050-2001, 2500-2525)
        
        Random random = new Random();
        byte[] g1 = new byte[16];
        random.nextBytes(g1);
        PushEndpoint p1 = pushEndpointFactory.createPushEndpoint(g1, new IpPortSet(new IpPortImpl("1.2.3.4:5")), PushEndpoint.PLAIN, 0);
        byte[] p1b = p1.toBytes(false);
        byte[] g2 = new byte[16];
        random.nextBytes(g2);
        PushEndpoint p2 = pushEndpointFactory.createPushEndpoint(g2, new IpPortSet(new IpPortImpl("2.3.4.5:6"), new IpPortImpl("3.4.5.6:7")), PushEndpoint.PLAIN, 0);
        byte[] p2b = p2.toBytes(false);
        ByteUtils.short2beb((short)(p1b.length + p2b.length), out); // length of forthcoming push locations.
        out.write(p1b);                                // push locations...
        out.write(p2b);                                // ...push locations
        
        ByteUtils.short2beb((short)18, out);           // length of forthcoming alternate locations.
        out.write(new byte[] { 4, 5, 6, 7, 8, 0, 5, 6, 7, 8, 9, 0, 6, 7, 8, 9, 10, 0 } ); // alternate locations
        
        HeadPong pong = headPongFactory.createFromNetwork(new byte[16], (byte)0,
                (byte)0, 1, out.toByteArray(), Network.UNKNOWN);
        assertTrue(pong.hasFile());
        assertFalse(pong.hasCompleteFile());
        Iterator<Range> iterator = pong.getRanges().iterator();
        assertEquals(Range.createRange(1, 1000), iterator.next());
        assertEquals(Range.createRange(1050, 2001), iterator.next());
        assertEquals(Range.createRange(2500, 2525), iterator.next());
        assertFalse(iterator.hasNext());
        Set<IpPort> expectedLocs = new IpPortSet(new IpPortImpl("4.5.6.7:8"), new IpPortImpl("5.6.7.8:9"), new IpPortImpl("6.7.8.9:10"));
        assertEquals(expectedLocs, pong.getAltLocs());
        Set<PushEndpoint> pushLocs = pong.getPushLocs();
        assertEquals(2, pushLocs.size());
        Iterator<PushEndpoint> peIterator = pushLocs.iterator();
        PushEndpoint read1 = peIterator.next();
        PushEndpoint read2 = peIterator.next();
        assertFalse(peIterator.hasNext());
        PushEndpoint m1, m2;
        // figure out which order the set put the PEs in...
        if(Arrays.equals(read1.getClientGUID(), g1)) {
            m1 = read1;
            m2 = read2;
        } else {
            m1 = read2;
            m2 = read1;
        }
        assertEquals(g1, m1.getClientGUID());
        assertEquals(g2, m2.getClientGUID());
        assertEquals(1, m1.getProxies().size());
        assertEquals(2, m2.getProxies().size());
        
        assertFalse(pong.isTLSCapable());
        assertEquals("SAMB", pong.getVendor());
        assertFalse(pong.isFirewalled());
        assertEquals(-1, pong.getQueueStatus());
        assertFalse(pong.isBusy());
        assertTrue(pong.isDownloading());
        assertFalse(pong.isRoutingBroken());        
        
        // Just a quick test, since we already have the outputstream built up, to make
        // sure binary won't work if version != 1
        try {
            headPongFactory.createFromNetwork(new byte[16], (byte)0,
                    (byte)0, 2, out.toByteArray(), Network.UNKNOWN);
            fail("expected bpe!");
        } catch(BadPacketException bpe) {
            assertInstanceof(BadGGEPBlockException.class, bpe.getCause());
        }
    }
    
    public void testReadGGEP() throws Exception {
        PushEndpointFactory pushEndpointFactory = injector.getInstance(PushEndpointFactory.class);
        
        GGEP ggep = new GGEP();
        ggep.put("F", (byte)0x1);                               // TLS capable.
        ggep.put("C", (byte)0x2 | 0x8);                         // response code (partial, downloading)
        ggep.put("V", new byte[] { 'S', 'A', 'M', 'B' } );      // vendor
        ggep.put("Q", (byte)-1);                                // queue status.
        
        byte[] ranges = new byte[24];        
        ByteUtils.int2beb(1,    ranges, 0);
        ByteUtils.int2beb(1000, ranges, 4);
        ByteUtils.int2beb(1050, ranges, 8);
        ByteUtils.int2beb(2001, ranges, 12);
        ByteUtils.int2beb(2500, ranges, 16);
        ByteUtils.int2beb(2525, ranges, 20);  
        ggep.put("R", ranges);                                  // ranges
        
        Random random = new Random();
        byte[] g1 = new byte[16];
        random.nextBytes(g1);
        PushEndpoint p1 = pushEndpointFactory.createPushEndpoint(g1, new IpPortSet(new IpPortImpl("1.2.3.4:5")), PushEndpoint.PLAIN, 0);
        byte[] p1b = p1.toBytes(false);
        byte[] g2 = new byte[16];
        random.nextBytes(g2);
        PushEndpoint p2 = pushEndpointFactory.createPushEndpoint(g2, new IpPortSet(new IpPortImpl("2.3.4.5:6"), new IpPortImpl("3.4.5.6:7")), PushEndpoint.PLAIN, 0);
        byte[] p2b = p2.toBytes(false);
        byte[] pushbytes = new byte[p1b.length + p2b.length];
        System.arraycopy(p1b, 0, pushbytes, 0, p1b.length);
        System.arraycopy(p2b, 0, pushbytes, p1b.length, p2b.length);
        ggep.put("P", pushbytes);                               // push locations.
        
        ggep.put("A", new byte[] { 4, 5, 6, 7, 8, 0, 5, 6, 7, 8, 9, 0, 6, 7, 8, 9, 10, 0 } ); // alternate locations
        ggep.put("T", new byte[] { 0x60 } );                      // TLS indexes into alternate locations
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ggep.write(out);
        
        HeadPong pong = headPongFactory.createFromNetwork(new byte[16], (byte)0,
                (byte)0, 2, out.toByteArray(), Network.UNKNOWN);
        assertTrue(pong.hasFile());
        assertFalse(pong.hasCompleteFile());
        Iterator<Range> iterator = pong.getRanges().iterator();
        assertEquals(Range.createRange(1, 1000), iterator.next());
        assertEquals(Range.createRange(1050, 2001), iterator.next());
        assertEquals(Range.createRange(2500, 2525), iterator.next());
        assertFalse(iterator.hasNext());
        Set<IpPort> expectedLocs = new IpPortSet(new IpPortImpl("4.5.6.7:8"), new IpPortImpl("5.6.7.8:9"), new IpPortImpl("6.7.8.9:10"));
        assertEquals(expectedLocs, pong.getAltLocs());
        Set<IpPort> expectedTLS = new IpPortSet(new IpPortImpl("5.6.7.8:9"), new IpPortImpl("6.7.8.9:10"));
        for(IpPort ipp : pong.getAltLocs()) {
            if(ipp instanceof Connectable && ((Connectable)ipp).isTLSCapable())
                assertTrue(expectedTLS.remove(ipp));
        }
        assertTrue("still had: " + expectedTLS, expectedTLS.isEmpty());
        Set<PushEndpoint> pushLocs = pong.getPushLocs();
        assertEquals(2, pushLocs.size());
        Iterator<PushEndpoint> peIterator = pushLocs.iterator();
        PushEndpoint read1 = peIterator.next();
        PushEndpoint read2 = peIterator.next();
        assertFalse(peIterator.hasNext());
        PushEndpoint m1, m2;
        // figure out which order the set put the PEs in...
        if(Arrays.equals(read1.getClientGUID(), g1)) {
            m1 = read1;
            m2 = read2;
        } else {
            m1 = read2;
            m2 = read1;
        }
        assertEquals(g1, m1.getClientGUID());
        assertEquals(g2, m2.getClientGUID());
        assertEquals(1, m1.getProxies().size());
        assertEquals(2, m2.getProxies().size());
        
        assertTrue(pong.isTLSCapable());
        assertEquals("SAMB", pong.getVendor());
        assertFalse(pong.isFirewalled());
        assertEquals(-1, pong.getQueueStatus());
        assertFalse(pong.isBusy());
        assertTrue(pong.isDownloading());
        assertFalse(pong.isRoutingBroken());        
        
        // Just a quick test, since we already have the output stream built up, to make
        // sure binary won't work if version < 2
        try {
            headPongFactory.createFromNetwork(new byte[16], (byte)0,
                    (byte)0, 1, out.toByteArray(), Network.UNKNOWN);
            fail("expected bpe!");
        } catch(BadPacketException expected) {}
    }
    
    public void testHigherVersionGGEP() throws Exception {
        PushEndpointFactory pushEndpointFactory = injector.getInstance(PushEndpointFactory.class);
        
        GGEP ggep = new GGEP();
        ggep.put("F", (byte)0x1);                               // TLS capable.
        ggep.put("C", (byte)0x2 | 0x8);                         // response code (partial, downloading)
        ggep.put("V", new byte[] { 'S', 'A', 'M', 'B' } );      // vendor
        ggep.put("Q", (byte)-1);                                // queue status.
        
        byte[] ranges = new byte[24];        
        ByteUtils.int2beb(1,    ranges, 0);
        ByteUtils.int2beb(1000, ranges, 4);
        ByteUtils.int2beb(1050, ranges, 8);
        ByteUtils.int2beb(2001, ranges, 12);
        ByteUtils.int2beb(2500, ranges, 16);
        ByteUtils.int2beb(2525, ranges, 20);  
        ggep.put("R", ranges);                                  // ranges
        
        Random random = new Random();
        byte[] g1 = new byte[16];
        random.nextBytes(g1);
        PushEndpoint p1 = pushEndpointFactory.createPushEndpoint(g1, new IpPortSet(new IpPortImpl("1.2.3.4:5")), PushEndpoint.PLAIN, 0);
        byte[] p1b = p1.toBytes(false);
        byte[] g2 = new byte[16];
        random.nextBytes(g2);
        PushEndpoint p2 = pushEndpointFactory.createPushEndpoint(g2, new IpPortSet(new IpPortImpl("2.3.4.5:6"), new IpPortImpl("3.4.5.6:7")), PushEndpoint.PLAIN, 0);
        byte[] p2b = p2.toBytes(false);
        byte[] pushbytes = new byte[p1b.length + p2b.length];
        System.arraycopy(p1b, 0, pushbytes, 0, p1b.length);
        System.arraycopy(p2b, 0, pushbytes, p1b.length, p2b.length);
        ggep.put("P", pushbytes);                               // push locations.
        
        ggep.put("A", new byte[] { 4, 5, 6, 7, 8, 0, 5, 6, 7, 8, 9, 0, 6, 7, 8, 9, 10, 0 } ); // alternate locations
        ggep.put("T", new byte[] { 0x60 } );                      // TLS indexes into alternate locations
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ggep.write(out);
        
        HeadPong pong = headPongFactory.createFromNetwork(new byte[16], (byte)0,
                (byte)0, 3, out.toByteArray(), Network.UNKNOWN);
        assertTrue(pong.hasFile());
        assertFalse(pong.hasCompleteFile());
        Iterator<Range> iterator = pong.getRanges().iterator();
        assertEquals(Range.createRange(1, 1000), iterator.next());
        assertEquals(Range.createRange(1050, 2001), iterator.next());
        assertEquals(Range.createRange(2500, 2525), iterator.next());
        assertFalse(iterator.hasNext());
        Set<IpPort> expectedLocs = new IpPortSet(new IpPortImpl("4.5.6.7:8"), new IpPortImpl("5.6.7.8:9"), new IpPortImpl("6.7.8.9:10"));
        assertEquals(expectedLocs, pong.getAltLocs());
        Set<IpPort> expectedTLS = new IpPortSet(new IpPortImpl("5.6.7.8:9"), new IpPortImpl("6.7.8.9:10"));
        for(IpPort ipp : pong.getAltLocs()) {
            if(ipp instanceof Connectable && ((Connectable)ipp).isTLSCapable())
                assertTrue(expectedTLS.remove(ipp));
        }
        assertTrue(expectedTLS.isEmpty());
        Set<PushEndpoint> pushLocs = pong.getPushLocs();
        assertEquals(2, pushLocs.size());
        Iterator<PushEndpoint> peIterator = pushLocs.iterator();
        PushEndpoint read1 = peIterator.next();
        PushEndpoint read2 = peIterator.next();
        assertFalse(peIterator.hasNext());
        PushEndpoint m1, m2;
        // figure out which order the set put the PEs in...
        if(Arrays.equals(read1.getClientGUID(), g1)) {
            m1 = read1;
            m2 = read2;
        } else {
            m1 = read2;
            m2 = read1;
        }
        assertEquals(g1, m1.getClientGUID());
        assertEquals(g2, m2.getClientGUID());
        assertEquals(1, m1.getProxies().size());
        assertEquals(2, m2.getProxies().size());
        
        assertTrue(pong.isTLSCapable());
        assertEquals("SAMB", pong.getVendor());
        assertFalse(pong.isFirewalled());
        assertEquals(-1, pong.getQueueStatus());
        assertFalse(pong.isBusy());
        assertTrue(pong.isDownloading());
        assertFalse(pong.isRoutingBroken());  
    }
    
    public void testGGEP404() throws Exception {
        GGEP ggep = new GGEP();
        ggep.put("C", (byte)0);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ggep.write(out);
        
        HeadPong pong = headPongFactory.createFromNetwork(new byte[16], (byte)0,
                (byte)0, 2, out.toByteArray(), Network.UNKNOWN);
        assertFalse(pong.hasFile());
        
        assertNull(pong.getRanges());
        assertNull(pong.getVendor());
        assertEmpty(pong.getAltLocs());
        assertEmpty(pong.getPushLocs());
        assertFalse(pong.isBusy());
        assertFalse(pong.hasCompleteFile());
        assertFalse(pong.isDownloading());
        assertFalse(pong.isFirewalled());
        assertFalse(pong.isRoutingBroken());
        assertEquals(0, pong.getQueueStatus());
    }
    
    public void testOldRoutingBrokenClientsAreMarked() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(HeadPing.GGEP_PING); // old client didn't unmask the GGEP Ping flag.
        out.write(0); // 404.
        HeadPong pong = headPongFactory.createFromNetwork(new byte[16], (byte)0,
                (byte)0, 1, out.toByteArray(), Network.UNKNOWN);
        assertTrue(pong.isRoutingBroken());
    }
    
    public void testTLSCapableAndUnknownFeatures() throws Exception {
        GGEP ggep = new GGEP();
        // required fields.
        ggep.put("C", (byte)0x1);
        ggep.put("V", new byte[4]);
        ggep.put("Q", (byte)0);
        
        ggep.put("F", (byte)0xFF); // tls capable + a lot of unknown fields
        
        HeadPong pong = headPongFactory.createFromNetwork(new byte[16], (byte)0,
                (byte)0, 2, arrayof(ggep), Network.UNKNOWN);
        assertTrue(pong.isTLSCapable());
    }
    
    public void testNotTLSCapableButOtherFeatures() throws Exception {
        GGEP ggep = new GGEP();
        // required fields.
        ggep.put("C", (byte)0x1);
        ggep.put("V", new byte[4]);
        ggep.put("Q", (byte)0);
        
        ggep.put("F", (byte)~0x1); // every feature but TLS capable
        
        HeadPong pong = headPongFactory.createFromNetwork(new byte[16], (byte)0,
                (byte)0, 2, arrayof(ggep), Network.UNKNOWN);
        assertFalse(pong.isTLSCapable());
    }
    
    public void testNotTLSCapableBecauseOfLackOfFeatureKey() throws Exception {
        GGEP ggep = new GGEP();
        // required fields.
        ggep.put("C", (byte)0x1);
        ggep.put("V", new byte[4]);
        ggep.put("Q", (byte)0);
        
        HeadPong pong = headPongFactory.createFromNetwork(new byte[16], (byte)0,
                (byte)0, 2, arrayof(ggep), Network.UNKNOWN);
        assertFalse(pong.isTLSCapable());
    }
    
    public void testQueueRequired() throws Exception {
        GGEP ggep = new GGEP();
        // required fields.
        ggep.put("C", (byte)0x1);
        ggep.put("V", new byte[4]);
        
        try {
            headPongFactory.createFromNetwork(new byte[16], (byte)0,
                    (byte)0, 2, arrayof(ggep), Network.UNKNOWN);
            fail("expected exception!");
        } catch(BadPacketException bpe) {}
    }
    
    public void testVendorRequired() throws Exception {
        GGEP ggep = new GGEP();
        // required fields.
        ggep.put("C", (byte)0x1);
        ggep.put("Q", (byte)0);
        
        try {
            headPongFactory.createFromNetwork(new byte[16], (byte)0,
                    (byte)0, 2, arrayof(ggep), Network.UNKNOWN);
            fail("expected exception!");
        } catch(BadPacketException bpe) {}
    }
    
    public void testCodeRequired() throws Exception {
        GGEP ggep = new GGEP();
        // required fields.
        ggep.put("V", new byte[4]);
        ggep.put("Q", (byte)0);
        
        try {
            headPongFactory.createFromNetwork(new byte[16], (byte)0,
                    (byte)0, 2, arrayof(ggep), Network.UNKNOWN);
            fail("expected exception!");
        } catch(BadPacketException bpe) {}        
    }
    
    public void testEmptyQueueFails() throws Exception {
        GGEP ggep = new GGEP();
        // required fields.
        ggep.put("C", (byte)0x1);
        ggep.put("V", new byte[4]);
        ggep.put("Q", new byte[0]);
        
        try {
            headPongFactory.createFromNetwork(new byte[16], (byte)0,
                    (byte)0, 2, arrayof(ggep), Network.UNKNOWN);
            fail("expected exception!");
        } catch(BadPacketException bpe) {}
    }
    
    public void testEmptyVendorFails() throws Exception {
        GGEP ggep = new GGEP();
        // required fields.
        ggep.put("C", (byte)0x1);
        ggep.put("V", new byte[0]);
        ggep.put("Q", (byte)0);
        
        try {
            headPongFactory.createFromNetwork(new byte[16], (byte)0,
                    (byte)0, 2, arrayof(ggep), Network.UNKNOWN);
            fail("expected exception!");
        } catch(BadPacketException bpe) {}
    }
    
    public void testEmptyCodeFails() throws Exception {
        GGEP ggep = new GGEP();
        // required fields.
        ggep.put("C", new byte[0]);
        ggep.put("V", new byte[4]);
        ggep.put("Q", (byte)0);
        
        try {
            headPongFactory.createFromNetwork(new byte[16], (byte)0,
                    (byte)0, 2, arrayof(ggep), Network.UNKNOWN);
            fail("expected exception!");
        } catch(BadPacketException bpe) {}
    }
    
    public void testWriteBasicGGEPHeadPong() throws Exception {
        byte[] guid = new byte[16];
        Random r = new Random();
        r.nextBytes(guid);
        
        MockHeadPongRequestor req = new MockHeadPongRequestor();
        req.setPongGGEPCapable(true);
        req.setUrn(UrnHelper.SHA1);
        req.setGuid(guid);
        
        networkManager.setIncomingTLSEnabled(false);
        networkManager.setAcceptedIncomingConnection(true);
        FileDescStub fd = new FileDescStub("file", UrnHelper.SHA1, 0);
        fileManager.getGnutellaFileList().add(fd);
        int expectedUploads = -UploadSettings.HARD_MAX_UPLOADS.getValue();
        
        HeadPong pong = headPongFactory.create(req);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        pong.write(out);
        byte[] written = out.toByteArray();
        
        assertEquals(guid, written, 0, 16);
        // 16...23, rest of Gnutella header, ignore.
        assertEquals("LIME", new String(written, 23, 4));   // headpong vendor ID
        assertEquals(24, ByteUtils.leb2short(written, 27)); // headpong selector
        assertEquals(2, ByteUtils.leb2short(written, 29));  // current headpong version
        
        int[] endOffset = new int[1];
        GGEP writtenGGEP = new GGEP(written, 31, endOffset);
        assertEquals(written.length, endOffset[0]);
        
        assertEquals("got headers: " + writtenGGEP.getHeaders(), 3, writtenGGEP.getHeaders().size());
        assertEquals(new byte[] { 0x1 }, writtenGGEP.getBytes("C"));
        assertEquals(new byte[] { (byte)expectedUploads }, writtenGGEP.getBytes("Q"));
        assertEquals("LIME".getBytes(), writtenGGEP.getBytes("V"));        
    }
    
    public void testWriteTLS() throws Exception {
        NetworkManagerStub networkManager = (NetworkManagerStub)injector.getInstance(NetworkManager.class);
        byte[] guid = new byte[16];
        Random r = new Random();
        r.nextBytes(guid);
        
        MockHeadPongRequestor req = new MockHeadPongRequestor();
        req.setPongGGEPCapable(true);
        req.setUrn(UrnHelper.SHA1);
        req.setGuid(guid);
        
        networkManager.setIncomingTLSEnabled(true);
        networkManager.setAcceptedIncomingConnection(true);
        networkManager.setTls(true);
        networkManager.setIncomingTLSEnabled(true);
        networkManager.setOutgoingTLSEnabled(true);
        FileDescStub fd = new FileDescStub("file", UrnHelper.SHA1, 0);
        fileManager.getGnutellaFileList().add(fd);
        int expectedUploads = -UploadSettings.HARD_MAX_UPLOADS.getValue();
        
        HeadPong pong = headPongFactory.create(req);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        pong.write(out);
        byte[] written = out.toByteArray();
        
        assertEquals(guid, written, 0, 16);
        // 16...23, rest of Gnutella header, ignore.
        assertEquals("LIME", new String(written, 23, 4));   // headpong vendor ID
        assertEquals(24, ByteUtils.leb2short(written, 27)); // headpong selector
        assertEquals(2, ByteUtils.leb2short(written, 29));  // current headpong version
        
        int[] endOffset = new int[1];
        GGEP writtenGGEP = new GGEP(written, 31, endOffset);
        assertEquals(written.length, endOffset[0]);
        
        assertEquals("got headers: " + writtenGGEP.getHeaders(), 4, writtenGGEP.getHeaders().size());
        assertEquals(new byte[] { 0x1 }, writtenGGEP.getBytes("C"));
        assertEquals(new byte[] { (byte)expectedUploads }, writtenGGEP.getBytes("Q"));
        assertEquals("LIME".getBytes(), writtenGGEP.getBytes("V"));
        assertEquals(new byte[] { 0x1 }, writtenGGEP.getBytes("F"));
    }
    
    public void testFirewalledBitAndDoesntWriteTLSIfFirewalled() throws Exception {
        NetworkManagerStub networkManager = (NetworkManagerStub)injector.getInstance(NetworkManager.class);
        byte[] guid = new byte[16];
        Random r = new Random();
        r.nextBytes(guid);
        
        MockHeadPongRequestor req = new MockHeadPongRequestor();
        req.setPongGGEPCapable(true);
        req.setUrn(UrnHelper.SHA1);
        req.setGuid(guid);
        
        networkManager.setIncomingTLSEnabled(true);
        networkManager.setAcceptedIncomingConnection(false);
        FileDescStub fd = new FileDescStub("file", UrnHelper.SHA1, 0);
        fileManager.getGnutellaFileList().add(fd);
        int expectedUploads = -UploadSettings.HARD_MAX_UPLOADS.getValue();
        
        HeadPong pong = headPongFactory.create(req);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        pong.write(out);
        byte[] written = out.toByteArray();
        
        assertEquals(guid, written, 0, 16);
        // 16...23, rest of Gnutella header, ignore.
        assertEquals("LIME", new String(written, 23, 4));   // headpong vendor ID
        assertEquals(24, ByteUtils.leb2short(written, 27)); // headpong selector
        assertEquals(2, ByteUtils.leb2short(written, 29));  // current headpong version
        
        int[] endOffset = new int[1];
        GGEP writtenGGEP = new GGEP(written, 31, endOffset);
        assertEquals(written.length, endOffset[0]);
        
        assertEquals("got headers: " + writtenGGEP.getHeaders(), 3, writtenGGEP.getHeaders().size());
        assertEquals(new byte[] { 0x1 | 0x4 }, writtenGGEP.getBytes("C"));
        assertEquals(new byte[] { (byte)expectedUploads }, writtenGGEP.getBytes("Q"));
        assertEquals("LIME".getBytes(), writtenGGEP.getBytes("V"));
    }
    
    public void testWritingFNF() throws Exception {
        byte[] guid = new byte[16];
        Random r = new Random();
        r.nextBytes(guid);
        
        MockHeadPongRequestor req = new MockHeadPongRequestor();
        req.setPongGGEPCapable(true);
        req.setUrn(GnutellaFileListStub.DEFAULT_URN);
        req.setGuid(guid);
                
        HeadPong pong = headPongFactory.create(req);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        pong.write(out);
        byte[] written = out.toByteArray();
        
        assertEquals(guid, written, 0, 16);
        // 16...23, rest of Gnutella header, ignore.
        assertEquals("LIME", new String(written, 23, 4));   // headpong vendor ID
        assertEquals(24, ByteUtils.leb2short(written, 27)); // headpong selector
        assertEquals(2, ByteUtils.leb2short(written, 29));  // current headpong version
        
        int[] endOffset = new int[1];
        GGEP writtenGGEP = new GGEP(written, 31, endOffset);
        assertEquals(written.length, endOffset[0]);
        
        assertEquals("got headers: " + writtenGGEP.getHeaders(), 1, writtenGGEP.getHeaders().size());
        assertEquals(new byte[] { 0x0 }, writtenGGEP.getBytes("C"));
    }
    
    public void testWritingIncompleteCode() throws Exception {
        byte[] guid = new byte[16];
        Random r = new Random();
        r.nextBytes(guid);
        
        MockHeadPongRequestor req = new MockHeadPongRequestor();
        req.setPongGGEPCapable(true);
        req.setUrn(UrnHelper.SHA1);
        req.setGuid(guid);
        
        final IncompleteFileDescStub fd = new IncompleteFileDescStub("test", UrnHelper.SHA1, 100);
        fileManager.getIncompleteFileList().add(fd);
        int expectedUploads = -UploadSettings.HARD_MAX_UPLOADS.getValue();
        mockery.checking(new Expectations() {{
            atLeast(1).of(downloadManager).isActivelyDownloading(with(same(fd.getSHA1Urn())));
            will(returnValue(false));
        }});
        
        HeadPong pong = headPongFactory.create(req);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        pong.write(out);
        byte[] written = out.toByteArray();
        
        assertEquals(guid, written, 0, 16);
        // 16...23, rest of Gnutella header, ignore.
        assertEquals("LIME", new String(written, 23, 4));   // headpong vendor ID
        assertEquals(24, ByteUtils.leb2short(written, 27)); // headpong selector
        assertEquals(2, ByteUtils.leb2short(written, 29));  // current headpong version
        
        int[] endOffset = new int[1];
        GGEP writtenGGEP = new GGEP(written, 31, endOffset);
        assertEquals(written.length, endOffset[0]);
        
        assertEquals("got headers: " + writtenGGEP.getHeaders(), 3, writtenGGEP.getHeaders().size());
        assertEquals(new byte[] { 0x2 }, writtenGGEP.getBytes("C"));
        assertEquals(new byte[] { (byte)expectedUploads }, writtenGGEP.getBytes("Q"));
        assertEquals("LIME".getBytes(), writtenGGEP.getBytes("V"));
    }
    
    public void testWritingIncompleteAndDownloadingCode() throws Exception {
        byte[] guid = new byte[16];
        Random r = new Random();
        r.nextBytes(guid);
        
        MockHeadPongRequestor req = new MockHeadPongRequestor();
        req.setPongGGEPCapable(true);
        req.setUrn(UrnHelper.SHA1);
        req.setGuid(guid);
        
        final IncompleteFileDescStub fd = new IncompleteFileDescStub("test", UrnHelper.SHA1, 100);
        mockery.checking(new Expectations() {{
            atLeast(1).of(downloadManager).isActivelyDownloading(with(same(fd.getSHA1Urn())));
            will(returnValue(true));
        }});
        fileManager.getIncompleteFileList().add(fd);
        int expectedUploads = -UploadSettings.HARD_MAX_UPLOADS.getValue();
        
        HeadPong pong = headPongFactory.create(req);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        pong.write(out);
        byte[] written = out.toByteArray();
        
        assertEquals(guid, written, 0, 16);
        // 16...23, rest of Gnutella header, ignore.
        assertEquals("LIME", new String(written, 23, 4));   // headpong vendor ID
        assertEquals(24, ByteUtils.leb2short(written, 27)); // headpong selector
        assertEquals(2, ByteUtils.leb2short(written, 29));  // current headpong version
        
        int[] endOffset = new int[1];
        GGEP writtenGGEP = new GGEP(written, 31, endOffset);
        assertEquals(written.length, endOffset[0]);
        
        assertEquals("got headers: " + writtenGGEP.getHeaders(), 3, writtenGGEP.getHeaders().size());
        assertEquals(new byte[] { 0x2 | 0x8 }, writtenGGEP.getBytes("C"));
        assertEquals(new byte[] { (byte)expectedUploads }, writtenGGEP.getBytes("Q"));
        assertEquals("LIME".getBytes(), writtenGGEP.getBytes("V"));
    }
    
    public void testWriteBusyQueueStatus() throws Exception {
        UploadManagerStub uploadManager = (UploadManagerStub)injector.getInstance(UploadManager.class);
        byte[] guid = new byte[16];
        Random r = new Random();
        r.nextBytes(guid);
        
        MockHeadPongRequestor req = new MockHeadPongRequestor();
        req.setPongGGEPCapable(true);
        req.setUrn(UrnHelper.SHA1);
        req.setGuid(guid);
        
        FileDescStub fd = new FileDescStub("file", UrnHelper.SHA1, 0);
        fileManager.getGnutellaFileList().add(fd);
        uploadManager.setNumQueuedUploads(UploadSettings.UPLOAD_QUEUE_SIZE.getValue());
        
        HeadPong pong = headPongFactory.create(req);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        pong.write(out);
        byte[] written = out.toByteArray();
        
        assertEquals(guid, written, 0, 16);
        // 16...23, rest of Gnutella header, ignore.
        assertEquals("LIME", new String(written, 23, 4));   // headpong vendor ID
        assertEquals(24, ByteUtils.leb2short(written, 27)); // headpong selector
        assertEquals(2, ByteUtils.leb2short(written, 29));  // current headpong version
        
        int[] endOffset = new int[1];
        GGEP writtenGGEP = new GGEP(written, 31, endOffset);
        assertEquals(written.length, endOffset[0]);
        
        assertEquals("got headers: " + writtenGGEP.getHeaders(), 3, writtenGGEP.getHeaders().size());
        assertEquals(new byte[] { 0x1 }, writtenGGEP.getBytes("C"));
        assertEquals(new byte[] { 0x7F }, writtenGGEP.getBytes("Q"));
        assertEquals("LIME".getBytes(), writtenGGEP.getBytes("V"));
    }
    
    public void testWriteCorrectQueueStatus() throws Exception {
        UploadManagerStub uploadManager = (UploadManagerStub)injector.getInstance(UploadManager.class);
        byte[] guid = new byte[16];
        Random r = new Random();
        r.nextBytes(guid);
        
        MockHeadPongRequestor req = new MockHeadPongRequestor();
        req.setPongGGEPCapable(true);
        req.setUrn(UrnHelper.SHA1);
        req.setGuid(guid);
        
        FileDescStub fd = new FileDescStub("file", UrnHelper.SHA1, 0);
        fileManager.getGnutellaFileList().add(fd);
        uploadManager.setNumQueuedUploads(3);
        
        HeadPong pong = headPongFactory.create(req);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        pong.write(out);
        byte[] written = out.toByteArray();
        
        assertEquals(guid, written, 0, 16);
        // 16...23, rest of Gnutella header, ignore.
        assertEquals("LIME", new String(written, 23, 4));   // headpong vendor ID
        assertEquals(24, ByteUtils.leb2short(written, 27)); // headpong selector
        assertEquals(2, ByteUtils.leb2short(written, 29));  // current headpong version
        
        int[] endOffset = new int[1];
        GGEP writtenGGEP = new GGEP(written, 31, endOffset);
        assertEquals(written.length, endOffset[0]);
        
        assertEquals("got headers: " + writtenGGEP.getHeaders(), 3, writtenGGEP.getHeaders().size());
        assertEquals(new byte[] { 0x1 }, writtenGGEP.getBytes("C"));
        assertEquals(new byte[] { 3 }, writtenGGEP.getBytes("Q"));
        assertEquals("LIME".getBytes(), writtenGGEP.getBytes("V"));
    }
    
    public void testWriteCorrectUploadsInProgressQueueStatus() throws Exception {
        UploadManagerStub uploadManager = (UploadManagerStub)injector.getInstance(UploadManager.class);
        byte[] guid = new byte[16];
        Random r = new Random();
        r.nextBytes(guid);
        
        MockHeadPongRequestor req = new MockHeadPongRequestor();
        req.setPongGGEPCapable(true);
        req.setUrn(UrnHelper.SHA1);
        req.setGuid(guid);
        
        FileDescStub fd = new FileDescStub("file", UrnHelper.SHA1, 0);
        fileManager.getGnutellaFileList().add(fd);
        uploadManager.setUploadsInProgress(UploadSettings.HARD_MAX_UPLOADS.getValue()-5);
        
        HeadPong pong = headPongFactory.create(req);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        pong.write(out);
        byte[] written = out.toByteArray();
        
        assertEquals(guid, written, 0, 16);
        // 16...23, rest of Gnutella header, ignore.
        assertEquals("LIME", new String(written, 23, 4));   // headpong vendor ID
        assertEquals(24, ByteUtils.leb2short(written, 27)); // headpong selector
        assertEquals(2, ByteUtils.leb2short(written, 29));  // current headpong version
        
        int[] endOffset = new int[1];
        GGEP writtenGGEP = new GGEP(written, 31, endOffset);
        assertEquals(written.length, endOffset[0]);
        
        assertEquals("got headers: " + writtenGGEP.getHeaders(), 3, writtenGGEP.getHeaders().size());
        assertEquals(new byte[] { 0x1 }, writtenGGEP.getBytes("C"));
        assertEquals(new byte[] { -5 }, writtenGGEP.getBytes("Q"));
        assertEquals("LIME".getBytes(), writtenGGEP.getBytes("V"));
    }
    
    public void testWriteRanges() throws Exception {
        byte[] guid = new byte[16];
        Random r = new Random();
        r.nextBytes(guid);
        
        MockHeadPongRequestor req = new MockHeadPongRequestor();
        req.setPongGGEPCapable(true);
        req.setUrn(UrnHelper.SHA1);
        req.setGuid(guid);
        req.setRequestsRanges(true);
        
        final IncompleteFileDescStub fd = new IncompleteFileDescStub("test", UrnHelper.SHA1, 100);
        fd.setRangesAsIntervals(Range.createRange(0, 500), Range.createRange(705, 1000), Range.createRange(20000, 25000));
        fileManager.getIncompleteFileList().add(fd);
        int expectedUploads = -UploadSettings.HARD_MAX_UPLOADS.getValue();
        mockery.checking(new Expectations() {{
            atLeast(1).of(downloadManager).isActivelyDownloading(with(same(fd.getSHA1Urn())));
            will(returnValue(false));
        }});
        
        HeadPong pong = headPongFactory.create(req);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        pong.write(out);
        byte[] written = out.toByteArray();
        
        assertEquals(guid, written, 0, 16);
        // 16...23, rest of Gnutella header, ignore.
        assertEquals("LIME", new String(written, 23, 4));   // headpong vendor ID
        assertEquals(24, ByteUtils.leb2short(written, 27)); // headpong selector
        assertEquals(2, ByteUtils.leb2short(written, 29));  // current headpong version
        
        int[] endOffset = new int[1];
        GGEP writtenGGEP = new GGEP(written, 31, endOffset);
        assertEquals(written.length, endOffset[0]);
        
        assertEquals("got headers: " + writtenGGEP.getHeaders(), 4, writtenGGEP.getHeaders().size());
        assertEquals(new byte[] { 0x2 }, writtenGGEP.getBytes("C"));
        assertEquals(new byte[] { (byte)expectedUploads }, writtenGGEP.getBytes("Q"));
        assertEquals("LIME".getBytes(), writtenGGEP.getBytes("V"));
        byte[] ranges = writtenGGEP.getBytes("R");
        assertEquals(0,     ByteUtils.beb2int(ranges, 0));
        assertEquals(500,   ByteUtils.beb2int(ranges, 4));
        assertEquals(705,   ByteUtils.beb2int(ranges, 8));
        assertEquals(1000,  ByteUtils.beb2int(ranges, 12));
        assertEquals(20000, ByteUtils.beb2int(ranges, 16));
        assertEquals(25000, ByteUtils.beb2int(ranges, 20));
        assertEquals(24, ranges.length);
    }
    
    public void testWriteLongRanges() throws Exception {
        byte[] guid = new byte[16];
        Random r = new Random();
        r.nextBytes(guid);
        
        MockHeadPongRequestor req = new MockHeadPongRequestor();
        req.setPongGGEPCapable(true);
        req.setUrn(UrnHelper.SHA1);
        req.setGuid(guid);
        req.setRequestsRanges(true);
        
        final IncompleteFileDescStub fd = new IncompleteFileDescStub("test", UrnHelper.SHA1, 100);
        fd.setRangesAsIntervals(Range.createRange(0, 500), Range.createRange(0xFFFFFFFF00l, 0xFFFFFFFFFFl));
        fileManager.getIncompleteFileList().add(fd);
        int expectedUploads = -UploadSettings.HARD_MAX_UPLOADS.getValue();
        mockery.checking(new Expectations() {{
            atLeast(1).of(downloadManager).isActivelyDownloading(with(same(fd.getSHA1Urn())));
            will(returnValue(false));
        }});
        
        HeadPong pong = headPongFactory.create(req);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        pong.write(out);
        byte[] written = out.toByteArray();
        
        assertEquals(guid, written, 0, 16);
        // 16...23, rest of Gnutella header, ignore.
        assertEquals("LIME", new String(written, 23, 4));   // headpong vendor ID
        assertEquals(24, ByteUtils.leb2short(written, 27)); // headpong selector
        assertEquals(2, ByteUtils.leb2short(written, 29));  // current headpong version
        
        int[] endOffset = new int[1];
        GGEP writtenGGEP = new GGEP(written, 31, endOffset);
        assertEquals(written.length, endOffset[0]);
        
        assertEquals("got headers: " + writtenGGEP.getHeaders(), 5, writtenGGEP.getHeaders().size());
        assertEquals(new byte[] { 0x2 }, writtenGGEP.getBytes("C"));
        assertEquals(new byte[] { (byte)expectedUploads }, writtenGGEP.getBytes("Q"));
        assertEquals("LIME".getBytes(), writtenGGEP.getBytes("V"));
        byte[] ranges = writtenGGEP.getBytes("R");
        assertEquals(8, ranges.length);
        assertEquals(0,     ByteUtils.beb2int(ranges, 0));
        assertEquals(500,   ByteUtils.beb2int(ranges, 4));
        byte[] ranges5 = writtenGGEP.getBytes("R5");
        assertEquals(10, ranges5.length);
        assertEquals(0xFFFFFFFF00l, ByteUtils.beb2long(ranges5, 0, 5));
        assertEquals(0xFFFFFFFFFFl, ByteUtils.beb2long(ranges5, 5, 5));
        
        // try only long ranges now
        fd.setRangesAsIntervals(Range.createRange(0xFFFFFFFF00l, 0xFFFFFFFFFFl));
        pong = headPongFactory.create(req);
        out = new ByteArrayOutputStream();
        pong.write(out);
        written = out.toByteArray();
        
        writtenGGEP = new GGEP(written, 31, endOffset);
        assertEquals(written.length, endOffset[0]);
        
        // there should not be an R extention
        try {
            writtenGGEP.getBytes("R");
            fail("there was an R extention with only long ranges");
        } catch (BadGGEPPropertyException expected){}
        ranges5 = writtenGGEP.getBytes("R5");
        assertEquals(10, ranges5.length);
        assertEquals(0xFFFFFFFF00l, ByteUtils.beb2long(ranges5, 0, 5));
        assertEquals(0xFFFFFFFFFFl, ByteUtils.beb2long(ranges5, 5, 5));
    }
    
    public void testWriteRangesOnlyIfRequested() throws Exception {
        byte[] guid = new byte[16];
        Random r = new Random();
        r.nextBytes(guid);
        
        MockHeadPongRequestor req = new MockHeadPongRequestor();
        req.setPongGGEPCapable(true);
        req.setUrn(UrnHelper.SHA1);
        req.setGuid(guid);
        req.setRequestsRanges(false);
        
        final IncompleteFileDescStub fd = new IncompleteFileDescStub("test", UrnHelper.SHA1, 100);
        fd.setRangesAsIntervals(Range.createRange(0, 500), Range.createRange(705, 1000), Range.createRange(20000, 25000));
        fileManager.getIncompleteFileList().add(fd);
        int expectedUploads = -UploadSettings.HARD_MAX_UPLOADS.getValue();
        mockery.checking(new Expectations() {{
            atLeast(1).of(downloadManager).isActivelyDownloading(with(same(fd.getSHA1Urn())));
            will(returnValue(false));
        }});
        
        HeadPong pong = headPongFactory.create(req);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        pong.write(out);
        byte[] written = out.toByteArray();
        
        assertEquals(guid, written, 0, 16);
        // 16...23, rest of Gnutella header, ignore.
        assertEquals("LIME", new String(written, 23, 4));   // headpong vendor ID
        assertEquals(24, ByteUtils.leb2short(written, 27)); // headpong selector
        assertEquals(2, ByteUtils.leb2short(written, 29));  // current headpong version
        
        int[] endOffset = new int[1];
        GGEP writtenGGEP = new GGEP(written, 31, endOffset);
        assertEquals(written.length, endOffset[0]);
        
        assertEquals("got headers: " + writtenGGEP.getHeaders(), 3, writtenGGEP.getHeaders().size());
        assertEquals(new byte[] { 0x2 }, writtenGGEP.getBytes("C"));
        assertEquals(new byte[] { (byte)expectedUploads }, writtenGGEP.getBytes("Q"));
        assertEquals("LIME".getBytes(), writtenGGEP.getBytes("V"));
    }
    
    public void testWriteBusyIfRangeRequestedButHasNone() throws Exception {
        byte[] guid = new byte[16];
        Random r = new Random();
        r.nextBytes(guid);
        
        MockHeadPongRequestor req = new MockHeadPongRequestor();
        req.setPongGGEPCapable(true);
        req.setUrn(UrnHelper.SHA1);
        req.setGuid(guid);
        req.setRequestsRanges(true);
        
        final IncompleteFileDescStub fd = new IncompleteFileDescStub("test", UrnHelper.SHA1, 100);
        fd.setRangesAsIntervals();
        fileManager.getIncompleteFileList().add(fd);
        mockery.checking(new Expectations() {{
            atLeast(1).of(downloadManager).isActivelyDownloading(with(same(fd.getSHA1Urn())));
            will(returnValue(false));
        }});
        
        HeadPong pong = headPongFactory.create(req);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        pong.write(out);
        byte[] written = out.toByteArray();
        
        assertEquals(guid, written, 0, 16);
        // 16...23, rest of Gnutella header, ignore.
        assertEquals("LIME", new String(written, 23, 4));   // headpong vendor ID
        assertEquals(24, ByteUtils.leb2short(written, 27)); // headpong selector
        assertEquals(2, ByteUtils.leb2short(written, 29));  // current headpong version
        
        int[] endOffset = new int[1];
        GGEP writtenGGEP = new GGEP(written, 31, endOffset);
        assertEquals(written.length, endOffset[0]);
        
        assertEquals("got headers: " + writtenGGEP.getHeaders(), 3, writtenGGEP.getHeaders().size());
        assertEquals(new byte[] { 0x2 }, writtenGGEP.getBytes("C"));
        assertEquals(new byte[] { 0x7F }, writtenGGEP.getBytes("Q"));
        assertEquals("LIME".getBytes(), writtenGGEP.getBytes("V"));
    }
    
    public void testWriteDoesntMakeBusyIfNoRangesButNotRequested() throws Exception {
        byte[] guid = new byte[16];
        Random r = new Random();
        r.nextBytes(guid);
        
        MockHeadPongRequestor req = new MockHeadPongRequestor();
        req.setPongGGEPCapable(true);
        req.setUrn(UrnHelper.SHA1);
        req.setGuid(guid);
        req.setRequestsRanges(false);
        
        final IncompleteFileDescStub fd = new IncompleteFileDescStub("test", UrnHelper.SHA1, 100);
        fd.setRangesAsIntervals();
        fileManager.getIncompleteFileList().add(fd);
        int expectedUploads = -UploadSettings.HARD_MAX_UPLOADS.getValue();
        mockery.checking(new Expectations() {{
            atLeast(1).of(downloadManager).isActivelyDownloading(with(same(fd.getSHA1Urn())));
            will(returnValue(false));
        }});
        
        HeadPong pong = headPongFactory.create(req);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        pong.write(out);
        byte[] written = out.toByteArray();
        
        assertEquals(guid, written, 0, 16);
        // 16...23, rest of Gnutella header, ignore.
        assertEquals("LIME", new String(written, 23, 4));   // headpong vendor ID
        assertEquals(24, ByteUtils.leb2short(written, 27)); // headpong selector
        assertEquals(2, ByteUtils.leb2short(written, 29));  // current headpong version
        
        int[] endOffset = new int[1];
        GGEP writtenGGEP = new GGEP(written, 31, endOffset);
        assertEquals(written.length, endOffset[0]);
        
        assertEquals("got headers: " + writtenGGEP.getHeaders(), 3, writtenGGEP.getHeaders().size());
        assertEquals(new byte[] { 0x2 }, writtenGGEP.getBytes("C"));
        assertEquals(new byte[] { (byte)expectedUploads }, writtenGGEP.getBytes("Q"));
        assertEquals("LIME".getBytes(), writtenGGEP.getBytes("V"));
    }
    
    public void testWritesPushWithTLSWithFWTLocations() throws Exception {
        PushEndpointFactory pushEndpointFactory = injector.getInstance(PushEndpointFactory.class);
        AlternateLocationFactory alternateLocationFactory = injector.getInstance(AlternateLocationFactory.class);
        AltLocManager altLocManager = injector.getInstance(AltLocManager.class);
        
        byte[] guid = new byte[16];
        Random r = new Random();
        r.nextBytes(guid);
        
        MockHeadPongRequestor req = new MockHeadPongRequestor();
        req.setPongGGEPCapable(true);
        req.setUrn(UrnHelper.SHA1);
        req.setGuid(guid);
        req.setRequestsPushLocs(true);
        
        final IncompleteFileDescStub fd = new IncompleteFileDescStub("test", UrnHelper.SHA1, 100);
        fd.setRangesAsIntervals();
        fileManager.getIncompleteFileList().add(fd);
        int expectedUploads = -UploadSettings.HARD_MAX_UPLOADS.getValue();
        mockery.checking(new Expectations() {{
            atLeast(1).of(downloadManager).isActivelyDownloading(with(same(fd.getSHA1Urn())));
            will(returnValue(false));
        }});
        
        GUID peGUID = new GUID(GUID.makeGuid());      
        PushAltLoc firewalled = (PushAltLoc) alternateLocationFactory.create(peGUID.toHexString()+";1.2.3.4:5", UrnHelper.SHA1);
        firewalled.updateProxies(true);
        altLocManager.add(firewalled, null);
        
        GUID peTlsGUID = new GUID();
        PushAltLoc tls = (PushAltLoc) alternateLocationFactory.create(peTlsGUID.toHexString() + ";pptls=6;2.3.4.5:5;3.4.5.6:7;4.5.6.7:8", UrnHelper.SHA1);
        tls.updateProxies(true);
        altLocManager.add(tls, null);
        
        GUID peFwtGUID = new GUID();
        PushAltLoc fwt = (PushAltLoc)alternateLocationFactory.create(peFwtGUID.toHexString() + ";fwt/1.0;10:20.21.23.23;pptls=8;2.3.4.5:5;3.4.5.6:7;4.5.6.7:8", UrnHelper.SHA1);
        fwt.updateProxies(true);
        altLocManager.add(fwt, null);
        
        HeadPong pong = headPongFactory.create(req);
        altLocManager.purge();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        pong.write(out);
        byte[] written = out.toByteArray();
        
        assertEquals(guid, written, 0, 16);
        // 16...23, rest of Gnutella header, ignore.
        assertEquals("LIME", new String(written, 23, 4));   // headpong vendor ID
        assertEquals(24, ByteUtils.leb2short(written, 27)); // headpong selector
        assertEquals(2, ByteUtils.leb2short(written, 29));  // current headpong version
        
        int[] endOffset = new int[1];
        GGEP writtenGGEP = new GGEP(written, 31, endOffset);
        assertEquals(written.length, endOffset[0]);
        
        assertEquals("got headers: " + writtenGGEP.getHeaders(), 4, writtenGGEP.getHeaders().size());
        assertEquals(new byte[] { 0x2 }, writtenGGEP.getBytes("C"));
        assertEquals(new byte[] { (byte)expectedUploads }, writtenGGEP.getBytes("Q"));
        assertEquals("LIME".getBytes(), writtenGGEP.getBytes("V"));
        byte[] pushes = writtenGGEP.get("P");
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(pushes));
        PushEndpoint pe1 = pushEndpointFactory.createFromBytes(in);
        PushEndpoint pe2 = pushEndpointFactory.createFromBytes(in);
        PushEndpoint pe3 = pushEndpointFactory.createFromBytes(in);
        assertEquals(0, in.available());
        assertEquals(-1, in.read());
        
        // Make sure it went in round-robin order for writing.
        assertEquals(0, pe1.getFWTVersion());
        assertEquals(1, pe2.getFWTVersion());
        assertEquals(0, pe3.getFWTVersion());
        
        // Ok, it's easy enough to test the FWT one, since there's only one.
        assertEquals(peFwtGUID.bytes(), pe2.getClientGUID());
        assertEquals("20.21.23.23", pe2.getAddress());
        assertEquals(10, pe2.getPort());
        Set<? extends IpPort> proxies = pe2.getProxies();
        Set<IpPort> expectedNormalProxies = new IpPortSet(new IpPortImpl("3.4.5.6:7"), new IpPortImpl("4.5.6.7:8"));
        Set<IpPort> expectedTLSProxies = new IpPortSet(new IpPortImpl("2.3.4.5:5"));
        Set<IpPort> copy = new IpPortSet(proxies);
        assertEquals(3, proxies.size());
        copy.retainAll(expectedNormalProxies);
        assertEquals(expectedNormalProxies, copy);
        for(IpPort ipp : copy) {
            if(ipp instanceof Connectable)
                assertFalse(((Connectable)ipp).isTLSCapable());
        }
        copy = new IpPortSet(proxies);
        copy.retainAll(expectedTLSProxies);
        assertEquals(expectedTLSProxies, copy);
        for(IpPort ipp : copy) {
            assertInstanceof(Connectable.class, ipp);
            assertTrue(((Connectable)ipp).isTLSCapable());
        }
        copy = new IpPortSet(proxies);
        copy.removeAll(expectedNormalProxies);
        copy.removeAll(expectedTLSProxies);
        assertEquals(0, copy.size());
        
        // Setup so that pe1 is peGUID && pe3 == tlsGUID
        if(!Arrays.equals(pe1.getClientGUID(), peGUID.bytes())) {
            PushEndpoint tmp = pe1;
            pe1 = pe3;
            pe3 = tmp;
        }
        
        assertEquals(pe1.getClientGUID(), peGUID.bytes());
        assertEquals(pe3.getClientGUID(), peTlsGUID.bytes());
        
        // PE1 only has 1 proxy, and it's not TLS capable.
        proxies = pe1.getProxies();
        assertEquals(1, proxies.size());
        IpPort proxy = proxies.iterator().next();
        assertEquals("1.2.3.4", proxy.getAddress());
        assertEquals(5, proxy.getPort());
        if(proxy instanceof Connectable)
            assertFalse(((Connectable)proxy).isTLSCapable());
        
        // PE3 has 3 proxies, two of which are TLS capable.
        proxies = pe3.getProxies();
        expectedTLSProxies = new IpPortSet(new IpPortImpl("3.4.5.6:7"), new IpPortImpl("4.5.6.7:8"));
        expectedNormalProxies = new IpPortSet(new IpPortImpl("2.3.4.5:5"));
        copy = new IpPortSet(proxies);
        assertEquals(3, proxies.size());
        copy.retainAll(expectedNormalProxies);
        assertEquals(expectedNormalProxies, copy);
        for(IpPort ipp : copy) {
            if(ipp instanceof Connectable)
                assertFalse(((Connectable)ipp).isTLSCapable());
        }
        copy = new IpPortSet(proxies);
        copy.retainAll(expectedTLSProxies);
        assertEquals(expectedTLSProxies, copy);
        for(IpPort ipp : copy) {
            assertInstanceof(Connectable.class, ipp);
            assertTrue(((Connectable)ipp).isTLSCapable());
        }
        copy = new IpPortSet(proxies);
        copy.removeAll(expectedNormalProxies);
        copy.removeAll(expectedTLSProxies);
        assertEquals(0, copy.size());
    }
    
    public void testWrittenFWTOnly() throws Exception {
        PushEndpointFactory pushEndpointFactory = injector.getInstance(PushEndpointFactory.class);
        AlternateLocationFactory alternateLocationFactory = injector.getInstance(AlternateLocationFactory.class);
        AltLocManager altLocManager = injector.getInstance(AltLocManager.class);
        
        byte[] guid = new byte[16];
        Random r = new Random();
        r.nextBytes(guid);
        
        MockHeadPongRequestor req = new MockHeadPongRequestor();
        req.setPongGGEPCapable(true);
        req.setUrn(UrnHelper.SHA1);
        req.setGuid(guid);
        req.setRequestsPushLocs(true);
        req.setRequestsFWTOnlyPushLocs(true);
        
        final IncompleteFileDescStub fd = new IncompleteFileDescStub("test", UrnHelper.SHA1, 100);
        fd.setRangesAsIntervals();
        fileManager.getIncompleteFileList().add(fd);
        int expectedUploads = -UploadSettings.HARD_MAX_UPLOADS.getValue();
        mockery.checking(new Expectations() {{
            atLeast(1).of(downloadManager).isActivelyDownloading(with(same(fd.getSHA1Urn())));
            will(returnValue(false));
        }});
        
        
        GUID peGUID = new GUID(GUID.makeGuid());      
        PushAltLoc firewalled = (PushAltLoc)alternateLocationFactory.create(peGUID.toHexString()+";1.2.3.4:5", UrnHelper.SHA1);
        firewalled.updateProxies(true);
        altLocManager.add(firewalled, null);
        
        GUID peTlsGUID = new GUID();
        PushAltLoc tls = (PushAltLoc)alternateLocationFactory.create(peTlsGUID.toHexString() + ";pptls=6;2.3.4.5:5;3.4.5.6:7;4.5.6.7:8", UrnHelper.SHA1);
        tls.updateProxies(true);
        altLocManager.add(tls, null);
        
        GUID peFwtGUID = new GUID();
        PushAltLoc fwt = (PushAltLoc)alternateLocationFactory.create(peFwtGUID.toHexString() + ";fwt/1.0;10:20.21.23.23;pptls=8;2.3.4.5:5;3.4.5.6:7;4.5.6.7:8", UrnHelper.SHA1);
        fwt.updateProxies(true);
        altLocManager.add(fwt, null);
        
        HeadPong pong = headPongFactory.create(req);
        altLocManager.purge();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        pong.write(out);
        byte[] written = out.toByteArray();
        
        assertEquals(guid, written, 0, 16);
        // 16...23, rest of Gnutella header, ignore.
        assertEquals("LIME", new String(written, 23, 4));   // headpong vendor ID
        assertEquals(24, ByteUtils.leb2short(written, 27)); // headpong selector
        assertEquals(2, ByteUtils.leb2short(written, 29));  // current headpong version
        
        int[] endOffset = new int[1];
        GGEP writtenGGEP = new GGEP(written, 31, endOffset);
        assertEquals(written.length, endOffset[0]);
        
        assertEquals("got headers: " + writtenGGEP.getHeaders(), 4, writtenGGEP.getHeaders().size());
        assertEquals(new byte[] { 0x2 }, writtenGGEP.getBytes("C"));
        assertEquals(new byte[] { (byte)expectedUploads }, writtenGGEP.getBytes("Q"));
        assertEquals("LIME".getBytes(), writtenGGEP.getBytes("V"));
        byte[] pushes = writtenGGEP.get("P");
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(pushes));
        PushEndpoint pe1 = pushEndpointFactory.createFromBytes(in);
        assertEquals(0, in.available());
        assertEquals(-1, in.read());
        
        assertEquals(1, pe1.getFWTVersion());
        assertEquals(peFwtGUID.bytes(), pe1.getClientGUID());
        assertEquals("20.21.23.23", pe1.getAddress());
        assertEquals(10, pe1.getPort());
        Set<? extends IpPort> proxies = pe1.getProxies();
        Set<IpPort> expectedNormalProxies = new IpPortSet(new IpPortImpl("3.4.5.6:7"), new IpPortImpl("4.5.6.7:8"));
        Set<IpPort> expectedTLSProxies = new IpPortSet(new IpPortImpl("2.3.4.5:5"));
        Set<IpPort> copy = new IpPortSet(proxies);
        assertEquals(3, proxies.size());
        copy.retainAll(expectedNormalProxies);
        assertEquals(expectedNormalProxies, copy);
        for(IpPort ipp : copy) {
            if(ipp instanceof Connectable)
                assertFalse(((Connectable)ipp).isTLSCapable());
        }
        copy = new IpPortSet(proxies);
        copy.retainAll(expectedTLSProxies);
        assertEquals(expectedTLSProxies, copy);
        for(IpPort ipp : copy) {
            assertInstanceof(Connectable.class, ipp);
            assertTrue(((Connectable)ipp).isTLSCapable());
        }
        copy = new IpPortSet(proxies);
        copy.removeAll(expectedNormalProxies);
        copy.removeAll(expectedTLSProxies);
        assertEquals(0, copy.size());
    }
    
    public void testNoPushWrittenIfNotRequested() throws Exception {
        AlternateLocationFactory alternateLocationFactory = injector.getInstance(AlternateLocationFactory.class);
        AltLocManager altLocManager = injector.getInstance(AltLocManager.class);
        
        byte[] guid = new byte[16];
        Random r = new Random();
        r.nextBytes(guid);
        
        MockHeadPongRequestor req = new MockHeadPongRequestor();
        req.setPongGGEPCapable(true);
        req.setUrn(UrnHelper.SHA1);
        req.setGuid(guid);
        req.setRequestsPushLocs(false);
        
        final IncompleteFileDescStub fd = new IncompleteFileDescStub("test", UrnHelper.SHA1, 100);
        fd.setRangesAsIntervals();
        fileManager.getIncompleteFileList().add(fd);
        int expectedUploads = -UploadSettings.HARD_MAX_UPLOADS.getValue();
        mockery.checking(new Expectations() {{
            atLeast(1).of(downloadManager).isActivelyDownloading(with(same(fd.getSHA1Urn())));
            will(returnValue(false));
        }});
              
        
        GUID peFwtGUID = new GUID();
        PushAltLoc fwt = (PushAltLoc)alternateLocationFactory.create(peFwtGUID.toHexString() + ";fwt/1.0;10:20.21.23.23;pptls=8;2.3.4.5:5;3.4.5.6:7;4.5.6.7:8", UrnHelper.SHA1);
        fwt.updateProxies(true);
        altLocManager.add(fwt, null);
        
        HeadPong pong = headPongFactory.create(req);
        altLocManager.purge();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        pong.write(out);
        byte[] written = out.toByteArray();
        
        assertEquals(guid, written, 0, 16);
        // 16...23, rest of Gnutella header, ignore.
        assertEquals("LIME", new String(written, 23, 4));   // headpong vendor ID
        assertEquals(24, ByteUtils.leb2short(written, 27)); // headpong selector
        assertEquals(2, ByteUtils.leb2short(written, 29));  // current headpong version
        
        int[] endOffset = new int[1];
        GGEP writtenGGEP = new GGEP(written, 31, endOffset);
        assertEquals(written.length, endOffset[0]);
        
        assertEquals("got headers: " + writtenGGEP.getHeaders(), 3, writtenGGEP.getHeaders().size());
        assertEquals(new byte[] { 0x2 }, writtenGGEP.getBytes("C"));
        assertEquals(new byte[] { (byte)expectedUploads }, writtenGGEP.getBytes("Q"));
        assertEquals("LIME".getBytes(), writtenGGEP.getBytes("V"));
    }
    
    public void testNoPushWrittenIfNotRequestedEvenIfFWTOnlyRequested() throws Exception {
        AlternateLocationFactory alternateLocationFactory = injector.getInstance(AlternateLocationFactory.class);
        AltLocManager altLocManager = injector.getInstance(AltLocManager.class);
        
        byte[] guid = new byte[16];
        Random r = new Random();
        r.nextBytes(guid);
        
        MockHeadPongRequestor req = new MockHeadPongRequestor();
        req.setPongGGEPCapable(true);
        req.setUrn(UrnHelper.SHA1);
        req.setGuid(guid);
        req.setRequestsPushLocs(false);
        req.setRequestsFWTOnlyPushLocs(true);
        
        final IncompleteFileDescStub fd = new IncompleteFileDescStub("test", UrnHelper.SHA1, 100);
        fd.setRangesAsIntervals();
        fileManager.getIncompleteFileList().add(fd);
        int expectedUploads = -UploadSettings.HARD_MAX_UPLOADS.getValue();
        mockery.checking(new Expectations() {{
            atLeast(1).of(downloadManager).isActivelyDownloading(with(same(fd.getSHA1Urn())));
            will(returnValue(false));
        }});
              
        
        GUID peFwtGUID = new GUID();
        PushAltLoc fwt = (PushAltLoc)alternateLocationFactory.create(peFwtGUID.toHexString() + ";fwt/1.0;10:20.21.23.23;pptls=8;2.3.4.5:5;3.4.5.6:7;4.5.6.7:8", UrnHelper.SHA1);
        fwt.updateProxies(true);
        altLocManager.add(fwt, null);
        
        HeadPong pong = headPongFactory.create(req);
        altLocManager.purge();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        pong.write(out);
        byte[] written = out.toByteArray();
        
        assertEquals(guid, written, 0, 16);
        // 16...23, rest of Gnutella header, ignore.
        assertEquals("LIME", new String(written, 23, 4));   // headpong vendor ID
        assertEquals(24, ByteUtils.leb2short(written, 27)); // headpong selector
        assertEquals(2, ByteUtils.leb2short(written, 29));  // current headpong version
        
        int[] endOffset = new int[1];
        GGEP writtenGGEP = new GGEP(written, 31, endOffset);
        assertEquals(written.length, endOffset[0]);
        
        assertEquals("got headers: " + writtenGGEP.getHeaders(), 3, writtenGGEP.getHeaders().size());
        assertEquals(new byte[] { 0x2 }, writtenGGEP.getBytes("C"));
        assertEquals(new byte[] { (byte)expectedUploads }, writtenGGEP.getBytes("Q"));
        assertEquals("LIME".getBytes(), writtenGGEP.getBytes("V"));
    }
    
    public void testWriteDirectLocs() throws Exception {
        AlternateLocationFactory alternateLocationFactory = injector.getInstance(AlternateLocationFactory.class);
        AltLocManager altLocManager = injector.getInstance(AltLocManager.class);
        
        byte[] guid = new byte[16];
        Random r = new Random();
        r.nextBytes(guid);
        
        MockHeadPongRequestor req = new MockHeadPongRequestor();
        req.setPongGGEPCapable(true);
        req.setUrn(UrnHelper.SHA1);
        req.setGuid(guid);
        req.setRequestsAltLocs(true);
        
        final IncompleteFileDescStub fd = new IncompleteFileDescStub("test", UrnHelper.SHA1, 100);
        fd.setRangesAsIntervals();
        fileManager.getIncompleteFileList().add(fd);
        int expectedUploads = -UploadSettings.HARD_MAX_UPLOADS.getValue();
        mockery.checking(new Expectations() {{
            atLeast(1).of(downloadManager).isActivelyDownloading(with(same(fd.getSHA1Urn())));
            will(returnValue(false));
        }});
              
        
        for(int i=0;i<10;i++ ) {
            AlternateLocation al = alternateLocationFactory.create("1.2.3."+i+":1234", UrnHelper.SHA1);
            altLocManager.add(al, null);
        }
        
        HeadPong pong = headPongFactory.create(req);
        altLocManager.purge();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        pong.write(out);
        byte[] written = out.toByteArray();
        
        assertEquals(guid, written, 0, 16);
        // 16...23, rest of Gnutella header, ignore.
        assertEquals("LIME", new String(written, 23, 4));   // headpong vendor ID
        assertEquals(24, ByteUtils.leb2short(written, 27)); // headpong selector
        assertEquals(2, ByteUtils.leb2short(written, 29));  // current headpong version
        
        int[] endOffset = new int[1];
        GGEP writtenGGEP = new GGEP(written, 31, endOffset);
        assertEquals(written.length, endOffset[0]);
        
        assertEquals("got headers: " + writtenGGEP.getHeaders(), 4, writtenGGEP.getHeaders().size());
        assertEquals(new byte[] { 0x2 }, writtenGGEP.getBytes("C"));
        assertEquals(new byte[] { (byte)expectedUploads }, writtenGGEP.getBytes("Q"));
        assertEquals("LIME".getBytes(), writtenGGEP.getBytes("V"));
        byte[] locs = writtenGGEP.getBytes("A");
        assertEquals(60, locs.length);
        Collection<IpPort> ipps = NetworkUtils.unpackIps(locs);
        assertEquals(10, ipps.size());
        Set<Byte> dots = new TreeSet<Byte>();
        for(IpPort ipp : ipps) {
            assertTrue(ipp.getAddress().startsWith("1.2.3."));
            assertEquals(1234, ipp.getPort());
            dots.add(ipp.getInetAddress().getAddress()[3]);
        }
        assertEquals(10, dots.size());
        Iterator<Byte> octects = dots.iterator();
        assertEquals(0, (int)octects.next());
        assertEquals(1, (int)octects.next());
        assertEquals(2, (int)octects.next());
        assertEquals(3, (int)octects.next());
        assertEquals(4, (int)octects.next());
        assertEquals(5, (int)octects.next());
        assertEquals(6, (int)octects.next());
        assertEquals(7, (int)octects.next());
        assertEquals(8, (int)octects.next());
        assertEquals(9, (int)octects.next());        
    }
    
    public void testNotWriteDirectLocsIfNotRequested() throws Exception {
        AlternateLocationFactory alternateLocationFactory = injector.getInstance(AlternateLocationFactory.class);
        AltLocManager altLocManager = injector.getInstance(AltLocManager.class);
        
        byte[] guid = new byte[16];
        Random r = new Random();
        r.nextBytes(guid);
        
        MockHeadPongRequestor req = new MockHeadPongRequestor();
        req.setPongGGEPCapable(true);
        req.setUrn(UrnHelper.SHA1);
        req.setGuid(guid);
        req.setRequestsAltLocs(false);
        
        final IncompleteFileDescStub fd = new IncompleteFileDescStub("test", UrnHelper.SHA1, 100);
        fd.setRangesAsIntervals();
        fileManager.getIncompleteFileList().add(fd);
        int expectedUploads = -UploadSettings.HARD_MAX_UPLOADS.getValue();
        mockery.checking(new Expectations() {{
            atLeast(1).of(downloadManager).isActivelyDownloading(with(same(fd.getSHA1Urn())));
            will(returnValue(false));
        }});
              
        
        for(int i=0;i<10;i++ ) {
            AlternateLocation al = alternateLocationFactory.create("1.2.3."+i+":1234", UrnHelper.SHA1);
            altLocManager.add(al, null);
        }
        
        HeadPong pong = headPongFactory.create(req);
        altLocManager.purge();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        pong.write(out);
        byte[] written = out.toByteArray();
        
        assertEquals(guid, written, 0, 16);
        // 16...23, rest of Gnutella header, ignore.
        assertEquals("LIME", new String(written, 23, 4));   // headpong vendor ID
        assertEquals(24, ByteUtils.leb2short(written, 27)); // headpong selector
        assertEquals(2, ByteUtils.leb2short(written, 29));  // current headpong version
        
        int[] endOffset = new int[1];
        GGEP writtenGGEP = new GGEP(written, 31, endOffset);
        assertEquals(written.length, endOffset[0]);
        
        assertEquals("got headers: " + writtenGGEP.getHeaders(), 3, writtenGGEP.getHeaders().size());
        assertEquals(new byte[] { 0x2 }, writtenGGEP.getBytes("C"));
        assertEquals(new byte[] { (byte)expectedUploads }, writtenGGEP.getBytes("Q"));
        assertEquals("LIME".getBytes(), writtenGGEP.getBytes("V"));
    }
    
    public void testWriteTLSWithLocs() throws Exception {
        AlternateLocationFactory alternateLocationFactory = injector.getInstance(AlternateLocationFactory.class);
        AltLocManager altLocManager = injector.getInstance(AltLocManager.class);
        
        byte[] guid = new byte[16];
        Random r = new Random();
        r.nextBytes(guid);
        
        MockHeadPongRequestor req = new MockHeadPongRequestor();
        req.setPongGGEPCapable(true);
        req.setUrn(UrnHelper.SHA1);
        req.setGuid(guid);
        req.setRequestsAltLocs(true);
        
        final IncompleteFileDescStub fd = new IncompleteFileDescStub("test", UrnHelper.SHA1, 100);
        fd.setRangesAsIntervals();
        fileManager.getIncompleteFileList().add(fd);
        int expectedUploads = -UploadSettings.HARD_MAX_UPLOADS.getValue();
        mockery.checking(new Expectations() {{
            atLeast(1).of(downloadManager).isActivelyDownloading(with(same(fd.getSHA1Urn())));
            will(returnValue(false));
        }});
            
        
        for(int i=0;i<10;i++ ) {
            AlternateLocation al = alternateLocationFactory.create("1.2.3."+i+":1234", UrnHelper.SHA1, i % 2 == 0);
            altLocManager.add(al, null);
        }
        
        HeadPong pong = headPongFactory.create(req);
        altLocManager.purge();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        pong.write(out);
        byte[] written = out.toByteArray();
        
        assertEquals(guid, written, 0, 16);
        // 16...23, rest of Gnutella header, ignore.
        assertEquals("LIME", new String(written, 23, 4));   // headpong vendor ID
        assertEquals(24, ByteUtils.leb2short(written, 27)); // headpong selector
        assertEquals(2, ByteUtils.leb2short(written, 29));  // current headpong version
        
        int[] endOffset = new int[1];
        GGEP writtenGGEP = new GGEP(written, 31, endOffset);
        assertEquals(written.length, endOffset[0]);
        
        assertEquals("got headers: " + writtenGGEP.getHeaders(), 5, writtenGGEP.getHeaders().size());
        assertEquals(new byte[] { 0x2 }, writtenGGEP.getBytes("C"));
        assertEquals(new byte[] { (byte)expectedUploads }, writtenGGEP.getBytes("Q"));
        assertEquals("LIME".getBytes(), writtenGGEP.getBytes("V"));
        byte[] locs = writtenGGEP.getBytes("A");
        assertEquals(60, locs.length);
        Collection<IpPort> ipps = NetworkUtils.unpackIps(locs);
        assertEquals(10, ipps.size());
        
        // rebuild bitnumbers to get an expected TLS array,
        // since the IPs could have been sent in any order.
        Set<Byte> dots = new TreeSet<Byte>();
        BitNumbers bn = new BitNumbers(10);
        int i = 0;
        for(IpPort ipp : ipps) {
            assertTrue(ipp.getAddress().startsWith("1.2.3."));
            assertEquals(1234, ipp.getPort());
            byte b = ipp.getInetAddress().getAddress()[3];
            dots.add(b);
            if(b % 2 == 0)
                bn.set(i);
            i++;
        }
        // make sure what we expect TLS to be is what it is.
        assertEquals(bn.toByteArray(), writtenGGEP.getBytes("T"));
        
        // Make sure we actually got the right IPs!
        assertEquals(10, dots.size());
        Iterator<Byte> octects = dots.iterator();
        assertEquals(0, (int)octects.next());
        assertEquals(1, (int)octects.next());
        assertEquals(2, (int)octects.next());
        assertEquals(3, (int)octects.next());
        assertEquals(4, (int)octects.next());
        assertEquals(5, (int)octects.next());
        assertEquals(6, (int)octects.next());
        assertEquals(7, (int)octects.next());
        assertEquals(8, (int)octects.next());
        assertEquals(9, (int)octects.next());        
        
        
    }
    
    private byte[] arrayof(GGEP ggep) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ggep.write(out);
        return out.toByteArray();
    }
}
