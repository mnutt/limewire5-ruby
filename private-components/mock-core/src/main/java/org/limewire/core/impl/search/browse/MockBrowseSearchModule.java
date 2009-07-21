package org.limewire.core.impl.search.browse;

import org.limewire.core.api.search.browse.BrowseSearchFactory;

import com.google.inject.AbstractModule;

public class MockBrowseSearchModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(BrowseSearchFactory.class).to(MockBrowseSearchFactory.class);
    }

}
