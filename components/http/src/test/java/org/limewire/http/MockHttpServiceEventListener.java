/**
 * 
 */
package org.limewire.http;

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.nio.NHttpConnection;
import org.limewire.http.protocol.HttpServiceEventListener;

public class MockHttpServiceEventListener implements HttpServiceEventListener {

    boolean responseSent;

    boolean connectionClosed;

    boolean connectionOpened;

    Exception exception;

    HttpResponse response;

    public void responseSent(NHttpConnection conn, HttpResponse response) {
        this.responseSent = true;
        this.response = response;
    }

    public void connectionClosed(NHttpConnection conn) {
        this.connectionClosed = true;
    }

    public void connectionOpen(NHttpConnection conn) {
        this.connectionOpened = true;
    }

    public void connectionTimeout(NHttpConnection conn) {
        // should never happen
        throw new RuntimeException();
    }

    public void fatalIOException(IOException ex, NHttpConnection conn) {
        this.exception = ex;
    }

    public void fatalProtocolException(HttpException ex,
            NHttpConnection conn) {
        this.exception = ex;
    }

}