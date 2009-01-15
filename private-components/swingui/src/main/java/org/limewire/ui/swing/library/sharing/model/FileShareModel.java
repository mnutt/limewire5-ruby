package org.limewire.ui.swing.library.sharing.model;

import java.io.File;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.ui.swing.library.sharing.SharingTarget;
import org.limewire.ui.swing.util.CategoryUtils;

public class FileShareModel implements LibraryShareModel {
    private final File file;

    private final LocalFileList gnutellaList;

    private final ShareListManager shareListManager;
    
    public FileShareModel(ShareListManager shareListManager, File file) {
        this.shareListManager = shareListManager;
        this.file = file;
        gnutellaList = shareListManager.getGnutellaShareList();
    }   
    
    @Override
    public void shareFriend(SharingTarget friend) {        
        if (friend.isGnutellaNetwork()) {
            gnutellaList.addFile(file);
        } else {
            shareListManager.getOrCreateFriendShareList(friend.getFriend()).addFile(file);
        }
    }
    
    @Override
    public void unshareFriend(SharingTarget friend) {
        if (friend.isGnutellaNetwork()) {
            gnutellaList.removeFile(file);
        } else {
            //TODO: need to handle share all settings here
            shareListManager.getOrCreateFriendShareList(friend.getFriend()).removeFile(file);
        }
    }

    
    @Override
    public boolean isGnutellaNetworkSharable() {
        return (CategoryUtils.getCategory(file) != Category.DOCUMENT || LibrarySettings.ALLOW_DOCUMENT_GNUTELLA_SHARING.getValue());
    }
 
    @Override
    public boolean isShared(SharingTarget friend) {
        //Handle gnutella 
        if(friend.isGnutellaNetwork()){
            return gnutellaList.contains(file);
        } else {
            return shareListManager.getOrCreateFriendShareList(friend.getFriend()).contains(file);
        }
    }

}
