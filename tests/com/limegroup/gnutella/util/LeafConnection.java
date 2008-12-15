package com.limegroup.gnutella.util;

import org.limewire.io.NetworkInstanceUtils;
import org.limewire.net.SocketsManager;
import org.limewire.security.SecureMessageVerifier;

import com.google.inject.Provider;
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
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactory;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.search.SearchResultHandler;
import com.limegroup.gnutella.simpp.SimppManager;
import com.limegroup.gnutella.version.UpdateHandler;

/**
 * Specialized class that uses special keywords for leaf routing tables.
 */
public final class LeafConnection extends NewConnection {

    /**
     * Constant keyword that is in ever standard leaf's QRP table.
     */
    public static final String LEAF_KEYWORD = "LEAFKEYWORD";

    /**
     * Constant alternate keyword for use in testing.
     */
    public static final String ALT_LEAF_KEYWORD = "ALTLEAFKEYWORD";

    private final String descriptor;

    /**
     * Creates a new LeafConnection with the specified list of keywords, etc.
     */
    LeafConnection(String[] keywords, String descriptor,
            boolean addStandardKeyword, int connections, boolean queriesMustBeInRoutingTable, QueryRouteTable qrt, 
            ConnectionManager connectionManager, NetworkManager networkManager,
            QueryRequestFactory queryRequestFactory, HeadersFactory headersFactory,
            HandshakeResponderFactory handshakeResponderFactory,
            QueryReplyFactory queryReplyFactory, MessageDispatcher messageDispatcher,
            NetworkUpdateSanityChecker networkUpdateSanityChecker,
            SearchResultHandler searchResultHandler, CapabilitiesVMFactory capabilitiesVMFactory,
            SocketsManager socketsManager, Acceptor acceptor,
            MessagesSupportedVendorMessage supportedVendorMessage,
            Provider<SimppManager> simppManager, Provider<UpdateHandler> updateHandler,
            Provider<ConnectionServices> connectionServices, GuidMapManager guidMapManager,
            SpamFilterFactory spamFilterFactory, MessageReaderFactory messageReaderFactory,
            MessageFactory messageFactory, ApplicationServices applicationServices,
            SecureMessageVerifier secureMessageVerifier, NetworkInstanceUtils networkInstanceUtils) {
        super(connections, queriesMustBeInRoutingTable, qrt, connectionManager, networkManager,
                queryRequestFactory, headersFactory, handshakeResponderFactory, queryReplyFactory,
                messageDispatcher, networkUpdateSanityChecker, searchResultHandler,
                capabilitiesVMFactory, socketsManager, acceptor, supportedVendorMessage,
                simppManager, updateHandler, connectionServices, guidMapManager, spamFilterFactory,
                messageReaderFactory, messageFactory, applicationServices, secureMessageVerifier,
                networkInstanceUtils);

        this.descriptor = descriptor;

        for (int i = 0; i < keywords.length; i++) {
            QRT.add(keywords[i]);
        }
        if (addStandardKeyword) {
            QRT.add(LEAF_KEYWORD);
        }
    }

    @Override
    public String toString() {
        return descriptor + ": " + QRT;
    }

}
