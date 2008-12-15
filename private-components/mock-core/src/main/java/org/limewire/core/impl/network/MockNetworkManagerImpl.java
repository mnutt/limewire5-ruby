package org.limewire.core.impl.network;

import java.io.IOException;

import org.limewire.core.api.network.NetworkManager;

public class MockNetworkManagerImpl implements NetworkManager {

    @Override
    public boolean isIncomingTLSEnabled() {
        return false;
    }

    @Override
    public boolean isOutgoingTLSEnabled() {
        return false;
    }

    @Override
    public void setIncomingTLSEnabled(boolean value) {
    }

    @Override
    public void setOutgoingTLSEnabled(boolean value) {
    }

    @Override
    public void portChanged() {
    }

    @Override
    public void setListeningPort(int port) throws IOException {
    }

    @Override
    public boolean addressChanged() {
        return false;
    }
}
