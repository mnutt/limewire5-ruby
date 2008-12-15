package org.limewire.core.impl.browse;

import org.limewire.core.api.browse.BrowseFactory;

import com.google.inject.AbstractModule;


public class MockBrowseModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(BrowseFactory.class).to(MockBrowseFactory.class);
    }

}
