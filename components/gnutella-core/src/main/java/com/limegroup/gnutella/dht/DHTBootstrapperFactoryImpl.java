package com.limegroup.gnutella.dht;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.simpp.SimppManager;

@Singleton
public class DHTBootstrapperFactoryImpl implements DHTBootstrapperFactory {
    
    private final Provider<SimppManager> simppManager;
    private final DHTNodeFetcherFactory dhtNodeFetcherFactory;
    
    @Inject
    public DHTBootstrapperFactoryImpl(Provider<SimppManager> simppManager,
            DHTNodeFetcherFactory dhtNodeFetcherFactory) {
        this.simppManager = simppManager;
        this.dhtNodeFetcherFactory = dhtNodeFetcherFactory;
    }

    public DHTBootstrapper createBootstrapper(DHTController dhtController) {
        return new DHTBootstrapperImpl(dhtController, simppManager, dhtNodeFetcherFactory);
    }

}
