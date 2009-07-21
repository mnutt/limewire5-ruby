package org.limewire.core.impl;

import java.lang.annotation.Annotation;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import org.limewire.core.api.Application;
import org.limewire.core.api.callback.GuiCallbackService;
import org.limewire.core.api.connection.FirewallStatusEvent;
import org.limewire.core.api.connection.FirewallTransferStatusEvent;
import org.limewire.core.api.lifecycle.MockLifeCycleModule;
import org.limewire.core.api.magnet.MockMagnetModule;
import org.limewire.core.impl.bittorrent.MockBittorrentModule;
import org.limewire.core.impl.browse.MockBrowseModule;
import org.limewire.core.impl.callback.MockGuiCallbackService;
import org.limewire.core.impl.connection.MockConnectionModule;
import org.limewire.core.impl.daap.MockDaapModule;
import org.limewire.core.impl.download.MockDownloadModule;
import org.limewire.core.impl.friend.MockFriendModule;
import org.limewire.core.impl.library.MockLibraryModule;
import org.limewire.core.impl.mojito.MockMojitoModule;
import org.limewire.core.impl.monitor.MockMonitorModule;
import org.limewire.core.impl.network.MockNetworkModule;
import org.limewire.core.impl.player.MockPlayerModule;
import org.limewire.core.impl.playlist.MockPlaylistModule;
import org.limewire.core.impl.properties.MockPropertyModule;
import org.limewire.core.impl.search.MockSearchModule;
import org.limewire.core.impl.search.browse.MockBrowseSearchModule;
import org.limewire.core.impl.spam.MockSpamModule;
import org.limewire.core.impl.support.MockSupportModule;
import org.limewire.core.impl.updates.MockUpdatesModule;
import org.limewire.core.impl.upload.MockUploadModule;
import org.limewire.core.impl.xmpp.MockXmppModule;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.listener.BroadcastPolicy;
import org.limewire.listener.CachingEventMulticasterImpl;
import org.limewire.listener.EventBean;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventMulticaster;
import org.limewire.listener.EventMulticasterImpl;
import org.limewire.listener.ListenerSupport;
import org.limewire.net.MockNetModule;
import org.limewire.xmpp.activity.XmppActivityEvent;
import org.limewire.concurrent.ScheduledListeningExecutorService;
import org.limewire.concurrent.AbstractLazySingletonProvider;
import org.limewire.concurrent.SimpleTimer;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.Key;
import com.google.inject.Singleton;
import com.google.inject.name.Names;

public class MockModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(Application.class).to(MockApplication.class);
        bind(GuiCallbackService.class).to(MockGuiCallbackService.class);
        bind(ServiceRegistry.class).to(MockServiceRegistry.class);

        install(new MockLifeCycleModule());
        install(new MockConnectionModule());
        install(new MockDaapModule());
        install(new MockSpamModule());
        install(new MockSearchModule());
        install(new MockBrowseSearchModule());
        install(new MockNetworkModule());
        install(new MockDownloadModule());
        install(new MockFriendModule());
        install(new MockLibraryModule());
        install(new MockMojitoModule());
        install(new MockMonitorModule());
        install(new MockBrowseModule());
        install(new MockPlayerModule());
        install(new MockPlaylistModule());
        install(new MockPropertyModule());
        install(new MockXmppModule());
        install(new MockSupportModule());
        install(new MockMagnetModule());
        install(new MockNetModule());
        install(new MockUploadModule());
        install(new MockUpdatesModule());
        install(new MockBittorrentModule());
                       
        EventMulticaster<XmppActivityEvent> activityMulticaster = new EventMulticasterImpl<XmppActivityEvent>(); 
        bind(new TypeLiteral<EventBroadcaster<XmppActivityEvent>>(){}).toInstance(activityMulticaster);
        bind(new TypeLiteral<ListenerSupport<XmppActivityEvent>>(){}).toInstance(activityMulticaster);        
        
        CachingEventMulticasterImpl<FirewallTransferStatusEvent> fwtStatusMulticaster = new CachingEventMulticasterImpl<FirewallTransferStatusEvent>(BroadcastPolicy.IF_NOT_EQUALS);
        bind(new TypeLiteral<EventBean<FirewallTransferStatusEvent>>(){}).toInstance(fwtStatusMulticaster);
        bind(new TypeLiteral<EventBroadcaster<FirewallTransferStatusEvent>>(){}).toInstance(fwtStatusMulticaster);
        bind(new TypeLiteral<ListenerSupport<FirewallTransferStatusEvent>>(){}).toInstance(fwtStatusMulticaster);       
        
        CachingEventMulticasterImpl<FirewallStatusEvent> firewalledStatusMulticaster = new CachingEventMulticasterImpl<FirewallStatusEvent>(BroadcastPolicy.IF_NOT_EQUALS);
        bind(new TypeLiteral<EventBean<FirewallStatusEvent>>(){}).toInstance(firewalledStatusMulticaster);
        bind(new TypeLiteral<EventBroadcaster<FirewallStatusEvent>>(){}).toInstance(firewalledStatusMulticaster);
        bind(new TypeLiteral<ListenerSupport<FirewallStatusEvent>>(){}).toInstance(firewalledStatusMulticaster);

        Annotation execAnn = Names.named("backgroundExecutor");
        Key<ScheduledListeningExecutorService> mainKey =
                Key.get(ScheduledListeningExecutorService.class, execAnn);
        bind(mainKey).toProvider(BackgroundTimerProvider.class);
        bind(ScheduledExecutorService.class).annotatedWith(execAnn).to(mainKey);
        bind(Executor.class).annotatedWith(execAnn).to(mainKey);
        bind(ExecutorService.class).annotatedWith(execAnn).to(mainKey);
        
    }

    @Singleton
    private static class BackgroundTimerProvider extends AbstractLazySingletonProvider<ScheduledListeningExecutorService> {
        @Override
        protected ScheduledListeningExecutorService createObject() {
            return new SimpleTimer(true);
        }
    }
}
