package org.limewire.core.impl.mojito;

import org.limewire.core.api.mojito.MojitoManager;

import com.google.inject.AbstractModule;

/**
 * Guice module to configure Mojito API for the mock core. 
 */
public class MockMojitoModule extends AbstractModule {

    /**
     * Configures Mojito API for the mock core. 
     */
    @Override
    protected void configure() {
        bind(MojitoManager.class).to(MockMojitoManager.class);
    }
    
}
