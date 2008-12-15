package org.limewire.swarm;

import org.limewire.io.NetworkInstanceUtils;
import org.limewire.io.SimpleNetworkInstanceUtils;
import org.limewire.net.EmptyProxySettings;
import org.limewire.net.EmptySocketBindingSettings;
import org.limewire.net.LimeWireNetModule;
import org.limewire.net.ProxySettings;
import org.limewire.net.SocketBindingSettings;

public class LimeWireSwarmTestModule extends LimeWireNetModule {
    @Override
    protected void configure() {
        super.configure();
        bind(ProxySettings.class).to(EmptyProxySettings.class);
        bind(SocketBindingSettings.class).to(EmptySocketBindingSettings.class);
        bind(NetworkInstanceUtils.class).to(SimpleNetworkInstanceUtils.class);
    }
}
