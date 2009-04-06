package org.limewire.xmpp.client.impl.messages.authtoken;

import java.io.IOException;

import org.apache.commons.codec.binary.Base64;
import org.jivesoftware.smack.packet.IQ;
import org.limewire.util.StringUtils;
import org.limewire.xmpp.client.impl.messages.InvalidIQException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.google.inject.internal.base.Objects;

public class AuthTokenIQ extends IQ {
    private byte [] authToken;

    public AuthTokenIQ(XmlPullParser parser) throws IOException, XmlPullParserException, InvalidIQException {
        do {
            int eventType = parser.getEventType();
            if(eventType == XmlPullParser.START_TAG) {
                if(parser.getName().equals("token")) {
                    String value = parser.getAttributeValue(null, "value");
                    if (value == null) {
                        throw new InvalidIQException("no value");
                    }
                    authToken = Base64.decodeBase64(StringUtils.toUTF8Bytes(value));
                }
            } else if(eventType == XmlPullParser.END_TAG) {
                if(parser.getName().equals("auth-token")) {
                    break;
                }
            }
        } while (parser.nextTag() != XmlPullParser.END_DOCUMENT);
        
        if (authToken == null) {
            throw new InvalidIQException("no auth token parsed");
        }
    }
    
    /**
     * @param authToken must not be null
     */
    public AuthTokenIQ(byte [] authToken) {
        this.authToken = Objects.nonNull(authToken, "authToken");
    }

    /**
     * @return not null
     */
    public byte[] getAuthToken() {
        return authToken;
    }

    @Override
    public String getChildElementXML() {        
        StringBuilder authTokenElement = new StringBuilder("<auth-token xmlns=\"jabber:iq:lw-auth-token\">");        
        authTokenElement.append("<token value=\"").append(StringUtils.getUTF8String(Base64.encodeBase64(authToken))).append("\"/>");
        authTokenElement.append("</auth-token>");
        return authTokenElement.toString();
    }
    
    @Override
    public String toString() {
        return StringUtils.toUTF8String(Base64.encodeBase64(authToken));
    }
}
