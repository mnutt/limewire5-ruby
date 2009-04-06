package org.limewire.core.api.upload;

/**
 * The states related to managed uploads encapsulated in {@link UploadItem}.  
 */
public enum UploadState {
    
    /**
     * Upload is being queued.
     */
    QUEUED, 
    
    /**
     * Upload completed.
     */
    DONE, 
    
    /**
     * Upload in progress.
     */
    UPLOADING, 
    
    /**
     * An error occurred and the upload has been stopped.
     */
    UNABLE_TO_UPLOAD, 
    
    /**
     * Upload cancelled by user.
     */
    CANCELED, 
    
    /**
     * Only related to bittorrent?
     * <p>{@link UploadStatus.WAITING_REQUESTS}
     */
    WAITING, 
    
    /**
     * This upload corresponds to a a live browse host.
     */
    BROWSE_HOST, 
    
    /**
     * The upload was a browse host, but is now complete.
     */
    BROWSE_HOST_DONE
}
