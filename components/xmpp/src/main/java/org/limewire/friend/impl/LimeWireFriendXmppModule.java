package org.limewire.friend.impl;

import java.util.Collection;
import java.util.Set;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendManager;
import org.limewire.core.api.friend.impl.LimeWireFriendModule;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Named;

// TODO fberger: remove this module
public class LimeWireFriendXmppModule extends AbstractModule {
    
    @Override
    protected void configure() {
        install(new LimeWireFriendModule());
        bind(FriendListListeners.class);
        bind(FriendManager.class).to(FriendListListeners.class);
    }

    // TODO fberger: move these out of the XMPP specific code by untying from FriendListListeners 
    @Provides @Named("known") Collection<Friend> knownFriendsList(FriendListListeners listeners) {
        return listeners.getKnownFriends();
    }
    
    @Provides @Named("available") Collection<Friend> availableFriendsList(FriendListListeners listeners) {
        return listeners.getAvailableFriends();
    }
    
    @Provides @Named("availableFriendIds") Set<String> availableFriendIds(FriendListListeners listeners) {
        return listeners.getAvailableFriendIds();
    }

}
