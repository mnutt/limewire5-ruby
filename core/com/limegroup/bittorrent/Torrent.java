package com.limegroup.bittorrent;

import java.util.List;

import org.limewire.io.Address;

interface Torrent {

	/**
	 * States of a torrent.  Some of them are functionally equivalent
	 * to Downloader states.
	 */
	public enum TorrentState {
        /*
         * d - Downloading a - Active p - Pausable
         * r - Resumable, i - Inactive, c - Completed
         * s - ShouldBeRemoved, iu - Inactive upload
         * ss - StopState
         */
    WAITING_FOR_TRACKER, // a, d, p
    VERIFYING, // a, p
    CONNECTING, // a, d, p
    DOWNLOADING, // a, d, p
    SAVING, // a
    SEEDING, // a, c, s
    QUEUED, // p, i
    PAUSED, // r, i, iu, ss
    STOPPED, // c, iu, ss
    DISK_PROBLEM, // c, s, ss
    TRACKER_FAILURE, // r, c, ss
    SCRAPING, //scraping == requesting from tracker // a, d, p
    INVALID // ss
	};
	
	/**
	 * @return true if the torrent is complete.
	 */
	public abstract boolean isComplete();

	/**
	 * Starts the torrent 
	 */
	public abstract void start();

	/**
	 * Stops the torrent
	 */
	public abstract void stop();

	public void measureBandwidth();

	public float getMeasuredBandwidth(boolean downstream);
	
	public boolean isActive();
	
	public boolean isDownloading();
	
	public TorrentState getState();
	
	public long getNextTrackerRequestTime();

	public int getNumConnections();
	
	public int getTriedHostCount();

	public int getNumPeers();

	public int getNumNonInterestingPeers();
	
	public int getNumChockingPeers();

	public long getTotalDownloaded();
	
	public long getAmountLost();
	
	/**
	 * Resumes the torrent.
	 */
	public abstract boolean resume();

	/**
	 * @return true if paused
	 */
	public boolean isPaused();
	
	public boolean isPausable();
	
	/**
	 * Pauses the torrent.
	 */
	public abstract void pause();

	/** Returns all sources as an address. */
    public abstract List<Address> getSourceAddresses();
}