package org.limewire.xmpp.client.impl.messages.nosave;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class NoSaveIQ extends IQ {
    private Map<String, Boolean> items;
    public NoSaveIQ(XmlPullParser parser) throws IOException, XmlPullParserException {
        items = new HashMap<String, Boolean>();
        do {
            int eventType = parser.getEventType();
            if(eventType == XmlPullParser.START_TAG) {
                if(parser.getName().equals("query")) {
                } else if(parser.getName().equals("item")) {
                    String jid = parser.getAttributeValue(null, "jid");
                    Boolean value = Boolean.parseBoolean(parser.getAttributeValue(null, "value"));
                    items.put(jid, value);
                }
            } else if(eventType == XmlPullParser.END_TAG) {
                if(parser.getName().equals("query")) {
                    return;
                }
            }
        } while (parser.nextTag() != XmlPullParser.END_DOCUMENT);
    }

    public NoSaveIQ(String jid, Boolean value) {
        items = new HashMap<String, Boolean>();
        items.put(jid, value);
    }

    public String getChildElementXML() {
        String s = "<query xmlns='google:nosave'>";
        for(Map.Entry<String, Boolean> entry : items.entrySet()) {
            s += "<item xmlns='google:nosave' jid='" + entry.getKey() + "' value='" + entry.getValue() + "'/>";
        }
        s += "</query>";
        return s;
    }

    public static IQProvider getIQProvider() {
        return new NoSaveIQProvider();
    }

    private static class NoSaveIQProvider implements IQProvider {

        public IQ parseIQ(XmlPullParser parser) throws Exception {
            return new NoSaveIQ(parser);
        }
    }
}
