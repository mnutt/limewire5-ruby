package com.limegroup.gnutella.downloader;

import org.limewire.io.Address;

public interface SameHostLimitChecker {

    /**
     * Returns true if a download can be started from the host with 
     * <code>address</code>. This checks the per host limit. 
     */
    boolean canDownloadFrom(Address address);
    
    
    
}
