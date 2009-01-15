package org.limewire.ui.swing.library.nav;

import java.io.File;

import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.Category;
import org.limewire.core.api.URN;
import org.limewire.core.api.friend.Friend;

/** Controls navigation of you & your friends libraries. */
public interface LibraryNavigator {
    
    /** Returns the renderable component. */
    JXPanel getComponent();
    
    /** Selects a friend's sharelist. */
    void selectFriendShareList(Friend friend);
    
    /** Selects a friend's library. */
    void selectFriendLibrary(Friend friend);
    
    /** Selects a specific item in your library. */
    void selectInLibrary(URN urn, Category category);
    
    /** Selects a specific item in your library. */
    void selectInLibrary(File file, Category category);
    
    /** Selects the library without any specific file being selected. */
    void selectLibrary();
    
    /** Returns the file item before the one passed in your library. */
    File getPreviousInLibrary(File file, Category category);
    
    /** Returns the file item after the one passed in your library. */
    File getNextInLibrary(File file, Category category);
    
    /**
     * Returns the selected friend in the library. 
     * Will return null if no friend is selected, 
     * or the friend is not a jabber user. 
     */
    Friend getSelectedFriend();

}
