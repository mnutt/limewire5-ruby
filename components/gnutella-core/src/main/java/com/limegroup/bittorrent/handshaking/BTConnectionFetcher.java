package com.limegroup.bittorrent.handshaking;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.Periodic;
import org.limewire.io.IOUtils;
import org.limewire.io.IpPort;
import org.limewire.net.SocketsManager;
import org.limewire.net.address.StrictIpPortSet;
import org.limewire.nio.AbstractNBSocket;
import org.limewire.nio.observer.ConnectObserver;
import org.limewire.nio.observer.Shutdownable;
import org.limewire.service.ErrorService;

import com.limegroup.bittorrent.BTConnectionFactory;
import com.limegroup.bittorrent.ManagedTorrent;
import com.limegroup.bittorrent.TorrentLocation;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.Constants;

/**
 * Reacts to the start and completion of a BitTorrent connection handshake.
 * Additionally, <code>BTConnectionFetcher</code> returns an outgoing handshake.
 */
public class BTConnectionFetcher implements BTHandshakeObserver, Runnable, Shutdownable  {
	
	private static final Log LOG = LogFactory.getLog(BTConnectionFetcher.class);
	
	/*
	 * final String we send as a header for bittorrent connections
	 */
	private static final String BITTORRENT_PROTOCOL = "BitTorrent protocol";

	/*
	 * same as above as byte array
	 */
	public static byte[] BITTORRENT_PROTOCOL_BYTES;
	static {
		try {
			BITTORRENT_PROTOCOL_BYTES = BITTORRENT_PROTOCOL
					.getBytes(org.limewire.util.Constants.ASCII_ENCODING);
		} catch (UnsupportedEncodingException e) {
			ErrorService.error(e);
		}
	}
	
	/*
	 * extension bytes
	 */
	private static final byte[] EXTENSION_BYTES = new byte[8];

	/**
	 * Set of connectors that are still establishing transport layer
	 * connections
	 */
	private final StrictIpPortSet<TorrentConnector> connecting =
		new StrictIpPortSet<TorrentConnector>();
	
	/**
	 * Set of handshakers that are negotiating the bt protocol layer
	 */
	private final StrictIpPortSet<BTHandshaker> handshaking =
		new StrictIpPortSet<BTHandshaker>();
	
	/**
	 * the torrent this fetcher belongs to
	 */
	private final ManagedTorrent _torrent;
	
	/**
	 * The fixed outgoing handshake for this torrent.
	 */
	private final ByteBuffer _handshake;
	
	/**
	 * Whether this fetcher is shutdown.
	 */
	private volatile boolean shutdown;
	
	/**
	 * A periodic fetching of connections.
	 */
	private final Periodic scheduled;
	
	/**
	 * The number of connection attempts.
	 */
	private volatile int _triedHosts;
    
    private final SocketsManager socketsManager;
    
    private final BTConnectionFactory btcFactory;
	
	BTConnectionFetcher(ManagedTorrent torrent,
            ScheduledExecutorService scheduler,
            ApplicationServices applicationServices,
            SocketsManager socketsManager,
            BTConnectionFactory btcFactory) {
        this.socketsManager = socketsManager;
        this.btcFactory = btcFactory;
        _torrent = torrent;
        ByteBuffer handshake = ByteBuffer.allocate(68);
		handshake.put((byte) BITTORRENT_PROTOCOL.length()); // 19
		handshake.put(BITTORRENT_PROTOCOL_BYTES); // "BitTorrent protocol"
		handshake.put(EXTENSION_BYTES);
		handshake.put(_torrent.getInfoHash());
		handshake.put(applicationServices.getMyBTGUID());
		handshake.flip();
		// Note: with gathering writes everything but the info hash 
		// can be shared.
		_handshake = handshake.asReadOnlyBuffer();
		scheduled = new Periodic(this,scheduler);
	}
	
	public synchronized void fetch() {
		if (shutdown || !_torrent.needsMoreConnections())
			return;
		long nextRetryTime = _torrent.getNextLocationRetryTime();
		if (nextRetryTime != Long.MAX_VALUE)
			scheduled.rescheduleIfSooner(nextRetryTime);
	}
	
	public void run() {
		fetchImpl();
	}
	
	private void fetchImpl() {
		if (shutdown)
			return;
		
		while (_torrent.needsMoreConnections() &&
				connecting.size() < socketsManager.getNumAllowedSockets() &&
				_torrent.hasNonBusyLocations()) {
			fetchConnection();
		}
		
		// we didn't start enough fetchers - see if there 
		// are any busy hosts we could retry later.
		if (connecting.size() < socketsManager.getNumAllowedSockets())
			fetch();
	}

	/**
	 * Starts a connector thread if possible
	 */
	private void fetchConnection() {

		// get a location to connect to that we know is not currently
		// trying to connect to us.
		TorrentLocation ep = null;
		do {
			ep = _torrent.getTorrentLocation();
			if (ep == null) {
				LOG.debug("no hosts to connect to");
				return;
			}
		} while (connecting.contains(ep) || handshaking.contains(ep));

		TorrentConnector connector = new TorrentConnector(ep);
		connecting.add(connector);
		++_triedHosts;
		try {
			connector.toCancel = socketsManager.connect(new InetSocketAddress(ep.getAddress(), ep.getPort()),
                                                 Constants.TIMEOUT, connector);
		} catch (IOException impossible) {
			connecting.remove(connector); // remove just in case
		}
		
		if (LOG.isDebugEnabled())
			LOG.debug("starting a connector to "+ep.getAddress()+" total "+connecting.size());
	}
	
	public void shutdown() {
	    synchronized(this) {
	        if (shutdown)
	            return;
	        shutdown = true;
	    }
		scheduled.unschedule();

		// copy because shutdown() removes
		List<Shutdownable> conns = new ArrayList<Shutdownable>(connecting.size()+handshaking.size());
		conns.addAll(connecting);
		conns.addAll(handshaking);
		for (Shutdownable connector : conns) 
			connector.shutdown();
	}

	public ByteBuffer getOutgoingHandshake() {
		return _handshake.duplicate();
	}

	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.BTHandshakeObserver#handshakerStarted(com.limegroup.bittorrent.handshaking.IncomingBTHandshaker)
	 */
	public void handshakerStarted(IncomingBTHandshaker shaker) {
		if (LOG.isDebugEnabled())
			LOG.debug("incoming handshaker from "+shaker.getInetAddress()+":"+shaker.getPort());
		
		if (shutdown || handshaking.contains(shaker)) {
			LOG.debug("rejecting it");
			shaker.shutdown();
		} else 
			handshaking.add(shaker);
		
		if (connecting.contains(shaker)) {
			for (TorrentConnector connector : connecting) {
				if (IpPort.COMPARATOR.compare(connector, shaker) == 0) {
					LOG.debug("stopping a connection attempt to same location");
					connector.shutdown();
					return;
				}
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.BTHandshakeObserver#handshakerDone(com.limegroup.bittorrent.handshaking.BTHandshaker)
	 */
	public void handshakerDone(BTHandshaker shaker) {
		assert shutdown || handshaking.contains(shaker) : "unknown shaker "+shaker;
		handshaking.remove(shaker);
	}
	
	/**
	 * @return the number of connection tries
	 */
	public int getTriedHostCount() {
		return _triedHosts;
	}
	
	/**
	 * Connector that establishes the transport layer connection between
	 * two hosts.
	 */
	private class TorrentConnector implements ConnectObserver, IpPort {
		private final TorrentLocation destination;
		private final AtomicBoolean shutdown = new AtomicBoolean(false);
		
		/** a ref to a connecting socket we need to close should we get shutdown */
		volatile Socket toCancel;
		
		TorrentConnector(TorrentLocation destination) {
			this.destination = destination;
		}
		
		public void handleConnect(Socket sock) {
			if (shutdown.get()) 
				return;
			
			if (LOG.isDebugEnabled())
				LOG.debug("established transport to "+sock.getInetAddress());
			
			connecting.remove(this);
			
			if (handshaking.contains(this)) {
				if (LOG.isDebugEnabled())
					LOG.debug("handshaker for this location exists");
				IOUtils.close(sock);
				return;
			}
			
			BTHandshaker shaker = new OutgoingBTHandshaker(destination, _torrent, (AbstractNBSocket)sock,btcFactory);
			handshaking.add(shaker);
			shaker.startHandshaking();
			fetch();
		}

		public void handleIOException(IOException iox) {
			shutdown();
		}

		public void shutdown() {
			if (shutdown.getAndSet(true))
				return;
			IOUtils.close(toCancel);
			connecting.remove(this);
			fetch();
		}
		
		public String getAddress() {
			return destination.getAddress();
		}
		public InetAddress getInetAddress() {
			return destination.getInetAddress();
		}
		public int getPort() {
			return destination.getPort();
		}
        public InetSocketAddress getInetSocketAddress() {
            return destination.getInetSocketAddress();
        }
	}
}
