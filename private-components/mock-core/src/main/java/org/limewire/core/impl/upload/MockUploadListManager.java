package org.limewire.core.impl.upload;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.Category;
import org.limewire.core.api.upload.UploadItem;
import org.limewire.core.api.upload.UploadListManager;
import org.limewire.core.api.upload.UploadState;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList.Connector;

public class MockUploadListManager implements UploadListManager {

    private EventList<UploadItem> uploadItems;

    public MockUploadListManager() {
        Connector<UploadItem> uploadConnector = GlazedLists.beanConnector(UploadItem.class);
        uploadItems = GlazedListsFactory.swingThreadProxyEventList(GlazedListsFactory
                .observableElementList(new BasicEventList<UploadItem>(), uploadConnector));
        
        addUpload(UploadState.DONE, "File.mp3", 30000, 15000, Category.AUDIO);
        addUpload(UploadState.QUEUED, "File.avi", 3000, 150, Category.VIDEO);
        addUpload(UploadState.UPLOADING, "File2mp3", 30000, 25544, Category.AUDIO);
        addUpload(UploadState.DONE, "File3.exe", 300, 150, Category.PROGRAM);
        addUpload(UploadState.UNABLE_TO_UPLOAD, "File3.doc", 300, 15, Category.DOCUMENT);
        
    }
    
    private void addUpload(UploadState state, String fileName, long fileSize, long amtUploaded, Category category) {
        UploadItem item = new MockUploadItem(state, fileName, fileSize, amtUploaded, category);
        uploadItems.add(item);
        item.addPropertyChangeListener(new UploadPropertyListener(item));
    }

    @Override
    public EventList<UploadItem> getSwingThreadSafeUploads() {
        return uploadItems;
    }

    @Override
    public List<UploadItem> getUploadItems() {
        return uploadItems;
    }
    
    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
    }
    
    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
    }
    
    @Override
    public void updateUploadsCompleted() {
    }
    
    private class UploadPropertyListener implements PropertyChangeListener {
        private UploadItem item;

        public UploadPropertyListener(UploadItem item){
            this.item = item;
        }
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (item.getState() == UploadState.CANCELED) {
                uploadItems.remove(item);
            }
        }
    }

}
