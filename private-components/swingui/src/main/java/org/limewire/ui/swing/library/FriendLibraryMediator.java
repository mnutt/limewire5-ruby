package org.limewire.ui.swing.library;

import org.limewire.collection.glazedlists.PluggableList;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.ui.swing.library.nav.NavMediator;

import com.google.inject.Inject;

public class FriendLibraryMediator implements NavMediator<FriendLibraryPanel> {

    private final FriendLibraryFactory friendLibraryFactory;
    private FriendLibraryPanel panel;
    
    final PluggableList<RemoteFileItem> baseLibraryList;
    final FriendLibraryListSourceChanger currentFriendFilterChanger;
    
    /** Friend that is currently selected, null if all files are being shown*/
    private Friend currentFriend;
    
    @Inject
    public FriendLibraryMediator(FriendLibraryFactory friendLibraryFactory, RemoteLibraryManager remoteLibraryManager) {
        this.friendLibraryFactory = friendLibraryFactory;
        
        baseLibraryList = new PluggableList<RemoteFileItem>(remoteLibraryManager.getAllFriendsFileList().getModel().getPublisher(), remoteLibraryManager.getAllFriendsFileList().getModel().getReadWriteLock());
        currentFriendFilterChanger = new FriendLibraryListSourceChanger(baseLibraryList, remoteLibraryManager);
    }
    
    public Friend getSelectedFriend() {
        if(panel == null)
            return currentFriend;
        else
            return panel.getSelectedFriend();
    }
    
    public void setFriend(Friend friend) {
        if(panel != null)
            panel.setFriend(friend);
        else
            currentFriend = friend;
    }
    
    @Override
    public FriendLibraryPanel getComponent() {
        if(panel == null)
            panel = friendLibraryFactory.createFriendLibrary(baseLibraryList, currentFriendFilterChanger);
        return panel;
    }
    
    /**
     * Adds a listener that is notified when the friend in the current view has changed.
     */
    public void addFriendListener(ListSourceChanger.ListChangedListener listener) {
        currentFriendFilterChanger.addListener(listener);
    }
}
