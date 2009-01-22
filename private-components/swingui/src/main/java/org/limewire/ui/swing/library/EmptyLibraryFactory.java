package org.limewire.ui.swing.library;

import javax.swing.JComponent;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.FriendFileList;
import org.limewire.ui.swing.components.Disposable;

/**
 * Creates an empty Panel in place of a Library when a user is not signed on or is not on LW.
 */
interface EmptyLibraryFactory {

    JComponent createEmptyLibrary(Friend friend, FriendFileList friendFileList, FriendLibraryMediator mediator, Disposable disposable, JComponent messageComponent);
}
