package org.limewire.core.impl.support;

import org.limewire.core.api.support.LocalClientInfo;
import org.limewire.core.api.support.LocalClientInfoFactory;
import org.limewire.core.api.support.SessionInfo;

import com.google.inject.AbstractModule;

/**
 * Guice module to configure the Support API for the mock core. 
 */
public class MockSupportModule extends AbstractModule {

    /**
     * Configures Support API for the mock core. 
     */
    @Override
    protected void configure() {
        bind(SessionInfo.class).to(MockSessionInfo.class);
        bind(LocalClientInfo.class).to(MockLocalClientInfo.class);
        bind(LocalClientInfoFactory.class).to(MockLocalClientInfoFactory.class);
    }

}
