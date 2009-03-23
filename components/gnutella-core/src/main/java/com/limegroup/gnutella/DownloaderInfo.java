package com.limegroup.gnutella;

/**
 * Classes that implement this interface can be real downloaders or
 * just stand-ins for downloaders.
 */

public interface DownloaderInfo {

    /**
     * Returns the amount read by this so far, in bytes.
     */
    public long getAmountRead();
    
    /**
     * Returns the size of this file in bytes, i.e., the total amount to
     * download or -1 if content length is unknown.
     */
    public long getContentLength();

    
    /**
     * Returns the state of the downloader.
     */
    public DownloadState getState();
    
    /**
     * Determines if the download is completed.
     */
    public boolean isCompleted();

    /** Enumerates the various states of a download. */
    public static enum DownloadState {
        INITIALIZING,
        QUEUED,
        CONNECTING,
        DOWNLOADING,
        BUSY,
        COMPLETE,
        ABORTED,
        GAVE_UP,
        DISK_PROBLEM,
        WAITING_FOR_GNET_RESULTS,
        CORRUPT_FILE,
        REMOTE_QUEUED,
        HASHING,
        SAVING,
        WAITING_FOR_USER,
        WAITING_FOR_CONNECTIONS,
        ITERATIVE_GUESSING,
        QUERYING_DHT,
        IDENTIFY_CORRUPTION,
        RECOVERY_FAILED,
        PAUSED,
        INVALID,
        RESUMING,
        FETCHING,
        DANGEROUS
    }
}
