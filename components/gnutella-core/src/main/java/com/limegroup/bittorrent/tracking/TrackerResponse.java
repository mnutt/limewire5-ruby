package com.limegroup.bittorrent.tracking;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.settings.BittorrentSettings;
import org.limewire.io.NetworkUtils;
import org.limewire.service.ErrorService;
import org.limewire.util.ByteUtils;
import org.limewire.util.StringUtils;

import com.limegroup.bittorrent.TorrentLocation;
import com.limegroup.bittorrent.ValueException;

/**
 * Parses the response from a tracker.
 */
class TrackerResponse {
	private static final Log LOG = LogFactory.getLog(TrackerResponse.class);

	/**
	 * List of peers contained in the tracker response
	 */
	public final List<TorrentLocation> PEERS;

	/**
	 * The interval in seconds that we must wait before sending the next request
	 */
	public final int INTERVAL;

	/**
	 * the number of peers, the tracker knows about
	 */
	public final int NUM_PEERS;

	/**
	 * the number of peers holding the complete file
	 */
	public final int DONE_PEERS;

	/**
	 * the failure reason the tracker supplied
	 */
	public final String FAILURE_REASON;

	/**
	 * private constructor for the TrackerResponse
	 * 
	 * @param t_response
	 *            the <tt>Map</tt> decoded from the InputSteam
	 * @throws ValueException for received bencoded data that does not 
	 * match the expected structure  
	 */
	public TrackerResponse(Object t_response) throws ValueException {
		if (!(t_response instanceof Map))
			throw new ValueException("bad tracker response");
		Map response = (Map) t_response;

		if (response.containsKey("failure reason")) {
			byte [] failureBytes = (byte [])response.get("failure reason");
			String reason = StringUtils.getASCIIString(failureBytes);
			if (reason.length() > 256) 
				reason = reason.substring(0, 255);
			FAILURE_REASON = reason;
		} else
			FAILURE_REASON = null;

		if (response.containsKey("peers")) {
			Object t_peers = response.get("peers");
			if (t_peers instanceof List)
				PEERS = parsePeers((List) t_peers);
			else if (t_peers instanceof byte [])
				PEERS = parsePeers( (byte[]) t_peers);
			else
				throw new ValueException("bad tracker response - bad peers "
						+ t_peers);
		} else
			PEERS = Collections.emptyList();

		Object t_interval = response.get("interval");
		Object t_minInterval = response.get("min_interval");

		int interval;
		if (t_interval instanceof Long) {
			interval = (int) ((Long) t_interval).longValue();
		} else if (t_minInterval instanceof Long) {
			interval = (int) ((Long) t_interval).longValue();
		} else
			interval = BittorrentSettings.TRACKER_MIN_REASK_INTERVAL.getValue();

		if (interval < BittorrentSettings.TRACKER_MIN_REASK_INTERVAL.getValue())
			INTERVAL = BittorrentSettings.TRACKER_MIN_REASK_INTERVAL.getValue();
		else if (interval > BittorrentSettings.TRACKER_MAX_REASK_INTERVAL
				.getValue())
			INTERVAL = BittorrentSettings.TRACKER_MAX_REASK_INTERVAL.getValue();
		else
			INTERVAL = interval;

		Object t_numPeers = response.get("num peers");
		if (t_numPeers instanceof Long) {
			NUM_PEERS = (int) ((Long) t_numPeers).longValue();
		} else {
			t_numPeers = response.get("incomplete");
			if (t_numPeers instanceof Long) {
				NUM_PEERS = (int) ((Long) t_numPeers).longValue();
			} else {
				NUM_PEERS = 0;
			}
		}

		Object t_donePeers = response.get("num done");
		if (t_donePeers instanceof Long) {
			DONE_PEERS = (int) ((Long) t_donePeers).longValue();
		} else {
			t_donePeers = response.get("complete");
			if (t_donePeers instanceof Long) {
				DONE_PEERS = (int) ((Long) t_donePeers).longValue();
			} else {
				DONE_PEERS = 0;
			}
		}
	}

	TrackerResponse(List<TorrentLocation> peers, int interval, int numPeers, int donePeers,
			String failureReason) {
		PEERS = peers;
		INTERVAL = Math.min(Math.max(interval,
				BittorrentSettings.TRACKER_MIN_REASK_INTERVAL.getValue()),
				BittorrentSettings.TRACKER_MAX_REASK_INTERVAL.getValue());
		NUM_PEERS = Math.max(0, numPeers);
		DONE_PEERS = Math.max(0, donePeers);
		FAILURE_REASON = failureReason;
	}

	/**
	 * private utility method to parse a List of peers
	 * 
	 * @param peers
	 *            List of <tt>Map</tt>
	 * @return List of <tt>TorrentLocation</tt>
	 * 
	 * @throws ValueException for received bencoded data that does not 
     * match the expected structure
	 */
	private static List<TorrentLocation> parsePeers(List peers) throws ValueException {
		List<TorrentLocation> ret = new ArrayList<TorrentLocation>();
		for (Iterator iter = peers.iterator(); iter.hasNext();) {
			Object t_peer = iter.next();
			if (!(t_peer instanceof Map))
				throw new ValueException("bad tracker response - bad peer "
						+ t_peer);
			ret.add(parsePeer((Map) t_peer));
		}
		return Collections.unmodifiableList(ret);
	}

	/**
	 * parsing List of peers from pairs of big endian 4-byte ip address and
	 * 2-byte ports
	 * 
	 * @param bytes
	 *            the array to parse the peers from
	 * @return non-null <tt>List</tt> of <tt>TorrentLocation</tt>
	 * @throws ValueException for received bencoded data that does not 
     * match the expected structure
	 */
	static List<TorrentLocation> parsePeers(byte[] bytes) throws ValueException {
        boolean containedInvalid = false;
		ArrayList<TorrentLocation> ret = new ArrayList<TorrentLocation>();
		for (int i = 0; i < bytes.length - 5; i += 6) {
			byte[] address = new byte[4];
			System.arraycopy(bytes, i, address, 0, 4);
			int port = ByteUtils.beb2int(bytes, i + 4, 2);
            
            if (!NetworkUtils.isValidPort(port)) {
                containedInvalid = true;
                continue;
            }
			try {
				InetAddress addr = InetAddress.getByAddress(address);
				ret.add(parsePeer(addr, port, null));
			} catch (UnknownHostException uhe) {
			    containedInvalid = true;
			}
		}
        
        if (ret.isEmpty() && containedInvalid)
            throw new ValueException("no peers or all invalid");

		return Collections.unmodifiableList(ret);
	}

	/**
	 * private utility method, creates a TorrentLocation from a <tt>Map</tt>
	 * 
	 * @param peer
	 *            a <tt>Map</tt> containing peer address, port and id
	 * @throws
	 *         ValueException for received bencoded data that does not 
     * match the expected structure
	 */
	private static TorrentLocation parsePeer(Map peer) throws ValueException {
		Object t_ip = peer.get("ip");
		if (!(t_ip instanceof byte []))
			throw new ValueException("bad tracker response - bad peer ip "
					+ t_ip);
		InetAddress addr;
		try {
			String ipS = new String((byte [])t_ip, org.limewire.util.Constants.ASCII_ENCODING);
			addr = InetAddress.getByName(ipS);
		} catch (UnknownHostException uhe) {
			throw new ValueException("bad tracker response - bad peer ip "
					+ t_ip);
		} catch (UnsupportedEncodingException impossible) {
			ErrorService.error(impossible);
			return null;
		}

		Object t_port = peer.get("port");
		if (!(t_port instanceof Long))
			throw new ValueException("bad tracker response - bad peer port "
					+ t_port);

		int port = (int) ((Long) t_port).longValue();

		Object t_peerId = peer.get("peer id");
		if ( ! (t_peerId instanceof byte []))
			throw new ValueException("bad tracker response - bad peer id ");
		
		byte [] peerId = (byte []) t_peerId;
		if (peerId.length != 20)
			throw new ValueException("bad tracker response - bad peer id ");

		return parsePeer(addr, port, peerId);

	}

	/**
	 * creates a new <tt>TorrentLocation</tt> object and check whether address
	 * and port are valid
	 * 
	 * @param addr
	 *            the <tt>InetAddress</tt> for the location
	 * @param port
	 *            an int, specifying the location's listening port
	 * @param peerId
	 *            the peer ID of the location
	 * @return new <tt>TorrentLocation</tt> object
	 * @throws ValueException for received bencoded data that does not 
     * match the expected structure
	 */
	private static TorrentLocation parsePeer(InetAddress addr, int port,
			byte [] peerId) throws ValueException {
		if (!NetworkUtils.isValidAddress(addr))
			throw new ValueException("bad tracker response - bad peer ip "
					+ addr);

		if (!NetworkUtils.isValidPort(port))
			throw new ValueException("bad tracker response - bad peer port "
					+ port);
		TorrentLocation to = new TorrentLocation(addr, port, peerId);

		if (LOG.isDebugEnabled())
			LOG.debug("got peer " + to);
		return to;
	}
	
	@Override
    public String toString() {
		return "tracker response: min interval "+INTERVAL+
		","+DONE_PEERS+"/"+NUM_PEERS+ (FAILURE_REASON != null ? FAILURE_REASON :"")+
		", num peers: "+PEERS.size();
	}
}
