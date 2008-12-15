package org.limewire.lws.server;

import java.util.List;
import java.util.Vector;


/**
 * This is what receives actual commands from an {@link LWSDispatcherSupport}.
 * This class abstract out the
 * {@link #addConnectionListener(ConnectionListener adding},
 * {@link #removeConnectionListener(ConnectionListener removing}, and
 * {@link #setConnected(boolean notifying} of {@link LWSConnectionListener}s and
 * leaves subclasses responsible for implementing the
 * {@link ReceivesCommandsFromDispatcher#receiveCommand(String, java.util.Map command handling).
 */
public abstract class AbstractReceivesCommandsFromDispatcher implements LWSReceivesCommandsFromDispatcher {

    private final List<LWSConnectionListener> connectionListeners 
        = new Vector<LWSConnectionListener>();

    public final void setConnected(boolean isConnected) {
        if (!connectionListeners.isEmpty()) {
            synchronized (connectionListeners) {
                for (LWSConnectionListener lis : connectionListeners) {
                    lis.connectionChanged(isConnected);
                }
            }
        }
    }

    public final boolean addConnectionListener(LWSConnectionListener lis) {
        synchronized (connectionListeners) {
            return connectionListeners.contains(lis) ? false : connectionListeners.add(lis);
        }
    }

    public final boolean removeConnectionListener(LWSConnectionListener lis) {
        return connectionListeners.remove(lis);
    }
}
