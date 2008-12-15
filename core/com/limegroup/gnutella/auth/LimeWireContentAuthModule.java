package com.limegroup.gnutella.auth;

import com.google.inject.AbstractModule;

public class LimeWireContentAuthModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(UrnValidator.class).to(UrnValidatorImpl.class);
    }

}
