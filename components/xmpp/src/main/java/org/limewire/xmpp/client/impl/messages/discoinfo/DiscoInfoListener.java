package org.limewire.xmpp.client.impl.messages.discoinfo;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.FriendPresenceEvent;
import org.limewire.core.api.friend.feature.FeatureInitializer;
import org.limewire.core.api.friend.feature.FeatureRegistry;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.BlockingEvent;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.xmpp.api.client.XMPPFriend;
import org.limewire.xmpp.api.client.XMPPConnection;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;

/**
 * sends disco info messages (http://jabber.org/protocol/disco#info) to newly available
 * presences and then calls the appropriate FeatureInitializer for each of the
 * features that come back in the response.
 */
public class DiscoInfoListener implements PacketListener, FeatureRegistry {

    private static final Log LOG = LogFactory.getLog(DiscoInfoListener.class);

    private final Map<URI, FeatureInitializer> featureInitializerMap;
    private final XMPPConnection connection;
    private final org.jivesoftware.smack.XMPPConnection smackConnection;

    private final XMPPConnectionListener connectionListener;
    private ListenerSupport<XMPPConnectionEvent> connectionSupport;
    private ListenerSupport<FriendPresenceEvent> friendPresenceSupport;
    private final FriendPresenceListener friendPresenceListener;
    private final PacketFilter packetFilter;

    public DiscoInfoListener(XMPPConnection connection,
                             org.jivesoftware.smack.XMPPConnection smackConnection) {
        this.connection = connection;
        this.smackConnection = smackConnection;
        this.featureInitializerMap = new ConcurrentHashMap<URI, FeatureInitializer>();
        this.friendPresenceListener = new FriendPresenceListener();
        this.connectionListener = new XMPPConnectionListener();
        this.packetFilter = new DiscoPacketFilter();
    }

    public void addListeners(ListenerSupport<XMPPConnectionEvent> connectionSupport,
                             ListenerSupport<FriendPresenceEvent> friendPresenceSupport) {
        this.connectionSupport = connectionSupport;
        this.friendPresenceSupport = friendPresenceSupport;

        connectionSupport.addListener(connectionListener);
        friendPresenceSupport.addListener(friendPresenceListener);
        smackConnection.addPacketListener(this, packetFilter);
    }

    @Override
    public void add(URI uri, FeatureInitializer featureInitializer, boolean external) {
        featureInitializerMap.put(uri, featureInitializer);

        if (external) {
            ServiceDiscoveryManager.getInstanceFor(smackConnection).addFeature(uri.toASCIIString());
        }
    }

    @Override
    public FeatureInitializer get(URI uri) {
        return featureInitializerMap.get(uri);
    }

    @Override
    public void processPacket(Packet packet) {
        DiscoverInfo discoverInfo = (DiscoverInfo) packet;
        String discoFromField = discoverInfo.getFrom();
        FriendPresence friendPresence = null;

        if ((discoFromField != null) &&
                isForThisConnection(discoFromField) ||
                ((friendPresence = matchValidPresence(discoFromField)) != null)) {

            String featureInitializer = friendPresence != null ? friendPresence.getPresenceId() : discoFromField;
            for (URI uri : featureInitializerMap.keySet()) {
                if (discoverInfo.containsFeature(uri.toASCIIString())) {
                    LOG.debugf("initializing feature {0} for {1}", uri.toASCIIString(), featureInitializer);
                    featureInitializerMap.get(uri).initializeFeature(friendPresence);
                }
            }
        }
    }

    public void cleanup() {
        if (connectionListener != null) {
            connectionSupport.removeListener(connectionListener);
        }
        if (friendPresenceSupport != null) {
            friendPresenceSupport.removeListener(friendPresenceListener);
        }
        for (URI uri : featureInitializerMap.keySet()) {
            featureInitializerMap.get(uri).cleanup();
        }
        smackConnection.removePacketListener(this);
    }

    /**
     * Asynchronously discovers features of an xmpp entity.  Does not wait for reply packets.
     *
     * @param entityName name of entity (can be anything, such as a
     *                   presence id, an xmpp server name, etc)
     */
    private void discoverFeatures(String entityName) {
        try {
            ServiceDiscoveryManager serviceDiscoveryManager = ServiceDiscoveryManager.getInstanceFor(smackConnection);
            serviceDiscoveryManager.discoverInfo(entityName);
        } catch (org.jivesoftware.smack.XMPPException exception) {
            if (exception.getXMPPError() != null &&
                    !exception.getXMPPError().getCondition().
                            equals(XMPPError.Condition.feature_not_implemented.toString())) {
                LOG.info(exception.getMessage(), exception);
            }
        }
    }


    private boolean isForThisConnection(String from) {
        return connection.getConfiguration().getServiceName().equals(from);
    }

    /**
     * @param from address (e.g. loginName@serviceName.com/resourceInfo)
     * @return the intended presence of the announced feature based on
     *         what is in the disco info packet.
     *         <p/>
     *         Returns NULL if there is no presence for the announced feature
     */
    private FriendPresence matchValidPresence(String from) {

        // does the from string match a presence
        XMPPFriend user = connection.getFriend(StringUtils.parseBareAddress(from));

        if (user != null) {
            FriendPresence presence = user.getFriendPresences().get(from);
            if (presence != null) {
                return presence;
            }
        }

        // gets here if packet contains a from string indicating
        // a presence we don't know about.  Or there is an unknown problem.
        return null;
    }


    // listen for new presences in order to discover presence features
    private class FriendPresenceListener implements EventListener<FriendPresenceEvent> {
        @BlockingEvent(queueName = "presence feature discovery")
        @Override
        public void handleEvent(final FriendPresenceEvent event) {
            if (event.getType() == FriendPresenceEvent.Type.ADDED) {
                discoverFeatures(event.getData().getPresenceId());
            }
        }
    }

    // listen for new connections in order to discover server features
    private class XMPPConnectionListener implements EventListener<XMPPConnectionEvent> {
        @BlockingEvent
        @Override
        public void handleEvent(XMPPConnectionEvent event) {
            if (event.getType() == XMPPConnectionEvent.Type.CONNECTED) {
                discoverFeatures(connection.getConfiguration().getServiceName());
            }
        }
    }

    private static class DiscoPacketFilter implements PacketFilter {
        @Override
        public boolean accept(Packet packet) {
            return packet instanceof DiscoverInfo &&
                    (((DiscoverInfo) packet).getType() == IQ.Type.SET
                            || ((DiscoverInfo) packet).getType() == IQ.Type.RESULT);
        }
    }

}
