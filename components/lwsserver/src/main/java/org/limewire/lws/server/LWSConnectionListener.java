package org.limewire.lws.server;

/**
 * Listens for notification to when we are connected and disconnected.
 */
public interface LWSConnectionListener {

    /**
     * Called when a connected event changes.
     * 
     * @param isConnected <tt>true</tt> if were are connected, <tt>false</tt>
     *        otherwise
     */
    void connectionChanged(boolean isConnected);

}