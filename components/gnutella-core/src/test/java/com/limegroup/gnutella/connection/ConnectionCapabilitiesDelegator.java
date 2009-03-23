package com.limegroup.gnutella.connection;

import com.limegroup.gnutella.handshaking.HandshakeResponse;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVM;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;

public class ConnectionCapabilitiesDelegator implements ConnectionCapabilities {
    
    private final ConnectionCapabilities delegate;
    
    public ConnectionCapabilitiesDelegator(ConnectionCapabilities delegate) {
        this.delegate = delegate;
    }

    public int getCapability(Capability tls) {
        return delegate.getCapability(tls);
    }

    public HandshakeResponse getHeadersRead() {
        return delegate.getHeadersRead();
    }

    public HandshakeResponse getHeadersWritten() {
        return delegate.getHeadersWritten();
    }

    public int getNumIntraUltrapeerConnections() {
        return delegate.getNumIntraUltrapeerConnections();
    }

    public int getRemoteHostFeatureQuerySelector() {
        return delegate.getRemoteHostFeatureQuerySelector();
    }

    public boolean getRemoteHostSupportsFeatureQueries() {
        return delegate.getRemoteHostSupportsFeatureQueries();
    }

    public int getRemoteHostUpdateVersion() {
        return delegate.getRemoteHostUpdateVersion();
    }

    public int getSupportedOOBProxyControlVersion() {
        return delegate.getSupportedOOBProxyControlVersion();
    }

    public String getUserAgent() {
        return delegate.getUserAgent();
    }

    public String getVersion() {
        return delegate.getVersion();
    }

    public boolean isCapabilitiesVmSet() {
        return delegate.isCapabilitiesVmSet();
    }

    public boolean isClientSupernodeConnection() {
        return delegate.isClientSupernodeConnection();
    }

    public boolean isGoodLeaf() {
        return delegate.isGoodLeaf();
    }

    public boolean isGoodUltrapeer() {
        return delegate.isGoodUltrapeer();
    }

    public boolean isGUESSUltrapeer() {
        return delegate.isGUESSUltrapeer();
    }

    public boolean isHighDegreeConnection() {
        return delegate.isHighDegreeConnection();
    }

    public boolean isLeafConnection() {
        return delegate.isLeafConnection();
    }

    public boolean isLimeWire() {
        return delegate.isLimeWire();
    }

    public boolean isOldLimeWire() {
        return delegate.isOldLimeWire();
    }

    public boolean isQueryRoutingEnabled() {
        return delegate.isQueryRoutingEnabled();
    }

    public boolean isSupernodeClientConnection() {
        return delegate.isSupernodeClientConnection();
    }

    public boolean isSupernodeConnection() {
        return delegate.isSupernodeConnection();
    }

    public boolean isSupernodeSupernodeConnection() {
        return delegate.isSupernodeSupernodeConnection();
    }

    public boolean isUltrapeerQueryRoutingConnection() {
        return delegate.isUltrapeerQueryRoutingConnection();
    }

    public boolean receivedHeaders() {
        return delegate.receivedHeaders();
    }

    public int remostHostIsActiveDHTNode() {
        return delegate.remostHostIsActiveDHTNode();
    }

    public int remostHostIsPassiveDHTNode() {
        return delegate.remostHostIsPassiveDHTNode();
    }

    public int remoteHostIsPassiveLeafNode() {
        return delegate.remoteHostIsPassiveLeafNode();
    }

    public int remoteHostSupportsHeaderUpdate() {
        return delegate.remoteHostSupportsHeaderUpdate();
    }

    public int remoteHostSupportsHopsFlow() {
        return delegate.remoteHostSupportsHopsFlow();
    }

    public int remoteHostSupportsInspections() {
        return delegate.remoteHostSupportsInspections();
    }

    public int remoteHostSupportsLeafGuidance() {
        return delegate.remoteHostSupportsLeafGuidance();
    }

    public int remoteHostSupportsPushProxy() {
        return delegate.remoteHostSupportsPushProxy();
    }

    public int remoteHostSupportsTCPConnectBack() {
        return delegate.remoteHostSupportsTCPConnectBack();
    }

    public int remoteHostSupportsTCPRedirect() {
        return delegate.remoteHostSupportsTCPRedirect();
    }

    public int remoteHostSupportsUDPConnectBack() {
        return delegate.remoteHostSupportsUDPConnectBack();
    }

    public int remoteHostSupportsUDPCrawling() {
        return delegate.remoteHostSupportsUDPCrawling();
    }

    public int remoteHostSupportsUDPRedirect() {
        return delegate.remoteHostSupportsUDPRedirect();
    }

    public boolean remoteHostSupportsWhatIsNew() {
        return delegate.remoteHostSupportsWhatIsNew();
    }

    public void setCapabilitiesVendorMessage(CapabilitiesVM vm) {
        delegate.setCapabilitiesVendorMessage(vm);
    }

    public void setHeadersRead(HandshakeResponse createResponse) {
        delegate.setHeadersRead(createResponse);
    }

    public void setHeadersWritten(HandshakeResponse writtenHeaders) {
        delegate.setHeadersWritten(writtenHeaders);
    }

    public void setMessagesSupportedVendorMessage(MessagesSupportedVendorMessage vm) {
        delegate.setMessagesSupportedVendorMessage(vm);
    }

    public boolean supportsPongCaching() {
        return delegate.supportsPongCaching();
    }

    public boolean supportsProbeQueries() {
        return delegate.supportsProbeQueries();
    }

    public int supportsVendorMessage(byte[] vendorID, int selector) {
        return delegate.supportsVendorMessage(vendorID, selector);
    }

    public boolean supportsVMRouting() {
        return delegate.supportsVMRouting();
    }

    public boolean canAcceptIncomingTCP() {
        return delegate.canAcceptIncomingTCP();
    }

    public boolean canDoFWT() {
        return delegate.canDoFWT();
    }

}
