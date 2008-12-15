package com.limegroup.bittorrent;

import java.util.concurrent.ScheduledExecutorService;

import com.limegroup.bittorrent.handshaking.BTConnectionFetcher;

/**
 * Defines an interface for keeping track of a single torrent.
 */
public interface ManagedTorrent extends Torrent, BTLinkListener {

    /**
     * notification that a request to the tracker(s) has started.
     */
    void setScraping();

    /**
     * Accessor for the info hash
     * 
     * @return byte[] containing the info hash
     */
    byte[] getInfoHash();

    /**
     * Accessor for meta info
     * 
     * @return <tt>BTMetaInfo</tt> for this torrent
     */
    BTMetaInfo getMetaInfo();

    /**
     * @return the <tt>TorrentContext</tt> for this torrent
     */
    TorrentContext getContext();

    void trackerRequestFailed();

    /**
     * adds location to try
     * 
     * @param to a TorrentLocation for this download
     */
    void addEndpoint(TorrentLocation to);

    /**
     * Stops the torrent because of tracker failure.
     */
    void stopVoluntarily();

    /**
     * @return true if we need to fetch any more connections
     */
    boolean needsMoreConnections();

    /**
     * @return true if a fetched connection should be added.
     */
    boolean shouldAddConnection(TorrentLocation loc);

    /**
     * adds a fetched connection
     * 
     * @return true if it was added
     */
    boolean addConnection(final BTConnection btc);

    /**
     * @return a peer we should try to connect to next
     */
    TorrentLocation getTorrentLocation();

    /**
     * two torrents are equal if their infoHashes are.
     */
    boolean equals(Object o);

    long getTotalUploaded();

    /**
     * @return the ratio of uploaded / downloaded data.
     */
    float getRatio();

    boolean hasNonBusyLocations();

    /**
     * @return the time until a recently failed location can be retried, or
     *         Long.MAX_VALUE if no such found.
     */
    long getNextLocationRetryTime();

    /**
     * @return true if continuing is hopeless
     */
    boolean shouldStop();

    /**
     * @return the <tt>BTConnectionFetcher</tt> for this torrent.
     */
    BTConnectionFetcher getFetcher();

    /**
     * @return the <tt>SchedulingThreadPool</tt> executing network- related
     *         tasks
     */
    ScheduledExecutorService getNetworkScheduledExecutorService();

    /**
     * @return true if this torrent is currently uploading
     */
    boolean isUploading();

    /**
     * @return true if this torrent is currently suspended A torrent is
     *         considered suspended if there are connections interested in it
     *         but all are choked.
     */
    boolean isSuspended();    
    
    /**
     * Returns the BTLinkManager for this torrent instance.
     */
    BTLinkManager getLinkManager();

    /**
     * Returns the number of peers you are currently uploading to. 
     */
    int getNumUploadPeers();

}