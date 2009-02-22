package org.limewire.xmpp.client.impl.messages.filetransfer;

import java.io.IOException;
import java.text.MessageFormat;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.xmpp.api.client.FileMetaData;
import org.limewire.xmpp.client.impl.messages.FileMetaDataImpl;
import org.limewire.xmpp.client.impl.messages.InvalidIQException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class FileTransferIQ extends IQ {

    private static Log LOG = LogFactory.getLog(FileTransferIQ.class);
    
    public enum TransferType {OFFER, REQUEST}

    private final FileMetaData fileMetaData;
    private final TransferType transferType;
    
    public FileTransferIQ(XmlPullParser parser) throws IOException, XmlPullParserException, InvalidIQException {
        FileMetaData parsedMetaData = null;
        TransferType parsedTransferType = null;
        do {
            int eventType = parser.getEventType();
            if(eventType == XmlPullParser.START_TAG) {
                if(parser.getName().equals("file-transfer")) {
                    String transferTypeValue = parser.getAttributeValue(null, "type");
                    if (transferTypeValue == null) {
                        throw new InvalidIQException("no transfer type specified");
                    }
                    try {
                        parsedTransferType = TransferType.valueOf(transferTypeValue);
                    } catch (IllegalArgumentException iae) {
                        throw new InvalidIQException("unknown transfer type: " + transferTypeValue);
                    }
                } else if(parser.getName().equals("file")) {
                    parsedMetaData = new FileMetaDataImpl(parser);
                }
            } else if(eventType == XmlPullParser.END_TAG) {
                if(parser.getName().equals("file-transfer")) {
                    break;
                }
            }
        } while (parser.nextTag() != XmlPullParser.END_DOCUMENT);
        
        if (parsedMetaData == null || parsedTransferType == null) {
            throw new InvalidIQException(MessageFormat.format("parsedMetaData {0}, parsedTransferType {1}", parsedMetaData, parsedTransferType));
        }
        this.fileMetaData = parsedMetaData;
        this.transferType = parsedTransferType;
    }
    
    public FileTransferIQ(FileMetaData fileMetaData, TransferType transferType) {
        this.fileMetaData = fileMetaData;
        this.transferType = transferType;
    }

    public FileMetaData getFileMetaData() {
        return fileMetaData;
    }

    public TransferType getTransferType() {
        return transferType;
    }

    public String getChildElementXML() {
        String fileTransfer = "<file-transfer xmlns='jabber:iq:lw-file-transfer' type='" + transferType.toString() + "'>";
        if(fileMetaData != null) {
            fileTransfer += fileMetaData.toXML();
        }
        fileTransfer += "</file-transfer>";
        return fileTransfer;
    }
    
    public static IQProvider getIQProvider() {
        return new FileTransferIQProvider();
    }

    private static class FileTransferIQProvider implements IQProvider {

        public IQ parseIQ(XmlPullParser parser) throws Exception {
            try {
                return new FileTransferIQ(parser);
            } catch (InvalidIQException ie) {
                LOG.debug("invalid iq", ie);
                // throwing would close connection
                return null;
            }
        }
    }
}
