package com.limegroup.gnutella.downloader;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.DownloadCallback;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.Downloader.DownloadStatus;
import com.limegroup.gnutella.version.UpdateHandler;

/**
 * Once an in-network download finishes, the UpdateHandler is notified.
 */
@Singleton
public class InNetworkCallback implements DownloadCallback {
    
    private final UpdateHandler updateHandler;

    @Inject
    public InNetworkCallback(UpdateHandler updateHandler) {
        this.updateHandler = updateHandler;
    }
    
    public void addDownload(Downloader d) {
    }

    public void downloadCompleted(Downloader d) {
        InNetworkDownloader downloader = (InNetworkDownloader) d;
        updateHandler.inNetworkDownloadFinished(
                downloader.getSha1Urn(),
                downloader.getState() == DownloadStatus.COMPLETE);
    }

    public void downloadsComplete() {
    }

    public void showDownloads() {
    }

    // always discard corruption.
    public void promptAboutCorruptDownload(Downloader dloader) {
        dloader.discardCorruptDownload(true);
    }

    public String getHostValue(String key) {
        return null;
    }
}