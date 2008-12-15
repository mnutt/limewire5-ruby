package org.limewire.core.impl.xmpp;

import org.limewire.xmpp.api.client.XMPPConnection;
import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;
import org.limewire.xmpp.api.client.XMPPService;
import org.limewire.xmpp.api.client.Presence.Mode;

class MockXmppService implements XMPPService {

    @Override
    public XMPPConnection login(XMPPConnectionConfiguration configuration) {
        return null;
    }

    @Override
    public void logout() {
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
    public void setMode(Mode mode) {
        
    }
}
