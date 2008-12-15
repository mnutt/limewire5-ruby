package org.limewire.core.impl.spam;

import org.limewire.core.api.spam.SpamManager;

import com.google.inject.AbstractModule;

public class MockSpamModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(SpamManager.class).to(MockSpamManagerImpl.class);
    }
}