package com.limegroup.bittorrent;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.bittorrent.disk.DiskManagerFactory;

@Singleton
public class BTContextFactoryImpl implements BTContextFactory {
    
    private final DiskManagerFactory diskManagerFactory;

    @Inject
    public BTContextFactoryImpl(DiskManagerFactory diskManagerFactory) {
        this.diskManagerFactory = diskManagerFactory;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.bittorrent.BTContextFactory#createBTContext(com.limegroup.bittorrent.BTMetaInfo)
     */
    public BTContext createBTContext(BTMetaInfo info) {
        return new BTContext(info, diskManagerFactory);
    }


}
