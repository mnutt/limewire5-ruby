package org.limewire.core.impl.monitor;

import org.limewire.core.api.monitor.IncomingSearchManager;

import com.google.inject.AbstractModule;

/**
 * Guice module to configure the Incoming Search API for the mock core. 
 */
public class MockMonitorModule extends AbstractModule {

    /**
     * Configures the Incoming Search API for the mock core. 
     */
    @Override
    protected void configure() {
        bind(IncomingSearchManager.class).to(MockIncomingSearchManager.class);
    }

}
