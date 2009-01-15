package org.limewire.ui.swing.library.sharing.model;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.ui.swing.library.sharing.SharingTarget;

public class MultiFileShareModel implements LibraryShareModel {
    private final LocalFileItem[] fileItems;

    private final LocalFileList gnutellaList;

    private final ShareListManager shareListManager;
    

    public MultiFileShareModel(ShareListManager shareListManager, LocalFileItem[] fileItems) {
        this.shareListManager = shareListManager;
        this.fileItems = fileItems;
        gnutellaList = shareListManager.getGnutellaShareList();
    }   
    
    @Override
    public void shareFriend(SharingTarget friend) {
        for (LocalFileItem item : fileItems) {
            if (friend.isGnutellaNetwork()) {
                gnutellaList.addFile(item.getFile());
            } else {
                shareListManager.getOrCreateFriendShareList(friend.getFriend()).addFile(
                        item.getFile());
            }
        }
    }
    
    @Override
    public void unshareFriend(SharingTarget friend) {
        for (LocalFileItem item : fileItems) {
            if (friend.isGnutellaNetwork()) {
                gnutellaList.removeFile(item.getFile());
            } else {
                // TODO: need to handle share all settings here
                shareListManager.getOrCreateFriendShareList(friend.getFriend()).removeFile(
                        item.getFile());
            }
        }
    }

    
    @Override
    public boolean isGnutellaNetworkSharable() {
        
        if (LibrarySettings.ALLOW_DOCUMENT_GNUTELLA_SHARING.getValue()) {            
            // no need to check each file if docs are allowed
            return true;
            
        } else {
            //document sharing not allowed
            for (LocalFileItem item : fileItems) {
                if (item.getCategory() == Category.DOCUMENT){
                    return false;
                }
            }
            
            return true;
        }
    }
 
    @Override
    public boolean isShared(SharingTarget friend) {
        return false;
    }

}
