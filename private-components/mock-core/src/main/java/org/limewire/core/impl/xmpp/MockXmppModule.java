package org.limewire.core.impl.xmpp;

import org.limewire.core.api.xmpp.XMPPResourceFactory;

import com.google.inject.AbstractModule;

public class MockXmppModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(XMPPResourceFactory.class).to(MockXmppResourceFactory.class);
    }

}
