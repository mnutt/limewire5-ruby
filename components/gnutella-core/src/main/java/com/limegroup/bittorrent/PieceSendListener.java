package com.limegroup.bittorrent;

/**
 * Listener that follows the sending of a Piece message.
 */
interface PieceSendListener {
	/** 
	 * Notification that data has been sent over the network
	 * @param written the number of bytes sent
	 */ 
	public void wroteBytes(int written);
	
	/**
	 * Notification that a Piece has been sent.
	 */
	public void pieceSent();
}
