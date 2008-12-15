package org.limewire.core.api.lifecycle;

import com.google.inject.AbstractModule;

public class MockLifeCycleModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(LifeCycleManager.class).to(MockLifeCycleManager.class);
    }
}
