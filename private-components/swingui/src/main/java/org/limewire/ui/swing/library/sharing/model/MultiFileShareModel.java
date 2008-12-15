package org.limewire.ui.swing.library.sharing.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.listener.SwingSafePropertyChangeSupport;
import org.limewire.ui.swing.library.sharing.SharingTarget;

public class MultiFileShareModel implements LibraryShareModel {
    private LocalFileItem[] fileItems;

    private LocalFileList gnutellaList;

    private ShareListManager shareListManager;
    
    private final PropertyChangeSupport support = new SwingSafePropertyChangeSupport(this);

    public MultiFileShareModel(ShareListManager shareListManager) {
        this.shareListManager = shareListManager;
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

    /**
     * @param fileItem  The LocalFileItem whose sharing info will be displayed
     */
    public void setFileItems(LocalFileItem... fileItems){
        LocalFileItem[] oldFileItems = this.fileItems;
        this.fileItems = fileItems;
        support.firePropertyChange("fileItems", oldFileItems, fileItems);
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
        //always false.  
        return false;
        
//        //returned true if all files are shared with friend        
//        for (LocalFileItem item : fileItems) {
//            // Handle gnutella
//            if (friend.isGnutellaNetwork()) {
//                if (!gnutellaList.contains(item.getFile())){
//                    return false;
//                }
//            } else {
//
//                // check for share all settings
//                switch (item.getCategory()) {
//                case AUDIO:
//                    if (shareListManager.getOrCreateFriendShareList(friend.getFriend())
//                            .isAddNewAudioAlways()) {
//                        return true;
//                    }
//                    break;
//                case VIDEO:
//                    if (shareListManager.getOrCreateFriendShareList(friend.getFriend())
//                            .isAddNewVideoAlways()) {
//                        return true;
//                    }
//                    break;
//                case IMAGE:
//                    if (shareListManager.getOrCreateFriendShareList(friend.getFriend())
//                            .isAddNewImageAlways()) {
//                        return true;
//                    }
//                    break;
//                }
//
//                // check individual file share settings
//                if (!shareListManager.getOrCreateFriendShareList(friend.getFriend()).contains(
//                        item.getFile())) {
//                    return false;
//                }
//            }
//        }
//        
//        return true;
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
