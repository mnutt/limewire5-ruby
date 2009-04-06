package org.limewire.core.api.browse;

import org.limewire.core.api.friend.FriendPresence;

/**
 * Factory for creating a {@link Browse} object from a {@link FriendPresence}.
 */
public interface BrowseFactory {
    /**
     * Creates a Browse object from the given FriendPresence.
     */
    Browse createBrowse(FriendPresence friendPresence);
}
