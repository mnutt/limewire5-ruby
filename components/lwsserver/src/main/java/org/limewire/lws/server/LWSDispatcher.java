package org.limewire.lws.server;

import org.apache.http.nio.protocol.NHttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandler;

/**
 * This is the main part of this component and allows us to attach our
 * authentication scheme and export and interface so they we can attach
 * instances of {@link HttpRequestHandler} to HTTP acceptors.
 */
public interface LWSDispatcher extends NHttpRequestHandler {
    
    /**
     * The prefix to all requests. This will be stripped off when sending to our
     * handlers.
     */
    String PREFIX = "store:";
    
    /**
     * Returns <code>true</code> if <code>lis</code> was added as a listener,
     * <code>false</code> otherwise.
     * 
     * @param lis new listener
     * @return <code>true</code> if <code>lis</code> was added as a listener,
     *         <code>false</code> otherwise.
     */
    boolean addConnectionListener(LWSConnectionListener lis);

    /**
     * Returns <code>true</code> if <code>lis</code> was removed as a listener,
     * <code>false</code> otherwise.
     * 
     * @param lis old listener
     * @return <code>true</code> if <code>lis</code> was removed as a listener,
     *         <code>false</code> otherwise.
     */
    boolean removeConnectionListener(LWSConnectionListener lis);
    
    /**
     * Notifies all connection listeners of the new state of connectivity.
     */
    void notifyConnectionListeners(boolean isConnected);
    
    /**
     * Reset all keys to <code>null</code> and de-authenticates.
     */
    void deauthenticate();
  
}