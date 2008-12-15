package com.limegroup.gnutella.dht.db;

import java.net.InetSocketAddress;
import java.util.List;

import junit.framework.Test;

import org.limewire.core.settings.DHTSettings;
import org.limewire.core.settings.NetworkSettings;
import org.limewire.io.GUID;
import org.limewire.io.IOUtils;
import org.limewire.io.NetworkUtils;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.util.MojitoUtils;
import org.limewire.util.PrivilegedAccessor;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.limegroup.gnutella.Acceptor;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.ExtendedEndpoint;
import com.limegroup.gnutella.HostCatcher;
import com.limegroup.gnutella.LifecycleManager;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.DHTTestUtils;
import com.limegroup.gnutella.dht.DHTManager.DHTMode;
import com.limegroup.gnutella.stubs.ConnectionManagerStub;
import com.limegroup.gnutella.stubs.LocalSocketAddressProviderStub;
import com.limegroup.gnutella.stubs.NetworkManagerStub;
import com.limegroup.gnutella.util.LimeTestCase;

/**
 * Integration test to ensure that push proxies are published.
 */
public class PushProxyPublishingTest extends LimeTestCase {

    private NetworkManagerStub networkManagerStub = new NetworkManagerStub();
    
    private Injector injector;

    private DHTManager dhtManager;

    private List<MojitoDHT> dhts;

    private HostCatcher hostCatcher;

    public PushProxyPublishingTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(PushProxyPublishingTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        PrivilegedAccessor.setValue(DHTSettings.PUSH_PROXY_STABLE_PUBLISHING_INTERVAL, "value", 1000L);
        DHTSettings.DISABLE_DHT_USER.setValue(false);
        DHTSettings.DISABLE_DHT_NETWORK.setValue(false);
        DHTSettings.FORCE_DHT_CONNECT.setValue(true);
        DHTTestUtils.setSettings(NetworkSettings.PORT.getValue());
        PrivilegedAccessor.setValue(DHTSettings.DHT_NODE_FETCHER_TIME, "value", 500L);
        
        injector = LimeTestUtils.createInjectorAndStart(LocalSocketAddressProviderStub.STUB_MODULE, new AbstractModule() {
            @Override
            protected void configure() {
                bind(NetworkManager.class).toInstance(networkManagerStub);
                bind(ConnectionManager.class).to(ConnectionManagerStub.class);
            }
        });
        dhtManager = injector.getInstance(DHTManager.class);
        hostCatcher = injector.getInstance(HostCatcher.class);
        DHTTestUtils.setLocalIsPrivate(injector, false);
        
        networkManagerStub.setCanReceiveSolicited(true);
        networkManagerStub.setAcceptedIncomingConnection(true);
        networkManagerStub.setAddress(NetworkUtils.getLocalAddress().getAddress());
     
        ((ConnectionManagerStub)injector.getInstance(ConnectionManager.class)).setConnected(true);
        
        Acceptor acceptor = injector.getInstance(Acceptor.class);
        networkManagerStub.setPort(acceptor.getPort(false));
        
        // make sure address is updated which isn't done by mock network manager
        dhtManager.addressChanged();
        
        dhts = MojitoUtils.createBootStrappedDHTs(1);
    }
    
    @Override
    protected void tearDown() throws Exception {
        injector.getInstance(LifecycleManager.class).shutdown();
        IOUtils.close(dhts);
    }
    
    public void testPushProxiesArePublished() throws Exception {
        MojitoDHT dht = dhts.get(0);
        assertTrue(dht.isBootstrapped());
        
        ExtendedEndpoint endpoint = new ExtendedEndpoint((InetSocketAddress)dht.getContactAddress());
        endpoint.setDHTMode(DHTMode.ACTIVE);
        endpoint.setDHTVersion(dhtManager.getVersion().shortValue());
        
        hostCatcher.add(endpoint, true);
        
        DHTTestUtils.waitForBootStrap(dhtManager, 5);
        
        // should have published after 3 secs with  a publishing interval of 1 sec
        Thread.sleep(3 * 1000);
        
        DHTPushEndpointFinder finder = injector.getInstance(DHTPushEndpointFinder.class);
        GUID guid = new GUID(injector.getInstance(ApplicationServices.class).getMyGUID());
        PushEndpoint pushEndpoint = finder.getPushEndpoint(guid);
        assertNotNull(pushEndpoint);
        assertEquals(networkManagerStub.getPort(), pushEndpoint.getPort());
        assertEquals(guid.bytes(), pushEndpoint.getClientGUID());
        
    }
}
