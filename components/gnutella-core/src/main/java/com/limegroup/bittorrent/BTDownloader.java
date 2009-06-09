package com.limegroup.bittorrent;

import java.io.File;
import java.io.IOException;

import com.limegroup.gnutella.downloader.CoreDownloader;

public interface BTDownloader extends CoreDownloader {

    /**
     * Initializes the BTDownloader from a torrent file.
     */
    void init(File torrent) throws IOException;

    /**
     * Returns the incomplete file for this Downloader.
     */
    File getIncompleteFile();

    /**
     * Returns the torrent file backing this downloader if any. Value may be
     * null.
     */
    File getTorrentFile();

    /**
     * Registers the internal torrent with the torrent manager.
     */
    void registerTorrentWithTorrentManager();

}