package com.limegroup.gnutella.connection;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.limewire.io.NetworkInstanceUtils;
import org.limewire.net.ConnectionDispatcher;
import org.limewire.net.SocketsManager;
import org.limewire.util.BaseTestCase;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.ConnectionManagerImpl;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.HostCatcher;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.NodeAssigner;
import com.limegroup.gnutella.QueryUnicaster;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.messages.PingRequestFactory;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactory;
import com.limegroup.gnutella.simpp.SimppManager;

public class ConnectionCheckerManagerImplTest extends BaseTestCase {

    private ConnectionCheckerManagerImpl connectionCheckerManager;
    private ConnectionManagerStub connectionManager;

    public ConnectionCheckerManagerImplTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(ConnectionCheckerManagerImplTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        Injector injector = LimeTestUtils.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(ConnectionManager.class).to(ConnectionManagerStub.class);
            }
        });
        
        connectionManager = (ConnectionManagerStub) injector.getInstance(ConnectionManager.class);
        connectionCheckerManager = (ConnectionCheckerManagerImpl) injector.getInstance(ConnectionCheckerManager.class);
    }
    
    public void testCheckForLiveConnection() throws Exception {
        Future<Boolean> result = connectionCheckerManager.checkForLiveConnection();
        assertTrue(result.get(12, TimeUnit.SECONDS));
        assertFalse(connectionManager.noInternetConnection);
        assertTrue(connectionCheckerManager.isConnected());
        assertEquals(0, connectionCheckerManager.getNumWorkarounds());
    }

    @Singleton
    private static class ConnectionManagerStub extends ConnectionManagerImpl {

        private boolean noInternetConnection;

        @Inject
        public ConnectionManagerStub(NetworkManager networkManager,
                Provider<HostCatcher> hostCatcher,
                @Named("global") Provider<ConnectionDispatcher> connectionDispatcher,
                @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor, Provider<SimppManager> simppManager,
                CapabilitiesVMFactory capabilitiesVMFactory,
                RoutedConnectionFactory managedConnectionFactory,
                Provider<QueryUnicaster> queryUnicaster,
                SocketsManager socketsManager, ConnectionServices connectionServices,
                Provider<NodeAssigner> nodeAssigner, 
                 Provider<IPFilter> ipFilter,
                ConnectionCheckerManager connectionCheckerManager,
                PingRequestFactory pingRequestFactory,
                NetworkInstanceUtils networkInstanceUtils) {
            super(networkManager, hostCatcher, connectionDispatcher, backgroundExecutor, simppManager,
                    capabilitiesVMFactory, managedConnectionFactory, queryUnicaster,
                    socketsManager, connectionServices, nodeAssigner, ipFilter, connectionCheckerManager,
                    pingRequestFactory, networkInstanceUtils);
        }
        
        @Override
        public void noInternetConnection() {
            noInternetConnection = true;
        }
        
    }
    
}
