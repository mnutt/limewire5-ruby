package org.limewire.core.api.upload;

import java.beans.PropertyChangeListener;
import java.io.File;

import org.limewire.core.api.Category;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.library.PropertiableFile;

/**
 * A single upload.
 */
public interface UploadItem extends PropertiableFile {
    
    public enum UploadItemType {
        GNUTELLA,
        BITTORRENT
    }
    
    public enum BrowseType {FRIEND, P2P, NONE}

    /**
     * Cancels the upload.
     */
    public void cancel();

    /**
     * @return the {@link UploadState} of the upload
     */
    public UploadState getState();
    
    /**
     * Returns the amount of data that this uploader and all previous
     * uploaders exchanging this file have uploaded. 
     */
    public long getTotalAmountUploaded();
    
    /**
     * @return the name of the file being uploaded.
     */
    public String getFileName();
    
    /**
     * @return the length of the file being uploaded.
     */ 
    public long getFileSize();
    
    public void addPropertyChangeListener(PropertyChangeListener listener);
    
    public void removePropertyChangeListener(PropertyChangeListener listener);

    /**
     * @return the <code>Category</code> of the File being uploaded
     */
    public Category getCategory();
    
    /**
     * Returns the source of this Upload.
     */
    public RemoteHost getRemoteHost();
    
    /**
     * Returns the current queue position if queued.
     */
    public int getQueuePosition();

    public long getRemainingUploadTime();

    float getUploadSpeed();

    UploadErrorState getErrorState();

    /**
     * Returns the file backing this upload item. 
     */
    public File getFile();
    
    /**
     * Returns the type of this upload item.
     */
    public UploadItemType getUploadItemType();

    /**
     * Returns the number of connections we are currently uploading to.
     */
    public int getNumUploadConnections();

    BrowseType getBrowseType();
}
