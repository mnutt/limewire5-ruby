package org.limewire.xmpp.api.client;

import org.limewire.core.api.friend.FriendPresence;
import org.limewire.listener.DefaultEvent;

public class LibraryChangedEvent extends DefaultEvent<FriendPresence, LibraryChanged> {

    public LibraryChangedEvent(FriendPresence source, LibraryChanged event) {
        super(source, event);
    }
}
