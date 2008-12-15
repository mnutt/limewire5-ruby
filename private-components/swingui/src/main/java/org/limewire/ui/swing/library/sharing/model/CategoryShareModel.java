package org.limewire.ui.swing.library.sharing.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.listener.SwingSafePropertyChangeSupport;
import org.limewire.ui.swing.library.sharing.SharingTarget;

public class CategoryShareModel implements LibraryShareModel{

    private Category category;
    private ShareListManager shareListManager;
    
    private final PropertyChangeSupport support = new SwingSafePropertyChangeSupport(this);
    
    public CategoryShareModel(ShareListManager shareListManager){
        this.shareListManager = shareListManager;
    }

    public void setCategory(Category category) {
        Category oldCategory = this.category;
        this.category = category;
        support.firePropertyChange("category", oldCategory, category);
    }
 
    @Override
    public void shareFriend(SharingTarget friend) {

        switch (category) {
        case AUDIO:
            if(isGnutella(friend.getFriend().getId()))
                shareListManager.getGnutellaShareList().setAddNewAudioAlways(true);
            else
                shareListManager.getOrCreateFriendShareList(friend.getFriend()).setAddNewAudioAlways(true);
            break;
        case VIDEO:
            if(isGnutella(friend.getFriend().getId()))
                shareListManager.getGnutellaShareList().setAddNewVideoAlways(true);
            else
                shareListManager.getOrCreateFriendShareList(friend.getFriend()).setAddNewVideoAlways(true);
            break;
        case IMAGE:
            if(isGnutella(friend.getFriend().getId()))
                shareListManager.getGnutellaShareList().setAddNewImageAlways(true);
            else
                shareListManager.getOrCreateFriendShareList(friend.getFriend()).setAddNewImageAlways(true);
            break;
        default:
            throw new IllegalStateException("Can not share " + category + " collection");
        }
    }

    @Override
    public void unshareFriend(SharingTarget friend) {

        switch (category) {
        case AUDIO:
            if(isGnutella(friend.getFriend().getId()))
                shareListManager.getGnutellaShareList().setAddNewAudioAlways(false);
            else
                shareListManager.getOrCreateFriendShareList(friend.getFriend()).setAddNewAudioAlways(false);
            break;
        case VIDEO:
            if(isGnutella(friend.getFriend().getId()))
                shareListManager.getGnutellaShareList().setAddNewVideoAlways(false);
            else
                shareListManager.getOrCreateFriendShareList(friend.getFriend()).setAddNewVideoAlways(false);
            break;
        case IMAGE:
            if(isGnutella(friend.getFriend().getId()))
                shareListManager.getGnutellaShareList().setAddNewImageAlways(false);
            else
                shareListManager.getOrCreateFriendShareList(friend.getFriend()).setAddNewImageAlways(false);
            break;
        default:
            throw new IllegalStateException("Can not share " + category + " collection");
        }
    }

    public boolean isShared(SharingTarget friend) {
        switch (category) {
        case AUDIO:
            if(isGnutella(friend.getFriend().getId()))
                return shareListManager.getGnutellaShareList().isAddNewAudioAlways();
            else
                return shareListManager.getOrCreateFriendShareList(friend.getFriend()).isAddNewAudioAlways();
        case VIDEO:
            if(isGnutella(friend.getFriend().getId()))
                return shareListManager.getGnutellaShareList().isAddNewVideoAlways();
            else
                return shareListManager.getOrCreateFriendShareList(friend.getFriend()).isAddNewVideoAlways();
        case IMAGE:
            if(isGnutella(friend.getFriend().getId()))
                return shareListManager.getGnutellaShareList().isAddNewImageAlways(); 
            else
                return shareListManager.getOrCreateFriendShareList(friend.getFriend()).isAddNewImageAlways();
        default:
            throw new IllegalStateException("Can not share " + category + " collection");
        }
    }

    private boolean isGnutella(String id) {
        return id.equals(SharingTarget.GNUTELLA_SHARE.getFriend().getId());
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
