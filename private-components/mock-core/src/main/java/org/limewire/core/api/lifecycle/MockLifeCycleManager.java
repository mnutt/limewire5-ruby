package org.limewire.core.api.lifecycle;

import org.limewire.listener.EventListener;

public class MockLifeCycleManager implements LifeCycleManager {

    @Override
    public void addListener(EventListener<LifeCycleEvent> listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isLoaded() {
        return true;
    }

    @Override
    public boolean isShutdown() {
        return false;
    }

    @Override
    public boolean isStarted() {
        return true;
    }

    @Override
    public boolean removeListener(EventListener<LifeCycleEvent> listener) {
        // TODO Auto-generated method stub
        return false;
    }

}
