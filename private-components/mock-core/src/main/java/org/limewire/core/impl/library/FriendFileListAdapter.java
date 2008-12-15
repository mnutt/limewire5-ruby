package org.limewire.core.impl.library;

import org.limewire.core.api.library.FriendFileList;
import org.limewire.core.api.library.GnutellaFileList;

public class FriendFileListAdapter extends FileListAdapter implements FriendFileList, GnutellaFileList {

    @Override
    public boolean isAddNewAudioAlways() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isAddNewImageAlways() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isAddNewVideoAlways() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setAddNewAudioAlways(boolean value) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setAddNewImageAlways(boolean value) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setAddNewVideoAlways(boolean value) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void removeDocuments() {
        
    }

}
