package org.limewire.xmpp.api.client;

import org.limewire.io.Connectable;
import org.limewire.io.GUID;

/**
 * Requirements for a class that non-blockingly sends connect back requests over the
 * wire to a user specified in <code>userId</code>.
 * 
 * The information is loosely typed, so that it can be implemented in a protocol
 * agnostic way.
 */
public interface ConnectBackRequestSender {

    /**
     * Asynchronously sends a connect request to <code>userId</code>
     * @param userId the peer that should be notified of the connect request
     * @param address this peers' public address to be used for connecting to it
     * @param clientGuid the client guid of the peer that is to connect to this peer
     * @param supportedFWTVersion the supported fwt version or 0 if a TCP connection
     * should be established
     * 
     * @return false if it could be determined immediately that the request will
     * not be sent successfully
     */
    public boolean send(String userId, Connectable address, GUID clientGuid, int supportedFWTVersion);
    
}
