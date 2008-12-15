package com.limegroup.bittorrent.tracking;

import com.limegroup.bittorrent.ManagedTorrent;

public interface TrackerManagerFactory {

    public TrackerManager getTrackerManager(ManagedTorrent t);

}