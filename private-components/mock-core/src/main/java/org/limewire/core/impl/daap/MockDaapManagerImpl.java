package org.limewire.core.impl.daap;

import java.io.IOException;

import org.limewire.core.api.daap.DaapManager;

public class MockDaapManagerImpl implements DaapManager {

    @Override
    public void disconnectAll() {
    }

    @Override
    public void restart() throws IOException {
    }

    @Override
    public void stop() {
    }

    @Override
    public void updateService() throws IOException {
    }

    @Override
    public boolean isServerRunning() {
        return false;
    }
}
