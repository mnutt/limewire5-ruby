package org.limewire.core.api.browse;

import org.limewire.core.api.friend.FriendPresence;

public interface BrowseFactory {
    Browse createBrowse(FriendPresence friendPresence);
}
