package com.limegroup.bittorrent.handshaking;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.nio.AbstractNBSocket;

import com.limegroup.bittorrent.BTConnectionFactory;
import com.limegroup.bittorrent.TorrentLocation;
import com.limegroup.bittorrent.TorrentManager;

class IncomingBTHandshaker extends BTHandshaker {

	private static final Log LOG = LogFactory.getLog(IncomingBTHandshaker.class);
	
	private static final byte[] PROTOCOL = new byte[] { 'p', 'r', 'o', 't',
		'o', 'c', 'o', 'l' };
	
	private final TorrentManager manager;
	
	public IncomingBTHandshaker(AbstractNBSocket sock, TorrentManager manager, BTConnectionFactory factory) {
		super(new TorrentLocation((InetSocketAddress)sock.getRemoteSocketAddress(), 
				new byte[20],
				new byte[8]), sock, factory);
		this.manager = manager;
	}
	
	@Override
    public void startHandshaking() {
		initIncomingHandshake();
		setReadInterest();
	}
	
	@Override
    protected boolean verifyIncoming() {
		for(; 
		currentBufIndex < incomingHandshake.length &&
		!incomingHandshake[currentBufIndex].hasRemaining();
		currentBufIndex++) {
			ByteBuffer current = incomingHandshake[currentBufIndex];
			switch(currentBufIndex) {
			case 0 : // 'protocol'
				if (!Arrays.equals(current.array(), PROTOCOL))
					return false;
				break;
			case 1 : // extention bytes
				break;
			case 2 : // infoHash
				torrent = 
					manager.getTorrentForHash(current.array());
				if (torrent == null) {
					if (LOG.isDebugEnabled())
						LOG.debug("incoming connection for unknown torrent");
					return false;
				}
				else if (!torrent.shouldAddConnection(loc))
					return false;
				else {
					observer = torrent.getFetcher();
					initOutgoingHandshake();
					setWriteInterest();
					observer.handshakerStarted(this);
				}
				break;
			case 3 : // peerId
				break;
			}
		}
		
		return true;
	}

	@Override
    protected void initIncomingHandshake() {
		incomingHandshake = new ByteBuffer[4];
		incomingHandshake[0] = ByteBuffer.allocate(8); // protocol bytes
		incomingHandshake[1] = ByteBuffer.wrap(loc.getExtBytes());
		incomingHandshake[2] = ByteBuffer.allocate(20); // infohash
		incomingHandshake[3] = ByteBuffer.wrap(loc.getPeerID());
	}

}
