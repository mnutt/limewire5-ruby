package com.limegroup.gnutella.util;

import org.limewire.io.NetworkInstanceUtils;
import org.limewire.net.SocketsManager;
import org.limewire.security.SecureMessageVerifier;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.Acceptor;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.GuidMapManager;
import com.limegroup.gnutella.MessageDispatcher;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.NetworkUpdateSanityChecker;
import com.limegroup.gnutella.connection.MessageReaderFactory;
import com.limegroup.gnutella.filters.SpamFilterFactory;
import com.limegroup.gnutella.handshaking.HandshakeResponderFactory;
import com.limegroup.gnutella.handshaking.HeadersFactory;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.QueryReplyFactory;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactory;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.search.SearchResultHandler;
import com.limegroup.gnutella.simpp.SimppManager;
import com.limegroup.gnutella.version.UpdateHandler;

@Singleton
public class TestConnectionFactory {

    private final Provider<ConnectionManager> connectionManager;

    private final NetworkManager networkManager;

    private final QueryRequestFactory queryRequestFactory;

    private final HeadersFactory headersFactory;

    private final HandshakeResponderFactory handshakeResponderFactory;

    private final QueryReplyFactory queryReplyFactory;

    private final Provider<MessageDispatcher> messageDispatcher;

    private final Provider<NetworkUpdateSanityChecker> networkUpdateSanityChecker;

    private final Provider<SearchResultHandler> searchResultHandler;

    private final CapabilitiesVMFactory capabilitiesVMFactory;

    private final Provider<SocketsManager> socketsManager;

    private final Provider<Acceptor> acceptor;

    private final MessagesSupportedVendorMessage supportedVendorMessage;

    private final Provider<SimppManager> simppManager;

    private final Provider<UpdateHandler> updateHandler;

    private final Provider<ConnectionServices> connectionServices;

    private final GuidMapManager guidMapManager;

    private final SpamFilterFactory spamFilterFactory;

    private final MessageFactory messageFactory;

    private final MessageReaderFactory messageReaderFactory;

    private final ApplicationServices applicationServices;

    private final Provider<SecureMessageVerifier> secureMessageVerifier;
    
    private final NetworkInstanceUtils networkInstanceUtils;

    @Inject
    public TestConnectionFactory(Provider<ConnectionManager> connectionManager,
            NetworkManager networkManager, QueryRequestFactory queryRequestFactory,
            HeadersFactory headersFactory, HandshakeResponderFactory handshakeResponderFactory,
            QueryReplyFactory queryReplyFactory, Provider<MessageDispatcher> messageDispatcher,
            Provider<NetworkUpdateSanityChecker> networkUpdateSanityChecker,
            Provider<SearchResultHandler> searchResultHandler,
            CapabilitiesVMFactory capabilitiesVMFactory, Provider<SocketsManager> socketsManager,
            Provider<Acceptor> acceptor, MessagesSupportedVendorMessage supportedVendorMessage,
            Provider<SimppManager> simppManager, Provider<UpdateHandler> updateHandler,
            Provider<ConnectionServices> connectionServices, GuidMapManager guidMapManager,
            SpamFilterFactory spamFilterFactory, MessageFactory messageFactory,
            MessageReaderFactory messageReaderFactory, ApplicationServices applicationServices,
            Provider<SecureMessageVerifier> secureMessageVerifier,
            NetworkInstanceUtils networkInstanceUtils) {
        this.connectionManager = connectionManager;
        this.networkManager = networkManager;
        this.queryRequestFactory = queryRequestFactory;
        this.headersFactory = headersFactory;
        this.handshakeResponderFactory = handshakeResponderFactory;
        this.queryReplyFactory = queryReplyFactory;
        this.messageDispatcher = messageDispatcher;
        this.networkUpdateSanityChecker = networkUpdateSanityChecker;
        this.applicationServices = applicationServices;
        this.searchResultHandler = searchResultHandler;
        this.capabilitiesVMFactory = capabilitiesVMFactory;
        this.socketsManager = socketsManager;
        this.acceptor = acceptor;
        this.supportedVendorMessage = supportedVendorMessage;
        this.simppManager = simppManager;
        this.updateHandler = updateHandler;
        this.connectionServices = connectionServices;
        this.guidMapManager = guidMapManager;
        this.spamFilterFactory = spamFilterFactory;
        this.messageFactory = messageFactory;
        this.messageReaderFactory = messageReaderFactory;
        this.secureMessageVerifier = secureMessageVerifier;
        this.networkInstanceUtils = networkInstanceUtils;
    }
    
    public OldConnection createOldConnection(int connections) {
        return new OldConnection(connections, false, connectionManager.get(), networkManager,
                queryRequestFactory, headersFactory, handshakeResponderFactory, queryReplyFactory,
                messageDispatcher.get(), networkUpdateSanityChecker.get(), searchResultHandler
                        .get(), capabilitiesVMFactory, socketsManager.get(), acceptor.get(),
                supportedVendorMessage, simppManager, updateHandler, connectionServices,
                guidMapManager, spamFilterFactory, messageReaderFactory, messageFactory,
                applicationServices, secureMessageVerifier.get(), networkInstanceUtils);
    }

    /**
     * Creates a generic <tt>NewConnection</tt> for testing with all of the
     * default values and the specified number of connections.
     */
    public NewConnection createNewConnection(int connections) {
        return new NewConnection(connections, false, new QueryRouteTable(),
                connectionManager.get(), networkManager, queryRequestFactory, headersFactory,
                handshakeResponderFactory, queryReplyFactory, messageDispatcher.get(),
                networkUpdateSanityChecker.get(), searchResultHandler.get(), capabilitiesVMFactory,
                socketsManager.get(), acceptor.get(), supportedVendorMessage, simppManager,
                updateHandler, connectionServices, guidMapManager, spamFilterFactory,
                messageReaderFactory, messageFactory, applicationServices, secureMessageVerifier
                        .get(), networkInstanceUtils);
    }

    /**
     * Creates a generic <tt>NewConnection</tt> for testing with all of the
     * default values.
     */
    public NewConnection createNewConnection() {
        return new NewConnection(10, false, new QueryRouteTable(), connectionManager.get(),
                networkManager, queryRequestFactory, headersFactory, handshakeResponderFactory,
                queryReplyFactory, messageDispatcher.get(), networkUpdateSanityChecker.get(),
                searchResultHandler.get(), capabilitiesVMFactory, socketsManager.get(), acceptor
                        .get(), supportedVendorMessage, simppManager, updateHandler,
                connectionServices, guidMapManager, spamFilterFactory, messageReaderFactory,
                messageFactory, applicationServices, secureMessageVerifier.get(), networkInstanceUtils);
    }

    /**
     * Creates a utility connection that has a hit in its query route tables for
     * every query.
     */
    public NewConnection createNewHitConnection() {
        return new NewConnection(10, false, new HitQueryRouteTable(), connectionManager.get(),
                networkManager, queryRequestFactory, headersFactory, handshakeResponderFactory,
                queryReplyFactory, messageDispatcher.get(), networkUpdateSanityChecker.get(),
                searchResultHandler.get(), capabilitiesVMFactory, socketsManager.get(), acceptor
                        .get(), supportedVendorMessage, simppManager, updateHandler,
                connectionServices, guidMapManager, spamFilterFactory, messageReaderFactory,
                messageFactory, applicationServices, secureMessageVerifier.get(), networkInstanceUtils);
    }

    public LeafConnection createAltLeafConnection() {
        return new LeafConnection(new String[] { LeafConnection.ALT_LEAF_KEYWORD },
                "ALT LEAF CONNECTION", false, 15, true, new QueryRouteTable(), connectionManager
                        .get(), networkManager, queryRequestFactory, headersFactory,
                handshakeResponderFactory, queryReplyFactory, messageDispatcher.get(),
                networkUpdateSanityChecker.get(), searchResultHandler.get(), capabilitiesVMFactory,
                socketsManager.get(), acceptor.get(), supportedVendorMessage, simppManager,
                updateHandler, connectionServices, guidMapManager, spamFilterFactory,
                messageReaderFactory, messageFactory, applicationServices, secureMessageVerifier
                        .get(), networkInstanceUtils);
    }

    public LeafConnection createWithKeywords(String[] keywords) {
        return new LeafConnection(keywords, "LEAF CONNECTION", true, 20, true,
                new QueryRouteTable(), connectionManager.get(), networkManager,
                queryRequestFactory, headersFactory, handshakeResponderFactory, queryReplyFactory,
                messageDispatcher.get(), networkUpdateSanityChecker.get(), searchResultHandler
                        .get(), capabilitiesVMFactory, socketsManager.get(), acceptor.get(),
                supportedVendorMessage, simppManager, updateHandler, connectionServices,
                guidMapManager, spamFilterFactory, messageReaderFactory, messageFactory,
                applicationServices, secureMessageVerifier.get(), networkInstanceUtils);
    }

    public LeafConnection createLeafConnection(boolean queriesMustBeInRoutingTable) {
        return new LeafConnection(new String[0], "LEAF_CONNECTION", false, 15,
                queriesMustBeInRoutingTable, new QueryRouteTable(), connectionManager.get(),
                networkManager, queryRequestFactory, headersFactory, handshakeResponderFactory,
                queryReplyFactory, messageDispatcher.get(), networkUpdateSanityChecker.get(),
                searchResultHandler.get(), capabilitiesVMFactory, socketsManager.get(), acceptor
                        .get(), supportedVendorMessage, simppManager, updateHandler,
                connectionServices, guidMapManager, spamFilterFactory, messageReaderFactory,
                messageFactory, applicationServices, secureMessageVerifier.get(), networkInstanceUtils);
    }

    public UltrapeerConnection createUltrapeerConnection() {
        return new UltrapeerConnection(null, 15, false, new QueryRouteTable(), connectionManager
                .get(), networkManager, queryRequestFactory, headersFactory,
                handshakeResponderFactory, queryReplyFactory, messageDispatcher.get(),
                networkUpdateSanityChecker.get(), searchResultHandler.get(), capabilitiesVMFactory,
                socketsManager.get(), acceptor.get(), supportedVendorMessage, simppManager,
                updateHandler, connectionServices, guidMapManager, spamFilterFactory,
                messageReaderFactory, messageFactory, applicationServices, secureMessageVerifier
                        .get(), networkInstanceUtils);
    }

    public UltrapeerConnection createUltrapeerConnection(String[] keywords) {
        return new UltrapeerConnection(keywords, 15, false, new QueryRouteTable(),
                connectionManager.get(), networkManager, queryRequestFactory, headersFactory,
                handshakeResponderFactory, queryReplyFactory, messageDispatcher.get(),
                networkUpdateSanityChecker.get(), searchResultHandler.get(), capabilitiesVMFactory,
                socketsManager.get(), acceptor.get(), supportedVendorMessage, simppManager,
                updateHandler, connectionServices, guidMapManager, spamFilterFactory,
                messageReaderFactory, messageFactory, applicationServices, secureMessageVerifier
                        .get(), networkInstanceUtils);
    }

    static final class HitQueryRouteTable extends QueryRouteTable {
        @Override
        public boolean contains(QueryRequest qr) {
            return true;
        }
    }

}
