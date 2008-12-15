package com.limegroup.bittorrent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.limewire.collection.NECallable;
import org.limewire.io.Address;
import org.limewire.net.address.StrictIpPortSet;
import org.limewire.nio.observer.Shutdownable;

import com.limegroup.bittorrent.messages.BTHave;

/**
 * Manages BitTorrent links since torrents have hundreds of connections.
 */
public class BTLinkManager implements Shutdownable,
        NECallable<List<? extends Chokable>> {
    /**
	 * The list of BTConnections that this torrent has.
	 * LOCKING: this
	 */
	private final List<BTConnection> _connections;
	
	/**
	 * The locations we are currently connected to.  Torrents have hundreds
	 * of connections so a set is better than iterating.
	 * LOCKING: this
	 */
	private final Set<TorrentLocation> endpoints; 
		
	
	BTLinkManager() {
		_connections = new ArrayList<BTConnection>();
		endpoints = new StrictIpPortSet<TorrentLocation>();
	}
	
	public void shutdown() {
		List<BTLink> copy;
		
		synchronized(this) {
			endpoints.clear();
			copy = new ArrayList<BTLink>(_connections);
			_connections.clear();
		}
		
		for (BTLink toClose : copy)
			toClose.shutdown();
	}
	
	public synchronized List<? extends Chokable> call() {
		return new ArrayList<Chokable>(_connections);
	}
	
	public synchronized void sendHave(BTHave have) {
		for (BTLink btc : _connections) 
			btc.sendHave(have);
	}
	
	public synchronized int getNumConnections() {
		return _connections.size();
	}
	
	public synchronized List<Address> getSourceAddresses() {
	    List<Address> locs = new ArrayList<Address>(_connections.size());
	    for(BTConnection connection : _connections) {
	        locs.add(connection.getEndpoint());
	    }
	    return locs;
	}
	
	public synchronized void addConnection(BTConnection connection) {
		_connections.add(connection);
		endpoints.add(connection.getEndpoint());
	}
	
	public synchronized void removeLink(BTLink link) {
		_connections.remove(link);
		endpoints.remove(link.getEndpoint());
	}
	
	public void disconnectSeedsChokeRest() {
		List<BTLink> seeds = new ArrayList<BTLink>(_connections.size());
		List<BTLink> notSeeds = new ArrayList<BTLink>(_connections.size());
		synchronized(this) {
			for (BTLink btc : _connections) {
				if (btc.isSeed())
					seeds.add(btc);
				else 
					notSeeds.add(btc);
			}
		}
		
		// close all seed connections
		for (BTLink seed : seeds)
			seed.shutdown();
		// suspend the rest
		for (BTLink notSeed : notSeeds)
			notSeed.suspendTraffic();
	}
	
	/**
	 * @param to
	 *            a <tt>TorrentLocation</tt> to check
	 * @return true if we are already connected to it
	 */
	public synchronized boolean isConnectedTo(TorrentLocation to) {
		return endpoints.contains(to);
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.Torrent#getNumBusyPeers()
	 */
	public synchronized int getNumNonInterestingPeers() {
		int busy = 0;
		for (BTLink con : _connections) {
			if (!con.isInteresting())
				busy++;
		}
		return busy;
	}
	
	/**
	 * Returns the number of peers we are curerntly uplaoding to. 
	 */
	public synchronized int getNumUploadingPeers() {
        int upload = 0;
        for (BTLink con : _connections) {
            if (con.isUploading())
                upload++;
        }
        return upload;
    }

	public synchronized int getNumChockingPeers() {
		int qd = 0;
		for (BTLink c : _connections) {
			if (c.isChoking())
				qd++;
		}
		return qd;
	}
	
	public List<BTConnection> getConnections() {
		return _connections;
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.Torrent#measureBandwidth()
	 */
	public synchronized void measureBandwidth() {
		for (BTLink con : _connections) 
			con.measureBandwidth();
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.Torrent#getMeasuredBandwidth(boolean)
	 */
	public synchronized float getMeasuredBandwidth(boolean downstream) {
		float ret = 0;
		for (BTLink con : _connections) 
			ret += con.getMeasuredBandwidth(downstream, true);
		return ret;
	}
	
	/**
	 * @return true if any of the links managed by this are currently uploading.
	 */
	public synchronized boolean hasUploading() {
		for (BTLink link : _connections) {
			if (link.isUploading())
				return true;
		}
		return false;
	}
	
	/**
	 * @return true if any of the links managed by this are not choked.
	 */
	public synchronized boolean hasUnchoked() {
		if (_connections.isEmpty())
			return false;
		for (BTLink link : _connections) {
			if (!link.isChoked())
				return false;
		}
		return true;
	}
	
	/**
	 * @return true if any of the links managed by this are interested.
	 */
	public synchronized boolean hasInterested() {
		if (_connections.isEmpty())
			return false;
		for (BTLink link : _connections) {
			if (!link.isInterested())
				return true;
		}
		return false;
	}
}
