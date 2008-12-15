package com.limegroup.gnutella;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import com.limegroup.gnutella.connection.ConnectionBandwidthStatistics;
import com.limegroup.gnutella.connection.ConnectionCapabilities;
import com.limegroup.gnutella.connection.ConnectionMessageStatistics;
import com.limegroup.gnutella.connection.ConnectionRoutingStatistics;
import com.limegroup.gnutella.connection.GnetConnectObserver;
import com.limegroup.gnutella.connection.RoutedConnection;
import com.limegroup.gnutella.filters.SpamFilter;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.vendor.SimppVM;
import com.limegroup.gnutella.messages.vendor.VendorMessage;
import com.limegroup.gnutella.routing.PatchTableMessage;
import com.limegroup.gnutella.routing.ResetTableMessage;

/**
 * A stubbed-out ManagedConnection that does nothing. Useful for testing, since
 * ManagedConnection has no public-access constructors. ManagedConnectionStub is
 * in this package instead of com.limegroup.gnutella.stubs because it requires
 * package-access to ManagedConnection.
 */
public class RoutedConnectionStub implements RoutedConnection {

    public ConnectionMessageStatistics getConnectionMessageStatistics() {
        // TODO Auto-generated method stub
        return null;
    }

    public float getMeasuredDownstreamBandwidth() {
        // TODO Auto-generated method stub
        return 0;
    }

    public float getMeasuredUpstreamBandwidth() {
        // TODO Auto-generated method stub
        return 0;
    }

    public Object getQRPLock() {
        // TODO Auto-generated method stub
        return null;
    }

    public ConnectionRoutingStatistics getRoutedConnectionStatistics() {
        // TODO Auto-generated method stub
        return null;
    }

    public void initialize(GnetConnectObserver observer) throws IOException {
        // TODO Auto-generated method stub
        
    }

    public boolean isBusyLeaf() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isMyPushProxy() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isPushProxyFor() {
        // TODO Auto-generated method stub
        return false;
    }

    public void measureBandwidth() {
        // TODO Auto-generated method stub
        
    }

    public void originateQuery(QueryRequest query) {
        // TODO Auto-generated method stub
        
    }

    public void patchQueryRouteTable(PatchTableMessage ptm) {
        // TODO Auto-generated method stub
        
    }

    public void resetQueryRouteTable(ResetTableMessage rtm) {
        // TODO Auto-generated method stub
        
    }

    public void send(Message m) {
        // TODO Auto-generated method stub
        
    }

    public void setLocalePreferencing(boolean b) {
        // TODO Auto-generated method stub
        
    }

    public void setPersonalFilter(SpamFilter filter) {
        // TODO Auto-generated method stub
        
    }

    public void setPushProxyFor(boolean pushProxyFor) {
        // TODO Auto-generated method stub
        
    }

    public void setRouteFilter(SpamFilter filter) {
        // TODO Auto-generated method stub
        
    }

    public boolean shouldForwardQuery(QueryRequest query) {
        // TODO Auto-generated method stub
        return false;
    }

    public void startMessaging() {
        // TODO Auto-generated method stub
        
    }

    public boolean allowNewPings() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean allowNewPongs() {
        // TODO Auto-generated method stub
        return false;
    }

    public void close() {
        // TODO Auto-generated method stub
        
    }

    public String getAddress() {
        // TODO Auto-generated method stub
        return null;
    }

    public ConnectionBandwidthStatistics getConnectionBandwidthStatistics() {
        // TODO Auto-generated method stub
        return null;
    }

    public ConnectionCapabilities getConnectionCapabilities() {
        // TODO Auto-generated method stub
        return null;
    }

    public long getConnectionTime() {
        // TODO Auto-generated method stub
        return 0;
    }

    public InetAddress getInetAddress() throws IllegalStateException {
        // TODO Auto-generated method stub
        return null;
    }

    public InetSocketAddress getInetSocketAddress() throws IllegalStateException {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public String getAddressDescription() {
        return null;
    }

    public int getListeningPort() {
        // TODO Auto-generated method stub
        return 0;
    }

    public String getLocalePref() {
        // TODO Auto-generated method stub
        return null;
    }

    public int getPort() {
        // TODO Auto-generated method stub
        return 0;
    }

    public String getPropertyWritten(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    public Socket getSocket() throws IllegalStateException {
        // TODO Auto-generated method stub
        return null;
    }

    public void handleSimppVM(SimppVM simppVM) throws IOException {
        // TODO Auto-generated method stub
        
    }

    public void handleVendorMessage(VendorMessage vm) {
        // TODO Auto-generated method stub
        
    }

    public boolean isOpen() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isOutgoing() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isReadDeflated() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isStable() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isStable(long millis) {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isTLSCapable() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isTLSEncoded() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isWriteDeflated() {
        // TODO Auto-generated method stub
        return false;
    }

    public void sendPostInitializeMessages() {
        // TODO Auto-generated method stub
        
    }

    public void sendUpdatedCapabilities() {
        // TODO Auto-generated method stub
        
    }

    public void setListeningPort(int port) {
        // TODO Auto-generated method stub
        
    }

    public void countDroppedMessage() {
        // TODO Auto-generated method stub
        
    }

    public byte[] getClientGUID() {
        // TODO Auto-generated method stub
        return null;
    }

    public int getNumMessagesReceived() {
        // TODO Auto-generated method stub
        return 0;
    }

    public void handlePingReply(PingReply pingReply, ReplyHandler handler) {
        // TODO Auto-generated method stub
        
    }

    public void handlePushRequest(PushRequest pushRequest, ReplyHandler handler) {
        // TODO Auto-generated method stub
        
    }

    public void handleQueryReply(QueryReply queryReply, ReplyHandler handler) {
        // TODO Auto-generated method stub
        
    }

    public boolean isGoodLeaf() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isGoodUltrapeer() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isHighDegreeConnection() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isKillable() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isLeafConnection() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isPersonalSpam(Message m) {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isSupernodeClientConnection() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isUltrapeerQueryRoutingConnection() {
        // TODO Auto-generated method stub
        return false;
    }

    public void reply(Message m) {
        // TODO Auto-generated method stub
        
    }

    public boolean supportsPongCaching() {
        // TODO Auto-generated method stub
        return false;
    }
  
}
