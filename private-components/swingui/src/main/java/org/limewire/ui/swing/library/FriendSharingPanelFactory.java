package org.limewire.ui.swing.library;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.FriendFileList;
import org.limewire.core.api.library.LocalFileItem;

import ca.odell.glazedlists.EventList;

/**
 * A factory for creating {@link FriendSharingPanel}s.
 */
interface FriendSharingPanelFactory {

    /**
     * Creates a new {@link FriendSharingPanel}.
     * @param returnToPanel The library panel to go back to. 
     * @param friend The friend this is for.
     * @param wholeLibraryList The list of all items in the library.
     * @param friendFileList The FileList for the particular friend.
     */
    FriendSharingPanel createPanel(LibraryMediator returnToPanel, 
                                   Friend friend,
                                   EventList<LocalFileItem> wholeLibraryList,
                                   FriendFileList friendFileList);
}
