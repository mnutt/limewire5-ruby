package org.limewire.core.impl.download;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;
import org.limewire.core.impl.URNImpl;
import org.limewire.core.impl.util.FilePropertyKeyPopulator;
import org.limewire.io.Address;
import org.limewire.listener.EventListener;
import org.limewire.listener.SwingSafePropertyChangeSupport;
import org.limewire.util.FileUtils;

import com.limegroup.gnutella.CategoryConverter;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.Downloader.DownloadStatus;
import com.limegroup.gnutella.downloader.DownloadStatusEvent;
import com.limegroup.gnutella.xml.LimeXMLDocument;

class CoreDownloadItem implements DownloadItem {
   
    private final PropertyChangeSupport support = new SwingSafePropertyChangeSupport(this);
    private final Downloader downloader;
    private volatile int hashCode = 0;
    private volatile long cachedSize;
    private volatile boolean cancelled = false;
    
    private Map<FilePropertyKey, Object> propertiesMap;  // TODO this is a shallow copy of LimeXMLDocument.fieldToValue

    /**
     * size in bytes. FINISHING state is only shown for files greater than this
     * size.
     */
    // set to 0 to show FINISHING state regardless of size
    private final long finishingThreshold = 0;

    private final QueueTimeCalculator queueTimeCalculator;

    public CoreDownloadItem(Downloader downloader, QueueTimeCalculator queueTimeCalculator) {
        this.downloader = downloader;
        this.queueTimeCalculator = queueTimeCalculator;
        
        downloader.addListener(new EventListener<DownloadStatusEvent>() {
            @Override
            public void handleEvent(DownloadStatusEvent event) {
                // broadcast the status has changed
                fireDataChanged();
                if (event.getType() == DownloadStatus.ABORTED) {
                    //attempt to delete ABORTED file
                    File file = CoreDownloadItem.this.downloader.getFile();
                    if (file != null) {
                        FileUtils.forceDelete(file);
                    }
                }
            }           
        });
    }

    
    void fireDataChanged() {
        cachedSize = downloader.getAmountRead();
        support.firePropertyChange("state", null, getState());
    }
    
    private Downloader getDownloader(){
        return downloader;
    }
    
    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }
    
    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener){
        support.removePropertyChangeListener(listener);
    }

    @Override
    public void cancel() {
        cancelled = true;
        support.firePropertyChange("state", null, getState());
        File file = downloader.getFile();
        downloader.stop(true);
        //attempt to delete file will not catch some aborted files necessary here for deleting files in error or stalled states
        if (file != null) {
            FileUtils.forceDelete(file);
        }
    }

    @Override
    public Category getCategory() {
        File file = downloader.getFile();
        if(file != null) {
            return CategoryConverter.categoryForFile(file);
        } else {
            // TODO: See if it's OK to always use save file.
            return CategoryConverter.categoryForFile(downloader.getSaveFile());
        }
    }

    @Override
    public long getCurrentSize() {
        if (getState() == DownloadState.DONE) {
            return getTotalSize();
        } else {
            return cachedSize;
        }

    }

    @Override
    public List<Address> getSources() {
        return downloader.getSourcesAsAddresses();
    }

    @Override
    public int getDownloadSourceCount() {
        return downloader.getNumHosts();
    }

    @Override
    public int getPercentComplete() {
        DownloadState state = getState();
        if(state == DownloadState.FINISHING || state == DownloadState.DONE){
            return 100;
        }

        if(getTotalSize() == 0)
            return 0;
        else
            return (int) (100 * getCurrentSize() / getTotalSize());
    }

    @Override
    public long getRemainingDownloadTime() {
        double remaining = (getTotalSize() - getCurrentSize()) / 1024.0;
        float speed = getDownloadSpeed();
        if (speed > 0) {
            return (long) (remaining / speed);
        } else {
            return UNKNOWN_TIME;
        }

    }
    
    @Override
    public float getDownloadSpeed(){
        try {
            return downloader.getMeasuredBandwidth();
        } catch (InsufficientDataException e) {
            return 0;
        }  
    }

    @Override
    public DownloadState getState() {
        if(cancelled){
            return DownloadState.CANCELLED;
        }
        return convertState(downloader.getState());
    }

    @Override
    public String getTitle() {
        // TODO return title, not file name
        return downloader.getSaveFile().getName();
    }

    @Override
    public long getTotalSize() {
        return downloader.getContentLength();
    }

    @Override
    public void pause() {
        downloader.pause();

    }

    @Override
    public void resume() {
        downloader.resume();

    }
    
    @Override
    public int getQueuePosition(){
        return downloader.getQueuePosition();
    }
    
    private DownloadState convertState(DownloadStatus status) {
        // TODO: double check states - some are not right
        switch (status) {
        case SAVING:
        case HASHING:
            if (getTotalSize() > finishingThreshold) {
                return DownloadState.FINISHING;
            } else {
                return DownloadState.DONE;
            }

        case DOWNLOADING:
        case FETCHING://"FETCHING" is downloading .torrent file
            return DownloadState.DOWNLOADING;

        
        case CONNECTING:
        case RESUMING:
        case INITIALIZING:
        case WAITING_FOR_CONNECTIONS:
            return DownloadState.CONNECTING;

        case COMPLETE:
            return DownloadState.DONE;

        case REMOTE_QUEUED:
        case BUSY://BUSY should look like locally queued but acts like remotely
            return DownloadState.REMOTE_QUEUED;
            
        case QUEUED:
            return DownloadState.LOCAL_QUEUED;

        case PAUSED:
            return DownloadState.PAUSED;

        
        case WAITING_FOR_USER:
        case GAVE_UP://"GAVE_UP" means no sources - Is this really an error state?
        case WAITING_FOR_GNET_RESULTS://should WAITING_FOR_GNET_RESULTS be in CONNECTING?
        case ITERATIVE_GUESSING:
        case QUERYING_DHT:
            return DownloadState.STALLED;

        case ABORTED:
            return DownloadState.CANCELLED;

        case DISK_PROBLEM:
        case CORRUPT_FILE:
        case IDENTIFY_CORRUPTION: // or should this be FINISHING?  doesn't seem to be used
        case RECOVERY_FAILED:
        case INVALID:
            return DownloadState.ERROR;
            
              //FIXME remove RuntimeException when we are out of alpha 
        default:
            throw new RuntimeException("Unknown State: " + status);
        }
    }

    public boolean equals(Object o) {
        if (o == null || !(o instanceof CoreDownloadItem)) {
            return false;
        }
        return getDownloader().equals(((CoreDownloadItem) o).getDownloader());
    }
    
    //TODO: better hashCode
    public int hashCode(){
        if(hashCode == 0){
           hashCode =  37* getDownloader().hashCode();
        }
        return hashCode;
    }

    @Override
    public ErrorState getErrorState() {
        switch (downloader.getState()) {
        case CORRUPT_FILE:
        case RECOVERY_FAILED:
            return ErrorState.CORRUPT_FILE;
        case DISK_PROBLEM:
            return ErrorState.DISK_PROBLEM;
        case INVALID:
            return ErrorState.FILE_NOT_SHARABLE;
        case GAVE_UP://"GAVE_UP" means no sources
            return ErrorState.UNABLE_TO_CONNECT;
        default:
            return ErrorState.NONE;
        }
    }

    @Override
    public long getRemainingQueueTime() {
        if(queueTimeCalculator == null){
            return DownloadItem.UNKNOWN_TIME;
        }
        if(downloader.getState() == DownloadStatus.BUSY){
            return downloader.getRemainingStateTime();
        }
        return queueTimeCalculator.getRemainingQueueTime(this);
    }

 
    @Override
    public int getLocalQueuePriority() {
        return downloader.getInactivePriority();
    }

    @Override
    public boolean isLaunchable() {
        return downloader.isLaunchable();
    }

    @Override
    public File getDownloadingFile() {
        return downloader.getFile();
    }
    
    @Override
    public File getLaunchableFile() {
        return downloader.getDownloadFragment();
    }
    
    @Override
    public URN getUrn() {
        com.limegroup.gnutella.URN urn = downloader.getSha1Urn();
        if(urn != null) {
            return new URNImpl(urn);
        }
        return null;
    }

    @Override
    public String getFileName() {
        return downloader.getSaveFile().getName();
    }

    @Override
    public Object getProperty(FilePropertyKey key) {
        return getPropertiesMap().get(key);
    }

    @Override
    public String getPropertyString(FilePropertyKey key) {
        Object value = getProperty(key);
        if (value != null) {
            String stringValue = value.toString();
            return stringValue;
        } else {
            return null;
        }
    }
    
    /**
     * Lazily builds the properties map for this local file item.
     */
    private Map<FilePropertyKey, Object> getPropertiesMap() {
        synchronized (this) {
            if(propertiesMap == null) {
                reloadProperties();
            }
            return propertiesMap;
        }
    }
    
    /**
     * Reloads the properties map to whatever values are stored in the
     * LimeXmlDocs for this file.
     */
    public void reloadProperties() {
        synchronized (this) {
            Map<FilePropertyKey, Object> reloadedMap = Collections
                    .synchronizedMap(new HashMap<FilePropertyKey, Object>());
            //"LimeXMLDocument" attribute is set in ManagedDownloaderImpl
            LimeXMLDocument doc = (LimeXMLDocument)downloader.getAttribute("LimeXMLDocument");
            if (doc != null) {
                FilePropertyKeyPopulator.populateProperties(getFileName(), getTotalSize(), downloader.getFile().lastModified(), reloadedMap, doc);
            }
            propertiesMap = reloadedMap;
        }
    }
}
