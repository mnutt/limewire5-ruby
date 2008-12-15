package org.limewire.xmpp.api.client;

import com.google.inject.Inject;

public interface XMPPConnectionListener {
    @Inject 
    public void register(XMPPService xmppService);

    public void connected(String connectionId);
}
