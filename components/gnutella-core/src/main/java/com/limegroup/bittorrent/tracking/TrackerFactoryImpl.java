package com.limegroup.bittorrent.tracking;

import java.net.URI;

import org.limewire.http.httpclient.LimeHttpClient;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.bittorrent.ManagedTorrent;
import com.limegroup.bittorrent.TorrentContext;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.NetworkManager;

@Singleton
public class TrackerFactoryImpl implements TrackerFactory {

    private final NetworkManager networkManager;
    private final ApplicationServices applicationServices;
    private final Provider<LimeHttpClient> httpClientProvider;

    @Inject
    public TrackerFactoryImpl(NetworkManager networkManager,
            ApplicationServices applicationServices,
            Provider<LimeHttpClient> httpClientProvider) {
        this.networkManager = networkManager;
        this.applicationServices = applicationServices;
        this.httpClientProvider = httpClientProvider;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.bittorrent.tracking.TrackerFactory#create(org.apache.commons.httpclient.URI,
     *      com.limegroup.bittorrent.TorrentContext,
     *      com.limegroup.bittorrent.ManagedTorrent)
     */
    public Tracker create(URI uri, TorrentContext context,
            ManagedTorrent torrent) {
        return new TrackerImpl(uri, context, torrent, networkManager, applicationServices, httpClientProvider);
    }

}
