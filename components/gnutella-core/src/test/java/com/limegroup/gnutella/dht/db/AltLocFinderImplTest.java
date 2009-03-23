package com.limegroup.gnutella.dht.db;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.limewire.io.GUID;
import org.limewire.io.IpPortImpl;
import org.limewire.mojito.KUID;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.dht.util.KUIDUtils;
import com.limegroup.gnutella.security.MerkleTree;

public class AltLocFinderImplTest extends DHTFinderTestCase {

    private AltLocFinder altLocFinder;

    public AltLocFinderImplTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(AltLocFinderImplTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        altLocFinder = injector.getInstance(AltLocFinder.class);
    }
    
    public void testAltLocListenerIsNotifiedOfNonFirewalledLocations() throws Exception {
        testAltLocListenerNonFirewalledLocations(true);
    }
    
    public void testAltLocListenerIsNotNotifiedOfNonFirewalledLocations() throws Exception {
        testAltLocListenerNonFirewalledLocations(false);
    }
    
    private URN publishDirectAltLoc(boolean publish) throws Exception {
        // set to non-firewalled, so created altloc value for self is not firewalled
        networkManager.setAcceptedIncomingConnection(true);
        // publish an alternate location in the DHT
        URN urn = URN.createSHA1Urn("urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB");
        AltLocValue value = altLocValueFactory.createAltLocValueForSelf(5555, new byte[MerkleTree.HASHSIZE]);
        assertFalse(value.isFirewalled());
        KUID kuid = KUIDUtils.toKUID(urn);

        // publish altloc value manually for successful case
        if (publish) {
            mojitoDHT.put(kuid, value).get();
        }
        return urn;
    }
    
    private void testAltLocListenerNonFirewalledLocations(boolean successful) throws Exception {
        URN urn = publishDirectAltLoc(successful);
        // expected alternate location
        final AlternateLocation expectedAltLoc = alternateLocationFactory.createDirectDHTAltLoc(new IpPortImpl(networkManager.getAddress(), networkManager.getPort()), urn, 5555, new byte[MerkleTree.HASHSIZE]);
        AltLocSearchHandler listener = new AltLocSearchHandler();
        // initialize with other value to see if it is actually called
        listener.success = !successful;
        
        altLocFinder.findAltLocs(urn, listener);
        listener.doneLatch.await(500, TimeUnit.MILLISECONDS);
        listener.alternateLocationLatch.await(500, TimeUnit.MILLISECONDS);

        if (successful) {
            assertEquals(expectedAltLoc, listener.altLoc);
        } else {
            assertNull(listener.altLoc);
            assertFalse(listener.success);
        }
    }
    
    public void testAltLocListenerIsNotifedOfFirewalledLocations() throws Exception {
        testAltLocListenerFirewalledLocations(true);
    }
    
    public void testAltLocListenerIsNotNotifedOfFirewalledLocations() throws Exception {
        testAltLocListenerFirewalledLocations(false);
    }
    
    private URN publishPushAltLoc(boolean publish) throws Exception {
        networkManager.setExternalAddress(new byte[] { 127, 0, 0, 1 });
        
        URN urn = URN.createSHA1Urn("urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB");
        AltLocValue value = altLocValueFactory.createAltLocValueForSelf(5555, new byte[MerkleTree.HASHSIZE]);
        assertTrue(value.isFirewalled());
        KUID kuid = KUIDUtils.toKUID(urn);

        // publish altloc value manually
        mojitoDHT.put(kuid, value).get();
        // publish push proxy manually
        PushProxiesValue pushProxiesValue = pushProxiesValueFactory.createDHTValueForSelf();
        if (publish) {
            mojitoDHT.put(KUIDUtils.toKUID(new GUID(pushProxiesValue.getGUID())), pushProxiesValue).get();
        }
        return urn;
    }
    
    private void testAltLocListenerFirewalledLocations(boolean successful) throws Exception {
        URN urn = publishPushAltLoc(successful);
        
        AltLocSearchHandler listener = new AltLocSearchHandler();
        // initialize with opposite value to see if it is actually set
        listener.success = !successful;
        
        altLocFinder.findAltLocs(urn, listener);
        listener.doneLatch.await(500, TimeUnit.MILLISECONDS);
        listener.alternateLocationLatch.await(500, TimeUnit.MILLISECONDS);

        // expected alternate location
        AlternateLocation expectedAltLoc = alternateLocationFactory.createPushAltLoc(pushEndpointFactory.createForSelf(), urn);
        
        if (successful) {
            assertEquals(expectedAltLoc, listener.altLoc);
        } else {
            assertNull(listener.altLoc);
            assertFalse(listener.success);
        }
    }
    
    private static class AltLocSearchHandler implements SearchListener<AlternateLocation> {

        CountDownLatch doneLatch = new CountDownLatch(1);
        CountDownLatch alternateLocationLatch = new CountDownLatch(1);
        volatile AlternateLocation altLoc;
        volatile boolean success;
        
        public void searchFailed() {
            this.success = false;
            doneLatch.countDown();
        }

        public void handleResult(AlternateLocation alternateLocation) {
            altLoc = alternateLocation;
            alternateLocationLatch.countDown();
        }
    };

}
