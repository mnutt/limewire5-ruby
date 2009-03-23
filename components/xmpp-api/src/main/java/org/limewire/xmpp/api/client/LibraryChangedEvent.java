package org.limewire.xmpp.api.client;

import org.limewire.core.api.friend.FriendPresence;
import org.limewire.listener.DefaultDataTypeEvent;

public class LibraryChangedEvent extends DefaultDataTypeEvent<FriendPresence, LibraryChanged> {

    public LibraryChangedEvent(FriendPresence data, LibraryChanged event) {
        super(data, event);
    }
}
