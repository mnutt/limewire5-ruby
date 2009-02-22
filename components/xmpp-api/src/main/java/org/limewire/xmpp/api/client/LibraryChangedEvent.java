package org.limewire.xmpp.api.client;

import org.limewire.core.api.friend.FriendPresence;
import org.limewire.listener.DefaultSourceTypeEvent;

public class LibraryChangedEvent extends DefaultSourceTypeEvent<FriendPresence, LibraryChanged> {

    public LibraryChangedEvent(FriendPresence source, LibraryChanged event) {
        super(source, event);
    }
}
