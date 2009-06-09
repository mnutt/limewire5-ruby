package org.limewire.core.impl.friend;

import org.limewire.core.api.friend.FriendManager;

import com.google.inject.AbstractModule;

public class MockFriendModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(FriendManager.class).to(MockFriendManager.class);
    }
}
