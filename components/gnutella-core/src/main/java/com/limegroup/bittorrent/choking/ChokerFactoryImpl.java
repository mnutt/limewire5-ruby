package com.limegroup.bittorrent.choking;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import org.limewire.collection.NECallable;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.bittorrent.Chokable;
import com.limegroup.gnutella.UploadServices;

@Singleton
public class ChokerFactoryImpl implements ChokerFactory {
    
    private final ScheduledExecutorService scheduledExecutorService;
    private final UploadServices uploadServices;
    
    @Inject    
	public ChokerFactoryImpl(@Named("nioExecutor") ScheduledExecutorService scheduledExecutorService,
            UploadServices uploadServices) {
        this.scheduledExecutorService = scheduledExecutorService;
        this.uploadServices = uploadServices;
    }


    /* (non-Javadoc)
     * @see com.limegroup.bittorrent.choking.ChokerFactory#getChoker(org.limewire.collection.NECallable, boolean)
     */
    public Choker getChoker(NECallable<List<? extends Chokable>> chokables,
            boolean seed) {
        return seed ? new SeedChoker(chokables, scheduledExecutorService,
                uploadServices) : new LeechChoker(chokables,
                scheduledExecutorService, uploadServices);
    }
}