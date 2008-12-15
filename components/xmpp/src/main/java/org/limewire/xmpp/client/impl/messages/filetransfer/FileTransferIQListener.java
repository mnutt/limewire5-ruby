package org.limewire.xmpp.client.impl.messages.filetransfer;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.util.StringUtils;
import org.limewire.listener.EventBroadcaster;
import org.limewire.xmpp.api.client.FileOffer;
import org.limewire.xmpp.api.client.FileOfferEvent;
import org.xmlpull.v1.XmlPullParserException;

public class FileTransferIQListener implements PacketListener {
    private static final Log LOG = LogFactory.getLog(FileTransferIQListener.class);

    private final EventBroadcaster<FileOfferEvent> fileOfferBroadcaster;

    public FileTransferIQListener(EventBroadcaster<FileOfferEvent> fileOfferBroadcaster) {
        this.fileOfferBroadcaster = fileOfferBroadcaster;
    }

    public void processPacket(Packet packet) {
        FileTransferIQ iq = (FileTransferIQ)packet;
        try {
            if(iq.getType().equals(IQ.Type.GET)) {
                handleGet(iq);
            } else if(iq.getType().equals(IQ.Type.RESULT)) {
                //handleResult(iq);
            } else if(iq.getType().equals(IQ.Type.SET)) {
                //handleSet(iq);
            } else if(iq.getType().equals(IQ.Type.ERROR)) {
                //handleError(iq);
            } else {
                //sendError(packet);
            }
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            //sendError(packet);
        } catch (XmlPullParserException e) {
            LOG.error(e.getMessage(), e);
            //sendError(packet);
        }
    }

    private void handleGet(FileTransferIQ packet) throws IOException, XmlPullParserException {
        if(LOG.isDebugEnabled()) {
            LOG.debug("handling file transfer get " + packet.getPacketID());
        }
        // TODO async?
        String userID = StringUtils.parseBareAddress(packet.getFrom());
        fileOfferBroadcaster.broadcast(new FileOfferEvent(new FileOffer(packet.getFileMetaData(), userID), FileOffer.EventType.OFFER));
        // TODO send acceptance or rejection;
        // TODO only needed for user feedback
    }

    public PacketFilter getPacketFilter() {
        return new PacketFilter(){
            public boolean accept(Packet packet) {
                return packet instanceof FileTransferIQ;
            }
        };
    }
}
