package org.limewire.core.impl.friend;

import org.limewire.friend.api.FriendManager;
import org.limewire.friend.api.FriendPresence;

public class MockFriendManager implements FriendManager {

    @Override
    public FriendPresence getMostRelevantFriendPresence(String id) {
        return null;
    }
}
