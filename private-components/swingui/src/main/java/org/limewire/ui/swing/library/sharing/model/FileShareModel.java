package org.limewire.ui.swing.library.sharing.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.listener.SwingSafePropertyChangeSupport;
import org.limewire.ui.swing.library.sharing.SharingTarget;
import org.limewire.ui.swing.util.CategoryUtils;

public class FileShareModel implements LibraryShareModel {
    private File file;

    private LocalFileList gnutellaList;

    private ShareListManager shareListManager;
    
    private final PropertyChangeSupport support = new SwingSafePropertyChangeSupport(this);

    public FileShareModel(ShareListManager shareListManager) {
        this.shareListManager = shareListManager;
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


    /**
     * @param file  The File whose sharing info will be displayed
     */
    public void setFile(File file){
        File oldFile = this.file;
        this.file = file;
        support.firePropertyChange("file", oldFile, file);
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

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }
    
    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener){
        support.removePropertyChangeListener(listener);
    }
    
}
