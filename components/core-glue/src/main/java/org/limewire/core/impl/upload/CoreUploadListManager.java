package org.limewire.core.impl.upload;

import java.awt.EventQueue;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.upload.UploadItem;
import org.limewire.core.api.upload.UploadListManager;
import org.limewire.core.api.upload.UploadState;
import org.limewire.core.settings.SharingSettings;
import org.limewire.listener.SwingSafePropertyChangeSupport;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.impl.ThreadSafeList;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.UploadServices;
import com.limegroup.gnutella.Uploader;

@Singleton
public class CoreUploadListManager implements UploadListener, UploadListManager{

    private final UploadServices uploadServices;
    private final PropertyChangeSupport changeSupport = new SwingSafePropertyChangeSupport(this);

    private final EventList<UploadItem> uploadItems;

    private EventList<UploadItem> swingThreadUploadItems;
    private ThreadSafeList<UploadItem> threadSafeUploadItems;
    
    private static final int PERIOD = 1000;

    @Inject
    public CoreUploadListManager(UploadListenerList uploadListenerList, @Named("backgroundExecutor")
            ScheduledExecutorService backgroundExecutor,
            UploadServices uploadServices) {

        this.uploadServices = uploadServices;
        
        threadSafeUploadItems = GlazedListsFactory.threadSafeList(new BasicEventList<UploadItem>());
        
        ObservableElementList.Connector<UploadItem> uploadConnector = GlazedLists.beanConnector(UploadItem.class);        
        uploadItems = GlazedListsFactory.observableElementList(threadSafeUploadItems, uploadConnector) ;

        uploadListenerList.addUploadListener(this);
        
        //TODO: change backgroundExecutor to listener - currently no listener for upload progress
        //hack to force tables to update
          Runnable command = new Runnable() {
              @Override
              public void run() {
                  update();
              }
          };
          backgroundExecutor.scheduleAtFixedRate(command, 0, PERIOD, TimeUnit.MILLISECONDS);
    }

    @Override
    public List<UploadItem> getUploadItems() {
        return uploadItems;
    }

    @Override
    public EventList<UploadItem> getSwingThreadSafeUploads() {
        assert EventQueue.isDispatchThread();
        if (swingThreadUploadItems == null) {
            swingThreadUploadItems = GlazedListsFactory.swingThreadProxyEventList(uploadItems);
        }
        return swingThreadUploadItems;
    }
    
    /**
     * Adds the specified listener to the list that is notified when a 
     * property value changes.  Listeners added from the Swing UI thread will
     * always receive notification events on the Swing UI thread. 
     */
    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }

    /**
     * Removes the specified listener from the list that is notified when a 
     * property value changes. 
     */
    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }
    
    /**
     * Checks for uploads in progress, and fires a property change event if
     * all uploads are completed.
     */
    @Override
    public void updateUploadsCompleted() {
        if (uploadServices.getNumUploads() == 0) {
            uploadsCompleted();
        }
    }

    @Override
    public void uploadAdded(Uploader uploader) {
        if (!uploader.getUploadType().isInternal()) {
            UploadItem item = new CoreUploadItem(uploader);
            uploadItems.add(item);
            item.addPropertyChangeListener(new UploadPropertyListener(item));
        }
    }

    @Override
    public void uploadRemoved(Uploader uploader) {
        UploadItem item = new CoreUploadItem(uploader);
        // This is called when uploads complete.  Remove if auto-clear is enabled.
        if ((item.getState() == UploadState.DONE || item.getState() == UploadState.BROWSE_HOST_DONE) && SharingSettings.CLEAR_UPLOAD.getValue()) {
            uploadItems.remove(new CoreUploadItem(uploader));
        }
    }
    
    @Override
    public void uploadsCompleted() {
        changeSupport.firePropertyChange(UPLOADS_COMPLETED, false, true);
    }
    
    // forces refresh
    private void update() {
        uploadItems.getReadWriteLock().writeLock().lock();
        try {
            // TODO use TransactionList for these for performance (requires using GlazedLists from head)
            for (UploadItem item : uploadItems) {
                if (item.getState() != UploadState.DONE && item.getState() != UploadState.BROWSE_HOST_DONE && item instanceof CoreUploadItem)
                    ((CoreUploadItem) item).fireDataChanged();
            }
        } finally {
            uploadItems.getReadWriteLock().writeLock().unlock();
        }
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

    @Override
    public void clearFinished() {
        List<UploadItem> finishedItems = new ArrayList<UploadItem>();
        threadSafeUploadItems.getReadWriteLock().writeLock().lock();
        try {
            for(UploadItem item : threadSafeUploadItems){
                if(item.getState() == UploadState.DONE || item.getState() == UploadState.UNABLE_TO_UPLOAD || item.getState() == UploadState.BROWSE_HOST_DONE){
                    finishedItems.add(item);
                }
            }
            threadSafeUploadItems.removeAll(finishedItems);
        } finally {
            threadSafeUploadItems.getReadWriteLock().writeLock().unlock();
        }
    }

}
