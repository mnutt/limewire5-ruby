package org.limewire.core.api.library;

import java.beans.PropertyChangeListener;

import org.limewire.friend.api.Friend;

import ca.odell.glazedlists.EventList;

/**
 * A {@link SearchResultList} specifically for a friend. This is the coalesced
 * version of multiple {@link PresenceLibrary PresenceLibraries}.
 */
public interface FriendLibrary extends SearchResultList {

    /** Returns the friend this library is for. */
    Friend getFriend();

    /**
     * Returns the current state of this friend library. This is a calculated
     * value of all sub-presence libraries. If any sub-library is loading, this
     * returns loading. Otherwise, if one is loaded, this returns loaded.
     * Otherwise, it assumes all have failed and returns failed.
     */
    LibraryState getState();

    /** Returns an EventList of all presence libraries that build up this list. */
    EventList<PresenceLibrary> getPresenceLibraryList();

    void addPropertyChangeListener(PropertyChangeListener listener);

    void removePropertyChangeListener(PropertyChangeListener listener);
}
