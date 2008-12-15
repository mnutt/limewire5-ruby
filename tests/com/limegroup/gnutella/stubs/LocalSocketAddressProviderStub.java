package com.limegroup.gnutella.stubs;

import org.limewire.io.LocalSocketAddressProvider;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Singleton;

@Singleton
public class LocalSocketAddressProviderStub implements LocalSocketAddressProvider {

    public static final Module STUB_MODULE = new AbstractModule() {
        @Override
        protected void configure() {
            bind(LocalSocketAddressProvider.class).to(LocalSocketAddressProviderStub.class);
        }
    };
    
    private byte[] localAddress;
    private int localPort;
    private boolean localAddressPrivate;
    private boolean tlsCapable;
    
    public byte[] getLocalAddress() {
        return localAddress;
    }

    public int getLocalPort() {
        return localPort;
    }

    public boolean isLocalAddressPrivate() {
        return localAddressPrivate;
    }

    public void setLocalAddress(byte[] localAddress) {
        this.localAddress = localAddress;
    }

    public void setLocalAddressPrivate(boolean localAddressPrivate) {
        this.localAddressPrivate = localAddressPrivate;
    }

    public void setLocalPort(int localPort) {
        this.localPort = localPort;
    }
    
    public boolean isTLSCapable() {
        return tlsCapable;
    }
    
    public LocalSocketAddressProviderStub setTLSCapable(boolean tlsCapable) {
        this.tlsCapable = tlsCapable;
        return this;
    }
    
}