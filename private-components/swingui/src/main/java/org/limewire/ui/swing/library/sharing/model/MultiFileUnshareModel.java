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

public class MultiFileUnshareModel implements LibraryShareModel {
    protected LocalFileItem[] fileItems;

    protected LocalFileList gnutellaList;

    protected ShareListManager shareListManager;
    
    protected final PropertyChangeSupport support = new SwingSafePropertyChangeSupport(this);

    public MultiFileUnshareModel(ShareListManager shareListManager) {
        this.shareListManager = shareListManager;
        gnutellaList = shareListManager.getGnutellaShareList();
    }   
    
    @Override
    public void shareFriend(SharingTarget friend) {
        throw new UnsupportedOperationException("Can't share with the MultiFileUnshareModel");
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
    public void setFileItem(LocalFileItem... fileItems){
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
        // returns true if any of the selected files are shared with the friend

        for (LocalFileItem item : fileItems) {
            // Handle gnutella
            if (friend.isGnutellaNetwork()) {
                return gnutellaList.contains(item.getFile());
            }

            // check for share all settings and individual share list
            if (shareListManager.getOrCreateFriendShareList(friend.getFriend()).contains(item.getFile())){
                return true;
            }
        }

        return false;
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
