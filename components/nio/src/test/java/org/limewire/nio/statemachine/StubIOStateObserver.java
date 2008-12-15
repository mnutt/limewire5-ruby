package org.limewire.nio.statemachine;

import java.io.IOException;

public class StubIOStateObserver implements IOStateObserver {
    
    private boolean statesFinished;
    private boolean shutdown;
    private IOException iox;
    
    public void clear() {
        statesFinished = false;
        shutdown = false;
        iox = null;
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
