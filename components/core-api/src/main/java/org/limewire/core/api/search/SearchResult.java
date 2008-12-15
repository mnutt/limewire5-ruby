package org.limewire.core.api.search;

import java.util.List;
import java.util.Map;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;
import org.limewire.core.api.endpoint.RemoteHost;

public interface SearchResult {

    String getFileExtension();
    
    Map<FilePropertyKey, Object> getProperties();

    Object getProperty(FilePropertyKey key);
    
    Category getCategory();

    long getSize();
    
    /**
     * Returns a subset of sources identified for a file. 
     * Limiting the number of sources to friends plus 2 other sources.
     */
    List<RemoteHost> getSources();
    
    URN getUrn();
    
    public boolean isSpam();
    
    /**
     * @return full file name including extension
     */
    String getFileName();
    
    String getMagnetURL();

    public int getRelevance();

    /**
     * @return true if the underlying associated file contains a license
     */
    boolean isLicensed();
}