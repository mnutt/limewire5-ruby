package com.limegroup.gnutella;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.limewire.common.LimeWireCommonModule;
import org.limewire.concurrent.AbstractLazySingletonProvider;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.concurrent.ListeningExecutorService;
import org.limewire.concurrent.ScheduledListeningExecutorService;
import org.limewire.concurrent.SimpleTimer;
import org.limewire.core.api.connection.FirewallStatusEvent;
import org.limewire.core.api.connection.FirewallTransferStatusEvent;
import org.limewire.core.api.download.SaveLocationManager;
import org.limewire.core.api.search.SearchManager;
import org.limewire.core.impl.search.SearchManagerImpl;
import org.limewire.core.settings.LimeWireCoreSettingsModule;
import org.limewire.geocode.LimewireGeocodeModule;
import org.limewire.http.LimeWireHttpModule;
import org.limewire.http.mongrel.MongrelManager;
import org.limewire.http.mongrel.MongrelManagerImpl;
import org.limewire.http.mongrel.MongrelGlue;
import org.limewire.http.webservice.WebServiceManager;
import org.limewire.http.webservice.WebServiceManagerImpl;
import org.limewire.http.webservice.WebServiceGlue;
import org.limewire.inject.AbstractModule;
import org.limewire.inspection.Inspector;
import org.limewire.inspection.InspectorImpl;
import org.limewire.io.LimeWireIOModule;
import org.limewire.io.LocalSocketAddressProvider;
import org.limewire.listener.AsynchronousCachingEventMulticasterImpl;
import org.limewire.listener.AsynchronousEventBroadcaster;
import org.limewire.listener.BroadcastPolicy;
import org.limewire.listener.EventBean;
import org.limewire.listener.ListenerSupport;
import org.limewire.mojito.LimeWireMojitoModule;
import org.limewire.mojito.io.MessageDispatcherFactory;
import org.limewire.net.ConnectionDispatcher;
import org.limewire.net.ConnectionDispatcherImpl;
import org.limewire.net.ExternalIP;
import org.limewire.net.LimeWireNetModule;
import org.limewire.net.TLSManager;
import org.limewire.net.address.AddressEvent;
import org.limewire.net.address.FirewalledAddressSerializer;
import org.limewire.nio.ByteBufferCache;
import org.limewire.nio.NIODispatcher;
import org.limewire.promotion.LimeWirePromotionModule;
import org.limewire.security.SecureMessageVerifier;
import org.limewire.security.SecureMessageVerifierImpl;
import org.limewire.security.SecurityToken;
import org.limewire.security.SettingsProvider;
import org.limewire.security.certificate.LimeWireSecurityCertificateModule;
import org.limewire.statistic.LimeWireStatisticsModule;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.limegroup.bittorrent.BTUploaderFactory;
import com.limegroup.bittorrent.BTUploaderFactoryImpl;
import com.limegroup.bittorrent.LimeWireBittorrentModule;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;
import com.limegroup.gnutella.altlocs.AlternateLocationFactoryImpl;
import com.limegroup.gnutella.auth.IpPortContentAuthorityFactory;
import com.limegroup.gnutella.auth.IpPortContentAuthorityFactoryImpl;
import com.limegroup.gnutella.auth.LimeWireContentAuthModule;
import com.limegroup.gnutella.bootstrap.Bootstrapper;
import com.limegroup.gnutella.bootstrap.LimeWireBootstrapModule;
import com.limegroup.gnutella.browser.LocalAcceptor;
import com.limegroup.gnutella.connection.LimeWireCoreConnectionModule;
import com.limegroup.gnutella.daap.DaapManager;
import com.limegroup.gnutella.dht.DHTBootstrapperFactory;
import com.limegroup.gnutella.dht.DHTBootstrapperFactoryImpl;
import com.limegroup.gnutella.dht.DHTControllerFacade;
import com.limegroup.gnutella.dht.DHTControllerFacadeImpl;
import com.limegroup.gnutella.dht.DHTControllerFactory;
import com.limegroup.gnutella.dht.DHTControllerFactoryImpl;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.DHTManagerImpl;
import com.limegroup.gnutella.dht.DHTNodeFetcherFactory;
import com.limegroup.gnutella.dht.DHTNodeFetcherFactoryImpl;
import com.limegroup.gnutella.dht.LimeWireDHTModule;
import com.limegroup.gnutella.dht.db.AltLocValueFactory;
import com.limegroup.gnutella.dht.db.AltLocValueFactoryImpl;
import com.limegroup.gnutella.dht.db.PushEndpointService;
import com.limegroup.gnutella.dht.db.PushProxiesValueFactory;
import com.limegroup.gnutella.dht.db.PushProxiesValueFactoryImpl;
import com.limegroup.gnutella.dht.io.LimeMessageDispatcherFactoryImpl;
import com.limegroup.gnutella.downloader.LWSIntegrationServicesDelegate;
import com.limegroup.gnutella.downloader.LimeWireDownloadModule;
import com.limegroup.gnutella.downloader.serial.conversion.DownloadUpgradeTaskService;
import com.limegroup.gnutella.filters.LimeWireFiltersModule;
import com.limegroup.gnutella.handshaking.HandshakeResponderFactory;
import com.limegroup.gnutella.handshaking.HandshakeResponderFactoryImpl;
import com.limegroup.gnutella.handshaking.HandshakeServices;
import com.limegroup.gnutella.handshaking.HandshakeServicesImpl;
import com.limegroup.gnutella.handshaking.HeadersFactory;
import com.limegroup.gnutella.handshaking.HeadersFactoryImpl;
import com.limegroup.gnutella.http.DefaultHttpExecutor;
import com.limegroup.gnutella.http.HttpExecutor;
import com.limegroup.gnutella.library.LimeWireLibraryModule;
import com.limegroup.gnutella.library.RareFileStrategy;
import com.limegroup.gnutella.library.RareFileStrategyImpl;
import com.limegroup.gnutella.licenses.LicenseFactory;
import com.limegroup.gnutella.licenses.LicenseFactoryImpl;
import com.limegroup.gnutella.lws.server.LWSManager;
import com.limegroup.gnutella.lws.server.LWSManagerImpl;
import com.limegroup.gnutella.malware.LimeWireMalwareModule;
import com.limegroup.gnutella.messagehandlers.MessageHandlerBinderImpl;
import com.limegroup.gnutella.messages.LocalPongInfo;
import com.limegroup.gnutella.messages.LocalPongInfoImpl;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.MessageFactoryImpl;
import com.limegroup.gnutella.messages.MessageParserBinder;
import com.limegroup.gnutella.messages.MessageParserBinderImpl;
import com.limegroup.gnutella.messages.OutgoingQueryReplyFactory;
import com.limegroup.gnutella.messages.OutgoingQueryReplyFactoryImpl;
import com.limegroup.gnutella.messages.PingReplyFactory;
import com.limegroup.gnutella.messages.PingReplyFactoryImpl;
import com.limegroup.gnutella.messages.PingRequestFactory;
import com.limegroup.gnutella.messages.PingRequestFactoryImpl;
import com.limegroup.gnutella.messages.QueryReplyFactory;
import com.limegroup.gnutella.messages.QueryReplyFactoryImpl;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.messages.QueryRequestFactoryImpl;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactory;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactoryImpl;
import com.limegroup.gnutella.messages.vendor.HeadPongFactory;
import com.limegroup.gnutella.messages.vendor.HeadPongFactoryImpl;
import com.limegroup.gnutella.messages.vendor.InspectionResponseFactory;
import com.limegroup.gnutella.messages.vendor.InspectionResponseFactoryImpl;
import com.limegroup.gnutella.messages.vendor.ReplyNumberVendorMessageFactory;
import com.limegroup.gnutella.messages.vendor.ReplyNumberVendorMessageFactoryImpl;
import com.limegroup.gnutella.messages.vendor.UDPCrawlerPongFactory;
import com.limegroup.gnutella.messages.vendor.UDPCrawlerPongFactoryImpl;
import com.limegroup.gnutella.messages.vendor.VendorMessageFactory;
import com.limegroup.gnutella.messages.vendor.VendorMessageFactoryImpl;
import com.limegroup.gnutella.messages.vendor.VendorMessageParserBinder;
import com.limegroup.gnutella.messages.vendor.VendorMessageParserBinderImpl;
import com.limegroup.gnutella.metadata.MetaDataFactory;
import com.limegroup.gnutella.metadata.MetaDataFactoryImpl;
import com.limegroup.gnutella.net.address.ConnectableConnector;
import com.limegroup.gnutella.net.address.SameNATAddressResolver;
import com.limegroup.gnutella.routing.QRPUpdater;
import com.limegroup.gnutella.rudp.LimeWireGnutellaRudpModule;
import com.limegroup.gnutella.search.LimeWireSearchModule;
import com.limegroup.gnutella.search.QueryDispatcher;
import com.limegroup.gnutella.search.QueryDispatcherImpl;
import com.limegroup.gnutella.search.QueryHandlerFactory;
import com.limegroup.gnutella.search.QueryHandlerFactoryImpl;
import com.limegroup.gnutella.settings.LimeWireSettingsModule;
import com.limegroup.gnutella.settings.SettingsBackedProxySettings;
import com.limegroup.gnutella.settings.SettingsBackedSocketBindingSettings;
import com.limegroup.gnutella.simpp.LimeWireSimppModule;
import com.limegroup.gnutella.spam.RatingTable;
import com.limegroup.gnutella.statistics.LimeWireGnutellaStatisticsModule;
import com.limegroup.gnutella.tigertree.LimeWireHashTreeModule;
import com.limegroup.gnutella.uploader.FileResponseEntityFactory;
import com.limegroup.gnutella.uploader.FileResponseEntityFactoryImpl;
import com.limegroup.gnutella.uploader.HTTPUploadSessionManager;
import com.limegroup.gnutella.uploader.HttpRequestHandlerFactory;
import com.limegroup.gnutella.uploader.HttpRequestHandlerFactoryImpl;
import com.limegroup.gnutella.uploader.LimeWireUploaderModule;
import com.limegroup.gnutella.uploader.UploadSlotManager;
import com.limegroup.gnutella.uploader.UploadSlotManagerImpl;
import com.limegroup.gnutella.util.FECUtils;
import com.limegroup.gnutella.util.FECUtilsImpl;
import com.limegroup.gnutella.version.UpdateCollectionFactory;
import com.limegroup.gnutella.version.UpdateCollectionFactoryImpl;
import com.limegroup.gnutella.version.UpdateHandler;
import com.limegroup.gnutella.version.UpdateHandlerImpl;
import com.limegroup.gnutella.version.UpdateMessageVerifier;
import com.limegroup.gnutella.version.UpdateMessageVerifierImpl;
import com.limegroup.gnutella.xml.LimeWireXmlModule;


/**
 * The module that defines what implementations are used within
 * LimeWire's core.  This class can be constructed with or without
 * an ActivitiyCallback class.  If it is without, then another module
 * must explicitly identify which class is going to define the
 * ActivityCallback.
 */
public class LimeWireCoreModule extends AbstractModule {
 
	private final Class<? extends ActivityCallback> activityCallbackClass;
    
    public LimeWireCoreModule() {
        this(null);
    }
    
    public LimeWireCoreModule(Class<? extends ActivityCallback> activityCallbackClass) {
        this.activityCallbackClass = activityCallbackClass;
    }
    
    @Override
    protected void configure() {
        binder().install(new LimeWireCommonModule());
        binder().install(new LimeWireCoreSettingsModule());
        binder().install(new LimeWireSettingsModule());
        binder().install(new LimeWireNetModule(SettingsBackedProxySettings.class, SettingsBackedSocketBindingSettings.class));
        binder().install(new LimeWireDownloadModule());
        binder().install(new LimeWireHashTreeModule());        
        binder().install(new LimeWireDHTModule());
        
        binder().install(new LimeWireHttpModule());
        binder().install(new LimeWireSearchModule());
        binder().install(new LimeWireStatisticsModule());
        binder().install(new LimeWireGnutellaStatisticsModule());
        binder().install(new LimeWireGnutellaRudpModule());
        binder().install(new LimeWireIOModule());
        binder().install(new LimeWireMojitoModule());
        binder().install(new LimeWireSecurityCertificateModule());
        binder().install(new LimewireGeocodeModule());        
        binder().install(new LimeWirePromotionModule(PromotionBinderRequestorImpl.class, PromotionServicesImpl.class));        
        binder().install(new LimeWireSimppModule());
        binder().install(new LimeWireBittorrentModule());
        binder().install(new LimeWireLibraryModule());
        binder().install(new LimeWireUploaderModule());
        binder().install(new LimeWireContentAuthModule());
        binder().install(new LimeWireFiltersModule());
        binder().install(new LimeWireCoreConnectionModule());
        binder().install(new LimeWireBootstrapModule());
        binder().install(new LimeWireMalwareModule());
        binder().install(new LimeWireXmlModule());
        
        if(activityCallbackClass != null) {
            bind(ActivityCallback.class).to(activityCallbackClass);
        }        

        bind(DownloadCallback.class).to(ActivityCallback.class);
        bind(NetworkManager.class).to(NetworkManagerImpl.class);
        bind(TLSManager.class).to(NetworkManagerImpl.class);
        bind(new TypeLiteral<ListenerSupport<AddressEvent>>(){}).to(NetworkManagerImpl.class);
        bind(DHTManager.class).to(DHTManagerImpl.class);
        bind(DHTControllerFactory.class).to(DHTControllerFactoryImpl.class);
        bind(PingReplyFactory.class).to(PingReplyFactoryImpl.class);
        bind(PushProxiesValueFactory.class).to(PushProxiesValueFactoryImpl.class);
        bind(HandshakeResponderFactory.class).to(HandshakeResponderFactoryImpl.class);
        bind(HeadersFactory.class).to(HeadersFactoryImpl.class);
        bind(PushEndpointFactory.class).to(PushEndpointFactoryImpl.class);
        bind(HeadPongFactory.class).to(HeadPongFactoryImpl.class);
        bind(UploadManager.class).to(HTTPUploadManager.class);
        bind(HTTPUploadSessionManager.class).to(HTTPUploadManager.class);
        bind(QueryHandlerFactory.class).to(QueryHandlerFactoryImpl.class);
        bind(QueryRequestFactory.class).to(QueryRequestFactoryImpl.class);
        bind(AltLocValueFactory.class).to(AltLocValueFactoryImpl.class);
        bind(AlternateLocationFactory.class).to(AlternateLocationFactoryImpl.class);
        bind(HttpExecutor.class).to(DefaultHttpExecutor.class);
        bind(HttpRequestHandlerFactory.class).to(HttpRequestHandlerFactoryImpl.class);
        bind(ResponseFactory.class).to(ResponseFactoryImpl.class);
        bind(QueryReplyFactory.class).to(QueryReplyFactoryImpl.class);
        bind(MessageDispatcherFactory.class).to(LimeMessageDispatcherFactoryImpl.class);
        bind(CapabilitiesVMFactory.class).to(CapabilitiesVMFactoryImpl.class);
        bind(LifecycleManager.class).to(LifecycleManagerImpl.class);
        bind(LocalPongInfo.class).to(LocalPongInfoImpl.class);
        bind(MongrelManager.class).to(MongrelManagerImpl.class);
        bind(WebServiceManager.class).to(WebServiceManagerImpl.class);
        bind(ConnectionServices.class).to(ConnectionServicesImpl.class);
        bind(SearchServices.class).to(SearchServicesImpl.class);
        bind(SearchManager.class).to(SearchManagerImpl.class);
        bind(DownloadServices.class).to(DownloadServicesImpl.class);
        bind(UploadServices.class).to(UploadServicesImpl.class);
        bind(ApplicationServices.class).to(ApplicationServicesImpl.class);
        bind(SpamServices.class).to(SpamServicesImpl.class);
        bind(DHTControllerFacade.class).to(DHTControllerFacadeImpl.class);
        bind(DHTBootstrapperFactory.class).to(DHTBootstrapperFactoryImpl.class);
        bind(DHTNodeFetcherFactory.class).to(DHTNodeFetcherFactoryImpl.class);
        bind(UDPReplyHandlerFactory.class).to(UDPReplyHandlerFactoryImpl.class);
        bind(UDPReplyHandlerCache.class).to(UDPReplyHandlerCacheImpl.class);
        bind(SocketProcessor.class).to(Acceptor.class);
        bind(DownloadManager.class).to(DownloadManagerImpl.class).asEagerSingleton();
        bind(BrowseHostHandlerManagerImpl.class).asEagerSingleton();
        bind(ReplyNumberVendorMessageFactory.class).to(ReplyNumberVendorMessageFactoryImpl.class);
        bind(GuidMapManager.class).to(GuidMapManagerImpl.class);
        bind(BrowseHostHandlerManager.class).to(BrowseHostHandlerManagerImpl.class);
        bind(PushEndpointCache.class).to(PushEndpointCacheImpl.class);
        bind(PushEndpointService.class).annotatedWith(Names.named("pushEndpointCache")).to(PushEndpointCacheImpl.class);
        bind(FileResponseEntityFactory.class).to(FileResponseEntityFactoryImpl.class);
        bind(MessageFactory.class).to(MessageFactoryImpl.class);
        bind(MessageParserBinder.class).to(MessageParserBinderImpl.class);
        bind(VendorMessageFactory.class).to(VendorMessageFactoryImpl.class);
        bind(VendorMessageParserBinder.class).to(VendorMessageParserBinderImpl.class);
        bind(UDPCrawlerPongFactory.class).to(UDPCrawlerPongFactoryImpl.class);
        bind(LicenseFactory.class).to(LicenseFactoryImpl.class);
        bind(MetaDataFactory.class).to(MetaDataFactoryImpl.class);
        bind(SaveLocationManager.class).to(DownloadManager.class);
        bind(BTUploaderFactory.class).to(BTUploaderFactoryImpl.class);
        bind(PingRequestFactory.class).to(PingRequestFactoryImpl.class);
        bind(IpPortContentAuthorityFactory.class).to(IpPortContentAuthorityFactoryImpl.class);
        bind(UpdateCollectionFactory.class).to(UpdateCollectionFactoryImpl.class);
        bind(ConnectionDispatcher.class).annotatedWith(Names.named("global")).to(ConnectionDispatcherImpl.class).in(Scopes.SINGLETON);
        bind(ConnectionDispatcher.class).annotatedWith(Names.named("local")).to(ConnectionDispatcherImpl.class).in(Scopes.SINGLETON);
        bind(UDPPinger.class).to(UDPPingerImpl.class);
        bind(Inspector.class).to(InspectorImpl.class);
        bind(LWSManager.class).to(LWSManagerImpl.class);
        bind(LWSIntegrationServicesDelegate.class).to(DownloadManager.class);
        bind(LocalSocketAddressProvider.class).to(LocalSocketAddressProviderImpl.class);
        bind(SettingsProvider.class).to(MacCalculatorSettingsProviderImpl.class);
        bind(ReplyHandler.class).annotatedWith(Names.named("forMeReplyHandler")).to(ForMeReplyHandler.class);
        bind(MessageRouter.class).to(StandardMessageRouter.class);
        bind(UploadSlotManager.class).to(UploadSlotManagerImpl.class);
        bind(BandwidthManager.class).to(BandwidthManagerImpl.class);
        bind(SecureMessageVerifier.class).toProvider(SecureMessageVerifierProvider.class);
        bind(SecureMessageVerifier.class).annotatedWith(Names.named("inspection")).toProvider(InspectionVerifierProvider.class);
        bind(PongCacher.class).to(PongCacherImpl.class);        
        bind(BandwidthTracker.class).annotatedWith(Names.named("uploadTracker")).to(UploadManager.class);     // For NodeAssigner.
        bind(BandwidthTracker.class).annotatedWith(Names.named("downloadTracker")).to(DownloadManager.class); // For NodeAssigner.
        bind(NIODispatcher.class).toProvider(NIODispatcherProvider.class);
        bind(ByteBufferCache.class).toProvider(ByteBufferCacheProvider.class);
        bind(ResponseVerifier.class).to(ResponseVerifierImpl.class);
        bind(HandshakeServices.class).to(HandshakeServicesImpl.class);
        bind(ConnectionManager.class).to(ConnectionManagerImpl.class);
        bind(MessageHandlerBinder.class).to(MessageHandlerBinderImpl.class);
        bind(QueryDispatcher.class).to(QueryDispatcherImpl.class);
        bind(Acceptor.class).to(AcceptorImpl.class);        
        bind(UpdateHandler.class).to(UpdateHandlerImpl.class);
        bind(SecurityToken.TokenProvider.class).to(SecurityToken.AddressSecurityTokenProvider.class);
        bind(UpdateMessageVerifier.class).to(UpdateMessageVerifierImpl.class);
        bind(InspectionResponseFactory.class).to(InspectionResponseFactoryImpl.class);
        bind(FECUtils.class).to(FECUtilsImpl.class);
        bind(NodeAssigner.class).to(NodeAssignerImpl.class);
        bind(OutgoingQueryReplyFactory.class).to(OutgoingQueryReplyFactoryImpl.class);
        bind(UPnPManagerConfiguration.class).to(UPnPManagerConfigurationImpl.class);
        bind(Bootstrapper.Listener.class).to(HostCatcher.class);
        bind(RareFileStrategy.class).to(RareFileStrategyImpl.class);
        bind(MulticastService.class).to(MulticastServiceImpl.class);
        bind(NetworkUpdateSanityChecker.class).to(NetworkUpdateSanityCheckerImpl.class);
        
        bindAll(Names.named("fastExecutor"), ScheduledExecutorService.class, FastExecutorProvider.class, ExecutorService.class, Executor.class);
        bindAll(Names.named("unlimitedExecutor"), ListeningExecutorService.class, UnlimitedExecutorProvider.class, Executor.class, ExecutorService.class);
        bindAll(Names.named("backgroundExecutor"), ScheduledListeningExecutorService.class, BackgroundTimerProvider.class, ExecutorService.class, Executor.class, ScheduledExecutorService.class);
        bindAll(Names.named("dhtExecutor"), ListeningExecutorService.class, DHTExecutorProvider.class, Executor.class, ExecutorService.class);
        bindAll(Names.named("messageExecutor"), ListeningExecutorService.class, MessageExecutorProvider.class, Executor.class, ExecutorService.class);
        bindAll(Names.named("nioExecutor"), ScheduledExecutorService.class, NIOScheduledExecutorServiceProvider.class, ExecutorService.class, Executor.class);
        
        
        Executor fwtEventExecutor = ExecutorsHelper.newProcessingQueue("FirewallEventThread");
        
        AsynchronousCachingEventMulticasterImpl<FirewallTransferStatusEvent> asyncTransferMulticaster 
            = new AsynchronousCachingEventMulticasterImpl<FirewallTransferStatusEvent>(fwtEventExecutor, BroadcastPolicy.IF_NOT_EQUALS);
        bind(new TypeLiteral<EventBean<FirewallTransferStatusEvent>>(){}).toInstance(asyncTransferMulticaster);
        bind(new TypeLiteral<AsynchronousEventBroadcaster<FirewallTransferStatusEvent>>(){}).toInstance(asyncTransferMulticaster);
        bind(new TypeLiteral<ListenerSupport<FirewallTransferStatusEvent>>(){}).toInstance(asyncTransferMulticaster);
        
        AsynchronousCachingEventMulticasterImpl<FirewallStatusEvent> asyncStatusMulticaster =
            new AsynchronousCachingEventMulticasterImpl<FirewallStatusEvent>(fwtEventExecutor, BroadcastPolicy.IF_NOT_EQUALS);
        bind(new TypeLiteral<EventBean<FirewallStatusEvent>>(){}).toInstance(asyncStatusMulticaster);
        bind(new TypeLiteral<AsynchronousEventBroadcaster<FirewallStatusEvent>>(){}).toInstance(asyncStatusMulticaster);
        bind(new TypeLiteral<ListenerSupport<FirewallStatusEvent>>(){}).toInstance(asyncStatusMulticaster);
        
        // These are bound because they are Singletons & Services, and must be started.
        bind(Statistics.class);
        bind(CoreRandomGlue.class);
        bind(ConnectionAcceptorGlue.class);
        bind(DownloadUpgradeTaskService.class);
        bind(LocalAcceptor.class);
        bind(Pinger.class);
        bind(ConnectionWatchdog.class);
        bind(RatingTable.class);
        bind(OutOfBandThroughputMeasurer.class);
        bind(HostCatcher.class);
        bind(LimeCoreGlue.class);
        bind(MongrelGlue.class);
        bind(WebServiceGlue.class);
        bind(QRPUpdater.class);
        bind(DaapManager.class);
        bind(FirewalledAddressSerializer.class).asEagerSingleton();
        bind(SameNATAddressResolver.class).asEagerSingleton();
        bind(ConnectableConnector.class).asEagerSingleton();
        bind(PushEndpointSerializer.class).asEagerSingleton();
    }
    
    
    @Singleton
    private static class SecureMessageVerifierProvider extends AbstractLazySingletonProvider<SecureMessageVerifier> {
        @Override
        protected SecureMessageVerifier createObject() {
            return new SecureMessageVerifierImpl("GCBADNZQQIASYBQHFKDERTRYAQATBAQBD4BIDAIA7V7VHAI5OUJCSUW7JKOC53HE473BDN2SHTXUIAGDDY7YBNSREZUUKXKAEJI7WWJ5RVMPVP6F6W5DB5WLTNKWZV4BHOAB2NDP6JTGBN3LTFIKLJE7T7UAI6YQELBE7O5J277LPRQ37A5VPZ6GVCTBKDYE7OB7NU6FD3BQENKUCNNBNEJS6Z27HLRLMHLSV37SEIBRTHORJAA4OAQVACLWAUEPCURQXTFSSK4YFIXLQQF7AWA46UBIDAIA67Q2BBOWTM655S54VNODNOCXXF4ZJL537I5OVAXZK5GAWPIHQJTVCWKXR25NIWKP4ZYQOEEBQC2ESFTREPUEYKAWCO346CJSRTEKNYJ4CZ5IWVD4RUUOBI5ODYV3HJTVSFXKG7YL7IQTKYXR7NRHUAJEHPGKJ4N6VBIZBCNIQPP6CWXFT4DJFC3GL2AHWVJFMQAUYO76Z5ESUA4BQQAAFAMAHR2O6ZOZA4SFMDNGGUC7PDA7W7HMUGEA32R7SCKAANQXFWMOD6KJE43YM53HIPVADVKFL5FA6MKL5GHTBHIURAWGGQTXPEGPLXB7KYTMC6TAPUPFYGNWB4THDQVN4PDARIU3UGXQKFHNAQFL6TUJBA6KXTBLAJBSXD54J6NUVIECRUOA7R57AH6GWGO7VOBDRTIYBXPSY7FTI",
                        null);    
        }
    };
    
    @Singleton
    private static class InspectionVerifierProvider extends AbstractLazySingletonProvider<SecureMessageVerifier> {
        @Override
        protected SecureMessageVerifier createObject() {
            return new SecureMessageVerifierImpl("GCBADNZQQIASYBQHFKDERTRYAQATBAQBD4BIDAIA7V7VHAI5OUJCSUW7JKOC53HE473BDN2SHTXUIAGDDY7YBNSREZUUKXKAEJI7WWJ5RVMPVP6F6W5DB5WLTNKWZV4BHOAB2NDP6JTGBN3LTFIKLJE7T7UAI6YQELBE7O5J277LPRQ37A5VPZ6GVCTBKDYE7OB7NU6FD3BQENKUCNNBNEJS6Z27HLRLMHLSV37SEIBRTHORJAA4OAQVACLWAUEPCURQXTFSSK4YFIXLQQF7AWA46UBIDAIA67Q2BBOWTM655S54VNODNOCXXF4ZJL537I5OVAXZK5GAWPIHQJTVCWKXR25NIWKP4ZYQOEEBQC2ESFTREPUEYKAWCO346CJSRTEKNYJ4CZ5IWVD4RUUOBI5ODYV3HJTVSFXKG7YL7IQTKYXR7NRHUAJEHPGKJ4N6VBIZBCNIQPP6CWXFT4DJFC3GL2AHWVJFMQAUYO76Z5ESUA4BQQAAFAMAEYRDUD6O2YID3ORGATJV7UQLUEJORGPY4ETQUH3SKDGITTQENVN6IRZBDJOUZLD6UKX2APFEEA6IJVMCURT4VWBICX5L7GKAUYU325AMMNR7PW6GWGXHR24D5HVTIO6JZ2VRMTOIE7GIZPINPOJXWYDUZQG57ZVBII6XHW2KGITQKQLODJTZGRJHELY6BRXL7VHHQDGCIBWYU",
                        null);
        }
    }
    
    @Singleton
    private static class UnlimitedExecutorProvider extends AbstractLazySingletonProvider<ListeningExecutorService> {
        @Override
        protected ListeningExecutorService createObject() {
            return ExecutorsHelper.newThreadPool(ExecutorsHelper.daemonThreadFactory("IdleThread"));
        }
    }
    
    @Singleton
    private static class FastExecutorProvider extends AbstractLazySingletonProvider<ScheduledExecutorService> {
        @Override
        protected ScheduledThreadPoolExecutor createObject() {
            ScheduledThreadPoolExecutor stpe = new ScheduledThreadPoolExecutor(1, ExecutorsHelper.daemonThreadFactory("ScheduledThread"));
            return stpe;
        }
    }    
    
    @Singleton
    private static class BackgroundTimerProvider extends AbstractLazySingletonProvider<ScheduledListeningExecutorService> {
        @Override
        protected ScheduledListeningExecutorService createObject() {
            return new SimpleTimer(true);
        }
    }
    
    @Singleton
    private static class MessageExecutorProvider extends AbstractLazySingletonProvider<ListeningExecutorService> {
        @Override
        protected ListeningExecutorService createObject() {
            return ExecutorsHelper.newProcessingQueue("Message-Executor");
        }
    }

    @Singleton
    private static class DHTExecutorProvider extends AbstractLazySingletonProvider<ListeningExecutorService> {
        @Override
        protected ListeningExecutorService createObject() {
            return ExecutorsHelper.newProcessingQueue("DHT-Executor");
        }
    } 
    
    @Provides
    @ExternalIP
    public byte[] get(NetworkManager networkManager) {
        return networkManager.getExternalAddress();
    }
    
    ///////////////////////////////////////////////////////////////////////////
    /// BELOW ARE ALL HACK PROVIDERS THAT NEED TO BE UPDATED TO CONSTRUCT OBJECTS!
    // (This needs to wait till components are injected and stop using singletons too.)
    
    @Singleton
    private static class NIODispatcherProvider implements Provider<NIODispatcher> {
        public NIODispatcher get() {
            return NIODispatcher.instance();
        }
    };
    
    @Singleton
    private static class ByteBufferCacheProvider implements Provider<ByteBufferCache> {
        private final Provider<NIODispatcher> nioDispatcher;
        
        @Inject
        public ByteBufferCacheProvider(Provider<NIODispatcher> nioDispatcher) {
            this.nioDispatcher = nioDispatcher;
        }
        
        public ByteBufferCache get() {
            return nioDispatcher.get().getBufferCache();
        }
    };
    
    @Singleton
    private static class NIOScheduledExecutorServiceProvider implements Provider<ScheduledExecutorService> {
        private final Provider<NIODispatcher> nioDispatcher;
        
        @Inject
        public NIOScheduledExecutorServiceProvider(Provider<NIODispatcher> nioDispatcher) {
            this.nioDispatcher = nioDispatcher;
        }
        
        public ScheduledExecutorService get() {
            return nioDispatcher.get().getScheduledExecutorService();
        }
    };
        
    ///////////////////////////////////////////////////////////////
    // !!! DO NOT ADD THINGS BELOW HERE !!!  PUT THEM ABOVE THE HACKS!
}
