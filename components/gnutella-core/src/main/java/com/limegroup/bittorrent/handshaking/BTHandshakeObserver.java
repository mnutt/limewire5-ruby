package com.limegroup.bittorrent.handshaking;

/**
 * An observer for events that happen to BTHandshakers
 */
interface BTHandshakeObserver {

	/**
	 * Notification that the following incoming handshaker
	 * has started handshaking
	 */
	public void handshakerStarted(IncomingBTHandshaker shaker);

	/**
	 * Notification that the passed handshaker has completed.
	 */
	public void handshakerDone(BTHandshaker shaker);

}