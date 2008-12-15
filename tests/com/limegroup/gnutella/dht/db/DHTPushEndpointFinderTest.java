package com.limegroup.gnutella.dht.db;

import org.limewire.io.GUID;

import junit.framework.Test;

import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.dht.util.KUIDUtils;

public class DHTPushEndpointFinderTest extends DHTFinderTestCase {

    private DHTPushEndpointFinder pushEndpointFinder;

    public DHTPushEndpointFinderTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(DHTPushEndpointFinderTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        pushEndpointFinder = injector.getInstance(DHTPushEndpointFinder.class);
    }

    public void testGetPushEndPoint() throws Exception {
        // publish push proxy manually
        PushProxiesValue pushProxiesValue = pushProxiesValueFactory.createDHTValueForSelf();
        mojitoDHT.put(KUIDUtils.toKUID(new GUID(pushProxiesValue.getGUID())), pushProxiesValue).get();

        PushEndpoint pushEndpoint = pushEndpointFinder.getPushEndpoint(new GUID(pushProxiesValue.getGUID()));
        // only compare values, so we don't have to recreate the push endpoint
        assertEquals(pushProxiesValue.getGUID(), pushEndpoint.getClientGUID());
        assertEquals(pushProxiesValue.getPushProxies(), pushEndpoint.getProxies());
        assertEquals(pushProxiesValue.getFeatures(), pushEndpoint.getFeatures());
        assertEquals(pushProxiesValue.getFwtVersion(), pushEndpoint.getFWTVersion());
    }
    
    public void testGetUnavailablePushEndpoint() throws Exception {
        PushEndpoint result = pushEndpointFinder.getPushEndpoint(new GUID());
        assertNull(result);
    }
    
}
