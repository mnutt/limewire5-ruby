package org.limewire.core.impl.friend;

import org.limewire.concurrent.ListeningFuture;
import org.limewire.friend.api.FriendConnection;
import org.limewire.friend.api.FriendConnectionConfiguration;
import org.limewire.friend.api.FriendConnectionFactory;
import org.limewire.friend.api.FriendConnectionFactoryRegistry;

class MockFriendConnectionFactory implements FriendConnectionFactory {

    @Override
    public ListeningFuture<FriendConnection> login(FriendConnectionConfiguration configuration) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void register(FriendConnectionFactoryRegistry registry) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public ListeningFuture<String> requestLoginUrl(FriendConnectionConfiguration configuration) {
        // TODO Auto-generated method stub
        return null;
    }

}
