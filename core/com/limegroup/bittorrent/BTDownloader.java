package com.limegroup.bittorrent;

import com.limegroup.gnutella.downloader.CoreDownloader;

/**
 * Public interface for the facade that BitTorrent downloaders use to connect to
 * LimeWire's core download system.
 */
public interface BTDownloader extends CoreDownloader {

    /**
     * Initializes the downloader with the given meta information. 
     */
    public void initBtMetaInfo(BTMetaInfo btMetaInfo);
}