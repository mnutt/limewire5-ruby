package org.limewire.http;

import junit.framework.TestCase;

import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.limewire.http.protocol.LimeResponseConnControl;

public class LimeResponseConnControlTest extends TestCase {

    public void testProcessHttpResponseHttpContext() throws Exception {
        LimeResponseConnControl control = new LimeResponseConnControl();
        BasicHttpResponse response = new BasicHttpResponse(
                HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "");
        control.process(response, new BasicHttpContext(null));
        assertNull(response.getFirstHeader(HTTP.CONN_DIRECTIVE));

        response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
        control.process(response, new BasicHttpContext(null));
        Header header = response.getFirstHeader(HTTP.CONN_DIRECTIVE);
        assertNotNull(header);
        assertEquals("Close", header.getValue());

        // the connection control should not override an explicitly request
        // keep-alive
        response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
        response.setHeader(HTTP.CONN_DIRECTIVE, "Keep-Alive");
        control.process(response, new BasicHttpContext(null));
        header = response.getFirstHeader(HTTP.CONN_DIRECTIVE);
        assertNotNull(header);
        assertEquals("Keep-Alive", header.getValue());
    }

}
