package org.limewire.xmpp.client.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.Callable;
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
import org.limewire.concurrent.ListeningExecutorService;
import org.limewire.concurrent.ListeningFuture;
import org.limewire.core.api.friend.client.FileOfferEvent;
import org.limewire.core.api.friend.client.FriendException;
import org.limewire.core.api.friend.client.FriendRequestEvent;
import org.limewire.core.api.friend.client.LibraryChangedEvent;
import org.limewire.core.api.friend.feature.FeatureEvent;
import org.limewire.core.api.friend.impl.DefaultFriendAuthenticator;
import org.limewire.core.api.friend.FriendPresenceEvent;
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
import org.limewire.xmpp.api.client.RosterEvent;
import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;
import org.limewire.xmpp.api.client.XMPPFriend;
import org.limewire.xmpp.api.client.XMPPPresence;
import org.limewire.xmpp.client.impl.features.FileOfferInitializer;
import org.limewire.xmpp.client.impl.features.LibraryChangedNotifierFeatureInitializer;
import org.limewire.xmpp.client.impl.features.LimewireFeatureInitializer;
import org.limewire.xmpp.client.impl.features.NoSaveFeatureInitializer;
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
import org.limewire.xmpp.client.impl.messages.nosave.NoSaveIQ;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

public class XMPPConnectionImpl implements org.limewire.xmpp.api.client.XMPPConnection {

    private static final Log LOG = LogFactory.getLog(XMPPConnectionImpl.class);

    private final XMPPConnectionConfiguration configuration;
    private final EventBroadcaster<FileOfferEvent> fileOfferBroadcaster;
    private final EventBroadcaster<FriendRequestEvent> friendRequestBroadcaster;
    private final EventBroadcaster<LibraryChangedEvent> libraryChangedEventEventBroadcaster;
    private final EventMulticaster<XMPPConnectionEvent> connectionMulticaster;
    private final ListenerSupport<FriendPresenceEvent> friendPresenceSupport;
    private final AddressFactory addressFactory;
    private final DefaultFriendAuthenticator authenticator;
    private final EventBroadcaster<FeatureEvent> featureBroadcaster;
    private final EventBroadcaster<ConnectBackRequestedEvent> connectRequestEventBroadcaster;
    private final XMPPAddressRegistry xmppAddressRegistry;
    private final ListenerSupport<AddressEvent> addressListenerSupport;
    private final ListeningExecutorService executorService;
    private final List<ConnectionConfigurationFactory> connectionConfigurationFactories;

    private final EventListenerList<RosterEvent> rosterListeners;
    private final Map<String, XMPPFriend> friends;
    private final SmackConnectionListener smackConnectionListener;
    private final AtomicBoolean loggedIn = new AtomicBoolean(false);
    private final AtomicBoolean loggingIn = new AtomicBoolean(false);

    private volatile org.jivesoftware.smack.XMPPConnection connection;
    private volatile DiscoInfoListener discoInfoListener;

    @Inject
    public XMPPConnectionImpl(@Assisted XMPPConnectionConfiguration configuration,
                       @Assisted ListeningExecutorService executorService,
                       EventBroadcaster<RosterEvent> rosterBroadcaster,
                       EventBroadcaster<FileOfferEvent> fileOfferBroadcaster,
                       EventBroadcaster<FriendRequestEvent> friendRequestBroadcaster,
                       EventBroadcaster<LibraryChangedEvent> libraryChangedEventEventBroadcaster,
                       EventMulticaster<XMPPConnectionEvent> connectionMulticaster,
                       ListenerSupport<FriendPresenceEvent> friendPresenceSupport,
                       AddressFactory addressFactory, DefaultFriendAuthenticator authenticator,
                       EventMulticaster<FeatureEvent> featureSupport,
                       EventBroadcaster<ConnectBackRequestedEvent> connectRequestEventBroadcaster,
                       XMPPAddressRegistry xmppAddressRegistry, 
                       ListenerSupport<AddressEvent> addressListenerSupport,                       
                       List<ConnectionConfigurationFactory> connectionConfigurationFactories) {
        this.configuration = configuration;
        this.fileOfferBroadcaster = fileOfferBroadcaster;
        this.friendRequestBroadcaster = friendRequestBroadcaster;
        this.libraryChangedEventEventBroadcaster = libraryChangedEventEventBroadcaster;
        this.connectionMulticaster = connectionMulticaster;
        this.friendPresenceSupport = friendPresenceSupport;
        this.addressFactory = addressFactory;
        this.authenticator = authenticator;
        this.featureBroadcaster = featureSupport;
        this.connectRequestEventBroadcaster = connectRequestEventBroadcaster;
        this.xmppAddressRegistry = xmppAddressRegistry;
        this.addressListenerSupport = addressListenerSupport;
        this.executorService = executorService;
        this.connectionConfigurationFactories = connectionConfigurationFactories;
        rosterListeners = new EventListenerList<RosterEvent>();
        // FIXME: this is only used by tests
        if(configuration.getRosterListener() != null) {
            rosterListeners.addListener(configuration.getRosterListener());
        }
        rosterListeners.addListener(new EventRebroadcaster<RosterEvent>(rosterBroadcaster));
        friends = new TreeMap<String, XMPPFriend>(String.CASE_INSENSITIVE_ORDER);
        
        smackConnectionListener = new SmackConnectionListener();
    }
    
    @Override
    public String toString() {
        return org.limewire.util.StringUtils.toString(this, configuration, connection);
    }
    
    public ListeningFuture<Void> setMode(final XMPPPresence.Mode mode) {
        return executorService.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                setModeImpl(mode);
                return null;
            }
        });   
    }    

    void setModeImpl(XMPPPresence.Mode mode) throws FriendException {
        synchronized (this) {
            try {
                checkLoggedIn();
                connection.sendPacket(getPresenceForMode(mode));
            } catch (org.jivesoftware.smack.XMPPException e) {
                throw new FriendException(e);
            } 
        }
    }

    private Packet getPresenceForMode(XMPPPresence.Mode mode) {
        org.jivesoftware.smack.packet.Presence presence = new org.jivesoftware.smack.packet.Presence(
                org.jivesoftware.smack.packet.Presence.Type.available);
        presence.setMode(org.jivesoftware.smack.packet.Presence.Mode.valueOf(mode.name()));
        return presence;
    }

    public XMPPConnectionConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public ListeningFuture<Void> login() {
        return executorService.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                loginImpl();
                return null;
            }
        });   
    }

    void loginImpl() throws FriendException {
        synchronized (this) {
            try {
                connectionMulticaster.broadcast(new XMPPConnectionEvent(this, XMPPConnectionEvent.Type.CONNECTING));
                loggingIn.set(true);
                org.jivesoftware.smack.XMPPConnection.addConnectionCreationListener(smackConnectionListener);
                org.jivesoftware.smack.XMPPConnection.DEBUG_ENABLED = configuration.isDebugEnabled();
                connect();
                LOG.infof("connected.");
                LOG.infof("logging in {0} with resource: {1} ...", configuration.getUserInputLocalID(), configuration.getResource());
                connection.login(configuration.getUserInputLocalID(), configuration.getPassword(), configuration.getResource());
                LOG.infof("logged in.");
                loggedIn.set(true);
                connectionMulticaster.broadcast(new XMPPConnectionEvent(this, XMPPConnectionEvent.Type.CONNECTED));
            } catch (org.jivesoftware.smack.XMPPException e) {
                connectionMulticaster.broadcast(new XMPPConnectionEvent(this, XMPPConnectionEvent.Type.CONNECT_FAILED, e));
                if(connection != null && connection.isConnected()) {
                    connection.disconnect();
                }
                org.jivesoftware.smack.XMPPConnection.removeConnectionCreationListener(smackConnectionListener);
                connection = null;
                throw new FriendException(e);
            } finally {
                loggingIn.set(false);
            }
        }
    }

    private void connect() throws org.jivesoftware.smack.XMPPException {
        for(ConnectionConfigurationFactory factory : connectionConfigurationFactories) {
            try {
                connectUsingFactory(factory);
                return;
            } catch (FriendException e) {
                LOG.debug(e.getMessage(), e);
            }
        }
    }

    private void connectUsingFactory(ConnectionConfigurationFactory factory) throws FriendException {
        ConnectionConfigurationFactory.RequestContext requestContext = new ConnectionConfigurationFactory.RequestContext();
        while(factory.hasMore(configuration, requestContext)) {
            ConnectionConfiguration connectionConfig = factory.getConnectionConfiguration(configuration, requestContext);
            connection = new XMPPConnection(connectionConfig);
            connection.addRosterListener(new RosterListenerImpl());
            LOG.infof("connecting to {0} at {1}:{2} ...", connectionConfig.getServiceName(), connectionConfig.getHost(), connectionConfig.getPort());
            try {
                connection.connect();
                return;
            } catch (org.jivesoftware.smack.XMPPException e) {
                LOG.debug(e.getMessage(), e);
                requestContext.incrementRequests();
            }            
        }
        throw new FriendException("couldn't connect using " + factory);
    }

    @Override
    public boolean isLoggingIn() {
        return loggingIn.get();
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
        synchronized (this) {
            if(isLoggedIn()) {
                loggedIn.set(false);
                LOG.infof("disconnecting from {0} at {1}:{2} ...", connection.getServiceName(), connection.getHost(), connection.getPort());
                connection.disconnect();
                synchronized (friends) {
                    friends.clear();
                }
                XMPPConnection.removeConnectionCreationListener(smackConnectionListener);
                connection = null;
                LOG.info("disconnected.");
            } 
        }
    }
    
    public boolean isLoggedIn() {
        return loggedIn.get();
    }
    
    private void checkLoggedIn() throws FriendException {
        synchronized (this) {
            if(!isLoggedIn()) {
                throw new FriendException("not connected");
            }
        }
    }

    private class RosterListenerImpl implements org.jivesoftware.smack.RosterListener {

        public void entriesAdded(Collection<String> addedIds) {
            try {
                synchronized (XMPPConnectionImpl.this) {
                    checkLoggedIn();
                    synchronized (friends) {
                        Roster roster = connection.getRoster();
                        if(roster != null) {
                            Map<String, XMPPFriend> newFriends = new HashMap<String, XMPPFriend>();
                            for(String id : addedIds) {
                                RosterEntry rosterEntry = roster.getEntry(id);
                                XMPPFriendImpl friend = new XMPPFriendImpl(
                                    id, rosterEntry, configuration, connection, discoInfoListener);
                                LOG.debugf("friend {0} added", friend);
                                newFriends.put(id, friend);
                            }
                            friends.putAll(newFriends);
                            rosterListeners.broadcast(new RosterEvent(newFriends.values(), RosterEvent.Type.FRIENDS_ADDED));
                        }
                    }
                }
            } catch (org.jivesoftware.smack.XMPPException e) {
                LOG.debugf(e, "error getting roster");    
            } catch (FriendException e) {
                LOG.debugf(e, "error getting roster");    
            }
        }

        public void entriesUpdated(Collection<String> updatedIds) {
            try {
                synchronized (XMPPConnectionImpl.this) {
                    checkLoggedIn();
                    synchronized (friends) {
                        Roster roster = connection.getRoster();
                        if(roster != null) {
                            Set<XMPPFriend> updatedFriends = new HashSet<XMPPFriend>();
                            for(String id : updatedIds) {
                                RosterEntry rosterEntry = roster.getEntry(id);
                                XMPPFriendImpl friend = (XMPPFriendImpl) friends.get(id);
                                if(friend == null) {
                                    // should never happen ?
                                    friend = new XMPPFriendImpl(
                                        id, rosterEntry, configuration, connection, discoInfoListener);
                                    friends.put(id, friend);
                                } else {
                                    friend.setRosterEntry(rosterEntry);
                                }
                                updatedFriends.add(friend);
                                LOG.debugf("friend {0} updated", friend);
                            }
                            rosterListeners.broadcast(new RosterEvent(updatedFriends, RosterEvent.Type.FRIENDS_UPDATED));
                        }
                    }
                }
            } catch (org.jivesoftware.smack.XMPPException e) {
                LOG.debugf(e, "error getting roster");    
            } catch (FriendException e) {
                LOG.debugf(e, "error getting roster");    
            }
        }

        public void entriesDeleted(Collection<String> removedIds) {
            synchronized (friends) {
                Set<XMPPFriend> deletedFriends = new HashSet<XMPPFriend>();
                for(String id : removedIds) {
                    XMPPFriend friend = friends.remove(id);
                    if(friend != null) {
                        deletedFriends.add(friend);
                        LOG.debugf("friend {0} removed", friend);
                    }
                }
                rosterListeners.broadcast(new RosterEvent(deletedFriends, RosterEvent.Type.FRIENDS_DELETED));
            }
        }

        public void presenceChanged(final org.jivesoftware.smack.packet.Presence presence) {
            String localJID;
            try {
                localJID = getLocalJid();
            } catch (FriendException e) {
                localJID = null;
            }
            if(!presence.getFrom().equals(localJID)) {
                XMPPFriendImpl friend = getFriend(presence);
                if(friend != null) {
                    LOG.debugf("presence from {0} changed to {1}", presence.getFrom(), presence.getType());
                    synchronized (friend) {
                        if (presence.getType().equals(org.jivesoftware.smack.packet.Presence.Type.available)) {
                            if(!friend.getFriendPresences().containsKey(presence.getFrom())) {
                                addNewPresence(friend, presence);
                            } else {
                                updatePresence(friend, presence);
                            }
                        } else if (presence.getType().equals(org.jivesoftware.smack.packet.Presence.Type.unavailable)) {
                            PresenceImpl p = (PresenceImpl)friend.getPresence(presence.getFrom());
                            if(p != null) {
                                p.update(presence);
                                friend.removePresense(p);
                            }
                        }
                    }
                } else {
                    LOG.debugf("no friend for presence {0}", presence.getFrom());
                }
            }
        }

        private XMPPFriendImpl getFriend(org.jivesoftware.smack.packet.Presence presence) {
            XMPPFriendImpl friend;
            synchronized (friends) {
                friend = (XMPPFriendImpl) friends.get(StringUtils.parseBareAddress(presence.getFrom()));
            }
            return friend;
        }

        private void addNewPresence(final XMPPFriendImpl friend, final org.jivesoftware.smack.packet.Presence presence) {
            final PresenceImpl presenceImpl = new PresenceImpl(presence, friend, featureBroadcaster);
            friend.addPresense(presenceImpl);
        }

        private void updatePresence(XMPPFriendImpl friend, org.jivesoftware.smack.packet.Presence presence) {
            PresenceImpl currentPresence = (PresenceImpl)friend.getFriendPresences().get(presence.getFrom());
            currentPresence.update(presence);
            friend.updatePresence(currentPresence);
        }
    }


    @Override
    public ListeningFuture<Void> addFriend(final String id, final String name) {
        return executorService.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                addFriendImpl(id, name);
                return null;
            }
        }); 
    }

    void addFriendImpl(String id, String name) throws FriendException {
        synchronized (this) {
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
                throw new FriendException(e);
            } 
        }
    }

    @Override
    public ListeningFuture<Void> removeFriend(final String id) {
        return executorService.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                removeFriendImpl(id);
                return null;
            }
        }); 
    }

    private void removeFriendImpl(String id) throws FriendException {
        synchronized (this) {
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
                throw new FriendException(e);
            }
        }
    }

    @Override
    public XMPPFriend getFriend(String id) {
        synchronized (friends) {
            return friends.get(id);
        }
    }

    @Override
    public Collection<XMPPFriend> getFriends() {
        synchronized (friends) {
            return new ArrayList<XMPPFriend>(friends.values());
        }
    }

    public void sendPacket(Packet packet) throws FriendException {
        synchronized (this) {
            try {
                checkLoggedIn();
                connection.sendPacket(packet);
            } catch (org.jivesoftware.smack.XMPPException e) {
                throw new FriendException(e);
            } 
        }
    }

    public String getLocalJid() throws FriendException {
        synchronized (this) {
            checkLoggedIn();
            return connection.getUser();
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
                if (ProviderManager.getInstance().getIQProvider(NoSaveIQ.ELEMENT_NAME, NoSaveIQ.NAME_SPACE) == null) {
                    ProviderManager.getInstance().addIQProvider(NoSaveIQ.ELEMENT_NAME, NoSaveIQ.NAME_SPACE, NoSaveIQ.getIQProvider());
                }
            }

            ChatStateManager.getInstance(connection);

            discoInfoListener = new DiscoInfoListener(XMPPConnectionImpl.this, connection);
            discoInfoListener.addListeners(connectionMulticaster, friendPresenceSupport);

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
            new NoSaveFeatureInitializer(connection, XMPPConnectionImpl.this, 
                    rosterListeners, friendPresenceSupport).register(discoInfoListener);
            
            SubscriptionListener sub = new SubscriptionListener(connection,
                                                friendRequestBroadcaster);
            connection.addPacketListener(sub, sub);
        }

        @Override
        public void connectionClosed() {
            connectionMulticaster.broadcast(new XMPPConnectionEvent(XMPPConnectionImpl.this, XMPPConnectionEvent.Type.DISCONNECTED));
            cleanup();
        }

        @Override
        public void connectionClosedOnError(Exception e) {
            connectionMulticaster.broadcast(new XMPPConnectionEvent(XMPPConnectionImpl.this, XMPPConnectionEvent.Type.DISCONNECTED, e));
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
                discoInfoListener.cleanup();
            }
            if(addressIQListener != null) {
                addressListenerSupport.removeListener(addressIQListener);
            }
        }
    }
}
