package com.limegroup.bittorrent.handshaking;

import java.util.concurrent.ScheduledExecutorService;

import org.limewire.net.SocketsManager;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.bittorrent.BTConnectionFactory;
import com.limegroup.bittorrent.ManagedTorrent;
import com.limegroup.gnutella.ApplicationServices;

@Singleton
public class BTConnectionFetcherFactoryImpl implements BTConnectionFetcherFactory {
    
    private final ScheduledExecutorService scheduledExecutorService;
    private final ApplicationServices applicationServices;
    private final SocketsManager socketsManager;
    private final BTConnectionFactory btcFactory;
        
	@Inject
	public BTConnectionFetcherFactoryImpl(
            @Named("nioExecutor") ScheduledExecutorService scheduledExecutorService,
            ApplicationServices applicationServices,
            SocketsManager socketsManager,
            BTConnectionFactory btcFactory) {
        this.scheduledExecutorService = scheduledExecutorService;
        this.applicationServices = applicationServices;
        this.socketsManager = socketsManager;
        this.btcFactory = btcFactory;
    }


    /* (non-Javadoc)
     * @see com.limegroup.bittorrent.handshaking.BTConnectionFetcherFactory#getBTConnectionFetcher(com.limegroup.bittorrent.ManagedTorrent)
     */
    public BTConnectionFetcher getBTConnectionFetcher(ManagedTorrent torrent) {
		return new BTConnectionFetcher(torrent, scheduledExecutorService, 
                applicationServices, socketsManager, btcFactory);
	}
}
