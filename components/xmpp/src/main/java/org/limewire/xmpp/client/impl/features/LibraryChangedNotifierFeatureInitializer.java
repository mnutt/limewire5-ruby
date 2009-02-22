package org.limewire.xmpp.client.impl.features;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.feature.FeatureInitializer;
import org.limewire.core.api.friend.feature.FeatureRegistry;
import org.limewire.core.api.friend.feature.features.LibraryChangedNotifier;
import org.limewire.core.api.friend.feature.features.LibraryChangedNotifierFeature;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.xmpp.client.impl.messages.library.LibraryChangedIQ;

public class LibraryChangedNotifierFeatureInitializer implements FeatureInitializer {

    private static final Log LOG = LogFactory.getLog(LibraryChangedNotifierFeatureInitializer.class);

    private final XMPPConnection connection;

    public LibraryChangedNotifierFeatureInitializer(XMPPConnection connection){
        this.connection = connection;
    }

    @Override
    public void register(FeatureRegistry registry) {
        registry.add(LibraryChangedNotifierFeature.ID, this);
    }

    @Override
    public void initializeFeature(FriendPresence friendPresence) {
        friendPresence.addFeature(new LibraryChangedNotifierFeature(new LibraryChangedNotifierImpl(friendPresence.getPresenceId(), connection)));
    }

    @Override
    public void removeFeature(FriendPresence friendPresence) {
        friendPresence.removeFeature(LibraryChangedNotifierFeature.ID);
    }

    private static class LibraryChangedNotifierImpl implements LibraryChangedNotifier {
        private final String presenceId;
        private final XMPPConnection connection;

        public LibraryChangedNotifierImpl(String presenceId, XMPPConnection connection) {
            this.presenceId = presenceId;
            this.connection = connection;
        }

        public void sendLibraryRefresh() {
            if(connection.isConnected()) {
                final LibraryChangedIQ libraryChangedIQ = new LibraryChangedIQ();
                libraryChangedIQ.setType(IQ.Type.SET);
                libraryChangedIQ.setTo(presenceId);
                libraryChangedIQ.setPacketID(IQ.nextID());
                try {
                    connection.sendPacket(libraryChangedIQ);
                } catch (XMPPException e) {
                    LOG.debugf("library refresh failed", e);
                }
            }
        }
    }
}
