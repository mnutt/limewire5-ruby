package org.limewire.core.impl.xmpp;

import org.limewire.concurrent.ListeningFuture;
import org.limewire.concurrent.SimpleFuture;
import org.limewire.xmpp.api.client.XMPPPresence.Mode;
import org.limewire.xmpp.api.client.XMPPConnection;
import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;
import org.limewire.xmpp.api.client.XMPPService;

class MockXmppService implements XMPPService {

    @Override
    public ListeningFuture<XMPPConnection> login(XMPPConnectionConfiguration configuration) {
        return new SimpleFuture<XMPPConnection>((XMPPConnection)null);    
    }

    @Override
    public ListeningFuture<Void> logout() {
        return new SimpleFuture<Void>((Void)null);
    }

    @Override
    public boolean isLoggedIn() {
        return false;
    }

    @Override
    public boolean isLoggingIn() {
        return false;
    }

    @Override
    public XMPPConnection getActiveConnection() {
        return null;
    }

    @Override
    public ListeningFuture<Void> setMode(Mode mode) {
        return new SimpleFuture<Void>((Void)null);    
    }
}
