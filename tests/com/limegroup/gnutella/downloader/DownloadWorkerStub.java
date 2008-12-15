package com.limegroup.gnutella.downloader;

import java.util.concurrent.ScheduledExecutorService;

import org.limewire.net.SocketsManager;
import org.limewire.net.TLSManager;

import com.google.inject.Provider;
import com.limegroup.gnutella.RemoteFileDesc;

class DownloadWorkerStub extends DownloadWorker {


    public DownloadWorkerStub(DownloadWorkerSupport manager, RemoteFileDescContext rfd, VerifyingFile vf,
            HTTPDownloaderFactory httpDownloaderFactory,
            ScheduledExecutorService backgroundExecutor, ScheduledExecutorService nioExecutor,
            Provider<PushDownloadManager> pushDownloadManager, SocketsManager socketsManager,
            TLSManager TLSManager) {
        super(manager, rfd, vf, httpDownloaderFactory, backgroundExecutor, nioExecutor,
                pushDownloadManager, socketsManager, new DownloadStatsTrackerImpl(), TLSManager);
    }

    @Override
    HTTPDownloader getDownloader() {
        return null;
    }

    @Override
    public RemoteFileDesc getRFD() {
        return null;
    }

    @Override
    void interrupt() {
    }
}
