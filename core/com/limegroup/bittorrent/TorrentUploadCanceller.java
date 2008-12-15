package com.limegroup.bittorrent;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ActivityCallback;

@Singleton
public class TorrentUploadCanceller implements TorrentEventListener {

    private final ActivityCallback activityCallback;

    @Inject
    private TorrentUploadCanceller(ActivityCallback activityCallback) {
        this.activityCallback = activityCallback;
    }

    public void handleTorrentEvent(TorrentEvent evt) {
        if (evt.getType() != TorrentEvent.Type.STOP_REQUESTED)
            return;
        ManagedTorrent t = evt.getTorrent();
        activityCallback.promptTorrentUploadCancel(t);
    }

    public void register(TorrentManager torrentManager) {
        torrentManager.addEventListener(this);
    }
}
