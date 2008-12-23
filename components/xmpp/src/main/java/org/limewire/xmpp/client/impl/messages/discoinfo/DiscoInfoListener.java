package org.limewire.xmpp.client.impl.messages.discoinfo;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.limewire.concurrent.ThreadExecutor;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.feature.FeatureInitializer;
import org.limewire.core.api.friend.feature.FeatureRegistry;
import org.limewire.listener.EventListener;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.PresenceEvent;
import org.limewire.xmpp.api.client.RosterEvent;
import org.limewire.xmpp.api.client.User;
import org.limewire.xmpp.api.client.XMPPConnection;

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
    private final RosterListener rosterListener;
    private final PresenceListener presenceListener;      

    public DiscoInfoListener(XMPPConnection connection,
                             org.jivesoftware.smack.XMPPConnection smackConnection) {
        this.connection = connection;
        this.smackConnection = smackConnection;
        featureInitializerMap = new ConcurrentHashMap<URI, FeatureInitializer>();
        rosterListener = new RosterListener();
        presenceListener = new PresenceListener();
    }
    
    @Override
    public void add(URI uri, FeatureInitializer featureInitializer) {
        featureInitializerMap.put(uri, featureInitializer);
        ServiceDiscoveryManager.getInstanceFor(smackConnection).addFeature(uri.toASCIIString());
    }

    @Override
    public FeatureInitializer get(URI uri) {
        return featureInitializerMap.get(uri);
    }

    @Override
    public void processPacket(Packet packet) {
        DiscoverInfo discoverInfo = (DiscoverInfo)packet;
        User user = connection.getUser(StringUtils.parseBareAddress(discoverInfo.getFrom()));
        if (user != null) {
            FriendPresence presence = user.getFriendPresences().get(discoverInfo.getFrom());
            if(presence != null) {
                for(URI uri : featureInitializerMap.keySet()) {
                    if(discoverInfo.containsFeature(uri.toASCIIString())) {
                        featureInitializerMap.get(uri).initializeFeature(presence);
                    }
                }
            }
        }
    }
    
    public PacketFilter getPacketFilter() {
        return new PacketFilter(){
            public boolean accept(Packet packet) {
                return packet instanceof DiscoverInfo && 
                        (((DiscoverInfo)packet).getType() == IQ.Type.SET
                        || ((DiscoverInfo)packet).getType() == IQ.Type.RESULT);
            }
        };
    }
    
    public RosterListener getRosterListener() {
        return rosterListener;
    }
    
    class RosterListener implements EventListener<RosterEvent> {
        @Override
        public void handleEvent(RosterEvent event) {
            if(event.getType() == User.EventType.USER_ADDED) {
                event.getSource().addPresenceListener(presenceListener);
            }
        }
    }

    private class PresenceListener implements EventListener<PresenceEvent> {
        @Override
        public void handleEvent(final PresenceEvent event) {
            if(event.getType() == Presence.EventType.PRESENCE_NEW
                    && event.getSource().getType() == Presence.Type.available) {
                Thread t = ThreadExecutor.newManagedThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ServiceDiscoveryManager serviceDiscoveryManager = ServiceDiscoveryManager.getInstanceFor(smackConnection);
                            serviceDiscoveryManager.discoverInfo(event.getSource().getJID());
                        } catch (org.jivesoftware.smack.XMPPException exception) {
                            if(exception.getXMPPError() != null && exception.getXMPPError().getCode() != 501) {
                                LOG.info(exception.getMessage(), exception);
                            }
                        }
                    }
                }, "disco-info-" + event.getSource().getJID());
                t.start();
            }
        }
    }
}
