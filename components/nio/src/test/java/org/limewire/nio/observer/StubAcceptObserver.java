package org.limewire.nio.observer;

import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

import org.limewire.nio.NIOSocket;

@SuppressWarnings("unchecked")
public class StubAcceptObserver implements AcceptObserver {
    private List sockets = new LinkedList();
    private List ioxes = new LinkedList();
    private boolean shutdown = false;

    public void handleAccept(Socket socket) throws IOException {
        sockets.add(socket);
    }

    public void handleIOException(IOException iox) {
        ioxes.add(iox);
    }

    public void shutdown() {
        shutdown = true;
    }

    public List getIOXes() {
        return ioxes;
    }
    
    public IOException getNextIOException() {
        return (IOException)ioxes.remove(0);
    }

    public boolean isShutdown() {
        return shutdown;
    }

    public List getSockets() {
        return sockets;
    }
    
    public NIOSocket getNextSocket() {
        return (NIOSocket)sockets.remove(0);
    }

}
