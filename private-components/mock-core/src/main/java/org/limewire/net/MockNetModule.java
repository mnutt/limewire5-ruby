package org.limewire.net;

import org.limewire.net.address.AddressFactory;
import org.limewire.net.address.MockAddressFactory;

import com.google.inject.AbstractModule;

public class MockNetModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(AddressFactory.class).to(MockAddressFactory.class);
    }
}
