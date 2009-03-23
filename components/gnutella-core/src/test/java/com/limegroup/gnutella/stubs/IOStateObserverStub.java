package com.limegroup.gnutella.stubs;

import java.io.IOException;

import org.limewire.nio.statemachine.IOStateObserver;

public class IOStateObserverStub implements IOStateObserver {
    
    private volatile boolean statesFinished;
    private volatile boolean shutdown;
    private volatile IOException iox;
    
    public void clear() {
        statesFinished = false;
        shutdown = false;
        iox = null;
    }
    
    public synchronized void waitForFinish() throws Exception {
        while(!statesFinished && !shutdown && iox == null)
            wait();
    }

    public synchronized void handleStatesFinished() {
        statesFinished = true;
        notify();
    }

    public synchronized void shutdown() {
        shutdown = true;
        notify();
    }

    public synchronized void handleIOException(IOException iox) {
        this.iox = iox;
        notify();
    }

    public IOException getIox() {
        return iox;
    }

    public boolean isShutdown() {
        return shutdown;
    }

    public boolean isStatesFinished() {
        return statesFinished;
    }

}
