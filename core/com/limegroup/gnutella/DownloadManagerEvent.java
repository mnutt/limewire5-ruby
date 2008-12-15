package com.limegroup.gnutella;

import org.limewire.listener.DefaultEvent;

import com.limegroup.gnutella.downloader.CoreDownloader;

public class DownloadManagerEvent extends DefaultEvent<CoreDownloader, DownloadManagerEvent.Type> {

    public enum Type {
        ADDED,
        REMOVED
    }
    
    public DownloadManagerEvent(CoreDownloader source, Type event) {
        super(source, event);
    }

}
