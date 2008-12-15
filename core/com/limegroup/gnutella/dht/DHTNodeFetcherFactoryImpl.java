package com.limegroup.gnutella.dht;

import java.util.concurrent.ScheduledExecutorService;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.HostCatcher;
import com.limegroup.gnutella.UDPPinger;
import com.limegroup.gnutella.messages.PingRequestFactory;

@Singleton
public class DHTNodeFetcherFactoryImpl implements DHTNodeFetcherFactory {

    private final ConnectionServices connectionServices;
    private final ScheduledExecutorService backgroundExecutor;
    private final Provider<HostCatcher> hostCatcher;
    private final Provider<UDPPinger> udpPingerFactory;
    private final PingRequestFactory pingRequestFactory;
    
    @Inject
    public DHTNodeFetcherFactoryImpl(ConnectionServices connectionServices,
            @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
            Provider<HostCatcher> hostCatcher,
            Provider<UDPPinger> udpPingerFactory,
            PingRequestFactory pingRequestFactory) {
        this.connectionServices = connectionServices;
        this.backgroundExecutor = backgroundExecutor;
        this.hostCatcher = hostCatcher;
        this.udpPingerFactory = udpPingerFactory;
        this.pingRequestFactory = pingRequestFactory;
    }

    public DHTNodeFetcher createNodeFetcher(DHTBootstrapper dhtBootstrapper) {
        return new DHTNodeFetcher(dhtBootstrapper, connectionServices,
                hostCatcher, backgroundExecutor, udpPingerFactory,
                pingRequestFactory);
    }

}
