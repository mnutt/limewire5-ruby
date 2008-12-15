package com.limegroup.bittorrent;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.BandwidthManager;
import com.limegroup.gnutella.uploader.UploadSlotManager;

@Singleton
public class BTConnectionFactoryImpl implements BTConnectionFactory {

    private final Provider<UploadSlotManager> uSlotManager;
    private final Provider<BandwidthManager> bwManager;
    
    @Inject
    public BTConnectionFactoryImpl(Provider<UploadSlotManager> uSlotManager, 
            Provider<BandwidthManager> bwManager) {
        this.uSlotManager = uSlotManager;
        this.bwManager = bwManager;
    }
    
    public BTConnection createBTConnection(TorrentContext context,
            TorrentLocation loc) {
        return new BTConnection(context, loc, bwManager.get(), uSlotManager.get());
    }

}
