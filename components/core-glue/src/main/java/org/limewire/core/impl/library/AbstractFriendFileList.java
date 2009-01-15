package org.limewire.core.impl.library;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.FriendFileList;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.listener.SwingSafePropertyChangeSupport;

import ca.odell.glazedlists.EventList;

import com.limegroup.gnutella.library.FileListChangedEvent.Type;

abstract class AbstractFriendFileList extends LocalFileListImpl implements FriendFileList {

    private final PropertyChangeSupport changeSupport = new SwingSafePropertyChangeSupport(this); 
   
    AbstractFriendFileList(EventList<LocalFileItem> eventList, CoreLocalFileItemFactory fileItemFactory) {
        super(eventList, fileItemFactory);
    }

    // upgrade to require FriendFileList
    @Override
    abstract protected com.limegroup.gnutella.library.FriendFileList getCoreFileList();

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }
    
    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }
    
    @Override
    public void clearCategory(Category category) {
        getCoreFileList().clearCategory(category);
    }

    @Override
    public void addSnapshotCategory(Category category) {
        getCoreFileList().addSnapshotCategory(category);
    }
    
    @Override
    public boolean isCategoryAutomaticallyAdded(Category category) {
        switch(category) {
        case AUDIO:
            return getCoreFileList().isAddNewAudioAlways();
        case IMAGE:
            return getCoreFileList().isAddNewImageAlways();
        case VIDEO:
            return getCoreFileList().isAddNewVideoAlways();
        default:
            throw new IllegalArgumentException("invalid category: " + category);
        }
    }
    
    @Override
    public void setCategoryAutomaticallyAdded(Category category, boolean added) {
        switch (category) {
        case AUDIO:
            getCoreFileList().setAddNewAudioAlways(added);
            break;
        case IMAGE:
            getCoreFileList().setAddNewImageAlways(added);
            break;
        case VIDEO:
            getCoreFileList().setAddNewVideoAlways(added);
            break;
        default:
            throw new IllegalArgumentException("invalid category: " + category);
        }
    }
    
    
    @Override
    protected void collectionUpdate(Type type, boolean shared) {
        switch(type) {
        case AUDIO_COLLECTION:
            changeSupport.firePropertyChange("audioCollection", !shared, shared);
            break;
        case IMAGE_COLLECTION:
            changeSupport.firePropertyChange("imageCollection", !shared, shared);
            break;
        case VIDEO_COLLECTION:
            changeSupport.firePropertyChange("videoCollection", !shared, shared);
            break;
        }
    }
    

}
