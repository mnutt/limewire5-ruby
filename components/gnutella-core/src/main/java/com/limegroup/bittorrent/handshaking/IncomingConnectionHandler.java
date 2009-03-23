package com.limegroup.bittorrent.handshaking;

import org.limewire.nio.AbstractNBSocket;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.bittorrent.BTConnectionFactory;
import com.limegroup.bittorrent.TorrentManager;

@Singleton
public class IncomingConnectionHandler {
	
    private final BTConnectionFactory factory;
    @Inject
    IncomingConnectionHandler(BTConnectionFactory factory) {
        this.factory = factory;
    }
	public void handleIncoming(AbstractNBSocket s, TorrentManager t) {
		IncomingBTHandshaker shaker = 
			new IncomingBTHandshaker(s, t, factory);
		shaker.startHandshaking();
	}
}
