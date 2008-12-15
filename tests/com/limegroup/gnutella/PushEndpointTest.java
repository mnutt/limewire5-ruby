package com.limegroup.gnutella;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import junit.framework.Test;

import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.io.IpPortSet;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.io.SimpleNetworkInstanceUtils;
import org.limewire.util.BaseTestCase;
import org.limewire.util.ByteUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.name.Names;
import com.limegroup.gnutella.stubs.ScheduledExecutorServiceStub;

/**
 * Tests {@link PushEndpoint}, {@link PushEndpointFactory} and {@link PushEndpointCacheImpl}.
 * 
 * TODO split into separate unit tests
 */
public class PushEndpointTest extends BaseTestCase {

    private IpPort ppi1;
    private IpPort ppi2;
    private IpPort ppi3;
    private IpPort ppi4;
    private IpPort ppi5;
    private IpPort ppi6;
    
    private IpPort tls1;
    private IpPort tls2;
    private IpPort tls3;
    private IpPort tls4;
    private IpPort tls5;
    private IpPort tls6;
    private PushEndpointFactory factory;
    private PushEndpointCacheImpl pushEndpointCache;
    
	public PushEndpointTest(String name) {
		super(name);
	}
    
    public static Test suite() {
        return buildTestSuite(PushEndpointTest.class);
    }
    
    @Override
    public void setUp() throws Exception {
        ppi1 = new IpPortImpl("1.2.3.4", 1235);
        ppi2 = new IpPortImpl("1.2.3.5", 1235);
        ppi3 = new IpPortImpl("1.2.3.6", 1235);
        ppi4 = new IpPortImpl("1.2.3.7", 1235);
        ppi5 = new IpPortImpl("1.2.3.8", 1235);
        ppi6 = new IpPortImpl("1.2.3.9", 1235);

        tls1 = new ConnectableImpl("1.2.3.4", 1235, true);
        tls2 = new ConnectableImpl("1.2.3.5", 1235, true);
        tls3 = new ConnectableImpl("1.2.3.6", 1235, true);
        tls4 = new ConnectableImpl("1.2.3.7", 1235, true);
        tls5 = new ConnectableImpl("1.2.3.8", 1235, true);
        tls6 = new ConnectableImpl("1.2.3.9", 1235, true);
        
        Injector injector = LimeTestUtils.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(ScheduledExecutorService.class).annotatedWith(Names.named("backgroundExecutor")).toInstance(new ScheduledExecutorServiceStub());
                bind(NetworkInstanceUtils.class).toInstance(new SimpleNetworkInstanceUtils());
            }
        });
        
        pushEndpointCache = (PushEndpointCacheImpl)injector.getInstance(PushEndpointCache.class);
        factory = injector.getInstance(PushEndpointFactory.class);
    }
    
    public void testConstructorGUID() throws Exception {
        GUID guid1 = new GUID(GUID.makeGuid());        
        PushEndpoint empty = factory.createPushEndpoint(guid1.bytes()); 
        assertEquals(guid1, new GUID(empty.getClientGUID()));
        assertEquals(PushEndpoint.HEADER_SIZE,AbstractPushEndpoint.getSizeBytes(empty.getProxies(), false));
        assertEquals(PushEndpoint.HEADER_SIZE,AbstractPushEndpoint.getSizeBytes(empty.getProxies(), true));
        assertEquals(0, empty.getProxies().size());
    }
    
    public void testConstructorProxies() throws Exception {
        GUID guid2 = new GUID(GUID.makeGuid());

        Set<IpPort> set1 = new HashSet<IpPort>();
        Set<IpPort> set2 = new HashSet<IpPort>();

        set1.add(ppi1); 
        set2.add(ppi1);
        set2.add(ppi2);
        
        PushEndpoint one = factory.createPushEndpoint(guid2.bytes(), set1);
        assertEquals(PushEndpoint.HEADER_SIZE+PushEndpoint.PROXY_SIZE, AbstractPushEndpoint.getSizeBytes(one.getProxies(), false));
        assertEquals(PushEndpoint.HEADER_SIZE+PushEndpoint.PROXY_SIZE, AbstractPushEndpoint.getSizeBytes(one.getProxies(), true));
        assertEquals(1,one.getProxies().size());
        assertEquals(0,one.getFWTVersion());
        
        PushEndpoint two = factory.createPushEndpoint(guid2.bytes(), set2);
        assertEquals(PushEndpoint.HEADER_SIZE+2*PushEndpoint.PROXY_SIZE, AbstractPushEndpoint.getSizeBytes(two.getProxies(), false));
        assertEquals(PushEndpoint.HEADER_SIZE+2*PushEndpoint.PROXY_SIZE, AbstractPushEndpoint.getSizeBytes(two.getProxies(), true));
        assertEquals(2,two.getProxies().size());
        assertEquals(0,two.getFWTVersion());
    }
    
    @SuppressWarnings("null")
    public void testConstructorTLS() throws Exception {
        GUID guid4 = new GUID(GUID.makeGuid());
    	
    	IpPort ppi1 = new IpPortImpl("1.2.3.4",1234);
    	IpPort ppi2 = new IpPortImpl("1.2.3.5",1235);
        IpPort ppi3 = new ConnectableImpl("1.2.3.6", 1236, true);
		
        Set<IpPort> set3 = new HashSet<IpPort>();
    	
        set3.add(ppi1);
        set3.add(ppi2);
        set3.add(ppi3);
    	
        PushEndpoint tls = factory.createPushEndpoint(guid4.bytes(), set3);
        assertEquals(PushEndpoint.HEADER_SIZE+3*PushEndpoint.PROXY_SIZE, AbstractPushEndpoint.getSizeBytes(tls.getProxies(), false));
        assertEquals(PushEndpoint.HEADER_SIZE+3*PushEndpoint.PROXY_SIZE+1, AbstractPushEndpoint.getSizeBytes(tls.getProxies(), true));
        assertEquals(3,tls.getProxies().size());
        assertEquals(0,tls.getFWTVersion());
        Set proxies = tls.getProxies();
        assertEquals(3, proxies.size());
        int notTLS = 0;
        int isTLS = 0;
        Connectable tlsIPP = null;
        for(Object ipp : proxies) {
            if(ipp instanceof Connectable && ((Connectable)ipp).isTLSCapable()) {
                isTLS++;
                tlsIPP = (Connectable)ipp;
            } else {
                notTLS++;
            }
        }
        assertEquals(2, notTLS);
        assertEquals(1, isTLS);
        assertEquals("1.2.3.6", tlsIPP.getAddress());
    }
    
    public void testConstructorFeatures() throws Exception {
        GUID guid3 = new GUID(GUID.makeGuid());
    	PushEndpoint three = factory.createPushEndpoint(guid3.bytes(), null, (byte)0, 1);
    	assertGreaterThan(0, three.getFWTVersion());
    	assertEquals("1.1.1.1", three.getAddress());
    	assertEquals(6346, three.getPort());
    }
    
    public void testConstructorIpPort() throws Exception {
        GUID guid3 = new GUID(GUID.makeGuid());
    	IpPort ip = new IpPortImpl("1.2.3.4",5);
    	PushEndpoint four = factory.createPushEndpoint(guid3.bytes(), null, (byte)0, 1, ip);
    	assertEquals("1.2.3.4", four.getAddress());
    	assertEquals(5, four.getPort());
    }
    
    public void testBasicToAndFromBytes() throws Exception {
    	GUID guid1 = new GUID(GUID.makeGuid());		
    	IpPortSet set1 = ippset(ppi1);
    	PushEndpoint one = factory.createPushEndpoint(guid1.bytes(), set1);
        for (IpPort ipp : one.getProxies()) {
            if (ipp instanceof Connectable
                    && ((Connectable) ipp).isTLSCapable())
                fail("TLS capable: " + ipp);
        }
        
    	assertEquals(0,one.getFWTVersion());
        
        byte[] expected = new byte[23];
        expected[ 0] = 0x1; // 1 proxy, no f2f, no features
        for(int i = 0; i < 15; i++)
            expected[i+1] = guid1.bytes()[i];
        expected[17] = 1;
        expected[18] = 2;
        expected[19] = 3;
        expected[20] = 4;
        ByteUtils.short2leb((short)1235, expected, 21);
        
        // Test toBytes
    	byte [] network = one.toBytes(false);
        assertEquals(expected, network);
        // toBytes(...)
        byte [] network2 = new byte[expected.length + 2];
        one.toBytes(network2, 2, false);
        assertEquals(expected, network2, 2, expected.length);
        
        // And make sure w/ TLS=true doesn't change things, since there's no TLS ipps!
        network = one.toBytes(true);
        assertEquals(expected, network);
        // toBytes(...)
        network2 = new byte[expected.length + 2];
        one.toBytes(network2, 2, true);
        assertEquals(expected, network2, 2, expected.length);
    	
        // Test fromBytes
    	assertEquals(AbstractPushEndpoint.getSizeBytes(one.getProxies(), false), expected.length);
    	PushEndpoint one_prim = factory.createFromBytes(new DataInputStream(new ByteArrayInputStream(expected)));
    	assertEquals(one, one_prim);
    	// And make sure none of the proxies are TLS capable.
        for (IpPort ipp : one_prim.getProxies()) {
            if (ipp instanceof Connectable
                    && ((Connectable) ipp).isTLSCapable())
                fail("TLS capable: " + ipp);
        }
        assertEquals(0, IpPort.COMPARATOR.compare(ppi1, one_prim.getProxies().iterator().next()));
    }
    
    public void testToAndFromWithTLS() throws Exception {
        GUID guid1 = new GUID(GUID.makeGuid());
        IpPortSet tet1 = ippset(ppi1, ppi2, tls3, tls4);

        byte[] expected = new byte[42];
        expected[ 0] = 0x4 | (byte)0x80; // 1 proxy, no f2f, tls fields added
        for(int i = 0; i < 15; i++)
            expected[i+1] = guid1.bytes()[i];
        expected[17] = (byte)0x30;
        for(int i = 0; i < 4; i++) {
            expected[18 + (i * 6)] = 1;
            expected[19 + (i * 6)] = 2;
            expected[20 + (i * 6)] = 3;
            expected[21 + (i * 6)] = (byte)(i+4);
            ByteUtils.short2leb((short)1235, expected, 22 + (i*6));
        }
        
        PushEndpoint one = factory.createPushEndpoint(guid1.bytes(), tet1);
        assertEquals(0,one.getFWTVersion());        
        // Make sure the proxies we read are TLS capable.
        Set proxies = one.getProxies();
        Set expectedProxies = new IpPortSet(tet1);
        assertEquals(4, proxies.size());
        int notTLS = 0;
        int isTLS = 0;
        for(Object ipp : proxies) {
            if(ipp instanceof Connectable && ((Connectable)ipp).isTLSCapable()) {
                isTLS++;
                expectedProxies.remove(ipp);
            } else {
                notTLS++;
                expectedProxies.remove(ipp);
            }
        }
        assertEquals(2, notTLS);
        assertEquals(2, isTLS);
        assertEquals(0, expectedProxies.size());
        
        
        // Test toBytes
        byte[] network = one.toBytes(true);
        assertEquals(expected, network);
        // toBytes(...)
        byte[] network2 = new byte[expected.length + 2];
        one.toBytes(network2, 2, true);
        assertEquals(expected, network2, 2, expected.length);
        
        // And make sure w/ TLS=false does change things!
        network = one.toBytes(false);
        assertNotEquals(expected, network);
        // iterate through the bytes -- the only one that should be different is the first & TLS byte
        for(int i = 0; i < expected.length; i++) {
            if(i == 0)
                assertEquals("wrong initial byte!", 0x4, network[0]);
            else if(i < 17)
                assertEquals("wrong at i: " + i, expected[i], network[i]);
            else if(i == 17)
                continue;
            else
                assertEquals("wrong at i: " + i, expected[i], network[i-1]);
        }
        
        // toBytes(...)
        network2 = new byte[expected.length + 2];
        one.toBytes(network2, 2, false);
        assertNotEquals(expected, network2, 2, expected.length);        
        // network2 should be identital to network (which we verified above)...
        assertEquals(network, network2, 2, network.length);
        
        // Test fromBytes
        assertEquals(AbstractPushEndpoint.getSizeBytes(one.getProxies(), true), expected.length);
        PushEndpoint one_prim = factory.createFromBytes(new DataInputStream(new ByteArrayInputStream(expected)));
        assertEquals(one, one_prim);
        
        // Test deserialized PE for TLS understanding
        proxies = one_prim.getProxies();
        expectedProxies = new IpPortSet(tet1);
        assertEquals(4, proxies.size());
        notTLS = 0;
        isTLS = 0;
        for (Object ipp : proxies) {
            if (ipp instanceof Connectable
                    && ((Connectable) ipp).isTLSCapable()) {
                isTLS++;
                expectedProxies.remove(ipp);
            } else {
                notTLS++;
                expectedProxies.remove(ipp);
            }
        }
        assertEquals(2, notTLS);
        assertEquals(2, isTLS);
        assertEquals(0, expectedProxies.size());
    }
        
    public void testToBytesMorePPIs() throws Exception {
        // make sure we don't mark TLS if it isn't in the endpoints.
        // makes sure we cut off adding PPIs at 4.
        
        GUID guid1 = new GUID(GUID.makeGuid());
        IpPortSet set = ippset(ppi1, ppi2, ppi3, ppi4, tls5, tls6);
        byte[] expected = new byte[41];
        expected[ 0] = 0x4; // 1 proxy, no f2f, no tls
        for(int i = 0; i < 15; i++)
            expected[i+1] = guid1.bytes()[i];
        for(int i = 0; i < 4; i++) {
            expected[17 + (i * 6)] = 1;
            expected[18 + (i * 6)] = 2;
            expected[19 + (i * 6)] = 3;
            expected[20 + (i * 6)] = (byte)(i+4);
            ByteUtils.short2leb((short)1235, expected, 21 + (i*6));
        }
        
        PushEndpoint one = factory.createPushEndpoint(guid1.bytes(), set);
        assertEquals(0,one.getFWTVersion());
        
        // Test toBytes
        byte[] network = one.toBytes(true);
        assertEquals(expected, network);
        // toBytes(...)
        byte[] network2 = new byte[expected.length + 2];
        one.toBytes(network2, 2, true);
        assertEquals(expected, network2, 2, expected.length);
        
        // Make sure, just for kicks, that tls=false doesn't change.
        network = one.toBytes(false);
        assertEquals(expected, network);
        // toBytes(...)
        network2 = new byte[expected.length + 2];
        one.toBytes(network2, 2, false);
        assertEquals(expected, network2, 2, expected.length);

        // Reconstruct it from the network & make sure none of them had proxies.
        one = factory.createFromBytes(new DataInputStream(new ByteArrayInputStream(network)));
        for(IpPort ipp : one.getProxies()) {
            if(ipp instanceof Connectable && ((Connectable)ipp).isTLSCapable())
                fail("TLS capable: " + ipp);
        }
    }       
        
    public void testSimpleHTTPStringValue() throws Exception {
    	GUID guid1 = new GUID();
    	IpPortSet set = ippset(ppi1);
    	PushEndpoint one = factory.createPushEndpoint(guid1.bytes(), set);
    	String httpString = one.httpStringValue();
        assertEquals(guid1.toHexString() + ";1.2.3.4:1235", httpString);
    	PushEndpoint one_prim = factory.createPushEndpoint(httpString);
    	assertEquals(1,one_prim.getProxies().size());
    	set.retainAll(one_prim.getProxies());
    	assertEquals(1, set.size());
    }
    
    public void testHttpStringValueWithMyIp() throws Exception {
        GUID g1 = new GUID();
        IpPortSet set = ippset(ppi1, ppi2, ppi3, ppi4, ppi5, ppi6);
        
    	//now test a bigger endpoint with an ip in it
    	IpPort ip = new IpPortImpl("1.2.3.4",5);
       	PushEndpoint six = factory.createPushEndpoint(g1.bytes(), set, (byte)0, 2, ip);
    	String httpString = six.httpStringValue();
        assertEquals(g1.toHexString() + ";fwt/2;5:1.2.3.4;1.2.3.4:1235;1.2.3.5:1235;1.2.3.6:1235;1.2.3.7:1235", httpString);
    	PushEndpoint four = factory.createPushEndpoint(httpString);
    	assertEquals(2,four.getFWTVersion());
    	assertEquals(4,four.getProxies().size());
    	assertEquals("1.2.3.4",four.getAddress());
    	assertEquals(5,four.getPort());
    	
        set.retainAll(four.getProxies());
    	assertEquals(four.getProxies().size(), set.size());
    }
    
    /**
     * It should not be possible to create a push endpoint with fwt support
     * but without host. 
     */
    public void testHttpStringFWTPushEndpointWithoutHost() throws Exception {
        String httpValue = "FFB3EC3B9D93A8F9CE42AED28F674900;fwt/1;222.222.222.222:2222";
        PushEndpoint pushEndpoint = factory.createPushEndpoint(httpValue);
        assertNull(pushEndpoint.getValidExternalAddress());
        assertNull(pushEndpoint.getInetAddress());
        assertNull(pushEndpoint.getInetSocketAddress());
        assertEquals(0, pushEndpoint.getFWTVersion());
    }
    
    public void testHttpStringWithTLS() throws Exception {
        GUID g1 = new GUID();
        IpPortSet set = ippset(ppi1, tls2, ppi3, tls4);
        PushEndpoint pe = factory.createPushEndpoint(g1.bytes(), set);
        String httpString = pe.httpStringValue();
        assertEquals(g1.toHexString() + ";pptls=5;1.2.3.4:1235;1.2.3.5:1235;1.2.3.6:1235;1.2.3.7:1235", httpString);
        PushEndpoint read = factory.createPushEndpoint(httpString);
        Iterator<? extends IpPort> i = read.getProxies().iterator();
        IpPort read1 = i.next();
        IpPort read2 = i.next();
        IpPort read3 = i.next();
        IpPort read4 = i.next();
        assertFalse(i.hasNext());
        
        assertEquals("1.2.3.4", read1.getAddress());
        assertEquals("1.2.3.5", read2.getAddress());
        assertEquals("1.2.3.6", read3.getAddress());
        assertEquals("1.2.3.7", read4.getAddress());
        
        // these two MUST be Connectables.
        assertInstanceof(Connectable.class, read2);
        assertInstanceof(Connectable.class, read4);
        assertTrue(((Connectable)read2).isTLSCapable());
        assertTrue(((Connectable)read4).isTLSCapable());
        
        // 1 & 3 MAY be Connectables, but MUST NOT be tls capable
        assertTrue( (!(read1 instanceof Connectable)) || (!((Connectable)read1).isTLSCapable()));
        assertTrue( (!(read3 instanceof Connectable)) || (!((Connectable)read3).isTLSCapable()));
    }
    
    public void testHttpStringWithTLSAndMyIP() throws Exception {
        GUID g1 = new GUID();
        IpPortSet set = ippset(tls1, ppi2, tls3, ppi4);
        IpPort myIp = new IpPortImpl("1.3.2.5:7");
        PushEndpoint pe = factory.createPushEndpoint(g1.bytes(), set, (byte)0, 2, myIp);
        String httpString = pe.httpStringValue();
        assertEquals(g1.toHexString() + ";fwt/2;7:1.3.2.5;pptls=A;1.2.3.4:1235;1.2.3.5:1235;1.2.3.6:1235;1.2.3.7:1235", httpString);
        assertEquals(2, pe.getFWTVersion());
        PushEndpoint read = factory.createPushEndpoint(httpString);
        assertEquals("1.3.2.5", read.getAddress());
        Iterator<? extends IpPort> i = read.getProxies().iterator();
        IpPort read1 = i.next();
        IpPort read2 = i.next();
        IpPort read3 = i.next();
        IpPort read4 = i.next();
        assertFalse(i.hasNext());
        
        assertEquals("1.2.3.4", read1.getAddress());
        assertEquals("1.2.3.5", read2.getAddress());
        assertEquals("1.2.3.6", read3.getAddress());
        assertEquals("1.2.3.7", read4.getAddress());
        
        // these two MUST be Connectables.
        assertInstanceof(Connectable.class, read1);
        assertInstanceof(Connectable.class, read3);
        assertTrue(((Connectable)read1).isTLSCapable());
        assertTrue(((Connectable)read3).isTLSCapable());
        
        // 2 & 4 MAY be Connectables, but MUST NOT be tls capable
        assertTrue( (!(read2 instanceof Connectable)) || (!((Connectable)read2).isTLSCapable()));
        assertTrue( (!(read4 instanceof Connectable)) || (!((Connectable)read4).isTLSCapable()));
    }
    
    public void testNoFWTInHTTPGetsNoEndpoint() throws Exception {
        GUID g1 = new GUID();
        IpPortSet set1 = ippset(ppi1);
        IpPort me = new IpPortImpl("1.2.3.4:5");
        
    	//now test an endpoint with an ip in it, but which does not support
    	//FWT.  We should not get the ip in the http representation
    	PushEndpoint noFWT = factory.createPushEndpoint(g1.bytes(), set1, (byte)0, 0, me);
    	String httpString = noFWT.httpStringValue();
        assertEquals(g1.toHexString() +";1.2.3.4:1235", httpString);
    	
    	PushEndpoint parsed = factory.createPushEndpoint(httpString);
    	assertEquals(RemoteFileDesc.BOGUS_IP,parsed.getAddress());
    }
    
    @SuppressWarnings("null")
    public void testUpdateProxiesAndOverwriteProxies() throws Exception {
        GUID g1 = new GUID();
        IpPortSet set1 = ippset(ppi1, ppi2, ppi3, ppi4);
        PushEndpoint pe = factory.createPushEndpoint(g1.bytes(), set1);
        pe.updateProxies(true);
        
        PushEndpoint pe2 = factory.createPushEndpoint(g1.bytes());
        assertEquals(0, pe2.getProxies().size());
        pe2.updateProxies(false);
        assertEquals(4, pe2.getProxies().size());
        
        // Basic overwrite.
        IpPortSet set2 = ippset(ppi5, ppi6);
        pushEndpointCache.overwriteProxies(g1.bytes(), set2);
        assertEquals(2, pe.getProxies().size());
        assertEquals(2, pe2.getProxies().size());
        
        // Overwrite w/ HTTP string
        pushEndpointCache.overwriteProxies(g1.bytes(), "1.2.3.4:5,1.2.3.5:5,1.2.3.6:6");
        assertEquals(3, pe.getProxies().size());
        assertEquals(3, pe2.getProxies().size());
        int tls = 0;
        for(IpPort ipp : pe.getProxies()) {
            if(ipp instanceof Connectable && ((Connectable)ipp).isTLSCapable())
                tls++;
        }
        assertEquals(0, tls);
        
        pushEndpointCache.overwriteProxies(g1.bytes(), "pptls=2,2.3.4.5:5,2.3.4.6:6,2.3.4.7:7");
        assertEquals(3, pe.getProxies().size());
        assertEquals(3, pe2.getProxies().size());
        tls = 0;
        IpPort tlsIpp = null;
        for(IpPort ipp : pe.getProxies()) {
            if(ipp instanceof Connectable && ((Connectable)ipp).isTLSCapable()) {
                tls++;
                tlsIpp = ipp;
            }
        }
        assertEquals(1, tls);
        assertEquals("2.3.4.7", tlsIpp.getAddress());
    }
    

    public void testToAndFromBytesWithFWT() throws Exception {
        GUID guid2 = new GUID(GUID.makeGuid());
        IpPortSet set6 = ippset(ppi1, ppi2, ppi3, ppi4, ppi5, ppi6);
        
        // test a PE that claims it supports FWT but doesn't have external address -
        // its FWT status gets cleared
        PushEndpoint six = factory.createPushEndpoint(guid2.bytes(), set6, (byte)0, 2);
        assertEquals(2,six.getFWTVersion());
        byte[] network = six.toBytes(false);
        assertEquals(AbstractPushEndpoint.getSizeBytes(six.getProxies(), false),network.length);
        
        pushEndpointCache.clear();
        PushEndpoint four = factory.createFromBytes(new DataInputStream(new ByteArrayInputStream(network)));
        assertEquals(0,four.getFWTVersion());
        assertEquals(4,four.getProxies().size());
        
        IpPortSet sent = new IpPortSet();
        sent.addAll(set6);
        assertTrue(set6.containsAll(four.getProxies()));
        
        // test a PE that carries its external address
        pushEndpointCache.clear();
        PushEndpoint ext = factory.createPushEndpoint(guid2.bytes(), set6, (byte)0, 2, new IpPortImpl("1.2.3.4",5));
        network = ext.toBytes(false);
        assertEquals(AbstractPushEndpoint.getSizeBytes(set6, false)+6,network.length);
        
        pushEndpointCache.clear();
        PushEndpoint ext2 = factory.createFromBytes(new DataInputStream(new ByteArrayInputStream(network)));
        assertEquals(ext,ext2);
        assertEquals("1.2.3.4",ext2.getAddress());
        assertEquals(5,ext2.getPort());
        assertEquals(4,ext2.getProxies().size());
        
        // test that a PE with external address which can't do FWT 
        // does not use up the extra 6 bytes
        
        pushEndpointCache.clear();
        PushEndpoint noFWT = factory.createPushEndpoint(guid2.bytes(), set6, (byte)0, 0, new IpPortImpl("1.2.3.4",5));
        network = noFWT.toBytes(false);
        assertEquals(AbstractPushEndpoint.getSizeBytes(set6, false),network.length);
        
        pushEndpointCache.clear();
        PushEndpoint noFWT2 = factory.createFromBytes(new DataInputStream(new ByteArrayInputStream(network)));
        assertEquals(noFWT,noFWT2);
        assertEquals(RemoteFileDesc.BOGUS_IP,noFWT2.getAddress());
        assertEquals(4,noFWT2.getProxies().size());
    }
    
    /**
     * Ensures invalid push endpoint binary representations that specify fwt
     * but don't provide a host are not deserialized into PushEndPoints, this
     * varies from reading invalid PushEndPoints from an http value.
     */
    public void testCreateFromBytesFWTPushEndPointWithoutHost() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // set fwt version to 1 and one proxy
        out.write(1 | 1 << 3);
        out.write(new GUID().bytes());
        out.write(new byte[] { (byte)129, 12, 1, 1 });
        ByteUtils.short2leb((short)5555, out);
        
        try {
            factory.createFromBytes(new DataInputStream(new ByteArrayInputStream(out.toByteArray())));
            fail("IOException expected for fwt push endpoint without host info");
        } catch (IOException ie) {
        }
    }

    public void testUnknownFeatures() throws Exception {
        PushEndpoint unknown = factory.createPushEndpoint("2A8CA57F43E6E0B7FF823F0CC7880500;someFeature/2.3;1.2.3.5:1235;1.2.3.6:1235");
    	assertEquals(2,unknown.getProxies().size());
    	assertEquals(0,unknown.getFWTVersion());
    	
    	//now an endpoint with the fwt header moved elsewhere
    	unknown = factory.createPushEndpoint("2A8CA57F43E6E0B7FF823F0CC7880500;1.2.3.5:1235;fwt/1.3;1.2.3.6:1235;555:129.1.2.3");
    	assertEquals(2,unknown.getProxies().size());
    	assertEquals(1,unknown.getFWTVersion());
    	
    	//now an endpoint only with the guid
    	unknown = factory.createPushEndpoint("2A8CA57F43E6E0B7FF823F0CC7880500");
    	assertEquals(0,unknown.getProxies().size());
    	assertEquals(0,unknown.getFWTVersion());
    	
    	//now an endpoint only guid and port:ip
    	unknown = factory.createPushEndpoint("2A8CA57F43E6E0B7FF823F0CC7880500;5:1.2.3.4");
    	assertEquals(0,unknown.getProxies().size());
    	assertEquals(0,unknown.getFWTVersion());
    	assertEquals("1.2.3.4",unknown.getAddress());
    	assertEquals(5,unknown.getPort());
    	
    	//now an endpoint only guid and two port:ips.. the second one should be ignored
    	unknown = factory.createPushEndpoint("2A8CA57F43E6E0B7FF823F0CC7880500;5:1.2.3.4;6:2.3.4.5");
    	assertEquals(0,unknown.getProxies().size());
    	assertEquals(0,unknown.getFWTVersion());
    	assertEquals("1.2.3.4",unknown.getAddress());
    	assertEquals(5,unknown.getPort());
    }
    
    private IpPortSet ippset(IpPort... ipps) {
        IpPortSet set = new IpPortSet();
        for(int i = 0; i < ipps.length; i++)
            set.add(ipps[i]);
        return set;
    }
    
}
