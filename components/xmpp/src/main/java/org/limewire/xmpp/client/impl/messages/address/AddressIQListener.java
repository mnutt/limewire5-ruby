package org.limewire.xmpp.client.impl.messages.address;

import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.util.StringUtils;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.feature.FeatureInitializer;
import org.limewire.core.api.friend.feature.FeatureRegistry;
import org.limewire.core.api.friend.feature.features.AddressFeature;
import org.limewire.core.api.friend.feature.features.LimewireFeature;
import org.limewire.io.Address;
import org.limewire.listener.EventListener;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.net.address.AddressEvent;
import org.limewire.net.address.AddressFactory;
import org.limewire.xmpp.api.client.User;
import org.limewire.xmpp.api.client.XMPPAddress;
import org.limewire.xmpp.client.impl.XMPPAddressRegistry;
import org.limewire.xmpp.client.impl.XMPPConnectionImpl;

public class AddressIQListener implements PacketListener, EventListener<AddressEvent> {
    private static final Log LOG = LogFactory.getLog(AddressIQListener.class);

    private final XMPPConnectionImpl connection;
    private volatile Address address;
    private final XMPPAddressRegistry addressRegistry;
    private final AddressFactory factory; 
    private final Map<String, Address> pendingAddresses;

    public AddressIQListener(XMPPConnectionImpl connection,
                             AddressFactory factory,
                             FeatureRegistry featureRegistry,
                             XMPPAddressRegistry addressRegistry) {
        this.connection = connection;
        this.factory = factory;
        this.addressRegistry = addressRegistry;
        this.pendingAddresses = new HashMap<String, Address>();
        new AddressIQFeatureInitializer().register(featureRegistry);
    }

    public void processPacket(Packet packet) {
        AddressIQ iq = (AddressIQ)packet;
        if(iq.getType().equals(IQ.Type.SET)) {
            handleSet(iq);
        }
    }

    private void handleSet(AddressIQ iq) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("handling set");
        }
        handleAddressUpdate(iq);
    }

    private void handleAddressUpdate(AddressIQ iq) {
        synchronized (this) {
            User user = connection.getUser(StringUtils.parseBareAddress(iq.getFrom()));
            if (user != null) {
                FriendPresence presence = user.getFriendPresences().get(iq.getFrom());
                if(presence != null) {
                    LOG.debugf("updating address on presence {0} to {1}", presence.getPresenceId(), iq.getAddress());
                    addressRegistry.put(new XMPPAddress(presence.getPresenceId()), iq.getAddress());
                    presence.addFeature(new AddressFeature(new XMPPAddress(presence.getPresenceId())));
                } else {
                    LOG.debugf("address {0} for presence {1} is pending", iq.getAddress(), iq.getFrom());
                    pendingAddresses.put(iq.getFrom(), iq.getAddress());
                }
            }
        }
    }

    public PacketFilter getPacketFilter() {
        return new PacketFilter(){
            public boolean accept(Packet packet) {
                return packet instanceof AddressIQ;
            }
        };
    }
    
    @Override
    public void handleEvent(AddressEvent event) {
        if (event.getType().equals(Address.EventType.ADDRESS_CHANGED)) {
            // TODO async?
            LOG.debugf("new address to publish: {0}", event);
            synchronized (AddressIQListener.this) {
                address = event.getSource();
                for(User user : connection.getUsers()) {
                    for(Map.Entry<String, FriendPresence> presenceEntry : user.getFriendPresences().entrySet()) {
                        if(presenceEntry.getValue().hasFeatures(LimewireFeature.ID)) {
                            sendAddress(address, presenceEntry.getKey());
                        }
                    }
                }
            }
        }
    }
    
    private void sendAddress(Address address, String jid) {
        LOG.debugf("sending new address to {0}", jid);
        AddressIQ queryResult = new AddressIQ(address, factory);
        queryResult.setTo(jid);
        queryResult.setFrom(connection.getLocalJid());
        queryResult.setType(IQ.Type.SET);
        connection.sendPacket(queryResult);
    }

    private class AddressIQFeatureInitializer implements FeatureInitializer {
        @Override
        public void register(FeatureRegistry registry) {
            registry.add(AddressFeature.ID, this);
        }

        @Override
        public void initializeFeature(FriendPresence friendPresence) {
            synchronized (AddressIQListener.this) {
                if (address != null) {
                    sendAddress(address, friendPresence.getPresenceId());
                }
                if (pendingAddresses.containsKey(friendPresence.getPresenceId())) {
                    LOG.debugf("updating address on presence {0} to {1}", friendPresence.getPresenceId(), address);
                    Address pendingAddress = pendingAddresses.remove(friendPresence.getPresenceId());
                    addressRegistry.put(new XMPPAddress(friendPresence.getPresenceId()), pendingAddress);
                    friendPresence.addFeature(new AddressFeature(new XMPPAddress(friendPresence.getPresenceId()))); 
                }
            }
        }

        @Override
        public void removeFeature(FriendPresence friendPresence) {
            addressRegistry.remove(new XMPPAddress(friendPresence.getPresenceId()));
            friendPresence.removeFeature(AddressFeature.ID);
        }
    }
}
