package org.limewire.core.impl.upload;

import com.limegroup.gnutella.Uploader;

/**
 * Defines a listener for upload events. 
 */
public interface UploadListener {
    
    public void uploadAdded(Uploader uploader);

    public void uploadRemoved(Uploader uploader);
    
    public void uploadsCompleted();

}
