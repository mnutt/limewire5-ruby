package org.limewire.xmpp.client.impl.messages.authtoken;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.commons.codec.binary.Base64;
import org.jivesoftware.smack.packet.IQ;
import org.limewire.util.StringUtils;
import org.limewire.xmpp.client.impl.messages.InvalidIQException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

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
                    return;
                }
            }
        } while (parser.nextTag() != XmlPullParser.END_DOCUMENT);
    }
    
    public AuthTokenIQ() {
        
    }
    
    public AuthTokenIQ(byte [] authToken) {
        this.authToken = authToken;
    }

    public byte [] getAuthToken() {
        return authToken;
    }

    public String getChildElementXML() {        
        String authTokenElement = "<auth-token xmlns=\"jabber:iq:lw-auth-token\">";        
        if(this.authToken != null) {
            try {
                authTokenElement += "<token";
                authTokenElement += " value=\"" + new String(Base64.encodeBase64(authToken), "UTF-8") + "\"/>";
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
        authTokenElement += "</auth-token>";
        return authTokenElement;
    } 
}
