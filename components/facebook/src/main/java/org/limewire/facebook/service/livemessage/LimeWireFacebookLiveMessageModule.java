package org.limewire.facebook.service.livemessage;

import org.limewire.concurrent.ScheduledListeningExecutorService;
import org.limewire.concurrent.SimpleTimer;
import org.limewire.facebook.service.Facebook;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.assistedinject.FactoryProvider;

public class LimeWireFacebookLiveMessageModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(AddressHandlerFactory.class).toProvider(FactoryProvider.newFactory(AddressHandlerFactory.class, AddressHandler.class));
        bind(AuthTokenHandlerFactory.class).toProvider(FactoryProvider.newFactory(AuthTokenHandlerFactory.class, AuthTokenHandler.class));
        bind(ConnectBackRequestHandlerFactory.class).toProvider(FactoryProvider.newFactory(ConnectBackRequestHandlerFactory.class, ConnectBackRequestHandler.class));
        bind(DiscoInfoHandlerFactory.class).toProvider(FactoryProvider.newFactory(DiscoInfoHandlerFactory.class, DiscoInfoHandler.class));
        bind(FileOfferHandlerFactory.class).toProvider(FactoryProvider.newFactory(FileOfferHandlerFactory.class, FileOfferHandler.class));
        bind(LibraryRefreshHandlerFactory.class).toProvider(FactoryProvider.newFactory(LibraryRefreshHandlerFactory.class, LibraryRefreshHandler.class));
        bind(LiveMessageHandlerRegistry.class).to(LiveMessageHandlerRegistryImpl.class);
        bind(PresenceHandlerFactory.class).toProvider(FactoryProvider.newFactory(PresenceHandlerFactory.class, PresenceHandler.class));
    }

    @Provides @Facebook ScheduledListeningExecutorService provideFacebookExecutor() {
        return new SimpleTimer(true);
    }
}
