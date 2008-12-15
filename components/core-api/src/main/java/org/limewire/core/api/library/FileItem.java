package org.limewire.core.api.library;


/**
 * A File that is displayed in a library
 */
public interface FileItem extends PropertiableFile {
    
    /**
     * @return the name without the extension
     */
    String getName();
    
    /**
     * @return the full file name including extension
     */
    String getFileName(); 
    
    long getSize();

    long getCreationTime();
    
}