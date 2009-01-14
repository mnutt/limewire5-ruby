package org.limewire.core.impl.mojito;

import java.beans.PropertyChangeListener;
import java.io.PrintWriter;

import org.limewire.core.api.mojito.MojitoManager;

/**
 * Mock implementation of MojitoManager.
 */
public class MockMojitoManager implements MojitoManager {

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
    }
    
    @Override
    public String getName() {
        return null;
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    /**
     * Invokes the specified command on the Mojito DHT, and forwards output
     * to the specified PrintWriter. 
     */
    @Override
    public boolean handle(String command, PrintWriter out) {
        return false;
    }

}
