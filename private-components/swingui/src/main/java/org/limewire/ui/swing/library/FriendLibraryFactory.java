package org.limewire.ui.swing.library;

import javax.swing.JComponent;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.FriendFileList;
import org.limewire.core.api.library.RemoteFileItem;

import ca.odell.glazedlists.EventList;

interface FriendLibraryFactory {
    
    JComponent createFriendLibrary(Friend friend, FriendFileList friendFileList, EventList<RemoteFileItem> eventList, FriendLibraryMediator mediator);
}
