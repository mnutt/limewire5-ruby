package org.limewire.core.impl.friend;

import java.util.Collection;
import java.util.Collections;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendManager;
import org.limewire.core.api.friend.FriendPresence;

public class MockFriendManager implements FriendManager {

    @Override
    public boolean containsAvailableFriend(String id) {
        return false;
    }

    @Override
    public FriendPresence getMostRelevantFriendPresence(String id) {
        return null;
    }

    @Override
    public Collection<Friend> getKnownFriends() {
        return Collections.emptyList();
    }
}
