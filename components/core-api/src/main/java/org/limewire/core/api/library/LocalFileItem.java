package org.limewire.core.api.library;

import java.io.File;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.xmpp.api.client.FileMetaData;

/**
 * A File that is displayed in a library
 */
public interface LocalFileItem extends FileItem {
    /** Returns the file this is based on. */
    File getFile();

    /** Creates {@link FileMetaData} out of this {@link FileItem}. */
    FileMetaData toMetadata();
    
    /** Returns true if this is shared with Gnutella. */
    boolean isSharedWithGnutella();
    
    /** Returns the number of friends this is shared with. */
    int getFriendShareCount();
    
    /** Determines if this file is sharable. */
    boolean isShareable();
    
    /**True if the file is incomplete**/
    boolean isIncomplete();
    
    /**
     * Sets the property of this file item to a new value.
     */
    void setProperty(FilePropertyKey key, Object value);
    
    /** Returns the last modified date of the file. */
    long getLastModifiedTime();

    /** Returns the number of times someone has searched for this file. */
    int getNumHits();

    /** Returns the number of uploads this has completed. */
    int getNumUploads();   
    
    /** Returns the number of uploads this has completed. */
    int getNumUploadAttempts();    
}
