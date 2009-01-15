package org.limewire.ui.swing.library.table;

import java.io.File;

import javax.swing.JScrollPane;

import org.limewire.core.api.Category;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.library.image.LibraryImagePanel;
import org.limewire.ui.swing.library.sharing.ShareWidget;

import ca.odell.glazedlists.EventList;

public interface LibraryTableFactory {
    
    /**
     * Creates a table for MyLibrary
     */
    <T extends LocalFileItem> LibraryTable<T> createMyTable(Category category, EventList<T> eventList);
    
    /**
     * Creates a table for Friends
     */
    <T extends RemoteFileItem> LibraryTable<T> createFriendTable(Category category, EventList<T> eventList, Friend friend);
    
    /**
     * Creates an image list for MyLibrary/Friends
     */
    LibraryImagePanel createImagePanel(EventList<LocalFileItem> eventList, JScrollPane scrollPane, ShareWidget<File> sharePanel);
    
    /**
     * Creates a table for sharing files with a specified Friend
     */
    <T extends LocalFileItem> LibraryTable<T> createSharingTable(Category category, EventList<T> eventList, LocalFileList friendFileList, Friend friend);

    /**
     * Creates an image list for sharing files with a specified Friend
     */
    LibraryImagePanel createSharingImagePanel(EventList<LocalFileItem> eventList, JScrollPane scrollPane, LocalFileList friendFileList);
}
