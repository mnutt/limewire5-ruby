package org.limewire.http;

import java.util.StringTokenizer;

import org.apache.http.Header;
import org.apache.http.HttpMessage;

/**
 * Provides utility methods for HttpCore.
 */
public class HttpCoreUtils {

    /**
     * Returns true, if message contains a header with <code>name</code> and the
     * value is a comma separated list that contains <code>token</code>.
     */
    public static boolean hasHeaderListValue(HttpMessage message, String name, String token) {   
        Header[] headers = message.getHeaders(name);
        for (Header header : headers) {
            StringTokenizer t = new StringTokenizer(header.getValue(), ",");
            while (t.hasMoreTokens()) {
                if (token.equals(t.nextToken().trim())) {
                    return true;
                }
            }
        }
        return false;
    }

}
