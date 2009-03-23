package com.limegroup.bittorrent;

/**
 * Listener for a disk read event for a Piece. 
 */
public interface PieceReadListener {

	/**
	 * Notification that the disk read has completed.
	 * @param interval the interval that was read
	 * @param data the data of that interval.
	 */
	public void pieceRead(BTInterval interval, byte [] data);
	
	/**
	 * Notification that reading of the Piece failed.
	 */
	public void pieceReadFailed(BTInterval interval);
}
