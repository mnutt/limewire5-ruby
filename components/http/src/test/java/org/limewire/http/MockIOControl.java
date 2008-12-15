package org.limewire.http;

import java.io.IOException;

import org.apache.http.nio.IOControl;

public class MockIOControl implements IOControl {

    public volatile boolean shutdown;
    
    public volatile boolean inputRequested;
    
    public volatile boolean outputRequested;
    
    public synchronized void requestInput() {
        inputRequested = true;
    }

    public synchronized void requestOutput() {
        outputRequested = true;
    }

    public synchronized void shutdown() throws IOException {
        this.shutdown = true;
    }

    public synchronized void suspendInput() {
        inputRequested = false;
    }

    public synchronized void suspendOutput() {
        outputRequested = false;
    }
    
}
