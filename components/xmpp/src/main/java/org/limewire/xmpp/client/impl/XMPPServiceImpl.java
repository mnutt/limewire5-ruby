package org.limewire.xmpp.client.impl;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.util.StringUtils;
import org.limewire.core.api.friend.feature.FeatureEvent;
import org.limewire.core.api.friend.feature.features.ConnectBackRequestFeature;
import org.limewire.io.Connectable;
import org.limewire.io.GUID;
import org.limewire.lifecycle.Asynchronous;
import org.limewire.lifecycle.Service;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventMulticaster;
import org.limewire.listener.ListenerSupport;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.net.ConnectBackRequestedEvent;
import org.limewire.net.address.AddressEvent;
import org.limewire.net.address.AddressFactory;
import org.limewire.xmpp.activity.XmppActivityEvent;
import org.limewire.xmpp.api.client.ConnectBackRequestSender;
import org.limewire.xmpp.api.client.FileOfferEvent;
import org.limewire.xmpp.api.client.FriendRequestEvent;
import org.limewire.xmpp.api.client.JabberSettings;
import org.limewire.xmpp.api.client.LibraryChangedEvent;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.Presence.Mode;
import org.limewire.xmpp.api.client.RosterEvent;
import org.limewire.xmpp.api.client.User;
import org.limewire.xmpp.api.client.XMPPConnection;
import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;
import org.limewire.xmpp.api.client.XMPPException;
import org.limewire.xmpp.api.client.XMPPService;
import org.limewire.xmpp.client.impl.messages.connectrequest.ConnectBackRequestIQ;

import com.google.inject.Inject;
import com.google.inject.Singleton;


@Singleton
public class XMPPServiceImpl implements Service, XMPPService, ConnectBackRequestSender {

    private static final Log LOG = LogFactory.getLog(XMPPServiceImpl.class);
    
    private final EventBroadcaster<RosterEvent> rosterBroadcaster;
    private final EventBroadcaster<FileOfferEvent> fileOfferBroadcaster;
    private final EventBroadcaster<FriendRequestEvent> friendRequestBroadcaster;
    private final EventBroadcaster<LibraryChangedEvent> libraryChangedBroadcaster;
    private final AddressFactory addressFactory;
    private final EventMulticaster<XMPPConnectionEvent> connectionBroadcaster;
    private final XMPPAuthenticator authenticator;
    private final EventMulticaster<FeatureEvent> featureSupport;
    private final EventBroadcaster<ConnectBackRequestedEvent> connectRequestEventBroadcaster;
    private final XMPPAddressRegistry xmppAddressRegistry;
    private final JabberSettings jabberSettings;
    private final ListenerSupport<AddressEvent> addressListenerSupport;

    // Connections that are logged in or logging in
    final List<XMPPConnectionImpl> connections;
    private boolean multipleConnectionsAllowed;   

    @Inject
    public XMPPServiceImpl(EventBroadcaster<RosterEvent> rosterBroadcaster,
            EventBroadcaster<FileOfferEvent> fileOfferBroadcaster,
            EventBroadcaster<FriendRequestEvent> friendRequestBroadcaster,
            EventBroadcaster<LibraryChangedEvent> libraryChangedBroadcaster,
            EventMulticaster<XMPPConnectionEvent> connectionBroadcaster,
            AddressFactory addressFactory, XMPPAuthenticator authenticator,
            EventMulticaster<FeatureEvent> featureSupport,
            EventBroadcaster<ConnectBackRequestedEvent> connectRequestEventBroadcaster,
            XMPPAddressRegistry xmppAddressRegistry,
            JabberSettings jabberSettings,
            ListenerSupport<AddressEvent> addressListenerSupport) {
        this.rosterBroadcaster = rosterBroadcaster;
        this.fileOfferBroadcaster = fileOfferBroadcaster;
        this.friendRequestBroadcaster = friendRequestBroadcaster;
        this.libraryChangedBroadcaster = libraryChangedBroadcaster;
        this.connectionBroadcaster = connectionBroadcaster;
        this.addressFactory = addressFactory;
        this.authenticator = authenticator;
        this.featureSupport = featureSupport;
        this.connectRequestEventBroadcaster = connectRequestEventBroadcaster;
        this.xmppAddressRegistry = xmppAddressRegistry;
        this.jabberSettings = jabberSettings;
        this.addressListenerSupport = addressListenerSupport;

        connections = new CopyOnWriteArrayList<XMPPConnectionImpl>();
        multipleConnectionsAllowed = false;
        connectionBroadcaster.addListener(new ReconnectionManager(this));
        // We'll install our own subscription listeners
        Roster.setDefaultSubscriptionMode(Roster.SubscriptionMode.manual);
    }

    @Inject
    void register(org.limewire.lifecycle.ServiceRegistry registry) {
        registry.register(this);
    }
    
    /**
     * Sets the connection mode to idle (extended away) after receiving activity
     * events triggered by periods of inactivity
     * @param listenerSupport
     */
    @Inject
    void register(ListenerSupport<XmppActivityEvent> listenerSupport) {
        listenerSupport.addListener(new EventListener<XmppActivityEvent>() {
            @Override
            public void handleEvent(XmppActivityEvent event) {
                switch(event.getSource()) {
                case Idle:
                    try {
                        setMode(Mode.xa);
                    } catch (XMPPException e) {
                        LOG.debugf(e, "couldn't set mode based on {0}", event);
                    }
                    break;
                case Active:
                    try {
                        setMode(jabberSettings.isDoNotDisturbSet() ? Mode.dnd : Mode.available);
                    } catch (XMPPException e) {
                        LOG.debugf(e, "couldn't set mode based on {0}", event);
                    }
                }
            }
        });
    }

    @Override
    public void initialize() {
    }

    @Override
    public void start() {
    }

    /**
     * Logs out all existing connections.
     */
    @Asynchronous
    @Override
    public void stop() {
        logout();
    }

    @Override
    public String getServiceName() {
        return "XMPP";
    }

    @Override
    public XMPPConnection login(XMPPConnectionConfiguration configuration) throws XMPPException {
        return login(configuration, false);
    }

    XMPPConnection login(XMPPConnectionConfiguration configuration, boolean isReconnect) throws XMPPException {
        synchronized (this) {
            if(!multipleConnectionsAllowed) {
                XMPPConnection activeConnection = getActiveConnection();
                if(isReconnect) {
                    if(activeConnection != null) {
                        return activeConnection;
                    }
                } else {
                    if(activeConnection != null && activeConnection.getConfiguration().equals(configuration)) {
                        return activeConnection;
                    } else {
                        logout();
                    }    
                }                
            }
            
            XMPPConnectionImpl connection = new XMPPConnectionImpl(
                    configuration, rosterBroadcaster,
                    fileOfferBroadcaster,
                    friendRequestBroadcaster,
                    libraryChangedBroadcaster,
                    connectionBroadcaster,
                    addressFactory, authenticator, featureSupport,
                    connectRequestEventBroadcaster, 
                    xmppAddressRegistry,
                    addressListenerSupport);
            try {
                connections.add(connection);
                connection.login();
                //maintain the last set login state available or do not disturb
                connection.setMode(jabberSettings.isDoNotDisturbSet() ? Presence.Mode.dnd : Presence.Mode.available);
                return connection;
            } catch(XMPPException e) {
                connections.remove(connection);
                LOG.debug(e.getMessage(), e);
                throw new XMPPException(e);
            }
        }
    }
    
    @Override
    public boolean isLoggedIn() {
        for(XMPPConnection connection : connections) {
            if(connection.isLoggedIn()) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public boolean isLoggingIn() {
        for(XMPPConnection connection : connections) {
            if(connection.isLoggingIn()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void logout() {
        for(XMPPConnection connection : connections) {
            connection.logout();
        }
        connections.clear();
    }

    @Override
    public XMPPConnectionImpl getActiveConnection() {
        if(connections.isEmpty()) {
            return null;
        } else {
            try {
                return connections.get(0);
            } catch(IndexOutOfBoundsException ioobe) {
                return null; // possible because connections is CoW
            }
        }
    }

    // Only for testing
    void setMultipleConnectionsAllowed(boolean allowed) {
        multipleConnectionsAllowed = allowed;
    }

    // Only for testing
    List<? extends XMPPConnection> getConnections() {
        return Collections.unmodifiableList(connections);
    } 
    
    @Override
    public boolean send(String userId, Connectable address, GUID clientGuid, int supportedFWTVersion) {
        LOG.debug("send connect request");
        XMPPConnectionImpl connection = getActiveConnection();
        if (connection == null) {
            return false;
        }
        User user = connection.getUser(StringUtils.parseBareAddress(userId));
        if (user == null) {
            return false;
        }
        Presence presence = user.getPresences().get(userId);
        if (presence == null) {
            return false;
        }
        if (!presence.hasFeatures(ConnectBackRequestFeature.ID)) {
            return false;
        }
        ConnectBackRequestIQ connectRequest = new ConnectBackRequestIQ(address, clientGuid, supportedFWTVersion);
        connectRequest.setTo(userId);
        try {
            connectRequest.setFrom(connection.getLocalJid());
            LOG.debugf("sending request: {0}", connectRequest);
            connection.sendPacket(connectRequest);
        } catch (XMPPException e) {
            LOG.debug("sending connect back request failed", e);
            return false;
        }
        return true;
    }
    
    @Override
    public void setMode(Mode mode) throws XMPPException {
        for(XMPPConnection connection : connections) {
            connection.setMode(mode);
        }
    }
}
