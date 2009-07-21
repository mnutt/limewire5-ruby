package org.limewire.core.impl.upload;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.upload.UploadErrorState;
import org.limewire.core.api.upload.UploadItem;
import org.limewire.core.api.upload.UploadState;
import org.limewire.core.impl.util.FilePropertyKeyPopulator;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.feature.LimewireFeature;
import org.limewire.listener.SwingSafePropertyChangeSupport;
import org.limewire.util.FileUtils;

import com.limegroup.bittorrent.BTUploader;
import com.limegroup.gnutella.CategoryConverter;
import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.Uploader;
import com.limegroup.gnutella.Uploader.UploadStatus;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.uploader.UploadType;

class CoreUploadItem implements UploadItem {

    private final Uploader uploader;    
    private final FriendPresence friendPresence;
    private final PropertyChangeSupport support = new SwingSafePropertyChangeSupport(this);
    
    public final static long UNKNOWN_TIME = Long.MAX_VALUE;
    private final UploadItemType uploadItemType;
    private boolean isFinished = false;
    private UploadRemoteHost uploadRemoteHost;
    
    public CoreUploadItem(Uploader uploader, FriendPresence friendPresence) {
        this.uploader = uploader;
        this.friendPresence = friendPresence;
        uploadItemType = uploader instanceof BTUploader ? UploadItemType.BITTORRENT : UploadItemType.GNUTELLA;
    }

    @Override
    public void cancel() {
        uploader.stop();
        fireDataChanged();
    }

    @Override
    public String getFileName() {
        return uploader.getFileName();
    }

    @Override
    public long getFileSize() {
        return uploader.getFileSize();
    }

    @Override
    public UploadState getState() {
        switch (getUploaderStatus()) {
        case CANCELLED:
            return UploadState.CANCELED;
        case COMPLETE:
            if(uploader.getUploadType() == UploadType.BROWSE_HOST){
                return UploadState.BROWSE_HOST_DONE;
            }
            return UploadState.DONE;

        case CONNECTING:
        case UPLOADING:
        case THEX_REQUEST:
        case PUSH_PROXY:
        case UPDATE_FILE:
            return UploadState.UPLOADING;

        case QUEUED:
            return UploadState.QUEUED;

        case WAITING_REQUESTS:
            return UploadState.WAITING;
            
        case LIMIT_REACHED:
        case INTERRUPTED:
        case FILE_NOT_FOUND:
        case UNAVAILABLE_RANGE:
        case MALFORMED_REQUEST:
        case SUSPENDED:
        case BANNED_GREEDY:
        case FREELOADER:
            return UploadState.UNABLE_TO_UPLOAD;
            
        case BROWSE_HOST:
            return UploadState.BROWSE_HOST;

        }

        throw new IllegalStateException("Unknown Upload status : " + uploader.getState());
    }
    
    private UploadStatus getUploaderStatus() {
        // do not change the status if we are at an intermediary
        // complete or connecting state.
        // (meaning that this particular chunk finished, but more will come)
        // we use isFinished to tell us when it's finished, because that is
        // set when remove is called, which is only called when the entire
        // upload has finished.
        // we use getTotalAmountUploaded to know if a byte has been read
        // (which would mean we're not connecting anymore)
        UploadStatus state = uploader.getState();
        UploadStatus lastState = uploader.getLastTransferState();
        if ( (state == UploadStatus.COMPLETE && !isFinished) ||
             (state == UploadStatus.CONNECTING &&
                 uploader.getTotalAmountUploaded() != 0)
           ) {
            state = lastState;
        }
        
        // Reset the current state to be the lastState if we're complete now,
        // but our last transfer wasn't uploading, queued, or thex.
        if(uploader.getUploadType() != UploadType.BROWSE_HOST &&
          state == UploadStatus.COMPLETE && 
          lastState != UploadStatus.UPLOADING &&
          lastState != UploadStatus.QUEUED &&
          lastState != UploadStatus.THEX_REQUEST) {
            state = lastState;
        }
        
        return state;    
    }

    @Override
    public long getTotalAmountUploaded() {
        return uploader.getTotalAmountUploaded();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((uploader == null) ? 0 : uploader.hashCode());
        return result;
    }

    /**
     * Tests if the Uploaders from construction are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CoreUploadItem other = (CoreUploadItem) obj;
        if (uploader == null) {
            if (other.uploader != null)
                return false;
        } else if (!uploader.equals(other.uploader))
            return false;
        return true;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }

    void fireDataChanged() {
        support.firePropertyChange("state", null, getState());
    }

    @Override
    public Category getCategory() {
        return CategoryConverter.categoryForFileName(getFileName());
    }

    @Override
    public RemoteHost getRemoteHost() {
        if(uploadRemoteHost == null)
            uploadRemoteHost = new UploadRemoteHost();
        return uploadRemoteHost;
    }
    
    @Override
    public BrowseType getBrowseType(){
        if (getState() != UploadState.BROWSE_HOST && getState() != UploadState.BROWSE_HOST_DONE){
            return BrowseType.NONE;
        }
        
        if ("".equals(getFileName())){
            return BrowseType.P2P;
        }
        
        return BrowseType.FRIEND;
    }
    
    @Override
    public String toString(){
        return "CoreUploadItem: " + getFileName() + ", " + getState();
    }

    @Override
    public int getQueuePosition() {
        return uploader.getQueuePosition();
    }
    
    @Override
    public float getUploadSpeed() {
        try {
            uploader.measureBandwidth();
            return uploader.getMeasuredBandwidth();
        } catch (InsufficientDataException e) {
            return 0;
        }
    }
    
    @Override
    public long getRemainingUploadTime() {
        float speed = getUploadSpeed();
        if (speed > 0) {
            double remaining = (getFileSize() - getTotalAmountUploaded()) / 1024.0;
            return (long) (remaining / speed);
        } else {
            return UNKNOWN_TIME;
        }
    }
    
    @Override
    public UploadErrorState getErrorState() {
        switch (uploader.getState()) {
        case LIMIT_REACHED:
        case BANNED_GREEDY:
        case FREELOADER:
            return UploadErrorState.LIMIT_REACHED;
        case INTERRUPTED:
        case SUSPENDED:
        case WAITING_REQUESTS:
            return UploadErrorState.INTERRUPTED;
        case FILE_NOT_FOUND:
        case MALFORMED_REQUEST:
        case UNAVAILABLE_RANGE:
            return UploadErrorState.FILE_ERROR;       
        }
        return UploadErrorState.NO_ERROR;
    }
    
    @Override
    public Object getProperty(FilePropertyKey property) {
        FileDesc fd = uploader.getFileDesc();
        if(fd != null) {
            switch(property) {
            case NAME:
                return FileUtils.getFilenameNoExtension(fd.getFileName());
            case DATE_CREATED:
                long ct = fd.lastModified();
                return ct == -1 ? null : ct;
            case FILE_SIZE:
                return fd.getFileSize();            
            default:
                Category category = CategoryConverter.categoryForFileName(fd.getFileName());
                return FilePropertyKeyPopulator.get(category, property, fd.getXMLDocument());
            }
        } else {
            return null;
        }
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

    @Override
    public URN getUrn() {
        com.limegroup.gnutella.URN urn = uploader.getUrn();
        if(urn != null) {
            return urn;
        }
        return null;
    }

    @Override
    public File getFile() {
        return uploader.getFile();
    }

    @Override
    public UploadItemType getUploadItemType() {
        return uploadItemType;
    }

    @Override
    public int getNumUploadConnections() {
        return uploader.getNumUploadConnections();
    }
    
    /**
     * Called when upload is finished. This enables the DONE state. This method
     * is necessary to present false DONE states.
     */
    void finish(){
        isFinished = true;
        fireDataChanged();
    }
    
    /**
     * Creates a RemoteHost for this uploader. This allows browses on the 
     * person uploading this file.
     */
    private class UploadRemoteHost implements RemoteHost {
        
        @Override
        public FriendPresence getFriendPresence() {
            return friendPresence;
        }

        @Override
        public boolean isBrowseHostEnabled() {
            if(friendPresence.getFriend().isAnonymous()) {
                return uploader.isBrowseHostEnabled();
            } else {
                //ensure friend/user still logged in through LW
                return friendPresence.hasFeatures(LimewireFeature.ID);
            }
        }

        @Override
        public boolean isChatEnabled() {
            if(friendPresence.getFriend().isAnonymous()) {
                return false;
            }else { //TODO: this isn't entirely correct. Friend could have signed
                // ouf of LW but still be logged in through other service allowing chat
                return friendPresence.hasFeatures(LimewireFeature.ID);
            }
        }

        @Override
        public boolean isSharingEnabled() {
            if(friendPresence.getFriend().isAnonymous()) {
                return false;
            } else {
                //ensure friend/user still logged in through LW
                return friendPresence.hasFeatures(LimewireFeature.ID);
            }
        }
    }
}
