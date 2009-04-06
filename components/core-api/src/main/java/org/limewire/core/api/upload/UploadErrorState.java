package org.limewire.core.api.upload;

/**
 * Used to lump various core {@link Uploader} error states into general groups usable for
 *  the generic {@link UploadItem}. 
 */
public enum UploadErrorState {
    LIMIT_REACHED, INTERRUPTED, FILE_ERROR, 
    
    /**
     * The {@link Uploader} managed by the {@link UploadItem} is not in an error state.
     */
    NO_ERROR
}
