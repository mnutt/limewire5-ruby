package org.limewire.mojito.handler.request;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;

import junit.framework.TestSuite;

import org.limewire.mojito.Context;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.MojitoFactory;
import org.limewire.mojito.MojitoTestCase;
import org.limewire.mojito.exceptions.DHTException;
import org.limewire.mojito.result.PingResult;
import org.limewire.mojito.settings.ContextSettings;
import org.limewire.mojito.settings.NetworkSettings;
import org.limewire.mojito.util.UnitTestUtils;

public class PingRequestHandlerTest extends MojitoTestCase {
    
    public PingRequestHandlerTest(String name) {
        super(name);
    }

    public static TestSuite suite() {
        return buildTestSuite(PingRequestHandlerTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setLocalIsPrivate(false);
    }

    public void testPingRequest() throws Exception {
        final long waitForFutureDone = 250; // ms
        
        NetworkSettings.MAX_ERRORS.setValue(0);
        NetworkSettings.DEFAULT_TIMEOUT.setValue(250);
        
        MojitoDHT dht1 = null;
        MojitoDHT dht2 = null;
        
        try {
            
            dht1 = MojitoFactory.createDHT();
            dht1.bind(2000);
            dht1.start();
            
            dht2 = MojitoFactory.createDHT();
            dht2.bind(3000);
            dht2.start();
            
            // Regular Ping
            try {
                PingResult result = dht2.ping(new InetSocketAddress("localhost", 2000)).get();
                assertNotNull(result);
            } catch (ExecutionException err) {
                fail(err);
            }
            
            // FutureTask.get() returns before FutureTask.done()
            // is called. We use the done() method internally to
            // deregister ping tasks. If done() doesn't get called
            // before we're doing out next ping it will return the
            // same Future handle (i.e. there's no ping sent!)...
            Thread.sleep(waitForFutureDone);
            
            // A Node that's bootstrapping should not respond to
            // Pings to prevent other Nodes from selecting it as
            // their initial bootstrap Node
            Context context1 = (Context)dht1;
            
            UnitTestUtils.setBootstrapping(dht1, true);
            assertFalse(dht1.isBootstrapped());
            assertTrue(dht1.isBootstrapping());
            
            try {
                PingResult result = dht2.ping(new InetSocketAddress("localhost", 2000)).get();
                assertFalse(dht1.isBootstrapped());
                assertTrue(dht1.isBootstrapping());
                fail("DHT-1 did respond to our request " + result);
            } catch (ExecutionException expected) {
                assertTrue(expected.getCause() instanceof DHTException);
            }
            
            Thread.sleep(waitForFutureDone);
            
            // next collision Ping. Different Node IDs -> should not work (same as above)
            context1.setContactAddress(new InetSocketAddress("localhost", 2000));
            Context context2 = (Context)dht2;
            
            // Disable local assertion to make sure the network level
            // collision ping checks work
            ContextSettings.ASSERT_COLLISION_PING.setValue(false);
            try {
                PingResult result = context2.collisionPing(dht1.getLocalNode()).get();
                fail("DHT-1 did respond to our request " + result);
            } catch (ExecutionException expected) {
                assertTrue(expected.getCause() instanceof DHTException);
            } catch (IllegalArgumentException err) {
                fail("Should not have thrown an IllegalArgumentException", err);
            }
            
            Thread.sleep(waitForFutureDone);
            
            // Re-Enable local assertion to make sure you can't create
            // malformed collision pings like above
            ContextSettings.ASSERT_COLLISION_PING.setValue(true);
            try {
                PingResult result = context2.collisionPing(dht1.getLocalNode()).get();
                fail("DHT-1 did respond to our request " + result);
            } catch (ExecutionException expected) {
                assertTrue(expected.getCause() instanceof DHTException);
            } catch (IllegalArgumentException expected) {
            }
            
            Thread.sleep(waitForFutureDone);
            
            // Set DHT-2's Node ID to DHT-1 and try again. This should work!
            UnitTestUtils.setNodeID(dht2, dht1.getLocalNodeID());
            try {
                PingResult result = context2.collisionPing(dht1.getLocalNode()).get();
                assertNotNull(result);
            } catch (ExecutionException err) {
                fail(err);
            }
            
            // Make sure DHT-1 is still bootstrapping!
            assertFalse(dht1.isBootstrapped());
            assertTrue(context1.isBootstrapping());
        } finally {
            if (dht1 != null) { dht1.close(); }
            if (dht2 != null) { dht2.close(); }
        }
    }
}
