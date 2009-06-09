package org.limewire.bittorrent;

import java.io.File;

public interface TorrentSettings {

    /**
     * Returns the upload rate limit for libtorrent in bytes/second. A value of
     * 0 is means it is unlimited.
     */
    public int getMaxUploadBandwidth();

    /**
     * Returns the download rate limit for libtorrent in bytes/second. A value
     * of 0 is means it is unlimited.
     */
    public int getMaxDownloadBandwidth();

    /**
     * Returns true if the Torrent capabilities are enabled.
     */
    public boolean isTorrentsEnabled();

    /**
     * Returns the path that incomplete torrent downloads will be downloaded to.
     */
    public File getTorrentDownloadFolder();

}