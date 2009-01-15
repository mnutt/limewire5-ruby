package com.limegroup.gnutella;

import java.util.concurrent.ScheduledExecutorService;

import org.limewire.http.httpclient.LimeHttpClient;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.net.ConnectionDispatcher;
import org.limewire.net.FirewallService;
import org.limewire.net.SocketsManager;
import org.limewire.nio.NIODispatcher;
import org.limewire.promotion.PromotionSearcher;
import org.limewire.promotion.PromotionServices;
import org.limewire.rudp.RUDPContext;
import org.limewire.rudp.UDPMultiplexor;
import org.limewire.rudp.UDPSelectorProvider;
import org.limewire.security.SecureMessageVerifier;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import com.limegroup.bittorrent.BTContextFactory;
import com.limegroup.bittorrent.BTMetaInfoFactory;
import com.limegroup.bittorrent.BTUploaderFactory;
import com.limegroup.bittorrent.ManagedTorrentFactory;
import com.limegroup.bittorrent.TorrentManager;
import com.limegroup.bittorrent.dht.DHTPeerLocator;
import com.limegroup.bittorrent.dht.DHTPeerPublisher;
import com.limegroup.bittorrent.disk.DiskManagerFactory;
import com.limegroup.bittorrent.handshaking.IncomingConnectionHandler;
import com.limegroup.bittorrent.tracking.TrackerFactory;
import com.limegroup.bittorrent.tracking.TrackerManagerFactory;
import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;
import com.limegroup.gnutella.auth.ContentManager;
import com.limegroup.gnutella.auth.IpPortContentAuthorityFactory;
import com.limegroup.gnutella.bootstrap.UDPHostCacheFactory;
import com.limegroup.gnutella.browser.ExternalControl;
import com.limegroup.gnutella.browser.LocalAcceptor;
import com.limegroup.gnutella.browser.LocalHTTPAcceptor;
import com.limegroup.gnutella.connection.ConnectionCheckerManager;
import com.limegroup.gnutella.connection.MessageReaderFactory;
import com.limegroup.gnutella.connection.RoutedConnectionFactory;
import com.limegroup.gnutella.daap.DaapManager;
import com.limegroup.gnutella.dht.DHTBootstrapperFactory;
import com.limegroup.gnutella.dht.DHTControllerFactory;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.DHTNodeFetcherFactory;
import com.limegroup.gnutella.dht.db.AltLocFinder;
import com.limegroup.gnutella.dht.db.AltLocValueFactory;
import com.limegroup.gnutella.dht.db.PushProxiesValueFactory;
import com.limegroup.gnutella.downloader.DiskController;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.filters.MutableGUIDFilter;
import com.limegroup.gnutella.filters.SpamFilterFactory;
import com.limegroup.gnutella.geocode.CachedGeoLocation;
import com.limegroup.gnutella.guess.OnDemandUnicaster;
import com.limegroup.gnutella.handshaking.HandshakeResponderFactory;
import com.limegroup.gnutella.handshaking.HeadersFactory;
import com.limegroup.gnutella.http.FeaturesWriter;
import com.limegroup.gnutella.http.HttpExecutor;
import com.limegroup.gnutella.library.CreationTimeCache;
import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.library.LocalFileDetailsFactory;
import com.limegroup.gnutella.library.SharedFilesKeywordIndex;
import com.limegroup.gnutella.library.UrnCache;
import com.limegroup.gnutella.licenses.LicenseCache;
import com.limegroup.gnutella.licenses.LicenseFactory;
import com.limegroup.gnutella.licenses.LicenseVerifier;
import com.limegroup.gnutella.lws.server.LWSManager;
import com.limegroup.gnutella.messagehandlers.InspectionRequestHandler;
import com.limegroup.gnutella.messagehandlers.UDPCrawlerPingHandler;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.PingReplyFactory;
import com.limegroup.gnutella.messages.PingRequestFactory;
import com.limegroup.gnutella.messages.QueryReplyFactory;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.messages.StaticMessages;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactory;
import com.limegroup.gnutella.messages.vendor.HeadPongFactory;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutella.messages.vendor.ReplyNumberVendorMessageFactory;
import com.limegroup.gnutella.messages.vendor.UDPCrawlerPongFactory;
import com.limegroup.gnutella.messages.vendor.VendorMessageFactory;
import com.limegroup.gnutella.metadata.MetaDataFactory;
import com.limegroup.gnutella.metadata.MetaDataReader;
import com.limegroup.gnutella.search.QueryDispatcher;
import com.limegroup.gnutella.search.QueryHandlerFactory;
import com.limegroup.gnutella.search.SearchResultHandler;
import com.limegroup.gnutella.simpp.SimppManager;
import com.limegroup.gnutella.spam.RatingTable;
import com.limegroup.gnutella.spam.SpamManager;
import com.limegroup.gnutella.statistics.QueryStats;
import com.limegroup.gnutella.statistics.TcpBandwidthStatistics;
import com.limegroup.gnutella.uploader.FileResponseEntityFactory;
import com.limegroup.gnutella.uploader.HTTPHeaderUtils;
import com.limegroup.gnutella.uploader.HttpRequestHandlerFactory;
import com.limegroup.gnutella.uploader.UploadSlotManager;
import com.limegroup.gnutella.version.UpdateCollectionFactory;
import com.limegroup.gnutella.version.UpdateHandler;
import com.limegroup.gnutella.xml.LimeXMLDocumentFactory;
import com.limegroup.gnutella.xml.LimeXMLDocumentHelper;
import com.limegroup.gnutella.xml.LimeXMLProperties;
import com.limegroup.gnutella.xml.LimeXMLReplyCollectionFactory;
import com.limegroup.gnutella.xml.LimeXMLSchemaRepository;
import com.limegroup.gnutella.xml.SchemaReplyCollectionMapper;

/**
 * Contains mostly all references to singletons within LimeWire.
 * This class should only be used if it is not possible to inject
 * the correct values into what you're using.  In most cases,
 * it should be possible to just get the injector and call
 * injector.injectMembers(myObject), which is still a superior
 * option to retrieving the individual objects from this class.
 */
@Singleton
public class LimeWireCore {
        
    private final Injector injector;
    
    @Inject
    public LimeWireCore(Injector injector) {
        this.injector = injector;
    }

    public Injector getInjector() {
        return injector;
    }

    public LocalFileDetailsFactory getLocalFileDetailsFactory() {
        return injector.getInstance(LocalFileDetailsFactory.class);
    }

    public AlternateLocationFactory getAlternateLocationFactory() {
        return injector.getInstance(AlternateLocationFactory.class);
    }

    public AltLocValueFactory getAltLocValueFactory() {
        return injector.getInstance(AltLocValueFactory.class);
    }

    public SocketsManager getSocketsManager() {
        return injector.getInstance(SocketsManager.class);
    }

    public RoutedConnectionFactory getManagedConnectionFactory() {
        return injector.getInstance(RoutedConnectionFactory.class);
    }

    public QueryRequestFactory getQueryRequestFactory() {
        return injector.getInstance(QueryRequestFactory.class);
    }

    public QueryHandlerFactory getQueryHandlerFactory() {
        return injector.getInstance(QueryHandlerFactory.class);
    }

    public UploadSlotManager getUploadSlotManager() {
        return injector.getInstance(UploadSlotManager.class);
    }

    public FileManager getFileManager() {
        return injector.getInstance(FileManager.class);
    }

    public UploadManager getUploadManager() {
        return injector.getInstance(UploadManager.class);
    }

    public HeadPongFactory getHeadPongFactory() {
        return injector.getInstance(HeadPongFactory.class);
    }

    public FeaturesWriter getFeaturesWriter() {
        return injector.getInstance(FeaturesWriter.class);
    }

    public HTTPHeaderUtils getHttpHeaderUtils() {
        return injector.getInstance(HTTPHeaderUtils.class);
    }

    public TrackerFactory getTrackerFactory() {
        return injector.getInstance(TrackerFactory.class);
    }

    public TrackerManagerFactory getTrackerManagerFactory() {
        return injector.getInstance(TrackerManagerFactory.class);
    }

    public TorrentManager getTorrentManager() {
        return injector.getInstance(TorrentManager.class);
    }

    public ManagedTorrentFactory getManagedTorrentFactory() {
        return injector.getInstance(ManagedTorrentFactory.class);
    }

    public PushEndpointFactory getPushEndpointFactory() {
        return injector.getInstance(PushEndpointFactory.class);
    }

    public HeadersFactory getHeadersFactory() {
        return injector.getInstance(HeadersFactory.class);
    }

    public HandshakeResponderFactory getHandshakeResponderFactory() {
        return injector.getInstance(HandshakeResponderFactory.class);
    }

    public PushProxiesValueFactory getPushProxiesValueFactory() {
        return injector.getInstance(PushProxiesValueFactory.class);
    }

    public PingReplyFactory getPingReplyFactory() {
        return injector.getInstance(PingReplyFactory.class);
    }

    public DHTControllerFactory getDhtControllerFactory() {
        return injector.getInstance(DHTControllerFactory.class);
    }

    public DHTManager getDhtManager() {
        return injector.getInstance(DHTManager.class);
    }

    public ConnectionManager getConnectionManager() {
        return injector.getInstance(ConnectionManager.class);
    }

    public NetworkManager getNetworkManager() {
        return injector.getInstance(NetworkManager.class);
    }

    public UDPService getUdpService() {
        return injector.getInstance(UDPService.class);
    }

    public Acceptor getAcceptor() {
        return injector.getInstance(Acceptor.class);
    }
    
    public ForMeReplyHandler getForMeReplyHandler() {
        return injector.getInstance(ForMeReplyHandler.class);
    }
    
    public QueryUnicaster getQueryUnicaster() {
        return injector.getInstance(QueryUnicaster.class);
    }
    
    public OnDemandUnicaster getOnDemandUnicaster() {
        return injector.getInstance(OnDemandUnicaster.class);
    }

    public MessageRouter getMessageRouter() {
        return injector.getInstance(MessageRouter.class);
    }

    public DownloadManager getDownloadManager() {
        return injector.getInstance(DownloadManager.class);
    }

    public AltLocFinder getAltLocFinder() {
        return injector.getInstance(AltLocFinder.class);
    }

    public ConnectionDispatcher getLocalConnectionDispatcher() {
        return injector.getInstance(Key.get(ConnectionDispatcher.class, Names.named("local")));
    }

    public ConnectionDispatcher getConnectionDispatcher() {
        return injector.getInstance(Key.get(ConnectionDispatcher.class, Names.named("global")));
    }

    public LocalAcceptor getLocalAcceptor() {
        return injector.getInstance(LocalAcceptor.class);
    }
    
    public LocalHTTPAcceptor getLocalHTTPAcceptor() {
        return injector.getInstance(LocalHTTPAcceptor.class);
    }

    public HostCatcher getHostCatcher() {
        return injector.getInstance(HostCatcher.class);
    }

    public HTTPAcceptor getHttpUploadAcceptor() {
        return injector.getInstance(HTTPAcceptor.class);
    }

    public PushManager getPushManager() {
        return injector.getInstance(PushManager.class);
    }

    public ResponseVerifier getResponseVerifier() {
        return injector.getInstance(ResponseVerifier.class);
    }

    public SearchResultHandler getSearchResultHandler() {
        return injector.getInstance(SearchResultHandler.class);
    }

    public AltLocManager getAltLocManager() {
        return injector.getInstance(AltLocManager.class);
    }

    public ContentManager getContentManager() {
        return injector.getInstance(ContentManager.class);
    }

    public IPFilter getIpFilter() {
        return injector.getInstance(IPFilter.class);
    }

    public IPFilter getHostileFilter() {
        return injector.getInstance(Key.get(IPFilter.class,Names.named("hostileFilter")));
    }

    public NetworkUpdateSanityChecker getNetworkUpdateSanityChecker() {
        return injector.getInstance(NetworkUpdateSanityChecker.class);
    }

    public BandwidthManager getBandwidthManager() {
        return injector.getInstance(BandwidthManager.class);
    }

    public HttpExecutor getHttpExecutor() {
        return injector.getInstance(HttpExecutor.class);
    }

    public QueryStats getQueryStats() {
        return injector.getInstance(QueryStats.class);
    }

    public NodeAssigner getNodeAssigner() {
        return injector.getInstance(NodeAssigner.class);
    }

    public Statistics getStatistics() {
        return injector.getInstance(Statistics.class);
    }

    public SecureMessageVerifier getSecureMessageVerifier() {
        return injector.getInstance(SecureMessageVerifier.class);
    }

    public CreationTimeCache getCreationTimeCache() {
        return injector.getInstance(CreationTimeCache.class);
    }

    public HttpRequestHandlerFactory getHttpRequestHandlerFactory() {
        return injector.getInstance(HttpRequestHandlerFactory.class); 
    }

    public UrnCache getUrnCache() {
        return injector.getInstance(UrnCache.class);
    }

    public ResponseFactory getResponseFactory() {
        return injector.getInstance(ResponseFactory.class);
    }

    public QueryReplyFactory getQueryReplyFactory() {
        return injector.getInstance(QueryReplyFactory.class);
    }

    public StaticMessages getStaticMessages() {
        return injector.getInstance(StaticMessages.class);
    }
    
    public DownloadCallback getInNetworkCallback() {
        return injector.getInstance(Key.get(DownloadCallback.class, Names.named("inNetwork")));
    }

    public MessageDispatcher getMessageDispatcher() {
        return injector.getInstance(MessageDispatcher.class);
    }

    public MulticastService getMulticastService() {
        return injector.getInstance(MulticastService.class);
    }
    
    public DiskManagerFactory getDiskManagerFactory() {
        return injector.getInstance(DiskManagerFactory.class);
    }

    public IncomingConnectionHandler getIncomingConnectionHandler() {
        return injector.getInstance(IncomingConnectionHandler.class);
    }

    public ConnectionWatchdog getConnectionWatchdog() {
        return injector.getInstance(ConnectionWatchdog.class);
    }

    public Pinger getPinger() {
        return injector.getInstance(Pinger.class);
    }

    public PongCacher getPongCacher() {
        return injector.getInstance(PongCacher.class);
    }

    public UPnPManager getUPnPManager() {
        return injector.getInstance(UPnPManager.class);
    }

    public MutableGUIDFilter getMutableGUIDFilter() {
        return injector.getInstance(MutableGUIDFilter.class);
    }

    public LicenseCache getLicenseCache() {
        return injector.getInstance(LicenseCache.class);
    }

    public MessagesSupportedVendorMessage getMessagesSupportedVendorMessage() {
        return injector.getInstance(MessagesSupportedVendorMessage.class);
    }

    public QueryDispatcher getQueryDispatcher() {
        return injector.getInstance(QueryDispatcher.class);
    }

    public RatingTable getRatingTable() {
        return injector.getInstance(RatingTable.class);
    }

    public SpamManager getSpamManager() {
        return injector.getInstance(SpamManager.class);
    }

    public UpdateHandler getUpdateHandler() {
        return injector.getInstance(UpdateHandler.class);
    }

    public LimeXMLProperties getLimeXMLProperties() {
        return injector.getInstance(LimeXMLProperties.class);
    }

    public LimeXMLSchemaRepository getLimeXMLSchemaRepository() {
        return injector.getInstance(LimeXMLSchemaRepository.class);
    }

    public SchemaReplyCollectionMapper getSchemaReplyCollectionMapper() {
        return injector.getInstance(SchemaReplyCollectionMapper.class);
    }

    public SimppManager getSimppManager() {
        return injector.getInstance(SimppManager.class);
    }

    public CapabilitiesVMFactory getCapabilitiesVMFactory() {
        return injector.getInstance(CapabilitiesVMFactory.class);
    }

    public UDPMultiplexor getUdpMultiplexor() {
        return injector.getInstance(UDPMultiplexor.class);
    }

    public UDPSelectorProvider getUDPSelectorProvider() {
        return injector.getInstance(UDPSelectorProvider.class);
    }

    public RUDPContext getRUDPContext() {
        return injector.getInstance(RUDPContext.class);
    }

    public BTContextFactory getBTContextFactory() {
        return injector.getInstance(BTContextFactory.class);
    }
    
    public ActivityCallback getActivityCallback() {
        return injector.getInstance(ActivityCallback.class);
    }
    
    public LifecycleManager getLifecycleManager() {
        return injector.getInstance(LifecycleManager.class);
    }

    public ConnectionServices getConnectionServices() {
        return injector.getInstance(ConnectionServices.class);
    }

    public SearchServices getSearchServices() {
        return injector.getInstance(SearchServices.class);
    }

    public ScheduledExecutorService getBackgroundExecutor() {
        return injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("backgroundExecutor")));
    }

    public DownloadServices getDownloadServices() {
        return injector.getInstance(DownloadServices.class);
    }

    public UploadServices getUploadServices() {
        return injector.getInstance(UploadServices.class);
    }

    public ApplicationServices getApplicationServices() {
        return injector.getInstance(ApplicationServices.class);
    }

    public SpamServices getSpamServices() {
        return injector.getInstance(SpamServices.class);
    }
    
    public ConnectionCheckerManager getConnectionCheckerManager() {
        return injector.getInstance(ConnectionCheckerManager.class);
    }
    
    public ExternalControl getExternalControl() {
        return injector.getInstance(ExternalControl.class);
    }
    
    public NIODispatcher getNIODispatcher() {
        return injector.getInstance(NIODispatcher.class);
    }

    public DHTBootstrapperFactory getDHTBootstrapperFactory() {
        return injector.getInstance(DHTBootstrapperFactory.class);
    }

    public DHTNodeFetcherFactory getDHTNodeFetcherFactory() {
        return injector.getInstance(DHTNodeFetcherFactory.class);
    }    
    
    public DownloadCallback getDownloadCallback() {
        return injector.getInstance(DownloadCallback.class);
    }

    public ReplyNumberVendorMessageFactory getReplyNumberVendorMessageFactory() {
        return injector.getInstance(ReplyNumberVendorMessageFactory.class);
    }

    public UDPPinger getUDPPinger() {
        return injector.getInstance(UDPPinger.class);
    }

    public UniqueHostPinger getUniqueHostPinger() {
        return injector.getInstance(UniqueHostPinger.class);
    }

    public ScheduledExecutorService getNIOExecutor() {
        return injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("nioExecutor")));
    }

    public GuidMapManager getGuidMapManager() {
        return injector.getInstance(GuidMapManager.class);
    }

    public BrowseHostHandlerManager getBrowseHostHandlerManager() {
        return injector.getInstance(BrowseHostHandlerManager.class);
    }

    public PushEndpointCache getPushEndpointCache() {
        return injector.getInstance(PushEndpointCache.class);
    }
    
    public SpamFilterFactory getSpamFilterFactory() {
        return injector.getInstance(SpamFilterFactory.class);
    }

    public UDPReplyHandlerFactory getUDPReplyHandlerFactory() {
        return injector.getInstance(UDPReplyHandlerFactory.class);
    }

    public UDPReplyHandlerCache getUDPReplyHandlerCache() {
        return injector.getInstance(UDPReplyHandlerCache.class);
    }
    
    public Provider<InspectionRequestHandler> getInspectionRequestHandlerFactory() {
        return injector.getProvider(InspectionRequestHandler.class);
    }
    
    public Provider<UDPCrawlerPingHandler> getUDPCrawlerPingHandlerFactory() {
        return injector.getProvider(UDPCrawlerPingHandler.class);
    }

    public FileResponseEntityFactory getFileRepsoneEntityFactory() {
        return injector.getInstance(FileResponseEntityFactory.class);
    }

    public MessageFactory getMessageFactory() {
        return injector.getInstance(MessageFactory.class);
    }

    public MessageReaderFactory getMessageReaderFactory() {
        return injector.getInstance(MessageReaderFactory.class);
    }

    public VendorMessageFactory getVendorMessageFactory() {
        return injector.getInstance(VendorMessageFactory.class);
    }

    public UDPCrawlerPongFactory getUDPCrawlerPongFactory() {
        return injector.getInstance(UDPCrawlerPongFactory.class);
    }

    public UDPHostCacheFactory getUDPHostCacheFactory() {
        return injector.getInstance(UDPHostCacheFactory.class);
    }

	public LimeXMLReplyCollectionFactory getLimeXMLReplyCollectionFactory() {
        return injector.getInstance(LimeXMLReplyCollectionFactory.class);
    }

    public LicenseFactory getLicenseFactory() {
        return injector.getInstance(LicenseFactory.class);
    }

    public LimeXMLDocumentFactory getLimeXMLDocumentFactory() {
        return injector.getInstance(LimeXMLDocumentFactory.class);
    }

    public MetaDataFactory getMetaDataFactory() {
        return injector.getInstance(MetaDataFactory.class);
    }

    public LimeXMLDocumentHelper getLimeXMLDocumentHelper() {
        return injector.getInstance(LimeXMLDocumentHelper.class);
    }

    public MetaDataReader getMetaDataReader() {
        return injector.getInstance(MetaDataReader.class);
    }

    public BTUploaderFactory getBTUploaderFactory() {
        return injector.getInstance(BTUploaderFactory.class);
    }

    public PingRequestFactory getPingRequestFactory() {
        return injector.getInstance(PingRequestFactory.class);
    }

    public IpPortContentAuthorityFactory getIpPortContentAuthorityFactory() {
        return injector.getInstance(IpPortContentAuthorityFactory.class);
    }

    public UpdateCollectionFactory getUpdateCollectionFactory() {
        return injector.getInstance(UpdateCollectionFactory.class);
    }

    public LimeCoreGlue getLimeCoreGlue() {
        return injector.getInstance(LimeCoreGlue.class);
    }

    public LicenseVerifier getLicenseVerifier() {
        return injector.getInstance(LicenseVerifier.class);
    }
    
    public LWSManager getLWSManger() {
        return injector.getInstance(LWSManager.class);
    }

    public DiskController getDiskController() {
        return injector.getInstance(DiskController.class);
    }

    public RemoteFileDescFactory getRemoteFileDescFactory() {
        return injector.getInstance(RemoteFileDescFactory.class);
    }

    public LimeHttpClient getLimeHttpClient() {
        return injector.getInstance(LimeHttpClient.class);
    }

    public TcpBandwidthStatistics getTcpBandwidthStatistics() {
        return injector.getInstance(TcpBandwidthStatistics.class);
    }
    
    public NetworkInstanceUtils getNetworkInstanceUtils() {
        return injector.getInstance(NetworkInstanceUtils.class);
    }
    
    public DHTPeerPublisher getDHTPeerPublisher() {
        return injector.getInstance(DHTPeerPublisher.class);
    }
    
    public DHTPeerLocator getDHTPeerLocator() {
        return injector.getInstance(DHTPeerLocator.class);
    }
    
    public BTMetaInfoFactory getBTMetaInfoFactory() {
        return injector.getInstance(BTMetaInfoFactory.class);
    }    
    
    public PromotionSearcher getPromotionSearcher() {
        return injector.getInstance(PromotionSearcher.class);
    }   
    
    public PromotionServices getPromotionServices() {
        return injector.getInstance(PromotionServices.class);
    }

    public CachedGeoLocation getCachedGeoLocation() {
        return injector.getInstance(CachedGeoLocation.class);
    }
    
    public ServiceRegistry getServiceRegistry() {
        return injector.getInstance(ServiceRegistry.class);
    }

    public SharedFilesKeywordIndex getSharedFilesKeywordIndex() {
        return injector.getInstance(SharedFilesKeywordIndex.class);
    }
    
    public FirewallService getFirewallService() {
        return injector.getInstance(FirewallService.class);
    }

    public DaapManager getDaapManager() {
        return injector.getInstance(DaapManager.class);
    }
}
