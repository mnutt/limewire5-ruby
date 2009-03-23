package org.limewire.ui.swing.library.sharing.model;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.ui.swing.library.sharing.SharingTarget;
import org.limewire.ui.swing.util.BackgroundExecutorService;

public class MultiFileUnshareModel implements LibraryShareModel {
    private final LocalFileItem[] fileItems;

    private final LocalFileList gnutellaList;

    private final ShareListManager shareListManager;

    public MultiFileUnshareModel(ShareListManager shareListManager, LocalFileItem... fileItems) {
        this.shareListManager = shareListManager;
        this.fileItems = fileItems;
        gnutellaList = shareListManager.getGnutellaShareList();
    }   
    
    @Override
    public void shareFriend(SharingTarget friend) {
        throw new UnsupportedOperationException("Can't share with the MultiFileUnshareModel");
    }
    
    @Override
    public void unshareFriend(final SharingTarget friend) {
        //perform actual unsharing on background thread
        BackgroundExecutorService.execute(new Runnable(){
            public void run() {
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
        });
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
        // returns true if any of the selected files are shared with the friend

        for (LocalFileItem item : fileItems) {
            // Handle gnutella
            if (friend.isGnutellaNetwork()) {
                if (gnutellaList.contains(item.getFile())) {
                    return true;
                }
            } else { // not gnutella
                if (shareListManager.getOrCreateFriendShareList(friend.getFriend()).contains(item.getFile())) {
                    return true;
                }
            }
        }

        return false;
    }
    
}
