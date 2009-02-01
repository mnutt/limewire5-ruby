package com.limegroup.gnutella.downloader;

import org.limewire.listener.DefaultEvent;

import com.limegroup.gnutella.Downloader.DownloadState;

public class DownloadStateEvent extends DefaultEvent<CoreDownloader, DownloadState> {

    public DownloadStateEvent(CoreDownloader source, DownloadState event) {
        super(source, event);
    }

}
