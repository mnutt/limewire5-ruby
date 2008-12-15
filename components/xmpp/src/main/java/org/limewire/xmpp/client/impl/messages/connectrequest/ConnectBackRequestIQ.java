package org.limewire.xmpp.client.impl.messages.connectrequest;

import java.io.IOException;
import java.text.MessageFormat;

import org.apache.commons.codec.binary.Base64;
import org.jivesoftware.smack.packet.IQ;
import org.limewire.io.Connectable;
import org.limewire.io.GUID;
import org.limewire.io.NetworkUtils;
import org.limewire.net.address.ConnectableSerializer;
import org.limewire.util.Objects;
import org.limewire.util.StringUtils;
import org.limewire.xmpp.client.impl.messages.InvalidIQException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;


/**
 * IQ to be send to request the other peer to open a connection back to this peer.
 * 
 * The connection can be of two types:
 * 
 * 1) a regular TCP connection, in this case {@link #getSupportedFWTVersion()} is 0
 * 2) a reliable udp connection, in this case {@link #getSupportedFWTVersion()} conveys 
 *    the supported protocol version
 * In both cases a valid address for connecting needs to be provided
 */
public class ConnectBackRequestIQ extends IQ {

    // private final Log LOG = LogFactory.getLog(ConnectRequestIQ.class);
    
    private final Connectable address;
    private final int supportedfwtVersion;
    private final GUID clientGuid;
    
    public static final String ELEMENT_NAME = "connect-back-request";
    
    public static final String NAME_SPACE = "jabber:iq:lw-connect-request";
    
    /**
     * Only constructs valid connect request iqs, otherwise throws {@link IOException}. 
     */
    public ConnectBackRequestIQ(XmlPullParser parser) throws IOException, XmlPullParserException, InvalidIQException {
       int eventType = parser.getEventType();
       GUID guid = null;
       int fwtVersion = -1;
       Connectable connectable = null;
       for (; eventType != XmlPullParser.END_DOCUMENT; eventType = parser.next()) {
           if (eventType == XmlPullParser.START_TAG) {
               if (parser.getName().equals(ELEMENT_NAME)) {
                   String value = parser.getAttributeValue(null, "client-guid");
                   if (value == null) {
                       throw new InvalidIQException("no guid provided");
                   }
                   try { 
                       guid = new GUID(value);
                   } catch (IllegalArgumentException iae) {
                       throw new InvalidIQException("invalid guid: " + value, iae);
                   }
                   value = parser.getAttributeValue(null, "supported-fwt-version");
                   if (value == null) {
                       throw new InvalidIQException("no fwt version provided");
                   }
                   try {
                       fwtVersion = Integer.parseInt(value);
                   } catch (NumberFormatException nfe) {
                       throw new InvalidIQException("fwt version no a valid number: " + value, nfe);
                   }
               } else if (parser.getName().equals("address")) {
                   String type = parser.getAttributeValue(null, "type");
                   ConnectableSerializer serializer = new ConnectableSerializer();
                   if (type == null || !type.equals(serializer.getAddressType())) {
                       throw new InvalidIQException("no address type provided or invalid: " + type);
                   }
                   String value = parser.getAttributeValue(null, "value");
                   if (value == null) {
                       throw new InvalidIQException("no address value found");
                   }
                   connectable = serializer.deserialize(Base64.decodeBase64(StringUtils.toUTF8Bytes(value)));
                   if (!NetworkUtils.isValidIpPort(connectable)) {
                       throw new InvalidIQException("invalid address: " + connectable);
                   }
               }
           } else if (eventType == XmlPullParser.END_TAG && parser.getName().equals(ELEMENT_NAME)) {
               // exit loop when we see end tag for connect-request
               break;
           }
       }
       if (guid == null || fwtVersion == -1 || connectable == null) {
           throw new InvalidIQException(MessageFormat.format("incomplete connect request, {0}, {1}, {2}", guid, fwtVersion, connectable));
       }
       clientGuid = guid;
       supportedfwtVersion = fwtVersion;
       address = connectable;
    }
    
    /**
     * 
     * @param address needs to be a valid address, otherwise will throw {@link IllegalArgumentException}
     * @param clientGuid
     * @param supportedFWTVersion 0 if fwt is not supported
     */
    public ConnectBackRequestIQ(Connectable address, GUID clientGuid, int supportedFWTVersion) {
        this.address = address;
        this.clientGuid = Objects.nonNull(clientGuid, "clientGuid");
        this.supportedfwtVersion = supportedFWTVersion;
        if (!NetworkUtils.isValidIpPort(address)) {
            throw new IllegalArgumentException("invalid address: " + address);
        }
    }

    @Override
    public String getChildElementXML() {
        ConnectableSerializer serializer = new ConnectableSerializer();
        String message = "<{0} xmlns=\"{1}\" client-guid=\"{2}\" supported-fwt-version=\"{3}\"><address type=\"{4}\" value=\"{5}\"/></{6}>";
        try {
            return MessageFormat.format(message, ELEMENT_NAME, NAME_SPACE,
                    clientGuid.toHexString(),
                    String.valueOf(supportedfwtVersion), serializer.getAddressType(),
                    StringUtils.getUTF8String(Base64.encodeBase64(serializer.serialize(address))),
                    ELEMENT_NAME);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } 
    }
    
    public Connectable getAddress() {
        return address;
    }

    public int getSupportedFWTVersion() {
        return supportedfwtVersion;
    }

    public GUID getClientGuid() {
        return clientGuid;
    }
    
    @Override
    public String toString() {
        return StringUtils.toString(this);
    }
}
