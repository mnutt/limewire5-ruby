package org.limewire.net;

import org.limewire.io.NetworkInstanceUtils;
import org.limewire.io.SimpleNetworkInstanceUtils;

public class LimeWireNetTestModule extends LimeWireNetModule {
    @Override
    protected void configure() {
        super.configure();
        bind(ProxySettings.class).to(EmptyProxySettings.class);
        bind(SocketBindingSettings.class).to(EmptySocketBindingSettings.class);
        bind(NetworkInstanceUtils.class).to(SimpleNetworkInstanceUtils.class);
    }
}
