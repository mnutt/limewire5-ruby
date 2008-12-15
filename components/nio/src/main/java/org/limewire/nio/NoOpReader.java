package org.limewire.nio;

import java.io.IOException;

import org.limewire.nio.observer.ReadObserver;

/**
 * A simple reader that does nothing.
 * This is used primarily to allow objects that always require a non-null reader
 * to clean up references to old readers while still maintaining a non-null reader.
 */
class NoOpReader implements ReadObserver {
    public void handleRead() throws IOException {}
    public void handleIOException(IOException iox) {}
    public void shutdown() {}

}
