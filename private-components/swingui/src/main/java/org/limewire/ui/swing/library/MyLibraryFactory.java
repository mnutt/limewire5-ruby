package org.limewire.ui.swing.library;

import org.limewire.collection.glazedlists.PluggableList;
import org.limewire.core.api.library.LocalFileItem;

public interface MyLibraryFactory {

    public MyLibraryPanel createMyLibraryPanel(PluggableList<LocalFileItem> baseLibraryList,
            LibraryListSourceChanger currentFriendFilterChanger);
}
