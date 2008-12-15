package org.limewire.ui.swing.library;

import javax.swing.JComponent;

import org.limewire.core.api.friend.Friend;

/**
 * Creates an empty Panel in place of a Library when a user is not signed on or is not on LW.
 */
interface EmptyLibraryFactory {

    JComponent createEmptyLibrary(Friend friend, FriendLibraryMediator mediator, JComponent messageComponent);
}
