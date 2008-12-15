package com.limegroup.bittorrent;


import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;

import org.limewire.io.ConnectableImpl;
import org.limewire.io.IpPort;
import org.limewire.util.Base32;


/**
 * Represents a remote computer on the Internet running BitTorrent software.
 * It's downloading files with BitTorrent, so we can use it as a location to 
 * get parts of files from.
 * <p>
 * The TorrentLocation class extends IpPortImpl to have an IP address and port number.
 */
public class TorrentLocation extends ConnectableImpl {

	/**
	 * Use this for unknown peer ids
	 */
	private static final byte [] NULL_PEER_STRING = new byte[20];

	/**
	 * The extention bytes that we support.
	 * Since we don't support any extentions at the moment, they're all 0.
	 */
	private static final byte[] EXTENTION_BYTES = new byte[8];

	/**
	 * How many times can a connect attempt to this endpoint fail.
	 */
	private static final int MAX_STRIKES = 1;

	/**
	 * How much time to wait before retrying in case of failure.
	 */
	private static final int BUSY_WAIT_TIME = 5 * 60 * 1000;

	/** 
	 * The peer ID of the location.  It may get changed during the 
	 * handshaking process, so this object should not be part of
	 * any collection during handshaking.
	 */
	private final byte [] PEER_ID;

	/**
	 * Extention bytes for this location.
	 */
	private final byte[] EXTENSION_BYTES;

	/**
	 * # of times connecting to this location has failed
	 */
	private int _strikes = 0;

	/**
	 * time for the next attempt.
	 */
	private long _nextRetryTime = 0;

	/**
	 * Constructs a torrent location with the specified address, port,
	 * peerId and extention bytes.
	 */
	public TorrentLocation(InetSocketAddress address, byte[] peerId,
			byte[] extensionBytes) {
		super(address, false);
		PEER_ID = (peerId == null) ? NULL_PEER_STRING : peerId;
		EXTENSION_BYTES = extensionBytes;
	}

	/**
	 * Creates a new torrent location with unknown extention bytes
	 * (Tracker responses do not carry that information)
	 */
	public TorrentLocation(InetAddress address, int port, byte [] peerId) {
		super(address, port, false);
		PEER_ID = (peerId == null) ? NULL_PEER_STRING : peerId;
		EXTENSION_BYTES = EXTENTION_BYTES;
	}

	public TorrentLocation(TorrentLocation to) {
		this(to.getInetSocketAddress(), to.getPeerID(),
				to.EXTENSION_BYTES);
	}

	/**
	 * @return BitTorrent peer ID
	 */
	public byte [] getPeerID() {
		return PEER_ID;
	}
	
	/**
	 * @return the extention bytes
	 */
	public byte [] getExtBytes() {
		return EXTENSION_BYTES;
	}

	/**
	 * call this method if a connection attempt for this location has failed
	 */
	public void strike() {
		_nextRetryTime = System.currentTimeMillis() + BUSY_WAIT_TIME;
		_strikes++;
	}

	/**
	 * @return true if we should wait before retrying
	 */
	public boolean isBusy(long now) {
		return _nextRetryTime > now;
	}
	
	/**
	 * @param now the current time in milliseconds
	 * @return number of milliseconds to wait before next connection attempt
	 */
	public long getWaitTime(long now) {
		return Math.max(0, _nextRetryTime - now);
	}
	
	/**
	 * @return true if this location has failed too many times
	 */
	public boolean isOut() {
		return _strikes > MAX_STRIKES;
	}

	/**
	 * @return true, if this is an Endpoint to a LimeWire node
	 */
	public boolean isLimePeer() {
		return PEER_ID[0] == (byte)'L' &&
		PEER_ID[1] == (byte)'I' &&
		PEER_ID[2] == (byte)'M' &&
		PEER_ID[3] == (byte)'E';
	}

	@Override
    public boolean equals(Object o) {
		if (!(o instanceof TorrentLocation))
			return false;
		TorrentLocation other = (TorrentLocation)o;
		if (IpPort.COMPARATOR.compare(this, other) != 0)
			return false;
		return Arrays.equals(other.PEER_ID,this.PEER_ID);
	}

	@Override
    public String toString() {
		return getAddress() + ":" + getPort() + ":" + new String(PEER_ID) + ":"
				+ Base32.encode(EXTENSION_BYTES);
	}
}
