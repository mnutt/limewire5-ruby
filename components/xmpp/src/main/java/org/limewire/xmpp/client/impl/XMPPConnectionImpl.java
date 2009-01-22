package org.limewire.xmpp.client.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.ChatStateManager;

import org.limewire.core.api.friend.feature.FeatureEvent;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventListenerList;
import org.limewire.listener.EventMulticaster;
import org.limewire.listener.EventRebroadcaster;
import org.limewire.listener.ListenerSupport;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.net.ConnectBackRequestedEvent;
import org.limewire.net.address.AddressEvent;
import org.limewire.net.address.AddressFactory;
import org.limewire.xmpp.api.client.FileOfferEvent;
import org.limewire.xmpp.api.client.FriendRequestEvent;
import org.limewire.xmpp.api.client.LibraryChangedEvent;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.RosterEvent;
import org.limewire.xmpp.api.client.User;
import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;
import org.limewire.xmpp.api.client.XMPPException;
import org.limewire.xmpp.client.impl.features.FileOfferInitializer;
import org.limewire.xmpp.client.impl.features.LibraryChangedNotifierFeatureInitializer;
import org.limewire.xmpp.client.impl.features.LimewireFeatureInitializer;
import org.limewire.xmpp.client.impl.messages.address.AddressIQListener;
import org.limewire.xmpp.client.impl.messages.address.AddressIQProvider;
import org.limewire.xmpp.client.impl.messages.authtoken.AuthTokenIQListener;
import org.limewire.xmpp.client.impl.messages.authtoken.AuthTokenIQProvider;
import org.limewire.xmpp.client.impl.messages.connectrequest.ConnectBackRequestIQ;
import org.limewire.xmpp.client.impl.messages.connectrequest.ConnectBackRequestIQListener;
import org.limewire.xmpp.client.impl.messages.connectrequest.ConnectBackRequestIQProvider;
import org.limewire.xmpp.client.impl.messages.discoinfo.DiscoInfoListener;
import org.limewire.xmpp.client.impl.messages.filetransfer.FileTransferIQ;
import org.limewire.xmpp.client.impl.messages.filetransfer.FileTransferIQListener;
import org.limewire.xmpp.client.impl.messages.library.LibraryChangedIQ;
import org.limewire.xmpp.client.impl.messages.library.LibraryChangedIQListener;

import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.Type;

public class XMPPConnectionImpl implements org.limewire.xmpp.api.client.XMPPConnection {

    private static final Log LOG = LogFactory.getLog(XMPPConnectionImpl.class);

    private final XMPPConnectionConfiguration configuration;
    private final EventBroadcaster<FileOfferEvent> fileOfferBroadcaster;
    private final EventBroadcaster<FriendRequestEvent> friendRequestBroadcaster;
    private final EventBroadcaster<LibraryChangedEvent> libraryChangedEventEventBroadcaster;
    private final EventBroadcaster<XMPPConnectionEvent> connectionBroadcaster;
    private final AddressFactory addressFactory;
    private final XMPPAuthenticator authenticator;
    private final EventMulticaster<FeatureEvent> featureSupport;
    private final EventBroadcaster<ConnectBackRequestedEvent> connectRequestEventBroadcaster;
    private final XMPPAddressRegistry xmppAddressRegistry;
    private final ListenerSupport<AddressEvent> addressListenerSupport;

    private final EventListenerList<RosterEvent> rosterListeners;
    private final Map<String, UserImpl> users;
    private final SmackConnectionListener smackConnectionListener;
    private final AtomicBoolean loggingIn = new AtomicBoolean(false);

    private volatile org.jivesoftware.smack.XMPPConnection connection;
    private volatile DiscoInfoListener discoInfoListener;

    XMPPConnectionImpl(XMPPConnectionConfiguration configuration,
                       EventBroadcaster<RosterEvent> rosterBroadcaster,
                       EventBroadcaster<FileOfferEvent> fileOfferBroadcaster,
                       EventBroadcaster<FriendRequestEvent> friendRequestBroadcaster,
                       EventBroadcaster<LibraryChangedEvent> libraryChangedEventEventBroadcaster,
                       EventBroadcaster<XMPPConnectionEvent> connectionBroadcaster,
                       AddressFactory addressFactory, XMPPAuthenticator authenticator,
                       EventMulticaster<FeatureEvent> featureSupport,
                       EventBroadcaster<ConnectBackRequestedEvent> connectRequestEventBroadcaster,
                       XMPPAddressRegistry xmppAddressRegistry, 
                       ListenerSupport<AddressEvent> addressListenerSupport) {
        this.configuration = configuration;
        this.fileOfferBroadcaster = fileOfferBroadcaster;
        this.friendRequestBroadcaster = friendRequestBroadcaster;
        this.libraryChangedEventEventBroadcaster = libraryChangedEventEventBroadcaster;
        this.connectionBroadcaster = connectionBroadcaster;
        this.addressFactory = addressFactory;
        this.authenticator = authenticator;
        this.featureSupport = featureSupport;
        this.connectRequestEventBroadcaster = connectRequestEventBroadcaster;
        this.xmppAddressRegistry = xmppAddressRegistry;
        this.addressListenerSupport = addressListenerSupport;

        rosterListeners = new EventListenerList<RosterEvent>();
        // FIXME: this is only used by tests
        if(configuration.getRosterListener() != null) {
            rosterListeners.addListener(configuration.getRosterListener());
        }
        rosterListeners.addListener(new EventRebroadcaster<RosterEvent>(rosterBroadcaster));
        users = new TreeMap<String, UserImpl>(String.CASE_INSENSITIVE_ORDER);
        
        smackConnectionListener = new SmackConnectionListener();
    }
    
    @Override
    public String toString() {
        return org.limewire.util.StringUtils.toString(this, configuration, connection);
    }

    public void setMode(Presence.Mode mode) {
        connection.sendPacket(getPresenceForMode(mode));
    }

    private Packet getPresenceForMode(Presence.Mode mode) {
        org.jivesoftware.smack.packet.Presence presence = new org.jivesoftware.smack.packet.Presence(
                org.jivesoftware.smack.packet.Presence.Type.available);
        presence.setMode(org.jivesoftware.smack.packet.Presence.Mode.valueOf(mode.name()));
        return presence;
    }

    public XMPPConnectionConfiguration getConfiguration() {
        return configuration;
    }

    public void login() throws XMPPException {
        connectionBroadcaster.broadcast(new XMPPConnectionEvent(this, XMPPConnectionEvent.Type.CONNECTING));
        loggingIn.set(true);
        synchronized (this) {
            try {
                org.jivesoftware.smack.XMPPConnection.addConnectionCreationListener(smackConnectionListener);
                org.jivesoftware.smack.XMPPConnection.DEBUG_ENABLED = configuration.isDebugEnabled();
                ConnectionConfiguration connectionConfig = getConnectionConfig();
                connection = new org.jivesoftware.smack.XMPPConnection(connectionConfig);
                connection.addRosterListener(new RosterListenerImpl(connection));
                if (LOG.isInfoEnabled())
                    LOG.info("connecting to " + connectionConfig.getServiceName() + " at " + connectionConfig.getHost() + ":" + connectionConfig.getPort() + "...");
                connection.connect();
                SubscriptionListener sub = new SubscriptionListener(connection,
                                                    friendRequestBroadcaster);
                connection.addPacketListener(sub, sub);
                LOG.info("connected.");
                if (LOG.isInfoEnabled())
                    LOG.infof("logging in " + configuration.getUserInputLocalID() + " with resource: " + configuration.getResource());
                connection.login(configuration.getUserInputLocalID(), configuration.getPassword(), configuration.getResource());
                LOG.info("logged in.");
                connectionBroadcaster.broadcast(new XMPPConnectionEvent(this, XMPPConnectionEvent.Type.CONNECTED));
            } catch (org.jivesoftware.smack.XMPPException e) {
                connectionBroadcaster.broadcast(new XMPPConnectionEvent(this, XMPPConnectionEvent.Type.CONNECT_FAILED, e));
                if(connection != null && connection.isConnected()) {
                    connection.disconnect();
                }
                org.jivesoftware.smack.XMPPConnection.removeConnectionCreationListener(smackConnectionListener);
                throw new XMPPException(e);
            } finally {
                loggingIn.set(false);
            }
        }
    }

    @Override
    public boolean isLoggingIn() {
        return loggingIn.get();
    }

    public void logout() {
        synchronized (this) {
            if(connection != null && connection.isAuthenticated()) {
                LOG.infof("disconnecting from {0} at {1}:{2} ...", connection.getServiceName(), connection.getHost(), connection.getPort());
                connection.disconnect();
                synchronized (users) {
                    users.clear();
                }
                XMPPConnection.removeConnectionCreationListener(smackConnectionListener);
                connection = null;
                LOG.info("disconnected.");
            }
        }
    }

    public boolean isLoggedIn() {
        return connection != null && connection.isAuthenticated();
    }

    /**
     * Converts a LimeWire XMPPConnectionConfiguration into a Smack
     * ConnectionConfiguration, trying to obtain the hostname from DNS SRV
     * as per RFC 3920 and falling back to the service name and default port
     * if the SRV lookup fails. This method blocks during the DNS lookup.
     */
    private ConnectionConfiguration getConnectionConfig() {
        String host = configuration.getServiceName(); // Fallback
        int port = 5222; // Default XMPP client port
        String serviceName = configuration.getServiceName();
        try {
            String domain = "_xmpp-client._tcp." + serviceName;
            Lookup lookup = new Lookup(domain, Type.SRV);
            Record[] answers = lookup.run();
            int result = lookup.getResult();
            if(result == Lookup.SUCCESSFUL && answers != null) {
                // RFC 2782: use the server with the lowest-numbered priority,
                // break ties by preferring servers with higher weights
                int lowestPriority = Integer.MAX_VALUE;
                int highestWeight = Integer.MIN_VALUE;
                for(Record rec : answers) {
                    if(rec instanceof SRVRecord) {
                        SRVRecord srvRec = (SRVRecord)rec;
                        int priority = srvRec.getPriority();
                        int weight = srvRec.getWeight();
                        if(priority < lowestPriority
                                && weight > highestWeight) {
                            port = srvRec.getPort();
                            host = srvRec.getTarget().toString();
                            lowestPriority = priority;
                            highestWeight = weight;
                        }
                    }
                }
            }
        } catch(IOException iox) {
            // Failure looking up the SRV record - use the service name
            LOG.debug("Failed to look up SRV record", iox);
        }
        return new ConnectionConfiguration(host, port, serviceName);
    }

    private class RosterListenerImpl implements org.jivesoftware.smack.RosterListener {
        private final org.jivesoftware.smack.XMPPConnection connection;

        public RosterListenerImpl(org.jivesoftware.smack.XMPPConnection connection) {
            this.connection = connection;
        }

        public void entriesAdded(Collection<String> addedIds) {
            synchronized (users) {
                Roster roster = connection.getRoster();
                for(String id : addedIds) {
                    RosterEntry rosterEntry = roster.getEntry(id);
                    UserImpl user = new UserImpl(id, rosterEntry, configuration, connection, discoInfoListener);
                    if(LOG.isDebugEnabled()) {
                        LOG.debug("user " + user + " added");
                    }
                    users.put(id, user);
                    rosterListeners.broadcast(new RosterEvent(user, User.EventType.USER_ADDED));
                }
                users.notifyAll();
            }
        }

        public void entriesUpdated(Collection<String> updatedIds) {
            synchronized (users) {
                for(String id : updatedIds) {
                    Roster roster = connection.getRoster();
                    RosterEntry rosterEntry = roster.getEntry(id);
                    UserImpl user = users.get(id);
                    if(user == null) {
                        // should never happen ?
                        user = new UserImpl(id, rosterEntry, configuration, connection, discoInfoListener);
                        users.put(id, user);
                    } else {
                        user.setRosterEntry(rosterEntry);
                    }
                    if(LOG.isDebugEnabled()) {
                        LOG.debug("user " + user + " updated");
                    }
                    rosterListeners.broadcast(new RosterEvent(user, User.EventType.USER_UPDATED));
                }
                users.notifyAll();
            }
        }

        public void entriesDeleted(Collection<String> removedIds) {
            synchronized (users) {
                for(String id : removedIds) {
                    User user = users.remove(id);
                    if(user != null) {
                        if(LOG.isDebugEnabled()) {
                            LOG.debug("user " + user + " removed");
                        }
                        rosterListeners.broadcast(new RosterEvent(user, User.EventType.USER_DELETED));
                    }
                }
                users.notifyAll();
            }
        }

        public void presenceChanged(final org.jivesoftware.smack.packet.Presence presence) {
            if(!presence.getFrom().equals(connection.getUser())) {
                UserImpl user = getUser(presence);
                if(LOG.isDebugEnabled()) {
                    LOG.debug("presence.from " + presence.getFrom() + " changed to " + presence.getType());
                }
                synchronized (user) {
                    if (presence.getType().equals(org.jivesoftware.smack.packet.Presence.Type.available)) {
                        if(!user.getFriendPresences().containsKey(presence.getFrom())) {
                            addNewPresence(user, presence);
                        } else {
                            updatePresence(user, presence);
                        }
                    } else if (presence.getType().equals(org.jivesoftware.smack.packet.Presence.Type.unavailable)) {
                        PresenceImpl p = (PresenceImpl)user.getPresence(presence.getFrom());
                        if(p != null) {
                            p.update(presence);
                            user.removePresense(p);
                        }
                    }
                }
            }
        }

        private UserImpl getUser(org.jivesoftware.smack.packet.Presence presence) {
            UserImpl user;
            synchronized (users) {
                user = users.get(StringUtils.parseBareAddress(presence.getFrom()));
                while(user == null) {
                    try {
                        LOG.debugf("presence {0} waiting for roster entry for user {1} ...",
                                presence.getFrom(), StringUtils.parseBareAddress(presence.getFrom()));
                        users.wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    user = users.get(StringUtils.parseBareAddress(presence.getFrom()));
                    if(user != null) {
                        if (LOG.isDebugEnabled())
                            LOG.debugf("found user {0} for presence {1}",
                                StringUtils.parseBareAddress(presence.getFrom()), presence.getFrom());
                    }
                }

            }
            return user;
        }

        private void addNewPresence(final UserImpl user, final org.jivesoftware.smack.packet.Presence presence) {
            final PresenceImpl presenceImpl = new PresenceImpl(presence, user, featureSupport);
            user.addPresense(presenceImpl);
        }

        private void updatePresence(UserImpl user, org.jivesoftware.smack.packet.Presence presence) {
            PresenceImpl currentPresence = (PresenceImpl)user.getFriendPresences().get(presence.getFrom());
            currentPresence.update(presence);
            user.updatePresence(currentPresence);
        }
    }
    

    public void addUser(String id, String name) throws XMPPException {
        Roster roster = connection.getRoster();
        if(roster != null) {
            // TODO smack enhancement
            // TODO to support notifications when 
            // TODO the Roster is created

            try {
                roster.createEntry(id, name, null);
            } catch (org.jivesoftware.smack.XMPPException e) {
                throw new XMPPException(e);
            }
        }
    }
    
    public void removeUser(String id) throws XMPPException {
        Roster roster = connection.getRoster();
        if(roster != null) {
            // TODO smack enhancement
            // TODO to support notifications when 
            // TODO the Roster is created

            try {
                RosterEntry entry = roster.getEntry(id);
                if(entry!= null) {
                    roster.removeEntry(entry);    
                }                
            } catch (org.jivesoftware.smack.XMPPException e) {
                throw new XMPPException(e);
            }
        }
    }

    @Override
    public User getUser(String id) {
        synchronized (users) { 
            return users.get(id);
        }
    }

    @Override
    public Collection<User> getUsers() {
        synchronized (users) { 
            return new ArrayList<User>(users.values());
        }
    }

    public void sendPacket(Packet packet) {
        synchronized (this) {
            if (connection.isConnected()) {
                connection.sendPacket(packet);
            }
        }
    }

    public String getLocalJid() {
        return connection.getUser();
    }
    
    private class SmackConnectionListener implements ConnectionListener, ConnectionCreationListener {
        private volatile AddressIQListener addressIQListener;

        @Override
        public void connectionCreated(XMPPConnection connection) {
            if(XMPPConnectionImpl.this.connection != connection) {
                return;
            }

            if(LOG.isDebugEnabled()) {
                LOG.debug("connection created for "+ connection.toString());
            }
            connection.addConnectionListener(this);

            synchronized (ProviderManager.getInstance()) {
                if(ProviderManager.getInstance().getIQProvider("address", "jabber:iq:lw-address") == null) {
                    ProviderManager.getInstance().addIQProvider("address", "jabber:iq:lw-address", new AddressIQProvider(addressFactory));
                }
                if(ProviderManager.getInstance().getIQProvider("file-transfer", "jabber:iq:lw-file-transfer") == null) {
                    ProviderManager.getInstance().addIQProvider("file-transfer", "jabber:iq:lw-file-transfer", FileTransferIQ.getIQProvider());
                }
                if(ProviderManager.getInstance().getIQProvider("auth-token", "jabber:iq:lw-auth-token") == null) {
                    ProviderManager.getInstance().addIQProvider("auth-token", "jabber:iq:lw-auth-token", new AuthTokenIQProvider());
                }
                if(ProviderManager.getInstance().getIQProvider("library-changed", "jabber:iq:lw-lib-change") == null) {
                    ProviderManager.getInstance().addIQProvider("library-changed", "jabber:iq:lw-lib-change", LibraryChangedIQ.getIQProvider());
                }
                if (ProviderManager.getInstance().getIQProvider(ConnectBackRequestIQ.ELEMENT_NAME, ConnectBackRequestIQ.NAME_SPACE) == null) {
                    ProviderManager.getInstance().addIQProvider(ConnectBackRequestIQ.ELEMENT_NAME, ConnectBackRequestIQ.NAME_SPACE, new ConnectBackRequestIQProvider());
                }
            }

            ChatStateManager.getInstance(connection);

            discoInfoListener = new DiscoInfoListener(XMPPConnectionImpl.this, connection);
            rosterListeners.addListener(discoInfoListener.getRosterListener());
            connection.addPacketListener(discoInfoListener, discoInfoListener.getPacketFilter());

            addressIQListener = new AddressIQListener(XMPPConnectionImpl.this, addressFactory, discoInfoListener, xmppAddressRegistry);
            addressListenerSupport.addListener(addressIQListener);
            connection.addPacketListener(addressIQListener, addressIQListener.getPacketFilter());

            FileTransferIQListener fileTransferIQListener = new FileTransferIQListener(fileOfferBroadcaster);
            connection.addPacketListener(fileTransferIQListener, fileTransferIQListener.getPacketFilter());

            AuthTokenIQListener authTokenIQListener = new AuthTokenIQListener(XMPPConnectionImpl.this, authenticator, discoInfoListener);
            connection.addPacketListener(authTokenIQListener, authTokenIQListener.getPacketFilter());

            LibraryChangedIQListener libChangedIQListener = new LibraryChangedIQListener(libraryChangedEventEventBroadcaster, XMPPConnectionImpl.this);
            connection.addPacketListener(libChangedIQListener, libChangedIQListener.getPacketFilter());

            ConnectBackRequestIQListener connectRequestIQListener = new ConnectBackRequestIQListener(connectRequestEventBroadcaster, discoInfoListener);
            connection.addPacketListener(connectRequestIQListener, connectRequestIQListener.getPacketFilter());
            
            new FileOfferInitializer(connection).register(discoInfoListener);
            new LibraryChangedNotifierFeatureInitializer(connection).register(discoInfoListener);
            new LimewireFeatureInitializer().register(discoInfoListener);
        }

        @Override
        public void connectionClosed() {
            connectionBroadcaster.broadcast(new XMPPConnectionEvent(XMPPConnectionImpl.this, XMPPConnectionEvent.Type.DISCONNECTED));
            cleanup();
        }

        @Override
        public void connectionClosedOnError(Exception e) {
            connectionBroadcaster.broadcast(new XMPPConnectionEvent(XMPPConnectionImpl.this, XMPPConnectionEvent.Type.DISCONNECTED, e));
            cleanup();
        }

        @Override
        public void reconnectingIn(int seconds) {
        }

        @Override
        public void reconnectionFailed(Exception e) {
        }

        @Override
        public void reconnectionSuccessful() {
        }
        
        void cleanup() {
            ChatStateManager.remove(connection);
            if(discoInfoListener != null) {
                rosterListeners.removeListener(discoInfoListener.getRosterListener());
            }
            if(addressIQListener != null) {
                addressListenerSupport.removeListener(addressIQListener);    
            }
        }
    }
}
