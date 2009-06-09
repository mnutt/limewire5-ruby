package org.limewire.ui.swing.library;

import org.limewire.collection.glazedlists.PluggableList;
import org.limewire.core.api.library.RemoteFileItem;

public interface FriendLibraryFactory {

    public FriendLibraryPanel createFriendLibrary(PluggableList<RemoteFileItem> baseLibraryList,
            FriendLibraryListSourceChanger currentFriendFilterChanger);
}
