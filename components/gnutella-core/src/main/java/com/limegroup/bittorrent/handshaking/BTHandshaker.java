package com.limegroup.bittorrent.handshaking;


import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.IpPort;
import org.limewire.nio.AbstractNBSocket;
import org.limewire.nio.channel.ChannelReadObserver;
import org.limewire.nio.channel.ChannelWriter;
import org.limewire.nio.channel.InterestReadableByteChannel;
import org.limewire.nio.channel.InterestScatteringByteChannel;
import org.limewire.nio.channel.InterestWritableByteChannel;

import com.limegroup.bittorrent.BTConnection;
import com.limegroup.bittorrent.BTConnectionFactory;
import com.limegroup.bittorrent.ManagedTorrent;
import com.limegroup.bittorrent.TorrentLocation;

/**
 * Includes both a read and write channels, along with an observer for 
 * BitTorrent connection handshaking.
 * <p>
 * The abstract method <code>startHandshaking</code> should initialize
 * <code>incomingHandshake</code>, while <code>verifyIncoming</code> should 
 * validate the contents of that array.
 */
abstract class BTHandshaker implements  
ChannelWriter, ChannelReadObserver, IpPort {

	private static final Log LOG = LogFactory.getLog(BTHandshaker.class);
	
	
	protected ManagedTorrent torrent;
	protected BTHandshakeObserver observer;
	
	protected ByteBuffer outgoingHandshake;
	protected ByteBuffer [] incomingHandshake;
	protected int currentBufIndex;
	
	protected InterestWritableByteChannel writeChannel;
	protected InterestScatteringByteChannel readChannel;
	
	protected boolean incomingDone, finishingHandshakes;
	protected volatile boolean shutdown;
	
	protected final TorrentLocation loc;
	protected final AbstractNBSocket sock;
    
    private final BTConnectionFactory btcFactory;
	
	protected BTHandshaker(TorrentLocation loc, AbstractNBSocket sock, BTConnectionFactory btcFactory) {
		this.loc = loc;
		this.sock = sock;
        this.btcFactory = btcFactory;
	}
	
	public abstract void startHandshaking();
	
	public void handleRead() throws IOException {
		if (shutdown)
			return;
		
		long read = 0;
		while((read = readChannel.read(incomingHandshake)) > 0 && 
				incomingHandshake[incomingHandshake.length - 1].hasRemaining());
		
		if (read == -1 || !verifyIncoming()) { // bad incoming handshake, drop.
			if (LOG.isDebugEnabled())
				LOG.debug("bad incoming handshake on element "+currentBufIndex+
						" or channel closed "+read);
			shutdown();
			return;
		}
		
		if (!incomingHandshake[incomingHandshake.length - 1].hasRemaining()) { 
			// done with incoming handshake
			if (LOG.isDebugEnabled())
				LOG.debug("incoming handshake finished "+sock.getInetAddress());
			incomingDone = true;
		}
		
		tryToFinishHandshakes();
	}
	
	protected abstract boolean verifyIncoming();
	
	public boolean handleWrite() throws IOException {
		if (shutdown)
			return false;
		
		// write out our handshake
		while (outgoingHandshake.hasRemaining() &&
				writeChannel.write(outgoingHandshake) > 0);
		
		if (!outgoingHandshake.hasRemaining()) 
			writeChannel.interestWrite(this, false);
		
		tryToFinishHandshakes();
		
		return true; //this falls through to SocketAdapter which ignores it.
	}

	protected abstract void initIncomingHandshake();
	
	protected void initOutgoingHandshake() {
		outgoingHandshake = torrent.getFetcher().getOutgoingHandshake();
	}
	
	protected final void setReadInterest() {
		sock.setReadObserver(this);
		readChannel.interestRead(true);
	}
	
	protected final void setWriteInterest() {
		sock.setWriteObserver(this);
		writeChannel.interestWrite(this, true);
	}
	
	private void tryToFinishHandshakes() {
		if (finishingHandshakes || shutdown)
			return;

		if (incomingDone && !outgoingHandshake.hasRemaining()) {
			finishingHandshakes = true;
			
			if (torrent.shouldAddConnection(loc)) {
				BTConnection btc = btcFactory.createBTConnection(torrent.getContext(), 
						loc);

				if (LOG.isDebugEnabled())
					LOG.debug("created connection "
							+ sock.getInetAddress().getHostAddress());
				
				// add the connection and re-schedule fetching.
				if (torrent.addConnection(btc))
					btc.init(sock, torrent, torrent.getNetworkScheduledExecutorService());
				observer.handshakerDone(this);
			}
			else {
				if (LOG.isDebugEnabled())
					LOG.debug("have enough connections, remembering loc "+loc);
				
				torrent.addEndpoint(loc);
				shutdown();
			}
		}
	}
	
	public void handleIOException(IOException iox) {
		shutdown();
	}

	public void shutdown() {
		synchronized(this) {
			if (shutdown)
				return;
			shutdown = true;
		}
		
		if (observer != null)
			observer.handshakerDone(this);
		
		sock.close();
	}

	public void setWriteChannel(InterestWritableByteChannel newChannel) {
		writeChannel = newChannel;
	}

	public InterestWritableByteChannel getWriteChannel() {
		return writeChannel;
	}

	public void setReadChannel(InterestReadableByteChannel newChannel) {
		// if this throws we got problemos.
		readChannel = (InterestScatteringByteChannel) newChannel;
	}

	public InterestReadableByteChannel getReadChannel() {
		return readChannel;
	}

	public String getAddress() {
		return loc.getAddress();
	}

	public InetAddress getInetAddress() {
		return loc.getInetAddress();
	}

	public int getPort() {
		return loc.getPort();
	}
    
    public InetSocketAddress getInetSocketAddress() {
        return loc.getInetSocketAddress();
    }
    
    @Override
    public String toString() {
        return "shaker finishing handshakes "+finishingHandshakes+
        " incoming done "+incomingDone+
        " current buf "+currentBufIndex+
        " shutdown "+shutdown;
    }
}