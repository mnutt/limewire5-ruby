package org.limewire.bittorrent;

public interface TorrentStatus {

    /**
     * Returns the rate in bytes/second that data is being downloaded for this
     * torrent.
     */
    public float getDownloadRate();

    /**
     * Returns the rate in byte/second that data is being uploaded for this
     * torrent.
     */
    public float getUploadRate();

    /**
     * Returns the number of peers for this torrent.
     */
    public int getNumPeers();

    /**
     * Returns the number of unchoked peers for this torrent.
     */
    public int getNumUploads();

    /**
     * Returns the number of peers for this torrent are active seeds.
     */
    public int getNumSeeds();

    /**
     * Returns the total number of open connections for this torrent.
     */
    public int getNumConnections();

    /**
     * Returns the progress for downloading this torrent. A number from 0.0 to
     * 1.0 representing 0 to 100%.
     */
    public float getProgress();

    /**
     * Returns the total amount of the torrent downloaded and verified.
     */
    public long getTotalDone();

    /**
     * Returns the total amount of the torrent downloaded.
     */
    public long getTotalDownload();

    /**
     * Returns the total amount of the torrent uploaded.
     */
    public long getTotalUpload();

    /**
     * Returns true if the torrent is paused.
     */
    public boolean isPaused();

    /**
     * Returns true if the torrent is finished.
     */
    public boolean isFinished();

    /**
     * Returns true if the torrent is in an error state.
     */
    public boolean isError();

    /**
     * Returns the LibTorrentState for this torrent.
     */
    public TorrentState getState();

}