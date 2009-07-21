package com.limegroup.bittorrent;

import org.limewire.bittorrent.Torrent;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ActivityCallback;

@Singleton
public class BTUploaderFactoryImpl implements BTUploaderFactory {

    private final Provider<ActivityCallback> activityCallback;

    @Inject
    public BTUploaderFactoryImpl(Provider<ActivityCallback> activityCallback) {
        this.activityCallback = activityCallback;
    }

    @Override
    public BTUploader createBTUploader(Torrent torrent) {
        BTUploader btUploader = new BTUploader(torrent, activityCallback.get());
        btUploader.registerTorrentListener();
        activityCallback.get().addUpload(btUploader);
        return btUploader;
    }

}
