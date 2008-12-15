package org.limewire.core.api.library;

import java.beans.PropertyChangeListener;

public interface FriendFileList extends LocalFileList {

    public boolean isAddNewAudioAlways();
    
    public void setAddNewAudioAlways(boolean value);
    
    public boolean isAddNewVideoAlways();
    
    public void setAddNewVideoAlways(boolean value);
    
    public boolean isAddNewImageAlways();
    
    public void setAddNewImageAlways(boolean value);
    
    public void addPropertyChangeListener(PropertyChangeListener listener);
    
    public void removePropertyChangeListener(PropertyChangeListener listener);
}
