package org.limewire.libtorrent;


import org.limewire.bittorrent.TorrentSettings;

import com.sun.jna.Structure;

public class LibTorrentSettings extends Structure {

    /**
     * upload rate limit for libtorrent in bytes/second. A value of 0 is means
     * it is unlimited.
     */
    public int max_upload_bandwidth = 0;

    /**
     * Download rate limit for libtorrent in bytes/second. A value of 0 is means
     * it is unlimited.
     */
    public int max_download_bandwidth = 0;

    public LibTorrentSettings(TorrentSettings torrentSettings) {
        this.max_upload_bandwidth = torrentSettings.getMaxUploadBandwidth();
        this.max_download_bandwidth = torrentSettings.getMaxDownloadBandwidth();
    }
}
