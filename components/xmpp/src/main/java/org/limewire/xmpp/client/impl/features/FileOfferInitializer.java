package org.limewire.xmpp.client.impl.features;

import org.apache.commons.logging.Log;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.IQ;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.feature.FeatureInitializer;
import org.limewire.core.api.friend.feature.FeatureRegistry;
import org.limewire.core.api.friend.feature.features.FileOfferFeature;
import org.limewire.core.api.friend.feature.features.FileOfferer;
import org.limewire.logging.LogFactory;
import org.limewire.xmpp.api.client.FileMetaData;
import org.limewire.xmpp.client.impl.messages.filetransfer.FileTransferIQ;

public class FileOfferInitializer implements FeatureInitializer{
    private final XMPPConnection connection;

    public FileOfferInitializer(XMPPConnection connection) {
        this.connection = connection;
    }

    @Override
    public void register(FeatureRegistry registry) {
        registry.add(FileOfferFeature.ID, this);
    }

    @Override
    public void initializeFeature(FriendPresence friendPresence) {
        friendPresence.addFeature(new FileOfferFeature(new FileOffererImpl(friendPresence.getPresenceId(), connection)));
    }
    

    @Override
    public void removeFeature(FriendPresence friendPresence) {
        friendPresence.removeFeature(FileOfferFeature.ID);
    }

    private static class FileOffererImpl implements FileOfferer {
        private static final Log LOG = LogFactory.getLog(FileOffererImpl.class);

        private String presenceID;
        private final XMPPConnection connection;

        public FileOffererImpl(String presenceId, XMPPConnection connection) {
            this.presenceID = presenceId;
            this.connection = connection;
        }

        public void offerFile(FileMetaData file) {
            if(LOG.isInfoEnabled()) {
                LOG.info("offering file " + file.toString() + " to " + presenceID);
            }
            final FileTransferIQ transferIQ = new FileTransferIQ(file, FileTransferIQ.TransferType.OFFER);
            transferIQ.setType(IQ.Type.GET);
            transferIQ.setTo(presenceID);
            transferIQ.setPacketID(IQ.nextID());
            connection.sendPacket(transferIQ);
        }
    }
}
