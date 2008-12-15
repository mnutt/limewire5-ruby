package com.limegroup.bittorrent;

/**
 * Defines the interface for getting equal chunks of a BitTorrent for 
 * transfers.
 * <p>
 * See <a href="http://jonas.nitro.dk/bittorrent/bittorrent-rfc.html#anchor3">Piece</a>.
 */
public interface BTPiece {

	/**
	 * @return the interval this Piece represents
	 */
	public BTInterval getInterval();

	/**
	 * @return requested data
	 */
	public byte[] getData();

}