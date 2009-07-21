package org.limewire.core.api.library;

import java.beans.PropertyChangeListener;

import org.limewire.friend.api.FriendPresence;

/**
 * A library specific to a presence of a friend. Multiple
 * {@link PresenceLibrary PresenceLibraries} are coalesced into a single
 * {@link FriendLibrary}.
 */
public interface PresenceLibrary extends SearchResultList {
    /** The {@link FriendPresence} associated with this library. */
    FriendPresence getPresence();

    /** Returns the current state of this presence library. */
    LibraryState getState();

    /** Sets the current state. */
    void setState(LibraryState newState);

    void addPropertyChangeListener(PropertyChangeListener listener);

    void removePropertyChangeListener(PropertyChangeListener listener);
}
