package com.limegroup.bittorrent;

/**
 * Interface describing objects that the unchoking logic works on.
 */
public interface Chokable {

	/**
	 * @return true if we are choking the remote host
	 */
	public boolean isChoked();

	/**
	 * @return true if the remote host is interested in us
	 */
	public boolean isInterested();

	/**
	 * @return whether the remote host should be interested
	 * in downloading from us.
	 */
	public boolean shouldBeInterested();

	/**
	 * @param read whether to return download bandwidth
	 * @param shortTerm whether to return short-term average or long-term
	 * @return the measured bandwidth on this connection for
	 * downloading or uploading
	 */
	public float getMeasuredBandwidth(boolean read, boolean shortTerm);

	/**
	 * Chokes the connection
	 */
	public void choke();

	/**
	 * Unchokes the connection
	 */
	public void unchoke(int now);

	/**
	 * @return the round during which this was last unchoked
	 */
	public int getUnchokeRound();

	/**
	 * clears the round during which this was choked
	 */
	public void clearUnchokeRound();

}