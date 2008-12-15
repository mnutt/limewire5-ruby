package org.limewire.http;

import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.IOSession;

public class MockIOEventDispatch implements IOEventDispatch {

    public void connected(IOSession session) {
    }

    public void disconnected(IOSession session) {
    }

    public void inputReady(IOSession session) {
    }

    public void outputReady(IOSession session) {
    }

    public void timeout(IOSession session) {
    }

}
