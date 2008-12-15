package com.limegroup.bittorrent;

import org.limewire.nio.observer.IOErrorObserver;

import com.limegroup.bittorrent.messages.BTHave;
import com.limegroup.gnutella.BandwidthTracker;

/**
 * An interface describing a link between two BitTorrent hosts.
 */
interface BTLink extends Chokable, IOErrorObserver, BandwidthTracker {
	
	/**
	 * @return true if this is a link to a seed node.
	 */
	public boolean isSeed();
	
	/**
	 * Suspends the traffic on this link
	 */
	public void suspendTraffic();
	
	/**
	 * @return true if data is being uploaded on this link
	 */
	public boolean isUploading();
	
	/**
	 * @return a <tt>TorrentLocation</tt> describing the remote
	 * end of this link
	 */
	public TorrentLocation getEndpoint();
	
	/**
	 * @return true if the host on the other side of the link is busy
	 */
	public boolean isChoking();
	
	/**
	 * @return true if the remote host has ranges we don't.
	 */
	public boolean isInteresting();
	
	/**
	 * sends a <tt>BTHave</tt> message on this link. 
	 */
	public void sendHave(BTHave have);
	
	/**
	 * @return true if a new link to the remote host should be retried in 
	 * case this one fails.
	 */
	public boolean isWorthRetrying();
}
