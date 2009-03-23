package org.limewire.ui.swing.library.table;

import java.io.File;

import javax.swing.JScrollPane;

import org.limewire.core.api.Category;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.playlist.Playlist;
import org.limewire.ui.swing.library.LibraryListSourceChanger;
import org.limewire.ui.swing.library.image.LibraryImagePanel;
import org.limewire.ui.swing.library.sharing.ShareWidget;

import ca.odell.glazedlists.EventList;

public interface LibraryTableFactory {
    
    /**
     * Creates a table for MyLibrary
     */
    <T extends LocalFileItem> LibraryTable<T> createMyTable(Category category,
            EventList<T> eventList, LibraryListSourceChanger listChanger);
    
    /**
     * Creates an image list for My Library
     */
    LibraryImagePanel createMyImagePanel(EventList<LocalFileItem> eventList,
            JScrollPane scrollPane, ShareWidget<File> sharePanel,
            LibraryListSourceChanger listChanger);
    
    /**
     * Creates a table for Friends
     */
    <T extends RemoteFileItem> LibraryTable<T> createFriendTable(Category category,
            EventList<T> eventList, Friend friend);

    /**
     * Creates a table for a playlist.
     */
    <T extends LocalFileItem> LibraryTable<T> createPlaylistTable(
            Playlist playlist, EventList<T> eventList);
    
}
