package com.limegroup.gnutella.stubs;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import org.limewire.io.Connectable;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.net.ConnectionDispatcher;
import org.limewire.net.SocketsManager;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.ConnectionManagerImpl;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.HostCatcher;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.NodeAssigner;
import com.limegroup.gnutella.QueryUnicaster;
import com.limegroup.gnutella.connection.ConnectionCheckerManager;
import com.limegroup.gnutella.connection.RoutedConnection;
import com.limegroup.gnutella.connection.RoutedConnectionFactory;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.handshaking.HandshakeResponse;
import com.limegroup.gnutella.handshaking.HandshakeStatus;
import com.limegroup.gnutella.messages.PingRequestFactory;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactory;
import com.limegroup.gnutella.simpp.SimppManager;

/** A (incomplete!) stub for ConnectionManager. */
@Singleton
public class ConnectionManagerStub extends ConnectionManagerImpl {

    private boolean enableRemove = false;

    private Boolean connected;

    private Set<Connectable> pushProxies;

    private Boolean fullyConnected;

    private List<RoutedConnection> initializedConnections;

    private Integer preferredConnectionCount;

    @Inject
    public ConnectionManagerStub(NetworkManager networkManager, Provider<HostCatcher> hostCatcher,
            @Named("global")
            Provider<ConnectionDispatcher> connectionDispatcher, @Named("backgroundExecutor")
            ScheduledExecutorService backgroundExecutor, Provider<SimppManager> simppManager,
            CapabilitiesVMFactory capabilitiesVMFactory,
            RoutedConnectionFactory managedConnectionFactory,
            Provider<QueryUnicaster> queryUnicaster,
            SocketsManager socketsManager, ConnectionServices connectionServices,
            Provider<NodeAssigner> nodeAssigner, Provider<IPFilter> ipFilter,
            ConnectionCheckerManager connectionCheckerManager, PingRequestFactory pingRequestFactory,
            NetworkInstanceUtils networkInstanceUtils) {
        super(networkManager, hostCatcher, connectionDispatcher, backgroundExecutor, simppManager,
                capabilitiesVMFactory, managedConnectionFactory, queryUnicaster,
                socketsManager, connectionServices, nodeAssigner, ipFilter,
                connectionCheckerManager, pingRequestFactory, networkInstanceUtils);
    }

    /** Calls c.close iff enableRemove */
    @Override
    public void remove(RoutedConnection c) {
        if (enableRemove)
            c.close();
    }

    @Override
    public boolean isConnected() {
        if(connected != null)
            return connected;
        else
            return super.isConnected();
    }

    @Override
    public boolean isSupernode() {
        return true;
    }

    @Override
    public HandshakeStatus allowConnection(HandshakeResponse hr) {
        return HandshakeStatus.OK;
    }

    @Override
    public String toString() {
        return "ConnectionManagerStub";
    }

    public void setEnableRemove(boolean enableRemove) {
        this.enableRemove = enableRemove;
    }

    public void setConnected(boolean connected) {
        this.connected = Boolean.valueOf(connected);
    }

    public void setPushProxies(Set<? extends Connectable> pushProxies) {
        this.pushProxies = Collections.unmodifiableSet(pushProxies);
    }

    @Override
    public Set<Connectable> getPushProxies() {
        if (pushProxies != null)
            return pushProxies;
        else
            return super.getPushProxies();
    }

    public void setFullyConnected(boolean fullyConnected) {
        this.fullyConnected = Boolean.valueOf(fullyConnected);
    }
    

    public void setInitializedConnections(List<RoutedConnection> initializedConnections) {
        this.initializedConnections = initializedConnections;
    }

    public void setPreferredConnectionCount(int i) {
        this.preferredConnectionCount = Integer.valueOf(i);
    }


    @Override
    public List<RoutedConnection> getInitializedConnections() {
        if(initializedConnections != null)
            return initializedConnections;
        else
            return super.getInitializedConnections();
    }

    @Override
    public int getPreferredConnectionCount() {
        if(preferredConnectionCount != null)
            return preferredConnectionCount;
        else
            return super.getPreferredConnectionCount();
    }

    @Override
    public boolean isFullyConnected() {
        if(fullyConnected != null)
            return fullyConnected;
        else
            return super.isFullyConnected();
    }
    
}
