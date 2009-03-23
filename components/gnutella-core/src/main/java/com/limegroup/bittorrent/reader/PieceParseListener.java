package com.limegroup.bittorrent.reader;

/**
 * interface that listens for the event of  
 * getting data from the buffer and writing it to disk.
 */
interface PieceParseListener {
	
	/**
	 * @param stateChange true if the state should be changed.
	 */
	public void dataConsumed(boolean stateChange);
}
