package org.limewire.core.impl.friend;

import java.util.ArrayList;
import java.util.Collection;

import org.limewire.concurrent.ListeningFuture;
import org.limewire.concurrent.SimpleFuture;
import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendConnection;
import org.limewire.friend.api.FriendConnectionConfiguration;
import org.limewire.friend.api.FriendPresence;

public class MockFriendConnection implements FriendConnection {
    private FriendConnectionConfiguration config;
    
    public MockFriendConnection(FriendConnectionConfiguration config) {
        this.config = config;
    }

    @Override
    public FriendConnectionConfiguration getConfiguration() {
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
    public boolean supportsMode() {
        return true;
    }

    @Override
    public ListeningFuture<Void> setMode(FriendPresence.Mode mode) {
        return new SimpleFuture<Void>((Void)null);
    }

    @Override
    public boolean supportsAddRemoveFriend() {
        return true;
    }

    @Override
    public ListeningFuture<Void> addNewFriend(String id, String name) {
        return new SimpleFuture<Void>((Void)null);
    }

    @Override
    public ListeningFuture<Void> removeFriend(String id) {
        return new SimpleFuture<Void>((Void)null);
    }

    @Override
    public Friend getFriend(String id) {
        return null;
    }

    @Override
    public Collection<Friend> getFriends() {
        return new ArrayList<Friend>();
    }

}
