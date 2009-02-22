package org.limewire.xmpp.client.impl.messages;

import java.io.StringReader;

import org.xmlpull.mxp1.MXParser;
import org.xmlpull.v1.XmlPullParser;

public class IQTestUtils {

    public static XmlPullParser createParser(String input) throws Exception {
        XmlPullParser parser = new MXParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        parser.setInput(new StringReader(input));
        return parser;
    }

}
