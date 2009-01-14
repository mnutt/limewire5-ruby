package org.limewire.ui.swing.library.sharing.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.FriendFileList;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.listener.SwingSafePropertyChangeSupport;
import org.limewire.ui.swing.library.sharing.SharingTarget;

public class CategoryShareModel implements LibraryShareModel{

    private final ShareListManager shareListManager;
    private final PropertyChangeSupport support = new SwingSafePropertyChangeSupport(this);
    
    private Category category;    
    
    public CategoryShareModel(ShareListManager shareListManager){
        this.shareListManager = shareListManager;
    }

    public void setCategory(Category category) {
        Category oldCategory = this.category;
        this.category = category;
        support.firePropertyChange("category", oldCategory, category);
    }
        
    private FriendFileList getFileListForFriend(SharingTarget friend) {
        if(friend.isGnutellaNetwork()) {
            return shareListManager.getGnutellaShareList();
        } else {
            return shareListManager.getOrCreateFriendShareList(friend.getFriend());
        }
    }
 
    @Override
    public void shareFriend(SharingTarget friend) {
        getFileListForFriend(friend).setCategoryAutomaticallyAdded(category, true);
    }

    @Override
    public void unshareFriend(SharingTarget friend) {
        getFileListForFriend(friend).setCategoryAutomaticallyAdded(category, false);
        getFileListForFriend(friend).clearCategory(category);
    }

    public boolean isShared(SharingTarget friend) {
        return getFileListForFriend(friend).isCategoryAutomaticallyAdded(category);
    }

    @Override
    public boolean isGnutellaNetworkSharable() {
        return true;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }
    
    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener){
        support.removePropertyChangeListener(listener);
    }
}
