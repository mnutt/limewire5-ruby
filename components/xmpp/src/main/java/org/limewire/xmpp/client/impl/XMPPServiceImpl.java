package org.limewire.xmpp.client.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.util.StringUtils;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.concurrent.ListeningExecutorService;
import org.limewire.concurrent.ListeningFuture;
import org.limewire.core.api.friend.client.ConnectBackRequestSender;
import org.limewire.core.api.friend.client.FriendException;
import org.limewire.core.api.friend.feature.features.ConnectBackRequestFeature;
import org.limewire.core.api.friend.feature.features.LimewireFeature;
import org.limewire.inspection.Inspectable;
import org.limewire.inspection.InspectableContainer;
import org.limewire.inspection.InspectionHistogram;
import org.limewire.inspection.InspectionPoint;
import org.limewire.io.Connectable;
import org.limewire.io.GUID;
import org.limewire.lifecycle.Asynchronous;
import org.limewire.lifecycle.Service;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventMulticaster;
import org.limewire.listener.ListenerSupport;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.xmpp.activity.XmppActivityEvent;
import org.limewire.xmpp.api.client.JabberSettings;
import org.limewire.xmpp.api.client.XMPPPresence;
import org.limewire.xmpp.api.client.XMPPPresence.Mode;
import org.limewire.xmpp.api.client.XMPPFriend;
import org.limewire.xmpp.api.client.XMPPConnection;
import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;
import org.limewire.xmpp.api.client.XMPPService;
import org.limewire.xmpp.client.impl.messages.connectrequest.ConnectBackRequestIQ;

import com.google.inject.Inject;
import com.google.inject.Singleton;


@Singleton
public class XMPPServiceImpl implements Service, XMPPService, ConnectBackRequestSender {

    private static final Log LOG = LogFactory.getLog(XMPPServiceImpl.class);

    private final XMPPConnectionImplFactory connectionImplFactory;
    private final JabberSettings jabberSettings;
    private final ListeningExecutorService executorService;

    // Connections that are logged in or logging in
    final List<XMPPConnectionImpl> connections;
    private boolean multipleConnectionsAllowed;

    @SuppressWarnings("unused")
    @InspectableContainer
    private class LazyInspectableContainer {
        @InspectionPoint("xmpp service")
        private final Inspectable inspectable = new Inspectable() {
            @Override
            public Object inspect() {
                Map<Object, Object> map = new HashMap<Object, Object>();
                map.put("loggedIn", isLoggedIn());
                collectFriendStatistics(map);
                return map;
            }
            
            private void collectFriendStatistics(Map<Object, Object> data) {
                int count = 0;
                XMPPConnection connection = getActiveConnection();
                InspectionHistogram<Integer> presencesHistogram = new InspectionHistogram<Integer>();
                if (connection != null) {
                    for (XMPPFriend user : connection.getFriends()) {
                        Map<String, XMPPPresence> presences = user.getPresences();
                        presencesHistogram.count(presences.size());
                        for (XMPPPresence presence : presences.values()) {
                            if (presence.hasFeatures(LimewireFeature.ID)) {
                                count++;
                                // break from inner presence loop, count each user only once
                                break;
                            }
                        }
                    }
                }
                data.put("limewire friends", count);
                data.put("presences", presencesHistogram.inspect());
            }
        };
    }

    @Inject
    public XMPPServiceImpl(XMPPConnectionImplFactory connectionImplFactory,
                           EventMulticaster<XMPPConnectionEvent> connectionBroadcaster,
            JabberSettings jabberSettings) {
        this.connectionImplFactory = connectionImplFactory;
        this.jabberSettings = jabberSettings;

        connections = new CopyOnWriteArrayList<XMPPConnectionImpl>();
        multipleConnectionsAllowed = false;
        connectionBroadcaster.addListener(new ReconnectionManager(this));
        // We'll install our own subscription listeners
        Roster.setDefaultSubscriptionMode(Roster.SubscriptionMode.manual);
        executorService = ExecutorsHelper.newSingleThreadExecutor(ExecutorsHelper.daemonThreadFactory("XMPPServiceImpl"));
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
                        setModeImpl(Mode.xa);
                    } catch (FriendException e) {
                        LOG.debugf(e, "couldn't set mode based on {0}", event);
                    }
                    break;
                case Active:
                    try {
                        setModeImpl(jabberSettings.isDoNotDisturbSet() ? Mode.dnd : Mode.available);
                    } catch (FriendException e) {
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
        logoutImpl();
    }

    @Override
    public String getServiceName() {
        return "XMPP";
    }

    @Override
    public ListeningFuture<XMPPConnection> login(final XMPPConnectionConfiguration configuration) {
        return executorService.submit(new Callable<XMPPConnection>() {
            @Override
            public XMPPConnection call() throws Exception {
                return loginImpl(configuration);
            }
        }); 
    }

    XMPPConnection loginImpl(XMPPConnectionConfiguration configuration) throws FriendException {
        return loginImpl(configuration, false);
    }

    XMPPConnection loginImpl(XMPPConnectionConfiguration configuration, boolean isReconnect) throws FriendException {
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
                        logoutImpl();
                    }    
                }                
            }
            
            XMPPConnectionImpl connection = connectionImplFactory.createConnection(
                    configuration, executorService);
            try {
                connections.add(connection);
                connection.loginImpl();
                //maintain the last set login state available or do not disturb
                connection.setModeImpl(jabberSettings.isDoNotDisturbSet() ? XMPPPresence.Mode.dnd : XMPPPresence.Mode.available);
                return connection;
            } catch(FriendException e) {
                connections.remove(connection);
                LOG.debug(e.getMessage(), e);
                throw new FriendException(e);
            }
        }
    }

    @Override
    public boolean isLoggedIn() {
        for(XMPPConnectionImpl connection : connections) {
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
    public ListeningFuture<Void> logout() {
        return executorService.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                logoutImpl();
                return null;
            }
        }); 
    }

    void logoutImpl() {
        for(XMPPConnectionImpl connection : connections) {
            connection.logoutImpl();
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
        XMPPFriend user = connection.getFriend(StringUtils.parseBareAddress(userId));
        if (user == null) {
            return false;
        }
        XMPPPresence presence = user.getPresences().get(userId);
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
        } catch (FriendException e) {
            LOG.debug("sending connect back request failed", e);
            return false;
        }
        return true;
    }
    
    @Override
    public ListeningFuture<Void> setMode(final Mode mode) {
        return executorService.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                setModeImpl(mode);
                return null;
            }
        }); 
    }

    private void setModeImpl(Mode mode) throws FriendException {
        for(XMPPConnectionImpl connection : connections) {
            connection.setModeImpl(mode);
        }
    }
    
}
