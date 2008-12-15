package com.limegroup.gnutella;

import java.net.Socket;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.limewire.io.Connectable;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.net.SocketsManager.ConnectType;

import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.connection.ConnectionLifecycleListener;
import com.limegroup.gnutella.connection.RoutedConnection;
import com.limegroup.gnutella.connection.GnutellaConnectionEvent;
import com.limegroup.gnutella.handshaking.HandshakeResponse;
import com.limegroup.gnutella.handshaking.HandshakeStatus;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.messages.vendor.QueryStatusResponse;

public class ConnectionManagerAdapter implements ConnectionManager {

    @Override
    public int getNumFetchingConnections() {
        // TODO Auto-generated method stub
        return 0;
    }
    
    public void acceptConnection(Socket socket) {

    }

    public boolean allowAnyConnection() {

        return false;
    }

    public HandshakeStatus allowConnection(HandshakeResponse hr) {

        return null;
    }

    public HandshakeStatus allowConnection(HandshakeResponse hr, boolean leaf) {

        return null;
    }

    public HandshakeStatus allowConnectionAsLeaf(HandshakeResponse hr) {

        return null;
    }

    public boolean allowLeafDemotion() {

        return false;
    }

    public boolean canSendConnectBack(Network network) {

        return false;
    }

    public void connect() {

    }

    public void connectBackSent(Network network) {

    }

    public boolean connectionInitialized(RoutedConnection c) {

        return false;
    }

    public void connectionInitializingIncoming(RoutedConnection c) {

    }

    public int countConnectionsWithNMessages(int messageThreshold) {

        return 0;
    }

    public void createConnectionAsynchronously(String hostname, int portnum, ConnectType type) {

    }

    public void disconnect(boolean willTryToReconnect) {

    }

    public int getActiveConnectionMessages() {

        return 0;
    }

    public Endpoint getConnectedGUESSUltrapeer() {

        return null;
    }

    public List<RoutedConnection> getConnectedGUESSUltrapeers() {
        return Collections.emptyList();
    }

    public List<RoutedConnection> getConnections() {
        return Collections.emptyList();
    }

    public long getCurrentAverageUptime() {

        return 0;
    }

    public List<RoutedConnection> getInitializedClientConnections() {
        return Collections.emptyList();
    }

    public List<RoutedConnection> getInitializedClientConnectionsMatchLocale(String loc) {
        return Collections.emptyList();
    }

    public List<RoutedConnection> getInitializedConnections() {
        return Collections.emptyList();
    }

    public List<RoutedConnection> getInitializedConnectionsMatchLocale(String loc) {
        return Collections.emptyList();
    }

    public float getMeasuredDownstreamBandwidth() {

        return 0;
    }

    public float getMeasuredUpstreamBandwidth() {

        return 0;
    }

    public int getNumClientSupernodeConnections() {

        return 0;
    }

    public int getNumConnections() {

        return 0;
    }

    public int getNumFreeLeafSlots() {

        return 0;
    }

    public int getNumFreeLimeWireLeafSlots() {

        return 0;
    }

    public int getNumFreeLimeWireNonLeafSlots() {

        return 0;
    }

    public int getNumFreeNonLeafSlots() {

        return 0;
    }

    public int getNumInitializedClientConnections() {

        return 0;
    }

    public int getNumInitializedConnections() {

        return 0;
    }

    public int getNumLimeWireLocalePrefSlots() {

        return 0;
    }

    public int getNumOldConnections() {

        return 0;
    }

    public int getNumUltrapeerConnections() {

        return 0;
    }

    public int getPreferredConnectionCount() {

        return 0;
    }

    public Set<Connectable> getPushProxies() {
        return Collections.emptySet();
    }

    public boolean hasFreeSlots() {

        return false;
    }

    public boolean hasSupernodeClientConnection() {

        return false;
    }

    public void start() {

    }

    public boolean isActiveSupernode() {

        return false;
    }

    public boolean isBehindProxy() {

        return false;
    }

    public boolean isConnected() {

        return false;
    }

    public boolean isConnectedTo(String hostName) {

        return false;
    }

    public boolean isConnecting() {

        return false;
    }

    public boolean isConnectingTo(IpPort host) {

        return false;
    }

    public boolean isConnectionIdle() {

        return false;
    }

    public boolean isFullyConnected() {

        return false;
    }

    public boolean isLocaleMatched() {

        return false;
    }

    public boolean isShieldedLeaf() {

        return false;
    }

    public boolean isSupernode() {

        return false;
    }

    public boolean isSupernodeCapable() {

        return false;
    }

    public void measureBandwidth() {

    }

    public void noInternetConnection() {

    }

    public void remove(RoutedConnection mc) {

    }

    public boolean sendTCPConnectBackRequests() {

        return false;
    }

    public boolean sendUDPConnectBackRequests(GUID cbGuid) {

        return false;
    }

    public void sendUpdatedCapabilities() {

    }

    public boolean supernodeNeeded() {

        return false;
    }

    public void tryToBecomeAnUltrapeer(int demotionLimit) {

    }

    public void updateQueryStatus(QueryStatusResponse stat) {

    }

    public void acceptConnection(String word, Socket s) {

    }

    public boolean isBlocking() {

        return false;
    }

    public void addEventListener(ConnectionLifecycleListener listener) {

    }

    public void dispatchEvent(ConnectionLifecycleEvent event) {

    }

    public void removeEventListener(ConnectionLifecycleListener listener) {

    }

    public void handleEvent(GnutellaConnectionEvent event) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
