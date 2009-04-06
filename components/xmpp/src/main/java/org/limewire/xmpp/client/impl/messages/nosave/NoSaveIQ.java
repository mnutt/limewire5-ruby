package org.limewire.xmpp.client.impl.messages.nosave;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.limewire.util.Objects;
import org.limewire.xmpp.client.impl.messages.InvalidIQException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class NoSaveIQ extends IQ {
    
    private final Map<String, Boolean> items = new HashMap<String, Boolean>();
    
    public NoSaveIQ(XmlPullParser parser) throws IOException, XmlPullParserException, InvalidIQException {
        do {
            int eventType = parser.getEventType();
            if(eventType == XmlPullParser.START_TAG) {
                if(parser.getName().equals("query")) {
                } else if(parser.getName().equals("item")) {
                    String jid = parser.getAttributeValue(null, "jid");
                    if (jid == null) { 
                        throw new InvalidIQException("no jid value");
                    }
                    String value = parser.getAttributeValue(null, "value");
                    if (value == null) {
                        throw new InvalidIQException("no value in value attribute");
                    }
                    items.put(jid, Boolean.valueOf(value));
                }
            } else if(eventType == XmlPullParser.END_TAG) {
                if(parser.getName().equals("query")) {
                    break;
                }
            }
        } while (parser.nextTag() != XmlPullParser.END_DOCUMENT);
    }

    public NoSaveIQ(String jid, Boolean value) {
        items.put(Objects.nonNull(jid, "jid"), Objects.nonNull(value, "value"));
    }

    @Override
    public String getChildElementXML() {
        StringBuilder s = new StringBuilder("<query xmlns='google:nosave'>");
        for(Map.Entry<String, Boolean> entry : items.entrySet()) {
            s.append("<item xmlns='google:nosave' jid='").append(entry.getKey()).append("' value='").append(entry.getValue()).append("'/>");
        }
        s.append("</query>");
        return s.toString();
    }

    public static IQProvider getIQProvider() {
        return new NoSaveIQProvider();
    }

    private static class NoSaveIQProvider implements IQProvider {

        public IQ parseIQ(XmlPullParser parser) throws Exception {
            try { 
                return new NoSaveIQ(parser);
            } catch (InvalidIQException iie) {
                // throwing would close connection
                return null;
            }
        }
    }
}
