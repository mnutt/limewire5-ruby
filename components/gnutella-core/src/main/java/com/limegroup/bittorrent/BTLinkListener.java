package com.limegroup.bittorrent;

/**
 * Defines an interface to listen to links between two BitTorrent hosts.
 */
public interface BTLinkListener {
	public void linkClosed(BTLink closed);
	public void countDownloaded(int downloaded);
	public void linkInterested(BTLink interested);
	public void linkNotInterested(BTLink notInterested);
}
