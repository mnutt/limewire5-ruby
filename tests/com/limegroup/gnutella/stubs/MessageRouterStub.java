package com.limegroup.gnutella.stubs;

import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledExecutorService;

import org.limewire.io.GUID;
import org.limewire.net.SocketsManager;
import org.limewire.security.MACCalculatorRepositoryManager;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.GuidMapManager;
import com.limegroup.gnutella.HostCatcher;
import com.limegroup.gnutella.MessageDispatcher;
import com.limegroup.gnutella.MessageHandlerBinder;
import com.limegroup.gnutella.MulticastService;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.PongCacher;
import com.limegroup.gnutella.QueryUnicaster;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.StandardMessageRouter;
import com.limegroup.gnutella.Statistics;
import com.limegroup.gnutella.UDPReplyHandlerCache;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.UploadManager;
import com.limegroup.gnutella.auth.ContentManager;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.filters.URNFilter;
import com.limegroup.gnutella.guess.OnDemandUnicaster;
import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.library.SharedFilesKeywordIndex;
import com.limegroup.gnutella.messagehandlers.InspectionRequestHandler;
import com.limegroup.gnutella.messagehandlers.LimeACKHandler;
import com.limegroup.gnutella.messagehandlers.OOBHandler;
import com.limegroup.gnutella.messagehandlers.UDPCrawlerPingHandler;
import com.limegroup.gnutella.messages.OutgoingQueryReplyFactory;
import com.limegroup.gnutella.messages.PingReplyFactory;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.PingRequestFactory;
import com.limegroup.gnutella.messages.QueryReplyFactory;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.messages.StaticMessages;
import com.limegroup.gnutella.messages.vendor.HeadPongFactory;
import com.limegroup.gnutella.messages.vendor.ReplyNumberVendorMessageFactory;
import com.limegroup.gnutella.routing.QRPUpdater;
import com.limegroup.gnutella.search.QueryDispatcher;
import com.limegroup.gnutella.search.QueryHandlerFactory;
import com.limegroup.gnutella.search.SearchResultHandler;
import com.limegroup.gnutella.simpp.SimppManager;
import com.limegroup.gnutella.version.UpdateHandler;

/** A stub for MessageRouter that does nothing. */
@Singleton
public class MessageRouterStub extends StandardMessageRouter {
    
    @Inject
    public MessageRouterStub(NetworkManager networkManager,
            QueryRequestFactory queryRequestFactory,
            QueryHandlerFactory queryHandlerFactory,
            OnDemandUnicaster onDemandUnicaster,
            HeadPongFactory headPongFactory, PingReplyFactory pingReplyFactory,
            ConnectionManager connectionManager, @Named("forMeReplyHandler")
            ReplyHandler forMeReplyHandler, QueryUnicaster queryUnicaster,
            FileManager fileManager, ContentManager contentManager,
            DHTManager dhtManager, UploadManager uploadManager,
            DownloadManager downloadManager, UDPService udpService,
            SearchResultHandler searchResultHandler,
            SocketsManager socketsManager, HostCatcher hostCatcher,
            QueryReplyFactory queryReplyFactory, StaticMessages staticMessages,
            Provider<MessageDispatcher> messageDispatcher,
            MulticastService multicastService, QueryDispatcher queryDispatcher,
            Provider<ActivityCallback> activityCallback,
            ConnectionServices connectionServices,
            ApplicationServices applicationServices,
            @Named("backgroundExecutor")
            ScheduledExecutorService backgroundExecutor,
            Provider<PongCacher> pongCacher,
            Provider<SimppManager> simppManager,
            Provider<UpdateHandler> updateHandler,
            GuidMapManager guidMapManager, 
            UDPReplyHandlerCache udpReplyHandlerCache,
            Provider<InspectionRequestHandler> inspectionRequestHandlerFactory,
            Provider<UDPCrawlerPingHandler> udpCrawlerPingHandlerFactory,
            Statistics statistics,
            ReplyNumberVendorMessageFactory replyNumberVendorMessageFactory,
            PingRequestFactory pingRequestFactory,
            MessageHandlerBinder messageHandlerBinder,
            Provider<OOBHandler> oobHandlerFactory,
            Provider<MACCalculatorRepositoryManager> macManager,
            Provider<LimeACKHandler> limeACKHandler,
            OutgoingQueryReplyFactory outgoingQueryReplyFactory,
            SharedFilesKeywordIndex sharedFilesKeywordIndex, 
            QRPUpdater qrpUpdater, URNFilter urnFilter) {
        super(networkManager, queryRequestFactory, queryHandlerFactory,
                onDemandUnicaster, headPongFactory, pingReplyFactory,
                connectionManager, forMeReplyHandler, queryUnicaster,
                fileManager, contentManager, dhtManager, uploadManager,
                downloadManager, udpService, searchResultHandler,
                socketsManager, hostCatcher, queryReplyFactory,
                staticMessages, messageDispatcher, multicastService,
                queryDispatcher, activityCallback, connectionServices,
                applicationServices, backgroundExecutor, pongCacher,
                simppManager, updateHandler, guidMapManager,
                udpReplyHandlerCache, inspectionRequestHandlerFactory,
                udpCrawlerPingHandlerFactory, statistics,
                replyNumberVendorMessageFactory, pingRequestFactory,
                messageHandlerBinder, oobHandlerFactory, macManager,
                limeACKHandler, outgoingQueryReplyFactory,
                sharedFilesKeywordIndex, qrpUpdater, urnFilter);
    }
    
    @Override
    public void downloadFinished(GUID guid) throws IllegalArgumentException {
    }

    @Override
    protected boolean respondToQueryRequest(QueryRequest queryRequest,
                                            byte[] clientGUID,
                                            ReplyHandler handler) {
        return false;
    }

    @Override
    protected void respondToPingRequest(PingRequest request,
                                        ReplyHandler handler) {
	}

    @Override
    protected void respondToUDPPingRequest(PingRequest request, 
                                           InetSocketAddress addr,
                                           ReplyHandler handler) {}

}
