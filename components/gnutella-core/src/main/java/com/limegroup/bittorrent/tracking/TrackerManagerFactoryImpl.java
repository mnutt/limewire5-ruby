package com.limegroup.bittorrent.tracking;

import java.util.concurrent.ScheduledExecutorService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.bittorrent.ManagedTorrent;

@Singleton
public class TrackerManagerFactoryImpl implements TrackerManagerFactory {
    
    private final TrackerFactory trackerFactory;
    private final ScheduledExecutorService backgroundExecutor;
    
    @Inject
    public TrackerManagerFactoryImpl(TrackerFactory trackerFactory, @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor) {
        this.trackerFactory = trackerFactory;
        this.backgroundExecutor = backgroundExecutor;
    }
	
	/* (non-Javadoc)
     * @see com.limegroup.bittorrent.tracking.TrackerManagerFactory#getTrackerManager(com.limegroup.bittorrent.ManagedTorrent)
     */
	public TrackerManager getTrackerManager(ManagedTorrent t) {
		return new TrackerManager(t, trackerFactory, backgroundExecutor);
	}
}
