package com.limegroup.bittorrent;

import java.util.concurrent.ScheduledExecutorService;

import org.limewire.http.reactor.LimeConnectingIOReactorFactory;
import org.limewire.io.NetworkInstanceUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.bittorrent.choking.ChokerFactory;
import com.limegroup.bittorrent.handshaking.BTConnectionFetcherFactory;
import com.limegroup.bittorrent.tracking.TrackerManagerFactory;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.auth.ContentManager;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.util.EventDispatcher;

@Singleton
public class ManagedTorrentFactoryImpl implements ManagedTorrentFactory {
    
    private final EventDispatcher<TorrentEvent, TorrentEventListener> eventDispatcher;
    private final ScheduledExecutorService scheduledExecutorService;
    private final NetworkManager networkManager;
    private final TrackerManagerFactory trackerManagerFactory;
    private final ChokerFactory chokerFactory;
    private final BTLinkManagerFactory linkManagerFactory;
    private final BTConnectionFetcherFactory connectionFetcherFactory;
    private final Provider<ContentManager> contentManager;
    private final Provider<IPFilter> ipFilter;
    private final Provider<TorrentManager> torrentManager;
    private final Provider<FileManager> fileManager;
    private final NetworkInstanceUtils networkInstanceUtils;
    private final LimeConnectingIOReactorFactory limeConnectingIOReactorFactory;

    @Inject
    public ManagedTorrentFactoryImpl(
            EventDispatcher<TorrentEvent, TorrentEventListener> eventDispatcher,
            @Named("nioExecutor") ScheduledExecutorService scheduledExecutorService,
            NetworkManager networkManager,
            TrackerManagerFactory trackerManagerFactory,
            ChokerFactory chokerFactory,
            BTLinkManagerFactory linkManagerFactory,
            BTConnectionFetcherFactory connectionFetcherFactory,
            Provider<ContentManager> contentManager,
            Provider<IPFilter> ipFilter,
            Provider<TorrentManager> torrentManager,
            Provider<FileManager> fileManager,
            NetworkInstanceUtils networkInstanceUtils,
            LimeConnectingIOReactorFactory limeConnectingIOReactorFactory) {
        this.eventDispatcher = eventDispatcher;
        this.scheduledExecutorService = scheduledExecutorService;
        this.networkManager = networkManager;
        this.trackerManagerFactory = trackerManagerFactory;
        this.chokerFactory = chokerFactory;
        this.linkManagerFactory = linkManagerFactory;
        this.connectionFetcherFactory = connectionFetcherFactory;
        this.contentManager = contentManager;
        this.ipFilter = ipFilter;
        this.torrentManager = torrentManager;
        this.fileManager = fileManager;
        this.networkInstanceUtils = networkInstanceUtils;
        this.limeConnectingIOReactorFactory = limeConnectingIOReactorFactory;
    }

    /* (non-Javadoc)
     * @see com.limegroup.bittorrent.ManagedTorrentFactory#create(com.limegroup.bittorrent.TorrentContext)
     */
    public ManagedTorrent createFromContext(TorrentContext context) {        
        return new ManagedTorrentImpl(context, eventDispatcher, scheduledExecutorService,
                networkManager, trackerManagerFactory, chokerFactory, linkManagerFactory,
                connectionFetcherFactory, contentManager.get(), ipFilter.get(), torrentManager.get(),
                fileManager.get(), networkInstanceUtils, limeConnectingIOReactorFactory);
    }


}
