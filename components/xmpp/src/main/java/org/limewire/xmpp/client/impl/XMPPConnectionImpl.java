package org.limewire.xmpp.client.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
    /**
     * read locking when calling methods on <code>connection</code>
     * write locking when setting the value of <code>connection</code>
     */
    private final ReadWriteLock connectionLock;
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

        connectionLock = new ReentrantReadWriteLock();
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

    public void setMode(Presence.Mode mode) throws XMPPException {
        connectionLock.readLock().lock();
        try {
            checkLoggedIn();
            connection.sendPacket(getPresenceForMode(mode));
        } catch (org.jivesoftware.smack.XMPPException e) {
            throw new XMPPException(e);
        } finally {
            connectionLock.readLock().unlock();
        }
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
        connectionLock.writeLock().lock();        
        try {
            connectionBroadcaster.broadcast(new XMPPConnectionEvent(this, XMPPConnectionEvent.Type.CONNECTING));        
            loggingIn.set(true);
            org.jivesoftware.smack.XMPPConnection.addConnectionCreationListener(smackConnectionListener);
            org.jivesoftware.smack.XMPPConnection.DEBUG_ENABLED = configuration.isDebugEnabled();
            ConnectionConfiguration connectionConfig = ConnectionConfigurationFactory.getConnectionConfigurationFromDNS(configuration);
            connection = new org.jivesoftware.smack.XMPPConnection(connectionConfig);
            connection.addRosterListener(new RosterListenerImpl());
            LOG.infof("connecting to {0} at {1}:{2} ...", connectionConfig.getServiceName(), connectionConfig.getHost(), connectionConfig.getPort());
            connection.connect();
            LOG.infof("connected.");
            LOG.infof("logging in {0} with resource: {1} ...", configuration.getUserInputLocalID(), configuration.getResource());
            connection.login(configuration.getUserInputLocalID(), configuration.getPassword(), configuration.getResource());
            LOG.infof("logged in.");
            connectionBroadcaster.broadcast(new XMPPConnectionEvent(this, XMPPConnectionEvent.Type.CONNECTED));
        } catch (org.jivesoftware.smack.XMPPException e) {
            connectionBroadcaster.broadcast(new XMPPConnectionEvent(this, XMPPConnectionEvent.Type.CONNECT_FAILED, e));
            if(connection != null && connection.isConnected()) {
                connection.disconnect();
            }
            org.jivesoftware.smack.XMPPConnection.removeConnectionCreationListener(smackConnectionListener);
            connection = null;
            throw new XMPPException(e);
        } finally {
            loggingIn.set(false);
            connectionLock.writeLock().unlock();
        }
    }

    @Override
    public boolean isLoggingIn() {
        return loggingIn.get();
    }

    public void logout() {
        connectionLock.writeLock().lock();
        try {
            if(isLoggedIn()) {
                LOG.infof("disconnecting from {0} at {1}:{2} ...", connection.getServiceName(), connection.getHost(), connection.getPort());
                connection.disconnect();
                synchronized (users) {
                    users.clear();
                }
                XMPPConnection.removeConnectionCreationListener(smackConnectionListener);
                connection = null;
                LOG.info("disconnected.");
            }
        } finally {
            connectionLock.writeLock().unlock();
        }
    }

    public boolean isLoggedIn() {
        connectionLock.readLock().lock();
        try {
            return connection != null && connection.isAuthenticated();
        } finally {
            connectionLock.readLock().unlock();
        }
    }
    
    private void checkLoggedIn() throws XMPPException {
        connectionLock.readLock().lock();
        try {
            if(!isLoggedIn()) {
                throw new XMPPException("not connected");
            }
        } finally {
            connectionLock.readLock().unlock();
        }
    }

    private class RosterListenerImpl implements org.jivesoftware.smack.RosterListener {

        public void entriesAdded(Collection<String> addedIds) {
            connectionLock.readLock().lock();
            try {
                if(isLoggedIn()) {
                    synchronized (users) {
                        Roster roster = connection.getRoster();
                        for(String id : addedIds) {
                            RosterEntry rosterEntry = roster.getEntry(id);
                            UserImpl user = new UserImpl(id, rosterEntry, configuration, connection, discoInfoListener);
                            LOG.debugf("user {0} added", user);
                            users.put(id, user);
                            rosterListeners.broadcast(new RosterEvent(user, User.EventType.USER_ADDED));
                        }
                    }
                }
            } catch (org.jivesoftware.smack.XMPPException e) {
                LOG.debugf(e, "error getting roster");    
            } finally {
                connectionLock.readLock().unlock();
            }
        }

        public void entriesUpdated(Collection<String> updatedIds) {
            connectionLock.readLock().lock();
            try {
                if(isLoggedIn()) {
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
                            LOG.debugf("user {0} updated", user);
                            rosterListeners.broadcast(new RosterEvent(user, User.EventType.USER_UPDATED));
                        }
                    }
                }
            } catch (org.jivesoftware.smack.XMPPException e) {
                LOG.debugf(e, "error getting roster");    
            } finally {
                connectionLock.readLock().unlock();
            }
        }

        public void entriesDeleted(Collection<String> removedIds) {
            synchronized (users) {
                for(String id : removedIds) {
                    User user = users.remove(id);
                    if(user != null) {
                        LOG.debugf("user {0} removed", user);
                        rosterListeners.broadcast(new RosterEvent(user, User.EventType.USER_DELETED));
                    }
                }
            }
        }

        public void presenceChanged(final org.jivesoftware.smack.packet.Presence presence) {
            String localJID;
            try {
                localJID = getLocalJid();
            } catch (XMPPException e) {
                localJID = null;
            }
            if(!presence.getFrom().equals(localJID)) {
                UserImpl user = getUser(presence);
                if(user != null) {
                    LOG.debugf("presence from {0} changed to {1}", presence.getFrom(), presence.getType());
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
                } else {
                    LOG.debugf("no user for presence {0}", presence.getFrom());    
                }
            }
        }

        private UserImpl getUser(org.jivesoftware.smack.packet.Presence presence) {
            UserImpl user;
            synchronized (users) {
                user = users.get(StringUtils.parseBareAddress(presence.getFrom()));
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
        connectionLock.readLock().lock();
        try {
            checkLoggedIn();
            Roster roster = connection.getRoster();
            if(roster != null) {
                // TODO smack enhancement
                // TODO to support notifications when
                // TODO the Roster is created
                roster.createEntry(id, name, null);
            }
        } catch (org.jivesoftware.smack.XMPPException e) {
            throw new XMPPException(e);
        } finally {
            connectionLock.readLock().unlock();
        }
    }
    
    public void removeUser(String id) throws XMPPException {
        connectionLock.readLock().lock();
        try {
            checkLoggedIn();
            Roster roster = connection.getRoster();
            if(roster != null) {
                // TODO smack enhancement
                // TODO to support notifications when
                // TODO the Roster is created

                RosterEntry entry = roster.getEntry(id);
                if(entry!= null) {
                    roster.removeEntry(entry);    
                }
            }
        } catch (org.jivesoftware.smack.XMPPException e) {
            throw new XMPPException(e);
        } finally {
            connectionLock.readLock().unlock();
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

    public void sendPacket(Packet packet) throws XMPPException {
        connectionLock.readLock().lock(); 
        try {
            checkLoggedIn();
            connection.sendPacket(packet);
        } catch (org.jivesoftware.smack.XMPPException e) {
            throw new XMPPException(e);
        } finally {
            connectionLock.readLock().unlock();
        }
    }

    public String getLocalJid() throws XMPPException {
        connectionLock.readLock().lock();
        try {
            checkLoggedIn();
            return connection.getUser();
        } finally {
            connectionLock.readLock().unlock();
        }
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
            
            SubscriptionListener sub = new SubscriptionListener(connection,
                                                friendRequestBroadcaster);
            connection.addPacketListener(sub, sub);
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
