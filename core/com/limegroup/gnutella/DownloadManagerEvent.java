package com.limegroup.gnutella;

import org.limewire.listener.DefaultSourceTypeEvent;

import com.limegroup.gnutella.downloader.CoreDownloader;

public class DownloadManagerEvent extends DefaultSourceTypeEvent<CoreDownloader, DownloadManagerEvent.Type> {

    public enum Type {
        ADDED,
        REMOVED
    }
    
    public DownloadManagerEvent(CoreDownloader source, Type event) {
        super(source, event);
    }

}
