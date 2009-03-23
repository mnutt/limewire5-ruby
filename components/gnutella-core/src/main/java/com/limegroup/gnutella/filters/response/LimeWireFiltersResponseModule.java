package com.limegroup.gnutella.filters.response;

import com.google.inject.AbstractModule;

public class LimeWireFiltersResponseModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(ResponseFilterFactory.class).to(ResponseFilterFactoryImpl.class);
    }
}
