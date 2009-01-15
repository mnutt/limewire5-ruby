package org.limewire.ui.swing.library.sharing.model;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.FriendFileList;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.library.sharing.SharingTarget;

public class CategoryShareModel implements LibraryShareModel{

    private final ShareListManager shareListManager;
    
    private final Category category;    
    
    public CategoryShareModel(ShareListManager shareListManager, Category category){
        this.shareListManager = shareListManager;
        this.category = category;
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

}
