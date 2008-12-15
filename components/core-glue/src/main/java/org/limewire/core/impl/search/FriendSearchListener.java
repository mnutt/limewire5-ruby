package org.limewire.core.impl.search;

import java.util.Collection;

import org.limewire.core.api.library.RemoteFileItem;

public interface FriendSearchListener {
    public void handleFriendResults(Collection<RemoteFileItem> results);
}
