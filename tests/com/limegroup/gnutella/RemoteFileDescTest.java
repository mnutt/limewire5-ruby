package com.limegroup.gnutella;


import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Test;

import org.limewire.io.ConnectableImpl;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.io.IpPortSet;

import com.google.inject.Injector;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;
import com.limegroup.gnutella.helpers.UrnHelper;
import com.limegroup.gnutella.http.HTTPConstants;

/**
 * This class tests the methods of the <tt>RemoteFileDesc</tt> class.
 */
@SuppressWarnings("unchecked")
public final class RemoteFileDescTest extends com.limegroup.gnutella.util.LimeTestCase {	  

	private byte[] TEST_GUID;
    private PushEndpointFactory pushEndpointFactory;
    private RemoteFileDescFactory remoteFileDescFactory;

	public RemoteFileDescTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(RemoteFileDescTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

	@Override
    protected void setUp() {
	    Injector injector = LimeTestUtils.createInjector();
	    pushEndpointFactory = injector.getInstance(PushEndpointFactory.class);
	    remoteFileDescFactory = injector.getInstance(RemoteFileDescFactory.class);
		TEST_GUID = GUID.makeGuid();
	}

	/**
	 * Make sure that invalid port values are rejected.
	 */
	public void testConstructorWithInvalidPorts() throws Exception {
		int[] invalidPorts = new int[2];
		invalidPorts[0] = -1;
		invalidPorts[1] = 66000;
		for(int i=0; i<invalidPorts.length; i++) {
			try {
			    remoteFileDescFactory.createRemoteFileDesc(new ConnectableImpl("www.limewire.org", invalidPorts[i], false), 10, "test", 10,
			            TEST_GUID, 10, false, 3, false, null, URN.NO_URN_SET, false, "", -1);
			    fail("rfd1 should have received an exception for invalid port");
			} catch(IllegalArgumentException iae) {
				// this is expected
			}
		}
	}

	/**
	 * Make sure that valid port values are accepted.
	 */
	public void testConstructorWithValidPorts() throws Exception {
		int[] validPorts = new int[2];
		validPorts[0] = 10000;
		validPorts[1] = 6000;
		for(int i=0; i<validPorts.length; i++) {
			try {
			    remoteFileDescFactory.createRemoteFileDesc(new ConnectableImpl("www.limewire.org", validPorts[i], false), 10, "test", 10,
                        TEST_GUID, 10, false, 3, false, null, URN.NO_URN_SET, false, "", -1);
			} catch(IllegalArgumentException e) {
				fail("rfd1 should not have received an exception for valid port: "+
					 validPorts[i], e);
			}
		}
	}

	/**
	 * Tests the getUrl method to make sure it's correctly constructing URLs.
	 */
	public void testRemoteFileDescGetUrlPath() throws Exception {
		Set urns = new HashSet();
		urns.add(UrnHelper.URNS[0]);
		RemoteFileDesc rfd =
		    remoteFileDescFactory.createRemoteFileDesc(new ConnectableImpl("www.test.org", 3000, false), 10, "test", 10, TEST_GUID, 10, true, 3,
                true, null, urns, false, "", -1);
		String urlPath = rfd.getUrlPath();
		assertEquals(HTTPConstants.URI_RES_N2R + UrnHelper.URNS[0].httpStringValue(), urlPath);		
	}
	
	/**
	 * tests if the rfd correctly determines if it is altloc and push capable
	 */
	public void testIsAltlocPushCapable() throws Exception {
		
		IpPort ppi = new IpPortImpl("1.2.3.4",6346);
		IpPort ppi2 = new IpPortImpl("1.2.3.4",6346);
		Set proxies = new IpPortSet();
		Set proxies2 = new IpPortSet();
		proxies.add(ppi);
		proxies2.add(ppi);
		proxies2.add(ppi2);
		
		GUID g1 = new GUID();
		GUID g2 = new GUID();
        PushEndpoint pe = pushEndpointFactory.createPushEndpoint(g1.bytes(), proxies);
        PushEndpoint pe2 = pushEndpointFactory.createPushEndpoint(g2.bytes(), proxies2);
        
        //test an rfd with push proxies
		 RemoteFileDesc fwalled = remoteFileDescFactory.createRemoteFileDesc(pe, 10, HTTPConstants.URI_RES_N2R+
                UrnHelper.URNS[0].httpStringValue(), 10,
                pe.getClientGUID(), 10, true, 2, true, null, UrnHelper.URN_SETS[0], false, "", -1);
		 
		 assertTrue(Arrays.equals(pe.getClientGUID(),fwalled.getClientGUID()));
		 
		 RemoteFileDesc nonfwalled = 
		     remoteFileDescFactory.createRemoteFileDesc(new ConnectableImpl("www.limewire.org", 6346, false), 10, HTTPConstants.URI_RES_N2R+
        				   UrnHelper.URNS[1].httpStringValue(), 10,
                GUID.makeGuid(), 10, true, 2, true, null, UrnHelper.URN_SETS[1], false, "", -1);
		 
		 RemoteFileDesc differentPE = remoteFileDescFactory.createRemoteFileDesc(fwalled, pe2);
		 assertTrue(Arrays.equals(pe2.getClientGUID(),differentPE.getClientGUID()));
		 
		 //both rfds should report as being altloc capable, but only
		 //the firewalled rfd should be pushCapable
		 assertTrue(fwalled.isAltLocCapable());
		 assertTrue(fwalled.getAddress() instanceof PushEndpoint);
		 assertTrue(nonfwalled.isAltLocCapable());
		 assertFalse(nonfwalled instanceof PushEndpoint);
		 
		 //now create an rfd which claims to be firewalled but has no push proxies
		 GUID g3 = new GUID();
		 PushEndpoint noProxies = pushEndpointFactory.createPushEndpoint(g3.bytes());

		 RemoteFileDesc fwalledNotGood = 
		     remoteFileDescFactory.createRemoteFileDesc(fwalled, noProxies);
		 
		 //it should not be a capable altloc.
		 assertFalse(fwalledNotGood.isAltLocCapable());
		 assertTrue(fwalledNotGood.getAddress() instanceof PushEndpoint);
	}
	
}
