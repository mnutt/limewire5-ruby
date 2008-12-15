package org.limewire.core.api.magnet;

import com.google.inject.AbstractModule;

public class MockMagnetModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(MagnetFactory.class).to(MockMagnetFactory.class);
    }

}
