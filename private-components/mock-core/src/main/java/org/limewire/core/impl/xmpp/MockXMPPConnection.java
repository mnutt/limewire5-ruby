package org.limewire.core.impl.xmpp;

import java.util.ArrayList;
import java.util.Collection;

import org.limewire.concurrent.ListeningFuture;
import org.limewire.concurrent.SimpleFuture;
import org.limewire.xmpp.api.client.XMPPPresence.Mode;
import org.limewire.xmpp.api.client.XMPPFriend;
import org.limewire.xmpp.api.client.XMPPConnection;
import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;

public class MockXMPPConnection implements XMPPConnection {
    private XMPPConnectionConfiguration config;
    
    public MockXMPPConnection(XMPPConnectionConfiguration config) {
        this.config = config;
    }

    @Override
    public XMPPConnectionConfiguration getConfiguration() {
        return config;
    }


    @Override
    public boolean isLoggedIn() {
        return true;
    }

    @Override
    public boolean isLoggingIn() {
        return false;
    }

    @Override
    public ListeningFuture<Void> login() {
        return new SimpleFuture<Void>((Void)null);
    }

    @Override
    public ListeningFuture<Void> logout() {
        return new SimpleFuture<Void>((Void)null);
    }

    @Override
    public ListeningFuture<Void> setMode(Mode mode) {
        return new SimpleFuture<Void>((Void)null);
    }

    @Override
    public ListeningFuture<Void> addFriend(String id, String name) {
        return new SimpleFuture<Void>((Void)null);
    }

    @Override
    public ListeningFuture<Void> removeFriend(String id) {
        return new SimpleFuture<Void>((Void)null);
    }

    @Override
    public XMPPFriend getFriend(String id) {
        return null;
    }

    @Override
    public Collection<XMPPFriend> getFriends() {
        return new ArrayList<XMPPFriend>();
    }

}
