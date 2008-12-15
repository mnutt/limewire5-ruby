package org.limewire.core.impl.library;

import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.RemoteFileList;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;

public class RemoteFileListAdapter implements RemoteFileList {

    @Override
    public void addFile(RemoteFileItem file) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void removeFile(RemoteFileItem file) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public EventList<RemoteFileItem> getModel() {
        return new BasicEventList<RemoteFileItem>();
    }

    @Override
    public EventList<RemoteFileItem> getSwingModel() {
        return new BasicEventList<RemoteFileItem>();
    }

    @Override
    public int size() {
        // TODO Auto-generated method stub
        return 0;
    }

}
