package com.limegroup.gnutella.simpp;

import com.google.inject.AbstractModule;

public class LimeWireSimppModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(SimppManager.class).to(SimppManagerImpl.class);
        bind(SimppDataProvider.class).to(SimppDataProviderImpl.class);
    }
    
}
