package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.version.DownloadInformation;

/** Public interface by which InNetworkDownloads can be used. */
public interface InNetworkDownloader extends ManagedDownloader {

    public void initDownloadInformation(DownloadInformation downloadInformation, long startTime);

    /**
     * @return how many times was this download attempted
     */
    public int getDownloadAttempts();

    public long getStartTime();

}

