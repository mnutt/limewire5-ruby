package com.limegroup.bittorrent;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ActivityCallback;

@Singleton
public class BTUploaderFactoryImpl implements BTUploaderFactory {

    private final Provider<TorrentManager> torrentManager;
    private final Provider<ActivityCallback> activityCallback;

    @Inject
    public BTUploaderFactoryImpl(Provider<TorrentManager> torrentManager, Provider<ActivityCallback> activityCallback) {
        this.torrentManager = torrentManager;
        this.activityCallback = activityCallback;
    }
    
    public BTUploader createBTUploader(ManagedTorrent torrent,
            BTMetaInfo info) {
        return new BTUploader(torrent, info, torrentManager.get(), activityCallback.get());
    }

}
