package com.limegroup.gnutella.util;

import java.util.concurrent.atomic.AtomicReference;

import org.limewire.io.NetworkInstanceUtils;
import org.limewire.net.SocketsManager;
import org.limewire.net.SocketsManager.ConnectType;
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
import com.limegroup.gnutella.connection.ConnectionCapabilities;
import com.limegroup.gnutella.connection.ConnectionCapabilitiesDelegator;
import com.limegroup.gnutella.connection.GnutellaConnection;
import com.limegroup.gnutella.connection.MessageReaderFactory;
import com.limegroup.gnutella.filters.SpamFilterFactory;
import com.limegroup.gnutella.handshaking.HandshakeResponderFactory;
import com.limegroup.gnutella.handshaking.HeadersFactory;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.QueryReplyFactory;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactory;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutella.routing.PatchTableMessage;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.routing.ResetTableMessage;
import com.limegroup.gnutella.search.SearchResultHandler;
import com.limegroup.gnutella.simpp.SimppManager;
import com.limegroup.gnutella.version.UpdateHandler;

/**
 * Helper class that overrides getNumIntraUltrapeerConnections for
 * testing the horizon calculation and testing the new search
 * architecture.
 */
public abstract class TestConnection extends GnutellaConnection {
    
    private final boolean queriesMustBeInRoutingTables;

    private final int connections;
    
    private int numQueries = 0;
    
    private int totalTTL = 0;
    
    private boolean receivedQuery;
    
    /**
     * Constant for the <tt>QueryRouteTable</tt> for this
     * connection -- can be used by subclasses.
     */
    protected QueryRouteTable QRT;
    
    TestConnection(int connections, boolean queriesMustBeInRoutingTable, ConnectionManager connectionManager, NetworkManager networkManager,
            QueryRequestFactory queryRequestFactory,
            HeadersFactory headersFactory,
            HandshakeResponderFactory handshakeResponderFactory,
            QueryReplyFactory queryReplyFactory,
            MessageDispatcher messageDispatcher,
            NetworkUpdateSanityChecker networkUpdateSanityChecker,
            SearchResultHandler searchResultHandler,
            CapabilitiesVMFactory capabilitiesVMFactory,
            SocketsManager socketsManager, Acceptor acceptor,
            MessagesSupportedVendorMessage supportedVendorMessage,
            Provider<SimppManager> simppManager, Provider<UpdateHandler> updateHandler,
            Provider<ConnectionServices> connectionServices, GuidMapManager guidMapManager, SpamFilterFactory spamFilterFactory,
            MessageReaderFactory messageReaderFactory,
            MessageFactory messageFactory,
            ApplicationServices applicationServices,
            SecureMessageVerifier secureMessageVerifier,
            NetworkInstanceUtils networkInstanceUtils) {
        super("60.76.5.3", 4444, ConnectType.PLAIN, connectionManager, networkManager, queryRequestFactory, headersFactory, handshakeResponderFactory, queryReplyFactory, messageDispatcher, networkUpdateSanityChecker, searchResultHandler, capabilitiesVMFactory, socketsManager, acceptor, supportedVendorMessage, simppManager, updateHandler, connectionServices, guidMapManager, spamFilterFactory, messageReaderFactory, messageFactory, applicationServices, secureMessageVerifier, null, networkInstanceUtils);

        this.queriesMustBeInRoutingTables = queriesMustBeInRoutingTable;
        this.connections = connections;
    }

    
    /**
     * Override the stability check method -- assume we're always stable.
     */
    @Override
    public boolean isStable(long time) {
        return true;
    }
    
    /**
     * Accessor for the <tt>QueryRouteTable</tt> instance.
     */
    public QueryRouteTable getQueryRouteTable() {
        return QRT;
    }

    @Override
    public void originateQuery(QueryRequest query) {
        send(query);
    }

    /**
     * Overridden to keep track of messages sent.
     */
    @Override
    public void send(Message msg) {         
        if(msg instanceof ResetTableMessage) {
            QRT.reset((ResetTableMessage)msg);
        } else if(msg instanceof PatchTableMessage) {
            try {
                QRT.patch((PatchTableMessage)msg);
            } catch (BadPacketException e) {
                throw new IllegalArgumentException("should not have received a bad packet");
            }
        }

        if(!(msg instanceof QueryRequest)) return;
        
        receivedQuery = true;
        numQueries++;
        QueryRequest qr = (QueryRequest)msg;
        if(queriesMustBeInRoutingTables && !shouldForwardQuery(qr))  {
            throw new IllegalArgumentException("received query that's not in table1: "+qr+" "+this);
        }
        int ttl = qr.getTTL();
        totalTTL += ttl;
    }
    
    public int getNumQueries() {
        return numQueries;
    }
    
    public int getTotalTTL() {
        return totalTTL;
    }
    
    public boolean receivedQuery() {
        return receivedQuery;
    }
    

    private final AtomicReference<ConnectionCapabilities> connectionCapabilitiesRef = new AtomicReference<ConnectionCapabilities>();
    @Override
    public ConnectionCapabilities getConnectionCapabilities() {
        if (connectionCapabilitiesRef.get() == null)
            connectionCapabilitiesRef.compareAndSet(null, new StubCapabilities(super
                    .getConnectionCapabilities()));
        return connectionCapabilitiesRef.get();
    }
    
    
    private class StubCapabilities extends ConnectionCapabilitiesDelegator {
        public StubCapabilities(ConnectionCapabilities delegate) {
            super(delegate);
        }


        @Override
        public int getNumIntraUltrapeerConnections() {
            return TestConnection.this.connections;
        }
        
        @Override
        public boolean isUltrapeerQueryRoutingConnection() {
            return false;
        }
    }
}







