package org.limewire.core.impl.daap;

import org.limewire.core.api.daap.DaapManager;

import com.google.inject.AbstractModule;

public class MockDaapModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(DaapManager.class).to(MockDaapManagerImpl.class);
    }
}
