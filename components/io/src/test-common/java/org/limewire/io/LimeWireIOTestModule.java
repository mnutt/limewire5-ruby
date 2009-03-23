package org.limewire.io;

public class LimeWireIOTestModule extends LimeWireIOModule{
    @Override
    protected void configure() {
        super.configure();
        bind(LocalSocketAddressProvider.class).to(LocalSocketAddressProviderStub.class);
    }
}
