package com.limegroup.gnutella.dht;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.AssertionFailedError;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.DHTSettings;
import org.limewire.core.settings.FilterSettings;
import org.limewire.core.settings.PingPongSettings;
import org.limewire.io.LocalSocketAddressProvider;
import org.limewire.mojito.settings.ContextSettings;
import org.limewire.mojito.settings.NetworkSettings;
import org.limewire.util.AssertComparisons;
import org.limewire.util.BaseTestCase;

import com.google.inject.Injector;
import com.limegroup.gnutella.dht.DHTEvent.Type;
import com.limegroup.gnutella.stubs.LocalSocketAddressProviderStub;

public class DHTTestUtils {

    public static void setSettings(int port) {
        FilterSettings.BLACK_LISTED_IP_ADDRESSES.setValue(
                new String[] {"*.*.*.*"});
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.setValue(
                new String[] {"127.*.*.*", "18.239.0.*"});
                
        org.limewire.core.settings.NetworkSettings.PORT.setValue(port);
        BaseTestCase.assertEquals("unexpected port", port, org.limewire.core.settings.NetworkSettings.PORT.getValue());
                
        ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        ConnectionSettings.WATCHDOG_ACTIVE.setValue(false);
        PingPongSettings.PINGS_ACTIVE.setValue(false);
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(false);
        
        // DHT Settings
        DHTSettings.PERSIST_ACTIVE_DHT_ROUTETABLE.setValue(false);
        DHTSettings.PERSIST_DHT_DATABASE.setValue(false);
        DHTSettings.ENABLE_PUSH_PROXY_QUERIES.setValue(true);
        ContextSettings.SHUTDOWN_MESSAGES_MULTIPLIER.setValue(0);
        
         // We're working on the loopback. Everything should be done
        // in less than 500ms
        NetworkSettings.DEFAULT_TIMEOUT.setValue(500);
        
        // Nothing should take longer than 1.5 seconds. If we start seeing
        // LockTimeoutExceptions on the loopback then check this Setting!
        ContextSettings.WAIT_ON_LOCK.setValue(1500);
        
    }

    public static void setLocalIsPrivate(Injector injector, boolean localIsPrivate) {
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(localIsPrivate);
        NetworkSettings.FILTER_CLASS_C.setValue(localIsPrivate);
        NetworkSettings.LOCAL_IS_PRIVATE.setValue(false);
        LocalSocketAddressProviderStub localSocketAddressProviderStub = (LocalSocketAddressProviderStub) injector.getInstance(LocalSocketAddressProvider.class);
        localSocketAddressProviderStub.setLocalAddressPrivate(localIsPrivate);
    }

    /**
     * Waits up to <code>seconds</code> fo the dht to be bootstrapped.
     * 
     * @throw {@link AssertionFailedError} if not bootstrapped.
     */
    public static void waitForBootStrap(DHTManager dhtManager, int seconds) throws Exception {
        final CountDownLatch bootStrapped = new CountDownLatch(1);
        dhtManager.addEventListener(new DHTEventListener() {
            public void handleDHTEvent(DHTEvent evt) {
                if (evt.getType() == Type.CONNECTED) {
                    bootStrapped.countDown();
                }
            }
        });
        if (!dhtManager.isBootstrapped()) {
            AssertComparisons.assertTrue(bootStrapped.await(seconds, TimeUnit.SECONDS));
        }
    }
}
