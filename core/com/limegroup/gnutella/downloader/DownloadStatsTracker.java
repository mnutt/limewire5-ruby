package com.limegroup.gnutella.downloader;

import org.limewire.inspection.Inspectable;

/**
 * Used to track statistics about the way clients connect to peers for downloads (i.e., direct or push), and why pushes are done.
 */
public interface DownloadStatsTracker extends Inspectable {
    
    /**
     * increment a counter indicating connecting directly succeeded
     */
    void successfulDirectConnect();

    /**
     * increment a counter indicating connecting directly failed
     */
    void failedDirectConnect();

    /**
     * increment a counter indicating connecting with a push succeeded
     */
    void successfulPushConnect();

    /**
     * increment a counter indicating connecting directly failed
     */
    void failedPushConnect();

    /**
     * increment a counter corresponding to a particular reasons for
     * attempting to connect via a push
     */
    void increment(PushReason reason);

    /**
     * an enumeration of the reasons for doing a push, as opposed to connecting directly
     */
    public enum PushReason {DIRECT_FAILED, MULTICAST_REPLY, PRIVATE_NETWORK, INVALID_PORT, FIREWALL}
}
