package com.limegroup.gnutella.downloader;

import org.limewire.listener.DefaultEvent;

import com.limegroup.gnutella.Downloader.DownloadStatus;

public class DownloadStatusEvent extends DefaultEvent<CoreDownloader, DownloadStatus> {

    public DownloadStatusEvent(CoreDownloader source, DownloadStatus event) {
        super(source, event);
    }

}
