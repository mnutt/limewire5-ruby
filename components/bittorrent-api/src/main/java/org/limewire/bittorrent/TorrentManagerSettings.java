package org.limewire.bittorrent;

import java.io.File;

public interface TorrentManagerSettings {

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

    /**
     * Returns true if the setting to report library load failures is turned on.
     */
    boolean isReportingLibraryLoadFailture();

    /**
     * Returns the listening start port.
     */
    public int getListenStartPort();

    /**
     * Returns the listening end port.
     */
    public int getListenEndPort();

    /**
     * Returns the directory that the fast resume file and torrent file are
     * stored for torrent uploads.
     */
    public File getTorrentUploadsFolder();

    /**
     * Default seed ratio to have considered met seeding criteria.
     */
    public float getSeedRatioLimit();

    /**
     * Default seed time over download time to have considered met seeding
     * criteria.
     */
    public float getSeedTimeRatioLimit();

    /**
     * Default amount of seed time to have considered having met seeding
     * criteria.
     */
    public int getSeedTimeLimit();

    /**
     * Returns the limit for the active number of active managed torrent downloads.
     */
    public int getActiveDownloadsLimit();

    /**
     * Returns the limit for the active number of active managed torrent seeds.
     */
    public int getActiveSeedsLimit();

    /**
     * Returns the limit for the total number of active managed torrents.
     */
    public int getActiveLimit();
}