package org.limewire.core.impl.connection;

import org.limewire.core.api.connection.GnutellaConnectionManager;

import com.google.inject.AbstractModule;

/**
 * Guice module to configure the Connections API for the mock core. 
 */
public class MockConnectionModule extends AbstractModule {

    /**
     * Configures the Connections API for the mock core. 
     */
    @Override
    protected void configure() {
        bind(GnutellaConnectionManager.class).to(MockConnectionManagerImpl.class);
    }

}
