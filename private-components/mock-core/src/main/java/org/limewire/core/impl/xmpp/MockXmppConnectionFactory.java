package org.limewire.core.impl.xmpp;

import org.limewire.concurrent.ListeningFuture;
import org.limewire.concurrent.SimpleFuture;
import org.limewire.friend.api.FriendConnection;
import org.limewire.friend.api.FriendConnectionConfiguration;
import org.limewire.friend.api.FriendConnectionFactory;
import org.limewire.friend.api.FriendConnectionFactoryRegistry;
import org.limewire.friend.api.Network;

class MockXmppConnectionFactory implements FriendConnectionFactory {

    @Override
    public ListeningFuture<FriendConnection> login(FriendConnectionConfiguration configuration) {
        return new SimpleFuture<FriendConnection>((FriendConnection)null);
    }

    @Override
    public void register(FriendConnectionFactoryRegistry registry) {
        registry.register(Network.Type.XMPP, this);
    }

    @Override
    public ListeningFuture<String> requestLoginUrl(FriendConnectionConfiguration configuration) {
        return null;
    }
}
