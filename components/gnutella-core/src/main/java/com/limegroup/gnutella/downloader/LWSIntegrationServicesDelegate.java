package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.DownloadManager;

/**
 * An interface for the {@link LWSIntegrationServices} to talk to. Currently
 * {@link DownloadManager} immplements this.
 */
public interface LWSIntegrationServicesDelegate {
    
    void visitDownloads(Visitor<CoreDownloader> d);

}
